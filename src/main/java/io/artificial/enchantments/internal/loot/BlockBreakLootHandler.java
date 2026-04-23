package io.artificial.enchantments.internal.loot;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.loot.LootContext;
import io.artificial.enchantments.api.loot.LootModifier;
import io.artificial.enchantments.api.loot.LootModifierRegistry;
import io.artificial.enchantments.internal.ItemEnchantmentService;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles block-break loot modification for enchanted tools.
 *
 * <p>This class processes loot modifications when a player breaks a block
 * with a tool containing enchantments that have registered loot modifiers.
 * It extracts vanilla drops, creates a LootContext, and invokes all
 * registered modifiers in sequence.
 *
 * <p><strong>Processing Order:</strong>
 * <ol>
 *   <li>Extract enchantments from the tool</li>
 *   <li>Get vanilla drops from the block</li>
 *   <li>Create LootContext with drops, tool, block, player</li>
 *   <li>For each enchantment with modifiers, invoke modifiers</li>
 *   <li>Apply modified drops back to the event</li>
 * </ol>
 *
 * <p><strong>Explicit Ownership:</strong>
 * Only enchantments with registered loot modifiers are processed.
 * Non-targeted loot (vanilla drops, unenchanted tools) remains untouched.
 *
 * @since 0.4.0
 */
public final class BlockBreakLootHandler {

    private static final Logger LOGGER = Logger.getLogger("ArtificialEnchantments");

    private final LootModifierRegistry modifierRegistry;
    private final ItemEnchantmentService enchantmentService;

    // Tracks the last processed exp value for retrieval after processing
    private int lastExpToDrop;

    /**
     * Creates a new handler with the required dependencies.
     *
     * @param modifierRegistry the registry for loot modifiers
     * @param enchantmentService the service for reading enchantments from items
     */
    public BlockBreakLootHandler(
            @NotNull LootModifierRegistry modifierRegistry,
            @NotNull ItemEnchantmentService enchantmentService
    ) {
        this.modifierRegistry = modifierRegistry;
        this.enchantmentService = enchantmentService;
    }

