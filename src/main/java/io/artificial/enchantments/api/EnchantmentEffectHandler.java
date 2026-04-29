package io.artificial.enchantments.api;

import io.artificial.enchantments.api.context.*;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Handler for enchantment effects triggered by game events.
 * 
 * <p>Implement this interface to define custom behavior for your enchantments.
 * Each method corresponds to a specific game event type and receives a context
 * object with relevant data.
 *
 * <p><strong>Typed Callbacks vs Event Bus:</strong><br>
 * You can handle effects through either typed callbacks (this interface) or
 * the {@link EnchantmentEventBus}. Both paths fire through the same dispatch
 * spine, so choose based on your preference:
 * <ul>
 *   <li><strong>Typed callbacks:</strong> Type-safe, compile-time checked, easier for simple effects</li>
 *   <li><strong>Event bus:</strong> Decoupled, allows multiple listeners, better for complex interactions</li>
 * </ul>
 *
 * <p><strong>Cancellation:</strong><br>
 * Contexts that support cancellation implement {@link org.bukkit.event.Cancellable}.
 * Call {@link EffectContext#tryCancel()} to cancel the underlying Bukkit event.
 * Cancellation propagates through both typed callbacks and event bus listeners.
 *
 * <p><strong>Threading:</strong><br>
 * Handlers execute inline with the library's dispatch flow. Keep them light,
 * avoid blocking work, and reschedule anything expensive through your own
 * scheduler usage.
 *
 * <p><strong>Example Implementation:</strong>
 * <pre>{@code
 * public class LifeStealHandler implements EnchantmentEffectHandler {
 *     @Override
 *     public void onEntityDamageByEntity(@NotNull CombatContext context) {
 *         if (!context.isAttack()) return;
 *         
 *         double scaledValue = context.getScaledValue();
 *         Player attacker = (Player) context.getAttacker();
 *         LivingEntity victim = context.getVictim();
 *         
 *         // Heal attacker based on scaled value
 *         double healAmount = context.getFinalDamage() * scaledValue * 0.1;
 *         attacker.heal(healAmount);
 *     }
 * }
 * }</pre>
 *
 * @see EffectContext
 * @see CombatContext
 * @see ToolContext
 * @see EnchantmentEventBus
 * @since 0.1.0
 */
public interface EnchantmentEffectHandler {

    /**
     * Called when an entity damages another entity (attack context).
     * 
     * <p>Fires for melee attacks, projectile hits, and other damage sources
     * where an attacker can be identified.
     *
     * @param context the combat context with attacker, victim, and damage data
     * @since 0.1.0
     */
    default void onEntityDamageByEntity(@NotNull CombatContext context) { }

    /**
     * Called when an entity takes damage from any source.
     * 
     * <p>Fires for all damage events, including environmental damage where
     * there may be no attacker. Check {@link CombatContext#getAttacker()}
     * to determine if there is an attacking entity.
     *
     * @param context the combat context with victim and damage data
     * @since 0.1.0
     */
    default void onEntityDamage(@NotNull CombatContext context) { }

    /**
     * Called when a player successfully blocks with a shield.
     *
     * @param context the combat context with defender and damage data
     * @since 0.1.0
     */
    default void onShieldBlock(@NotNull CombatContext context) { }

    /**
     * Called after a block is broken.
     * 
     * <p>The drops list in the context can be modified to change what items
     * are dropped from the broken block.
     *
     * @param context the tool context with player, tool, and block data
     * @since 0.1.0
     */
    default void onBlockBreak(@NotNull ToolContext context) { }

    /**
     * Called before a block break is confirmed (pre-event).
     * 
     * <p>Use this to cancel unbreakable blocks or apply pre-break effects.
     * Cancelling here prevents the block from being broken.
     *
     * @param context the tool context with player, tool, and block data
     * @since 0.1.0
     */
    default void onBlockBreakPre(@NotNull ToolContext context) { }

    /**
     * Called when a player places a block.
     *
     * @param context the tool context with player, tool, and block data
     * @since 0.1.0
     */
    default void onBlockPlace(@NotNull ToolContext context) { }

    /**
     * Called when a player interacts with a block (right-click, left-click).
     *
     * @param context the interaction context with player and block data
     * @since 0.1.0
     */
    default void onBlockInteract(@NotNull InteractionContext context) { }

    /**
     * Called when a player interacts with an entity (right-click).
     *
     * @param context the interaction context with player and entity data
     * @since 0.1.0
     */
    default void onEntityInteract(@NotNull InteractionContext context) { }

    /**
     * Called when a projectile is launched (arrow, trident, etc.).
     * 
     * <p>Modify velocity, gravity, or pierce level through the context.
     *
     * @param context the projectile context with projectile and shooter data
     * @since 0.1.0
     */
    default void onProjectileLaunch(@NotNull ProjectileContext context) { }

    /**
     * Called when a projectile hits a block or entity.
     *
     * @param context the projectile context with hit data
     * @since 0.1.0
     */
    default void onProjectileHit(@NotNull ProjectileContext context) { }

    /**
     * Called when fishing rod actions occur (cast, reel, bite).
     * 
     * <p>Modify wait time and lure speed through the context.
     *
     * @param context the fishing context with player and hook data
     * @since 0.1.0
     */
    default void onFishingAction(@NotNull FishingContext context) { }

    /**
     * Called periodically while an item is held in the main or off hand.
     *
     * @param context the tick context with player and item data
     * @since 0.1.0
     */
    default void onHeldTick(@NotNull TickContext context) { }

    /**
     * Called periodically while an armor item is equipped.
     *
     * @param context the tick context with player and item data
     * @since 0.1.0
     */
    default void onArmorTick(@NotNull TickContext context) { }

    /**
     * Called when an item is used (interacted with).
     *
     * @param context the item context with usage data
     * @since 0.1.0
     */
    default void onItemUsed(@NotNull ItemContext context) { }

    /**
     * Called when an item takes durability damage.
     * 
     * <p>Modify the damage amount through the context.
     *
     * @param context the item context with durability data
     * @since 0.1.0
     */
    default void onDurabilityDamage(@NotNull ItemContext context) { }

    /**
     * Called when a player consumes an item (food, potion).
     * 
     * <p>Modify food level, saturation, and health restoration through
     * the context.
     *
     * @param context the consumable context with consumption data
     * @since 0.1.0
     */
    default void onItemConsume(@NotNull ConsumableContext context) { }

    /**
     * Called when a bow or crossbow is fired.
     * 
     * <p>Modify force and critical hit status through the context.
     *
     * @param context the weapon context with bow and projectile data
     * @since 0.1.0
     */
    default void onBowShoot(@NotNull WeaponContext context) { }

    /**
     * Called when a trident is thrown.
     * 
     * <p>Modify force and pierce level through the context.
     *
     * @param context the weapon context with trident and projectile data
     * @since 0.1.0
     */
    default void onTridentThrow(@NotNull WeaponContext context) { }

    /**
     * Called when an item is dropped.
     *
     * @param context the item context with drop data
     * @since 0.1.0
     */
    default void onItemDrop(@NotNull ItemContext context) { }

    /**
     * Called when an item is picked up.
     *
     * @param context the item context with pickup data
     * @since 0.1.0
     */
    default void onItemPickup(@NotNull ItemContext context) { }

    /**
     * Attempts to cancel the given context if it supports cancellation.
     * 
     * <p>Convenience method for handler implementations.
     *
     * @param context the context object to cancel
     * @return true if cancellation was successful, false if not cancellable
     * @since 0.1.0
     */
    default boolean tryCancel(@NotNull Object context) {
        if (context instanceof Cancellable cancellable) {
            cancellable.setCancelled(true);
            return true;
        }
        return false;
    }

    /**
     * Checks if the given context has been cancelled.
     * 
     * <p>Convenience method for handler implementations.
     *
     * @param context the context object to check
     * @return true if the context is cancellable and cancelled
     * @since 0.1.0
     */
    default boolean isCancelled(@NotNull Object context) {
        return context instanceof Cancellable cancellable && cancellable.isCancelled();
    }
}
