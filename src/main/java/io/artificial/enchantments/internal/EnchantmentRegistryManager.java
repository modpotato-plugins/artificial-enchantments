package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal registry manager for tracking enchantment definitions.
 * 
 * <p>This class maintains the source of truth for all registered enchantments
 * and coordinates with the Paper native registry during bootstrap. It provides
 * thread-safe storage and indexing for efficient lookup by key or material.
 * 
 * <p>Key responsibilities:
 * <ul>
 *   <li>Maintain primary storage of enchantment definitions</li>
 *   <li>Index enchantments by applicable materials for fast lookup</li>
 *   <li>Track pending native registrations</li>
 *   <li>Coordinate with PaperRegistryBridge for bidirectional lookup</li>
 * </ul>
 *
 * @since 0.1.0
 */
public final class EnchantmentRegistryManager {

    private static final Object LOCK = new Object();
    private static volatile EnchantmentRegistryManager instance;

    // Primary storage: key -> definition
    private final Map<NamespacedKey, EnchantmentDefinition> definitions;

    // Index: material -> applicable enchantments (for fast lookup)
    private final Map<org.bukkit.Material, Set<EnchantmentDefinition>> materialIndex;

    // Pending registrations awaiting native registry compose
    private final Set<NamespacedKey> pendingNativeRegistration;

    // Track which enchantments have been registered to native registry
    private final Set<NamespacedKey> nativeRegistered;
    private volatile boolean nativeRegistrationClosed;

    private EnchantmentRegistryManager() {
        this.definitions = new ConcurrentHashMap<>();
        this.materialIndex = new ConcurrentHashMap<>();
        this.pendingNativeRegistration = ConcurrentHashMap.newKeySet();
        this.nativeRegistered = ConcurrentHashMap.newKeySet();
        this.nativeRegistrationClosed = false;
    }

    /**
     * Gets the singleton instance of the registry manager.
     *
     * @return the shared EnchantmentRegistryManager instance
     */
    public static EnchantmentRegistryManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new EnchantmentRegistryManager();
                }
            }
        }
        return instance;
    }

    /**
     * Registers an enchantment definition.
     * This makes it available for native registration during bootstrap.
     *
     * @param definition the enchantment definition to register
     * @return true if the enchantment was newly registered, false if it already existed
     */
    public boolean register(@NotNull EnchantmentDefinition definition) {
        if (nativeRegistrationClosed) {
            throw new IllegalStateException(
                    "Native enchantment registration is already closed. Register custom enchantments from a Paper PluginBootstrap "
                            + "using ArtificialEnchantmentsAPI.registerBootstrapEnchantment(...)."
            );
        }

        NamespacedKey key = definition.getKey();

        EnchantmentDefinition existing = definitions.putIfAbsent(key, definition);
        if (existing != null) {
            return false;
        }

        pendingNativeRegistration.add(key);

        // Index by applicable materials
        for (org.bukkit.Material material : definition.getApplicableMaterials()) {
            materialIndex.computeIfAbsent(material, k -> ConcurrentHashMap.newKeySet())
                    .add(definition);
        }

        return true;
    }

    /**
     * Unregisters an enchantment definition.
     *
     * @param key the namespaced key of the enchantment to unregister
     * @return true if an enchantment was removed, false if not found
     */
    public boolean unregister(@NotNull NamespacedKey key) {
        EnchantmentDefinition removed = definitions.remove(key);
        if (removed == null) {
            return false;
        }

        pendingNativeRegistration.remove(key);
        nativeRegistered.remove(key);

        for (org.bukkit.Material material : removed.getApplicableMaterials()) {
            Set<EnchantmentDefinition> enchantments = materialIndex.get(material);
            if (enchantments != null) {
                enchantments.remove(removed);
            }
        }

        return true;
    }

    /**
     * Gets an enchantment definition by its key.
     *
     * @param key the enchantment's namespaced key
     * @return optional containing the definition, or empty if not found
     */
    @NotNull
    public Optional<EnchantmentDefinition> get(@NotNull NamespacedKey key) {
        return Optional.ofNullable(definitions.get(key));
    }

    /**
     * Gets an enchantment definition by its key (nullable variant).
     *
     * @param key the enchantment's namespaced key
     * @return the definition, or null if not found
     */
    @Nullable
    public EnchantmentDefinition getEnchantment(@NotNull NamespacedKey key) {
        return definitions.get(key);
    }

    /**
     * Gets all registered enchantment definitions.
     *
     * @return unmodifiable collection of all enchantments
     */
    @NotNull
    public Collection<EnchantmentDefinition> getAll() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    /**
     * Gets all enchantments applicable to a specific material.
     *
     * @param material the material to check
     * @return unmodifiable set of applicable enchantment definitions
     */
    @NotNull
    public Set<EnchantmentDefinition> getForMaterial(@NotNull org.bukkit.Material material) {
        Set<EnchantmentDefinition> enchantments = materialIndex.get(material);
        return enchantments != null
                ? Collections.unmodifiableSet(new HashSet<>(enchantments))
                : Collections.emptySet();
    }

    /**
     * Returns all enchantments pending native registration.
     * Called during the bootstrap registry compose event.
     *
     * @return collection of enchantment definitions awaiting native registration
     */
    @NotNull
    public Collection<EnchantmentDefinition> getPendingRegistrations() {
        Set<EnchantmentDefinition> pending = new HashSet<>();
        for (NamespacedKey key : pendingNativeRegistration) {
            EnchantmentDefinition definition = definitions.get(key);
            if (definition != null) {
                pending.add(definition);
            }
        }
        return pending;
    }

    /**
     * Marks an enchantment as successfully registered to native registry.
     *
     * @param key the namespaced key of the enchantment to mark as registered
     */
    public void markNativeRegistered(@NotNull NamespacedKey key) {
        pendingNativeRegistration.remove(key);
        nativeRegistered.add(key);
    }

    /**
     * Removes an enchantment from the pending native registration queue without
     * marking it as registered. Called when native registration fails so the
     * pending set does not accumulate stale entries.
     *
     * @param key the namespaced key of the enchantment whose pending state should be cleared
     */
    public void clearPendingNativeRegistration(@NotNull NamespacedKey key) {
        pendingNativeRegistration.remove(key);
    }

    /**
     * Checks if an enchantment has been registered to the native registry.
     *
     * @param key the namespaced key of the enchantment to check
     * @return true if the enchantment has been registered to the native registry
     */
    public boolean isNativeRegistered(@NotNull NamespacedKey key) {
        return nativeRegistered.contains(key);
    }

    /**
     * Marks the native registry composition window as closed.
     */
    public void markNativeRegistrationClosed() {
        this.nativeRegistrationClosed = true;
    }

    /**
     * Checks whether native enchantment registration is closed.
     *
     * @return true once the Paper enchantment registry compose event has run
     */
    public boolean isNativeRegistrationClosed() {
        return nativeRegistrationClosed;
    }

    /**
     * Clears all registrations. Used primarily for testing.
     */
    public void clear() {
        definitions.clear();
        materialIndex.clear();
        pendingNativeRegistration.clear();
        nativeRegistered.clear();
        nativeRegistrationClosed = false;
    }
}
