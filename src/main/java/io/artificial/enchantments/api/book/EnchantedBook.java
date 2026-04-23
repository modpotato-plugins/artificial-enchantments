package io.artificial.enchantments.api.book;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * API for creating and managing enchanted books with custom enchantments.
 * <p>
 * This interface provides methods for creating enchanted books, querying their
 * enchantments, and extracting enchantments for application to other items.
 * <p>
 * Books use the same native-first storage model as items, storing enchantments
 * directly in the book's ItemMeta. This ensures compatibility with vanilla
 * anvil mechanics and client visibility without packet manipulation.
 *
 * <p><strong>Example Usage:</strong>
 * <pre>{@code
 * // Create a book with a single enchantment
 * ItemStack book = EnchantedBook.builder(api)
 *     .withEnchantment(lifeSteal, 3)
 *     .build();
 *
 * // Create a book with multiple enchantments
 * ItemStack multiBook = EnchantedBook.builder(api)
 *     .withEnchantment(sharpness, 5)
 *     .withEnchantment(fireAspect, 2)
 *     .withEnchantment(looting, 3)
 *     .build();
 *
 * // Query a book's enchantments
 * Map<EnchantmentDefinition, Integer> enchants = EnchantedBook.getEnchantments(book);
 * }</pre>
 *
 * @see io.artificial.enchantments.internal.book.BookFactory
 * @since 0.2.0
 */
public interface EnchantedBook {

    /**
     * Checks if an item is an enchanted book (vanilla or custom).
     * <p>
     * This returns true for both vanilla ENCHANTED_BOOK items and books
     * created through this API.
     *
     * @param item the item to check (must not be null)
     * @return true if the item is an enchanted book
     * @throws IllegalArgumentException if item is null
     * @since 0.2.0
     */
    boolean isEnchantedBook(@NotNull ItemStack item);

    /**
     * Checks if an enchanted book contains custom (artificial) enchantments.
     * <p>
     * This distinguishes between books with only vanilla enchantments and
     * books that have enchantments registered through this API.
     *
     * @param book the book to check (must not be null)
     * @return true if the book has at least one artificial enchantment
     * @throws IllegalArgumentException if book is null or not a book
     * @since 0.2.0
     */
    boolean hasCustomEnchantments(@NotNull ItemStack book);

    /**
     * Gets all artificial enchantments stored on a book.
     * <p>
     * Returns a map of enchantment definitions to their levels. Only
     * enchantments registered through this API are included.
     *
     * @param book the book to query (must not be null)
     * @return unmodifiable map of enchantments to levels (never null, may be empty)
     * @throws IllegalArgumentException if book is null or not a book
     * @since 0.2.0
     */
    @NotNull
    Map<EnchantmentDefinition, Integer> getEnchantments(@NotNull ItemStack book);

    /**
     * Gets the level of a specific artificial enchantment on a book.
     *
     * @param book the book to query (must not be null)
     * @param enchantment the enchantment to look for (must not be null)
     * @return the enchantment level, or 0 if not present
     * @throws IllegalArgumentException if book or enchantment is null
     * @since 0.2.0
     */
    int getEnchantmentLevel(@NotNull ItemStack book, @NotNull EnchantmentDefinition enchantment);

    /**
     * Gets the level of an artificial enchantment by its key.
     *
     * @param book the book to query (must not be null)
     * @param key the enchantment's namespaced key (must not be null)
     * @return the enchantment level, or 0 if not present
     * @throws IllegalArgumentException if book or key is null
     * @since 0.2.0
     */
    int getEnchantmentLevel(@NotNull ItemStack book, @NotNull NamespacedKey key);

    /**
     * Gets all enchantment keys on a book as NamespacedKey to level mapping.
     * <p>
     * This includes both vanilla and artificial enchantments.
     *
     * @param book the book to query (must not be null)
     * @return unmodifiable map of all enchantment keys to levels
     * @throws IllegalArgumentException if book is null or not a book
     * @since 0.2.0
     */
    @NotNull
    Map<NamespacedKey, Integer> getAllEnchantmentKeys(@NotNull ItemStack book);

