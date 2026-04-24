package io.artificial.enchantments.api.context;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Context for fishing-related enchantment effects.
 *
 * <p>Provides access to player, fishing rod, fish hook, caught items/entities,
 * and fishing mechanics data during fishing events.
 *
 * @since 0.1.0
 */
public interface FishingContext extends EffectContext {

    /**
     * Gets the player fishing.
     *
     * @return the fishing player
     */
    @NotNull
    Player getPlayer();

    /**
     * Gets the fishing rod item.
     *
     * @return the fishing rod
     */
    @NotNull
    ItemStack getFishingRod();

    /**
     * Gets the fish hook entity.
     *
     * @return the fish hook
     */
    @NotNull
    FishHook getHook();

    /**
     * Gets the location of the hook.
     *
     * @return the hook location
     */
    @NotNull
    Location getLocation();

    /**
     * Gets the caught item, if any.
     *
     * @return the caught item, or null
     */
    @Nullable
    Item getCaughtItem();

    /**
     * Gets the caught entity, if any.
     *
     * @return the caught entity, or null
     */
    @Nullable
    org.bukkit.entity.Entity getCaughtEntity();

    /**
     * Checks if the player is casting the line.
     *
     * @return true if casting
     */
    boolean isCast();

    /**
     * Checks if the player is reeling in.
     *
     * @return true if reeling
     */
    boolean isReel();

    /**
     * Checks if a fish has bitten.
     *
     * @return true if fish biting
     */
    boolean isBite();

    /**
     * Checks if an item has been caught.
     *
     * @return true if caught item
     */
    boolean hasCaughtItem();

    /**
     * Checks if an entity has been caught.
     *
     * @return true if caught entity
     */
    boolean hasCaughtEntity();

    /**
     * Sets the wait time for fish bite in ticks.
     *
     * @param ticks the wait time
     */
    void setWaitTime(int ticks);

    /**
     * Gets the current wait time.
     *
     * @return the wait time in ticks
     */
    int getWaitTime();

    /**
     * Sets the lure speed modifier.
     *
     * @param speed the lure speed
     */
    void setLureSpeed(int speed);

    /**
     * Gets the current lure speed.
     *
     * @return the lure speed
     */
    int getLureSpeed();

    /**
     * Sets whether lure enchantment effects apply.
     *
     * @param apply true to apply lure
     */
    void setApplyLure(boolean apply);

    /**
     * Checks if lure effects are being applied.
     *
     * @return true if lure applies
     */
    boolean isApplyLure();
}
