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

public final class EnchantmentTableListener implements Listener {

    private final Plugin plugin;
    private final EnchantOfferGenerator offerGenerator;
    private final EnchantmentRegistryManager registryManager;
    private final PaperRegistryBridge registryBridge;
    private final Logger logger;

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
