package io.artificial.enchantments.api.scaling;

import org.jetbrains.annotations.NotNull;

/**
 * Defines a parameterized scaling algorithm that can be registered
 * and used to create {@link LevelScaling} instances.
 *
 * <p>Scaling algorithms encapsulate the logic for creating scaling formulas
 * with specific parameters. They provide metadata about the algorithm
 * including description and parameter requirements.
 *
 * <p><strong>Example Implementation:</strong>
 * <pre>{@code
 * ScalingAlgorithm linear = new ScalingAlgorithm() {
 *     @Override
 *     public LevelScaling create(double... params) {
 *         if (params.length < 2) {
 *             throw new IllegalArgumentException("LINEAR requires 2 parameters: base, increment");
 *         }
 *         return LevelScaling.linear(params[0], params[1]);
 *     }
 *
 *     @Override
 *     public String getDescription() {
 *         return "Linear scaling: base + (level - 1) * increment";
 *     }
 *
 *     @Override
 *     public int getParameterCount() {
 *         return 2;
 *     }
 *
 *     @Override
 *     public String[] getParameterNames() {
 *         return new String[]{"base", "increment"};
 *     }
 * };
 * }</pre>
 *
 * @see ScalingAlgorithmRegistry
 * @since 0.2.0
 */
public interface ScalingAlgorithm {

    /**
     * Creates a {@link LevelScaling} instance with the given parameters.
     *
     * <p>The parameters are algorithm-specific. The implementation should validate
     * parameters and throw {@link IllegalArgumentException} if they are invalid.
     *
     * @param params the algorithm-specific parameters
     * @return a configured LevelScaling instance
     * @throws IllegalArgumentException if parameters are invalid
     * @since 0.2.0
     */
    @NotNull
    LevelScaling create(double... params);

    /**
     * Gets a human-readable description of this algorithm.
     *
     * @return the algorithm description
     * @since 0.2.0
     */
    @NotNull
    String getDescription();

    /**
     * Gets the number of parameters this algorithm requires.
     *
     * @return the expected parameter count
     * @since 0.2.0
     */
    int getParameterCount();

    /**
     * Gets the names of parameters for this algorithm.
     *
     * @return an array of parameter names, same length as {@link #getParameterCount()}
     * @since 0.2.0
     */
    @NotNull
    String[] getParameterNames();
}
