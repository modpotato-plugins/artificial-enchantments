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
 */
public final class PaperRegistryBridge {

    private static volatile PaperRegistryBridge instance;
    private static final Object LOCK = new Object();

    private final EnchantmentRegistryManager internalRegistry;

    private PaperRegistryBridge() {
        this.internalRegistry = EnchantmentRegistryManager.getInstance();
    }

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
     */
    @NotNull
    public Optional<EnchantmentDefinition> getDefinition(@NotNull NamespacedKey key) {
        return internalRegistry.get(key);
    }

    /**
     * Gets the native Paper Enchantment by its key.
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
     */
    public boolean isNativeRegistered(@NotNull NamespacedKey key) {
        return internalRegistry.isNativeRegistered(key);
    }

    /**
     * Gets all registered enchantment definitions.
     */
    @NotNull
    public Collection<EnchantmentDefinition> getAllDefinitions() {
        return internalRegistry.getAll();
    }

    /**
     * Gets all enchantments applicable to a specific material.
     */
    @NotNull
    public Set<EnchantmentDefinition> getEnchantmentsForMaterial(@NotNull org.bukkit.Material material) {
        return internalRegistry.getForMaterial(material);
    }

    /**
     * Registers an enchantment definition with both the internal registry
     * and prepares it for native Paper registration.
     */
    public boolean registerEnchantment(@NotNull EnchantmentDefinition definition) {
        if (internalRegistry.isNativeRegistered(definition.getKey())) {
            return false;
        }

        return internalRegistry.register(definition);
    }

    /**
     * Unregisters an enchantment. Note: Native unregistration is not
     * fully supported by Paper, so this only removes from internal tracking.
     */
    public boolean unregisterEnchantment(@NotNull NamespacedKey key) {
        return internalRegistry.unregister(key);
    }

    /**
     * Converts a native Paper Enchantment to the library's EnchantmentDefinition.
     */
    @Nullable
    public EnchantmentDefinition fromNative(@NotNull Enchantment enchantment) {
        return internalRegistry.get(enchantment.getKey()).orElse(null);
    }

    /**
     * Gets the native Paper Enchantment for a library definition.
     */
    @Nullable
    public Enchantment toNative(@NotNull EnchantmentDefinition definition) {
        return getNativeEnchantment(definition.getKey());
    }
}
