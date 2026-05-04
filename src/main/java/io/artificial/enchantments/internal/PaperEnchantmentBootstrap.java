package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import net.kyori.adventure.key.Key;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Paper 1.21+ PluginBootstrap implementation for registering custom enchantments
 * via the native registry system during the bootstrap phase.
 *
 * <p>This bootstrap handles RegistryEvents.ENCHANTMENT.compose() to convert library
 * EnchantmentDefinitions into native Paper enchantment registry entries.
 *
 * <p>Requires {@code load: STARTUP} in plugin.yml.
 */
public class PaperEnchantmentBootstrap implements PluginBootstrap {

    private static final Logger LOGGER = Logger.getLogger("ArtificialEnchantments");

    /**
     * Creates a new Paper enchantment bootstrap.
     *
     * @since 0.2.0
     */
    public PaperEnchantmentBootstrap() {
    }

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        LOGGER.info("[ArtificialEnchantments] Initializing Paper native enchantment bootstrap...");

        context.getLifecycleManager().registerEventHandler(
                RegistryEvents.ENCHANTMENT.compose(),
                event -> {
                    LOGGER.info("[ArtificialEnchantments] Processing enchantment registry compose event...");

                    EnchantmentRegistryManager registryManager = EnchantmentRegistryManager.getInstance();

                    try {
                        for (EnchantmentDefinition definition : registryManager.getPendingRegistrations()) {
                            boolean registered = false;
                            try {
                                Key key = Key.key(
                                        definition.getKey().getNamespace(),
                                        definition.getKey().getKey()
                                );

                                event.registry().register(
                                        EnchantmentKeys.create(key),
                                        b -> PaperEnchantmentConverter.convertToBuilder(definition, b, event)
                                );

                                registered = true;
                                LOGGER.info("[ArtificialEnchantments] Registered native enchantment: " + definition.getKey());
                            } catch (Exception e) {
                                LOGGER.severe("[ArtificialEnchantments] Failed to register enchantment " + definition.getKey() + ": " + e.getMessage());
                                e.printStackTrace();
                            } finally {
                                if (registered) {
                                    registryManager.markNativeRegistered(definition.getKey());
                                } else {
                                    // Clear from pending so stale entries don't accumulate.
                                    registryManager.clearPendingNativeRegistration(definition.getKey());
                                }
                            }
                        }
                    } finally {
                        registryManager.markNativeRegistrationClosed();
                    }

                    LOGGER.info("[ArtificialEnchantments] Enchantment registry compose processing complete");
                }
        );

        LOGGER.info("[ArtificialEnchantments] Bootstrap phase registered for enchantment registry");
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        LOGGER.info("[ArtificialEnchantments] Creating plugin instance after bootstrap...");
        return new io.artificial.enchantments.ArtificialEnchantmentsPlugin();
    }
}
