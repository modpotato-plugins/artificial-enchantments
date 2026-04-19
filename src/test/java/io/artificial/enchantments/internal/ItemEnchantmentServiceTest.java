package io.artificial.enchantments.internal;

import be.seeseemelk.mockbukkit.MockBukkit;
import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.EnchantmentEffectHandler;
import io.artificial.enchantments.api.ItemStorage;
import io.artificial.enchantments.api.scaling.LevelScaling;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ItemEnchantmentService Tests")
class ItemEnchantmentServiceTest {

    private ItemEnchantmentService service;
    private EnchantmentRegistryManager registry;
    private static final String TEST_NS = "test";

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        registry = EnchantmentRegistryManager.getInstance();
        registry.clear();
        
        ItemStorage itemStorage = createMockItemStorage();
        service = new ItemEnchantmentService(itemStorage, registry);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Service creation with valid dependencies succeeds")
    void serviceCreationSucceeds() {
        ItemStorage storage = createMockItemStorage();
        
        ItemEnchantmentService svc = new ItemEnchantmentService(storage, registry);
        
        assertNotNull(svc);
    }

    @Test
    @DisplayName("Service creation with null itemStorage throws")
    void serviceCreationWithNullStorageThrows() {
        assertThrows(NullPointerException.class, () -> 
            new ItemEnchantmentService(null, registry));
    }

    @Test
    @DisplayName("Service creation with null registry throws")
    void serviceCreationWithNullRegistryThrows() {
        ItemStorage storage = createMockItemStorage();
        assertThrows(NullPointerException.class, () -> 
            new ItemEnchantmentService(storage, null));
    }

    @Test
    @DisplayName("Register enchantment adds to registry")
    void registerEnchantmentAddsToRegistry() {
        EnchantmentDefinition enchantment = createTestEnchantment("service_test");
        
        EnchantmentDefinition result = service.registerEnchantment(enchantment);
        
        assertEquals(enchantment.getKey(), result.getKey());
        assertTrue(registry.get(enchantment.getKey()).isPresent());
    }

    @Test
    @DisplayName("Register null enchantment throws")
    void registerNullEnchantmentThrows() {
        assertThrows(NullPointerException.class, () -> 
            service.registerEnchantment(null));
    }

    @Test
    @DisplayName("Unregister existing enchantment returns true")
    void unregisterExistingReturnsTrue() {
        EnchantmentDefinition enchantment = createTestEnchantment("unregister_test");
        service.registerEnchantment(enchantment);
        
        boolean result = service.unregisterEnchantment(enchantment.getKey());
        
        assertTrue(result);
        assertTrue(registry.get(enchantment.getKey()).isEmpty());
    }

