package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.EnchantmentEventBus;
import io.artificial.enchantments.api.context.*;
import io.artificial.enchantments.api.event.CombatEvent;
import io.artificial.enchantments.api.event.EnchantEffectEvent;
import io.artificial.enchantments.api.event.ToolEvent;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
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
            default -> {
                // Other event types not yet implemented for event bus
                LOGGER.fine("Event bus dispatch not yet implemented for: " + eventType);
                yield null;
            }
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
    }
}
