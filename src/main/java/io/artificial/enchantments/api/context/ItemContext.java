package io.artificial.enchantments.api.context;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ItemContext extends EffectContext {

    @Nullable
    Player getPlayer();

    @NotNull
    ItemStack getItem();

    @Nullable
    EquipmentSlot getSlot();

    @NotNull
    Location getLocation();

    int getCurrentDurability();

    int getMaxDurability();

    int getDamageTaken();

    void setDamageTaken(int damage);

    void reduceDamage(int reduction);

    boolean willBreak();

    boolean isDrop();

    boolean isPickup();

    boolean isCraftingIngredient();

    boolean isAnvilCombination();
}
