package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentEffectHandler;
import io.artificial.enchantments.api.context.*;
import org.jetbrains.annotations.NotNull;

/**
 * Dispatches effects to typed callback handlers (EnchantmentEffectHandler implementations).
 *
 * <p>This dispatcher invokes the appropriate method on a handler based on the event type.
 * It is called by the EffectDispatchSpine as the first path in the dual-path dispatch system.
 *
 * <p>The dispatcher maps DispatchEventType values to their corresponding handler methods:
 * <ul>
 *   <li>ENTITY_DAMAGE_BY_ENTITY → onEntityDamageByEntity(CombatContext)</li>
 *   <li>ENTITY_DAMAGE → onEntityDamage(CombatContext)</li>
 *   <li>SHIELD_BLOCK → onShieldBlock(CombatContext)</li>
 *   <li>BLOCK_BREAK → onBlockBreak(ToolContext)</li>
 *   <li>BLOCK_BREAK_PRE → onBlockBreakPre(ToolContext)</li>
 *   <li>BLOCK_PLACE → onBlockPlace(ToolContext)</li>
 *   <li>BLOCK_INTERACT → onBlockInteract(InteractionContext)</li>
 *   <li>ENTITY_INTERACT → onEntityInteract(InteractionContext)</li>
 *   <li>PROJECTILE_LAUNCH → onProjectileLaunch(ProjectileContext)</li>
 *   <li>PROJECTILE_HIT → onProjectileHit(ProjectileContext)</li>
 *   <li>FISHING_ACTION → onFishingAction(FishingContext)</li>
 *   <li>HELD_TICK → onHeldTick(TickContext)</li>
 *   <li>ARMOR_TICK → onArmorTick(TickContext)</li>
 *   <li>ITEM_USED → onItemUsed(ItemContext)</li>
 *   <li>DURABILITY_DAMAGE → onDurabilityDamage(ItemContext)</li>
 *   <li>ITEM_CONSUME → onItemConsume(ConsumableContext)</li>
 *   <li>BOW_SHOOT → onBowShoot(WeaponContext)</li>
 *   <li>TRIDENT_THROW → onTridentThrow(WeaponContext)</li>
 *   <li>ITEM_DROP → onItemDrop(ItemContext)</li>
 *   <li>ITEM_PICKUP → onItemPickup(ItemContext)</li>
 * </ul>
 *
 * @see EnchantmentEffectHandler
 * @see EffectDispatchSpine
 */
public final class TypedCallbackDispatcher {

    /**
     * Creates a new typed callback dispatcher.
     *
     * @since 0.2.0
     */
    public TypedCallbackDispatcher() {
    }

    /**
     * Dispatches an effect to the appropriate handler method based on event type.
     *
     * @param handler the effect handler to invoke
     * @param context the effect context containing event data
     * @param eventType the type of event to dispatch
     * @return true if the context was cancelled during handler execution, false otherwise
     */
    public boolean dispatch(
            @NotNull EnchantmentEffectHandler handler,
            @NotNull EffectContext context,
            @NotNull EffectDispatchSpine.DispatchEventType eventType
    ) {
        switch (eventType) {
            case ENTITY_DAMAGE_BY_ENTITY -> {
                if (context instanceof CombatContext combatContext) {
                    handler.onEntityDamageByEntity(combatContext);
                }
            }
            case ENTITY_DAMAGE -> {
                if (context instanceof CombatContext combatContext) {
                    handler.onEntityDamage(combatContext);
                }
            }
            case SHIELD_BLOCK -> {
                if (context instanceof CombatContext combatContext) {
                    handler.onShieldBlock(combatContext);
                }
            }
            case BLOCK_BREAK -> {
                if (context instanceof ToolContext toolContext) {
                    handler.onBlockBreak(toolContext);
                }
            }
            case BLOCK_BREAK_PRE -> {
                if (context instanceof ToolContext toolContext) {
                    handler.onBlockBreakPre(toolContext);
                }
            }
            case BLOCK_PLACE -> {
                if (context instanceof ToolContext toolContext) {
                    handler.onBlockPlace(toolContext);
                }
            }
            case BLOCK_INTERACT -> {
                if (context instanceof InteractionContext interactionContext) {
                    handler.onBlockInteract(interactionContext);
                }
            }
            case ENTITY_INTERACT -> {
                if (context instanceof InteractionContext interactionContext) {
                    handler.onEntityInteract(interactionContext);
                }
            }
            case PROJECTILE_LAUNCH -> {
                if (context instanceof ProjectileContext projectileContext) {
                    handler.onProjectileLaunch(projectileContext);
                }
            }
            case PROJECTILE_HIT -> {
                if (context instanceof ProjectileContext projectileContext) {
                    handler.onProjectileHit(projectileContext);
                }
            }
            case FISHING_ACTION -> {
                if (context instanceof FishingContext fishingContext) {
                    handler.onFishingAction(fishingContext);
                }
            }
            case HELD_TICK -> {
                if (context instanceof TickContext tickContext) {
                    handler.onHeldTick(tickContext);
                }
            }
            case ARMOR_TICK -> {
                if (context instanceof TickContext tickContext) {
                    handler.onArmorTick(tickContext);
                }
            }
            case ITEM_USED -> {
                if (context instanceof ItemContext itemContext) {
                    handler.onItemUsed(itemContext);
                }
            }
            case DURABILITY_DAMAGE -> {
                if (context instanceof ItemContext itemContext) {
                    handler.onDurabilityDamage(itemContext);
                }
            }
            case ITEM_CONSUME -> {
                if (context instanceof ConsumableContext consumableContext) {
                    handler.onItemConsume(consumableContext);
                }
            }
            case BOW_SHOOT -> {
                if (context instanceof WeaponContext weaponContext) {
                    handler.onBowShoot(weaponContext);
                }
            }
            case TRIDENT_THROW -> {
                if (context instanceof WeaponContext weaponContext) {
                    handler.onTridentThrow(weaponContext);
                }
            }
            case ITEM_DROP -> {
                if (context instanceof ItemContext itemContext) {
                    handler.onItemDrop(itemContext);
                }
            }
            case ITEM_PICKUP -> {
                if (context instanceof ItemContext itemContext) {
                    handler.onItemPickup(itemContext);
                }
            }
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        }

        return context.isCancelled();
    }
}
