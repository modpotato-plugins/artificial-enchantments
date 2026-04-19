package io.artificial.enchantments.api.context;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ToolContext extends EffectContext {

    @NotNull
    Player getPlayer();

    @NotNull
    ItemStack getTool();

    @NotNull
    Block getBlock();

    @NotNull
    Location getLocation();

    @Nullable
    BlockFace getBlockFace();

    boolean isBreak();

    boolean isPreBreak();

    boolean isPlace();

    boolean isInteract();

    @Nullable
    List<ItemStack> getDrops();

    void setDrops(@Nullable List<ItemStack> drops);

    void addDrop(@NotNull ItemStack item);

    boolean removeDrop(@NotNull ItemStack item);

    int getExpToDrop();

    void setExpToDrop(int exp);

    boolean willDropItems();

    boolean willDamageTool();
}
