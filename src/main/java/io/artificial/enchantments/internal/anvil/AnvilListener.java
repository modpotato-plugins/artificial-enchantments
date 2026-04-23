package io.artificial.enchantments.internal.anvil;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.ItemStorage;
import io.artificial.enchantments.internal.EnchantmentRegistryManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Listener for anvil events to enable custom enchantment support.
 * <p>
 * Hooks into PrepareAnvilEvent to calculate and display anvil results
 * for custom enchantments. Handles book+item, book+book, and item+item
 * combinations.
 *
 * @since 0.2.0
 */
public final class AnvilListener implements Listener {

    private static final NamespacedKey PRIOR_WORK_KEY;
    private static final NamespacedKey TREASURE_KEY;

    static {
        PRIOR_WORK_KEY = new NamespacedKey("artificial_enchantments", "prior_work");
        TREASURE_KEY = new NamespacedKey("artificial_enchantments", "treasure");
    }

    private final Plugin plugin;
    private final ItemStorage itemStorage;
    private final EnchantmentRegistryManager registryManager;
    private final AnvilCombinationLogic combinationLogic;
    private final Logger logger;

    /**
     * Creates a new AnvilListener.
     *
     * @param plugin the plugin instance
     * @param itemStorage the item storage for enchantment data
     * @param registryManager the enchantment registry manager
     * @throws NullPointerException if any parameter is null
     * @since 0.2.0
     */
    public AnvilListener(
            @NotNull Plugin plugin,
            @NotNull ItemStorage itemStorage,
            @NotNull EnchantmentRegistryManager registryManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.itemStorage = Objects.requireNonNull(itemStorage, "itemStorage cannot be null");
        this.registryManager = Objects.requireNonNull(registryManager, "registryManager cannot be null");
        this.combinationLogic = new AnvilCombinationLogic(itemStorage);
        this.logger = plugin.getLogger();
    }

    /**
     * Handles PrepareAnvilEvent to calculate custom enchantment combinations.
     *
     * @param event the prepare anvil event
     * @since 0.2.0
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPrepareAnvil(@NotNull PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        AnvilView view = event.getView();
        ItemStack first = inventory.getFirstItem();
        ItemStack second = inventory.getSecondItem();
        String renameText = view.getRenameText();

        if (isEmpty(first) && isEmpty(second)) {
            return;
        }

        boolean hasCustomFirst = hasCustomEnchantments(first);
        boolean hasCustomSecond = hasCustomEnchantments(second);

        if (!hasCustomFirst && !hasCustomSecond) {
            return;
        }

        AnvilCombinationLogic.CombinationResult result = calculateCombination(
                first, second, renameText
        );

        if (result == null || result.result() == null) {
            event.setResult(null);
            return;
        }

        if (result.tooExpensive()) {
            event.setResult(null);
            return;
        }

        event.setResult(result.result());
        view.setRepairCost(result.cost());
    }

    @Nullable
    private AnvilCombinationLogic.CombinationResult calculateCombination(
            @Nullable ItemStack first,
            @Nullable ItemStack second,
            @Nullable String renameText) {
        if (isEmpty(first)) {
            return null;
        }

        int priorWorkFirst = getPriorWork(first);

        if (isEmpty(second)) {
            if (renameText != null && !renameText.isEmpty()) {
                return handleRenameOnly(first, renameText, priorWorkFirst);
            }
            return null;
        }

        int priorWorkSecond = getPriorWork(second);
        boolean isTreasure = isTreasureBook(second);

        if (isEnchantedBook(first) && isEnchantedBook(second)) {
            return combinationLogic.combineBooks(first, second, priorWorkFirst, priorWorkSecond);
        }

        if (isEnchantedBook(second)) {
            return combinationLogic.combine(first, second, priorWorkFirst, priorWorkSecond, isTreasure);
        }

        return combinationLogic.combine(first, second, priorWorkFirst, priorWorkSecond, false);
    }

    @Nullable
    private AnvilCombinationLogic.CombinationResult handleRenameOnly(
            @NotNull ItemStack item,
            @NotNull String newName,
            int priorWork) {
        int cost = AnvilCostCalculator.calculateRenameCost(null, newName, priorWork);

        if (AnvilCostCalculator.isTooExpensive(cost)) {
            return null;
        }

        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(newName);
            result.setItemMeta(meta);
        }

        int newPriorWork = AnvilCostCalculator.calculateNewPriorWork(priorWork);

        return new AnvilCombinationLogic.CombinationResult(
                result,
                cost,
                newPriorWork,
                false,
                itemStorage.getEnchantments(item),
                java.util.Collections.emptySet()
        );
    }

    private boolean hasCustomEnchantments(@Nullable ItemStack item) {
        if (isEmpty(item)) {
            return false;
        }
        Map<EnchantmentDefinition, Integer> enchantments = itemStorage.getEnchantments(item);
        return !enchantments.isEmpty();
    }

    private boolean isEnchantedBook(@Nullable ItemStack item) {
        if (isEmpty(item)) {
            return false;
        }
        return item.getType() == Material.ENCHANTED_BOOK;
    }

    private boolean isEmpty(@Nullable ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private int getPriorWork(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        Integer priorWork = container.get(PRIOR_WORK_KEY, PersistentDataType.INTEGER);
        return priorWork != null ? priorWork : 0;
    }

    private boolean isTreasureBook(@NotNull ItemStack item) {
        String treasure = itemStorage.getAuxiliaryMetadata(item, "treasure_book");
        if ("true".equals(treasure)) {
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Enchantment enchantment : storageMeta.getStoredEnchants().keySet()) {
                if (!enchantment.isDiscoverable()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Applies prior work penalty to an item after a successful anvil operation.
     * <p>
     * This should be called when the anvil transaction completes to track
     * the item's anvil use history.
     *
     * @param item the item to update
     * @param priorWork the new prior work value
     * @return the updated item
     * @since 0.2.0
     */
    @NotNull
    public ItemStack applyPriorWork(@NotNull ItemStack item, int priorWork) {
        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(PRIOR_WORK_KEY, PersistentDataType.INTEGER, priorWork);
            result.setItemMeta(meta);
        }
        return result;
    }
}
