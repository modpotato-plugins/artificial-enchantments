package io.artificial.enchantments.api.context;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Context for weapon-related enchantment effects.
 *
 * <p>Provides access to player, weapon, target, projectile, and force data
 * during ranged weapon usage events such as bow shots, crossbow shots, and
 * trident throws.
 *
 * @since 0.1.0
 */
public interface WeaponContext extends EffectContext {

    /**
     * Gets the player using the weapon.
     *
     * @return the player, or null if launched by non-player entity
     */
    @Nullable
    Player getPlayer();

    /**
     * Gets the weapon item being used.
     *
     * @return the weapon item
     */
    @NotNull
    ItemStack getWeapon();

    /**
     * Gets the location where the weapon was used.
     *
     * @return the launch location
     */
    @NotNull
    Location getLocation();

    /**
     * Gets the target entity, if any.
     *
     * @return the target entity, or null if no target
     */
    @Nullable
    LivingEntity getTarget();

    /**
     * Gets the projectile being launched.
     *
     * @return the projectile entity, or null if not yet launched
     */
    @Nullable
    Projectile getProjectile();

    /**
     * Checks if the weapon is a bow.
     *
     * @return true if bow
     */
    boolean isBow();

    /**
     * Checks if the weapon is a crossbow.
     *
     * @return true if crossbow
     */
    boolean isCrossbow();

    /**
     * Checks if the weapon is a trident.
     *
     * @return true if trident
     */
    boolean isTrident();

    /**
     * Checks if the weapon is currently shooting.
     *
     * @return true if shooting
     */
    boolean isShooting();

    /**
     * Checks if the weapon is being thrown.
     *
     * @return true if throwing
     */
    boolean isThrowing();

    /**
     * Gets the force/draw strength of the shot (0.0 to 1.0).
     *
     * @return the force value
     */
    float getForce();

    /**
     * Sets the force/draw strength of the shot.
     *
     * @param force the new force value
     */
    void setForce(float force);

    /**
     * Checks if this shot is a critical hit.
     *
     * @return true if critical
     */
    boolean isCritical();

    /**
     * Sets whether this shot is a critical hit.
     *
     * @param critical true to make critical
     */
    void setCritical(boolean critical);

    /**
     * Gets the pierce level for this projectile.
     *
     * @return the pierce level
     */
    int getPierceLevel();

    /**
     * Sets the pierce level for this projectile.
     *
     * @param level the new pierce level
     */
    void setPierceLevel(int level);
}
