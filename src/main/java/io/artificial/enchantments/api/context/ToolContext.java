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

/**
 * Context for tool-related enchantment effects.
 *
 * <p>Provides access to player, tool, block, drops, and block action
 * data during block break, place, and interact events.
 *
 * @since 0.1.0
 */
public interface ToolContext extends EffectContext {

    /**
     * Gets the player using the tool.
     *
     * @return the player
     */
    @NotNull
    Player getPlayer();

    /**
     * Gets the tool item.
     *
     * @return the tool
     */
    @NotNull
    ItemStack getTool();

    /**
     * Gets the block being affected.
     *
     * @return the block
     */
    @NotNull
    Block getBlock();

    /**
     * Gets the location of the affected block.
     *
     * @return the block location
     */
    @NotNull
    Location getLocation();

    /**
     * Gets the face of the block being hit.
     *
     * @return the block face, or null
     */
    @Nullable
    BlockFace getBlockFace();

    /**
     * Checks if the block is being broken.
     *
     * @return true if breaking
     */
    boolean isBreak();

    /**
     * Checks if this is a pre-break event (before block drops).
     *
     * @return true if pre-break
     */
    boolean isPreBreak();

    /**
     * Checks if a block is being placed.
     *
     * @return true if placing
     */
    boolean isPlace();

    /**
     * Checks if this is a block interact event.
     *
     * @return true if interacting
     */
    boolean isInteract();

    /**
     * Gets the list of drops from breaking the block.
     *
     * @return the drops, or null
     */
    @Nullable
    List<ItemStack> getDrops();

    /**
     * Sets the drops from breaking the block.
     *
     * @param drops the new drops list
     */
    void setDrops(@Nullable List<ItemStack> drops);

    /**
     * Adds an item to the drops.
     *
     * @param item the item to add
     */
    void addDrop(@NotNull ItemStack item);

    /**
     * Removes an item from the drops.
     *
     * @param item the item to remove
     * @return true if removed
     */
    boolean removeDrop(@NotNull ItemStack item);

    /**
     * Gets the experience to drop from breaking the block.
     *
     * @return the experience amount
     */
    int getExpToDrop();

    /**
     * Sets the experience to drop.
     *
     * @param exp the new experience amount
     */
    void setExpToDrop(int exp);

    /**
     * Checks if items will be dropped.
     *
     * @return true if dropping items
     */
    boolean willDropItems();

    /**
     * Checks if the tool will take damage.
     *
     * @return true if tool will be damaged
     */
    boolean willDamageTool();
}
