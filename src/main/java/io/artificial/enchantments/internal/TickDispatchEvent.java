package io.artificial.enchantments.internal;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Synthetic event used to carry tick-scan data into the dispatch spine.
 */
public final class TickDispatchEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack item;
    private final EquipmentSlot slot;
    private final boolean held;
    private final int tickCount;
    private final long heldDuration;

    public TickDispatchEvent(
            @NotNull Player player,
            @NotNull ItemStack item,
            @NotNull EquipmentSlot slot,
            boolean held,
            int tickCount,
            long heldDuration
    ) {
        super(false);
        this.player = player;
        this.item = item.clone();
        this.slot = slot;
        this.held = held;
        this.tickCount = tickCount;
        this.heldDuration = heldDuration;
    }

    /**
     * Returns the player currently being scanned.
     *
     * @return player owning the scanned slot
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns a snapshot of the item in the scanned slot.
     *
     * @return cloned item snapshot
     */
    @NotNull
    public ItemStack getItem() {
        return item;
    }

    /**
     * Returns the scanned equipment slot.
     *
     * @return slot being scanned
     */
    @NotNull
    public EquipmentSlot getSlot() {
        return slot;
    }

    /**
     * Returns whether the scan came from a held-item slot.
     *
     * @return true for held-item scans, false for armor scans
     */
    public boolean isHeld() {
        return held;
    }

    /**
     * Returns the consecutive scan duration represented in ticks.
     *
     * @return consecutive scan duration in ticks
     */
    public int getTickCount() {
        return tickCount;
    }

    /**
     * Returns the consecutive scan duration represented in milliseconds.
     *
     * @return consecutive scan duration in milliseconds
     */
    public long getHeldDuration() {
        return heldDuration;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Returns the shared handler list required by Bukkit events.
     *
     * @return handler list for this synthetic event type
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
