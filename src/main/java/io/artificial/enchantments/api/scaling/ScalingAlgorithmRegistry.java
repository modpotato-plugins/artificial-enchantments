package io.artificial.enchantments.api.scaling;

import org.jetbrains.annotations.NotNull;
import java.util.Optional;
import java.util.Set;

/**
 * Registry for named scaling algorithms.
 * 
 * <p>This registry allows developers to register and use named scaling algorithms
 * that can be retrieved by name and instantiated with parameters. Built-in
 * algorithms are pre-registered on startup.
 *
 * <p><strong>Built-in Algorithms:</strong>
 * <ul>
 *   <li>{@code LINEAR} - Linear scaling: base + (level - 1) * increment</li>
 *   <li>{@code EXPONENTIAL} - Exponential scaling: base * multiplier^(level - 1)</li>
 *   <li>{@code DIMINISHING} - Diminishing returns: max * (level / (level + factor))</li>
 *   <li>{@code CONSTANT} - Constant value at all levels</li>
 *   <li>{@code STEPPED} - Stepped values with interpolation</li>
 *   <li>{@code DECAYING} - Exponential decay: max * (1 - decayFactor^level)</li>
 * </ul>
 *
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // Using built-in algorithm
 * EnchantmentDefinition def = EnchantmentDefinition.builder()
 *     .key(key)
 *     .displayName(name)
 *     .minLevel(1)
 *     .maxLevel(5)
 *     .scaling("LINEAR", 1.0, 0.5)  // base=1.0, increment=0.5
 *     .applicable(Material.DIAMOND_SWORD)
 *     .build();
 *
 * // Registering custom algorithm
 * registry.register("CUSTOM", new ScalingAlgorithm() {
 *     public LevelScaling create(double... params) {
 *         return level -> params[0] * Math.log(level + params[1]);
 *     }
 *     public String getDescription() { return "Logarithmic scaling"; }
 *     public int getParameterCount() { return 2; }
 *     public String[] getParameterNames() { return new String[]{"coefficient", "offset"}; }
 * });
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> All registry operations are thread-safe.
 *
 * @see ScalingAlgorithm
 * @see LevelScaling
 * @since 0.2.0
 */
public interface ScalingAlgorithmRegistry {

    /**
     * Registers a custom scaling algorithm with a unique name.
     *
     * <p>The algorithm name must be unique and non-empty. Attempting to register
     * an algorithm with a name that already exists will throw an exception.
     *
     * @param name the unique algorithm name (must not be null or empty)
     * @param algorithm the algorithm implementation (must not be null)
     * @throws IllegalArgumentException if name is null/empty or algorithm is null
     * @throws IllegalStateException if an algorithm with this name already exists
     * @since 0.2.0
     */
    void register(@NotNull String name, @NotNull ScalingAlgorithm algorithm);

    /**
     * Gets a scaling algorithm by name with the specified parameters.
     *
     * <p>The algorithm creates a {@link LevelScaling} instance configured with
     * the provided parameters. The parameters are validated to ensure they
     * produce finite values.
     *
     * @param name the algorithm name (must not be null or empty)
     * @param params the algorithm-specific parameters
     * @return a configured LevelScaling instance
     * @throws IllegalArgumentException if algorithm not found or parameters invalid
     * @since 0.2.0
     */
    @NotNull
    LevelScaling get(@NotNull String name, double... params);

    /**
     * Checks if an algorithm with the specified name is registered.
     *
     * @param name the algorithm name to check (must not be null)
     * @return true if the algorithm exists
     * @since 0.2.0
     */
    boolean hasAlgorithm(@NotNull String name);

    /**
     * Gets all registered algorithm names.
     *
     * @return an unmodifiable set of all algorithm names
     * @since 0.2.0
     */
    @NotNull
    Set<String> getRegisteredNames();

    /**
     * Gets the metadata for a registered algorithm.
     *
     * @param name the algorithm name (must not be null)
     * @return the algorithm metadata if found, empty otherwise
     * @since 0.2.0
     */
    @NotNull
    Optional<ScalingAlgorithmMetadata> getMetadata(@NotNull String name);

    /**
     * Unregisters a custom algorithm by name.
     *
     * <p>Built-in algorithms cannot be unregistered. Attempting to unregister
     * a built-in algorithm will return false.
     *
     * @param name the algorithm name to unregister (must not be null)
     * @return true if the algorithm was unregistered, false if not found or built-in
     * @since 0.2.0
     */
    boolean unregister(@NotNull String name);
}
