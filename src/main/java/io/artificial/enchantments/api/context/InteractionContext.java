package io.artificial.enchantments.api.context;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface InteractionContext extends EffectContext {

    @NotNull
    Player getPlayer();

    @NotNull
    ItemStack getItem();

    @NotNull
    EquipmentSlot getHand();

    @NotNull
    Location getLocation();

    boolean isBlockInteraction();

    boolean isEntityInteraction();

    @Nullable
    Block getBlock();

    @Nullable
    BlockFace getBlockFace();

    @Nullable
    Entity getTargetEntity();

    boolean isLeftClick();

    boolean isRightClick();

    boolean isSneaking();

    @Nullable
    org.bukkit.util.Vector getInteractionPoint();
}
