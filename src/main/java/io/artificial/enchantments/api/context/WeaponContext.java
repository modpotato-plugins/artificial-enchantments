package io.artificial.enchantments.api.context;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface WeaponContext extends EffectContext {

    @Nullable
    Player getPlayer();

    @NotNull
    ItemStack getWeapon();

    @NotNull
    Location getLocation();

    @Nullable
    LivingEntity getTarget();

    @Nullable
    Projectile getProjectile();

    boolean isBow();

    boolean isCrossbow();

    boolean isTrident();

    boolean isShooting();

    boolean isThrowing();

    float getForce();

    void setForce(float force);

    boolean isCritical();

    void setCritical(boolean critical);

    int getPierceLevel();

    void setPierceLevel(int level);
}
