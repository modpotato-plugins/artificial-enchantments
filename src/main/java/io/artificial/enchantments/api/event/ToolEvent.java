package io.artificial.enchantments.api.event;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ToolEvent extends EnchantEffectEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack tool;
    private final Block block;
    private final BlockFace face;
    private final ToolType type;
    private final List<ItemStack> drops;
    private int expToDrop;
    private boolean cancelled;

    public ToolEvent(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Player player,
            @NotNull ItemStack tool,
            @NotNull Block block,
            @Nullable BlockFace face,
            @NotNull ToolType type,
            @Nullable List<ItemStack> drops,
            int expToDrop
    ) {
        super(enchantment, level, scaledValue);
        this.player = player;
        this.tool = tool;
        this.block = block;
        this.face = face;
        this.type = type;
        this.drops = drops;
        this.expToDrop = expToDrop;
        this.cancelled = false;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public ItemStack getTool() {
        return tool;
    }

    @NotNull
    public Block getBlock() {
        return block;
    }

    @Nullable
    public BlockFace getFace() {
        return face;
    }

    @NotNull
    public ToolType getToolType() {
        return type;
    }

    @Nullable
    public List<ItemStack> getDrops() {
        return drops;
    }

    public void setDrops(@Nullable List<ItemStack> drops) {
        if (this.drops != null) {
            this.drops.clear();
            if (drops != null) {
                this.drops.addAll(drops);
            }
        }
    }

    public int getExpToDrop() {
        return expToDrop;
    }

    public void setExpToDrop(int exp) {
        this.expToDrop = Math.max(0, exp);
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

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public enum ToolType {
        BLOCK_BREAK,
        BLOCK_BREAK_PRE,
        BLOCK_PLACE,
        BLOCK_INTERACT
    }
}
