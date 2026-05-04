package io.artificial.enchantments.internal;

import be.seeseemelk.mockbukkit.MockBukkit;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
@DisplayName("ArtificialEnchantmentsAPIImpl Tests")
class ArtificialEnchantmentsAPIImplTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        ArtificialEnchantmentsAPIImpl.resetForTesting();
        EnchantmentRegistryManager.getInstance().clear();
    }

    @AfterEach
    void tearDown() {
        ArtificialEnchantmentsAPIImpl.resetForTesting();
        EnchantmentRegistryManager.getInstance().clear();
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Concurrent create returns a single shared instance")
    void concurrentCreateReturnsSingleSharedInstance() throws InterruptedException {
        Plugin plugin = MockBukkit.createMockPlugin();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(8);
        Set<ArtificialEnchantmentsAPIImpl> instances = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < 8; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    instances.add(ArtificialEnchantmentsAPIImpl.create(plugin));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        executor.shutdownNow();

        assertEquals(1, instances.size());
        ArtificialEnchantmentsAPIImpl instance = instances.iterator().next();
        assertNotNull(instance);
        assertSame(instance, ArtificialEnchantmentsAPIImpl.getInstance());
    }
}
