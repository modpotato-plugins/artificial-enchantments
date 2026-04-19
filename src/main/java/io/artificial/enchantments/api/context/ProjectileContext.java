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

public interface ProjectileContext extends EffectContext {

    @NotNull
    Projectile getProjectile();

    @Nullable
    LivingEntity getShooter();

    @NotNull
    Location getLaunchLocation();

    @NotNull
    ItemStack getWeapon();

    @Nullable
    Entity getHitEntity();

    @Nullable
    Block getHitBlock();

    @Nullable
    Vector getHitPosition();

    boolean hasHitEntity();

    boolean hasHitBlock();

    boolean isLaunch();

    boolean isHit();

    void setVelocity(@NotNull Vector velocity);

    @NotNull
    Vector getVelocity();

    void setGravity(boolean gravity);

    boolean hasGravity();

    void setPierceLevel(int level);

    int getPierceLevel();
}
