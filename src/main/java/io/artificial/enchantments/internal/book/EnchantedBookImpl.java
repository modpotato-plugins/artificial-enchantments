package io.artificial.enchantments.internal.book;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.book.EnchantedBook;
import io.artificial.enchantments.api.ItemStorage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link EnchantedBook} using native ItemMeta storage.
 * <p>
 * This implementation follows the native-first policy, storing enchantments
 * directly in the book's EnchantmentStorageMeta. No auxiliary NBT is used
 * for enchantment data.
 *
 * @since 0.2.0
 */
public class EnchantedBookImpl implements EnchantedBook {

    private final ItemStorage itemStorage;

    public EnchantedBookImpl(@NotNull ItemStorage itemStorage) {
        this.itemStorage = Objects.requireNonNull(itemStorage, "itemStorage cannot be null");
    }

    @Override
    public boolean isEnchantedBook(@NotNull ItemStack item) {
        Objects.requireNonNull(item, "item cannot be null");
        return item.getType() == Material.ENCHANTED_BOOK;
    }

    @Override
    public boolean hasCustomEnchantments(@NotNull ItemStack book) {
        Objects.requireNonNull(book, "book cannot be null");
        if (!isEnchantedBook(book)) {
            return false;
        }

        Map<EnchantmentDefinition, Integer> enchantments = getEnchantments(book);
        return !enchantments.isEmpty();
    }

    @Override
    @NotNull
    public Map<EnchantmentDefinition, Integer> getEnchantments(@NotNull ItemStack book) {
        Objects.requireNonNull(book, "book cannot be null");
        if (!isEnchantedBook(book)) {
            return Collections.emptyMap();
        }

        return itemStorage.getEnchantments(book);
    }

    @Override
    public int getEnchantmentLevel(@NotNull ItemStack book, @NotNull EnchantmentDefinition enchantment) {
        Objects.requireNonNull(book, "book cannot be null");
        Objects.requireNonNull(enchantment, "enchantment cannot be null");
        
        if (!isEnchantedBook(book)) {
            return 0;
        }

        return itemStorage.getEnchantmentLevel(book, enchantment);
    }

    @Override
    public int getEnchantmentLevel(@NotNull ItemStack book, @NotNull NamespacedKey key) {
        Objects.requireNonNull(book, "book cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        
        if (!isEnchantedBook(book)) {
            return 0;
        }

        return itemStorage.getEnchantmentLevel(book, key);
    }

    @Override
    @NotNull
    public Map<NamespacedKey, Integer> getAllEnchantmentKeys(@NotNull ItemStack book) {
        Objects.requireNonNull(book, "book cannot be null");
        if (!isEnchantedBook(book)) {
            return Collections.emptyMap();
        }

        ItemMeta meta = book.getItemMeta();
        if (!(meta instanceof EnchantmentStorageMeta storageMeta)) {
            return Collections.emptyMap();
        }

        Map<NamespacedKey, Integer> result = new HashMap<>();
        for (var entry : storageMeta.getStoredEnchants().entrySet()) {
            result.put(entry.getKey().getKey(), entry.getValue());
        }

        return Collections.unmodifiableMap(result);
    }

    @Override
    public boolean hasEnchantment(@NotNull ItemStack book, @NotNull EnchantmentDefinition enchantment) {
        return getEnchantmentLevel(book, enchantment) > 0;
    }

    @Override
    public boolean hasEnchantment(@NotNull ItemStack book, @NotNull NamespacedKey key) {
        return getEnchantmentLevel(book, key) > 0;
    }

    @Override
    @NotNull
    public Builder builder() {
        return new EnchantedBookBuilderImpl(itemStorage);
    }
}
