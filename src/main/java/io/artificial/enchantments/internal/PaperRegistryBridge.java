package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Bridge between the library's enchantment registry and Paper's native
 * enchantment registry. Provides bidirectional lookup and validation.
 * 
 * <p>This class enables seamless interoperability between the library's
 * {@link EnchantmentDefinition} format and Paper's native {@link Enchantment}
 * registry. It handles:
 * <ul>
 *   <li>Bidirectional lookup between library and native enchantments</li>
 *   <li>Registration coordination between internal and native registries</li>
 *   <li>Validation of native registration status</li>
 * </ul>
 *
 * @since 0.2.0
 */
public final class PaperRegistryBridge {

    private static volatile PaperRegistryBridge instance;
    private static final Object LOCK = new Object();

    private final EnchantmentRegistryManager internalRegistry;

    private PaperRegistryBridge() {
        this.internalRegistry = EnchantmentRegistryManager.getInstance();
    }

    /**
     * Gets the singleton instance of the registry bridge.
     *
     * @return the shared PaperRegistryBridge instance
     */
    @NotNull
    public static PaperRegistryBridge getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new PaperRegistryBridge();
                }
            }
        }
        return instance;
    }

    /**
     * Gets a library EnchantmentDefinition by its key.
     *
     * @param key the enchantment's namespaced key
     * @return optional containing the definition, or empty if not found
     */
    @NotNull
    public Optional<EnchantmentDefinition> getDefinition(@NotNull NamespacedKey key) {
        return internalRegistry.get(key);
    }

    /**
     * Gets the native Paper Enchantment by its key.
     * 
     * <p>Returns null if the enchantment hasn't been registered to the native registry yet.
     *
     * @param key the enchantment's namespaced key
     * @return the native Paper Enchantment, or null if not yet registered
     */
    @Nullable
    public Enchantment getNativeEnchantment(@NotNull NamespacedKey key) {
        if (!internalRegistry.isNativeRegistered(key)) {
            return null;
        }

        try {
            return Registry.ENCHANTMENT.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks if an enchantment is registered in the native Paper registry.
     *
     * @param key the enchantment's namespaced key
     * @return true if registered in the native Paper registry
     */
    public boolean isNativeRegistered(@NotNull NamespacedKey key) {
        return internalRegistry.isNativeRegistered(key);
    }

    /**
     * Gets all registered enchantment definitions.
     *
     * @return unmodifiable collection of all enchantment definitions
     */
    @NotNull
    public Collection<EnchantmentDefinition> getAllDefinitions() {
        return internalRegistry.getAll();
    }

    /**
     * Gets all enchantments applicable to a specific material.
     *
     * @param material the material to check
     * @return unmodifiable set of applicable enchantment definitions
     */
    @NotNull
    public Set<EnchantmentDefinition> getEnchantmentsForMaterial(@NotNull org.bukkit.Material material) {
        return internalRegistry.getForMaterial(material);
    }

    /**
     * Registers an enchantment definition with both the internal registry
     * and prepares it for native Paper registration.
     * 
     * <p>Returns false if the enchantment is already native-registered.
     *
     * @param definition the enchantment definition to register
     * @return true if registered successfully, false if already native-registered
     */
    public boolean registerEnchantment(@NotNull EnchantmentDefinition definition) {
        if (internalRegistry.isNativeRegistered(definition.getKey())) {
            return false;
        }

        return internalRegistry.register(definition);
    }

    /**
     * Unregisters an enchantment from the internal tracking.
     * 
     * <p>Note: Native unregistration is not supported after Paper's registry
     * compose window closes. This only removes from internal tracking.
     *
     * @param key the enchantment's namespaced key
     * @return true if unregistered, false if not found
     */
    public boolean unregisterEnchantment(@NotNull NamespacedKey key) {
        return internalRegistry.unregister(key);
    }

    /**
     * Converts a native Paper Enchantment to the library's EnchantmentDefinition.
     *
     * @param enchantment the native Paper Enchantment
     * @return the library EnchantmentDefinition, or null if not found
     */
    @Nullable
    public EnchantmentDefinition fromNative(@NotNull Enchantment enchantment) {
        return internalRegistry.get(enchantment.getKey()).orElse(null);
    }

    /**
     * Gets the native Paper Enchantment for a library definition.
     *
     * @param definition the library enchantment definition
     * @return the native Paper Enchantment, or null if not yet registered
     */
    @Nullable
    public Enchantment toNative(@NotNull EnchantmentDefinition definition) {
        return getNativeEnchantment(definition.getKey());
    }
}
