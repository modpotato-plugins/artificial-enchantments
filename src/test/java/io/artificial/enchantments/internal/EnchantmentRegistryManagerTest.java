package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.EnchantmentEffectHandler;
import io.artificial.enchantments.api.scaling.LevelScaling;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.*;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EnchantmentRegistryManager.
 * 
 * <p>Tests cover:
 * <ul>
 *   <li>Registration operations (register, unregister)</li>
 *   <li>Lookup operations (get, getAll, getForMaterial)</li>
 *   <li>Native registration tracking</li>
 *   <li>Validation and edge cases</li>
 * </ul>
 */
@DisplayName("EnchantmentRegistryManager Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EnchantmentRegistryManagerTest {

    private EnchantmentRegistryManager registry;
    private static final String TEST_NAMESPACE = "test";

    @BeforeEach
    void setUp() {
        // Get fresh instance and clear it
        registry = EnchantmentRegistryManager.getInstance();
        registry.clear();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        if (registry != null) {
            registry.clear();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Registry is singleton")
    void registryIsSingleton() {
        EnchantmentRegistryManager instance1 = EnchantmentRegistryManager.getInstance();
        EnchantmentRegistryManager instance2 = EnchantmentRegistryManager.getInstance();
        
        assertSame(instance1, instance2, "Registry should be singleton");
    }

    @Test
    @Order(2)
    @DisplayName("Register new enchantment succeeds")
    void registerNewEnchantmentSucceeds() {
        EnchantmentDefinition enchantment = createTestEnchantment("test_enchant", Material.DIAMOND_SWORD);
        
        boolean result = registry.register(enchantment);
        
        assertTrue(result, "Registration should succeed for new enchantment");
    }

    @Test
    @Order(3)
    @DisplayName("Register duplicate enchantment fails")
    void registerDuplicateEnchantmentFails() {
        EnchantmentDefinition enchantment = createTestEnchantment("duplicate_enchant", Material.DIAMOND_SWORD);
        
        registry.register(enchantment);
        boolean result = registry.register(enchantment);
        
        assertFalse(result, "Registration should fail for duplicate enchantment");
    }

    @Test
    @Order(4)
    @DisplayName("Get existing enchantment returns definition")
    void getExistingEnchantmentReturnsDefinition() {
        EnchantmentDefinition enchantment = createTestEnchantment("gettable_enchant", Material.DIAMOND_SWORD);
        registry.register(enchantment);
        
        Optional<EnchantmentDefinition> result = registry.get(enchantment.getKey());
        
        assertTrue(result.isPresent(), "Should find registered enchantment");
        assertEquals(enchantment.getKey(), result.get().getKey(), "Keys should match");
    }

    @Test
    @Order(5)
    @DisplayName("Get non-existing enchantment returns empty")
    void getNonExistingEnchantmentReturnsEmpty() {
        NamespacedKey key = new NamespacedKey(TEST_NAMESPACE, "nonexistent");
        
        Optional<EnchantmentDefinition> result = registry.get(key);
        
        assertTrue(result.isEmpty(), "Should not find unregistered enchantment");
    }

    @Test
    @Order(6)
    @DisplayName("GetEnchantment returns definition for existing")
    void getEnchantmentReturnsDefinitionForExisting() {
        EnchantmentDefinition enchantment = createTestEnchantment("gettable_enchant2", Material.DIAMOND_SWORD);
        registry.register(enchantment);
        
        EnchantmentDefinition result = registry.getEnchantment(enchantment.getKey());
        
        assertNotNull(result, "Should find registered enchantment");
        assertEquals(enchantment.getKey(), result.getKey(), "Keys should match");
    }

    @Test
    @Order(7)
    @DisplayName("GetEnchantment returns null for non-existing")
    void getEnchantmentReturnsNullForNonExisting() {
        NamespacedKey key = new NamespacedKey(TEST_NAMESPACE, "nonexistent2");
        
        EnchantmentDefinition result = registry.getEnchantment(key);
        
        assertNull(result, "Should return null for unregistered enchantment");
    }

    @Test
    @Order(8)
    @DisplayName("Unregister existing enchantment succeeds")
    void unregisterExistingEnchantmentSucceeds() {
        EnchantmentDefinition enchantment = createTestEnchantment("removable_enchant", Material.DIAMOND_SWORD);
        registry.register(enchantment);
        
        boolean result = registry.unregister(enchantment.getKey());
        
        assertTrue(result, "Unregistration should succeed");
        assertTrue(registry.get(enchantment.getKey()).isEmpty(), "Should no longer be registered");
    }

    @Test
    @Order(9)
    @DisplayName("Unregister non-existing enchantment fails")
    void unregisterNonExistingEnchantmentFails() {
        NamespacedKey key = new NamespacedKey(TEST_NAMESPACE, "never_registered");
        
        boolean result = registry.unregister(key);
        
        assertFalse(result, "Unregistration should fail for unknown enchantment");
    }

    @Test
    @Order(10)
    @DisplayName("GetAll returns all registered enchantments")
    void getAllReturnsAllRegisteredEnchantments() {
        EnchantmentDefinition enchant1 = createTestEnchantment("enchant_1", Material.DIAMOND_SWORD);
        EnchantmentDefinition enchant2 = createTestEnchantment("enchant_2", Material.DIAMOND_PICKAXE);
        EnchantmentDefinition enchant3 = createTestEnchantment("enchant_3", Material.DIAMOND_AXE);
        
        registry.register(enchant1);
        registry.register(enchant2);
        registry.register(enchant3);
        
        Collection<EnchantmentDefinition> all = registry.getAll();
        
        assertEquals(3, all.size(), "Should return all 3 enchantments");
        assertTrue(all.contains(enchant1), "Should contain enchant1");
        assertTrue(all.contains(enchant2), "Should contain enchant2");
        assertTrue(all.contains(enchant3), "Should contain enchant3");
    }

    @Test
    @Order(11)
    @DisplayName("GetAll returns unmodifiable collection")
    void getAllReturnsUnmodifiableCollection() {
        EnchantmentDefinition enchantment = createTestEnchantment("single", Material.DIAMOND_SWORD);
        registry.register(enchantment);
        
        Collection<EnchantmentDefinition> all = registry.getAll();
        
        assertThrows(UnsupportedOperationException.class, () -> all.add(enchantment),
                "Collection should be unmodifiable");
    }

    @Test
    @Order(12)
    @DisplayName("GetForMaterial returns enchantments for specific material")
    void getForMaterialReturnsEnchantmentsForSpecificMaterial() {
        EnchantmentDefinition swordEnchant = createTestEnchantment("sword_enchant", Material.DIAMOND_SWORD);
        EnchantmentDefinition pickaxeEnchant = createTestEnchantment("pickaxe_enchant", Material.DIAMOND_PICKAXE);
        EnchantmentDefinition anotherSwordEnchant = createTestEnchantment("another_sword", Material.DIAMOND_SWORD);
        
        registry.register(swordEnchant);
        registry.register(pickaxeEnchant);
        registry.register(anotherSwordEnchant);
        
        Set<EnchantmentDefinition> swordEnchantments = registry.getForMaterial(Material.DIAMOND_SWORD);
        
        assertEquals(2, swordEnchantments.size(), "Should return 2 sword enchantments");
        assertTrue(swordEnchantments.contains(swordEnchant), "Should contain sword_enchant");
        assertTrue(swordEnchantments.contains(anotherSwordEnchant), "Should contain another_sword");
    }

    @Test
    @Order(13)
    @DisplayName("GetForMaterial returns empty set for unknown material")
    void getForMaterialReturnsEmptyForUnknownMaterial() {
        Set<EnchantmentDefinition> result = registry.getForMaterial(Material.BAMBOO);
        
        assertTrue(result.isEmpty(), "Should return empty set for material with no enchantments");
    }

    @Test
    @Order(14)
    @DisplayName("GetForMaterial returns unmodifiable set")
    void getForMaterialReturnsUnmodifiableSet() {
        EnchantmentDefinition enchantment = createTestEnchantment("test", Material.DIAMOND_SWORD);
        registry.register(enchantment);
        
        Set<EnchantmentDefinition> result = registry.getForMaterial(Material.DIAMOND_SWORD);
        
        assertThrows(UnsupportedOperationException.class, () -> result.add(enchantment),
                "Set should be unmodifiable");
    }

    @Test
    @Order(15)
    @DisplayName("Clear removes all registrations")
    void clearRemovesAllRegistrations() {
        registry.register(createTestEnchantment("clear_test1", Material.DIAMOND_SWORD));
        registry.register(createTestEnchantment("clear_test2", Material.DIAMOND_PICKAXE));
        
        registry.clear();
        
        assertTrue(registry.getAll().isEmpty(), "All registrations should be cleared");
    }

    @Test
    @Order(16)
    @DisplayName("MarkNativeRegistered moves from pending to registered")
    void markNativeRegisteredMovesFromPending() {
        EnchantmentDefinition enchantment = createTestEnchantment("native_test", Material.DIAMOND_SWORD);
        registry.register(enchantment);
        
        assertFalse(registry.isNativeRegistered(enchantment.getKey()), 
                "Should not be native registered initially");
        
        registry.markNativeRegistered(enchantment.getKey());
        
        assertTrue(registry.isNativeRegistered(enchantment.getKey()), 
                "Should be native registered after marking");
    }

    @Test
    @Order(17)
    @DisplayName("GetPendingRegistrations returns unregistered enchantments")
    void getPendingRegistrationsReturnsUnregisteredEnchantments() {
        EnchantmentDefinition enchant1 = createTestEnchantment("pending1", Material.DIAMOND_SWORD);
        EnchantmentDefinition enchant2 = createTestEnchantment("pending2", Material.DIAMOND_PICKAXE);
        
        registry.register(enchant1);
        registry.register(enchant2);
        registry.markNativeRegistered(enchant1.getKey());
        
        Collection<EnchantmentDefinition> pending = registry.getPendingRegistrations();
        
        assertEquals(1, pending.size(), "Should have 1 pending registration");
        assertTrue(pending.contains(enchant2), "Pending should contain enchant2");
        assertFalse(pending.contains(enchant1), "Pending should not contain enchant1 (already native registered)");
    }

    @Test
    @Order(18)
    @DisplayName("Material index updates correctly on unregister")
    void materialIndexUpdatesCorrectlyOnUnregister() {
        EnchantmentDefinition enchantment = createTestEnchantment("index_test", Material.DIAMOND_SWORD);
        registry.register(enchantment);
        
        assertEquals(1, registry.getForMaterial(Material.DIAMOND_SWORD).size());
        
        registry.unregister(enchantment.getKey());
        
        assertEquals(0, registry.getForMaterial(Material.DIAMOND_SWORD).size(),
                "Material index should be updated after unregister");
    }

    @Test
    @Order(19)
    @DisplayName("Multiple enchantments for same material are indexed correctly")
    void multipleEnchantmentsForSameMaterialIndexedCorrectly() {
        Material material = Material.DIAMOND_SWORD;
        
        EnchantmentDefinition enchant1 = createTestEnchantment("multi_1", material);
        EnchantmentDefinition enchant2 = createTestEnchantment("multi_2", material);
        EnchantmentDefinition enchant3 = createTestEnchantment("multi_3", material);
        
        registry.register(enchant1);
        registry.register(enchant2);
        registry.register(enchant3);
        
        Set<EnchantmentDefinition> result = registry.getForMaterial(material);
        
        assertEquals(3, result.size(), "All 3 enchantments should be indexed");
    }

    @Test
    @Order(20)
    @DisplayName("Enchantment with multiple materials is indexed for each")
    void enchantmentWithMultipleMaterialsIndexedForEach() {
        EnchantmentDefinition enchantment = createTestEnchantment(
                "multi_material",
                Material.DIAMOND_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD
        );
        
        registry.register(enchantment);
        
        assertTrue(registry.getForMaterial(Material.DIAMOND_SWORD).contains(enchantment));
        assertTrue(registry.getForMaterial(Material.IRON_SWORD).contains(enchantment));
        assertTrue(registry.getForMaterial(Material.GOLDEN_SWORD).contains(enchantment));
        assertFalse(registry.getForMaterial(Material.STONE_SWORD).contains(enchantment));
    }

    // Helper method to create test enchantments
    private EnchantmentDefinition createTestEnchantment(String name, Material... materials) {
        return new TestEnchantmentDefinition(name, materials);
    }

    /**
     * Simple test implementation of EnchantmentDefinition
     */
    private class TestEnchantmentDefinition implements EnchantmentDefinition {
        private final NamespacedKey key;
        private final Set<Material> applicableMaterials;

        TestEnchantmentDefinition(String name, Material... materials) {
            this.key = new NamespacedKey(TEST_NAMESPACE, name);
            this.applicableMaterials = Set.of(materials);
        }

        @Override
        public NamespacedKey getKey() {
            return key;
        }

        @Override
        public Component getDisplayName() {
            return Component.text(key.getKey());
        }

        @Override
        public Component getDescription() {
            return null;
        }

        @Override
        public int getMinLevel() {
            return 1;
        }

        @Override
        public int getMaxLevel() {
            return 5;
        }

        @Override
        public LevelScaling getScaling() {
            return LevelScaling.linear(1.0, 0.5);
        }

        @Override
        public Set<Material> getApplicableMaterials() {
            return applicableMaterials;
        }

        @Override
        public boolean isApplicableTo(Material material) {
            return applicableMaterials.contains(material);
        }

        @Override
        public boolean isApplicableTo(org.bukkit.inventory.ItemStack item) {
            return applicableMaterials.contains(item.getType());
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EnchantmentDefinition that)) return false;
            return key.equals(that.getKey());
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }
}