    /**
     * Checks if a specific artificial enchantment exists on a book.
     *
     * @param book the book to check (must not be null)
     * @param enchantment the enchantment to look for (must not be null)
     * @return true if the book has the enchantment at any level
     * @throws IllegalArgumentException if book or enchantment is null
     * @since 0.2.0
     */
    boolean hasEnchantment(@NotNull ItemStack book, @NotNull EnchantmentDefinition enchantment);

    /**
     * Checks if an artificial enchantment exists by its key.
     *
     * @param book the book to check (must not be null)
     * @param key the enchantment's namespaced key (must not be null)
     * @return true if the book has the enchantment at any level
     * @throws IllegalArgumentException if book or key is null
     * @since 0.2.0
     */
    boolean hasEnchantment(@NotNull ItemStack book, @NotNull NamespacedKey key);

    /**
     * Gets a builder for creating enchanted books.
     * <p>
     * The returned builder can be used to construct books with one or more
     * enchantments. Books are created as ENCHANTED_BOOK items with enchantments
     * stored in native ItemMeta.
     *
     * @return a new book builder instance
     * @since 0.2.0
     */
    @NotNull
    Builder builder();

    /**
     * Builder for creating enchanted books.
     * <p>
     * This builder follows a fluent API pattern for convenient book creation.
     * Multiple enchantments can be added, and books are validated on build.
     *
     * @since 0.2.0
     */
    interface Builder {

        /**
         * Adds an enchantment to the book at the specified level.
         * <p>
         * The level must be within the enchantment's defined min/max bounds.
         * If the enchantment is already present, the higher level is kept
         * (matching vanilla behavior for combining books).
         *
         * @param enchantment the enchantment to add (must not be null)
         * @param level the level to apply (must be within bounds)
         * @return this builder for chaining
         * @throws IllegalArgumentException if enchantment is null or level is invalid
         * @since 0.2.0
         */
        @NotNull
        Builder withEnchantment(@NotNull EnchantmentDefinition enchantment, int level);

        /**
         * Adds an enchantment by its registry key.
         * <p>
         * This is a convenience method that looks up the definition before adding.
         *
         * @param key the enchantment's namespaced key (must not be null)
         * @param level the level to apply
         * @return this builder for chaining
         * @throws IllegalArgumentException if enchantment not found or level invalid
         * @since 0.2.0
         */
        @NotNull
        Builder withEnchantment(@NotNull NamespacedKey key, int level);

        /**
         * Sets a custom display name for the book.
         * <p>
         * If not set, the book will use the default name based on its
         * enchantments (e.g., "Enchanted Book" or "Book of Life Steal").
         *
         * @param displayName the custom display name, or null for default
         * @return this builder for chaining
         * @since 0.2.0
         */
        @NotNull
        Builder displayName(@Nullable String displayName);

        /**
         * Adds lore lines to the book.
         * <p>
         * These lines appear below the enchantment descriptions in the
         * book's tooltip.
         *
         * @param lore the lore lines to add (must not be null)
         * @return this builder for chaining
         * @throws IllegalArgumentException if lore is null
         * @since 0.2.0
         */
        @NotNull
        Builder lore(@NotNull String... lore);

        /**
         * Sets whether the book is a "treasure" book (from chests/loot).
         * <p>
         * This affects anvil cost calculation - treasure books cost more.
         *
         * @param treasure true if this is a treasure book
         * @return this builder for chaining
         * @since 0.2.0
         */
        @NotNull
        Builder treasure(boolean treasure);

        /**
         * Builds and returns the enchanted book.
         * <p>
         * The book is created as an ENCHANTED_BOOK item with all specified
         * enchantments stored in native ItemMeta. The returned item is a
         * new ItemStack instance.
         *
         * @return the created enchanted book
         * @throws IllegalStateException if no enchantments were added
         * @since 0.2.0
         */
        @NotNull
        ItemStack build();

        /**
         * Resets the builder to its initial state.
         * <p>
         * This clears all enchantments and settings, allowing the builder
         * to be reused for creating another book.
         *
         * @return this builder for chaining
         * @since 0.2.0
         */
        @NotNull
        Builder reset();
    }
}
