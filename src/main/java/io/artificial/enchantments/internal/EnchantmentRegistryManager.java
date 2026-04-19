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
 * This class maintains the source of truth for all registered enchantments
 * and coordinates with the Paper native registry during bootstrap.
 */
public final class EnchantmentRegistryManager {

    private static final Object LOCK = new Object();
    private static volatile EnchantmentRegistryManager instance;

    // Primary storage: key -> definition
    private final Map<NamespacedKey, EnchantmentDefinition> definitions;

    // Index: material -> applicable enchantments (for fast lookup)
    private final Map<org.bukkit.Material, Set<EnchantmentDefinition>> materialIndex;

    // Pending registrations awaiting native registry freeze
    private final Set<NamespacedKey> pendingNativeRegistration;

    // Track which enchantments have been registered to native registry
    private final Set<NamespacedKey> nativeRegistered;

    private EnchantmentRegistryManager() {
        this.definitions = new ConcurrentHashMap<>();
        this.materialIndex = new ConcurrentHashMap<>();
        this.pendingNativeRegistration = ConcurrentHashMap.newKeySet();
        this.nativeRegistered = ConcurrentHashMap.newKeySet();
    }

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
     */
    public boolean register(@NotNull EnchantmentDefinition definition) {
        NamespacedKey key = definition.getKey();

        if (definitions.containsKey(key)) {
            return false;
        }

        definitions.put(key, definition);
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

    @NotNull
    public Optional<EnchantmentDefinition> get(@NotNull NamespacedKey key) {
        return Optional.ofNullable(definitions.get(key));
    }

    @Nullable
    public EnchantmentDefinition getEnchantment(@NotNull NamespacedKey key) {
        return definitions.get(key);
    }

    @NotNull
    public Collection<EnchantmentDefinition> getAll() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    @NotNull
    public Set<EnchantmentDefinition> getForMaterial(@NotNull org.bukkit.Material material) {
        Set<EnchantmentDefinition> enchantments = materialIndex.get(material);
        return enchantments != null
                ? Collections.unmodifiableSet(new HashSet<>(enchantments))
                : Collections.emptySet();
    }

    /**
     * Returns all enchantments pending native registration.
     * Called during bootstrap freeze event.
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
     */
    public void markNativeRegistered(@NotNull NamespacedKey key) {
        pendingNativeRegistration.remove(key);
        nativeRegistered.add(key);
    }

    /**
     * Checks if an enchantment has been registered to the native registry.
     */
    public boolean isNativeRegistered(@NotNull NamespacedKey key) {
        return nativeRegistered.contains(key);
    }

    /**
     * Clears all registrations. Used primarily for testing.
     */
    public void clear() {
        definitions.clear();
        materialIndex.clear();
        pendingNativeRegistration.clear();
        nativeRegistered.clear();
    }
}
