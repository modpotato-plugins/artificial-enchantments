package io.artificial.enchantments.optional.packetevents;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.ItemStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Processes items for visual customization on a per-player basis.
 * <p>
 * This class provides the core logic for modifying item appearance without
 * affecting the actual item state stored on the server. All modifications
 * are display-only and player-specific.
 * <p>
 * Supported customizations:
 * <ul>
 *   <li>Lore text modifications (add/remove enchantment info)</li>
 *   <li>Glint control (show/hide enchantment glint)</li>
 *   <li>Custom lore formatting via callbacks</li>
 *   <li>Rarity-based coloring</li>
 * </ul>
 * <p>
 * All methods are thread-safe for read operations. Write operations on player
 * preferences are synchronized via concurrent collections in the parent adapter.
 *
 * @since 0.1.0
 * @see PacketEventsAdapter
 * @see PacketEventsAdapter.PlayerVisualPreferences
 */
public final class ItemVisualProcessor {

    private final PacketEventsAdapter adapter;
    private final Map<String, Consumer<VisualContext>> registeredFormatters;

    /**
     * Creates a new visual processor.
     *
     * @param adapter the parent adapter
     * @throws NullPointerException if adapter is null
     */
    ItemVisualProcessor(@NotNull PacketEventsAdapter adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter cannot be null");
        this.registeredFormatters = new HashMap<>();
    }

    /**
     * Processes an item for display to a specific player.
     * <p>
     * This is the main entry point for visual customization. It applies
     * all enabled customizations based on the player's preferences.
     *
     * @param item the original item
     * @param playerId the player to customize for
     * @return the visually customized item (may be the same instance)
     * @throws NullPointerException if item is null
     */
    @NotNull
    public ItemStack processForPlayer(@NotNull ItemStack item, @NotNull UUID playerId) {
        Objects.requireNonNull(item, "item cannot be null");
        
        PacketEventsAdapter.PlayerVisualPreferences prefs = adapter.getPlayerPreferences(playerId);
        if (prefs == null || !prefs.isEnabled()) {
            return item;
        }

        return applyCustomization(item, prefs);
    }

    /**
     * Processes an item for display to a specific player with explicit preferences.
     *
     * @param item the original item
     * @param preferences the preferences to apply
     * @return the visually customized item
     * @throws NullPointerException if either parameter is null
     */
    @NotNull
    public ItemStack processWithPreferences(
            @NotNull ItemStack item, 
            @NotNull PacketEventsAdapter.PlayerVisualPreferences preferences) {
        Objects.requireNonNull(item, "item cannot be null");
        Objects.requireNonNull(preferences, "preferences cannot be null");
        
        if (!preferences.isEnabled()) {
            return item;
        }

        return applyCustomization(item, preferences);
    }

    /**
     * Registers a custom lore formatter for a specific enchantment type.
     * <p>
     * The formatter receives a {@link VisualContext} containing enchantment
     * information and can modify the lore output.
     *
     * @param enchantmentKey the enchantment's namespaced key
     * @param formatter the formatter callback
     * @throws NullPointerException if either parameter is null
     */
    public void registerLoreFormatter(
            @NotNull String enchantmentKey, 
            @NotNull Consumer<VisualContext> formatter) {
        Objects.requireNonNull(enchantmentKey, "enchantmentKey cannot be null");
        Objects.requireNonNull(formatter, "formatter cannot be null");
        registeredFormatters.put(enchantmentKey, formatter);
    }

    /**
     * Unregisters a custom lore formatter.
     *
     * @param enchantmentKey the enchantment's namespaced key
     * @throws NullPointerException if enchantmentKey is null
     */
    public void unregisterLoreFormatter(@NotNull String enchantmentKey) {
        Objects.requireNonNull(enchantmentKey, "enchantmentKey cannot be null");
        registeredFormatters.remove(enchantmentKey);
    }

    /**
     * Clears all registered custom formatters.
     */
    public void clearLoreFormatters() {
        registeredFormatters.clear();
    }

    /**
     * Gets a default visual preferences object with sensible defaults.
     *
     * @return default preferences
     */
    @NotNull
    public PacketEventsAdapter.PlayerVisualPreferences createDefaultPreferences() {
        return new PacketEventsAdapter.PlayerVisualPreferences();
    }

    /**
     * Creates a preferences builder with fluent API.
     *
     * @return a new preferences builder
     */
    @NotNull
    public PreferencesBuilder createPreferencesBuilder() {
        return new PreferencesBuilder();
    }