    @Test
    @DisplayName("Unregister non-existing enchantment returns false")
    void unregisterNonExistingReturnsFalse() {
        NamespacedKey key = new NamespacedKey(TEST_NS, "never_registered");
        
        boolean result = service.unregisterEnchantment(key);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Unregister null key throws")
    void unregisterNullKeyThrows() {
        assertThrows(NullPointerException.class, () -> 
            service.unregisterEnchantment(null));
    }

    @Test
    @DisplayName("Get all enchantments returns registered")
    void getAllEnchantmentsReturnsRegistered() {
        service.registerEnchantment(createTestEnchantment("all_1"));
        service.registerEnchantment(createTestEnchantment("all_2"));
        service.registerEnchantment(createTestEnchantment("all_3"));
        
        Collection<EnchantmentDefinition> result = service.getAllEnchantments();
        
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("Get enchantment by key returns definition")
    void getEnchantmentByKeyReturnsDefinition() {
        EnchantmentDefinition enchantment = createTestEnchantment("get_by_key");
        service.registerEnchantment(enchantment);
        
        Optional<EnchantmentDefinition> result = service.getEnchantment(enchantment.getKey());
        
        assertTrue(result.isPresent());
        assertEquals(enchantment.getKey(), result.get().getKey());
    }

    @Test
    @DisplayName("Get enchantment for material delegates to registry")
    void getEnchantmentsForMaterialDelegatesToRegistry() {
        EnchantmentDefinition swordEnchant = createTestEnchantment("sword_mat", Material.DIAMOND_SWORD);
        service.registerEnchantment(swordEnchant);
        
        Set<EnchantmentDefinition> result = service.getEnchantmentsForMaterial(Material.DIAMOND_SWORD);
        
        assertTrue(result.contains(swordEnchant));
    }

    @Test
    @DisplayName("Get enchantments for null material throws")
    void getEnchantmentsForNullMaterialThrows() {
        assertThrows(NullPointerException.class, () -> 
            service.getEnchantmentsForMaterial(null));
    }

    @Test
    @DisplayName("Apply enchantment by definition delegates to storage")
    void applyEnchantmentByDefinitionDelegates() {
        EnchantmentDefinition enchantment = createTestEnchantment("apply_def");
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.DIAMOND_SWORD);
        
        org.bukkit.inventory.ItemStack result = service.applyEnchantment(item, enchantment, 3);
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("Apply enchantment by key delegates to storage")
    void applyEnchantmentByKeyDelegates() {
        EnchantmentDefinition enchantment = createTestEnchantment("apply_key");
        service.registerEnchantment(enchantment);
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.DIAMOND_SWORD);
        
        org.bukkit.inventory.ItemStack result = service.applyEnchantment(item, enchantment.getKey(), 2);
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("Remove enchantment by definition delegates to storage")
    void removeEnchantmentByDefinitionDelegates() {
        EnchantmentDefinition enchantment = createTestEnchantment("remove_def");
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.DIAMOND_SWORD);
        
        org.bukkit.inventory.ItemStack result = service.removeEnchantment(item, enchantment);
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("Remove enchantment by key delegates to storage")
    void removeEnchantmentByKeyDelegates() {
        EnchantmentDefinition enchantment = createTestEnchantment("remove_key");
        service.registerEnchantment(enchantment);
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.DIAMOND_SWORD);
        
        org.bukkit.inventory.ItemStack result = service.removeEnchantment(item, enchantment.getKey());
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("Remove all enchantments delegates to storage")
    void removeAllEnchantmentsDelegates() {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.DIAMOND_SWORD);
        
        org.bukkit.inventory.ItemStack result = service.removeAllEnchantments(item);
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("Get enchantment level by definition delegates to storage")
    void getEnchantmentLevelByDefinitionDelegates() {
        EnchantmentDefinition enchantment = createTestEnchantment("level_def");
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.DIAMOND_SWORD);
        
        int level = service.getEnchantmentLevel(item, enchantment);
        
        assertEquals(0, level);
    }

    @Test
    @DisplayName("Get enchantment level by key delegates to storage")
    void getEnchantmentLevelByKeyDelegates() {
        EnchantmentDefinition enchantment = createTestEnchantment("level_key");
        service.registerEnchantment(enchantment);
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.DIAMOND_SWORD);
        
        int level = service.getEnchantmentLevel(item, enchantment.getKey());
        
        assertEquals(0, level);
    }

    @Test
    @DisplayName("Get enchantments returns map from storage")
    void getEnchantmentsReturnsMap() {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.DIAMOND_SWORD);
        
        Map<EnchantmentDefinition, Integer> result = service.getEnchantments(item);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Get enchantment keys returns map from storage")
    void getEnchantmentKeysReturnsMap() {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.DIAMOND_SWORD);
        
        Map<NamespacedKey, Integer> result = service.getEnchantmentKeys(item);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Has enchantment by definition delegates to storage")
    void hasEnchantmentByDefinitionDelegates() {
        EnchantmentDefinition enchantment = createTestEnchantment("has_def");
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.DIAMOND_SWORD);
        
        boolean result = service.hasEnchantment(item, enchantment);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Has enchantment by key delegates to storage")
    void hasEnchantmentByKeyDelegates() {
        EnchantmentDefinition enchantment = createTestEnchantment("has_key");
        service.registerEnchantment(enchantment);
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.DIAMOND_SWORD);
        
        boolean result = service.hasEnchantment(item, enchantment.getKey());
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Set auxiliary metadata delegates to storage")
    void setAuxiliaryMetadataDelegates() {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.DIAMOND_SWORD);
        
        org.bukkit.inventory.ItemStack result = service.setAuxiliaryMetadata(item, "test_key", "test_value");
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("Get auxiliary metadata delegates to storage")
    void getAuxiliaryMetadataDelegates() {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.DIAMOND_SWORD);
        
        String result = service.getAuxiliaryMetadata(item, "test_key");
        
