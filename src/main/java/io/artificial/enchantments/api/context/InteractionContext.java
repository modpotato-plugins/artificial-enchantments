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

/**
 * Context for player interaction enchantment effects.
 *
 * <p>Provides access to player, item, hand, block/entity targets, and
 * interaction details during player interact events.
 *
 * @since 0.1.0
 */
public interface InteractionContext extends EffectContext {

    /**
     * Gets the player performing the interaction.
     *
     * @return the interacting player
     */
    @NotNull
    Player getPlayer();

    /**
     * Gets the item used for the interaction.
     *
     * @return the item
     */
    @NotNull
    ItemStack getItem();

    /**
     * Gets the hand used for the interaction.
     *
     * @return the equipment slot (hand)
     */
    @NotNull
    EquipmentSlot getHand();

    /**
     * Gets the interaction location.
     *
     * @return the location
     */
    @NotNull
    Location getLocation();

    /**
     * Checks if this is a block interaction.
     *
     * @return true if interacting with block
     */
    boolean isBlockInteraction();

    /**
     * Checks if this is an entity interaction.
     *
     * @return true if interacting with entity
     */
    boolean isEntityInteraction();

    /**
     * Gets the block being interacted with, if any.
     *
     * @return the block, or null
     */
    @Nullable
    Block getBlock();

    /**
     * Gets the face of the block being clicked.
     *
     * @return the block face, or null
     */
    @Nullable
    BlockFace getBlockFace();

    /**
     * Gets the entity being interacted with, if any.
     *
     * @return the target entity, or null
     */
    @Nullable
    Entity getTargetEntity();

    /**
     * Checks if this is a left-click interaction.
     *
     * @return true if left click
     */
    boolean isLeftClick();

    /**
     * Checks if this is a right-click interaction.
     *
     * @return true if right click
     */
    boolean isRightClick();

    /**
     * Checks if the player is sneaking during interaction.
     *
     * @return true if sneaking
     */
    boolean isSneaking();

    /**
     * Gets the exact interaction point on the block/entity.
     *
     * @return the interaction point, or null
     */
    @Nullable
    org.bukkit.util.Vector getInteractionPoint();
}
