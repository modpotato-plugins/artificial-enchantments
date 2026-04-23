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
 * Event fired when an enchantment effect triggers during weapon use (bow, crossbow, trident).
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

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public ItemStack getWeapon() {
        return weapon;
    }

    @NotNull
    public Location getLocation() {
        return location;
    }

    @Nullable
    public LivingEntity getTarget() {
        return target;
    }

    @Nullable
    public Projectile getProjectile() {
        return projectile;
    }

    public boolean isBow() {
        return isBow;
    }

    public boolean isCrossbow() {
        return isCrossbow;
    }

    public boolean isTrident() {
        return isTrident;
    }

    public boolean isShooting() {
        return isShooting;
    }

    public boolean isThrowing() {
        return isThrowing;
    }

    public float getForce() {
        return force;
    }

    public void setForce(float force) {
        this.force = Math.clamp(force, 0.0f, 1.0f);
    }

    public boolean isCritical() {
        return critical;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
    }

    public int getPierceLevel() {
        return pierceLevel;
    }

    public void setPierceLevel(int pierceLevel) {
        this.pierceLevel = Math.max(0, pierceLevel);
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
}
