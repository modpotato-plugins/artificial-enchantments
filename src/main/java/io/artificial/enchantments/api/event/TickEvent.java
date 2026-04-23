package io.artificial.enchantments.api.event;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired periodically when an enchanted item is held or worn as armor.
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

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public ItemStack getItem() {
        return item;
    }

    @NotNull
    public EquipmentSlot getSlot() {
        return slot;
    }

    public boolean isHeld() {
        return isHeld;
    }

    public boolean isArmor() {
        return isArmor;
    }

    public int getTickCount() {
        return tickCount;
    }

    public long getHeldDuration() {
        return heldDuration;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