        assertNull(result);
    }

    private ItemStorage createMockItemStorage() {
        return new MockItemStorage();
    }

    private EnchantmentDefinition createTestEnchantment(String name) {
        return createTestEnchantment(name, Material.DIAMOND_SWORD);
    }

    private EnchantmentDefinition createTestEnchantment(String name, Material material) {
        return new TestEnchantmentDefinition(name, material);
    }

    private static class MockItemStorage implements ItemStorage {
        @Override
        public org.bukkit.inventory.ItemStack applyEnchantment(org.bukkit.inventory.ItemStack item, EnchantmentDefinition enchantment, int level) {
            return item.clone();
        }

        @Override
        public org.bukkit.inventory.ItemStack applyEnchantment(org.bukkit.inventory.ItemStack item, NamespacedKey key, int level) {
            return item.clone();
        }

        @Override
        public org.bukkit.inventory.ItemStack removeEnchantment(org.bukkit.inventory.ItemStack item, EnchantmentDefinition enchantment) {
            return item.clone();
        }

        @Override
        public org.bukkit.inventory.ItemStack removeEnchantment(org.bukkit.inventory.ItemStack item, NamespacedKey key) {
            return item.clone();
        }

        @Override
        public org.bukkit.inventory.ItemStack removeAllEnchantments(org.bukkit.inventory.ItemStack item) {
            return item.clone();
        }

        @Override
        public int getEnchantmentLevel(org.bukkit.inventory.ItemStack item, EnchantmentDefinition enchantment) {
            return 0;
        }

        @Override
        public int getEnchantmentLevel(org.bukkit.inventory.ItemStack item, NamespacedKey key) {
            return 0;
        }

        @Override
        public Map<EnchantmentDefinition, Integer> getEnchantments(org.bukkit.inventory.ItemStack item) {
            return Map.of();
        }

        @Override
        public Map<NamespacedKey, Integer> getEnchantmentKeys(org.bukkit.inventory.ItemStack item) {
            return Map.of();
        }

        @Override
        public boolean hasEnchantment(org.bukkit.inventory.ItemStack item, EnchantmentDefinition enchantment) {
            return false;
        }

        @Override
        public boolean hasEnchantment(org.bukkit.inventory.ItemStack item, NamespacedKey key) {
            return false;
        }

        @Override
        public Set<String> getAuxiliaryMetadataKeys(org.bukkit.inventory.ItemStack item) {
            return Set.of();
        }

        @Override
        public org.bukkit.inventory.ItemStack setAuxiliaryMetadata(org.bukkit.inventory.ItemStack item, String key, String value) {
            return item.clone();
        }

        @Override
        public String getAuxiliaryMetadata(org.bukkit.inventory.ItemStack item, String key) {
            return null;
        }

        @Override
        public boolean hasAuxiliaryMetadata(org.bukkit.inventory.ItemStack item, String key) {
            return false;
        }

        @Override
        public org.bukkit.inventory.ItemStack clearAuxiliaryMetadata(org.bukkit.inventory.ItemStack item) {
            return item.clone();
        }
    }

    private static class TestEnchantmentDefinition implements EnchantmentDefinition {
        private final NamespacedKey key;
        private final Set<Material> materials;

        TestEnchantmentDefinition(String name, Material material) {
            this.key = new NamespacedKey("test", name);
            this.materials = Set.of(material);
        }

        @Override
        public NamespacedKey getKey() { return key; }

        @Override
        public Component getDisplayName() { return Component.text(key.getKey()); }

        @Override
        public Component getDescription() { return null; }

        @Override
        public int getMinLevel() { return 1; }

        @Override
        public int getMaxLevel() { return 5; }

        @Override
        public LevelScaling getScaling() { return LevelScaling.linear(1.0, 0.5); }

        @Override
        public Set<Material> getApplicableMaterials() { return materials; }

        @Override
        public boolean isApplicableTo(Material material) { return materials.contains(material); }

        @Override
        public boolean isApplicableTo(org.bukkit.inventory.ItemStack item) { return materials.contains(item.getType()); }

        @Override
        public boolean isCurse() { return false; }

        @Override
        public boolean isTradeable() { return true; }

        @Override
        public boolean isDiscoverable() { return true; }

        @Override
        public Rarity getRarity() { return Rarity.COMMON; }

        @Override
        public EnchantmentEffectHandler getEffectHandler() { return null; }

        @Override
        public double calculateScaledValue(int level) { return getScaling().calculate(level); }

        @Override
        public boolean conflictsWith(EnchantmentDefinition other) { return false; }

        @Override
        public Set<NamespacedKey> getConflictingEnchantments() { return Set.of(); }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EnchantmentDefinition that)) return false;
            return key.equals(that.getKey());
        }

        @Override
        public int hashCode() { return key.hashCode(); }
    }
}
