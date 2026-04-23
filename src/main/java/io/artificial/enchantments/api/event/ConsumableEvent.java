package io.artificial.enchantments.api.event;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when an enchantment effect triggers during item consumption.
 */
public class ConsumableEvent extends EnchantEffectEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack item;
    private final Material material;
    private int foodLevel;
    private float saturation;
    private double healthRestored;
    private int consumptionTime;
    private boolean cancelled;

    public ConsumableEvent(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Player player,
            @NotNull ItemStack item,
            @NotNull Material material,
            int foodLevel,
            float saturation,
            double healthRestored,
            int consumptionTime
    ) {
        super(enchantment, level, scaledValue);
        this.player = player;
        this.item = item;
        this.material = material;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.healthRestored = healthRestored;
        this.consumptionTime = consumptionTime;
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
    public Material getMaterial() {
        return material;
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public void setFoodLevel(int foodLevel) {
        this.foodLevel = Math.clamp(foodLevel, 0, 20);
    }

    public float getSaturation() {
        return saturation;
    }

    public void setSaturation(float saturation) {
        this.saturation = Math.clamp(saturation, 0.0f, this.foodLevel);
    }

    public double getHealthRestored() {
        return healthRestored;
    }

    public void setHealthRestored(double healthRestored) {
        this.healthRestored = Math.max(0.0, healthRestored);
    }

    public int getConsumptionTime() {
        return consumptionTime;
    }

    public void setConsumptionTime(int consumptionTime) {
        this.consumptionTime = Math.max(0, consumptionTime);
    }

    public boolean isFood() {
        return material.isEdible();
    }

    public boolean isPotion() {
        return material == Material.POTION || material.name().endsWith("_POTION");
    }

    public boolean isDrink() {
        return isPotion() || material == Material.MILK_BUCKET || material == Material.HONEY_BOTTLE;
    }

    public boolean isAlwaysEdible() {
        return material == Material.GOLDEN_APPLE
                || material == Material.ENCHANTED_GOLDEN_APPLE
                || material == Material.CHORUS_FRUIT;
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
