package io.artificial.enchantments.internal.book;

import io.artificial.enchantments.api.ItemStorage;
import io.artificial.enchantments.api.book.EnchantedBook;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Factory for creating {@link EnchantedBook} instances.
 * <p>
 * This factory provides a centralized way to create EnchantedBook APIs
 * bound to a specific ItemStorage implementation.
 *
 * @since 0.2.0
 */
public final class BookFactory {

    private BookFactory() {
    }

    /**
     * Creates a new EnchantedBook API instance.
     * <p>
     * The returned instance uses the provided ItemStorage for all
     * enchantment storage operations.
     *
     * @param itemStorage the item storage to use (must not be null)
     * @return a new EnchantedBook instance
     * @throws NullPointerException if itemStorage is null
     * @since 0.2.0
     */
    @NotNull
    public static EnchantedBook create(@NotNull ItemStorage itemStorage) {
        Objects.requireNonNull(itemStorage, "itemStorage cannot be null");
        return new EnchantedBookImpl(itemStorage);
    }
}
