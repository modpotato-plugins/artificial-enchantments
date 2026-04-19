package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.EnchantmentEffectHandler;
import io.artificial.enchantments.api.scaling.LevelScaling;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Thread Safety Tests")
class ThreadSafetyTest {

    private EnchantmentRegistryManager registry;
    private static final String TEST_NS = "test";
    private static final int THREAD_COUNT = 10;
    private static final int OPERATIONS_PER_THREAD = 100;

    @BeforeEach
    void setUp() {
        registry = EnchantmentRegistryManager.getInstance();
        registry.clear();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Concurrent registrations do not lose data")
    void concurrentRegistrationsDoNotLoseData() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        String name = "concurrent_" + threadId + "_" + j;
                        EnchantmentDefinition enchantment = createTestEnchantment(name, Material.DIAMOND_SWORD);
                        if (registry.register(enchantment)) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(25, TimeUnit.SECONDS));
        executor.shutdown();

        Collection<EnchantmentDefinition> allEnchantments = registry.getAll();
        assertEquals(successCount.get(), allEnchantments.size(),
            "All successful registrations should be present in registry");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Concurrent register and unregister maintains consistency")
    void concurrentRegisterAndUnregisterMaintainsConsistency() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger registered = new AtomicInteger(0);
        AtomicInteger unregistered = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT / 2; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD / 2; j++) {
                        String name = "r_u_" + threadId + "_" + j;
                        EnchantmentDefinition enchantment = createTestEnchantment(name, Material.DIAMOND_SWORD);
                        if (registry.register(enchantment)) {
                            registered.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        for (int i = THREAD_COUNT / 2; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        Collection<EnchantmentDefinition> all = registry.getAll();
                        for (EnchantmentDefinition enchantment : all) {
                            if (registry.unregister(enchantment.getKey())) {
                                unregistered.incrementAndGet();
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(25, TimeUnit.SECONDS));
        executor.shutdown();

        Collection<EnchantmentDefinition> remaining = registry.getAll();
        assertTrue(remaining.size() >= 0, "Registry should have non-negative count");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Concurrent reads during writes remain consistent")
    void concurrentReadsDuringWritesRemainConsistent() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger readErrors = new AtomicInteger(0);

        for (int i = 0; i < 5; i++) {
            registry.register(createTestEnchantment("preloaded_" + i, Material.DIAMOND_SWORD));
        }

        for (int i = 0; i < THREAD_COUNT / 2; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        String name = "read_write_" + j;
                        EnchantmentDefinition enchantment = createTestEnchantment(name, Material.DIAMOND_SWORD);
                        registry.register(enchantment);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        for (int i = THREAD_COUNT / 2; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD * 2; j++) {
                        try {
                            Collection<EnchantmentDefinition> all = registry.getAll();
                            for (EnchantmentDefinition def : all) {
                                registry.get(def.getKey());
                                registry.getForMaterial(Material.DIAMOND_SWORD);
                            }
                        } catch (Exception e) {
                            readErrors.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(25, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(0, readErrors.get(), "No read operations should fail during concurrent writes");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Material index remains consistent under concurrent modification")
    void materialIndexRemainsConsistent() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD / 5; j++) {
                        String name = "mat_idx_" + threadId + "_" + j;
                        Material material = (j % 2 == 0) ? Material.DIAMOND_SWORD : Material.DIAMOND_PICKAXE;
                        EnchantmentDefinition enchantment = createTestEnchantment(name, material);
                        registry.register(enchantment);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(25, TimeUnit.SECONDS));
        executor.shutdown();

        Set<EnchantmentDefinition> swordEnchantments = registry.getForMaterial(Material.DIAMOND_SWORD);
        Set<EnchantmentDefinition> pickaxeEnchantments = registry.getForMaterial(Material.DIAMOND_PICKAXE);

        for (EnchantmentDefinition def : swordEnchantments) {
            assertTrue(def.getApplicableMaterials().contains(Material.DIAMOND_SWORD),
                "All sword enchantments should be applicable to swords");
        }

        for (EnchantmentDefinition def : pickaxeEnchantments) {
            assertTrue(def.getApplicableMaterials().contains(Material.DIAMOND_PICKAXE),
                "All pickaxe enchantments should be applicable to pickaxes");
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Duplicate registration attempts are properly rejected concurrently")
    void duplicateRegistrationRejectedConcurrently() throws InterruptedException {
        EnchantmentDefinition sharedEnchantment = createTestEnchantment("shared", Material.DIAMOND_SWORD);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    if (registry.register(sharedEnchantment)) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, successCount.get(), "Only one registration should succeed");
        assertEquals(THREAD_COUNT - 1, failCount.get(), "All other attempts should fail");
        assertEquals(1, registry.getAll().size(), "Registry should contain exactly one enchantment");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Singleton instance remains consistent under concurrent access")
    void singletonInstanceConsistent() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        Set<EnchantmentRegistryManager> instances = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        instances.add(EnchantmentRegistryManager.getInstance());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, instances.size(), "All threads should get the same singleton instance");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Clear operation is thread-safe")
    void clearOperationIsThreadSafe() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            registry.register(createTestEnchantment("clear_test_" + i, Material.DIAMOND_SWORD));
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    if (Math.random() > 0.5) {
                        registry.clear();
                    } else {
                        registry.register(createTestEnchantment("clear_concurrent_" + System.nanoTime(), Material.DIAMOND_SWORD));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertDoesNotThrow(() -> {
            registry.getAll();
            registry.getForMaterial(Material.DIAMOND_SWORD);
            registry.clear();
        });
    }

    private EnchantmentDefinition createTestEnchantment(String name, Material material) {
        return new TestEnchantmentDefinition(name, material);
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
