package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.scaling.LevelScaling;
import io.artificial.enchantments.internal.scaling.ScalingUtils;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DamageModifierHelper Tests")
class DamageModifierHelperTest {

    @Test
    @DisplayName("Additive damage calculation works correctly")
    void additiveDamageCalculationWorks() {
        LevelScaling scaling = ScalingUtils.linear(0, 2.0);
        
        double result = DamageModifierHelper.calculateAdditiveDamage(10.0, scaling, 3);
        
        assertEquals(14.0, result, 0.001);
    }

    @Test
    @DisplayName("Multiplicative damage calculation works correctly")
    void multiplicativeDamageCalculationWorks() {
        LevelScaling scaling = ScalingUtils.linear(0, 0.25);
        
        double result = DamageModifierHelper.calculateMultiplicativeDamage(10.0, scaling, 3);
        
        // Level 3: 0 + 2*0.25 = 0.5 multiplier
        // 10.0 * (1 + 0.5) = 15.0
        assertEquals(15.0, result, 0.001);
    }

    @ParameterizedTest
    @CsvSource({
        "10.0, 5.0, 0, 0, 15.0",
        "10.0, 5.0, 12.0, 0, 15.0",
        "10.0, 5.0, 0, 14.0, 14.0",
        "10.0, 5.0, 0, 10.0, 10.0"
    })
    @DisplayName("Apply additive bonus with caps works correctly")
    void applyAdditiveBonusWithCaps(double base, double bonus, double min, double max, double expected) {
        double result = DamageModifierHelper.applyAdditiveBonus(base, bonus, min, max);
        
        assertEquals(expected, result, 0.001);
    }

    @ParameterizedTest
    @CsvSource({
        "10.0, 1.5, 0, 0, 15.0",
        "10.0, 1.5, 12.0, 0, 15.0",
        "10.0, 1.5, 0, 14.0, 14.0"
    })
    @DisplayName("Apply multiplicative bonus with caps works correctly")
    void applyMultiplicativeBonusWithCaps(double base, double multiplier, double min, double max, double expected) {
        double result = DamageModifierHelper.applyMultiplicativeBonus(base, multiplier, min, max);
        
        assertEquals(expected, result, 0.001);
    }

    @ParameterizedTest
    @CsvSource({
        "10.0, 5.0, 15.0, 10.0",
        "10.0, 20.0, 15.0, 20.0",
        "10.0, 0.0, 5.0, 5.0",
        "10.0, 0.0, 15.0, 10.0"
    })
    @DisplayName("Clamp damage enforces bounds correctly")
    void clampDamageEnforcesBounds(double damage, double min, double max, double expected) {
        double result = DamageModifierHelper.clampDamage(damage, min, max);
        
        assertEquals(expected, result, 0.001);
    }

    @Test
    @DisplayName("To rounded damage rounds correctly")
    void toRoundedDamageRoundsCorrectly() {
        assertEquals(10, DamageModifierHelper.toRoundedDamage(10.0));
        assertEquals(10, DamageModifierHelper.toRoundedDamage(9.6));
        assertEquals(10, DamageModifierHelper.toRoundedDamage(10.4));
        assertEquals(11, DamageModifierHelper.toRoundedDamage(10.5));
    }

    @Test
    @DisplayName("To floored damage floors correctly")
    void toFlooredDamageFloorsCorrectly() {
        assertEquals(10, DamageModifierHelper.toFlooredDamage(10.0));
        assertEquals(9, DamageModifierHelper.toFlooredDamage(9.9));
        assertEquals(10, DamageModifierHelper.toFlooredDamage(10.1));
    }

    @Test
    @DisplayName("To ceiled damage ceils correctly")
    void toCeiledDamageCeilsCorrectly() {
        assertEquals(10, DamageModifierHelper.toCeiledDamage(10.0));
        assertEquals(10, DamageModifierHelper.toCeiledDamage(9.9));
        assertEquals(11, DamageModifierHelper.toCeiledDamage(10.1));
        assertEquals(1, DamageModifierHelper.toCeiledDamage(0.1));
    }

    @ParameterizedTest
    @CsvSource({
        "10.0, 0, 10.0",
        "10.0, 50, 5.0",
        "10.0, 100, 0.0",
        "10.0, 25, 7.5"
    })
    @DisplayName("Damage reduction calculates correctly")
    void damageReductionCalculatesCorrectly(double damage, double reductionPercent, double expected) {
        double result = DamageModifierHelper.calculateDamageReduction(damage, reductionPercent);
        
        assertEquals(expected, result, 0.001);
    }

    @Test
    @DisplayName("Damage reduction caps at 100%")
    void damageReductionCapsAt100() {
        double result = DamageModifierHelper.calculateDamageReduction(10.0, 150.0);
        
        assertEquals(0.0, result, 0.001);
    }

    @Test
    @DisplayName("Absorption calculation works correctly")
    void absorptionCalculationWorks() {
        assertEquals(5.0, DamageModifierHelper.calculateAbsorption(10.0, 5.0), 0.001);
        assertEquals(0.0, DamageModifierHelper.calculateAbsorption(5.0, 10.0), 0.001);
        assertEquals(0.0, DamageModifierHelper.calculateAbsorption(10.0, 10.0), 0.001);
    }

    @ParameterizedTest
    @CsvSource({
        "10.0, 0, 10.0",
        "10.0, 50, 12.5",
        "10.0, 100, 15.0"
    })
    @DisplayName("Armor penetration calculates correctly")
    void armorPenetrationCalculatesCorrectly(double base, double penPercent, double expected) {
        double result = DamageModifierHelper.calculateArmorPenetration(base, penPercent);
        
        assertEquals(expected, result, 0.001);
    }

