package io.artificial.enchantments.api.context;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Context for combat-related enchantment effects.
 *
 * <p>Provides access to attacker, victim, damage values, and combat metadata
 * during entity damage events.
 *
 * @since 0.1.0
 */
public interface CombatContext extends EffectContext {

    /**
     * Gets the attacking entity, if any.
     *
     * @return the attacker, or null for environmental damage
     */
    @Nullable
    LivingEntity getAttacker();

    /**
     * Gets the victim receiving damage.
     *
     * @return the victim entity
     */
    @NotNull
    LivingEntity getVictim();

    /**
     * Gets the location where the damage occurred.
     *
     * @return the damage location
     */
    @NotNull
    Location getLocation();

    /**
     * Gets the weapon used in the attack, if any.
     *
     * @return the weapon item, or null if no weapon was used
     */
    @Nullable
    ItemStack getWeapon();

    /**
     * Gets the base damage before modifiers.
     *
     * @return the base damage value
     */
    double getBaseDamage();

    /**
     * Gets the current damage after modifications.
     *
     * @return the current damage value
     */
    double getCurrentDamage();

    /**
     * Sets the damage to a specific value.
     *
     * @param damage the new damage value
     */
    void setDamage(double damage);

    /**
     * Adds an amount to the current damage.
     *
     * @param amount the amount to add (may be negative)
     */
    void addDamage(double amount);

    /**
     * Multiplies the current damage by a factor.
     *
     * @param factor the multiplier to apply
     */
    void multiplyDamage(double factor);

    /**
     * Gets the Bukkit damage source.
     *
     * @return the damage source
     */
    @NotNull
    DamageSource getDamageSource();

    /**
     * Checks if the damage is from a melee attack.
     *
     * @return true if melee damage
     */
    boolean isMelee();

    /**
     * Checks if the damage is from a projectile.
     *
     * @return true if projectile damage
     */
    boolean isProjectile();

    /**
     * Checks if the damage is magical.
     *
     * @return true if magic damage
     */
    boolean isMagic();

    /**
     * Checks if the damage is from an explosion.
     *
     * @return true if explosion damage
     */
    boolean isExplosion();

    /**
     * Checks if the attack was a critical hit.
     *
     * @return true if critical
     */
    boolean isCritical();

    /**
     * Checks if the victim is blocking with a shield.
     *
     * @return true if blocking
     */
    boolean isBlocking();

    /**
     * Checks if the damage was fully blocked by a shield.
     *
     * @return true if shield blocked
     */
    boolean isShieldBlock();

    /**
     * Checks if the victim is wearing armor.
     *
     * @return true if armored
     */
    boolean isArmored();
}
