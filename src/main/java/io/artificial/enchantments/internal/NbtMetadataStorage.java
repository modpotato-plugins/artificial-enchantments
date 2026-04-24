package io.artificial.enchantments.internal;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles auxiliary metadata storage in NBT for items.
 * <p>
 * This class is strictly for auxiliary data (display markers, compatibility flags,
 * custom data) and must NEVER store enchantment state. Native ItemMeta is the
 * sole source of truth for enchantment levels and presence.
 * <p>
 * All metadata is stored under a root compound key to avoid collisions with
 * other plugins or Minecraft's native data.
 * <p>
 * This class is thread-safe for read operations. Write operations should be
 * synchronized at a higher level to ensure atomic item updates.
 *
 * @since 0.1.0
 */
public class NbtMetadataStorage {

    private static final String ROOT_COMPOUND_KEY = "artificial_enchantments";
    private static final String METADATA_SUBKEY = "aux_meta";

    /**
     * Creates a new NBT metadata storage handler.
     *
     * @since 0.1.0
     */
    public NbtMetadataStorage() {
    }

    /**
     * Sets a string metadata value.
     *
     * @param item the item to modify
     * @param key the metadata key
     * @param value the value to store, or null to remove
     * @return the modified item
     */
    @NotNull
    public ItemStack setString(@NotNull ItemStack item, @NotNull String key, @Nullable String value) {
        Objects.requireNonNull(item, "item cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        if (key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be empty");
        }

        NBTItem nbtItem = new NBTItem(item);
        NBTCompound root = getOrCreateRootCompound(nbtItem);
        NBTCompound meta = getOrCreateMetadataCompound(root);

        if (value == null) {
            meta.removeKey(key);
        } else {
            meta.setString(key, value);
        }