    @Test
    @DisplayName("Is physical damage correctly identifies physical types")
    void isPhysicalDamageCorrectlyIdentifies() {
        assertTrue(DamageModifierHelper.isPhysicalDamage(EntityDamageEvent.DamageCause.ENTITY_ATTACK));
        assertTrue(DamageModifierHelper.isPhysicalDamage(EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK));
        assertTrue(DamageModifierHelper.isPhysicalDamage(EntityDamageEvent.DamageCause.PROJECTILE));
        
        assertFalse(DamageModifierHelper.isPhysicalDamage(EntityDamageEvent.DamageCause.MAGIC));
        assertFalse(DamageModifierHelper.isPhysicalDamage(EntityDamageEvent.DamageCause.FIRE));
    }

    @Test
    @DisplayName("Is magical damage correctly identifies magical types")
    void isMagicalDamageCorrectlyIdentifies() {
        assertTrue(DamageModifierHelper.isMagicalDamage(EntityDamageEvent.DamageCause.MAGIC));
        assertTrue(DamageModifierHelper.isMagicalDamage(EntityDamageEvent.DamageCause.THORNS));
        assertTrue(DamageModifierHelper.isMagicalDamage(EntityDamageEvent.DamageCause.WITHER));
        
        assertFalse(DamageModifierHelper.isMagicalDamage(EntityDamageEvent.DamageCause.ENTITY_ATTACK));
        assertFalse(DamageModifierHelper.isMagicalDamage(EntityDamageEvent.DamageCause.FIRE));
    }

    @Test
    @DisplayName("Is environmental damage correctly identifies environmental types")
    void isEnvironmentalDamageCorrectlyIdentifies() {
        assertTrue(DamageModifierHelper.isEnvironmentalDamage(EntityDamageEvent.DamageCause.FIRE));
        assertTrue(DamageModifierHelper.isEnvironmentalDamage(EntityDamageEvent.DamageCause.LAVA));
        assertTrue(DamageModifierHelper.isEnvironmentalDamage(EntityDamageEvent.DamageCause.DROWNING));
        assertTrue(DamageModifierHelper.isEnvironmentalDamage(EntityDamageEvent.DamageCause.STARVATION));
        
        assertFalse(DamageModifierHelper.isEnvironmentalDamage(EntityDamageEvent.DamageCause.ENTITY_ATTACK));
        assertFalse(DamageModifierHelper.isEnvironmentalDamage(EntityDamageEvent.DamageCause.MAGIC));
    }

    @ParameterizedTest
    @CsvSource({
        "10.0, 10, 1.0",
        "10.0, 25, 2.5",
        "10.0, 50, 5.0",
        "10.0, 100, 10.0"
    })
    @DisplayName("Life steal calculates correctly")
    void lifeStealCalculatesCorrectly(double damage, double percent, double expected) {
        double result = DamageModifierHelper.calculateLifeSteal(damage, percent);
        
        assertEquals(expected, result, 0.001);
    }

    @ParameterizedTest
    @CsvSource({
        "10.0, 10, 1.0",
        "10.0, 20, 2.0"
    })
    @DisplayName("Recoil calculates correctly")
    void recoilCalculatesCorrectly(double damage, double percent, double expected) {
        double result = DamageModifierHelper.calculateRecoil(damage, percent);
        
        assertEquals(expected, result, 0.001);
    }

    @Test
    @DisplayName("Critical damage calculates correctly")
    void criticalDamageCalculatesCorrectly() {
        assertEquals(15.0, DamageModifierHelper.calculateCriticalDamage(10.0, 1.5), 0.001);
        assertEquals(20.0, DamageModifierHelper.calculateCriticalDamage(10.0, 2.0), 0.001);
    }

    @Test
    @DisplayName("Final damage calculation applies modifiers in correct order")
    void finalDamageCalculationAppliesCorrectOrder() {
        double result = DamageModifierHelper.calculateFinalDamage(
            10.0,      // base
            5.0,       // additive
            1.5,       // multiplicative
            true,      // is critical
            2.0,       // critical multiplier
            0,         // no min
            0          // no max
        );
        
        // Order: (10 + 5) * 1.5 * 2 = 15 * 1.5 * 2 = 45
        assertEquals(45.0, result, 0.001);
    }

    @Test
    @DisplayName("Final damage calculation respects caps")
    void finalDamageCalculationRespectsCaps() {
        double result = DamageModifierHelper.calculateFinalDamage(
            10.0, 0, 3.0, false, 1.0, 0, 25.0
        );
        
        assertEquals(25.0, result, 0.001);
    }

    @Test
    @DisplayName("Life steal with scaling works correctly")
    void lifeStealWithScalingWorks() {
        LevelScaling scaling = ScalingUtils.linear(0, 5.0);
        
        double result = DamageModifierHelper.calculateScaledLifeSteal(20.0, scaling, 3);
        
        // Level 3: 0 + 2*5 = 10% life steal
        // 20.0 * 0.10 = 2.0
        assertEquals(2.0, result, 0.001);
    }

    @Test
    @DisplayName("Damage reduction with scaling works correctly")
    void damageReductionWithScalingWorks() {
        LevelScaling scaling = ScalingUtils.linear(0, 10.0);
        
        double result = DamageModifierHelper.calculateScaledDamageReduction(20.0, scaling, 3);
        
        // Level 3: 0 + 2*10 = 20% reduction
        // 20.0 * (1 - 0.20) = 16.0
        assertEquals(16.0, result, 0.001);
    }
}
