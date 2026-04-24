package io.artificial.enchantments.internal.enchanttable;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.internal.EnchantmentRegistryManager;
import io.artificial.enchantments.internal.PaperRegistryBridge;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Listener for enchantment table events to enable custom enchantment support.
 * <p>
 * Hooks into PrepareItemEnchantEvent to generate and inject custom enchantment
 * offers alongside vanilla offers. Also handles EnchantItemEvent to apply
 * selected custom enchantments to the item being enchanted.
 * <p>
 * <strong>Processing Flow:</strong>
 * <ol>
 *   <li>Player places item in enchantment table</li>
 *   <li>{@link #onPrepareItemEnchant} generates custom offers based on power (bookshelves)</li>
 *   <li>Custom offers are injected into empty slots of the offer array</li>
 *   <li>Player selects an offer and enchants the item</li>
 *   <li>{@link #onEnchantItem} extracts custom enchantments and applies them</li>
 * </ol>
 *
 * @since 0.2.0
 */
public final class EnchantmentTableListener implements Listener {

    private final Plugin plugin;
    private final EnchantOfferGenerator offerGenerator;
    private final EnchantmentRegistryManager registryManager;
    private final PaperRegistryBridge registryBridge;
    private final Logger logger;

    /**
     * Creates a new enchantment table listener.
     *
     * @param plugin the plugin instance
     * @param registryManager the enchantment registry manager
     * @param registryBridge the bridge to native Paper enchantments
     * @throws NullPointerException if any parameter is null
     * @since 0.2.0
     */
    public EnchantmentTableListener(
            @NotNull Plugin plugin,
            @NotNull EnchantmentRegistryManager registryManager,
            @NotNull PaperRegistryBridge registryBridge
    ) {
        this.plugin = plugin;
        this.registryManager = registryManager;
        this.registryBridge = registryBridge;
        this.offerGenerator = new EnchantOfferGenerator(registryManager);
        this.logger = plugin.getLogger();
    }

    /**
     * Handles PrepareItemEnchantEvent to generate and inject custom enchantment offers.
     * <p>
     * This method is called when a player places an item in the enchantment table.
     * It generates custom enchantment offers based on the table's power level
     * (number of bookshelves) and injects them into empty slots of the offer array.
     *
     * @param event the prepare item enchant event
     * @since 0.2.0
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPrepareItemEnchant(@NotNull PrepareItemEnchantEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            return;
        }

        int power = event.getEnchantmentBonus();
        EnchantmentOffer[] existingOffers = event.getOffers();

        Set<Enchantment> vanillaEnchantments = extractVanillaEnchantments(existingOffers);

        List<EnchantOfferGenerator.GeneratedOffer> customOffers = offerGenerator.generateOffers(
            item, power, vanillaEnchantments
        );

        if (customOffers.isEmpty()) {
            return;
        }

        injectOffersIntoSlots(existingOffers, customOffers);
    }

    /**
     * Handles EnchantItemEvent to apply selected custom enchantments to the item.
     * <p>
     * This method is called when a player selects an enchantment offer and clicks
     * to enchant the item. It extracts any custom enchantments from the enchantment
     * map and applies them to the resulting item using unsafe enchantments.
     *
     * @param event the enchant item event
     * @since 0.2.0
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEnchantItem(@NotNull EnchantItemEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            return;
        }

        Map<Enchantment, Integer> enchantsToAdd = event.getEnchantsToAdd();
        Map<EnchantmentDefinition, Integer> customEnchants = extractCustomEnchantments(enchantsToAdd);

        if (customEnchants.isEmpty()) {
            return;
        }

        applyCustomEnchantmentsToItem(item, customEnchants);
    }

    /**
     * Extracts vanilla enchantments from the current offer array.
     * <p>
     * Used to track which vanilla enchantments are already present
     * to avoid generating conflicting custom offers.
     *
     * @param offers the array of enchantment offers
     * @return set of vanilla enchantments currently offered
     * @since 0.2.0
     */
    @NotNull
    private Set<Enchantment> extractVanillaEnchantments(@NotNull EnchantmentOffer[] offers) {
        Set<Enchantment> enchantments = new HashSet<>();
        for (EnchantmentOffer offer : offers) {
            if (offer != null && offer.getEnchantment() != null) {
                enchantments.add(offer.getEnchantment());
            }
        }
        return enchantments;
    }

    /**
     * Injects custom offers into empty slots of the offer array.
     * <p>
     * Iterates through custom offers and places them in the first available
     * null slot in the offer array. Skips offers if the native enchantment
     * cannot be found in the Paper registry.
     *
     * @param slots the offer array to inject into
     * @param customOffers the list of generated custom offers
     * @since 0.2.0
     */
    private void injectOffersIntoSlots(
            @NotNull EnchantmentOffer[] slots,
            @NotNull List<EnchantOfferGenerator.GeneratedOffer> customOffers
    ) {
        int slotIndex = 0;
        for (EnchantOfferGenerator.GeneratedOffer customOffer : customOffers) {
            if (slotIndex >= slots.length) {
                break;
            }

            Enchantment nativeEnchant = registryBridge.getNativeEnchantment(customOffer.definition().getKey());
            if (nativeEnchant == null) {
                logger.warning("Cannot find native enchantment for: " + customOffer.definition().getKey());
                continue;
            }

            while (slotIndex < slots.length) {
                if (slots[slotIndex] == null) {
                    slots[slotIndex] = customOffer.toBukkitOffer(nativeEnchant);
                    break;
                }
                slotIndex++;
            }
        }
    }

    /**
     * Extracts custom enchantments from the enchantments to be added.
     * <p>
     * Filters the enchantment map to only include enchantments that are
     * registered in our custom registry, mapping them to their definitions.
     *
     * @param enchantsToAdd the map of enchantments to be added to the item
     * @return map of custom enchantment definitions to their levels
     * @since 0.2.0
     */
    @NotNull
    private Map<EnchantmentDefinition, Integer> extractCustomEnchantments(
            @NotNull Map<Enchantment, Integer> enchantsToAdd
    ) {
        Map<EnchantmentDefinition, Integer> customEnchants = new HashMap<>();

        for (Map.Entry<Enchantment, Integer> entry : enchantsToAdd.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (enchantment == null) {
                continue;
            }

            NamespacedKey key = enchantment.getKey();
            EnchantmentDefinition definition = registryManager.getEnchantment(key);

            if (definition != null) {
                customEnchants.put(definition, entry.getValue());
            }
        }

        return customEnchants;
    }

    /**
     * Applies custom enchantments to the enchanted item.
     * <p>
     * Validates each enchantment level against the definition's min/max levels
     * before applying. Uses addUnsafeEnchantment to bypass vanilla enchantment
     * type restrictions.
     *
     * @param item the item being enchanted
     * @param customEnchants map of custom enchantments to apply
     * @since 0.2.0
     */
    private void applyCustomEnchantmentsToItem(
            @NotNull ItemStack item,
            @NotNull Map<EnchantmentDefinition, Integer> customEnchants
    ) {
        for (Map.Entry<EnchantmentDefinition, Integer> entry : customEnchants.entrySet()) {
            EnchantmentDefinition definition = entry.getKey();
            int level = entry.getValue();

            if (level < definition.getMinLevel() || level > definition.getMaxLevel()) {
                logger.warning("Invalid enchantment level " + level + " for " + definition.getKey());
                continue;
            }

            item.addUnsafeEnchantment(
                registryBridge.getNativeEnchantment(definition.getKey()),
                level
            );
        }
    }
}