    /**
     * Processes loot modifications for a block break event.
     *
     * <p>This is the main entry point called when a block is broken. It handles
     * the entire modification pipeline from extracting enchantments to
     * applying modified drops.
     *
     * @param player the player breaking the block
     * @param tool the tool being used
     * @param block the block being broken
     * @param vanillaDrops the vanilla drops that would normally spawn
     * @param expToDrop the experience points to drop
     * @return the modified drops list, or original if no modifications applied
     */
    @NotNull
    public List<ItemStack> processBlockBreak(
            @NotNull Player player,
            @NotNull ItemStack tool,
            @NotNull Block block,
            @NotNull List<ItemStack> vanillaDrops,
            int expToDrop
    ) {
        Map<EnchantmentDefinition, Integer> enchantments = enchantmentService.getEnchantments(tool);

        if (enchantments.isEmpty()) {
            return vanillaDrops;
        }

        List<EnchantmentDefinition> enchantmentsWithModifiers = new ArrayList<>();
        for (EnchantmentDefinition enchantment : enchantments.keySet()) {
            if (modifierRegistry.hasModifier(enchantment)) {
                enchantmentsWithModifiers.add(enchantment);
            }
        }

        if (enchantmentsWithModifiers.isEmpty()) {
            return vanillaDrops;
        }

        List<ItemStack> modifiableDrops = new ArrayList<>();
        for (ItemStack drop : vanillaDrops) {
            modifiableDrops.add(drop.clone());
        }

        Location location = block.getLocation();

        try {
            for (EnchantmentDefinition enchantment : enchantmentsWithModifiers) {
                int level = enchantments.get(enchantment);
                double scaledValue = enchantment.calculateScaledValue(level);

                LootContextImpl context = new LootContextImpl(
                        tool,
                        block,
                        player,
                        location,
                        modifiableDrops,
                        level,
                        scaledValue,
                        expToDrop,
                        enchantmentService,
                        tool
                );

                List<LootModifier> modifiers = modifierRegistry.getModifiers(enchantment);
                for (LootModifier modifier : modifiers) {
                    try {
                        modifier.modify(context);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING,
                                "Error in loot modifier for " + enchantment.getKey() + ": " + e.getMessage(), e);
                    }
                }

                expToDrop = context.getExpToDrop();
            }

            this.lastExpToDrop = expToDrop;
            return Collections.unmodifiableList(modifiableDrops);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing loot modifications: " + e.getMessage(), e);
            this.lastExpToDrop = expToDrop;
            return vanillaDrops;
        }
    }

    /**
     * Gets the last processed exp to drop value.
     *
     * @return the XP amount from the last processing
     */
    public int getLastExpToDrop() {
        return lastExpToDrop;
    }

    /**
     * Internal implementation of LootContext.
     *
     * <p>This class holds all context data for a single block break event
     * and provides mutable access to the drops list.
     */
    private static final class LootContextImpl implements LootContext {
        private final ItemStack tool;
        private final Block block;
        private final Player player;
        private final Location location;
        private final List<ItemStack> drops;
        private final int level;
        private final double scaledValue;
        private final ItemEnchantmentService enchantmentService;
        private final ItemStack enchantedItem;
        private int expToDrop;

        LootContextImpl(
                ItemStack tool,
                Block block,
                Player player,
                Location location,
                List<ItemStack> drops,
                int level,
                double scaledValue,
                int expToDrop,
                ItemEnchantmentService enchantmentService,
                ItemStack enchantedItem
        ) {
            this.tool = tool;
            this.block = block;
            this.player = player;
            this.location = location;
            this.drops = drops;
            this.level = level;
            this.scaledValue = scaledValue;
            this.expToDrop = expToDrop;
            this.enchantmentService = enchantmentService;
            this.enchantedItem = enchantedItem;
        }

        @Override
        public @NotNull ItemStack getTool() {
            return tool.clone();
        }

        @Override
        public @NotNull Block getBlock() {
            return block;
        }

        @Override
        public @NotNull Player getPlayer() {
            return player;
        }

        @Override
        public @NotNull Location getLocation() {
            return location.clone();
        }

        @Override
        public @NotNull List<ItemStack> getDrops() {
            return drops;
        }

        @Override
        public void setDrops(@Nullable List<ItemStack> newDrops) {
            drops.clear();
            if (newDrops != null) {
                for (ItemStack drop : newDrops) {
                    drops.add(drop.clone());
                }
            }
        }

        @Override
        public void addDrop(@NotNull ItemStack drop) {
            drops.add(drop.clone());
        }

        @Override
        public boolean removeDrop(@NotNull ItemStack drop) {
            return drops.remove(drop);
        }

        @Override
        public void clearDrops() {
            drops.clear();
        }

        @Override
        public int getLevel() {
            return level;
        }

        @Override
        public double getScaledValue() {
            return scaledValue;
        }

        @Override
        public int getExpToDrop() {
            return expToDrop;
        }

        @Override
        public void setExpToDrop(int exp) {
            this.expToDrop = Math.max(0, exp);
        }

        @Override
        public boolean hasEnchantment(@NotNull String enchantmentKey) {
            Map<EnchantmentDefinition, Integer> enchantments = enchantmentService.getEnchantments(enchantedItem);
            for (EnchantmentDefinition enchantment : enchantments.keySet()) {
                if (enchantment.getKey().toString().equals(enchantmentKey)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int getEnchantmentLevel(@NotNull String enchantmentKey) {
            Map<EnchantmentDefinition, Integer> enchantments = enchantmentService.getEnchantments(enchantedItem);
            for (Map.Entry<EnchantmentDefinition, Integer> entry : enchantments.entrySet()) {
                if (entry.getKey().getKey().toString().equals(enchantmentKey)) {
                    return entry.getValue();
                }
            }
            return 0;
        }
    }
}
