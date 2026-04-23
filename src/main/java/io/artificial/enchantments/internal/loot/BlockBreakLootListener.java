package io.artificial.enchantments.internal.loot;

import io.artificial.enchantments.internal.ItemEnchantmentService;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener for block break events to apply loot modifiers.
 *
 * <p>This listener intercepts block break events and applies registered
 * loot modifiers from enchanted tools. It runs at HIGHEST priority to ensure
it modifies the drops list before vanilla drop processing occurs.
 *
 * <p><strong>Processing Flow:</strong>
 * <ol>
 *   <li>Player breaks a block</li>
 *   <li>Extract tool from player's main hand</li>
 *   <li>Get vanilla drops from block</li>
 *   <li>Check if tool has enchantments with loot modifiers</li>
 *   <li>Invoke modifiers in sequence</li>
 *   <li>Clear vanilla drops and spawn modified drops</li>
 * </ol>
 *
 * <p><strong>Explicit Ownership:</strong>
 * Only enchantments with registered loot modifiers affect drops.
 * Non-targeted loot (vanilla drops, unenchanted tools) remains untouched.
 *
 * @since 0.4.0
 */
public final class BlockBreakLootListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger("ArtificialEnchantments");

    private final Plugin plugin;
    private final BlockBreakLootHandler lootHandler;

    /**
     * Creates a new listener with the required dependencies.
     *
     * @param plugin the owning plugin
     * @param lootHandler the handler for processing loot modifications
     */
    public BlockBreakLootListener(
            @NotNull Plugin plugin,
            @NotNull BlockBreakLootHandler lootHandler
    ) {
        this.plugin = plugin;
        this.lootHandler = lootHandler;
    }

    /**
     * Handles block break events to apply loot modifiers.
     *
     * <p>Runs at HIGHEST priority to modify drops before vanilla processing.
     * Only creative mode players are skipped (no drops in creative).
     *
     * @param event the block break event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        Block block = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (tool.getType().isAir()) {
            return;
        }

        List<ItemStack> vanillaDrops = getVanillaDrops(block, tool);
        int expToDrop = event.getExpToDrop();

        try {
            List<ItemStack> modifiedDrops = lootHandler.processBlockBreak(
                    player,
                    tool,
                    block,
                    vanillaDrops,
                    expToDrop
            );

            if (modifiedDrops != vanillaDrops) {
                event.setDropItems(false);

                for (ItemStack drop : modifiedDrops) {
                    if (drop != null && !drop.getType().isAir() && drop.getAmount() > 0) {
                        block.getWorld().dropItemNaturally(block.getLocation(), drop);
                    }
                }

                int modifiedExp = lootHandler.getLastExpToDrop();
                if (modifiedExp != expToDrop) {
                    event.setExpToDrop(modifiedExp);
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "Error processing loot modification at " + block.getLocation() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Gets vanilla drops for a block given the tool used.
     *
     * <p>Respects silk touch and fortune enchantments on the tool.
     * This is a best-effort approximation of vanilla drop logic.
     *
     * @param block the block being broken
     * @param tool the tool being used
     * @return list of vanilla drops
     */
    @NotNull
    private List<ItemStack> getVanillaDrops(@NotNull Block block, @NotNull ItemStack tool) {
        List<ItemStack> drops = new ArrayList<>();

        if (tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            if (block.getType().isBlock()) {
                drops.add(new ItemStack(block.getType()));
            }
        } else {
            drops.addAll(block.getDrops(tool));
        }

        return drops;
    }
}
