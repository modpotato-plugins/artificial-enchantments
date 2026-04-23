package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.scaling.LevelScaling;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Pure functions for damage calculation and modification.
 * 
 * <p>These helpers support both additive and multiplicative damage modifications,
 * with proper handling for damage caps and minimum thresholds. All functions
 * are pure (no side effects) and thread-safe.</p>
 * 
 * <p>Damage operations:</p>
 * <ul>
 *   <li><strong>Additive:</strong> Adds a flat amount to damage (e.g., +5 damage)</li>
 *   <li><strong>Multiplicative:</strong> Multiplies damage by a factor (e.g., 1.5x damage)</li>
 *   <li><strong>Scaling:</strong> Uses LevelScaling to calculate bonus damage</li>
 * </ul>
 */
public final class DamageModifierHelper {

    private DamageModifierHelper() {
        throw new AssertionError("Utility class");
    }

    /**
     * Calculates additive bonus damage using a scaling formula.
     * 
     * <p>Formula: {@code baseDamage + scaling.calculate(level)}</p>
     *
     * @param baseDamage the base damage amount
     * @param scaling the scaling formula for bonus damage
     * @param level the enchantment level
     * @return the modified damage amount
     */
    public static double calculateAdditiveDamage(
            double baseDamage,
            @NotNull LevelScaling scaling,
            int level) {
        double bonus = scaling.calculate(level);
        return baseDamage + bonus;
    }

    /**
     * Calculates multiplicative bonus damage using a scaling formula.
     * 
     * <p>Formula: {@code baseDamage * (1 + scaling.calculate(level))}</p>
     * 
     * <p>The scaling result is treated as a percentage (e.g., 0.5 = +50% damage)</p>
     *
     * @param baseDamage the base damage amount
     * @param scaling the scaling formula for damage multiplier
     * @param level the enchantment level
     * @return the modified damage amount
     */
    public static double calculateMultiplicativeDamage(
            double baseDamage,
            @NotNull LevelScaling scaling,
            int level) {
        double multiplier = 1.0 + scaling.calculate(level);
        return baseDamage * multiplier;
    }

    /**
     * Applies additive damage bonus with optional caps.
     * 
     * <p>The result is clamped between minDamage and maxDamage if specified.</p>
     *
     * @param baseDamage the base damage amount
     * @param bonus the flat bonus to add
     * @param minDamage minimum damage cap (0 for no minimum)
     * @param maxDamage maximum damage cap (0 for no maximum)
     * @return the modified damage, clamped to limits
     */
    public static double applyAdditiveBonus(
            double baseDamage,
            double bonus,
            double minDamage,
            double maxDamage) {
        double result = baseDamage + bonus;
        return clampDamage(result, minDamage, maxDamage);
    }

    /**
     * Applies multiplicative damage bonus with optional caps.
     * 
     * <p>The result is clamped between minDamage and maxDamage if specified.</p>
     *
     * @param baseDamage the base damage amount
     * @param multiplier the damage multiplier (e.g., 1.5 for +50%)
     * @param minDamage minimum damage cap (0 for no minimum)
     * @param maxDamage maximum damage cap (0 for no maximum)
     * @return the modified damage, clamped to limits
     */
    public static double applyMultiplicativeBonus(
            double baseDamage,
            double multiplier,
            double minDamage,
            double maxDamage) {
        double result = baseDamage * multiplier;
        return clampDamage(result, minDamage, maxDamage);
    }

    /**
     * Clamps damage to specified minimum and maximum bounds.
     * 
     * <p>A minDamage of 0 means no minimum.
     * A maxDamage of 0 means no maximum.</p>
     *
     * @param damage the damage to clamp
     * @param minDamage minimum bound (0 = no minimum)
     * @param maxDamage maximum bound (0 = no maximum)
     * @return the clamped damage value
     */
    public static double clampDamage(double damage, double minDamage, double maxDamage) {
        if (minDamage > 0 && damage < minDamage) {
            return minDamage;
        }
        if (maxDamage > 0 && damage > maxDamage) {
            return maxDamage;
        }
        return damage;
    }

    /**
     * Calculates final damage rounded to nearest integer.
     * 
     * <p>Minecraft damage is typically represented as whole numbers.</p>
     *
     * @param damage the calculated damage
     * @return the rounded damage amount
     */
    public static int toRoundedDamage(double damage) {
        return (int) Math.round(damage);
    }

    /**
     * Calculates final damage rounded down (floor).
     * 
     * <p>Useful for ensuring minimum damage thresholds.</p>
     *
     * @param damage the calculated damage
     * @return the floored damage amount
     */
    public static int toFlooredDamage(double damage) {
        return (int) Math.floor(damage);
    }

    /**
     * Calculates final damage rounded up (ceiling).
     * 
     * <p>Useful for ensuring at least 1 damage is dealt.</p>
     *
     * @param damage the calculated damage
     * @return the ceiled damage amount
     */
    public static int toCeiledDamage(double damage) {
        return (int) Math.ceil(damage);
    }

    /**
     * Calculates damage reduction percentage from armor/enchantments.
     * 
     * <p>Formula: {@code damage * (1 - reductionPercent / 100)}</p>
     *
     * @param damage the incoming damage
     * @param reductionPercent percentage of damage to reduce (e.g., 20 for 20%)
     * @return the reduced damage amount
     */
    public static double calculateDamageReduction(double damage, double reductionPercent) {
        double reduction = Math.max(0, Math.min(100, reductionPercent)) / 100.0;
        return damage * (1 - reduction);
    }

