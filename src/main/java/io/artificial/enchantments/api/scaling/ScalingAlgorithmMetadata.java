package io.artificial.enchantments.api.scaling;

import org.jetbrains.annotations.NotNull;

/**
 * Immutable metadata for a registered scaling algorithm.
 *
 * <p>This class provides read-only information about a scaling algorithm
 * including its name, description, and parameter requirements.
 *
 * @since 0.2.0
 */
public final class ScalingAlgorithmMetadata {

    private final String name;
    private final String description;
    private final int parameterCount;
    private final String[] parameterNames;
    private final boolean builtIn;

    /**
     * Creates metadata for a scaling algorithm.
     *
     * @param name the algorithm name
     * @param description the algorithm description
     * @param parameterCount the number of parameters required
     * @param parameterNames the names of parameters
     * @param builtIn true if this is a built-in algorithm
     */
    public ScalingAlgorithmMetadata(
        @NotNull String name,
        @NotNull String description,
        int parameterCount,
        @NotNull String[] parameterNames,
        boolean builtIn
    ) {
        this.name = name;
        this.description = description;
        this.parameterCount = parameterCount;
        this.parameterNames = parameterNames.clone();
        this.builtIn = builtIn;
    }

    /**
     * Gets the algorithm name.
     *
     * @return the name
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Gets the algorithm description.
     *
     * @return the description
     */
    @NotNull
    public String getDescription() {
        return description;
    }

    /**
     * Gets the number of parameters required.
     *
     * @return the parameter count
     */
    public int getParameterCount() {
        return parameterCount;
    }

    /**
     * Gets the parameter names.
     *
     * @return a copy of the parameter names array
     */
    @NotNull
    public String[] getParameterNames() {
        return parameterNames.clone();
    }

    /**
     * Checks if this is a built-in algorithm.
     *
     * @return true if built-in
     */
    public boolean isBuiltIn() {
        return builtIn;
    }

    @Override
    @NotNull
    public String toString() {
        return "ScalingAlgorithmMetadata[name=" + name +
               ", description=" + description +
               ", parameters=" + parameterCount +
               ", builtIn=" + builtIn + "]";
    }
}