    /**
     * Applies all enabled customizations to an item.
     *
     * @param item the item to customize
     * @param prefs the preferences to apply
     * @return the customized item
     */
    @NotNull
    private ItemStack applyCustomization(
            @NotNull ItemStack item, 
            @NotNull PacketEventsAdapter.PlayerVisualPreferences prefs) {
        
        ItemStorage storage = adapter.getAPI().getItemStorage();
        if (storage == null) {
            return item;
        }

        Map<EnchantmentDefinition, Integer> enchantments = storage.getEnchantments(item);
        if (enchantments.isEmpty()) {
            // Apply glint control even if no artificial enchantments
            if (!prefs.isShowEnchantmentGlint() && item.hasItemMeta()) {
                ItemStack result = item.clone();
                ItemMeta meta = result.getItemMeta();
                if (meta != null) {
                    meta.setEnchantmentGlintOverride(false);
                    result.setItemMeta(meta);
                }
                return result;
            }
            return item;
        }

        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return item;
        }

        // Apply glint control
        if (!prefs.isShowEnchantmentGlint()) {
            meta.setEnchantmentGlintOverride(false);
        }

        // Apply lore modifications
        if (prefs.isModifyLore()) {
            List<Component> lore = buildCustomizedLore(meta, enchantments, prefs);
            meta.lore(lore);
        }