        return nbtItem.getItem();
    }

    /**
     * Gets a string metadata value.
     *
     * @param item the item to query
     * @param key the metadata key
     * @return the stored value, or null if not present
     */
    @Nullable
    public String getString(@NotNull ItemStack item, @NotNull String key) {
        Objects.requireNonNull(item, "item cannot be null");
        Objects.requireNonNull(key, "key cannot be null");

        NBTItem nbtItem = new NBTItem(item);
        NBTCompound root = nbtItem.getCompound(ROOT_COMPOUND_KEY);
        if (root == null) {
            return null;
        }

        NBTCompound meta = root.getCompound(METADATA_SUBKEY);
        if (meta == null) {
            return null;
        }

        return meta.hasTag(key) ? meta.getString(key) : null;
    }

    /**
     * Sets an integer metadata value.
     *
     * @param item the item to modify
     * @param key the metadata key
     * @param value the value to store
     * @return the modified item
     */
    @NotNull
    public ItemStack setInt(@NotNull ItemStack item, @NotNull String key, int value) {
        Objects.requireNonNull(item, "item cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        if (key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be empty");
        }

        NBTItem nbtItem = new NBTItem(item);
        NBTCompound root = getOrCreateRootCompound(nbtItem);
        NBTCompound meta = getOrCreateMetadataCompound(root);
        meta.setInteger(key, value);

        return nbtItem.getItem();
    }

    /**
     * Gets an integer metadata value.
     *
     * @param item the item to query
     * @param key the metadata key
     * @param defaultValue the default value if not present
     * @return the stored value, or defaultValue if not present
     */
    public int getInt(@NotNull ItemStack item, @NotNull String key, int defaultValue) {
        Objects.requireNonNull(item, "item cannot be null");
        Objects.requireNonNull(key, "key cannot be null");

        NBTItem nbtItem = new NBTItem(item);
        NBTCompound root = nbtItem.getCompound(ROOT_COMPOUND_KEY);
        if (root == null) {
            return defaultValue;
        }

        NBTCompound meta = root.getCompound(METADATA_SUBKEY);
        if (meta == null || !meta.hasTag(key)) {
            return defaultValue;
        }

        return meta.getInteger(key);
    }

    /**
     * Sets a boolean metadata value.
     *
     * @param item the item to modify
     * @param key the metadata key
     * @param value the value to store
     * @return the modified item
     */
    @NotNull
    public ItemStack setBoolean(@NotNull ItemStack item, @NotNull String key, boolean value) {
        Objects.requireNonNull(item, "item cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        if (key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be empty");
        }

        NBTItem nbtItem = new NBTItem(item);
        NBTCompound root = getOrCreateRootCompound(nbtItem);
        NBTCompound meta = getOrCreateMetadataCompound(root);
        meta.setBoolean(key, value);

        return nbtItem.getItem();
    }

    /**
     * Gets a boolean metadata value.
     *
     * @param item the item to query
     * @param key the metadata key
     * @param defaultValue the default value if not present
     * @return the stored value, or defaultValue if not present
     */
    public boolean getBoolean(@NotNull ItemStack item, @NotNull String key, boolean defaultValue) {
        Objects.requireNonNull(item, "item cannot be null");
        Objects.requireNonNull(key, "key cannot be null");

        NBTItem nbtItem = new NBTItem(item);
        NBTCompound root = nbtItem.getCompound(ROOT_COMPOUND_KEY);
        if (root == null) {
            return defaultValue;
        }

        NBTCompound meta = root.getCompound(METADATA_SUBKEY);
        if (meta == null || !meta.hasTag(key)) {
            return defaultValue;
        }

        return meta.getBoolean(key);
    }

    /**
     * Removes a metadata entry.
     *
     * @param item the item to modify
     * @param key the metadata key to remove
     * @return the modified item
     */
    @NotNull
    public ItemStack remove(@NotNull ItemStack item, @NotNull String key) {
        return setString(item, key, null);
    }

    /**
     * Checks if a metadata key exists.
     *
     * @param item the item to query
     * @param key the metadata key
     * @return true if the key exists
     */
    public boolean hasKey(@NotNull ItemStack item, @NotNull String key) {
        Objects.requireNonNull(item, "item cannot be null");
        Objects.requireNonNull(key, "key cannot be null");

        NBTItem nbtItem = new NBTItem(item);
        NBTCompound root = nbtItem.getCompound(ROOT_COMPOUND_KEY);
        if (root == null) {
            return false;
        }

        NBTCompound meta = root.getCompound(METADATA_SUBKEY);
        if (meta == null) {
            return false;
        }

        return meta.hasTag(key);
    }

    /**
     * Gets all metadata keys.
     *
     * @param item the item to query
     * @return an unmodifiable set of keys (may be empty)
     */
    @NotNull
    public Set<String> getKeys(@NotNull ItemStack item) {
        Objects.requireNonNull(item, "item cannot be null");

        NBTItem nbtItem = new NBTItem(item);
        NBTCompound root = nbtItem.getCompound(ROOT_COMPOUND_KEY);
        if (root == null) {
            return Collections.emptySet();
        }

        NBTCompound meta = root.getCompound(METADATA_SUBKEY);
        if (meta == null) {
            return Collections.emptySet();
        }

        return meta.getKeys().stream()
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Clears all auxiliary metadata while preserving the root compound.
     *
     * @param item the item to modify
     * @return the modified item
     */
    @NotNull
    public ItemStack clear(@NotNull ItemStack item) {
        Objects.requireNonNull(item, "item cannot be null");

        NBTItem nbtItem = new NBTItem(item);
        NBTCompound root = nbtItem.getCompound(ROOT_COMPOUND_KEY);
        if (root == null) {
            return item;
        }

        root.removeKey(METADATA_SUBKEY);

        if (root.getKeys().isEmpty()) {
            nbtItem.removeKey(ROOT_COMPOUND_KEY);
        }

        return nbtItem.getItem();
    }

    /**
     * Checks if any auxiliary metadata exists.
     *
     * @param item the item to query
     * @return true if any metadata keys exist
     */
    public boolean hasAnyMetadata(@NotNull ItemStack item) {
        Objects.requireNonNull(item, "item cannot be null");

        NBTItem nbtItem = new NBTItem(item);
        NBTCompound root = nbtItem.getCompound(ROOT_COMPOUND_KEY);
        if (root == null) {
            return false;
        }

        NBTCompound meta = root.getCompound(METADATA_SUBKEY);
        if (meta == null) {
            return false;
        }

        return !meta.getKeys().isEmpty();
    }

    private NBTCompound getOrCreateRootCompound(NBTItem nbtItem) {
        NBTCompound root = nbtItem.getCompound(ROOT_COMPOUND_KEY);
        if (root == null) {
            root = nbtItem.addCompound(ROOT_COMPOUND_KEY);
        }
        return root;
    }

    private NBTCompound getOrCreateMetadataCompound(NBTCompound root) {
        NBTCompound meta = root.getCompound(METADATA_SUBKEY);
        if (meta == null) {
            meta = root.addCompound(METADATA_SUBKEY);
        }
        return meta;
    }
}
