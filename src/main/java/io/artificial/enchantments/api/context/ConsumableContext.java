package io.artificial.enchantments.api.context;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Context for consumable-related enchantment effects.
 *
 * <p>Provides access to player, item, food level, saturation, and health
 * restoration data during item consumption events.
 *
 * @since 0.1.0
 */
public interface ConsumableContext extends EffectContext {

    /**
     * Gets the player consuming the item.
     *
     * @return the consuming player
     */
    @NotNull
    Player getPlayer();

    /**
     * Gets the item being consumed.
     *
     * @return the consumed item
     */
    @NotNull
    ItemStack getItem();

    /**
     * Gets the location where consumption occurred.
     *
     * @return the consumption location
     */
    @NotNull
    Location getLocation();

    /**
     * Gets the food level change from this item.
     *
     * @return the food level
     */
    int getFoodLevel();

    /**
     * Sets the food level change.
     *
     * @param level the new food level
     */
    void setFoodLevel(int level);

    /**
     * Gets the saturation change from this item.
     *
     * @return the saturation value
     */
    float getSaturation();

    /**
     * Sets the saturation change.
     *
     * @param saturation the new saturation value
     */
    void setSaturation(float saturation);

    /**
     * Gets the health restored by this item.
     *
     * @return the health restored
     */
    double getHealthRestored();

    /**
     * Sets the health restored.
     *
     * @param health the new health restored value
     */
    void setHealthRestored(double health);

    /**
     * Checks if the consumed item is food.
     *
     * @return true if food
     */
    boolean isFood();

    /**
     * Checks if the consumed item is a potion.
     *
     * @return true if potion
     */
    boolean isPotion();

    /**
     * Checks if the consumed item is a drink.
     *
     * @return true if drink
     */
    boolean isDrink();

    /**
     * Checks if the item is edible even when not hungry.
     *
     * @return true if always edible
     */
    boolean isAlwaysEdible();

    /**
     * Gets the consumption time in ticks.
     *
     * @return the consumption time
     */
    int getConsumptionTime();

    /**
     * Sets the consumption time in ticks.
     *
     * @param ticks the new consumption time
     */
    void setConsumptionTime(int ticks);
}
