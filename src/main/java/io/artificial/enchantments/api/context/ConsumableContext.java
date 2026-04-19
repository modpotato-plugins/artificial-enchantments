package io.artificial.enchantments.api.context;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface ConsumableContext extends EffectContext {

    @NotNull
    Player getPlayer();

    @NotNull
    ItemStack getItem();

    @NotNull
    Location getLocation();

    int getFoodLevel();

    void setFoodLevel(int level);

    float getSaturation();

    void setSaturation(float saturation);

    double getHealthRestored();

    void setHealthRestored(double health);

    boolean isFood();

    boolean isPotion();

    boolean isDrink();

    boolean isAlwaysEdible();

    int getConsumptionTime();

    void setConsumptionTime(int ticks);
}
