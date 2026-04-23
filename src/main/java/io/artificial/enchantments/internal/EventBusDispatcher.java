package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.EnchantmentEventBus;
import io.artificial.enchantments.api.context.*;
import io.artificial.enchantments.api.event.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dispatches effects through the event bus to registered listeners.
 *
 * <p>This dispatcher creates the appropriate {@link EnchantEffectEvent} subclass
 * and dispatches it via the {@link EnchantmentEventBus}. It is called by the
 * EffectDispatchSpine as the second path in the dual-path dispatch system.
 *
 * <p>Event creation mapping:
 * <ul>
 *   <li>Combat events → {@link CombatEvent}</li>
 *   <li>Tool events (block break/place) → {@link ToolEvent}</li>
 *   <li>Interaction events → {@link InteractionEvent}</li>
 *   <li>Projectile events → {@link ProjectileEvent}</li>
 *   <li>Fishing events → {@link FishingEvent}</li>
 *   <li>Tick events → {@link TickEvent}</li>
 *   <li>Item events → {@link ItemEvent}</li>
 *   <li>Consumable events → {@link ConsumableEvent}</li>
 *   <li>Weapon events → {@link WeaponEvent}</li>
 * </ul>
 *
 * <p>The dispatcher extracts necessary data from the underlying Bukkit event
 * to populate the EnchantEffectEvent with contextual information.
 *
 * @see EnchantmentEventBus
 * @see EffectDispatchSpine
 */
public final class EventBusDispatcher {

    private static final Logger LOGGER = Logger.getLogger("ArtificialEnchantments");

    private final EnchantmentEventBus eventBus;

