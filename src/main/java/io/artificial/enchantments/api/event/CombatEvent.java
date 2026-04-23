package io.artificial.enchantments.api.event;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when a combat-related enchantment effect triggers.
 *
 * <p>Carries data about the attacker, victim, weapon, and damage values
 * during entity damage events. This event is cancellable.
 *
 * @see EnchantEffectEvent
 * @since 0.1.0
 */
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

    /**
     * Creates a new {@code CombatEvent}.
     *
     * @param enchantment the enchantment that triggered this event
     * @param level the level of the enchantment
     * @param scaledValue the scaled value from the enchantment's scaling algorithm
     * @param attacker the attacking entity, or null for environmental damage
     * @param victim the victim receiving damage
     * @param weapon the weapon used, or null
     * @param baseDamage the base damage before modifiers
     * @param damageSource the Bukkit damage source
     * @param type the combat type
     */
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

    /**
     * Gets the attacking entity, if any.
     *
     * @return the attacker, or null for environmental damage
     */
    @Nullable
    public LivingEntity getAttacker() {
        return attacker;
    }

    /**
     * Gets the victim receiving damage.
     *
     * @return the victim entity
     */
    @NotNull
    public LivingEntity getVictim() {
        return victim;
    }

    /**
     * Gets the weapon used in the attack, if any.
     *
     * @return the weapon item, or null
     */
    @Nullable
    public ItemStack getWeapon() {
        return weapon;
    }

    /**
     * Gets the base damage before modifiers.
     *
     * @return the base damage value
     */
    public double getBaseDamage() {
        return baseDamage;
    }

    /**
     * Gets the final damage after modifications.
     *
     * @return the final damage value
     */
    public double getFinalDamage() {
        return finalDamage;
    }

    /**
     * Sets the final damage value.
     *
     * @param damage the new damage value (clamped to non-negative)
     */
    public void setFinalDamage(double damage) {
        this.finalDamage = Math.max(0, damage);
    }

    /**
     * Gets the Bukkit damage source.
     *
     * @return the damage source
     */
    @NotNull
    public DamageSource getDamageSource() {
        return damageSource;
    }

    /**
     * Gets the combat type.
     *
     * @return the combat type
     */
    @NotNull
    public CombatType getCombatType() {
        return type;
    }

    /**
     * Checks if this is an attack event.
     *
     * @return true if attack
     */
    public boolean isAttack() {
        return type == CombatType.ATTACK;
    }

    /**
     * Checks if this is a defense event.
     *
     * @return true if defense or shield block
     */
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

    /**
     * Gets the handler list for this event type.
     *
     * @return the handler list
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Represents the type of combat interaction.
     */
    public enum CombatType {
        /** Offensive attack. */
        ATTACK,
        /** Defensive damage reduction. */
        DEFENSE,
        /** Shield block interaction. */
        SHIELD_BLOCK
    }
}
