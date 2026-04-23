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
 * It supports optional PacketEvents integration for per-player visual customization.
 *
 * @since 0.1.0
 */
public class ArtificialEnchantmentsPlugin extends JavaPlugin {

    /**
     * Creates a new plugin instance.
     */
    public ArtificialEnchantmentsPlugin() {
    }

    @Nullable
    private PacketEventsAdapter packetEventsAdapter;

    @Nullable
    private EffectDispatchSpine effectDispatchSpine;

    @Nullable
    private EnchantmentTickTask tickTask;

    @Nullable
    private ItemEnchantmentService itemEnchantmentService;

    /**
     * Enables the plugin, initializing the API and registering all listeners.
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

    private void registerEnchantmentTableListener() {
        EnchantmentRegistryManager registryManager = EnchantmentRegistryManager.getInstance();
        PaperRegistryBridge registryBridge = PaperRegistryBridge.getInstance();

        EnchantmentTableListener listener = new EnchantmentTableListener(
            this, registryManager, registryBridge
        );

        getServer().getPluginManager().registerEvents(listener, this);
        getLogger().info("Enchantment table listener registered");
    }

    private void registerAnvilListener(@NotNull ItemStorage itemStorage) {
        EnchantmentRegistryManager registryManager = EnchantmentRegistryManager.getInstance();

        AnvilListener listener = new AnvilListener(this, itemStorage, registryManager);

        getServer().getPluginManager().registerEvents(listener, this);
        getLogger().info("Anvil listener registered");
    }

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
