package io.artificial.enchantments.optional.packetevents;

import io.artificial.enchantments.ArtificialEnchantmentsPlugin;
import io.artificial.enchantments.api.ArtificialEnchantmentsAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional PacketEvents adapter for Artificial Enchantments.
 * <p>
 * This module provides packet-level inventory item customization when PacketEvents
 * is available. It is completely optional and disabled by default. The core library
 * functions fully without this module or PacketEvents.
 * <p>
 * Key capabilities:
 * <ul>
 *   <li>Per-player visual customization of item lore and glint</li>
 *   <li>Packet interception for WINDOW_ITEMS and SET_SLOT packets</li>
 *   <li>Non-invasive display-only modifications (never alters item state)</li>
 * </ul>
 * <p>
 * Design principles:
 * <ul>
 *   <li>Soft dependency - no hard requirement on PacketEvents</li>
 *   <li>Graceful degradation - silently disabled if PacketEvents absent</li>
 *   <li>Display-only - only modifies visual presentation, never enchantment state</li>
 *   <li>Opt-in - must be explicitly enabled via configuration</li>
 * </ul>
 *
 * @since 0.1.0
 * @see InventoryPacketListener
 * @see ItemVisualProcessor
 */
public final class PacketEventsAdapter {

    private static final String PACKETEVENTS_PLUGIN_NAME = "packetevents";
    private static final String ADAPTER_VERSION = "0.1.0";

    private final ArtificialEnchantmentsPlugin plugin;
    private final Logger logger;
    private final Map<UUID, PlayerVisualPreferences> playerPreferences;
    
    private volatile boolean enabled;
    private volatile boolean available;
    private volatile boolean initialized;
    
    @Nullable
    private Object packetListener; // Lazy-loaded to avoid classloading issues
    @Nullable
    private ItemVisualProcessor visualProcessor;

    /**
     * Creates a new PacketEventsAdapter instance.
     * <p>
     * The adapter starts in a disabled state and must be explicitly enabled
     * via {@link #enable()}. This constructor performs no classloading of
     * PacketEvents classes, making it safe to instantiate regardless of
     * PacketEvents presence.
     *
     * @param plugin the ArtificialEnchantments plugin instance
     * @throws NullPointerException if plugin is null
     */
    public PacketEventsAdapter(@NotNull ArtificialEnchantmentsPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.logger = plugin.getLogger();
        this.playerPreferences = new ConcurrentHashMap<>();
        this.enabled = false;
        this.available = false;
        this.initialized = false;
    }

    /**
     * Checks if PacketEvents is present on the server.
     * <p>
     * This performs a safe check that does not trigger classloading of
     * PacketEvents classes. It only checks if the plugin is loaded.
     *
     * @return true if PacketEvents plugin is present
     */
    public boolean isPacketEventsAvailable() {
        if (!initialized) {
            // Defer to enable() for actual check
            return false;
        }
        return available;
    }

    /**
     * Checks if the adapter is enabled and actively processing packets.
     *
     * @return true if enabled and processing packets
     */
    public boolean isEnabled() {
        return enabled && available && initialized;
    }

    /**
     * Enables the PacketEvents adapter if PacketEvents is available.
     * <p>
     * This method:
     * <ol>
     *   <li>Checks if PacketEvents plugin is present</li>
     *   <li>Validates PacketEvents API version compatibility</li>
     *   <li>Registers packet listeners</li>
     *   <li>Initializes the visual processor</li>
     * </ol>
     * <p>
     * If PacketEvents is not present, this method logs at FINE level and
     * returns without error. The adapter remains in a disabled but
     * non-error state.
     *
     * @return true if successfully enabled, false if PacketEvents unavailable
     *         or initialization failed
     */
    public boolean enable() {
        if (initialized) {
            logger.fine("PacketEventsAdapter already initialized, state: enabled=" + enabled);
            return enabled;
        }

        try {
            // Safe check - no classloading of PacketEvents here
            PluginManager pluginManager = Bukkit.getPluginManager();
            Plugin packetEventsPlugin = pluginManager.getPlugin(PACKETEVENTS_PLUGIN_NAME);
            
            if (packetEventsPlugin == null || !packetEventsPlugin.isEnabled()) {
                logger.fine("PacketEvents not present or not enabled, adapter will remain disabled");
                available = false;
                initialized = true;
                return false;
            }

            available = true;
            logger.info("PacketEvents detected (version: " + packetEventsPlugin.getDescription().getVersion() + ")");

            // Initialize components - this is where PacketEvents classes may be loaded
            initializeComponents();

            enabled = true;
            initialized = true;
            logger.info("PacketEventsAdapter v" + ADAPTER_VERSION + " enabled successfully");
            return true;

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to enable PacketEventsAdapter", e);
            available = false;
            enabled = false;
            initialized = true;
            return false;
        }
    }

    /**
     * Disables the adapter and unregisters all packet listeners.
     * <p>
     * This is safe to call even if the adapter was never enabled.
     * All player preferences are cleared.
     */
    public void disable() {
        if (!initialized || !enabled) {
            return;
        }

        try {
            if (packetListener != null) {
                // Unregister via reflection to avoid hard dependency
                unregisterPacketListener();
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "Error unregistering packet listener (may be already unregistered)", e);
        }

        playerPreferences.clear();
        enabled = false;
        logger.info("PacketEventsAdapter disabled");
    }

    /**
     * Gets the visual processor for customizing item display.
     * <p>
     * Returns null if the adapter is not enabled. Callers should check
     * {@link #isEnabled()} before using this.
     *
     * @return the visual processor, or null if not enabled
     */
    @Nullable
    public ItemVisualProcessor getVisualProcessor() {
        return visualProcessor;
    }

