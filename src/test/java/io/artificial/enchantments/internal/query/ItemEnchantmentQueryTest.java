package io.artificial.enchantments.internal.query;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.EnchantmentEffectHandler;
import io.artificial.enchantments.api.ItemStorage;
import io.artificial.enchantments.api.query.ItemEnchantmentQuery;
import io.artificial.enchantments.api.scaling.LevelScaling;
import io.artificial.enchantments.internal.EnchantmentRegistryManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ItemEnchantmentQuery Tests")
class ItemEnchantmentQueryTest {

    private ItemEnchantmentQuery query;
    private MockItemStorage itemStorage;
    private EnchantmentRegistryManager registry;
    private static final String TEST_NS = "test";

    @BeforeEach
    void setUp() {
        registry = EnchantmentRegistryManager.getInstance();
        registry.clear();

        itemStorage = new MockItemStorage();
        query = new ItemEnchantmentQueryImpl(itemStorage, registry);
    }

    @Test
    @DisplayName("Query creation with valid dependencies succeeds")
    void queryCreationSucceeds() {
        assertNotNull(query);
    }

    @Test
    @DisplayName("Query creation with null itemStorage throws")
    void queryCreationWithNullStorageThrows() {
        assertThrows(NullPointerException.class, () ->
            new ItemEnchantmentQueryImpl(null, registry));
    }

    @Test
    @DisplayName("Query creation with null registry throws")
    void queryCreationWithNullRegistryThrows() {
        assertThrows(NullPointerException.class, () ->
            new ItemEnchantmentQueryImpl(itemStorage, null));
    }

    @Test
    @DisplayName("hasEnchantment returns false for null item")
    void hasEnchantmentReturnsFalseForNullItem() {
        EnchantmentDefinition enchantment = createTestEnchantment("test1", Material.DIAMOND_SWORD);
        registry.register(enchantment);

        assertFalse(query.hasEnchantment(null, enchantment));
    }

    @Test
    @DisplayName("hasEnchantment returns false for item without enchantment")
    void hasEnchantmentReturnsFalseForItemWithoutEnchantment() {
        EnchantmentDefinition enchantment = createTestEnchantment("test2", Material.DIAMOND_SWORD);
        registry.register(enchantment);
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);

