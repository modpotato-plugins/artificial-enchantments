package io.artificial.enchantments.api.context;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FishingContext extends EffectContext {

    @NotNull
    Player getPlayer();

    @NotNull
    ItemStack getFishingRod();

    @NotNull
    FishHook getHook();

    @NotNull
    Location getLocation();

    @Nullable
    Item getCaughtItem();

    @Nullable
    org.bukkit.entity.Entity getCaughtEntity();

    boolean isCast();

    boolean isReel();

    boolean isBite();

    boolean hasCaughtItem();

    boolean hasCaughtEntity();

    void setWaitTime(int ticks);

    int getWaitTime();

    void setLureSpeed(int speed);

    int getLureSpeed();

    void setApplyLure(boolean apply);

    boolean isApplyLure();
}
