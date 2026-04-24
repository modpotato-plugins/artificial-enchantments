package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.event.RegistryFreezeEvent;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import net.kyori.adventure.key.Key;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Paper 1.21+ PluginBootstrap implementation for registering custom enchantments
 * via the native registry system during the bootstrap phase.
 *
 * <p>This bootstrap handles RegistryEvents.ENCHANTMENT.freeze() to convert library
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
                RegistryEvents.ENCHANTMENT.freeze(),
                event -> {
                    LOGGER.info("[ArtificialEnchantments] Processing enchantment registry freeze event...");

                    EnchantmentRegistryManager registryManager = EnchantmentRegistryManager.getInstance();

                    for (EnchantmentDefinition definition : registryManager.getPendingRegistrations()) {
                        try {
                            Key key = Key.key(
                                    definition.getKey().getNamespace(),
                                    definition.getKey().getKey()
                            );

                            event.registry().register(
                                    EnchantmentKeys.create(key),
                                    b -> PaperEnchantmentConverter.convertToBuilder(definition, b, event)
                            );

                            registryManager.markNativeRegistered(definition.getKey());
                            LOGGER.info("[ArtificialEnchantments] Registered native enchantment: " + definition.getKey());
                        } catch (Exception e) {
                            LOGGER.severe("[ArtificialEnchantments] Failed to register enchantment " + definition.getKey() + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    LOGGER.info("[ArtificialEnchantments] Enchantment registry freeze processing complete");
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
