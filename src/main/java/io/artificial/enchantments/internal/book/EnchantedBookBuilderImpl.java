package io.artificial.enchantments.internal.book;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.ItemStorage;
import io.artificial.enchantments.api.book.EnchantedBook;
import io.artificial.enchantments.internal.EnchantmentRegistryManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builder implementation for creating enchanted books.
 *
 * @since 0.2.0
 */
public class EnchantedBookBuilderImpl implements EnchantedBook.Builder {

    private final ItemStorage itemStorage;
    private final EnchantmentRegistryManager registryManager;
    private final Map<EnchantmentDefinition, Integer> enchantments;
    private final List<String> lore;
    private String displayName;
    private boolean treasure;

    /**
     * Creates a new enchanted book builder.
     *
     * @param itemStorage the item storage for applying enchantments
     */
    public EnchantedBookBuilderImpl(@NotNull ItemStorage itemStorage) {
        this.itemStorage = Objects.requireNonNull(itemStorage, "itemStorage cannot be null");
        this.registryManager = EnchantmentRegistryManager.getInstance();
        this.enchantments = new HashMap<>();
        this.lore = new ArrayList<>();
        this.treasure = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public EnchantedBook.Builder withEnchantment(@NotNull EnchantmentDefinition enchantment, int level) {
        Objects.requireNonNull(enchantment, "enchantment cannot be null");

        if (level < enchantment.getMinLevel() || level > enchantment.getMaxLevel()) {
            throw new IllegalArgumentException(
                    "Level " + level + " is outside valid range [" +
                            enchantment.getMinLevel() + ", " + enchantment.getMaxLevel() + "] for " +
                            enchantment.getKey()
            );
        }

        enchantments.merge(enchantment, level, Math::max);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public EnchantedBook.Builder withEnchantment(@NotNull NamespacedKey key, int level) {
        Objects.requireNonNull(key, "key cannot be null");

        EnchantmentDefinition definition = registryManager.getEnchantment(key);
        if (definition == null) {
            throw new IllegalArgumentException("Enchantment not registered: " + key);
        }

        return withEnchantment(definition, level);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public EnchantedBook.Builder displayName(@Nullable String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public EnchantedBook.Builder lore(@NotNull String... lore) {
        Objects.requireNonNull(lore, "lore cannot be null");
        for (String line : lore) {
            if (line != null) {
                this.lore.add(line);
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public EnchantedBook.Builder treasure(boolean treasure) {
        this.treasure = treasure;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ItemStack build() {
        if (enchantments.isEmpty()) {
            throw new IllegalStateException("Cannot build enchanted book with no enchantments");
        }

        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("Failed to create book ItemMeta");
        }

        ItemStack tempBook = book;
        for (Map.Entry<EnchantmentDefinition, Integer> entry : enchantments.entrySet()) {
            tempBook = itemStorage.applyEnchantment(tempBook, entry.getKey(), entry.getValue());
        }
        book = tempBook;

        meta = (EnchantmentStorageMeta) book.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("Failed to get book ItemMeta after enchantment");
        }

        if (displayName != null) {
            meta.setDisplayName(displayName);
        }

        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }

        if (treasure) {
            book = itemStorage.setAuxiliaryMetadata(book, "treasure_book", "true");
        }

        book.setItemMeta(meta);
        return book;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public EnchantedBook.Builder reset() {
        enchantments.clear();
        lore.clear();
        displayName = null;
        treasure = false;
        return this;
    }
}
