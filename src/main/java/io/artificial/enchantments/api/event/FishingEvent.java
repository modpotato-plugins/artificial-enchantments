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
 * Event fired when an enchantment effect triggers during fishing actions.
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

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public ItemStack getFishingRod() {
        return fishingRod;
    }

    @NotNull
    public FishHook getHook() {
        return hook;
    }

    @NotNull
    public Location getLocation() {
        return location;
    }

    @Nullable
    public Item getCaughtItem() {
        return caughtItem;
    }

    @Nullable
    public Entity getCaughtEntity() {
        return caughtEntity;
    }

    @NotNull
    public State getState() {
        return state;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = Math.max(0, waitTime);
    }

    public int getLureSpeed() {
        return lureSpeed;
    }

    public void setLureSpeed(int lureSpeed) {
        this.lureSpeed = Math.max(0, lureSpeed);
    }

    public boolean isApplyLure() {
        return applyLure;
    }

    public void setApplyLure(boolean applyLure) {
        this.applyLure = applyLure;
    }

    public boolean isCast() {
        return state == State.CAST;
    }

    public boolean isReel() {
        return state == State.REEL;
    }

    public boolean isBite() {
        return state == State.BITE;
    }

    public boolean hasCaughtItem() {
        return caughtItem != null;
    }

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

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public enum State {
        CAST,
        REEL,
        BITE,
        CAUGHT
    }
}
