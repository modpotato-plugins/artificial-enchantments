package io.artificial.enchantments;

import io.artificial.enchantments.optional.packetevents.PacketEventsAdapter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Main plugin class for Artificial Enchantments library.
 *
 * <p>This plugin provides custom enchantment capabilities for Paper 1.21+ servers.
 * It supports optional PacketEvents integration for per-player visual customization.
 *
 * @since 0.1.0
 */
public class ArtificialEnchantmentsPlugin extends JavaPlugin {

    @Nullable
    private PacketEventsAdapter packetEventsAdapter;

    @Override
    public void onEnable() {
        getLogger().info("Artificial Enchantments library enabled");

        // Initialize optional PacketEvents adapter
        initializePacketEventsAdapter();
    }

    @Override
    public void onDisable() {
        // Disable PacketEvents adapter if it was enabled
        if (packetEventsAdapter != null) {
            packetEventsAdapter.disable();
            packetEventsAdapter = null;
        }

        getLogger().info("Artificial Enchantments library disabled");
    }

    /**
     * Gets the PacketEvents adapter if available and enabled.
     *
     * @return the adapter, or null if PacketEvents is not present or not enabled
     */
    @Nullable
    public PacketEventsAdapter getPacketEventsAdapter() {
        return packetEventsAdapter;
    }

    /**
     * Checks if PacketEvents integration is active.
     *
     * @return true if PacketEvents adapter is enabled and ready
     */
    public boolean isPacketEventsActive() {
        return packetEventsAdapter != null && packetEventsAdapter.isEnabled();
    }

    private void initializePacketEventsAdapter() {
        try {
            packetEventsAdapter = new PacketEventsAdapter(this);
            boolean enabled = packetEventsAdapter.enable();

            if (enabled) {
                getLogger().info("PacketEvents integration enabled");
            } else {
                getLogger().fine("PacketEvents not available or disabled, continuing without packet-level features");
            }
        } catch (Exception e) {
            getLogger().warning("Failed to initialize PacketEvents adapter: " + e.getMessage());
            packetEventsAdapter = null;
        }
    }
}
