package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.ItemStorage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Native-first implementation of ItemStorage.
 * <p>
 * This implementation uses Bukkit's native ItemMeta as the sole source of truth
 * for enchantment state. NBT is used only for auxiliary metadata through
 * {@link NbtMetadataStorage}.
 * <p>
 * Key design principles:
 * <ul>
 *   <li>All enchantment state lives in ItemMeta (via addEnchant/removeEnchant)</li>
 *   <li>No duplication of enchant state in NBT</li>
 *   <li>Atomic operations ensure complete success or no change</li>
 *   <li>Level validation uses enchantment definition bounds</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class NativeFirstItemStorage implements ItemStorage {

    private final Supplier<EnchantmentRegistryManager> registrySupplier;
    private final NbtMetadataStorage nbtStorage;
    private final Function<NamespacedKey, Enchantment> nativeResolver;

    private static final String ENCHANTMENT_PREFIX = "ae_enchant_";

    /**
     * Creates a new native-first item storage.
     *
     * @param registrySupplier supplier for the enchantment registry manager
     * @param nbtStorage the NBT metadata storage for auxiliary data
     * @since 0.1.0
     */
    public NativeFirstItemStorage(
            @NotNull Supplier<EnchantmentRegistryManager> registrySupplier,
            @NotNull NbtMetadataStorage nbtStorage) {
        this(registrySupplier, nbtStorage, NativeFirstItemStorage::lookupNativeEnchantment);
    }

    NativeFirstItemStorage(
            @NotNull Supplier<EnchantmentRegistryManager> registrySupplier,
            @NotNull NbtMetadataStorage nbtStorage,
            @NotNull Function<NamespacedKey, Enchantment> nativeResolver) {
        this.registrySupplier = Objects.requireNonNull(registrySupplier, "registrySupplier cannot be null");
        this.nbtStorage = Objects.requireNonNull(nbtStorage, "nbtStorage cannot be null");
        this.nativeResolver = Objects.requireNonNull(nativeResolver, "nativeResolver cannot be null");
    }

    @Override
    @NotNull
    public ItemStack applyEnchantment(
            @NotNull ItemStack item,
            @NotNull EnchantmentDefinition enchantment,
            int level) {
        validateItem(item);
        Objects.requireNonNull(enchantment, "enchantment cannot be null");

        if (level < enchantment.getMinLevel() || level > enchantment.getMaxLevel()) {
            throw new IllegalArgumentException(
                    "Level " + level + " is outside valid range [" +
                            enchantment.getMinLevel() + ", " + enchantment.getMaxLevel() + "]"
            );
        }

        if (item.getType() != Material.ENCHANTED_BOOK && !enchantment.isApplicableTo(item)) {
            throw new IllegalArgumentException(
                    "Enchantment " + enchantment.getKey() + " is not applicable to " + item.getType()
            );
        }

        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("Item has no ItemMeta: " + item.getType());
        }

        Enchantment nativeEnchant = resolveNativeEnchantment(enchantment.getKey());
        if (nativeEnchant == null) {
            throw new IllegalStateException(
                    "Cannot apply enchantment " + enchantment.getKey() + " - not registered in native registry"
            );
        }

        addNativeEnchantment(meta, nativeEnchant, level);
        result.setItemMeta(meta);

        return result;
    }

    @Override
    @NotNull
    public ItemStack applyEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key, int level) {
        Objects.requireNonNull(key, "key cannot be null");

        EnchantmentRegistryManager registry = registrySupplier.get();
        if (registry == null) {
            throw new IllegalStateException("Registry manager not available");
        }

        EnchantmentDefinition definition = registry.getEnchantment(key);
        if (definition == null) {
            throw new IllegalArgumentException("Enchantment not registered: " + key);
        }

        return applyEnchantment(item, definition, level);
    }

    @Override
    @NotNull
    public ItemStack removeEnchantment(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment) {
        validateItem(item);
        Objects.requireNonNull(enchantment, "enchantment cannot be null");

        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return item;
        }

        Enchantment nativeEnchant = resolveNativeEnchantment(enchantment.getKey());
        if (nativeEnchant != null && hasNativeEnchantment(meta, nativeEnchant)) {
            removeNativeEnchantment(meta, nativeEnchant);
            result.setItemMeta(meta);
        }

        return result;
    }

    @Override
    @NotNull
    public ItemStack removeEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key) {
        Objects.requireNonNull(key, "key cannot be null");

        EnchantmentRegistryManager registry = registrySupplier.get();
        if (registry == null) {
            return removeEnchantmentByNativeKey(item, key);
        }

        EnchantmentDefinition definition = registry.getEnchantment(key);
        if (definition != null) {
            return removeEnchantment(item, definition);
        }

        return removeEnchantmentByNativeKey(item, key);
    }

    @Override
    @NotNull
    public ItemStack removeAllEnchantments(@NotNull ItemStack item) {
        validateItem(item);

        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return result;
        }

        EnchantmentRegistryManager registry = registrySupplier.get();
        if (registry != null) {
            for (EnchantmentDefinition definition : registry.getAll()) {
                Enchantment nativeEnchant = resolveNativeEnchantment(definition.getKey());
                if (nativeEnchant != null && hasNativeEnchantment(meta, nativeEnchant)) {
                    removeNativeEnchantment(meta, nativeEnchant);
                }
            }
        }

        result.setItemMeta(meta);
        return result;
    }

    @Override
    public int getEnchantmentLevel(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment) {
        validateItem(item);
        Objects.requireNonNull(enchantment, "enchantment cannot be null");

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        Enchantment nativeEnchant = resolveNativeEnchantment(enchantment.getKey());
        if (nativeEnchant == null) {
            return 0;
        }

        return getNativeEnchantmentLevel(meta, nativeEnchant);
    }

    @Override
    public int getEnchantmentLevel(@NotNull ItemStack item, @NotNull NamespacedKey key) {
        validateItem(item);
        Objects.requireNonNull(key, "key cannot be null");

        EnchantmentRegistryManager registry = registrySupplier.get();
        if (registry != null) {
            EnchantmentDefinition definition = registry.getEnchantment(key);
            if (definition != null) {
                return getEnchantmentLevel(item, definition);
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        Enchantment nativeEnchant = resolveNativeEnchantment(key);
        if (nativeEnchant == null) {
            return 0;
        }

        return getNativeEnchantmentLevel(meta, nativeEnchant);
    }

    @Override
    @NotNull
    public Map<EnchantmentDefinition, Integer> getEnchantments(@NotNull ItemStack item) {
        validateItem(item);

        EnchantmentRegistryManager registry = registrySupplier.get();
        if (registry == null) {
            return Collections.emptyMap();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Collections.emptyMap();
        }

        Map<EnchantmentDefinition, Integer> result = new HashMap<>();

        for (EnchantmentDefinition definition : registry.getAll()) {
            Enchantment nativeEnchant = resolveNativeEnchantment(definition.getKey());
            if (nativeEnchant != null && hasNativeEnchantment(meta, nativeEnchant)) {
                int level = getNativeEnchantmentLevel(meta, nativeEnchant);
                if (level > 0) {
                    result.put(definition, level);
                }
            }
        }

        return Collections.unmodifiableMap(result);
    }

    @Override
    @NotNull
    public Map<NamespacedKey, Integer> getEnchantmentKeys(@NotNull ItemStack item) {
        validateItem(item);

        Map<EnchantmentDefinition, Integer> enchantments = getEnchantments(item);
        Map<NamespacedKey, Integer> result = new HashMap<>();

        for (Map.Entry<EnchantmentDefinition, Integer> entry : enchantments.entrySet()) {
            result.put(entry.getKey().getKey(), entry.getValue());
        }

        return Collections.unmodifiableMap(result);
    }

    @Override
    public boolean hasEnchantment(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment) {
        return getEnchantmentLevel(item, enchantment) > 0;
    }

    @Override
    public boolean hasEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key) {
        return getEnchantmentLevel(item, key) > 0;
    }

    @Override
    @NotNull
    public Set<String> getAuxiliaryMetadataKeys(@NotNull ItemStack item) {
        return nbtStorage.getKeys(item);
    }

    @Override
    @NotNull
    public ItemStack setAuxiliaryMetadata(@NotNull ItemStack item, @NotNull String key, @Nullable String value) {
        return nbtStorage.setString(item, ENCHANTMENT_PREFIX + key, value);
    }

    @Override
    @Nullable
    public String getAuxiliaryMetadata(@NotNull ItemStack item, @NotNull String key) {
        return nbtStorage.getString(item, ENCHANTMENT_PREFIX + key);
    }

    @Override
    public boolean hasAuxiliaryMetadata(@NotNull ItemStack item, @NotNull String key) {
        return nbtStorage.hasKey(item, ENCHANTMENT_PREFIX + key);
    }

    @Override
    @NotNull
    public ItemStack clearAuxiliaryMetadata(@NotNull ItemStack item) {
        return nbtStorage.clear(item);
    }

    private void validateItem(@NotNull ItemStack item) {
        Objects.requireNonNull(item, "item cannot be null");
        if (item.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot operate on AIR item");
        }
    }

    @Nullable
    private Enchantment resolveNativeEnchantment(@NotNull NamespacedKey key) {
        return nativeResolver.apply(key);
    }

    @Nullable
    private static Enchantment lookupNativeEnchantment(@NotNull NamespacedKey key) {
        try {
            return Registry.ENCHANTMENT.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    private void addNativeEnchantment(@NotNull ItemMeta meta, @NotNull Enchantment enchantment, int level) {
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            storageMeta.addStoredEnchant(enchantment, level, true);
        } else {
            meta.addEnchant(enchantment, level, true);
        }
    }

    private boolean hasNativeEnchantment(@NotNull ItemMeta meta, @NotNull Enchantment enchantment) {
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            return storageMeta.hasStoredEnchant(enchantment);
        }
        return meta.hasEnchant(enchantment);
    }

    private int getNativeEnchantmentLevel(@NotNull ItemMeta meta, @NotNull Enchantment enchantment) {
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            return storageMeta.getStoredEnchantLevel(enchantment);
        }
        return meta.getEnchantLevel(enchantment);
    }

    private void removeNativeEnchantment(@NotNull ItemMeta meta, @NotNull Enchantment enchantment) {
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            storageMeta.removeStoredEnchant(enchantment);
        } else {
            meta.removeEnchant(enchantment);
        }
    }

    @NotNull
    private ItemStack removeEnchantmentByNativeKey(@NotNull ItemStack item, @NotNull NamespacedKey key) {
        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return result;
        }

        Enchantment nativeEnchant = resolveNativeEnchantment(key);
        if (nativeEnchant != null && hasNativeEnchantment(meta, nativeEnchant)) {
            removeNativeEnchantment(meta, nativeEnchant);
            result.setItemMeta(meta);
        }

        return result;
    }
}
