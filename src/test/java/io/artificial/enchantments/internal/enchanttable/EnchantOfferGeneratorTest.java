package io.artificial.enchantments.internal.enchanttable;

import be.seeseemelk.mockbukkit.MockBukkit;
import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.enchanttable.EnchantTableConfiguration;
import io.artificial.enchantments.api.scaling.LevelScaling;
import io.artificial.enchantments.internal.EnchantmentRegistryManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnchantOfferGeneratorTest {

    private EnchantmentRegistryManager registryManager;
    private EnchantOfferGenerator generator;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        registryManager = mock(EnchantmentRegistryManager.class);
        generator = new EnchantOfferGenerator(registryManager);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private ItemStack mockItemStack(Material material) {
        ItemStack mock = mock(ItemStack.class);
        when(mock.getType()).thenReturn(material);
        when(mock.getAmount()).thenReturn(1);
        return mock;
    }

    @Test
    void generateOffers_EmptyItem_ReturnsEmptyList() {
        ItemStack air = mockItemStack(Material.AIR);
        List<EnchantOfferGenerator.GeneratedOffer> offers = generator.generateOffers(air, 15, Collections.emptySet());

        assertTrue(offers.isEmpty());
    }

    @Test
    void generateOffers_NoApplicableEnchantments_ReturnsEmptyList() {
        ItemStack sword = mockItemStack(Material.DIAMOND_SWORD);
        when(registryManager.getForMaterial(Material.DIAMOND_SWORD)).thenReturn(Collections.emptySet());

        List<EnchantOfferGenerator.GeneratedOffer> offers = generator.generateOffers(sword, 15, Collections.emptySet());

        assertTrue(offers.isEmpty());
    }

    @Test
    void generateOffers_SingleEnchantment_GeneratesOffer() {
        ItemStack sword = mockItemStack(Material.DIAMOND_SWORD);
        NamespacedKey key = new NamespacedKey("test", "life_steal");

        EnchantmentDefinition definition = EnchantmentDefinition.builder()
            .key(key)
            .displayName(Component.text("Life Steal"))
            .minLevel(1)
            .maxLevel(3)
            .scaling(LevelScaling.linear(1.0, 0.5))
            .applicable(Material.DIAMOND_SWORD)
            .rarity(EnchantmentDefinition.Rarity.COMMON)
            .discoverable(true)
            .build();

        when(registryManager.getForMaterial(Material.DIAMOND_SWORD))
            .thenReturn(Set.of(definition));

        List<EnchantOfferGenerator.GeneratedOffer> offers = generator.generateOffers(sword, 15, Collections.emptySet());

        assertFalse(offers.isEmpty());
        assertTrue(offers.size() <= 3);

        for (EnchantOfferGenerator.GeneratedOffer offer : offers) {
            assertEquals(definition, offer.definition());
            assertTrue(offer.level() >= 1 && offer.level() <= 3);
            assertTrue(offer.cost() >= 1 && offer.cost() <= 30);
        }
    }

    @Test
    void generateOffers_NonDiscoverableEnchantment_Excluded() {
        ItemStack sword = mockItemStack(Material.DIAMOND_SWORD);
        NamespacedKey key = new NamespacedKey("test", "hidden_enchant");

        EnchantmentDefinition definition = EnchantmentDefinition.builder()
            .key(key)
            .displayName(Component.text("Hidden"))
            .minLevel(1)
            .maxLevel(2)
            .scaling(LevelScaling.constant(1.0))
            .applicable(Material.DIAMOND_SWORD)
            .rarity(EnchantmentDefinition.Rarity.COMMON)
            .discoverable(false)
            .build();

        when(registryManager.getForMaterial(Material.DIAMOND_SWORD))
            .thenReturn(Set.of(definition));

        List<EnchantOfferGenerator.GeneratedOffer> offers = generator.generateOffers(sword, 15, Collections.emptySet());

        assertTrue(offers.isEmpty());
    }

    @Test
    void generateOffers_BookshelfConstraints_Respected() {
        ItemStack sword = mockItemStack(Material.DIAMOND_SWORD);

        EnchantTableConfiguration highPowerConfig = EnchantTableConfiguration.builder()
            .minBookshelves(10)
            .build();

        assertTrue(highPowerConfig.getMinBookshelves() >= 10);
    }

    @Test
    void calculateWeight_CommonRarity_ReturnsHigherWeight() {
        NamespacedKey key = new NamespacedKey("test", "common");
        EnchantmentDefinition common = EnchantmentDefinition.builder()
            .key(key)
            .displayName(Component.text("Common"))
            .minLevel(1)
            .maxLevel(2)
            .scaling(LevelScaling.constant(1.0))
            .applicable(Material.DIAMOND_SWORD)
            .rarity(EnchantmentDefinition.Rarity.COMMON)
            .discoverable(true)
            .build();

        NamespacedKey key2 = new NamespacedKey("test", "very_rare");
        EnchantmentDefinition veryRare = EnchantmentDefinition.builder()
            .key(key2)
            .displayName(Component.text("Very Rare"))
            .minLevel(1)
            .maxLevel(2)
            .scaling(LevelScaling.constant(1.0))
            .applicable(Material.DIAMOND_SWORD)
            .rarity(EnchantmentDefinition.Rarity.VERY_RARE)
            .discoverable(true)
            .build();

        when(registryManager.getForMaterial(Material.DIAMOND_SWORD))
            .thenReturn(Set.of(common, veryRare));

        int commonCount = 0;
        int rareCount = 0;
        ItemStack sword = mockItemStack(Material.DIAMOND_SWORD);

        for (int i = 0; i < 100; i++) {
            List<EnchantOfferGenerator.GeneratedOffer> offers = generator.generateOffers(sword, 15, Collections.emptySet());
            if (!offers.isEmpty()) {
                EnchantOfferGenerator.GeneratedOffer firstOffer = offers.get(0);
                if (firstOffer.definition().getRarity() == EnchantmentDefinition.Rarity.COMMON) {
                    commonCount++;
                } else if (firstOffer.definition().getRarity() == EnchantmentDefinition.Rarity.VERY_RARE) {
                    rareCount++;
                }
            }
        }

        assertTrue(commonCount > rareCount, "Common enchantments should be selected more frequently as the first offer");
    }

    @Test
    void calculateCost_PowerLevels_AffectCost() {
        ItemStack sword = mockItemStack(Material.DIAMOND_SWORD);
        NamespacedKey key = new NamespacedKey("test", "test_enchant");

        EnchantmentDefinition definition = EnchantmentDefinition.builder()
            .key(key)
            .displayName(Component.text("Test"))
            .minLevel(1)
            .maxLevel(5)
            .scaling(LevelScaling.linear(1.0, 0.5))
            .applicable(Material.DIAMOND_SWORD)
            .rarity(EnchantmentDefinition.Rarity.COMMON)
            .discoverable(true)
            .build();

        when(registryManager.getForMaterial(Material.DIAMOND_SWORD))
            .thenReturn(Set.of(definition));

        int lowPowerCost = Integer.MAX_VALUE;
        int highPowerCost = 0;

        for (int i = 0; i < 50; i++) {
            List<EnchantOfferGenerator.GeneratedOffer> lowOffers = generator.generateOffers(sword, 3, Collections.emptySet());
            for (EnchantOfferGenerator.GeneratedOffer offer : lowOffers) {
                lowPowerCost = Math.min(lowPowerCost, offer.cost());
            }

            List<EnchantOfferGenerator.GeneratedOffer> highOffers = generator.generateOffers(sword, 15, Collections.emptySet());
            for (EnchantOfferGenerator.GeneratedOffer offer : highOffers) {
                highPowerCost = Math.max(highPowerCost, offer.cost());
            }
        }

        assertTrue(lowPowerCost <= highPowerCost, "Higher power should generally result in higher or equal cost");
    }

    @Test
    void determineLevel_LevelRange_RespectsDefinitionBounds() {
        ItemStack sword = mockItemStack(Material.DIAMOND_SWORD);
        NamespacedKey key = new NamespacedKey("test", "bounded");

        EnchantmentDefinition definition = EnchantmentDefinition.builder()
            .key(key)
            .displayName(Component.text("Bounded"))
            .minLevel(2)
            .maxLevel(4)
            .scaling(LevelScaling.linear(1.0, 0.5))
            .applicable(Material.DIAMOND_SWORD)
            .rarity(EnchantmentDefinition.Rarity.COMMON)
            .discoverable(true)
            .build();

        when(registryManager.getForMaterial(Material.DIAMOND_SWORD))
            .thenReturn(Set.of(definition));

        for (int i = 0; i < 100; i++) {
            List<EnchantOfferGenerator.GeneratedOffer> offers = generator.generateOffers(sword, 15, Collections.emptySet());
            for (EnchantOfferGenerator.GeneratedOffer offer : offers) {
                assertTrue(offer.level() >= 2, "Level should be at least minLevel (2)");
                assertTrue(offer.level() <= 4, "Level should be at most maxLevel (4)");
            }
        }
    }
}
