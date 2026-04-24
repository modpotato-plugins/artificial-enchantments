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
 * Event fired when a projectile-related enchantment effect triggers.
 *
 * <p>Carries data about the projectile, shooter, launch location, and hit
 * information during projectile launch or hit events. This event is cancellable.
 *
 * @see EnchantEffectEvent
 * @since 0.1.0
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

    /**
     * Creates a new {@code ProjectileEvent}.
     *
     * @param enchantment the enchantment that triggered this event
     * @param level the level of the enchantment
     * @param scaledValue the scaled value from the enchantment's scaling algorithm
     * @param projectile the projectile entity
     * @param shooter the entity that shot the projectile, or null
     * @param launchLocation the location where the projectile was launched
     * @param weapon the weapon used, or null
     * @param hitEntity the entity hit, or null
     * @param hitBlock the block hit, or null
     * @param hitPosition the precise hit position, or null
     * @param isLaunch true if this is a launch event, false if hit
     */
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

    /**
     * Gets the projectile entity.
     *
     * @return the projectile
     */
    @NotNull
    public Projectile getProjectile() {
        return projectile;
    }

    /**
     * Gets the entity that shot the projectile.
     *
     * @return the shooter, or null
     */
    @Nullable
    public LivingEntity getShooter() {
        return shooter;
    }

    /**
     * Gets the location where the projectile was launched.
     *
     * @return the launch location
     */
    @NotNull
    public Location getLaunchLocation() {
        return launchLocation;
    }

    /**
     * Gets the weapon used to launch the projectile.
     *
     * @return the weapon, or null
     */
    @Nullable
    public ItemStack getWeapon() {
        return weapon;
    }

    /**
     * Gets the entity hit by the projectile.
     *
     * @return the hit entity, or null
     */
    @Nullable
    public Entity getHitEntity() {
        return hitEntity;
    }

    /**
     * Gets the block hit by the projectile.
     *
     * @return the hit block, or null
     */
    @Nullable
    public Block getHitBlock() {
        return hitBlock;
    }

    /**
     * Gets the precise hit position.
     *
     * @return the hit position vector, or null
     */
    @Nullable
    public Vector getHitPosition() {
        return hitPosition;
    }

    /**
     * Checks if this is a projectile launch event.
     *
     * @return true if launch
     */
    public boolean isLaunch() {
        return isLaunch;
    }

    /**
     * Checks if this is a projectile hit event.
     *
     * @return true if hit
     */
    public boolean isHit() {
        return !isLaunch;
    }

    /**
     * Checks if the projectile hit an entity.
     *
     * @return true if entity was hit
     */
    public boolean hasHitEntity() {
        return hitEntity != null;
    }

    /**
     * Checks if the projectile hit a block.
     *
     * @return true if block was hit
     */
    public boolean hasHitBlock() {
        return hitBlock != null;
    }

    /**
     * Gets the projectile velocity.
     *
     * @return the velocity vector
     */
    @NotNull
    public Vector getVelocity() {
        return velocity;
    }

    /**
     * Sets the projectile velocity.
     *
     * @param velocity the new velocity
     */
    public void setVelocity(@NotNull Vector velocity) {
        this.velocity = velocity;
    }

    /**
     * Checks if the projectile has gravity.
     *
     * @return true if gravity is enabled
     */
    public boolean hasGravity() {
        return gravity;
    }

    /**
     * Sets whether the projectile has gravity.
     *
     * @param gravity true to enable gravity
     */
    public void setGravity(boolean gravity) {
        this.gravity = gravity;
    }

    /**
     * Gets the pierce level.
     *
     * @return the pierce level
     */
    public int getPierceLevel() {
        return pierceLevel;
    }

    /**
     * Sets the pierce level.
     *
     * @param pierceLevel the new pierce level (clamped to non-negative)
     */
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
