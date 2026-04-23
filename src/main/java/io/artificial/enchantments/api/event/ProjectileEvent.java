package io.artificial.enchantments.api.event;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when an enchantment effect triggers during projectile launch or hit.
 */
public class ProjectileEvent extends EnchantEffectEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Projectile projectile;
    private final LivingEntity shooter;
    private final Location launchLocation;
    private final ItemStack weapon;
    private final Entity hitEntity;
    private final Block hitBlock;
    private final Vector hitPosition;
    private final boolean isLaunch;
    private boolean cancelled;

    private Vector velocity;
    private boolean gravity;
    private int pierceLevel;

    public ProjectileEvent(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Projectile projectile,
            @Nullable LivingEntity shooter,
            @NotNull Location launchLocation,
            @Nullable ItemStack weapon,
            @Nullable Entity hitEntity,
            @Nullable Block hitBlock,
            @Nullable Vector hitPosition,
            boolean isLaunch
    ) {
        super(enchantment, level, scaledValue);
        this.projectile = projectile;
        this.shooter = shooter;
        this.launchLocation = launchLocation;
        this.weapon = weapon;
        this.hitEntity = hitEntity;
        this.hitBlock = hitBlock;
        this.hitPosition = hitPosition;
        this.isLaunch = isLaunch;
        this.cancelled = false;
        this.velocity = projectile.getVelocity();
        this.gravity = projectile.hasGravity();
        this.pierceLevel = 0;
    }

    @NotNull
    public Projectile getProjectile() {
        return projectile;
    }

    @Nullable
    public LivingEntity getShooter() {
        return shooter;
    }

    @NotNull
    public Location getLaunchLocation() {
        return launchLocation;
    }

    @Nullable
    public ItemStack getWeapon() {
        return weapon;
    }

    @Nullable
    public Entity getHitEntity() {
        return hitEntity;
    }

    @Nullable
    public Block getHitBlock() {
        return hitBlock;
    }

    @Nullable
    public Vector getHitPosition() {
        return hitPosition;
    }

    public boolean isLaunch() {
        return isLaunch;
    }

    public boolean isHit() {
        return !isLaunch;
    }

    public boolean hasHitEntity() {
        return hitEntity != null;
    }

    public boolean hasHitBlock() {
        return hitBlock != null;
    }

    @NotNull
    public Vector getVelocity() {
        return velocity;
    }

    public void setVelocity(@NotNull Vector velocity) {
        this.velocity = velocity;
    }

    public boolean hasGravity() {
        return gravity;
    }

    public void setGravity(boolean gravity) {
        this.gravity = gravity;
    }

    public int getPierceLevel() {
        return pierceLevel;
    }

    public void setPierceLevel(int pierceLevel) {
        this.pierceLevel = Math.max(0, pierceLevel);
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
