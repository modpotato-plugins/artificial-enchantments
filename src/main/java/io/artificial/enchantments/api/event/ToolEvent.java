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

/**
 * Event fired when a tool-related enchantment effect triggers.
 *
 * <p>Carries data about the player, tool, block, and drops during block break,
 * block place, or block interact events. This event is cancellable.
 *
 * @see EnchantEffectEvent
 * @since 0.1.0
 */
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

    /**
     * Creates a new {@code ToolEvent}.
     *
     * @param enchantment the enchantment that triggered this event
     * @param level the level of the enchantment
     * @param scaledValue the scaled value from the enchantment's scaling algorithm
     * @param player the player using the tool
     * @param tool the tool item
     * @param block the block being affected
     * @param face the block face being interacted with, or null
     * @param type the tool interaction type
     * @param drops the drops from the block, or null
     * @param expToDrop the experience to drop
     */
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

    /**
     * Gets the player using the tool.
     *
     * @return the player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the tool item.
     *
     * @return the tool
     */
    @NotNull
    public ItemStack getTool() {
        return tool;
    }

    /**
     * Gets the block being affected.
     *
     * @return the block
     */
    @NotNull
    public Block getBlock() {
        return block;
    }

    /**
     * Gets the block face being interacted with.
     *
     * @return the block face, or null
     */
    @Nullable
    public BlockFace getFace() {
        return face;
    }

    /**
     * Gets the tool interaction type.
     *
     * @return the tool type
     */
    @NotNull
    public ToolType getToolType() {
        return type;
    }

    /**
     * Gets the drops from the block.
     *
     * @return the drops, or null
     */
    @Nullable
    public List<ItemStack> getDrops() {
        return drops;
    }

    /**
     * Sets the drops from the block.
     *
     * @param drops the new drops, or null
     */
    public void setDrops(@Nullable List<ItemStack> drops) {
        if (this.drops != null) {
            this.drops.clear();
            if (drops != null) {
                this.drops.addAll(drops);
            }
        }
    }

    /**
     * Gets the experience to drop.
     *
     * @return the experience value
     */
    public int getExpToDrop() {
        return expToDrop;
    }

    /**
     * Sets the experience to drop.
     *
     * @param exp the new experience value (clamped to non-negative)
     */
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

    /**
     * Gets the handler list for this event type.
     *
     * @return the handler list
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Represents the type of tool interaction.
     */
    public enum ToolType {
        /** Block break action. */
        BLOCK_BREAK,
        /** Pre-block break action (before drops). */
        BLOCK_BREAK_PRE,
        /** Block place action. */
        BLOCK_PLACE,
        /** Block interaction action. */
        BLOCK_INTERACT
    }
}
