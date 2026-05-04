package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.scaling.LevelScaling;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("NativeFirstItemStorage Tests")
class NativeFirstItemStorageTest {

    private EnchantmentRegistryManager registry;
    private NativeFirstItemStorage storage;

    @BeforeEach
    void setUp() {
        registry = EnchantmentRegistryManager.getInstance();
        registry.clear();
    }

    @AfterEach
    void tearDown() {
        registry.clear();
    }

    @Test
    @DisplayName("Enchanted books bypass target material applicability")
    void enchantedBooksBypassTargetMaterialApplicability() {
        NamespacedKey key = NamespacedKey.minecraft("sharpness");
        storage = new NativeFirstItemStorage(() -> registry, new NbtMetadataStorage(), lookupKey -> null);

        EnchantmentDefinition definition = EnchantmentDefinition.builder()
                .key(key)
                .displayName(Component.text("Sharpness"))
                .scaling(LevelScaling.linear(1.0, 1.0))
                .applicable(Material.DIAMOND_SWORD)
                .minLevel(1)
                .maxLevel(5)
                .build();

        ItemStack book = mock(ItemStack.class);
        ItemStack clonedBook = mock(ItemStack.class);
        EnchantmentStorageMeta meta = mock(EnchantmentStorageMeta.class);
        when(book.getType()).thenReturn(Material.ENCHANTED_BOOK);
        when(book.clone()).thenReturn(clonedBook);
        when(clonedBook.getType()).thenReturn(Material.ENCHANTED_BOOK);
        when(clonedBook.getItemMeta()).thenReturn(meta);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> storage.applyEnchantment(book, definition, 2)
        );

        assertTrue(exception.getMessage().contains("not registered in native registry"));
    }
}
