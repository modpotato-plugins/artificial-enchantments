package io.artificial.enchantments.api.event;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when an enchantment effect triggers during item usage, drop, pickup, or durability change.
 */
public class ItemEvent extends EnchantEffectEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack item;
    private final EquipmentSlot slot;
    private final boolean isDrop;
    private final boolean isPickup;
    private final int currentDurability;
    private final int maxDurability;
    private int damageTaken;
    private boolean cancelled;

    public ItemEvent(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Player player,
            @NotNull ItemStack item,
            @NotNull EquipmentSlot slot,
            boolean isDrop,
            boolean isPickup,
            int currentDurability,
            int maxDurability,
            int damageTaken
    ) {
        super(enchantment, level, scaledValue);
        this.player = player;
        this.item = item;
        this.slot = slot;
        this.isDrop = isDrop;
        this.isPickup = isPickup;
        this.currentDurability = currentDurability;
        this.maxDurability = maxDurability;
        this.damageTaken = damageTaken;
        this.cancelled = false;
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

    public boolean isDrop() {
        return isDrop;
    }

    public boolean isPickup() {
        return isPickup;
    }

    public int getCurrentDurability() {
        return currentDurability;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public int getDamageTaken() {
        return damageTaken;
    }

    public void setDamageTaken(int damageTaken) {
        this.damageTaken = Math.max(0, damageTaken);
    }

    public void reduceDamage(int amount) {
        this.damageTaken = Math.max(0, this.damageTaken - amount);
    }

    public boolean willBreak() {
        return currentDurability - damageTaken <= 0;
    }

    public boolean isCraftingIngredient() {
        return false;
    }

    public boolean isAnvilCombination() {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
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
