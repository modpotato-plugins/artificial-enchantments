package io.artificial.enchantments.optional.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.ItemStorage;
import io.artificial.enchantments.optional.packetevents.PacketEventsAdapter.PlayerVisualPreferences;
import io.artificial.enchantments.optional.packetevents.PacketEventsAdapter.VisualContext;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Packet listener for intercepting inventory-related packets.
 * <p>
 * This class listens for WINDOW_ITEMS and SET_SLOT packets sent to players
 * and applies per-player visual customization to item displays. It uses
 * PacketEvents API to modify packets without affecting server-side item state.
 * <p>
 * Key responsibilities:
 * <ul>
 *   <li>Intercept WINDOW_ITEMS (container contents) packets</li>
 *   <li>Intercept SET_SLOT (single slot update) packets</li>
 *   <li>Apply per-player lore modifications</li>
 *   <li>Control enchantment glint display</li>
 * </ul>
 * <p>
 * Important: This class is only loaded when PacketEvents is present. It must
 * not be referenced from core code to prevent classloading errors.
 *
 * @since 0.1.0
 * @see PacketEventsAdapter
 * @see ItemVisualProcessor
 */
final class InventoryPacketListener extends PacketListenerAbstract {

    private final PacketEventsAdapter adapter;
    private final ItemVisualProcessor processor;
    private boolean registered;

    /**
     * Creates a new packet listener.
     *
     * @param adapter the parent adapter
     * @throws NullPointerException if adapter is null
     */
    InventoryPacketListener(@NotNull PacketEventsAdapter adapter) {
        super(PacketListenerPriority.NORMAL);
        this.adapter = Objects.requireNonNull(adapter, "adapter cannot be null");
        this.processor = adapter.getVisualProcessor();
        this.registered = false;
    }

    /**
     * Registers this listener with PacketEvents.
     */
    void register() {
        if (registered) {
            return;
        }
        PacketEvents.getAPI().getEventManager().registerListener(this);
        registered = true;
        adapter.getPlugin().getLogger().fine("InventoryPacketListener registered");
    }

