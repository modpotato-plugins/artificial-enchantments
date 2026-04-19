package io.artificial.enchantments.api;

import io.artificial.enchantments.api.context.*;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

public interface EnchantmentEffectHandler {

    default void onEntityDamageByEntity(@NotNull CombatContext context) { }
    default void onEntityDamage(@NotNull CombatContext context) { }
    default void onShieldBlock(@NotNull CombatContext context) { }
    default void onBlockBreak(@NotNull ToolContext context) { }
    default void onBlockBreakPre(@NotNull ToolContext context) { }
    default void onBlockPlace(@NotNull ToolContext context) { }
    default void onBlockInteract(@NotNull InteractionContext context) { }
    default void onEntityInteract(@NotNull InteractionContext context) { }
    default void onProjectileLaunch(@NotNull ProjectileContext context) { }
    default void onProjectileHit(@NotNull ProjectileContext context) { }
    default void onFishingAction(@NotNull FishingContext context) { }
    default void onHeldTick(@NotNull TickContext context) { }
    default void onArmorTick(@NotNull TickContext context) { }
    default void onItemUsed(@NotNull ItemContext context) { }
    default void onDurabilityDamage(@NotNull ItemContext context) { }
    default void onItemConsume(@NotNull ConsumableContext context) { }
    default void onBowShoot(@NotNull WeaponContext context) { }
    default void onTridentThrow(@NotNull WeaponContext context) { }
    default void onItemDrop(@NotNull ItemContext context) { }
    default void onItemPickup(@NotNull ItemContext context) { }

    default boolean tryCancel(@NotNull Object context) {
        if (context instanceof Cancellable cancellable) {
            cancellable.setCancelled(true);
            return true;
        }
        return false;
    }

    default boolean isCancelled(@NotNull Object context) {
        return context instanceof Cancellable cancellable && cancellable.isCancelled();
    }
}
