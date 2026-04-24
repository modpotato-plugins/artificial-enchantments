package io.artificial.enchantments;

import io.artificial.enchantments.api.ArtificialEnchantmentsAPI;
import io.artificial.enchantments.api.ItemStorage;
import io.artificial.enchantments.internal.BukkitFoliaScheduler;
import io.artificial.enchantments.internal.EffectDispatchSpine;
import io.artificial.enchantments.internal.EnchantmentEffectListener;
import io.artificial.enchantments.internal.EnchantmentTickTask;
import io.artificial.enchantments.internal.EnchantmentRegistryManager;
import io.artificial.enchantments.internal.FoliaScheduler;
import io.artificial.enchantments.internal.ItemEnchantmentService;
import io.artificial.enchantments.internal.PaperRegistryBridge;
import io.artificial.enchantments.internal.anvil.AnvilListener;
import io.artificial.enchantments.internal.enchanttable.EnchantmentTableListener;
import io.artificial.enchantments.internal.loot.BlockBreakLootHandler;
import io.artificial.enchantments.internal.loot.BlockBreakLootListener;
import io.artificial.enchantments.optional.packetevents.PacketEventsAdapter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Main plugin class for Artificial Enchantments library.
 *
 * <p>This plugin provides custom enchantment capabilities for Paper 1.21+ servers.
 * It initializes the API on enable, registers all event listeners, and manages
 * the lifecycle of internal services including the optional PacketEvents integration.
 *
 * <p><strong>Lifecycle:</strong><br>
 * 1. {@link #onEnable()} - Initializes API, registers listeners, starts tick task<br>
 * 2. {@link #onDisable()} - Stops tick task, disables PacketEvents, shuts down spine<br>
 *
 * <p><strong>Services:</strong><br>
 * - Enchantment table listener for custom enchantments in tables<br>
 * - Anvil listener for combining and applying enchantments<br>
 * - Block break loot listener for drop modifications<br>
 * - Effect listener for dispatching enchantment effects<br>
 * - Tick task for periodic held/armor effects<br>
 * - Optional PacketEvents adapter for per-player visuals
 *
 * @see ArtificialEnchantmentsAPI
 * @since 0.1.0
 */
public class ArtificialEnchantmentsPlugin extends JavaPlugin {

    /** PacketEvents adapter for optional packet-level features. Null if not available. */
    @Nullable
    private PacketEventsAdapter packetEventsAdapter;

    /** Effect dispatch spine for routing enchantment effects. */
    @Nullable
    private EffectDispatchSpine effectDispatchSpine;

    /** Tick task for periodic held item and armor effects. */
    @Nullable
    private EnchantmentTickTask tickTask;

    /** Service for managing item enchantment operations. */
    @Nullable
    private ItemEnchantmentService itemEnchantmentService;

    /**
     * Creates a new plugin instance.
     *
     * <p>This constructor is called by Bukkit's plugin loader. The actual
     * initialization happens in {@link #onEnable()}.
     */
    public ArtificialEnchantmentsPlugin() {
    }

    /**
     * Enables the plugin, initializing the API and registering all listeners.
     *
     * <p>This method:
     * <ol>
     *   <li>Creates or retrieves the shared API instance</li>
     *   <li>Registers enchantment table listener for custom enchantments</li>
     *   <li>Registers anvil listener for enchantment combination</li>
     *   <li>Registers block break loot listener for drop modifications</li>
     *   <li>Registers effect listener for enchantment triggers</li>
     *   <li>Starts the tick task for periodic effects</li>
     *   <li>Initializes PacketEvents integration if available</li>
     * </ol>
     */
    @Override
    public void onEnable() {
        getLogger().info("Artificial Enchantments library enabled");

        ArtificialEnchantmentsAPI api = ArtificialEnchantmentsAPI.create(this);

        registerEnchantmentTableListener();
        registerAnvilListener(api.getItemStorage());
        registerBlockBreakLootListener(api);
        registerEffectListener(api);

        if (itemEnchantmentService != null && effectDispatchSpine != null) {
            this.tickTask = new EnchantmentTickTask(this, effectDispatchSpine, itemEnchantmentService);
            this.tickTask.start();
        }

        initializePacketEventsAdapter();
    }

    /**
     * Registers the enchantment table listener.
     *
     * <p>Sets up the listener that handles custom enchantments appearing
     * in enchantment tables.
     */
    private void registerEnchantmentTableListener() {
        EnchantmentRegistryManager registryManager = EnchantmentRegistryManager.getInstance();
        PaperRegistryBridge registryBridge = PaperRegistryBridge.getInstance();

        EnchantmentTableListener listener = new EnchantmentTableListener(
            this, registryManager, registryBridge
        );

        getServer().getPluginManager().registerEvents(listener, this);
        getLogger().info("Enchantment table listener registered");
    }

    /**
     * Registers the anvil listener.
     *
     * <p>Sets up the listener that handles combining items with enchantments
     * and applying enchantments via anvil.
     *
     * @param itemStorage the item storage for enchantment operations
     */
    private void registerAnvilListener(@NotNull ItemStorage itemStorage) {
        EnchantmentRegistryManager registryManager = EnchantmentRegistryManager.getInstance();

        AnvilListener listener = new AnvilListener(this, itemStorage, registryManager);

        getServer().getPluginManager().registerEvents(listener, this);
        getLogger().info("Anvil listener registered");
    }

    /**
     * Registers the block break loot listener.
     *
     * <p>Sets up the listener that modifies block drops based on enchantments.
     *
     * @param api the API instance
     */
    private void registerBlockBreakLootListener(@NotNull ArtificialEnchantmentsAPI api) {
        EnchantmentRegistryManager registryManager = EnchantmentRegistryManager.getInstance();
        ItemEnchantmentService enchantmentService = new ItemEnchantmentService(api.getItemStorage(), registryManager);

        BlockBreakLootListener listener = new BlockBreakLootListener(
            this,
            new BlockBreakLootHandler(api.getLootModifierRegistry(), enchantmentService)
        );

        getServer().getPluginManager().registerEvents(listener, this);
        getLogger().info("Block break loot listener registered");
    }

    /**
     * Registers the enchantment effect listener.
     *
     * <p>Sets up the listener that dispatches enchantment effects when
     * game events occur (combat, mining, etc.).
     *
     * @param api the API instance
     */
    private void registerEffectListener(@NotNull ArtificialEnchantmentsAPI api) {
        FoliaScheduler scheduler = new BukkitFoliaScheduler();
        EffectDispatchSpine spine = new EffectDispatchSpine(scheduler, api.getEventBus());
        this.effectDispatchSpine = spine;

        EnchantmentRegistryManager registryManager = EnchantmentRegistryManager.getInstance();
        this.itemEnchantmentService = new ItemEnchantmentService(api.getItemStorage(), registryManager);

        EnchantmentEffectListener listener = new EnchantmentEffectListener(this, spine, this.itemEnchantmentService);
        getServer().getPluginManager().registerEvents(listener, this);
        getLogger().info("Enchantment effect listener registered");
    }

    /**
     * Disables the plugin, stopping all tasks and cleaning up resources.
     *
     * <p>This method:
     * <ol>
     *   <li>Stops the tick task</li>
     *   <li>Disables PacketEvents adapter if enabled</li>
     *   <li>Shuts down the effect dispatch spine</li>
     * </ol>
     */
    @Override
    public void onDisable() {
        if (tickTask != null) {
            tickTask.stop();
            tickTask = null;
        }

        // Disable PacketEvents adapter if it was enabled
        if (packetEventsAdapter != null) {
            packetEventsAdapter.disable();
            packetEventsAdapter = null;
        }

        if (effectDispatchSpine != null) {
            effectDispatchSpine.shutdown();
            effectDispatchSpine = null;
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

    /**
     * Initializes the PacketEvents adapter.
     *
     * <p>Attempts to create and enable the PacketEvents adapter. If PacketEvents
     * is not present or fails to initialize, this method logs a warning and
     * continues without packet-level features.
     */
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
