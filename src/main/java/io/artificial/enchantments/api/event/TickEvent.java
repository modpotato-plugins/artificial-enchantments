package io.artificial.enchantments.api.event;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired periodically when an enchanted item is held or worn as armor.
 *
 * <p>Carries data about the player, item, slot, and consecutive scan counters
 * during periodic tick events. This event is not cancellable.
 *
 * @see EnchantEffectEvent
 * @since 0.1.0
 */
public class TickEvent extends EnchantEffectEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack item;
    private final EquipmentSlot slot;
    private final boolean isHeld;
    private final boolean isArmor;
    private final int tickCount;
    private final long heldDuration;

    /**
     * Creates a new {@code TickEvent}.
     *
     * @param enchantment the enchantment that triggered this event
     * @param level the level of the enchantment
     * @param scaledValue the scaled value from the enchantment's scaling algorithm
     * @param player the player holding or wearing the item
     * @param item the enchanted item
     * @param slot the equipment slot
     * @param isHeld true if the item is being held in hand
     * @param isArmor true if the item is worn as armor
     * @param tickCount the number of ticks represented by the current
     *                  consecutive scan window
     * @param heldDuration the approximate held or equipped duration in
     *                     milliseconds for the current consecutive scan window
     */
    public TickEvent(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Player player,
            @NotNull ItemStack item,
            @NotNull EquipmentSlot slot,
            boolean isHeld,
            boolean isArmor,
            int tickCount,
            long heldDuration
    ) {
        super(enchantment, level, scaledValue);
        this.player = player;
        this.item = item;
        this.slot = slot;
        this.isHeld = isHeld;
        this.isArmor = isArmor;
        this.tickCount = tickCount;
        this.heldDuration = heldDuration;
    }

    /**
     * Gets the player holding or wearing the item.
     *
     * @return the player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the enchanted item.
     *
     * @return the item
     */
    @NotNull
    public ItemStack getItem() {
        return item;
    }

    /**
     * Gets the equipment slot.
     *
     * @return the slot
     */
    @NotNull
    public EquipmentSlot getSlot() {
        return slot;
    }

    /**
     * Checks if the item is being held in hand.
     *
     * @return true if held
     */
    public boolean isHeld() {
        return isHeld;
    }

    /**
     * Checks if the item is worn as armor.
     *
     * @return true if armor
     */
    public boolean isArmor() {
        return isArmor;
    }

    /**
     * Gets the tick counter.
     *
     * @return the tick count
     */
    public int getTickCount() {
        return tickCount;
    }

    /**
     * Gets the duration the item has been held in ticks.
     *
     * @return the held duration
     */
    public long getHeldDuration() {
        return heldDuration;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Gets the handler list for this event type.
     *
     * @return the handler list
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
