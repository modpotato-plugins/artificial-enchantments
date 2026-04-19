# Task 7: Effect Runtime and Dispatch Spine - Learnings

## Architecture Overview

The dispatch system follows a dual-path architecture with a single unified spine:

1. **EffectDispatchSpine** - Central coordinator that processes all effects
2. **TypedCallbackDispatcher** - First path: invokes EnchantmentEffectHandler methods
3. **EventBusDispatcher** - Second path: dispatches EnchantEffectEvent to listeners
4. **ContextFactory** - Creates appropriate EffectContext implementations

## Key Design Patterns

### Unified Dispatch Spine
- Both typed callbacks and event bus flow through EffectDispatchSpine
- Ensures single point of validation, calculation, and cancellation
- Prevents duplicate effect execution

### Dispatch Flow
1. Calculate scaled value using LevelScaling
2. Create EffectContext via ContextFactory
3. Fire typed callback path (if handler exists)
4. Check cancellation - stop if cancelled
5. Fire event bus path (if listeners registered)
6. Check final cancellation

### Context Objects
Context implementations are private inner classes in ContextFactory:
- CombatContextImpl - wraps EntityDamageEvent/EntityDamageByEntityEvent
- ToolContextImpl - wraps BlockBreakEvent/BlockPlaceEvent
- InteractionContextImpl - wraps PlayerInteractEvent/PlayerInteractEntityEvent
- ProjectileContextImpl - wraps ProjectileLaunchEvent/ProjectileHitEvent
- FishingContextImpl - wraps PlayerFishEvent
- TickContextImpl - for scheduled tick events
- ItemContextImpl - for drop/pickup events
- ConsumableContextImpl - wraps PlayerItemConsumeEvent
- WeaponContextImpl - wraps EntityShootBowEvent

All contexts implement Cancellable to support cancellation propagation.

## Cancellation Propagation

The cancellation flow works bidirectionally:
1. Handler/Listener cancels → context.setCancelled(true)
2. Context cancellation → EffectDispatchSpine detects via context.isCancelled()
3. Spine propagates to Bukkit event → ((Cancellable)bukkitEvent).setCancelled(true)

For event bus path, check isCancellable() before calling checkCancelled() to avoid IllegalStateException.

## Thread Safety

FoliaScheduler provides abstraction for:
- Global region thread execution
- Location-based region thread execution
- Entity-specific scheduler execution

Immediate dispatch (synchronous) executes directly for performance. Async events execute on their calling thread.

## Event Type Mapping

DispatchEventType enum maps to:
- Handler methods in EnchantmentEffectHandler
- Event subclasses in event package (CombatEvent, ToolEvent, etc.)
- Context implementations in ContextFactory

## Integration Points

- EnchantmentDefinition provides: getEffectHandler(), calculateScaledValue()
- EnchantmentEventBus provides: dispatch(EnchantEffectEvent)
- EffectContext provides: isCancelled(), tryCancel()
- Bukkit Cancellable provides: setCancelled(), isCancelled()