    /**
     * Unregisters this listener from PacketEvents.
     */
    void unregister() {
        if (!registered) {
            return;
        }
        try {
            PacketEvents.getAPI().getEventManager().unregisterListener(this);
        } catch (Exception e) {
            adapter.getPlugin().getLogger().log(Level.FINE, "Error unregistering listener", e);
        }
        registered = false;
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (!adapter.isEnabled()) {
            return;
        }

        Player player = Bukkit.getPlayer(event.getUser().getUUID());
        if (player == null) {
            return;
        }

        PlayerVisualPreferences preferences = adapter.getPlayerPreferences(player.getUniqueId());
        if (preferences == null || !preferences.isEnabled()) {
            return;
        }

        try {
            if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
                handleSetSlot(event, player, preferences);
            } else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
                handleWindowItems(event, player, preferences);
            }
        } catch (Exception e) {
            adapter.getPlugin().getLogger().log(Level.FINE, 
                "Error processing packet for player " + player.getName(), e);
        }
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        // We don't modify incoming packets currently, but may in future
        // for client-side prediction handling
    }

    /**
     * Handles SET_SLOT packets (single slot updates).
     */
    private void handleSetSlot(
            @NotNull PacketSendEvent event, 
            @NotNull Player player,
            @NotNull PlayerVisualPreferences preferences) {
        
        WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);
        ItemStack peItemStack = packet.getItem();
        
        if (peItemStack == null || peItemStack.isEmpty()) {
            return;
        }

        org.bukkit.inventory.ItemStack bukkitItem = SpigotConversionUtil.toBukkitItemStack(peItemStack);
        org.bukkit.inventory.ItemStack modified = processItemForPlayer(bukkitItem, player, preferences);
        
        if (modified != bukkitItem) {
            ItemStack newPeStack = SpigotConversionUtil.fromBukkitItemStack(modified);
            packet.setItem(newPeStack);
        }
    }

    /**
     * Handles WINDOW_ITEMS packets (full container contents).
     */
    private void handleWindowItems(
            @NotNull PacketSendEvent event,
            @NotNull Player player,
            @NotNull PlayerVisualPreferences preferences) {

        WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);
        java.util.List<ItemStack> items = packet.getItems();

        if (items == null || items.isEmpty()) {
            return;
        }

        boolean modified = false;
        for (int i = 0; i < items.size(); i++) {
            ItemStack peItemStack = items.get(i);
            if (peItemStack == null || peItemStack.isEmpty()) {
                continue;
            }

            org.bukkit.inventory.ItemStack bukkitItem = SpigotConversionUtil.toBukkitItemStack(peItemStack);
            org.bukkit.inventory.ItemStack processed = processItemForPlayer(bukkitItem, player, preferences);

            if (processed != bukkitItem) {
                items.set(i, SpigotConversionUtil.fromBukkitItemStack(processed));
                modified = true;
            }
        }

        if (modified) {
            packet.setItems(items);
        }
    }

    /**
     * Processes an item for per-player display customization.
     * <p>
     * This method applies visual modifications based on player preferences:
     * <ul>
     *   <li>Lore modification with enchantment details</li>
     *   <li>Glint control (show/hide enchantment glint)</li>
     *   <li>Custom lore formatting</li>
     * </ul>
     *
     * @param item the original item
     * @param player the player viewing the item
     * @param preferences the player's visual preferences
     * @return the modified item (may be same instance if no changes)
     */
    @NotNull
    private org.bukkit.inventory.ItemStack processItemForPlayer(
            @NotNull org.bukkit.inventory.ItemStack item,
            @NotNull Player player,
            @NotNull PlayerVisualPreferences preferences) {
        
        // Check if item has any enchantments
        ItemStorage storage = adapter.getAPI().getItemStorage();
        if (storage == null) {
            return item;
        }

        Map<EnchantmentDefinition, Integer> enchantments = storage.getEnchantments(item);
        if (enchantments.isEmpty() && !preferences.isHideVanillaEnchantments()) {
            // No artificial enchantments and we're not modifying vanilla
            return item;
        }

        // Clone the item for modification
        org.bukkit.inventory.ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return item;
        }

        // Apply glint control
        if (!preferences.isShowEnchantmentGlint()) {
            meta.setEnchantmentGlintOverride(false);
        }

        // Apply lore modifications
        if (preferences.isModifyLore() && !enchantments.isEmpty()) {
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            
            // Add separator if there's existing lore
            if (!lore.isEmpty()) {
                lore.add(Component.empty());
            }

            // Add artificial enchantments to lore
            String prefix = preferences.getLorePrefix() != null 
                ? preferences.getLorePrefix() 
                : "§7";
            
            for (Map.Entry<EnchantmentDefinition, Integer> entry : enchantments.entrySet()) {
                EnchantmentDefinition enchant = entry.getKey();
                int level = entry.getValue();
                
                Component enchantLine;
                if (preferences.getCustomLoreFormatter() != null) {
                    VisualContext context = new VisualContext(
                        enchant.getDisplayName().toString(),
                        level,
                        true
                    );
                    // Apply custom formatter - the consumer modifies the lore list
                    preferences.getCustomLoreFormatter().accept(context);
                    enchantLine = formatEnchantmentLine(enchant, level, prefix);
                } else {
                    enchantLine = formatEnchantmentLine(enchant, level, prefix);
                }
                
                lore.add(enchantLine);
            }

            meta.lore(lore);
        }

        result.setItemMeta(meta);
        return result;
    }

    /**
     * Formats an enchantment line for item lore.
     *
     * @param enchant the enchantment definition
     * @param level the enchantment level
     * @param prefix the prefix string (color codes)
     * @return the formatted component
     */
    @NotNull
    private Component formatEnchantmentLine(
            @NotNull EnchantmentDefinition enchant, 
            int level, 
            @NotNull String prefix) {
        
        String enchantName = enchant.getDisplayName().toString();
        String romanLevel = toRomanNumerals(level);
        
        return Component.text(prefix + enchantName + " " + romanLevel)
            .color(NamedTextColor.GRAY);
    }

    /**
     * Converts a number to Roman numerals (for enchantment levels).
     *
     * @param number the number to convert
     * @return Roman numeral string
     */
    @NotNull
    private String toRomanNumerals(int number) {
        if (number < 1 || number > 3999) {
            return String.valueOf(number);
        }
        
        String[] numerals = {
            "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
            "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"
        };
        
        if (number < numerals.length) {
            return numerals[number];
        }
        
        // Fallback for higher numbers
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
}