    /**
     * Calculates damage reduction using scaling formula.
     * 
     * <p>The scaling result is treated as reduction percentage.</p>
     *
     * @param damage the incoming damage
     * @param reductionScaling the scaling formula for reduction percentage
     * @param level the enchantment level
     * @return the reduced damage amount
     */
    public static double calculateScaledDamageReduction(
            double damage,
            @NotNull LevelScaling reductionScaling,
            int level) {
        double reductionPercent = reductionScaling.calculate(level);
        return calculateDamageReduction(damage, reductionPercent);
    }

    /**
     * Calculates damage absorption (like absorption hearts).
     * 
     * <p>Returns the amount of damage that would be absorbed.</p>
     *
     * @param damage the incoming damage
     * @param absorptionAmount available absorption points
     * @return damage after absorption
     */
    public static double calculateAbsorption(double damage, double absorptionAmount) {
        return Math.max(0, damage - absorptionAmount);
    }

    /**
     * Calculates armor penetration damage bonus.
     * 
     * <p>Ignores a percentage of the target's armor.</p>
     * 
     * <p>Note: This calculates theoretical damage increase, actual armor
     * penetration mechanics depend on Minecraft's damage calculation.</p>
     *
     * @param baseDamage the base damage
     * @param penetrationPercent percentage of armor to ignore (e.g., 25 for 25%)
     * @return damage with armor penetration bonus applied
     */
    public static double calculateArmorPenetration(
            double baseDamage,
            double penetrationPercent) {
        double penetration = Math.max(0, Math.min(100, penetrationPercent)) / 100.0;
        return baseDamage * (1 + penetration * 0.5);
    }

    /**
     * Determines if damage type is physical (can be reduced by armor).
     *
     * @param cause the damage cause
     * @return true if the damage type is physical
     */
    public static boolean isPhysicalDamage(@NotNull EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE -> true;
            default -> false;
        };
    }

    /**
     * Determines if damage type is magical (ignores armor).
     *
     * @param cause the damage cause
     * @return true if the damage type is magical
     */
    public static boolean isMagicalDamage(@NotNull EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case MAGIC, THORNS, WITHER, DRAGON_BREATH -> true;
            default -> false;
        };
    }

    /**
     * Determines if damage type is environmental (fire, lava, drowning, etc.).
     *
     * @param cause the damage cause
     * @return true if the damage type is environmental
     */
    public static boolean isEnvironmentalDamage(@NotNull EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case FIRE, LAVA, HOT_FLOOR, DROWNING, SUFFOCATION, STARVATION,
                 POISON, MELTING, DRYOUT, FREEZE -> true;
            default -> false;
        };
    }

    /**
     * Calculates life steal amount from damage dealt.
     * 
     * <p>Formula: {@code damageDealt * (lifeStealPercent / 100)}</p>
     *
     * @param damageDealt the damage dealt to the target
     * @param lifeStealPercent percentage of damage to convert to healing (e.g., 10 for 10%)
     * @return the amount of health to restore
     */
    public static double calculateLifeSteal(double damageDealt, double lifeStealPercent) {
        return damageDealt * (lifeStealPercent / 100.0);
    }

    /**
     * Calculates life steal using a scaling formula.
     *
     * @param damageDealt the damage dealt to the target
     * @param lifeStealScaling the scaling formula for life steal percentage
     * @param level the enchantment level
     * @return the amount of health to restore
     */
    public static double calculateScaledLifeSteal(
            double damageDealt,
            @NotNull LevelScaling lifeStealScaling,
            int level) {
        double lifeStealPercent = lifeStealScaling.calculate(level);
        return calculateLifeSteal(damageDealt, lifeStealPercent);
    }

    /**
     * Calculates recoil/thorns damage to attacker.
     * 
     * <p>Formula: {@code damageDealt * (recoilPercent / 100)}</p>
     *
     * @param damageDealt the damage dealt by the attacker
     * @param recoilPercent percentage of damage reflected (e.g., 15 for 15%)
     * @return the damage to reflect back to attacker
     */
    public static double calculateRecoil(double damageDealt, double recoilPercent) {
        return damageDealt * (recoilPercent / 100.0);
    }

    /**
     * Calculates critical hit damage.
     * 
     * <p>Formula: {@code baseDamage * criticalMultiplier}</p>
     *
     * @param baseDamage the base damage before critical
     * @param criticalMultiplier the critical hit multiplier (default MC is 1.5)
     * @return the critical damage amount
     */
    public static double calculateCriticalDamage(double baseDamage, double criticalMultiplier) {
        return baseDamage * criticalMultiplier;
    }

    /**
     * Calculates damage with all modifiers applied in correct order.
     * 
     * <p>Order of operations:
     * <ol>
     *   <li>Additive bonuses</li>
     *   <li>Multiplicative bonuses</li>
     *   <li>Critical multiplier</li>
     *   <li>Clamping</li>
     * </ol>
     *
     * @param baseDamage starting damage
     * @param additiveBonus flat damage bonus
     * @param multiplicativeMultiplier damage multiplier
     * @param isCritical whether this is a critical hit
     * @param criticalMultiplier critical hit multiplier
     * @param minDamage minimum damage cap (0 for none)
     * @param maxDamage maximum damage cap (0 for none)
     * @return final calculated damage
     */
    public static double calculateFinalDamage(
            double baseDamage,
            double additiveBonus,
            double multiplicativeMultiplier,
            boolean isCritical,
            double criticalMultiplier,
            double minDamage,
            double maxDamage) {
        
        double damage = baseDamage;
        
        damage += additiveBonus;
        damage *= multiplicativeMultiplier;
        
        if (isCritical) {
            damage = calculateCriticalDamage(damage, criticalMultiplier);
        }
        
        return clampDamage(damage, minDamage, maxDamage);
    }
}