    /**
     * Creates a new EventBusDispatcher.
     *
     * @param eventBus the event bus to dispatch events through
     */
    public EventBusDispatcher(@NotNull EnchantmentEventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Dispatches an effect through the event bus.
     *
     * @param enchantment the enchantment definition
     * @param context the effect context
     * @param bukkitEvent the underlying Bukkit event
     * @param eventType the type of event being dispatched
     * @return true if the event was cancelled during dispatch, false otherwise
     */
    public boolean dispatch(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull Event bukkitEvent,
            @NotNull EffectDispatchSpine.DispatchEventType eventType
    ) {
        try {
            EnchantEffectEvent event = createEvent(enchantment, context, bukkitEvent, eventType);
            if (event == null) {
                return false;
            }

            eventBus.dispatch(event);

            // Sync any modifications back to the context
            syncEventToContext(event, context);

            // Check cancellation safely
            return event.isCancellable() && event.checkCancelled();

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error dispatching event for " + enchantment.getKey(), e);
            return false;
        }
    }

    /**
     * Creates the appropriate EnchantEffectEvent for the given event type.
     *
     * @param enchantment the enchantment definition
     * @param context the effect context
     * @param bukkitEvent the underlying Bukkit event
     * @param eventType the type of event
     * @return the created event, or null if the event type is not supported
     */
    @Nullable
    private EnchantEffectEvent createEvent(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull Event bukkitEvent,
            @NotNull EffectDispatchSpine.DispatchEventType eventType
    ) {
        return switch (eventType) {
            case ENTITY_DAMAGE_BY_ENTITY, ENTITY_DAMAGE, SHIELD_BLOCK ->
                    createCombatEvent(enchantment, context, bukkitEvent, eventType);
            case BLOCK_BREAK, BLOCK_BREAK_PRE, BLOCK_PLACE ->
                    createToolEvent(enchantment, context, bukkitEvent, eventType);
            case BLOCK_INTERACT, ENTITY_INTERACT ->
                    createInteractionEvent(enchantment, context, bukkitEvent, eventType);
            case PROJECTILE_LAUNCH, PROJECTILE_HIT ->
                    createProjectileEvent(enchantment, context, bukkitEvent, eventType);
            case FISHING_ACTION ->
                    createFishingEvent(enchantment, context, bukkitEvent, eventType);
            case HELD_TICK, ARMOR_TICK ->
                    createTickEvent(enchantment, context, bukkitEvent, eventType);
            case ITEM_USED, DURABILITY_DAMAGE, ITEM_DROP, ITEM_PICKUP ->
                    createItemEvent(enchantment, context, bukkitEvent, eventType);
            case ITEM_CONSUME ->
                    createConsumableEvent(enchantment, context, bukkitEvent, eventType);
            case BOW_SHOOT, TRIDENT_THROW ->
                    createWeaponEvent(enchantment, context, bukkitEvent, eventType);
        };
    }

    /**
     * Creates a CombatEvent from the context and Bukkit event.
     */
    @Nullable
    private CombatEvent createCombatEvent(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull Event bukkitEvent,
            @NotNull EffectDispatchSpine.DispatchEventType eventType
    ) {
        if (!(context instanceof CombatContext combatContext)) {
            return null;
        }

        LivingEntity attacker = combatContext.getAttacker();
        LivingEntity victim = combatContext.getVictim();
        ItemStack weapon = combatContext.getWeapon();
        double baseDamage = combatContext.getBaseDamage();
        DamageSource damageSource = combatContext.getDamageSource();

        CombatEvent.CombatType type = switch (eventType) {
            case ENTITY_DAMAGE_BY_ENTITY -> CombatEvent.CombatType.ATTACK;
            case SHIELD_BLOCK -> CombatEvent.CombatType.SHIELD_BLOCK;
            default -> CombatEvent.CombatType.DEFENSE;
        };

        CombatEvent event = new CombatEvent(
                enchantment,
                context.getLevel(),
                context.getScaledValue(),
                attacker,
                victim,
                weapon,
                baseDamage,
                damageSource,
                type
        );

        // Set initial final damage from context
        event.setFinalDamage(combatContext.getCurrentDamage());

        return event;
    }

    /**
     * Creates a ToolEvent from the context and Bukkit event.
     */
    @Nullable
    private ToolEvent createToolEvent(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull Event bukkitEvent,
            @NotNull EffectDispatchSpine.DispatchEventType eventType
    ) {
        if (!(context instanceof ToolContext toolContext)) {
            return null;
        }

        Player player = toolContext.getPlayer();
        ItemStack tool = toolContext.getTool();
        Block block = toolContext.getBlock();
        BlockFace face = toolContext.getBlockFace();

        ToolEvent.ToolType type = switch (eventType) {
            case BLOCK_BREAK -> ToolEvent.ToolType.BLOCK_BREAK;
            case BLOCK_BREAK_PRE -> ToolEvent.ToolType.BLOCK_BREAK_PRE;
            case BLOCK_PLACE -> ToolEvent.ToolType.BLOCK_PLACE;
            default -> ToolEvent.ToolType.BLOCK_INTERACT;
        };

        // Extract drops from BlockBreakEvent if available
        List<ItemStack> drops = null;
        int expToDrop = 0;

        if (bukkitEvent instanceof BlockBreakEvent breakEvent) {
            expToDrop = breakEvent.getExpToDrop();
        }

        return new ToolEvent(
                enchantment,
                context.getLevel(),
                context.getScaledValue(),
                player,
                tool,
                block,
                face,
                type,
                drops,
                expToDrop
        );
    }

    @Nullable
    private InteractionEvent createInteractionEvent(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull Event bukkitEvent,
            @NotNull EffectDispatchSpine.DispatchEventType eventType
    ) {
        if (!(context instanceof InteractionContext interactionContext)) {
            return null;
        }

        Player player = interactionContext.getPlayer();
        ItemStack item = interactionContext.getItem();
        EquipmentSlot hand = interactionContext.getHand();
        Location location = interactionContext.getLocation();
        Block block = interactionContext.getBlock();
        BlockFace blockFace = interactionContext.getBlockFace();
        Entity targetEntity = interactionContext.getTargetEntity();
        boolean isLeftClick = interactionContext.isLeftClick();
        boolean isRightClick = interactionContext.isRightClick();

        return new InteractionEvent(
                enchantment,
                context.getLevel(),
                context.getScaledValue(),
                player,
                item,
                hand,
                location,
                block,
                blockFace,
                targetEntity,
                isLeftClick,
                isRightClick,
                interactionContext.getInteractionPoint()
        );
    }

    @Nullable
    private ProjectileEvent createProjectileEvent(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull Event bukkitEvent,
            @NotNull EffectDispatchSpine.DispatchEventType eventType
    ) {
        if (!(context instanceof ProjectileContext projectileContext)) {
            return null;
        }

        Projectile projectile = projectileContext.getProjectile();
        LivingEntity shooter = projectileContext.getShooter();
        Location launchLocation = projectileContext.getLaunchLocation();
        ItemStack weapon = projectileContext.getWeapon();
        Entity hitEntity = projectileContext.getHitEntity();
        Block hitBlock = projectileContext.getHitBlock();
        Vector hitPosition = projectileContext.getHitPosition();
        boolean isLaunch = projectileContext.isLaunch();

        return new ProjectileEvent(
                enchantment,
                context.getLevel(),
                context.getScaledValue(),
                projectile,
                shooter,
                launchLocation,
                weapon,
                hitEntity,
                hitBlock,
                hitPosition,
                isLaunch
        );
    }

    @Nullable
    private FishingEvent createFishingEvent(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull Event bukkitEvent,
            @NotNull EffectDispatchSpine.DispatchEventType eventType
    ) {
        if (!(context instanceof FishingContext fishingContext)) {
            return null;
        }

        Player player = fishingContext.getPlayer();
        ItemStack fishingRod = fishingContext.getFishingRod();
        FishHook hook = fishingContext.getHook();
        Location location = fishingContext.getLocation();
        Item caughtItem = fishingContext.getCaughtItem();
        Entity caughtEntity = fishingContext.getCaughtEntity();
        int waitTime = fishingContext.getWaitTime();
        int lureSpeed = fishingContext.getLureSpeed();
        boolean applyLure = fishingContext.isApplyLure();

        FishingEvent.State state;
        if (fishingContext.isCast()) {
            state = FishingEvent.State.CAST;
        } else if (fishingContext.isReel()) {
            state = FishingEvent.State.REEL;
        } else if (fishingContext.isBite()) {
            state = FishingEvent.State.BITE;
        } else {
            state = FishingEvent.State.CAUGHT;
        }

        return new FishingEvent(
                enchantment,
                context.getLevel(),
                context.getScaledValue(),
                player,
                fishingRod,
                hook,
                location,
                caughtItem,
                caughtEntity,
                state,
                waitTime,
                lureSpeed,
                applyLure
        );
    }

    @Nullable
    private TickEvent createTickEvent(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull Event bukkitEvent,
            @NotNull EffectDispatchSpine.DispatchEventType eventType
    ) {
        if (!(context instanceof TickContext tickContext)) {
            return null;
        }

        Player player = tickContext.getPlayer();
        ItemStack item = tickContext.getItem();
        EquipmentSlot slot = tickContext.getSlot();
        boolean isHeld = tickContext.isHeld();
        boolean isArmor = tickContext.isArmor();
        int tickCount = tickContext.getTickCount();
        long heldDuration = tickContext.getHeldDuration();

        return new TickEvent(
                enchantment,
                context.getLevel(),
                context.getScaledValue(),
                player,
                item,
                slot,
                isHeld,
                isArmor,
                tickCount,
                heldDuration
        );
    }

    @Nullable
    private ItemEvent createItemEvent(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull Event bukkitEvent,
            @NotNull EffectDispatchSpine.DispatchEventType eventType
    ) {
        if (!(context instanceof ItemContext itemContext)) {
            return null;
        }

        Player player = itemContext.getPlayer();
        ItemStack item = itemContext.getItem();
        EquipmentSlot slot = itemContext.getSlot();
        int currentDurability = itemContext.getCurrentDurability();
        int maxDurability = itemContext.getMaxDurability();
        int damageTaken = itemContext.getDamageTaken();

        boolean isDrop = eventType == EffectDispatchSpine.DispatchEventType.ITEM_DROP;
        boolean isPickup = eventType == EffectDispatchSpine.DispatchEventType.ITEM_PICKUP;

        return new ItemEvent(
                enchantment,
                context.getLevel(),
                context.getScaledValue(),
                player,
                item,
                slot,
                isDrop,
                isPickup,
                currentDurability,
                maxDurability,
                damageTaken
        );
    }

    @Nullable
    private ConsumableEvent createConsumableEvent(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull Event bukkitEvent,
            @NotNull EffectDispatchSpine.DispatchEventType eventType
    ) {
        if (!(context instanceof ConsumableContext consumableContext)) {
            return null;
        }

        Player player = consumableContext.getPlayer();
        ItemStack item = consumableContext.getItem();
        Material material = item.getType();
        int foodLevel = consumableContext.getFoodLevel();
        float saturation = consumableContext.getSaturation();
        double healthRestored = consumableContext.getHealthRestored();
        int consumptionTime = consumableContext.getConsumptionTime();

        return new ConsumableEvent(
                enchantment,
                context.getLevel(),
                context.getScaledValue(),
                player,
                item,
                material,
                foodLevel,
                saturation,
                healthRestored,
                consumptionTime
        );
    }

    @Nullable
    private WeaponEvent createWeaponEvent(
            @NotNull EnchantmentDefinition enchantment,
            @NotNull EffectContext context,
            @NotNull Event bukkitEvent,
            @NotNull EffectDispatchSpine.DispatchEventType eventType
    ) {
        if (!(context instanceof WeaponContext weaponContext)) {
            return null;
        }

        Player player = weaponContext.getPlayer();
        ItemStack weapon = weaponContext.getWeapon();
        Location location = weaponContext.getLocation();
        LivingEntity target = weaponContext.getTarget();
        Projectile projectile = weaponContext.getProjectile();
        boolean isBow = weaponContext.isBow();
        boolean isCrossbow = weaponContext.isCrossbow();
        boolean isTrident = weaponContext.isTrident();
        boolean isShooting = weaponContext.isShooting();
        boolean isThrowing = weaponContext.isThrowing();
        float force = weaponContext.getForce();
        boolean critical = weaponContext.isCritical();
        int pierceLevel = weaponContext.getPierceLevel();

        return new WeaponEvent(
                enchantment,
                context.getLevel(),
                context.getScaledValue(),
                player,
                weapon,
                location,
                target,
                projectile,
                isBow,
                isCrossbow,
                isTrident,
                isShooting,
                isThrowing,
                force,
                critical,
                pierceLevel
        );
    }

    /**
     * Synchronizes any modifications from the event back to the context.
     *
     * @param event the dispatched event
     * @param context the original context
     */
    private void syncEventToContext(@NotNull EnchantEffectEvent event, @NotNull EffectContext context) {
        // Sync CombatEvent changes back to CombatContext
        if (event instanceof CombatEvent combatEvent && context instanceof CombatContext combatContext) {
            // If damage was modified in the event, update the context
            double finalDamage = combatEvent.getFinalDamage();
            if (Math.abs(finalDamage - combatContext.getCurrentDamage()) > 0.001) {
                combatContext.setDamage(finalDamage);
            }
        }

        // Sync ToolEvent changes back to ToolContext
        if (event instanceof ToolEvent toolEvent && context instanceof ToolContext toolContext) {
            // Sync drops if modified
            List<ItemStack> eventDrops = toolEvent.getDrops();
            if (eventDrops != null) {
                toolContext.setDrops(new ArrayList<>(eventDrops));
            }

            // Sync exp if modified
            toolContext.setExpToDrop(toolEvent.getExpToDrop());
        }

        if (event instanceof InteractionEvent interactionEvent && context instanceof InteractionContext interactionContext) {
            if (context instanceof Cancellable cancellableContext) {
                cancellableContext.setCancelled(interactionEvent.isCancelled());
            }
        }

        if (event instanceof ProjectileEvent projectileEvent && context instanceof ProjectileContext projectileContext) {
            projectileContext.setVelocity(projectileEvent.getVelocity());
            projectileContext.setGravity(projectileEvent.hasGravity());
            projectileContext.setPierceLevel(projectileEvent.getPierceLevel());
        }

        if (event instanceof FishingEvent fishingEvent && context instanceof FishingContext fishingContext) {
            if (fishingEvent.getWaitTime() != fishingContext.getWaitTime()) {
                fishingContext.setWaitTime(fishingEvent.getWaitTime());
            }
            if (fishingEvent.getLureSpeed() != fishingContext.getLureSpeed()) {
                fishingContext.setLureSpeed(fishingEvent.getLureSpeed());
            }
            if (fishingEvent.isApplyLure() != fishingContext.isApplyLure()) {
                fishingContext.setApplyLure(fishingEvent.isApplyLure());
            }
        }

        if (event instanceof ItemEvent itemEvent && context instanceof ItemContext itemContext) {
            if (itemEvent.getDamageTaken() != itemContext.getDamageTaken()) {
                itemContext.setDamageTaken(itemEvent.getDamageTaken());
            }
        }

        if (event instanceof ConsumableEvent consumableEvent && context instanceof ConsumableContext consumableContext) {
            if (consumableEvent.getFoodLevel() != consumableContext.getFoodLevel()) {
                consumableContext.setFoodLevel(consumableEvent.getFoodLevel());
            }
            if (consumableEvent.getSaturation() != consumableContext.getSaturation()) {
                consumableContext.setSaturation(consumableEvent.getSaturation());
            }
            if (consumableEvent.getHealthRestored() != consumableContext.getHealthRestored()) {
                consumableContext.setHealthRestored(consumableEvent.getHealthRestored());
            }
            if (consumableEvent.getConsumptionTime() != consumableContext.getConsumptionTime()) {
                consumableContext.setConsumptionTime(consumableEvent.getConsumptionTime());
            }
        }

        if (event instanceof WeaponEvent weaponEvent && context instanceof WeaponContext weaponContext) {
            if (weaponEvent.getForce() != weaponContext.getForce()) {
                weaponContext.setForce(weaponEvent.getForce());
            }
            if (weaponEvent.isCritical() != weaponContext.isCritical()) {
                weaponContext.setCritical(weaponEvent.isCritical());
            }
            if (weaponEvent.getPierceLevel() != weaponContext.getPierceLevel()) {
                weaponContext.setPierceLevel(weaponEvent.getPierceLevel());
            }
        }
    }
}
