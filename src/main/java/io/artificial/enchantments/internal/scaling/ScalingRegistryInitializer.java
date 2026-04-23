package io.artificial.enchantments.internal.scaling;

import io.artificial.enchantments.api.scaling.ScalingAlgorithmRegistry;

/**
 * Lazy initializer for the default scaling registry.
 *
 * <p>Provides a lazily-initialized default registry instance using the
 * initialization-on-demand holder idiom for thread safety.
 *
 * @since 0.2.0
 */
final class ScalingRegistryInitializer {

    private ScalingRegistryInitializer() {
        // Prevent instantiation
    }

    /**
     * Gets the default registry instance.
     *
     * @return the default registry
     */
    static ScalingAlgorithmRegistry getDefaultRegistry() {
        return Holder.INSTANCE;
    }

    /**
     * Initialization-on-demand holder idiom.
     */
    private static final class Holder {
        static final ScalingAlgorithmRegistry INSTANCE = new ScalingAlgorithmRegistryImpl();
    }
}
