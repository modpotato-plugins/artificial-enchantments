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
 * Event fired when an enchantment effect triggers during player interaction.
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

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @Nullable
    public ItemStack getItem() {
        return item;
    }

    @NotNull
    public EquipmentSlot getHand() {
        return hand;
    }

    @NotNull
    public Location getLocation() {
        return location;
    }

    @Nullable
    public Block getBlock() {
        return block;
    }

    @Nullable
    public BlockFace getBlockFace() {
        return blockFace;
    }

    @Nullable
    public Entity getTargetEntity() {
        return targetEntity;
    }

    public boolean isLeftClick() {
        return isLeftClick;
    }

    public boolean isRightClick() {
        return isRightClick;
    }

    public boolean isBlockInteraction() {
        return block != null;
    }

    public boolean isEntityInteraction() {
        return targetEntity != null;
    }

    public boolean isSneaking() {
        return player.isSneaking();
    }

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

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
