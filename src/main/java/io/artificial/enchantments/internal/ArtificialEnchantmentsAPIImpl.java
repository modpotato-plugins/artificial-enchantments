package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.ArtificialEnchantmentsAPI;
import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.EnchantmentEventBus;
import io.artificial.enchantments.api.ItemStorage;
import io.artificial.enchantments.api.loot.LootModifierRegistry;
import io.artificial.enchantments.api.query.ItemEnchantmentQuery;
import io.artificial.enchantments.api.scaling.ScalingAlgorithmRegistry;
import io.artificial.enchantments.internal.loot.LootModifierRegistryImpl;
import io.artificial.enchantments.internal.query.ItemEnchantmentQueryImpl;
import io.artificial.enchantments.internal.scaling.ScalingAlgorithmRegistryImpl;

import io.artificial.enchantments.internal.FoliaScheduler;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of {@link ArtificialEnchantmentsAPI}.
 * 
 * <p>This class provides the concrete implementation of the public API,
 * delegating operations to {@link ItemEnchantmentService} and other
 * internal components.
 *
 * @since 0.2.0
 */
public class ArtificialEnchantmentsAPIImpl implements ArtificialEnchantmentsAPI {

    private static final Object LOCK = new Object();
    private static volatile ArtificialEnchantmentsAPIImpl instance;

    private final Plugin plugin;
    private final ItemEnchantmentService itemService;
    private final ItemStorage itemStorage;
    private final EnchantmentRegistryManager registryManager;
    private final ItemEnchantmentQuery queryFacade;
    private final LootModifierRegistry lootModifierRegistry;
    private final EnchantmentEventBus eventBus;
    private final FoliaScheduler scheduler;
    private final ScalingAlgorithmRegistry scalingRegistry;
    private final String version;

    private ArtificialEnchantmentsAPIImpl(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.registryManager = EnchantmentRegistryManager.getInstance();
        this.itemStorage = createItemStorage();
        this.itemService = new ItemEnchantmentService(itemStorage, registryManager);
        this.queryFacade = new ItemEnchantmentQueryImpl(itemStorage, registryManager);
        this.lootModifierRegistry = new LootModifierRegistryImpl();
        this.eventBus = new EnchantmentEventBusImpl();
        this.scheduler = new BukkitFoliaScheduler();
        this.scalingRegistry = new ScalingAlgorithmRegistryImpl();
        this.version = loadVersion();
    }

    /**
     * Creates a new API instance bound to the specified plugin.
     *
     * @param plugin the plugin requesting the API
     * @return a new API instance
     */
    @NotNull
    public static ArtificialEnchantmentsAPIImpl create(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");

        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ArtificialEnchantmentsAPIImpl(plugin);
                }
            }
        }
        return instance;
    }

    /**
     * Gets the shared API instance.
     *
     * @return the shared API instance
     * @throws IllegalStateException if not initialized
     */
    @NotNull
    public static ArtificialEnchantmentsAPIImpl getInstance() {
        if (instance == null) {
            throw new IllegalStateException("API not initialized. Call create() first.");
        }
        return instance;
    }

    static void resetForTesting() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    @Override
    @NotNull
    public ArtificialEnchantmentsAPI registerEnchantment(@NotNull EnchantmentDefinition definition) {
        itemService.registerEnchantment(definition);
        return this;
    }

    @Override
    public boolean unregisterEnchantment(@NotNull NamespacedKey key) {
        return itemService.unregisterEnchantment(key);
    }

    @Override
    @NotNull
    public Optional<EnchantmentDefinition> getEnchantment(@NotNull NamespacedKey key) {
        return itemService.getEnchantment(key);
    }

    @Override
    @NotNull
    public Collection<EnchantmentDefinition> getAllEnchantments() {
        return itemService.getAllEnchantments();
    }

    @Override
    @NotNull
    public Set<EnchantmentDefinition> getEnchantmentsFor(@NotNull Material material) {
        return itemService.getEnchantmentsForMaterial(material);
    }

    @Override
    @NotNull
    public ItemStack applyEnchantment(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment, int level) {
        return itemService.applyEnchantment(item, enchantment, level);
    }

    @Override
    @NotNull
    public ItemStack applyEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key, int level) {
        return itemService.applyEnchantment(item, key, level);
    }

    @Override
    @NotNull
    public ItemStack removeEnchantment(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment) {
        return itemService.removeEnchantment(item, enchantment);
    }

    @Override
    @NotNull
    public ItemStack removeEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key) {
        return itemService.removeEnchantment(item, key);
    }

    @Override
    @NotNull
    public ItemStack removeAllEnchantments(@NotNull ItemStack item) {
        return itemService.removeAllEnchantments(item);
    }

    @Override
    public int getEnchantmentLevel(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment) {
        return itemService.getEnchantmentLevel(item, enchantment);
    }

    @Override
    public int getEnchantmentLevel(@NotNull ItemStack item, @NotNull NamespacedKey key) {
        return itemService.getEnchantmentLevel(item, key);
    }

    @Override
    @NotNull
    public java.util.Map<EnchantmentDefinition, Integer> getEnchantments(@NotNull ItemStack item) {
        return itemService.getEnchantments(item);
    }

    @Override
    public boolean hasEnchantment(@NotNull ItemStack item, @NotNull EnchantmentDefinition enchantment) {
        return itemService.hasEnchantment(item, enchantment);
    }

    @Override
    public boolean hasEnchantment(@NotNull ItemStack item, @NotNull NamespacedKey key) {
        return itemService.hasEnchantment(item, key);
    }

    @Override
    @NotNull
    public EnchantmentEventBus getEventBus() {
        return eventBus;
    }

    @Override
    @NotNull
    public ItemStorage getItemStorage() {
        return itemStorage;
    }

    @Override
    @NotNull
    public ItemEnchantmentQuery query() {
        return queryFacade;
    }

    @Override
    @NotNull
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    @NotNull
    public String getVersion() {
        return version;
    }

    @Override
    @NotNull
    public ScalingAlgorithmRegistry getScalingRegistry() {
        return scalingRegistry;
    }

    @Override
    @NotNull
    public LootModifierRegistry getLootModifierRegistry() {
        return lootModifierRegistry;
    }

    @Override
    @NotNull
    public FoliaScheduler getScheduler() {
        return scheduler;
    }

    @NotNull
    private ItemStorage createItemStorage() {
        NbtMetadataStorage nbtStorage = new NbtMetadataStorage();
        return new NativeFirstItemStorage(() -> registryManager, nbtStorage);
    }

    @NotNull
    private String loadVersion() {
        try (java.io.InputStream is = getClass().getResourceAsStream("/version.properties")) {
            if (is != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(is);
                String v = props.getProperty("version");
                if (v != null && !v.trim().isEmpty() && !v.startsWith("${")) {
                    return v.trim();
                }
            }
        } catch (Exception e) {
            // Fallback if properties file is missing or unreadable
        }
        return "unknown";
    }
}
