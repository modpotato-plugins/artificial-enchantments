package io.artificial.enchantments.api.context;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Context for projectile-related enchantment effects.
 *
 * <p>Provides access to projectile, shooter, launch location, hit data,
 * and projectile physics during projectile launch and hit events.
 *
 * @since 0.1.0
 */
public interface ProjectileContext extends EffectContext {

    /**
     * Gets the projectile entity.
     *
     * @return the projectile
     */
    @NotNull
    Projectile getProjectile();

    /**
     * Gets the entity that shot the projectile.
     *
     * @return the shooter, or null if launched by dispenser or other source
     */
    @Nullable
    LivingEntity getShooter();

    /**
     * Gets the location where the projectile was launched.
     *
     * @return the launch location
     */
    @NotNull
    Location getLaunchLocation();

    /**
     * Gets the weapon used to launch the projectile.
     *
     * @return the weapon item
     */
    @NotNull
    ItemStack getWeapon();

    /**
     * Gets the entity hit by the projectile, if any.
     *
     * @return the hit entity, or null
     */
    @Nullable
    Entity getHitEntity();

    /**
     * Gets the block hit by the projectile, if any.
     *
     * @return the hit block, or null
     */
    @Nullable
    Block getHitBlock();

    /**
     * Gets the exact position where the projectile hit.
     *
     * @return the hit position, or null
     */
    @Nullable
    Vector getHitPosition();

    /**
     * Checks if the projectile hit an entity.
     *
     * @return true if hit entity
     */
    boolean hasHitEntity();

    /**
     * Checks if the projectile hit a block.
     *
     * @return true if hit block
     */
    boolean hasHitBlock();

    /**
     * Checks if this is a launch event.
     *
     * @return true if launching
     */
    boolean isLaunch();

    /**
     * Checks if this is a hit event.
     *
     * @return true if hitting
     */
    boolean isHit();

    /**
     * Sets the projectile velocity.
     *
     * @param velocity the new velocity vector
     */
    void setVelocity(@NotNull Vector velocity);

    /**
     * Gets the current projectile velocity.
     *
     * @return the velocity vector
     */
    @NotNull
    Vector getVelocity();

    /**
     * Sets whether gravity affects the projectile.
     *
     * @param gravity true for gravity
     */
    void setGravity(boolean gravity);

    /**
     * Checks if gravity affects the projectile.
     *
     * @return true if has gravity
     */
    boolean hasGravity();

    /**
     * Sets the pierce level for this projectile.
     *
     * @param level the pierce level
     */
    void setPierceLevel(int level);

    /**
     * Gets the current pierce level.
     *
     * @return the pierce level
     */
    int getPierceLevel();
}
