package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.scaling.LevelScaling;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("EnchantmentDefinitionBuilder Validation Tests")
class EnchantmentDefinitionBuilderValidationTest {

    private static final String TEST_NAMESPACE = "test";
    private static final NamespacedKey TEST_KEY = new NamespacedKey(TEST_NAMESPACE, "test_enchant");
    private static final Component TEST_NAME = Component.text("Test Enchantment");
    private static final LevelScaling TEST_SCALING = LevelScaling.linear(1.0, 0.5);

    private EnchantmentDefinition.Builder builder;

    @BeforeEach
    void setUp() {
        builder = EnchantmentDefinition.builder();
    }

    @Test
    @DisplayName("Empty builder has validation errors for all required fields")
    void emptyBuilderHasValidationErrors() {
        List<String> errors = builder.getValidationErrors();

        assertFalse(errors.isEmpty(), "Empty builder should have validation errors");
        assertTrue(errors.stream().anyMatch(e -> e.contains("key")), "Should error about missing key");
        assertTrue(errors.stream().anyMatch(e -> e.contains("displayName")), "Should error about missing displayName");
        assertTrue(errors.stream().anyMatch(e -> e.contains("scaling")), "Should error about missing scaling");
        assertTrue(errors.stream().anyMatch(e -> e.contains("material")), "Should error about missing materials");
    }

    @Test
    @DisplayName("isValid() returns false for empty builder")
    void isValidReturnsFalseForEmptyBuilder() {
        assertFalse(builder.isValid(), "Empty builder should not be valid");
    }