        result.setItemMeta(meta);
        return result;
    }

    /**
     * Builds customized lore text with enchantment information.
     *
     * @param meta the item meta
     * @param enchantments the enchantments on the item
     * @param prefs the player's preferences
     * @return the customized lore list
     */
    @NotNull
    private List<Component> buildCustomizedLore(
            @NotNull ItemMeta meta,
            @NotNull Map<EnchantmentDefinition, Integer> enchantments,
            @NotNull PacketEventsAdapter.PlayerVisualPreferences prefs) {
        
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        
        if (!lore.isEmpty()) {
            lore.add(Component.empty());
        }

        String prefix = prefs.getLorePrefix() != null ? prefs.getLorePrefix() : "§7";

        for (Map.Entry<EnchantmentDefinition, Integer> entry : enchantments.entrySet()) {
            EnchantmentDefinition enchant = entry.getKey();
            int level = entry.getValue();
            
            Component line = formatEnchantmentLine(enchant, level, prefix, prefs);
            lore.add(line);
        }

        return lore;
    }

    /**
     * Formats a single enchantment line.
     *
     * @param enchant the enchantment definition
     * @param level the enchantment level
     * @param prefix the color prefix
     * @param prefs the player's preferences
     * @return the formatted component
     */
    @NotNull
    private Component formatEnchantmentLine(
            @NotNull EnchantmentDefinition enchant,
            int level,
            @NotNull String prefix,
            @NotNull PacketEventsAdapter.PlayerVisualPreferences prefs) {
        
        // Check for custom formatter
        String enchantKey = enchant.getKey().toString();
        Consumer<VisualContext> customFormatter = registeredFormatters.get(enchantKey);
        
        if (customFormatter != null && prefs.getCustomLoreFormatter() != null) {
            // Use player's custom formatter if available
            prefs.getCustomLoreFormatter().accept(
                new PacketEventsAdapter.VisualContext(enchant.getDisplayName().toString(), level, true)
            );
        }

        // Build the line
        String name = extractPlainText(enchant.getDisplayName());
        String romanLevel = toRomanNumerals(level);
        
        Component line = Component.text()
            .append(Component.text(prefix))
            .append(Component.text(name).color(getRarityColor(enchant.getRarity())))
            .append(Component.text(" "))
            .append(Component.text(romanLevel).color(NamedTextColor.WHITE))
            .build();

        return line;
    }

    /**
     * Extracts plain text from an Adventure Component.
     *
     * @param component the component
     * @return plain text string
     */
    @NotNull
    private String extractPlainText(@NotNull Component component) {
        StringBuilder sb = new StringBuilder();
        extractTextRecursive(component, sb);
        return sb.toString();
    }

    /**
     * Recursively extracts text from a component and its children.
     *
     * @param component the component
     * @param builder the string builder
     */
    private void extractTextRecursive(@NotNull Component component, @NotNull StringBuilder builder) {
        if (component instanceof net.kyori.adventure.text.TextComponent textComp) {
            builder.append(textComp.content());
        }
        
        for (Component child : component.children()) {
            extractTextRecursive(child, builder);
        }
    }

    /**
     * Gets the color associated with an enchantment rarity.
     *
     * @param rarity the rarity level
     * @return the appropriate color
     */
    @NotNull
    private NamedTextColor getRarityColor(@NotNull EnchantmentDefinition.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> NamedTextColor.YELLOW;
            case UNCOMMON -> NamedTextColor.AQUA;
            case RARE -> NamedTextColor.LIGHT_PURPLE;
            case VERY_RARE -> NamedTextColor.RED;
        };
    }

    /**
     * Converts a number to Roman numerals.
     *
     * @param number the number (1-3999)
     * @return Roman numeral string
     */
    @NotNull
    private String toRomanNumerals(int number) {
        if (number < 1 || number > 3999) {
            return String.valueOf(number);
        }
        
        // Common levels 1-20 have predefined values
        if (number <= 20) {
            String[] numerals = {
                "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
                "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"
            };
            return numerals[number];
        }
        
        // Algorithm for higher numbers
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        
        StringBuilder sb = new StringBuilder();
        int remaining = number;
        
        for (int i = 0; i < values.length && remaining > 0; i++) {
            while (remaining >= values[i]) {
                sb.append(symbols[i]);
                remaining -= values[i];
            }
        }
        
        return sb.toString();
    }

    /**
     * Fluent builder for creating visual preferences.
     * <p>
     * Provides a convenient way to construct {@link PacketEventsAdapter.PlayerVisualPreferences}
     * with method chaining.
     *
     * @since 0.1.0
     * @see PacketEventsAdapter.PlayerVisualPreferences
     */
    public final class PreferencesBuilder {
        private final PacketEventsAdapter.PlayerVisualPreferences prefs;

        private PreferencesBuilder() {
            this.prefs = new PacketEventsAdapter.PlayerVisualPreferences();
        }

        /**
         * Sets whether visual customization is enabled.
         *
         * @param enabled true to enable customization, false to disable
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        public PreferencesBuilder enabled(boolean enabled) {
            prefs.setEnabled(enabled);
            return this;
        }

        /**
         * Sets whether to modify item lore with enchantment information.
         *
         * @param modify true to modify lore, false to leave unchanged
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        public PreferencesBuilder modifyLore(boolean modify) {
            prefs.setModifyLore(modify);
            return this;
        }

        /**
         * Sets whether to show the enchantment glint effect.
         *
         * @param show true to show glint, false to hide it
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        public PreferencesBuilder showGlint(boolean show) {
            prefs.setShowEnchantmentGlint(show);
            return this;
        }

        /**
         * Sets whether to hide vanilla enchantments from display.
         *
         * @param hide true to hide vanilla enchantments, false to show them
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        public PreferencesBuilder hideVanillaEnchantments(boolean hide) {
            prefs.setHideVanillaEnchantments(hide);
            return this;
        }

        /**
         * Sets the lore prefix string used for enchantment lines.
         *
         * @param prefix the prefix string, or null for default
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        public PreferencesBuilder lorePrefix(@Nullable String prefix) {
            prefs.setLorePrefix(prefix);
            return this;
        }

        /**
         * Sets a custom lore formatter for enchantment display.
         *
         * @param formatter the custom formatter callback, or null for default formatting
         * @return this builder for chaining
         * @since 0.1.0
         */
        @NotNull
        public PreferencesBuilder customFormatter(@Nullable Consumer<PacketEventsAdapter.VisualContext> formatter) {
            prefs.setCustomLoreFormatter(formatter);
            return this;
        }

        /**
         * Builds and returns the configured preferences.
         *
         * @return the configured player visual preferences
         * @since 0.1.0
         */
        @NotNull
        public PacketEventsAdapter.PlayerVisualPreferences build() {
            return prefs;
        }
    }

    /**
     * Context object passed to lore formatters.
     * <p>
     * Contains information about an enchantment being formatted, allowing
     * custom formatters to modify the display output.
     *
     * @since 0.1.0
     */
    public static final class VisualContext {
        private final String enchantmentName;
        private final int level;
        private final boolean isArtificial;
        @Nullable
        private Component overrideComponent;

        /**
         * Creates a new visual context.
         *
         * @param enchantmentName the display name of the enchantment
         * @param level the enchantment level
         * @param isArtificial whether this is an artificial enchantment
         * @throws NullPointerException if enchantmentName is null
         * @since 0.1.0
         */
        public VisualContext(@NotNull String enchantmentName, int level, boolean isArtificial) {
            this.enchantmentName = Objects.requireNonNull(enchantmentName, "enchantmentName cannot be null");
            this.level = level;
            this.isArtificial = isArtificial;
        }

        /**
         * Gets the enchantment display name.
         *
         * @return the enchantment name
         * @since 0.1.0
         */
        @NotNull
        public String getEnchantmentName() {
            return enchantmentName;
        }

        /**
         * Gets the enchantment level.
         *
         * @return the enchantment level
         * @since 0.1.0
         */
        public int getLevel() {
            return level;
        }

        /**
         * Checks if this is an artificial enchantment.
         *
         * @return true if artificial, false if vanilla
         * @since 0.1.0
         */
        public boolean isArtificial() {
            return isArtificial;
        }

        /**
         * Sets an override component to replace the default formatted line.
         *
         * @param component the override component, or null to clear
         * @since 0.1.0
         */
        public void setOverrideComponent(@Nullable Component component) {
            this.overrideComponent = component;
        }

        /**
         * Gets the override component if set.
         *
         * @return the override component, or null if not set
         * @since 0.1.0
         */
        @Nullable
        public Component getOverrideComponent() {
            return overrideComponent;
        }
    }
}
