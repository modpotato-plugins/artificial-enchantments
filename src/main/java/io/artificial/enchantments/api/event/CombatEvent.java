package io.artificial.enchantments.api.event;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CombatEvent extends EnchantEffectEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity attacker;
    private final LivingEntity victim;
    private final ItemStack weapon;
    private final double baseDamage;
    private double finalDamage;
    private final DamageSource damageSource;
    private final CombatType type;
    private boolean cancelled;

    public CombatEvent(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @Nullable LivingEntity attacker,
            @NotNull LivingEntity victim,
            @Nullable ItemStack weapon,
            double baseDamage,
            @NotNull DamageSource damageSource,
            @NotNull CombatType type
    ) {
        super(enchantment, level, scaledValue);
        this.attacker = attacker;
        this.victim = victim;
        this.weapon = weapon;
        this.baseDamage = baseDamage;
        this.finalDamage = baseDamage;
        this.damageSource = damageSource;
        this.type = type;
        this.cancelled = false;
    }

    @Nullable
    public LivingEntity getAttacker() {
        return attacker;
    }

    @NotNull
    public LivingEntity getVictim() {
        return victim;
    }

    @Nullable
    public ItemStack getWeapon() {
        return weapon;
    }

    public double getBaseDamage() {
        return baseDamage;
    }

    public double getFinalDamage() {
        return finalDamage;
    }

    public void setFinalDamage(double damage) {
        this.finalDamage = Math.max(0, damage);
    }

    @NotNull
    public DamageSource getDamageSource() {
        return damageSource;
    }

    @NotNull
    public CombatType getCombatType() {
        return type;
    }

    public boolean isAttack() {
        return type == CombatType.ATTACK;
    }

    public boolean isDefense() {
        return type == CombatType.DEFENSE || type == CombatType.SHIELD_BLOCK;
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

    public enum CombatType {
        ATTACK,
        DEFENSE,
        SHIELD_BLOCK
    }
}
