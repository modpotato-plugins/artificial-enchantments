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

    /**
     * Creates a new {@code ConsumableEvent}.
     *
     * @param enchantment the enchantment that triggered this event
     * @param level the level of the enchantment
     * @param scaledValue the scaled value from the enchantment's scaling algorithm
     * @param player the player consuming the item
     * @param item the item being consumed
     * @param material the material of the consumed item
     * @param foodLevel the food level change
     * @param saturation the saturation change
     * @param healthRestored the health restored
     * @param consumptionTime the consumption time in ticks
     */
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

    /**
     * Gets the player consuming the item.
     *
     * @return the consuming player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the item being consumed.
     *
     * @return the consumed item
     */
    @NotNull
    public ItemStack getItem() {
        return item;
    }

    /**
     * Gets the material of the consumed item.
     *
     * @return the material
     */
    @NotNull
    public Material getMaterial() {
        return material;
    }

    /**
     * Gets the food level change.
     *
     * @return the food level
     */
    public int getFoodLevel() {
        return foodLevel;
    }

    /**
     * Sets the food level change.
     *
     * @param foodLevel the new food level (clamped to 0-20)
     */
    public void setFoodLevel(int foodLevel) {
        this.foodLevel = Math.clamp(foodLevel, 0, 20);
    }

    /**
     * Gets the saturation change.
     *
     * @return the saturation value
     */
    public float getSaturation() {
        return saturation;
    }

    /**
     * Sets the saturation change.
     *
     * @param saturation the new saturation (clamped to 0-foodLevel)
     */
    public void setSaturation(float saturation) {
        this.saturation = Math.clamp(saturation, 0.0f, this.foodLevel);
    }

    /**
     * Gets the health restored.
     *
     * @return the health restored
     */
    public double getHealthRestored() {
        return healthRestored;
    }

    /**
     * Sets the health restored.
     *
     * @param healthRestored the new health restored (clamped to non-negative)
     */
    public void setHealthRestored(double healthRestored) {
        this.healthRestored = Math.max(0.0, healthRestored);
    }

    /**
     * Gets the consumption time in ticks.
     *
     * @return the consumption time
     */
    public int getConsumptionTime() {
        return consumptionTime;
    }

    /**
     * Sets the consumption time in ticks.
     *
     * @param consumptionTime the new consumption time (clamped to non-negative)
     */
    public void setConsumptionTime(int consumptionTime) {
        this.consumptionTime = Math.max(0, consumptionTime);
    }

    /**
     * Checks if the consumed item is food.
     *
     * @return true if food
     */
    public boolean isFood() {
        return material.isEdible();
    }

    /**
     * Checks if the consumed item is a potion.
     *
     * @return true if potion
     */
    public boolean isPotion() {
        return material == Material.POTION || material.name().endsWith("_POTION");
    }

    /**
     * Checks if the consumed item is a drink.
     *
     * @return true if drink
     */
    public boolean isDrink() {
        return isPotion() || material == Material.MILK_BUCKET || material == Material.HONEY_BOTTLE;
    }

    /**
     * Checks if the item is edible even when not hungry.
     *
     * @return true if always edible
     */
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
