package io.artificial.enchantments.api.context;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CombatContext extends EffectContext {

    @Nullable
    LivingEntity getAttacker();

    @NotNull
    LivingEntity getVictim();

    @NotNull
    Location getLocation();

    @Nullable
    ItemStack getWeapon();

    double getBaseDamage();

    double getCurrentDamage();

    void setDamage(double damage);

    void addDamage(double amount);

    void multiplyDamage(double factor);

    @NotNull
    DamageSource getDamageSource();

    boolean isMelee();

    boolean isProjectile();

    boolean isMagic();

    boolean isExplosion();

    boolean isCritical();

    boolean isBlocking();

    boolean isShieldBlock();

    boolean isArmored();
}
