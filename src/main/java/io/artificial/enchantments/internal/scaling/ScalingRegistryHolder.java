package io.artificial.enchantments.internal.scaling;

import io.artificial.enchantments.api.scaling.ScalingAlgorithmRegistry;

/**
 * Holder for the scaling algorithm registry instance.
 *
 * <p>This class provides access to the registry for internal components
 * without making it a global static singleton. The registry is set during
 * API initialization and remains immutable thereafter.
 *
 * @since 0.2.0
 */
public final class ScalingRegistryHolder {

    private static volatile ScalingAlgorithmRegistry registry;
    private static volatile boolean initialized = false;

    private ScalingRegistryHolder() {
        // Prevent instantiation
    }

    /**
     * Initializes the holder with the registry instance.
     *
     * <p>This should be called once during API initialization.
     *
     * @param registryInstance the registry instance to use
     * @throws IllegalStateException if already initialized
     */
    public static void initialize(ScalingAlgorithmRegistry registryInstance) {
        if (initialized) {
            throw new IllegalStateException("ScalingRegistryHolder already initialized");
        }
        if (registryInstance == null) {
            throw new IllegalArgumentException("registry must not be null");
        }
        registry = registryInstance;
        initialized = true;
    }

    /**
     * Gets the registry instance.
     *
     * <p>If not initialized, returns a default registry.
     *
     * @return the registry instance
     */
    public static ScalingAlgorithmRegistry getRegistry() {
        if (!initialized || registry == null) {
            return ScalingRegistryInitializer.getDefaultRegistry();
        }
        return registry;
    }

    /**
     * Checks if the holder has been initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