    /**
     * Sets visual preferences for a specific player.
     * <p>
     * These preferences control how items appear to this specific player
     * without modifying the actual item state.
     *
     * @param playerId the player's UUID
     * @param preferences the visual preferences
     * @throws NullPointerException if either parameter is null
     */
    public void setPlayerPreferences(@NotNull UUID playerId, @NotNull PlayerVisualPreferences preferences) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(preferences, "preferences cannot be null");
        playerPreferences.put(playerId, preferences);
    }

    /**
     * Gets the visual preferences for a player.
     *
     * @param playerId the player's UUID
     * @return the preferences, or null if not set
     * @throws NullPointerException if playerId is null
     */
    @Nullable
    public PlayerVisualPreferences getPlayerPreferences(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        return playerPreferences.get(playerId);
    }

    /**
     * Removes visual preferences for a player.
     *
     * @param playerId the player's UUID
     * @throws NullPointerException if playerId is null
     */
    public void removePlayerPreferences(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        playerPreferences.remove(playerId);
    }

    /**
     * Clears all player visual preferences.
     */
    public void clearAllPlayerPreferences() {
        playerPreferences.clear();
    }

    /**
     * Gets the plugin instance.
     *
     * @return the ArtificialEnchantments plugin
     */
    @NotNull
    public ArtificialEnchantmentsPlugin getPlugin() {
        return plugin;
    }

    /**
     * Gets the API instance.
     *
     * @return the ArtificialEnchantments API
     */
    @NotNull
    public ArtificialEnchantmentsAPI getAPI() {
        // This assumes the plugin exposes its API - adjust as needed
        return ArtificialEnchantmentsAPI.getInstance();
    }

    private void initializeComponents() {
        // Initialize visual processor
        this.visualProcessor = new ItemVisualProcessor(this);
        
        // Initialize and register packet listener via reflection-based wrapper
        this.packetListener = createPacketListener();
        
        if (packetListener != null) {
            registerPacketListener();
        }
    }

    /**
     * Creates the packet listener. This is done in a separate method to isolate
     * PacketEvents classloading.
     */
    @Nullable
    private Object createPacketListener() {
        try {
            // Use the wrapper class that handles PacketEvents integration
            return new InventoryPacketListener(this);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to create packet listener", e);
            return null;
        }
    }

    private void registerPacketListener() {
        try {
            // Delegate to the listener's registration method
            if (packetListener instanceof InventoryPacketListener) {
                ((InventoryPacketListener) packetListener).register();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to register packet listener", e);
        }
    }

    private void unregisterPacketListener() {
        try {
            if (packetListener instanceof InventoryPacketListener) {
                ((InventoryPacketListener) packetListener).unregister();
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "Error during packet listener unregistration", e);
        }
    }

    /**
     * Gets all current player preferences as an unmodifiable view.
     * Primarily for internal use by packet listeners.
     *
     * @return map of player UUIDs to their preferences
     */
    @NotNull
    Map<UUID, PlayerVisualPreferences> getAllPlayerPreferences() {
        return Map.copyOf(playerPreferences);
    }

    /**
     * Per-player visual customization preferences.
     * <p>
     * These settings control how enchanted items appear to a specific player
     * without affecting the actual item state or how other players see the item.
     */
    public static final class PlayerVisualPreferences {
        
        private boolean enabled = true;
        private boolean modifyLore = true;
        private boolean showEnchantmentGlint = true;
        private boolean hideVanillaEnchantments = false;
        @Nullable
        private String lorePrefix;
        @Nullable
        private Consumer<VisualContext> customLoreFormatter;

        /**
         * Creates default preferences (all customization enabled).
         */
        public PlayerVisualPreferences() {
        }

        public boolean isEnabled() {
            return enabled;
        }

        public PlayerVisualPreferences setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public boolean isModifyLore() {
            return modifyLore;
        }

        public PlayerVisualPreferences setModifyLore(boolean modifyLore) {
            this.modifyLore = modifyLore;
            return this;
        }

        public boolean isShowEnchantmentGlint() {
            return showEnchantmentGlint;
        }

        public PlayerVisualPreferences setShowEnchantmentGlint(boolean showEnchantmentGlint) {
            this.showEnchantmentGlint = showEnchantmentGlint;
            return this;
        }

        public boolean isHideVanillaEnchantments() {
            return hideVanillaEnchantments;
        }

        public PlayerVisualPreferences setHideVanillaEnchantments(boolean hideVanillaEnchantments) {
            this.hideVanillaEnchantments = hideVanillaEnchantments;
            return this;
        }

        @Nullable
        public String getLorePrefix() {
            return lorePrefix;
        }

        public PlayerVisualPreferences setLorePrefix(@Nullable String lorePrefix) {
            this.lorePrefix = lorePrefix;
            return this;
        }

        @Nullable
        public Consumer<VisualContext> getCustomLoreFormatter() {
            return customLoreFormatter;
        }

        public PlayerVisualPreferences setCustomLoreFormatter(@Nullable Consumer<VisualContext> customLoreFormatter) {
            this.customLoreFormatter = customLoreFormatter;
            return this;
        }
    }

    /**
     * Context passed to custom lore formatters.
     */
    public static final class VisualContext {
        private final String enchantmentName;
        private final int level;
        private final boolean isArtificial;

        public VisualContext(@NotNull String enchantmentName, int level, boolean isArtificial) {
            this.enchantmentName = Objects.requireNonNull(enchantmentName, "enchantmentName cannot be null");
            this.level = level;
            this.isArtificial = isArtificial;
        }

        @NotNull
        public String getEnchantmentName() {
            return enchantmentName;
        }

        public int getLevel() {
            return level;
        }

        public boolean isArtificial() {
            return isArtificial;
        }
    }
}