    @Test
    @DisplayName("validate() throws for empty builder with detailed message")
    void validateThrowsForEmptyBuilder() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> builder.validate());
        assertTrue(exception.getMessage().contains("validation failed"));
        assertTrue(exception.getMessage().contains("key") || exception.getMessage().contains("Required field"));
    }

    @Test
    @DisplayName("Builder with all required fields has no validation errors")
    void builderWithAllRequiredFieldsIsValid() {
        builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(TEST_SCALING)
            .applicable(Material.DIAMOND_SWORD)
            .minLevel(1)
            .maxLevel(5);

        List<String> errors = builder.getValidationErrors();
        List<String> criticalErrors = errors.stream()
            .filter(e -> !e.startsWith("WARNING"))
            .toList();
        assertTrue(criticalErrors.isEmpty(),
            "Builder with all required fields should have no critical errors, but got: " + criticalErrors);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5, -100})
    @DisplayName("minLevel < 1 produces validation error")
    void minLevelLessThanOneIsInvalid(int minLevel) {
        builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(TEST_SCALING)
            .applicable(Material.DIAMOND_SWORD)
            .minLevel(minLevel);

        List<String> errors = builder.getValidationErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("minLevel") && e.contains(">= 1")),
            "Should error about minLevel >= 1, but got: " + errors);
    }

    @Test
    @DisplayName("maxLevel <= minLevel produces validation error")
    void maxLevelLessThanOrEqualToMinLevelIsInvalid() {
        builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(TEST_SCALING)
            .applicable(Material.DIAMOND_SWORD)
            .minLevel(5)
            .maxLevel(3);

        List<String> errors = builder.getValidationErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("maxLevel") && e.contains("minLevel")),
            "Should error about maxLevel > minLevel, but got: " + errors);
    }

    @Test
    @DisplayName("maxLevel > 255 produces validation error")
    void maxLevelExceedsMinecraftMaximum() {
        builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(TEST_SCALING)
            .applicable(Material.DIAMOND_SWORD)
            .minLevel(1)
            .maxLevel(300);

        List<String> errors = builder.getValidationErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("255")),
            "Should error about maxLevel exceeding 255, but got: " + errors);
    }

    @Test
    @DisplayName("Valid level bounds produce no errors")
    void validLevelBoundsAreAccepted() {
        builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(TEST_SCALING)
            .applicable(Material.DIAMOND_SWORD)
            .minLevel(1)
            .maxLevel(5);

        List<String> errors = builder.getValidationErrors();
        assertTrue(errors.stream().noneMatch(e -> e.contains("minLevel") || e.contains("maxLevel")),
            "Valid level bounds should not produce errors, but got: " + errors);
    }

    @Test
    @DisplayName("Empty materials set produces validation error")
    void emptyMaterialsIsInvalid() {
        builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(TEST_SCALING)
            .applicable(new Material[0]);

        List<String> errors = builder.getValidationErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("material")),
            "Should error about empty materials, but got: " + errors);
    }

    @Test
    @DisplayName("Non-item materials produce validation error")
    void nonItemMaterialsProduceError() {
        builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(TEST_SCALING)
            .applicable(Material.WATER, Material.DIAMOND_SWORD);

        List<String> errors = builder.getValidationErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("not items")),
            "Should error about non-item materials, but got: " + errors);
    }

    @Test
    @DisplayName("Valid item materials produce no errors")
    void validItemMaterialsAreAccepted() {
        builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(TEST_SCALING)
            .applicable(Material.DIAMOND_SWORD, Material.IRON_SWORD, Material.NETHERITE_SWORD);

        List<String> errors = builder.getValidationErrors();
        assertTrue(errors.stream().noneMatch(e -> e.contains("material") && !e.startsWith("WARNING")),
            "Valid materials should not produce errors, but got: " + errors);
    }

    @Test
    @DisplayName("Self-conflict produces validation error")
    void selfConflictIsInvalid() {
        builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(TEST_SCALING)
            .applicable(Material.DIAMOND_SWORD)
            .conflictsWith(TEST_KEY);

        List<String> errors = builder.getValidationErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("cannot conflict with itself")),
            "Should error about self-conflict, but got: " + errors);
    }

    @Test
    @DisplayName("Conflict with different key produces no error")
    void conflictWithDifferentKeyIsValid() {
        NamespacedKey otherKey = new NamespacedKey(TEST_NAMESPACE, "other_enchant");

        builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(TEST_SCALING)
            .applicable(Material.DIAMOND_SWORD)
            .conflictsWith(otherKey);

        List<String> errors = builder.getValidationErrors();
        assertTrue(errors.stream().noneMatch(e -> e.contains("conflict") && !e.startsWith("WARNING")),
            "Conflict with other key should be valid, but got: " + errors);
    }

    @Test
    @DisplayName("Curse + tradeable produces warning")
    void curseWithTradeableProducesWarning() {
        builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(TEST_SCALING)
            .applicable(Material.DIAMOND_SWORD)
            .curse()
            .tradeable(true);

        List<String> errors = builder.getValidationErrors();
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("WARNING") && e.contains("Curse") && e.contains("tradeable")),
            "Should warn about curse + tradeable, but got: " + errors);
    }

    @Test
    @DisplayName("High maxLevel produces warning")
    void highMaxLevelProducesWarning() {
        builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(TEST_SCALING)
            .applicable(Material.DIAMOND_SWORD)
            .minLevel(1)
            .maxLevel(50);

        List<String> errors = builder.getValidationErrors();
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("WARNING") && e.contains("maxLevel")),
            "Should warn about high maxLevel, but got: " + errors);
    }

    @Test
    @DisplayName("Warnings do not prevent build()")
    void warningsDoNotPreventBuild() {
        EnchantmentDefinition definition = builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(TEST_SCALING)
            .applicable(Material.DIAMOND_SWORD)
            .curse()
            .tradeable(true)
            .minLevel(1)
            .maxLevel(50)
            .build();

        assertNotNull(definition);
        assertTrue(definition.isCurse());
        assertTrue(definition.isTradeable());
    }

    @Test
    @DisplayName("Scaling producing NaN produces validation error")
    void scalingProducingNaNIsInvalid() {
        Function<Integer, Double> badFormula = level -> Double.NaN;

        builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(badFormula)
            .applicable(Material.DIAMOND_SWORD);

        List<String> errors = builder.getValidationErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("NaN")),
            "Should error about NaN scaling values, but got: " + errors);
    }

    @Test
    @DisplayName("Scaling producing Infinity produces validation error")
    void scalingProducingInfinityIsInvalid() {
        Function<Integer, Double> badFormula = level -> Double.POSITIVE_INFINITY;

        builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(badFormula)
            .applicable(Material.DIAMOND_SWORD);

        List<String> errors = builder.getValidationErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("non-finite") || e.contains("Infinity")),
            "Should error about non-finite scaling values, but got: " + errors);
    }

    @Test
    @DisplayName("Valid scaling produces no errors")
    void validScalingIsAccepted() {
        builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(LevelScaling.linear(1.0, 0.5))
            .applicable(Material.DIAMOND_SWORD);

        List<String> errors = builder.getValidationErrors();
        assertTrue(errors.stream().noneMatch(e -> e.contains("scaling") && !e.startsWith("WARNING")),
            "Valid scaling should not produce errors, but got: " + errors);
    }

    @Test
    @DisplayName("build() succeeds with valid configuration")
    void buildSucceedsWithValidConfiguration() {
        EnchantmentDefinition definition = builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(TEST_SCALING)
            .applicable(Material.DIAMOND_SWORD)
            .minLevel(1)
            .maxLevel(5)
            .build();

        assertNotNull(definition);
        assertEquals(TEST_KEY, definition.getKey());
        assertEquals(TEST_NAME, definition.getDisplayName());
        assertEquals(1, definition.getMinLevel());
        assertEquals(5, definition.getMaxLevel());
    }

    @Test
    @DisplayName("build() throws with detailed message on validation errors")
    void buildThrowsWithDetailedMessage() {
        builder
            .key(TEST_KEY)
            .displayName(TEST_NAME);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> builder.build());
        assertTrue(exception.getMessage().contains("Cannot build"));
        assertTrue(exception.getMessage().contains("error(s)"));
    }

    @Test
    @DisplayName("build() ignores warnings and succeeds")
    void buildIgnoresWarnings() {
        EnchantmentDefinition definition = builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(TEST_SCALING)
            .applicable(Material.DIAMOND_SWORD)
            .minLevel(1)
            .maxLevel(5)
            .curse()
            .tradeable(true)
            .build();

        assertNotNull(definition);
        assertTrue(definition.isCurse());
        assertTrue(definition.isTradeable());
    }

    @Test
    @DisplayName("Existing valid builder pattern still works")
    void existingBuilderPatternWorks() {
        EnchantmentDefinition definition = EnchantmentDefinition.builder()
            .key(new NamespacedKey("myplugin", "life_steal"))
            .displayName(Component.text("Life Steal"))
            .description(Component.text("Heals you when dealing damage"))
            .minLevel(1)
            .maxLevel(5)
            .scaling(LevelScaling.linear(0.1, 0.05))
            .applicable(Material.DIAMOND_SWORD, Material.IRON_SWORD)
            .rarity(EnchantmentDefinition.Rarity.RARE)
            .build();

        assertNotNull(definition);
        assertEquals("life_steal", definition.getKey().getKey());
        assertEquals(1, definition.getMinLevel());
        assertEquals(5, definition.getMaxLevel());
    }

    @Test
    @DisplayName("Chained builder calls return builder instance")
    void chainedBuilderCallsReturnBuilder() {
        EnchantmentDefinition.Builder result = builder
            .key(TEST_KEY)
            .displayName(TEST_NAME)
            .scaling(TEST_SCALING)
            .applicable(Material.DIAMOND_SWORD);

        assertSame(builder, result, "Builder methods should return same builder for chaining");
    }
}
