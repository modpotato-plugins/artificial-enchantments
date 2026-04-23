package io.artificial.enchantments.internal.anvil;

import be.seeseemelk.mockbukkit.MockBukkit;
import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.ItemStorage;
import io.artificial.enchantments.api.scaling.LevelScaling;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

@DisplayName("AnvilCombinationLogic Tests")
class AnvilCombinationLogicTest {

    private AnvilCombinationLogic combinationLogic;
    private MockedConstruction<ItemStack> itemStackConstruction;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        itemStackConstruction = mockConstruction(ItemStack.class, (mock, context) -> {
            if (!context.arguments().isEmpty() && context.arguments().get(0) instanceof Material material) {
                when(mock.getType()).thenReturn(material);
            }
            when(mock.getAmount()).thenReturn(1);
            when(mock.clone()).thenReturn(mock);
        });
        ItemStorage itemStorage = new MockItemStorage();
        combinationLogic = new AnvilCombinationLogic(itemStorage);
    }

    @AfterEach
    void tearDown() {
        if (itemStackConstruction != null) {
            itemStackConstruction.close();
        }
        MockBukkit.unmock();
    }

    private ItemStack mockItemStack(Material material) {
        ItemStack mock = mock(ItemStack.class);
        when(mock.getType()).thenReturn(material);
        when(mock.getAmount()).thenReturn(1);
        when(mock.clone()).thenReturn(mock);
        return mock;
    }

    private EnchantmentDefinition createEnchantment(String name, EnchantmentDefinition.Rarity rarity,
                                                     Set<Material> applicable, NamespacedKey... conflicts) {
        EnchantmentDefinition.Builder builder = EnchantmentDefinition.builder()
                .key(new NamespacedKey("test", name))
                .displayName(Component.text(name))
                .minLevel(1)
                .maxLevel(5)
                .scaling(LevelScaling.linear(1.0, 0.5))
                .rarity(rarity);

        if (!applicable.isEmpty()) {
            builder.applicable(applicable.toArray(new Material[0]));
        }

        for (NamespacedKey conflict : conflicts) {
            builder.conflictsWith(conflict);
        }

        return builder.build();
    }

    @Test
    @DisplayName("Combine with empty items returns empty result")
    void combineEmptyItems() {
        ItemStack target = mockItemStack(Material.AIR);
        ItemStack sacrifice = mockItemStack(Material.AIR);

        AnvilCombinationLogic.CombinationResult result = combinationLogic.combine(
                target, sacrifice, 0, 0, false
        );

        assertNull(result.result());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Combine with no enchantments returns empty result")
    void combineNoEnchantments() {
        ItemStack target = mockItemStack(Material.DIAMOND_SWORD);
        ItemStack sacrifice = mockItemStack(Material.DIAMOND_SWORD);

        AnvilCombinationLogic.CombinationResult result = combinationLogic.combine(
                target, sacrifice, 0, 0, false
        );

        assertNull(result.result());
    }

    @Test
    @DisplayName("Combine books returns merged enchantments")
    void combineBooks() {
        MockItemStorage storage = new MockItemStorage();
        combinationLogic = new AnvilCombinationLogic(storage);

        EnchantmentDefinition enchantA = createEnchantment("enchant_a", EnchantmentDefinition.Rarity.COMMON,
                Set.of(Material.DIAMOND_SWORD));
        EnchantmentDefinition enchantB = createEnchantment("enchant_b", EnchantmentDefinition.Rarity.COMMON,
                Set.of(Material.DIAMOND_SWORD));

        ItemStack bookA = mockItemStack(Material.ENCHANTED_BOOK);
        ItemStack bookB = mockItemStack(Material.ENCHANTED_BOOK);

        storage.setEnchantments(bookA, Map.of(enchantA, 2));
        storage.setEnchantments(bookB, Map.of(enchantB, 3));

        AnvilCombinationLogic.CombinationResult result = combinationLogic.combineBooks(
                bookA, bookB, 0, 0
        );

        assertTrue(result.isSuccess());
        assertEquals(2, result.appliedEnchantments().size());
        assertTrue(result.appliedEnchantments().containsKey(enchantA));
        assertTrue(result.appliedEnchantments().containsKey(enchantB));
    }

    @Test
    @DisplayName("Same level enchantments upgrade by 1")
    void sameLevelUpgrades() {
        MockItemStorage storage = new MockItemStorage();
        combinationLogic = new AnvilCombinationLogic(storage);

        EnchantmentDefinition enchant = createEnchantment("enchant", EnchantmentDefinition.Rarity.COMMON,
                Set.of(Material.DIAMOND_SWORD));

        ItemStack bookA = mockItemStack(Material.ENCHANTED_BOOK);
        ItemStack bookB = mockItemStack(Material.ENCHANTED_BOOK);

        storage.setEnchantments(bookA, Map.of(enchant, 2));
        storage.setEnchantments(bookB, Map.of(enchant, 2));

        AnvilCombinationLogic.CombinationResult result = combinationLogic.combineBooks(
                bookA, bookB, 0, 0
        );

        assertTrue(result.isSuccess());
        assertEquals(3, result.appliedEnchantments().get(enchant));
    }

    @Test
    @DisplayName("Max level enchantments don't upgrade beyond max")
    void maxLevelCap() {
        MockItemStorage storage = new MockItemStorage();
        combinationLogic = new AnvilCombinationLogic(storage);

        EnchantmentDefinition enchant = createEnchantment("enchant", EnchantmentDefinition.Rarity.COMMON,
                Set.of(Material.DIAMOND_SWORD));

        ItemStack bookA = mockItemStack(Material.ENCHANTED_BOOK);
        ItemStack bookB = mockItemStack(Material.ENCHANTED_BOOK);

        storage.setEnchantments(bookA, Map.of(enchant, 5));
        storage.setEnchantments(bookB, Map.of(enchant, 5));

        AnvilCombinationLogic.CombinationResult result = combinationLogic.combineBooks(
                bookA, bookB, 0, 0
        );

        assertTrue(result.isSuccess());
        assertEquals(5, result.appliedEnchantments().get(enchant));
    }

    @Test
    @DisplayName("Higher level takes precedence when levels differ")
    void higherLevelTakesPrecedence() {
        MockItemStorage storage = new MockItemStorage();
        combinationLogic = new AnvilCombinationLogic(storage);

        EnchantmentDefinition enchant = createEnchantment("enchant", EnchantmentDefinition.Rarity.COMMON,
                Set.of(Material.DIAMOND_SWORD));

        ItemStack bookA = mockItemStack(Material.ENCHANTED_BOOK);
        ItemStack bookB = mockItemStack(Material.ENCHANTED_BOOK);

        storage.setEnchantments(bookA, Map.of(enchant, 2));
        storage.setEnchantments(bookB, Map.of(enchant, 4));

        AnvilCombinationLogic.CombinationResult result = combinationLogic.combineBooks(
                bookA, bookB, 0, 0
        );

        assertTrue(result.isSuccess());
        assertEquals(4, result.appliedEnchantments().get(enchant));
    }

    @Test
    @DisplayName("Conflicting enchantments are detected")
    void conflictingEnchantments() {
        MockItemStorage storage = new MockItemStorage();
        combinationLogic = new AnvilCombinationLogic(storage);

        NamespacedKey keyA = new NamespacedKey("test", "enchant_a");
        NamespacedKey keyB = new NamespacedKey("test", "enchant_b");

        EnchantmentDefinition enchantA = createEnchantment("enchant_a", EnchantmentDefinition.Rarity.COMMON,
                Set.of(Material.DIAMOND_SWORD), keyB);
        EnchantmentDefinition enchantB = createEnchantment("enchant_b", EnchantmentDefinition.Rarity.COMMON,
                Set.of(Material.DIAMOND_SWORD), keyA);

        ItemStack bookA = mockItemStack(Material.ENCHANTED_BOOK);
        ItemStack bookB = mockItemStack(Material.ENCHANTED_BOOK);

        storage.setEnchantments(bookA, Map.of(enchantA, 2));
        storage.setEnchantments(bookB, Map.of(enchantB, 3));

        AnvilCombinationLogic.CombinationResult result = combinationLogic.combineBooks(
                bookA, bookB, 0, 0
        );

        assertTrue(result.isSuccess());
        assertFalse(result.conflicts().isEmpty());
        assertTrue(result.conflicts().contains(enchantB));
    }

    @Test
    @DisplayName("Too expensive result when cost exceeds max")
    void tooExpensiveResult() {
        MockItemStorage storage = new MockItemStorage();
        combinationLogic = new AnvilCombinationLogic(storage);

        EnchantmentDefinition enchant = createEnchantment("enchant", EnchantmentDefinition.Rarity.VERY_RARE,
                Set.of(Material.DIAMOND_SWORD));

        ItemStack bookA = mockItemStack(Material.ENCHANTED_BOOK);
        ItemStack bookB = mockItemStack(Material.ENCHANTED_BOOK);

        storage.setEnchantments(bookA, Map.of(enchant, 5));
        storage.setEnchantments(bookB, Map.of(enchant, 5));

        AnvilCombinationLogic.CombinationResult result = combinationLogic.combineBooks(
                bookA, bookB, 20, 20
        );

        assertTrue(result.tooExpensive());
        assertNull(result.result());
    }

    @Test
    @DisplayName("Prior work is calculated correctly")
    void priorWorkCalculation() {
        MockItemStorage storage = new MockItemStorage();
        combinationLogic = new AnvilCombinationLogic(storage);

        EnchantmentDefinition enchant = createEnchantment("enchant", EnchantmentDefinition.Rarity.COMMON,
                Set.of(Material.DIAMOND_SWORD));

        ItemStack bookA = mockItemStack(Material.ENCHANTED_BOOK);
        ItemStack bookB = mockItemStack(Material.ENCHANTED_BOOK);

        storage.setEnchantments(bookA, Map.of(enchant, 2));
        storage.setEnchantments(bookB, Map.of(enchant, 3));

        AnvilCombinationLogic.CombinationResult result = combinationLogic.combineBooks(
                bookA, bookB, 3, 5
        );

        int expectedPriorWork = AnvilCostCalculator.calculateNewPriorWork(5);
        assertEquals(expectedPriorWork, result.newPriorWork());
    }

    @Test
    @DisplayName("Cost is calculated with prior work penalties")
    void costWithPriorWork() {
        MockItemStorage storage = new MockItemStorage();
        combinationLogic = new AnvilCombinationLogic(storage);

        EnchantmentDefinition enchant = createEnchantment("enchant", EnchantmentDefinition.Rarity.COMMON,
                Set.of(Material.DIAMOND_SWORD));

        ItemStack bookA = mockItemStack(Material.ENCHANTED_BOOK);
        ItemStack bookB = mockItemStack(Material.ENCHANTED_BOOK);

        storage.setEnchantments(bookA, Map.of(enchant, 1));
        storage.setEnchantments(bookB, Map.of(enchant, 2));

        AnvilCombinationLogic.CombinationResult result = combinationLogic.combineBooks(
                bookA, bookB, 2, 3
        );

        int expectedBase = 2 + 2 + 3;
        assertTrue(result.cost() >= expectedBase);
    }

    private static class MockItemStorage implements ItemStorage {
        private final Map<ItemStack, Map<EnchantmentDefinition, Integer>> enchantments = new HashMap<>();

        void setEnchantments(ItemStack item, Map<EnchantmentDefinition, Integer> enchants) {
            enchantments.put(item, new HashMap<>(enchants));
        }

        @Override
        public ItemStack applyEnchantment(ItemStack item, EnchantmentDefinition enchantment, int level) {
            enchantments.computeIfAbsent(item, k -> new HashMap<>()).put(enchantment, level);
            return item;
        }

        @Override
        public ItemStack applyEnchantment(ItemStack item, NamespacedKey key, int level) {
            return item;
        }

        @Override
        public ItemStack removeEnchantment(ItemStack item, EnchantmentDefinition enchantment) {
            enchantments.getOrDefault(item, new HashMap<>()).remove(enchantment);
            return item;
        }

        @Override
        public ItemStack removeEnchantment(ItemStack item, NamespacedKey key) {
            return item;
        }

        @Override
        public ItemStack removeAllEnchantments(ItemStack item) {
            enchantments.remove(item);
            return item;
        }

        @Override
        public int getEnchantmentLevel(ItemStack item, EnchantmentDefinition enchantment) {
            return enchantments.getOrDefault(item, Collections.emptyMap()).getOrDefault(enchantment, 0);
        }

        @Override
        public int getEnchantmentLevel(ItemStack item, NamespacedKey key) {
            return 0;
        }

        @Override
        public Map<EnchantmentDefinition, Integer> getEnchantments(ItemStack item) {
            return enchantments.getOrDefault(item, Collections.emptyMap());
        }

        @Override
        public Map<NamespacedKey, Integer> getEnchantmentKeys(ItemStack item) {
            return Collections.emptyMap();
        }

        @Override
        public boolean hasEnchantment(ItemStack item, EnchantmentDefinition enchantment) {
            return getEnchantmentLevel(item, enchantment) > 0;
        }

        @Override
        public boolean hasEnchantment(ItemStack item, NamespacedKey key) {
            return false;
        }

        @Override
        public java.util.Set<String> getAuxiliaryMetadataKeys(ItemStack item) {
            return Collections.emptySet();
        }

        @Override
        public ItemStack setAuxiliaryMetadata(ItemStack item, String key, String value) {
            return item;
        }

        @Override
        public String getAuxiliaryMetadata(ItemStack item, String key) {
            return null;
        }

        @Override
        public boolean hasAuxiliaryMetadata(ItemStack item, String key) {
            return false;
        }

        @Override
        public ItemStack clearAuxiliaryMetadata(ItemStack item) {
            return item;
        }
    }
}
