package io.artificial.enchantments.api.context;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface TickContext extends EffectContext {

    @NotNull
    Player getPlayer();

    @NotNull
    ItemStack getItem();

    @NotNull
    EquipmentSlot getSlot();

    @NotNull
    Location getLocation();

    boolean isHeld();

    boolean isArmor();

    int getTickCount();

    long getHeldDuration();
}