        assertFalse(query.hasEnchantment(item, enchantment));
    }

    @Test
    @DisplayName("hasEnchantment returns true for item with enchantment")
    void hasEnchantmentReturnsTrueForItemWithEnchantment() {
        EnchantmentDefinition enchantment = createTestEnchantment("test3", Material.DIAMOND_SWORD);
        registry.register(enchantment);
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);
        itemStorage.applyEnchantment(item, enchantment, 1);

        assertTrue(query.hasEnchantment(item, enchantment));
    }

    @Test
    @DisplayName("hasEnchantment by key returns false for null item")
    void hasEnchantmentByKeyReturnsFalseForNullItem() {
        EnchantmentDefinition enchantment = createTestEnchantment("test4", Material.DIAMOND_SWORD);
        registry.register(enchantment);

        assertFalse(query.hasEnchantment(null, enchantment.getKey()));
    }

    @Test
    @DisplayName("hasEnchantment by key returns true for item with enchantment")
    void hasEnchantmentByKeyReturnsTrueForItemWithEnchantment() {
        EnchantmentDefinition enchantment = createTestEnchantment("test5", Material.DIAMOND_SWORD);
        registry.register(enchantment);
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);
        itemStorage.applyEnchantment(item, enchantment, 1);

        assertTrue(query.hasEnchantment(item, enchantment.getKey()));
    }

    @Test
    @DisplayName("hasEnchantment throws for null enchantment")
    void hasEnchantmentThrowsForNullEnchantment() {
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);

        assertThrows(NullPointerException.class, () ->
            query.hasEnchantment(item, (EnchantmentDefinition) null));
    }

    @Test
    @DisplayName("hasEnchantment by key throws for null key")
    void hasEnchantmentByKeyThrowsForNullKey() {
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);

        assertThrows(NullPointerException.class, () ->
            query.hasEnchantment(item, (NamespacedKey) null));
    }

    @Test
    @DisplayName("getLevel returns 0 for null item")
    void getLevelReturnsZeroForNullItem() {
        EnchantmentDefinition enchantment = createTestEnchantment("test6", Material.DIAMOND_SWORD);
        registry.register(enchantment);

        assertEquals(0, query.getLevel(null, enchantment));
    }

    @Test
    @DisplayName("getLevel returns 0 for item without enchantment")
    void getLevelReturnsZeroForItemWithoutEnchantment() {
        EnchantmentDefinition enchantment = createTestEnchantment("test7", Material.DIAMOND_SWORD);
        registry.register(enchantment);
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);

        assertEquals(0, query.getLevel(item, enchantment));
    }

    @Test
    @DisplayName("getLevel returns correct level for item with enchantment")
    void getLevelReturnsCorrectLevel() {
        EnchantmentDefinition enchantment = createTestEnchantment("test8", Material.DIAMOND_SWORD, 1, 5);
        registry.register(enchantment);
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);
        itemStorage.applyEnchantment(item, enchantment, 3);

        assertEquals(3, query.getLevel(item, enchantment));
    }

    @Test
    @DisplayName("getLevel by key returns correct level")
    void getLevelByKeyReturnsCorrectLevel() {
        EnchantmentDefinition enchantment = createTestEnchantment("test9", Material.DIAMOND_SWORD, 1, 5);
        registry.register(enchantment);
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);
        itemStorage.applyEnchantment(item, enchantment, 2);

        assertEquals(2, query.getLevel(item, enchantment.getKey()));
    }

    @Test
    @DisplayName("getLevel throws for null enchantment")
    void getLevelThrowsForNullEnchantment() {
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);

        assertThrows(NullPointerException.class, () ->
            query.getLevel(item, (EnchantmentDefinition) null));
    }

    @Test
    @DisplayName("getLevel by key throws for null key")
    void getLevelByKeyThrowsForNullKey() {
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);

        assertThrows(NullPointerException.class, () ->
            query.getLevel(item, (NamespacedKey) null));
    }

    @Test
    @DisplayName("getAllEnchantments returns empty map for null item")
    void getAllEnchantmentsReturnsEmptyForNullItem() {
        Map<EnchantmentDefinition, Integer> result = query.getAllEnchantments(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getAllEnchantments returns empty map for unenchanted item")
    void getAllEnchantmentsReturnsEmptyForUnenchantedItem() {
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);

        Map<EnchantmentDefinition, Integer> result = query.getAllEnchantments(item);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getAllEnchantments returns all enchantments on item")
    void getAllEnchantmentsReturnsAllEnchantments() {
        EnchantmentDefinition enchantment1 = createTestEnchantment("test10a", Material.DIAMOND_SWORD);
        EnchantmentDefinition enchantment2 = createTestEnchantment("test10b", Material.DIAMOND_SWORD);
        registry.register(enchantment1);
        registry.register(enchantment2);

        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);
        itemStorage.applyEnchantment(item, enchantment1, 2);
        itemStorage.applyEnchantment(item, enchantment2, 3);

        Map<EnchantmentDefinition, Integer> result = query.getAllEnchantments(item);

        assertEquals(2, result.size());
        assertEquals(2, result.get(enchantment1));
        assertEquals(3, result.get(enchantment2));
    }

    @Test
    @DisplayName("isEnchanted returns false for null item")
    void isEnchantedReturnsFalseForNullItem() {
        assertFalse(query.isEnchanted(null));
    }

    @Test
    @DisplayName("isEnchanted returns false for unenchanted item")
    void isEnchantedReturnsFalseForUnenchantedItem() {
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);

        assertFalse(query.isEnchanted(item));
    }

    @Test
    @DisplayName("isEnchanted returns true for enchanted item")
    void isEnchantedReturnsTrueForEnchantedItem() {
        EnchantmentDefinition enchantment = createTestEnchantment("test11", Material.DIAMOND_SWORD);
        registry.register(enchantment);
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);
        itemStorage.applyEnchantment(item, enchantment, 1);

        assertTrue(query.isEnchanted(item));
    }

    @Test
    @DisplayName("getEnchantmentsFor returns empty set for null material")
    void getEnchantmentsForReturnsEmptyForNullMaterial() {
        Set<EnchantmentDefinition> result = query.getEnchantmentsFor(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getEnchantmentsFor returns empty set for material with no applicable enchantments")
    void getEnchantmentsForReturnsEmptyForNoApplicable() {
        EnchantmentDefinition enchantment = createTestEnchantment("test12", Material.DIAMOND_SWORD);
        registry.register(enchantment);

        Set<EnchantmentDefinition> result = query.getEnchantmentsFor(Material.DIAMOND_PICKAXE);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getEnchantmentsFor returns applicable enchantments")
    void getEnchantmentsForReturnsApplicableEnchantments() {
        EnchantmentDefinition enchantment1 = createTestEnchantment("test13a", Material.DIAMOND_SWORD);
        EnchantmentDefinition enchantment2 = createTestEnchantment("test13b", Material.DIAMOND_SWORD);
        EnchantmentDefinition enchantment3 = createTestEnchantment("test13c", Material.DIAMOND_PICKAXE);
        registry.register(enchantment1);
        registry.register(enchantment2);
        registry.register(enchantment3);

        Set<EnchantmentDefinition> result = query.getEnchantmentsFor(Material.DIAMOND_SWORD);

        assertEquals(2, result.size());
        assertTrue(result.contains(enchantment1));
        assertTrue(result.contains(enchantment2));
        assertFalse(result.contains(enchantment3));
    }

    @Test
    @DisplayName("hasAllEnchantments returns false for null item")
    void hasAllEnchantmentsReturnsFalseForNullItem() {
        EnchantmentDefinition enchantment = createTestEnchantment("test14", Material.DIAMOND_SWORD);
        registry.register(enchantment);

        assertFalse(query.hasAllEnchantments(null, enchantment));
    }

    @Test
    @DisplayName("hasAllEnchantments returns true when all enchantments present")
    void hasAllEnchantmentsReturnsTrueWhenAllPresent() {
        EnchantmentDefinition enchantment1 = createTestEnchantment("test15a", Material.DIAMOND_SWORD);
        EnchantmentDefinition enchantment2 = createTestEnchantment("test15b", Material.DIAMOND_SWORD);
        registry.register(enchantment1);
        registry.register(enchantment2);

        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);
        itemStorage.applyEnchantment(item, enchantment1, 1);
        itemStorage.applyEnchantment(item, enchantment2, 1);

        assertTrue(query.hasAllEnchantments(item, enchantment1, enchantment2));
    }

    @Test
    @DisplayName("hasAllEnchantments returns false when any enchantment missing")
    void hasAllEnchantmentsReturnsFalseWhenAnyMissing() {
        EnchantmentDefinition enchantment1 = createTestEnchantment("test16a", Material.DIAMOND_SWORD);
        EnchantmentDefinition enchantment2 = createTestEnchantment("test16b", Material.DIAMOND_SWORD);
        registry.register(enchantment1);
        registry.register(enchantment2);

        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);
        itemStorage.applyEnchantment(item, enchantment1, 1);

        assertFalse(query.hasAllEnchantments(item, enchantment1, enchantment2));
    }

    @Test
    @DisplayName("hasAllEnchantments throws for null enchantments array")
    void hasAllEnchantmentsThrowsForNullArray() {
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);

        assertThrows(NullPointerException.class, () ->
            query.hasAllEnchantments(item, (EnchantmentDefinition[]) null));
    }

    @Test
    @DisplayName("hasAllEnchantments throws for empty enchantments array")
    void hasAllEnchantmentsThrowsForEmptyArray() {
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);

        assertThrows(IllegalArgumentException.class, () ->
            query.hasAllEnchantments(item));
    }

    @Test
    @DisplayName("hasAnyEnchantment returns false for null item")
    void hasAnyEnchantmentReturnsFalseForNullItem() {
        EnchantmentDefinition enchantment = createTestEnchantment("test17", Material.DIAMOND_SWORD);
        registry.register(enchantment);

        assertFalse(query.hasAnyEnchantment(null, enchantment));
    }

    @Test
    @DisplayName("hasAnyEnchantment returns true when any enchantment present")
    void hasAnyEnchantmentReturnsTrueWhenAnyPresent() {
        EnchantmentDefinition enchantment1 = createTestEnchantment("test18a", Material.DIAMOND_SWORD);
        EnchantmentDefinition enchantment2 = createTestEnchantment("test18b", Material.DIAMOND_SWORD);
        registry.register(enchantment1);
        registry.register(enchantment2);

        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);
        itemStorage.applyEnchantment(item, enchantment1, 1);

        assertTrue(query.hasAnyEnchantment(item, enchantment1, enchantment2));
    }

    @Test
    @DisplayName("hasAnyEnchantment returns false when none present")
    void hasAnyEnchantmentReturnsFalseWhenNonePresent() {
        EnchantmentDefinition enchantment1 = createTestEnchantment("test19a", Material.DIAMOND_SWORD);
        EnchantmentDefinition enchantment2 = createTestEnchantment("test19b", Material.DIAMOND_SWORD);
        registry.register(enchantment1);
        registry.register(enchantment2);

        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);

        assertFalse(query.hasAnyEnchantment(item, enchantment1, enchantment2));
    }

    @Test
    @DisplayName("hasAnyEnchantment throws for null enchantments array")
    void hasAnyEnchantmentThrowsForNullArray() {
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);

        assertThrows(NullPointerException.class, () ->
            query.hasAnyEnchantment(item, (EnchantmentDefinition[]) null));
    }

    @Test
    @DisplayName("hasAnyEnchantment throws for empty enchantments array")
    void hasAnyEnchantmentThrowsForEmptyArray() {
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);

        assertThrows(IllegalArgumentException.class, () ->
            query.hasAnyEnchantment(item));
    }

    @Test
    @DisplayName("getTotalEnchantmentLevel returns 0 for null item")
    void getTotalEnchantmentLevelReturnsZeroForNullItem() {
        assertEquals(0, query.getTotalEnchantmentLevel(null));
    }

    @Test
    @DisplayName("getTotalEnchantmentLevel returns 0 for unenchanted item")
    void getTotalEnchantmentLevelReturnsZeroForUnenchantedItem() {
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);

        assertEquals(0, query.getTotalEnchantmentLevel(item));
    }

    @Test
    @DisplayName("getTotalEnchantmentLevel returns sum of all levels")
    void getTotalEnchantmentLevelReturnsSum() {
        EnchantmentDefinition enchantment1 = createTestEnchantment("test20a", Material.DIAMOND_SWORD);
        EnchantmentDefinition enchantment2 = createTestEnchantment("test20b", Material.DIAMOND_SWORD);
        registry.register(enchantment1);
        registry.register(enchantment2);

        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);
        itemStorage.applyEnchantment(item, enchantment1, 2);
        itemStorage.applyEnchantment(item, enchantment2, 3);

        assertEquals(5, query.getTotalEnchantmentLevel(item));
    }

    @Test
    @DisplayName("getHighestLevel returns 0 for null item")
    void getHighestLevelReturnsZeroForNullItem() {
        assertEquals(0, query.getHighestLevel(null));
    }

    @Test
    @DisplayName("getHighestLevel returns 0 for unenchanted item")
    void getHighestLevelReturnsZeroForUnenchantedItem() {
        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);

        assertEquals(0, query.getHighestLevel(item));
    }

    @Test
    @DisplayName("getHighestLevel returns highest level among all enchantments")
    void getHighestLevelReturnsHighest() {
        EnchantmentDefinition enchantment1 = createTestEnchantment("test21a", Material.DIAMOND_SWORD);
        EnchantmentDefinition enchantment2 = createTestEnchantment("test21b", Material.DIAMOND_SWORD);
        registry.register(enchantment1);
        registry.register(enchantment2);

        ItemStack item = itemStorage.createItem(Material.DIAMOND_SWORD);
        itemStorage.applyEnchantment(item, enchantment1, 2);
        itemStorage.applyEnchantment(item, enchantment2, 5);

        assertEquals(5, query.getHighestLevel(item));
    }

    private EnchantmentDefinition createTestEnchantment(String name, Material material) {
        return createTestEnchantment(name, material, 1, 1);
    }

    private EnchantmentDefinition createTestEnchantment(String name, Material material, int minLevel, int maxLevel) {
        return new EnchantmentDefinition() {
            @Override
            public NamespacedKey getKey() {
                return new NamespacedKey(TEST_NS, name);
            }

            @Override
            public Component getDisplayName() {
                return Component.text(name);
            }

            @Override
            public Component getDescription() {
                return null;
            }

            @Override
            public int getMinLevel() {
                return minLevel;
            }

            @Override
            public int getMaxLevel() {
                return maxLevel;
            }

            @Override
            public LevelScaling getScaling() {
                return LevelScaling.linear(1.0, 0.5);
            }

            @Override
            public Set<Material> getApplicableMaterials() {
                return Set.of(material);
            }

            @Override
            public boolean isApplicableTo(Material material) {
                return getApplicableMaterials().contains(material);
            }

            @Override
            public boolean isApplicableTo(ItemStack item) {
                return isApplicableTo(item.getType());
            }

            @Override
            public boolean isCurse() {
                return false;
            }

            @Override
            public boolean isTradeable() {
                return true;
            }

            @Override
            public boolean isDiscoverable() {
                return true;
            }

            @Override
            public Rarity getRarity() {
                return Rarity.COMMON;
            }

            @Override
            public EnchantmentEffectHandler getEffectHandler() {
                return null;
            }

            @Override
            public double calculateScaledValue(int level) {
                return getScaling().calculate(level);
            }

            @Override
            public boolean conflictsWith(EnchantmentDefinition other) {
                return false;
            }

            @Override
            public Set<NamespacedKey> getConflictingEnchantments() {
                return Set.of();
            }
        };
    }

    private static class MockItemStorage implements ItemStorage {
        private final Map<Integer, Map<NamespacedKey, Integer>> enchantmentData = new ConcurrentHashMap<>();
        private int nextItemId = 1;

        ItemStack createItem(Material material) {
            ItemStack item = mock(ItemStack.class);
            when(item.getType()).thenReturn(material);
            return item;
        }

        private int getItemId(ItemStack item) {
            return item.hashCode();
        }

        @Override
        public ItemStack applyEnchantment(ItemStack item, EnchantmentDefinition enchantment, int level) {
            enchantmentData.computeIfAbsent(getItemId(item), k -> new HashMap<>())
                    .put(enchantment.getKey(), level);
            return item;
        }

        @Override
        public ItemStack applyEnchantment(ItemStack item, NamespacedKey key, int level) {
            enchantmentData.computeIfAbsent(getItemId(item), k -> new HashMap<>())
                    .put(key, level);
            return item;
        }

        @Override
        public ItemStack removeEnchantment(ItemStack item, EnchantmentDefinition enchantment) {
            Map<NamespacedKey, Integer> data = enchantmentData.get(getItemId(item));
            if (data != null) {
                data.remove(enchantment.getKey());
            }
            return item;
        }

        @Override
        public ItemStack removeEnchantment(ItemStack item, NamespacedKey key) {
            Map<NamespacedKey, Integer> data = enchantmentData.get(getItemId(item));
            if (data != null) {
                data.remove(key);
            }
            return item;
        }

        @Override
        public ItemStack removeAllEnchantments(ItemStack item) {
            enchantmentData.remove(getItemId(item));
            return item;
        }

        @Override
        public int getEnchantmentLevel(ItemStack item, EnchantmentDefinition enchantment) {
            Map<NamespacedKey, Integer> data = enchantmentData.get(getItemId(item));
            return data != null ? data.getOrDefault(enchantment.getKey(), 0) : 0;
        }

        @Override
        public int getEnchantmentLevel(ItemStack item, NamespacedKey key) {
            Map<NamespacedKey, Integer> data = enchantmentData.get(getItemId(item));
            return data != null ? data.getOrDefault(key, 0) : 0;
        }

        @Override
        public Map<EnchantmentDefinition, Integer> getEnchantments(ItemStack item) {
            Map<NamespacedKey, Integer> data = enchantmentData.get(getItemId(item));
            if (data == null || data.isEmpty()) {
                return Map.of();
            }

            Map<EnchantmentDefinition, Integer> result = new HashMap<>();
            EnchantmentRegistryManager registry = EnchantmentRegistryManager.getInstance();
            for (Map.Entry<NamespacedKey, Integer> entry : data.entrySet()) {
                registry.get(entry.getKey()).ifPresent(def -> result.put(def, entry.getValue()));
            }
            return Collections.unmodifiableMap(result);
        }

        @Override
        public Map<NamespacedKey, Integer> getEnchantmentKeys(ItemStack item) {
            Map<NamespacedKey, Integer> data = enchantmentData.get(getItemId(item));
            return data != null ? Collections.unmodifiableMap(new HashMap<>(data)) : Map.of();
        }

        @Override
        public boolean hasEnchantment(ItemStack item, EnchantmentDefinition enchantment) {
            return getEnchantmentLevel(item, enchantment) > 0;
        }

        @Override
        public boolean hasEnchantment(ItemStack item, NamespacedKey key) {
            return getEnchantmentLevel(item, key) > 0;
        }

        @Override
        public Set<String> getAuxiliaryMetadataKeys(ItemStack item) {
            return Set.of();
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
