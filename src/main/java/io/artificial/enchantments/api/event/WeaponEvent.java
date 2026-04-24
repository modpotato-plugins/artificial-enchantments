package io.artificial.enchantments.api.event;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when a weapon-related enchantment effect triggers.
 *
 * <p>Carries data about the player, weapon, target, and projectile during
 * bow, crossbow, or trident use events. This event is not cancellable.
 *
 * @see EnchantEffectEvent
 * @since 0.1.0
 */
public class WeaponEvent extends EnchantEffectEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack weapon;
    private final Location location;
    private final LivingEntity target;
    private final Projectile projectile;
    private final boolean isBow;
    private final boolean isCrossbow;
    private final boolean isTrident;
    private final boolean isShooting;
    private final boolean isThrowing;
    private float force;
    private boolean critical;
    private int pierceLevel;

    /**
     * Creates a new {@code WeaponEvent}.
     *
     * @param enchantment the enchantment that triggered this event
     * @param level the level of the enchantment
     * @param scaledValue the scaled value from the enchantment's scaling algorithm
     * @param player the player using the weapon
     * @param weapon the weapon item
     * @param location the location of the event
     * @param target the target entity, or null
     * @param projectile the projectile, or null
     * @param isBow true if this is a bow event
     * @param isCrossbow true if this is a crossbow event
     * @param isTrident true if this is a trident event
     * @param isShooting true if shooting
     * @param isThrowing true if throwing
     * @param force the force of the shot (0.0-1.0)
     * @param critical true if critical hit
     * @param pierceLevel the pierce level
     */
    public WeaponEvent(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Player player,
            @NotNull ItemStack weapon,
            @NotNull Location location,
            @Nullable LivingEntity target,
            @Nullable Projectile projectile,
            boolean isBow,
            boolean isCrossbow,
            boolean isTrident,
            boolean isShooting,
            boolean isThrowing,
            float force,
            boolean critical,
            int pierceLevel
    ) {
        super(enchantment, level, scaledValue);
        this.player = player;
        this.weapon = weapon;
        this.location = location;
        this.target = target;
        this.projectile = projectile;
        this.isBow = isBow;
        this.isCrossbow = isCrossbow;
        this.isTrident = isTrident;
        this.isShooting = isShooting;
        this.isThrowing = isThrowing;
        this.force = force;
        this.critical = critical;
        this.pierceLevel = pierceLevel;
    }

    /**
     * Gets the player using the weapon.
     *
     * @return the player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the weapon item.
     *
     * @return the weapon
     */
    @NotNull
    public ItemStack getWeapon() {
        return weapon;
    }

    /**
     * Gets the location of the event.
     *
     * @return the location
     */
    @NotNull
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the target entity.
     *
     * @return the target, or null
     */
    @Nullable
    public LivingEntity getTarget() {
        return target;
    }

    /**
     * Gets the projectile.
     *
     * @return the projectile, or null
     */
    @Nullable
    public Projectile getProjectile() {
        return projectile;
    }

    /**
     * Checks if this is a bow event.
     *
     * @return true if bow
     */
    public boolean isBow() {
        return isBow;
    }

    /**
     * Checks if this is a crossbow event.
     *
     * @return true if crossbow
     */
    public boolean isCrossbow() {
        return isCrossbow;
    }

    /**
     * Checks if this is a trident event.
     *
     * @return true if trident
     */
    public boolean isTrident() {
        return isTrident;
    }

    /**
     * Checks if this is a shooting event.
     *
     * @return true if shooting
     */
    public boolean isShooting() {
        return isShooting;
    }

    /**
     * Checks if this is a throwing event.
     *
     * @return true if throwing
     */
    public boolean isThrowing() {
        return isThrowing;
    }

    /**
     * Gets the force of the shot.
     *
     * @return the force (0.0-1.0)
     */
    public float getForce() {
        return force;
    }

    /**
     * Sets the force of the shot.
     *
     * @param force the new force (clamped to 0.0-1.0)
     */
    public void setForce(float force) {
        this.force = Math.clamp(force, 0.0f, 1.0f);
    }

    /**
     * Checks if this is a critical hit.
     *
     * @return true if critical
     */
    public boolean isCritical() {
        return critical;
    }

    /**
     * Sets whether this is a critical hit.
     *
     * @param critical true for critical hit
     */
    public void setCritical(boolean critical) {
        this.critical = critical;
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
