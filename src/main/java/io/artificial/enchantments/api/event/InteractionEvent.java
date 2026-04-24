package io.artificial.enchantments.api.event;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when an interaction-related enchantment effect triggers.
 *
 * <p>Carries data about the player, item, block, and entity during player
 * interaction events (left click, right click). This event is cancellable.
 *
 * @see EnchantEffectEvent
 * @since 0.1.0
 */
public class InteractionEvent extends EnchantEffectEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack item;
    private final EquipmentSlot hand;
    private final Location location;
    private final Block block;
    private final BlockFace blockFace;
    private final Entity targetEntity;
    private final boolean isLeftClick;
    private final boolean isRightClick;
    private final Vector interactionPoint;
    private boolean cancelled;

    /**
     * Creates a new {@code InteractionEvent}.
     *
     * @param enchantment the enchantment that triggered this event
     * @param level the level of the enchantment
     * @param scaledValue the scaled value from the enchantment's scaling algorithm
     * @param player the player interacting
     * @param item the item used, or null
     * @param hand the hand used
     * @param location the interaction location
     * @param block the block interacted with, or null
     * @param blockFace the block face clicked, or null
     * @param targetEntity the entity clicked, or null
     * @param isLeftClick true if left click
     * @param isRightClick true if right click
     * @param interactionPoint the precise interaction point, or null
     */
    public InteractionEvent(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Player player,
            @Nullable ItemStack item,
            @NotNull EquipmentSlot hand,
            @NotNull Location location,
            @Nullable Block block,
            @Nullable BlockFace blockFace,
            @Nullable Entity targetEntity,
            boolean isLeftClick,
            boolean isRightClick,
            @Nullable Vector interactionPoint
    ) {
        super(enchantment, level, scaledValue);
        this.player = player;
        this.item = item;
        this.hand = hand;
        this.location = location;
        this.block = block;
        this.blockFace = blockFace;
        this.targetEntity = targetEntity;
        this.isLeftClick = isLeftClick;
        this.isRightClick = isRightClick;
        this.interactionPoint = interactionPoint;
        this.cancelled = false;
    }

    /**
     * Gets the player interacting.
     *
     * @return the player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the item used.
     *
     * @return the item, or null
     */
    @Nullable
    public ItemStack getItem() {
        return item;
    }

    /**
     * Gets the hand used.
     *
     * @return the hand
     */
    @NotNull
    public EquipmentSlot getHand() {
        return hand;
    }

    /**
     * Gets the interaction location.
     *
     * @return the location
     */
    @NotNull
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the block interacted with.
     *
     * @return the block, or null
     */
    @Nullable
    public Block getBlock() {
        return block;
    }

    /**
     * Gets the block face clicked.
     *
     * @return the block face, or null
     */
    @Nullable
    public BlockFace getBlockFace() {
        return blockFace;
    }

    /**
     * Gets the entity clicked.
     *
     * @return the target entity, or null
     */
    @Nullable
    public Entity getTargetEntity() {
        return targetEntity;
    }

    /**
     * Checks if this is a left click.
     *
     * @return true if left click
     */
    public boolean isLeftClick() {
        return isLeftClick;
    }

    /**
     * Checks if this is a right click.
     *
     * @return true if right click
     */
    public boolean isRightClick() {
        return isRightClick;
    }

    /**
     * Checks if this is a block interaction.
     *
     * @return true if block was clicked
     */
    public boolean isBlockInteraction() {
        return block != null;
    }

    /**
     * Checks if this is an entity interaction.
     *
     * @return true if entity was clicked
     */
    public boolean isEntityInteraction() {
        return targetEntity != null;
    }

    /**
     * Checks if the player is sneaking.
     *
     * @return true if sneaking
     */
    public boolean isSneaking() {
        return player.isSneaking();
    }

    /**
     * Gets the precise interaction point.
     *
     * @return the interaction point, or null
     */
    @Nullable
    public Vector getInteractionPoint() {
        return interactionPoint;
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
}
