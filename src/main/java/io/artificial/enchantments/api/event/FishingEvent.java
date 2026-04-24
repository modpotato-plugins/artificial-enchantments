package io.artificial.enchantments.api.event;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when a fishing-related enchantment effect triggers.
 *
 * <p>Carries data about the player, fishing rod, hook, and catch state during
 * fishing actions (cast, reel, bite, caught). This event is cancellable.
 *
 * @see EnchantEffectEvent
 * @since 0.1.0
 */
public class FishingEvent extends EnchantEffectEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack fishingRod;
    private final FishHook hook;
    private final Location location;
    private final Item caughtItem;
    private final Entity caughtEntity;
    private final State state;
    private int waitTime;
    private int lureSpeed;
    private boolean applyLure;
    private boolean cancelled;

    /**
     * Creates a new {@code FishingEvent}.
     *
     * @param enchantment the enchantment that triggered this event
     * @param level the level of the enchantment
     * @param scaledValue the scaled value from the enchantment's scaling algorithm
     * @param player the player fishing
     * @param fishingRod the fishing rod item
     * @param hook the fish hook entity
     * @param location the location
     * @param caughtItem the caught item, or null
     * @param caughtEntity the caught entity, or null
     * @param state the fishing state
     * @param waitTime the wait time in ticks
     * @param lureSpeed the lure speed
     * @param applyLure whether to apply lure
     */
    public FishingEvent(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Player player,
            @NotNull ItemStack fishingRod,
            @NotNull FishHook hook,
            @NotNull Location location,
            @Nullable Item caughtItem,
            @Nullable Entity caughtEntity,
            @NotNull State state,
            int waitTime,
            int lureSpeed,
            boolean applyLure
    ) {
        super(enchantment, level, scaledValue);
        this.player = player;
        this.fishingRod = fishingRod;
        this.hook = hook;
        this.location = location;
        this.caughtItem = caughtItem;
        this.caughtEntity = caughtEntity;
        this.state = state;
        this.waitTime = waitTime;
        this.lureSpeed = lureSpeed;
        this.applyLure = applyLure;
        this.cancelled = false;
    }

    /**
     * Gets the player fishing.
     *
     * @return the player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the fishing rod item.
     *
     * @return the fishing rod
     */
    @NotNull
    public ItemStack getFishingRod() {
        return fishingRod;
    }

    /**
     * Gets the fish hook entity.
     *
     * @return the hook
     */
    @NotNull
    public FishHook getHook() {
        return hook;
    }

    /**
     * Gets the location.
     *
     * @return the location
     */
    @NotNull
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the caught item.
     *
     * @return the caught item, or null
     */
    @Nullable
    public Item getCaughtItem() {
        return caughtItem;
    }

    /**
     * Gets the caught entity.
     *
     * @return the caught entity, or null
     */
    @Nullable
    public Entity getCaughtEntity() {
        return caughtEntity;
    }

    /**
     * Gets the fishing state.
     *
     * @return the state
     */
    @NotNull
    public State getState() {
        return state;
    }

    /**
     * Gets the wait time in ticks.
     *
     * @return the wait time
     */
    public int getWaitTime() {
        return waitTime;
    }

    /**
     * Sets the wait time in ticks.
     *
     * @param waitTime the new wait time (clamped to non-negative)
     */
    public void setWaitTime(int waitTime) {
        this.waitTime = Math.max(0, waitTime);
    }

    /**
     * Gets the lure speed.
     *
     * @return the lure speed
     */
    public int getLureSpeed() {
        return lureSpeed;
    }

    /**
     * Sets the lure speed.
     *
     * @param lureSpeed the new lure speed (clamped to non-negative)
     */
    public void setLureSpeed(int lureSpeed) {
        this.lureSpeed = Math.max(0, lureSpeed);
    }

    /**
     * Checks if lure should be applied.
     *
     * @return true if applying lure
     */
    public boolean isApplyLure() {
        return applyLure;
    }

    /**
     * Sets whether lure should be applied.
     *
     * @param applyLure true to apply lure
     */
    public void setApplyLure(boolean applyLure) {
        this.applyLure = applyLure;
    }

    /**
     * Checks if this is a cast event.
     *
     * @return true if cast
     */
    public boolean isCast() {
        return state == State.CAST;
    }

    /**
     * Checks if this is a reel event.
     *
     * @return true if reel
     */
    public boolean isReel() {
        return state == State.REEL;
    }

    /**
     * Checks if this is a bite event.
     *
     * @return true if bite
     */
    public boolean isBite() {
        return state == State.BITE;
    }

    /**
     * Checks if an item was caught.
     *
     * @return true if caught item exists
     */
    public boolean hasCaughtItem() {
        return caughtItem != null;
    }

    /**
     * Checks if an entity was caught.
     *
     * @return true if caught entity exists
     */
    public boolean hasCaughtEntity() {
        return caughtEntity != null;
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

    /**
     * Represents the state of a fishing action.
     */
    public enum State {
        /** Casting the line. */
        CAST,
        /** Reeling in the line. */
        REEL,
        /** Fish biting. */
        BITE,
        /** Caught something. */
        CAUGHT
    }
}
