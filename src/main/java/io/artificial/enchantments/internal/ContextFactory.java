package io.artificial.enchantments.internal;

import io.artificial.enchantments.api.EnchantmentDefinition;
import io.artificial.enchantments.api.context.*;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating EffectContext implementations based on Bukkit events.
 *
 * <p>This factory extracts relevant data from Bukkit events and creates the
 * appropriate context implementation for each dispatch event type.
 *
 * @see EffectContext
 * @see EffectDispatchSpine
 */
public final class ContextFactory {

    /**
     * Creates a new context factory.
     */
    public ContextFactory() {
    }

    /**
     * Creates the appropriate context for the given event type.
     *
     * @param eventType the type of event being dispatched
     * @param enchantment the enchantment definition
     * @param level the enchantment level
     * @param scaledValue the calculated scaled value
     * @param bukkitEvent the underlying Bukkit event
     * @param slot the equipment slot (may be null)
     * @return the created context, or null if the event type is not supported
     */
    @Nullable
    public EffectContext createContext(
            @NotNull EffectDispatchSpine.DispatchEventType eventType,
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Event bukkitEvent,
            @Nullable EquipmentSlot slot
    ) {
        return switch (eventType) {
            case ENTITY_DAMAGE_BY_ENTITY, ENTITY_DAMAGE, SHIELD_BLOCK ->
                    createCombatContext(enchantment, level, scaledValue, bukkitEvent, slot);
            case BLOCK_BREAK, BLOCK_BREAK_PRE, BLOCK_PLACE ->
                    createToolContext(enchantment, level, scaledValue, bukkitEvent, eventType, slot);
            case BLOCK_INTERACT, ENTITY_INTERACT ->
                    createInteractionContext(enchantment, level, scaledValue, bukkitEvent, slot);
            case PROJECTILE_LAUNCH, PROJECTILE_HIT ->
                    createProjectileContext(enchantment, level, scaledValue, bukkitEvent, slot);
            case FISHING_ACTION ->
                    createFishingContext(enchantment, level, scaledValue, bukkitEvent, slot);
            case HELD_TICK, ARMOR_TICK ->
                    createTickContext(enchantment, level, scaledValue, bukkitEvent, slot, eventType);
            case ITEM_USED, DURABILITY_DAMAGE, ITEM_DROP, ITEM_PICKUP ->
                    createItemContext(enchantment, level, scaledValue, bukkitEvent, slot, eventType);
            case ITEM_CONSUME ->
                    createConsumableContext(enchantment, level, scaledValue, bukkitEvent, slot);
            case BOW_SHOOT, TRIDENT_THROW ->
                    createWeaponContext(enchantment, level, scaledValue, bukkitEvent, slot, eventType);
        };
    }

    @Nullable
    private CombatContext createCombatContext(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Event bukkitEvent,
            @Nullable EquipmentSlot slot
    ) {
        if (bukkitEvent instanceof EntityDamageByEntityEvent damageEvent) {
            return new CombatContextImpl(
                    enchantment, level, scaledValue, damageEvent, slot
            );
        } else if (bukkitEvent instanceof EntityDamageEvent damageEvent) {
            return new CombatContextImpl(
                    enchantment, level, scaledValue, damageEvent, slot
            );
        }
        return null;
    }

    @Nullable
    private ToolContext createToolContext(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Event bukkitEvent,
            @NotNull EffectDispatchSpine.DispatchEventType eventType,
            @Nullable EquipmentSlot slot
    ) {
        if (bukkitEvent instanceof BlockBreakEvent breakEvent) {
            return new ToolContextImpl(
                    enchantment, level, scaledValue, breakEvent, slot, eventType
            );
        } else if (bukkitEvent instanceof BlockPlaceEvent placeEvent) {
            return new ToolContextImpl(
                    enchantment, level, scaledValue, placeEvent, slot, eventType
            );
        }
        return null;
    }

    @Nullable
    private InteractionContext createInteractionContext(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Event bukkitEvent,
            @Nullable EquipmentSlot slot
    ) {
        if (bukkitEvent instanceof PlayerInteractEvent interactEvent) {
            return new InteractionContextImpl(
                    enchantment, level, scaledValue, interactEvent, slot
            );
        } else if (bukkitEvent instanceof PlayerInteractEntityEvent interactEntityEvent) {
            return new InteractionContextImpl(
                    enchantment, level, scaledValue, interactEntityEvent, slot
            );
        }
        return null;
    }

    @Nullable
    private ProjectileContext createProjectileContext(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Event bukkitEvent,
            @Nullable EquipmentSlot slot
    ) {
        if (bukkitEvent instanceof ProjectileLaunchEvent launchEvent) {
            return new ProjectileContextImpl(
                    enchantment, level, scaledValue, launchEvent, slot
            );
        } else if (bukkitEvent instanceof ProjectileHitEvent hitEvent) {
            return new ProjectileContextImpl(
                    enchantment, level, scaledValue, hitEvent, slot
            );
        }
        return null;
    }

    @Nullable
    private FishingContext createFishingContext(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Event bukkitEvent,
            @Nullable EquipmentSlot slot
    ) {
        if (bukkitEvent instanceof PlayerFishEvent fishEvent) {
            return new FishingContextImpl(
                    enchantment, level, scaledValue, fishEvent, slot
            );
        }
        return null;
    }

    @Nullable
    private TickContext createTickContext(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Event bukkitEvent,
            @Nullable EquipmentSlot slot,
            @NotNull EffectDispatchSpine.DispatchEventType eventType
    ) {
        // Tick contexts are typically not created from Bukkit events
        // They would be created from scheduled tick tasks
        return new TickContextImpl(
                enchantment, level, scaledValue, slot, eventType == EffectDispatchSpine.DispatchEventType.HELD_TICK
        );
    }

    @Nullable
    private ItemContext createItemContext(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Event bukkitEvent,
            @Nullable EquipmentSlot slot,
            @NotNull EffectDispatchSpine.DispatchEventType eventType
    ) {
        if (bukkitEvent instanceof PlayerDropItemEvent dropEvent) {
            return new ItemContextImpl(
                    enchantment, level, scaledValue, dropEvent.getItemDrop().getItemStack(),
                    slot, true, false
            );
        } else if (bukkitEvent instanceof EntityPickupItemEvent pickupEvent) {
            if (pickupEvent.getEntity() instanceof Player player) {
                return new ItemContextImpl(
                        enchantment, level, scaledValue, pickupEvent.getItem().getItemStack(),
                        slot, false, true
                );
            }
        }
        return null;
    }

    @Nullable
    private ConsumableContext createConsumableContext(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Event bukkitEvent,
            @Nullable EquipmentSlot slot
    ) {
        if (bukkitEvent instanceof PlayerItemConsumeEvent consumeEvent) {
            return new ConsumableContextImpl(
                    enchantment, level, scaledValue, consumeEvent, slot
            );
        }
        return null;
    }

    @Nullable
    private WeaponContext createWeaponContext(
            @NotNull EnchantmentDefinition enchantment,
            int level,
            double scaledValue,
            @NotNull Event bukkitEvent,
            @Nullable EquipmentSlot slot,
            @NotNull EffectDispatchSpine.DispatchEventType eventType
    ) {
        if (eventType == EffectDispatchSpine.DispatchEventType.BOW_SHOOT
                && bukkitEvent instanceof EntityShootBowEvent bowEvent) {
            return new WeaponContextImpl(
                    enchantment, level, scaledValue, bowEvent, slot
            );
        }
        if (eventType == EffectDispatchSpine.DispatchEventType.TRIDENT_THROW
                && bukkitEvent instanceof ProjectileLaunchEvent launchEvent) {
            if (launchEvent.getEntity() instanceof org.bukkit.entity.Trident tridentEntity) {
                if (tridentEntity.getShooter() instanceof Player player) {
                    ItemStack weapon;
                    if (slot != null) {
                        weapon = player.getInventory().getItem(slot);
                    } else {
                        weapon = player.getInventory().getItemInMainHand();
                        if (weapon.getType() != org.bukkit.Material.TRIDENT) {
                            weapon = player.getInventory().getItemInOffHand();
                        }
                    }
                    if (weapon != null && weapon.getType() == org.bukkit.Material.TRIDENT) {
                        return new WeaponContextImpl(
                                enchantment, level, scaledValue, player, weapon, tridentEntity, slot
                        );
                    }
                }
            }
        }
        return null;
    }

    // =================================================================================
    // Context Implementations
    // =================================================================================

    /**
     * Abstract base class for all context implementations providing common functionality.
     */
    private abstract static class AbstractContext implements EffectContext {
        protected final EnchantmentDefinition enchantment;
        protected final int level;
        protected final double scaledValue;

        AbstractContext(@NotNull EnchantmentDefinition enchantment, int level, double scaledValue) {
            this.enchantment = enchantment;
            this.level = level;
            this.scaledValue = scaledValue;
        }

        @Override
        @NotNull
        public EnchantmentDefinition getEnchantment() {
            return enchantment;
        }

        @Override
        public int getLevel() {
            return level;
        }

        @Override
        public double getScaledValue() {
            return scaledValue;
        }

        @Override
        public boolean isCancellable() {
            return this instanceof org.bukkit.event.Cancellable;
        }
    }

    /**
     * Implementation of CombatContext for entity damage events.
     */
    private static class CombatContextImpl extends AbstractContext implements CombatContext, org.bukkit.event.Cancellable {
        private final EntityDamageEvent damageEvent;
        private final EquipmentSlot slot;
        private boolean cancelled;
        private double currentDamage;

        CombatContextImpl(
                @NotNull EnchantmentDefinition enchantment,
                int level,
                double scaledValue,
                @NotNull EntityDamageEvent damageEvent,
                @Nullable EquipmentSlot slot
        ) {
            super(enchantment, level, scaledValue);
            this.damageEvent = damageEvent;
            this.slot = slot;
            this.cancelled = false;
            this.currentDamage = damageEvent.getDamage();
        }

        @Override
        @Nullable
        public LivingEntity getAttacker() {
            if (damageEvent instanceof EntityDamageByEntityEvent byEntityEvent) {
                Entity attacker = byEntityEvent.getDamager();
                if (attacker instanceof LivingEntity livingAttacker) {
                    return livingAttacker;
                }
                if (attacker instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) {
                    return shooter;
                }
            }
            return null;
        }

        @Override
        @NotNull
        public LivingEntity getVictim() {
            Entity victim = damageEvent.getEntity();
            if (victim instanceof LivingEntity livingVictim) {
                return livingVictim;
            }
            throw new IllegalStateException("Damage victim is not a LivingEntity");
        }

        @Override
        @NotNull
        public Location getLocation() {
            return damageEvent.getEntity().getLocation();
        }

        @Override
        @Nullable
        public ItemStack getWeapon() {
            LivingEntity attacker = getAttacker();
            if (attacker instanceof Player player && slot != null) {
                return player.getInventory().getItem(slot);
            }
            return null;
        }

        @Override
        public double getBaseDamage() {
            return damageEvent.getDamage();
        }

        @Override
        public double getCurrentDamage() {
            return currentDamage;
        }

        @Override
        public void setDamage(double damage) {
            this.currentDamage = Math.max(0, damage);
            damageEvent.setDamage(currentDamage);
        }

        @Override
        public void addDamage(double amount) {
            setDamage(currentDamage + amount);
        }

        @Override
        public void multiplyDamage(double factor) {
            setDamage(currentDamage * factor);
        }

        @Override
        @NotNull
        public DamageSource getDamageSource() {
            return damageEvent.getDamageSource();
        }

        @Override
        public boolean isMelee() {
            return damageEvent instanceof EntityDamageByEntityEvent byEntityEvent
                    && byEntityEvent.getDamager() instanceof LivingEntity;
        }

        @Override
        public boolean isProjectile() {
            return damageEvent.getDamageSource().getCausingEntity() instanceof Projectile
                    || (damageEvent instanceof EntityDamageByEntityEvent byEntityEvent
                    && byEntityEvent.getDamager() instanceof Projectile);
        }

        @Override
        public boolean isMagic() {
            return damageEvent.getCause() == EntityDamageEvent.DamageCause.MAGIC
                    || damageEvent.getCause() == EntityDamageEvent.DamageCause.POISON
                    || damageEvent.getCause() == EntityDamageEvent.DamageCause.WITHER;
        }

        @Override
        public boolean isExplosion() {
            return damageEvent.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                    || damageEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;
        }

        @Override
        public boolean isCritical() {
            // Critical hits are tracked via metadata or can be detected from attack context
            return false;
        }

        @Override
        public boolean isBlocking() {
            Entity victim = damageEvent.getEntity();
            if (victim instanceof Player player) {
                return player.isBlocking();
            }
            return false;
        }

        @Override
        public boolean isShieldBlock() {
            return damageEvent.isCancelled() && isBlocking();
        }

        @Override
        public boolean isArmored() {
            return getVictim().getEquipment() != null
                    && getVictim().getEquipment().getArmorContents().length > 0;
        }

        @Override
        public boolean isCancelled() {
            return cancelled || damageEvent.isCancelled();
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    /**
     * Implementation of ToolContext for block-related events.
     */
    private static class ToolContextImpl extends AbstractContext implements ToolContext, org.bukkit.event.Cancellable {
        private final Player player;
        private final ItemStack tool;
        private final Block block;
        private final BlockFace face;
        private final EffectDispatchSpine.DispatchEventType eventType;
        private final List<ItemStack> drops;
        private int expToDrop;
        private boolean cancelled;

        ToolContextImpl(
                @NotNull EnchantmentDefinition enchantment,
                int level,
                double scaledValue,
                @NotNull BlockBreakEvent breakEvent,
                @Nullable EquipmentSlot slot,
                @NotNull EffectDispatchSpine.DispatchEventType eventType
        ) {
            super(enchantment, level, scaledValue);
            this.player = breakEvent.getPlayer();
            this.tool = slot != null ? player.getInventory().getItem(slot) : player.getInventory().getItemInMainHand();
            this.block = breakEvent.getBlock();
            this.face = null;
            this.eventType = eventType;
            this.drops = new ArrayList<>();
            this.expToDrop = breakEvent.getExpToDrop();
            this.cancelled = breakEvent.isCancelled();
        }

        ToolContextImpl(
                @NotNull EnchantmentDefinition enchantment,
                int level,
                double scaledValue,
                @NotNull BlockPlaceEvent placeEvent,
                @Nullable EquipmentSlot slot,
                @NotNull EffectDispatchSpine.DispatchEventType eventType
        ) {
            super(enchantment, level, scaledValue);
            this.player = placeEvent.getPlayer();
            this.tool = slot != null ? player.getInventory().getItem(slot) : player.getInventory().getItemInMainHand();
            this.block = placeEvent.getBlock();
            this.face = placeEvent.getBlock().getFace(placeEvent.getBlockPlaced());
            this.eventType = eventType;
            this.drops = null;
            this.expToDrop = 0;
            this.cancelled = placeEvent.isCancelled();
        }

        @Override
        @NotNull
        public Player getPlayer() {
            return player;
        }

        @Override
        @NotNull
        public ItemStack getTool() {
            return tool;
        }

        @Override
        @NotNull
        public Block getBlock() {
            return block;
        }

        @Override
        @NotNull
        public Location getLocation() {
            return block.getLocation();
        }

        @Override
        @Nullable
        public BlockFace getBlockFace() {
            return face;
        }

        @Override
        public boolean isBreak() {
            return eventType == EffectDispatchSpine.DispatchEventType.BLOCK_BREAK;
        }

        @Override
        public boolean isPreBreak() {
            return eventType == EffectDispatchSpine.DispatchEventType.BLOCK_BREAK_PRE;
        }

        @Override
        public boolean isPlace() {
            return eventType == EffectDispatchSpine.DispatchEventType.BLOCK_PLACE;
        }

        @Override
        public boolean isInteract() {
            return eventType == EffectDispatchSpine.DispatchEventType.BLOCK_INTERACT;
        }

        @Override
        @Nullable
        public List<ItemStack> getDrops() {
            return drops;
        }

        @Override
        public void setDrops(@Nullable List<ItemStack> drops) {
            if (this.drops != null) {
                this.drops.clear();
                if (drops != null) {
                    this.drops.addAll(drops);
                }
            }
        }

        @Override
        public void addDrop(@NotNull ItemStack item) {
            if (drops != null) {
                drops.add(item);
            }
        }

        @Override
        public boolean removeDrop(@NotNull ItemStack item) {
            return drops != null && drops.remove(item);
        }

        @Override
        public int getExpToDrop() {
            return expToDrop;
        }

        @Override
        public void setExpToDrop(int exp) {
            this.expToDrop = Math.max(0, exp);
        }

        @Override
        public boolean willDropItems() {
            return isBreak() && drops != null;
        }

        @Override
        public boolean willDamageTool() {
            return isBreak();
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    /**
     * Implementation of InteractionContext for player interaction events.
     */
    private static class InteractionContextImpl extends AbstractContext implements InteractionContext, org.bukkit.event.Cancellable {
        private final Player player;
        private final ItemStack item;
        private final EquipmentSlot hand;
        private final Location location;
        private final Block block;
        private final BlockFace blockFace;
        private final Entity targetEntity;
        private final boolean isLeftClick;
        private final boolean isRightClick;
        private boolean cancelled;

        InteractionContextImpl(
                @NotNull EnchantmentDefinition enchantment,
                int level,
                double scaledValue,
                @NotNull PlayerInteractEvent interactEvent,
                @Nullable EquipmentSlot slot
        ) {
            super(enchantment, level, scaledValue);
            this.player = interactEvent.getPlayer();
            this.hand = interactEvent.getHand() != null ? interactEvent.getHand() : EquipmentSlot.HAND;
            this.item = interactEvent.getItem() != null ? interactEvent.getItem() : new ItemStack(org.bukkit.Material.AIR);
            this.location = interactEvent.getInteractionPoint() != null
                    ? interactEvent.getInteractionPoint().toLocation(player.getWorld())
                    : player.getLocation();
            this.block = interactEvent.getClickedBlock();
            this.blockFace = interactEvent.getBlockFace();
            this.targetEntity = null;
            this.isLeftClick = interactEvent.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_AIR
                    || interactEvent.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK;
            this.isRightClick = interactEvent.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                    || interactEvent.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;
            this.cancelled = interactEvent.useInteractedBlock() == org.bukkit.event.Event.Result.DENY;
        }

        InteractionContextImpl(
                @NotNull EnchantmentDefinition enchantment,
                int level,
                double scaledValue,
                @NotNull PlayerInteractEntityEvent interactEntityEvent,
                @Nullable EquipmentSlot slot
        ) {
            super(enchantment, level, scaledValue);
            this.player = interactEntityEvent.getPlayer();
            this.hand = interactEntityEvent.getHand() != null ? interactEntityEvent.getHand() : EquipmentSlot.HAND;
            this.item = player.getInventory().getItem(hand);
            this.location = interactEntityEvent.getRightClicked().getLocation();
            this.block = null;
            this.blockFace = null;
            this.targetEntity = interactEntityEvent.getRightClicked();
            this.isLeftClick = false;
            this.isRightClick = true;
            this.cancelled = interactEntityEvent.isCancelled();
        }

        @Override
        @NotNull
        public Player getPlayer() {
            return player;
        }

        @Override
        @NotNull
        public ItemStack getItem() {
            return item;
        }

        @Override
        @NotNull
        public EquipmentSlot getHand() {
            return hand;
        }

        @Override
        @NotNull
        public Location getLocation() {
            return location;
        }

        @Override
        public boolean isBlockInteraction() {
            return block != null && targetEntity == null;
        }

        @Override
        public boolean isEntityInteraction() {
            return targetEntity != null;
        }

        @Override
        @Nullable
        public Block getBlock() {
            return block;
        }

        @Override
        @Nullable
        public BlockFace getBlockFace() {
            return blockFace;
        }

        @Override
        @Nullable
        public Entity getTargetEntity() {
            return targetEntity;
        }

        @Override
        public boolean isLeftClick() {
            return isLeftClick;
        }

        @Override
        public boolean isRightClick() {
            return isRightClick;
        }

        @Override
        public boolean isSneaking() {
            return player.isSneaking();
        }

        @Override
        @Nullable
        public org.bukkit.util.Vector getInteractionPoint() {
            return isBlockInteraction() && block != null
                    ? location.toVector()
                    : null;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    /**
     * Implementation of ProjectileContext for projectile events.
     */
    private static class ProjectileContextImpl extends AbstractContext implements ProjectileContext, org.bukkit.event.Cancellable {
        private final Projectile projectile;
        private final LivingEntity shooter;
        private final Location launchLocation;
        private final ItemStack weapon;
        private final Entity hitEntity;
        private final Block hitBlock;
        private final org.bukkit.util.Vector hitPosition;
        private final boolean isLaunch;
        private boolean cancelled;

        ProjectileContextImpl(
                @NotNull EnchantmentDefinition enchantment,
                int level,
                double scaledValue,
                @NotNull ProjectileLaunchEvent launchEvent,
                @Nullable EquipmentSlot slot
        ) {
            super(enchantment, level, scaledValue);
            this.projectile = launchEvent.getEntity();
            this.shooter = projectile.getShooter() instanceof LivingEntity livingShooter ? livingShooter : null;
            this.launchLocation = projectile.getLocation();
            this.weapon = slot != null && shooter instanceof Player player
                    ? player.getInventory().getItem(slot)
                    : new ItemStack(org.bukkit.Material.AIR);
            this.hitEntity = null;
            this.hitBlock = null;
            this.hitPosition = null;
            this.isLaunch = true;
            this.cancelled = launchEvent.isCancelled();
        }

        ProjectileContextImpl(
                @NotNull EnchantmentDefinition enchantment,
                int level,
                double scaledValue,
                @NotNull ProjectileHitEvent hitEvent,
                @Nullable EquipmentSlot slot
        ) {
            super(enchantment, level, scaledValue);
            this.projectile = hitEvent.getEntity();
            this.shooter = projectile.getShooter() instanceof LivingEntity livingShooter ? livingShooter : null;
            this.launchLocation = projectile.getLocation();
            this.weapon = slot != null && shooter instanceof Player player
                    ? player.getInventory().getItem(slot)
                    : new ItemStack(org.bukkit.Material.AIR);
            this.hitEntity = hitEvent.getHitEntity();
            this.hitBlock = hitEvent.getHitBlock();
            this.hitPosition = hitEvent.getHitBlock() != null ? hitEvent.getHitBlock().getLocation().toVector() : null;
            this.isLaunch = false;
            this.cancelled = false;
        }

        @Override
        @NotNull
        public Projectile getProjectile() {
            return projectile;
        }

        @Override
        @Nullable
        public LivingEntity getShooter() {
            return shooter;
        }

        @Override
        @NotNull
        public Location getLaunchLocation() {
            return launchLocation;
        }

        @Override
        @NotNull
        public ItemStack getWeapon() {
            return weapon;
        }

        @Override
        @Nullable
        public Entity getHitEntity() {
            return hitEntity;
        }

        @Override
        @Nullable
        public Block getHitBlock() {
            return hitBlock;
        }

        @Override
        @Nullable
        public org.bukkit.util.Vector getHitPosition() {
            return hitPosition;
        }

        @Override
        public boolean hasHitEntity() {
            return hitEntity != null;
        }

        @Override
        public boolean hasHitBlock() {
            return hitBlock != null;
        }

        @Override
        public boolean isLaunch() {
            return isLaunch;
        }

        @Override
        public boolean isHit() {
            return !isLaunch;
        }

        @Override
        public void setVelocity(@NotNull org.bukkit.util.Vector velocity) {
            projectile.setVelocity(velocity);
        }

        @Override
        @NotNull
        public org.bukkit.util.Vector getVelocity() {
            return projectile.getVelocity();
        }

        @Override
        public void setGravity(boolean gravity) {
            projectile.setGravity(gravity);
        }

        @Override
        public boolean hasGravity() {
            return projectile.hasGravity();
        }

        @Override
        public void setPierceLevel(int level) {
            // Only available for AbstractArrow in newer versions
            if (projectile instanceof org.bukkit.entity.AbstractArrow arrow) {
                arrow.setPierceLevel(level);
            }
        }

        @Override
        public int getPierceLevel() {
            if (projectile instanceof org.bukkit.entity.AbstractArrow arrow) {
                return arrow.getPierceLevel();
            }
            return 0;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    /**
     * Implementation of FishingContext for fishing events.
     */
    private static class FishingContextImpl extends AbstractContext implements FishingContext, org.bukkit.event.Cancellable {
        private final Player player;
        private final ItemStack fishingRod;
        private final FishHook hook;
        private final Location location;
        private final Item caughtItem;
        private final Entity caughtEntity;
        private final org.bukkit.event.player.PlayerFishEvent.State state;
        private int waitTime;
        private int lureSpeed;
        private boolean applyLure;
        private boolean cancelled;

        FishingContextImpl(
                @NotNull EnchantmentDefinition enchantment,
                int level,
                double scaledValue,
                @NotNull PlayerFishEvent fishEvent,
                @Nullable EquipmentSlot slot
        ) {
            super(enchantment, level, scaledValue);
            this.player = fishEvent.getPlayer();
            this.fishingRod = slot != null ? player.getInventory().getItem(slot) : player.getInventory().getItemInMainHand();
            this.hook = fishEvent.getHook();
            this.location = hook.getLocation();
            this.caughtItem = fishEvent.getCaught() instanceof Item item ? item : null;
            this.caughtEntity = fishEvent.getCaught() instanceof Entity entity && !(entity instanceof Item) ? entity : null;
            this.state = fishEvent.getState();
            this.waitTime = 100 + (int) (Math.random() * 400);
            this.lureSpeed = 1;
            this.applyLure = true;
            this.cancelled = fishEvent.isCancelled();
        }

        @Override
        @NotNull
        public Player getPlayer() {
            return player;
        }

        @Override
        @NotNull
        public ItemStack getFishingRod() {
            return fishingRod;
        }

        @Override
        @NotNull
        public FishHook getHook() {
            return hook;
        }

        @Override
        @NotNull
        public Location getLocation() {
            return location;
        }

        @Override
        @Nullable
        public Item getCaughtItem() {
            return caughtItem;
        }

        @Override
        @Nullable
        public Entity getCaughtEntity() {
            return caughtEntity;
        }

        @Override
        public boolean isCast() {
            return state == org.bukkit.event.player.PlayerFishEvent.State.FISHING;
        }

        @Override
        public boolean isReel() {
            return state == org.bukkit.event.player.PlayerFishEvent.State.REEL_IN
                    || state == org.bukkit.event.player.PlayerFishEvent.State.IN_GROUND;
        }

        @Override
        public boolean isBite() {
            return state == org.bukkit.event.player.PlayerFishEvent.State.BITE;
        }

        @Override
        public boolean hasCaughtItem() {
            return caughtItem != null;
        }

        @Override
        public boolean hasCaughtEntity() {
            return caughtEntity != null;
        }

        @Override
        public void setWaitTime(int ticks) {
            this.waitTime = Math.max(0, ticks);
        }

        @Override
        public int getWaitTime() {
            return waitTime;
        }

        @Override
        public void setLureSpeed(int speed) {
            this.lureSpeed = Math.max(1, speed);
        }

        @Override
        public int getLureSpeed() {
            return lureSpeed;
        }

        @Override
        public void setApplyLure(boolean apply) {
            this.applyLure = apply;
        }

        @Override
        public boolean isApplyLure() {
            return applyLure;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    /**
     * Implementation of TickContext for scheduled tick events.
     */
    private static class TickContextImpl extends AbstractContext implements TickContext {
        private final Player player;
        private final ItemStack item;
        private final EquipmentSlot slot;
        private final boolean isHeld;
        private int tickCount;
        private long heldDuration;

        TickContextImpl(
                @NotNull EnchantmentDefinition enchantment,
                int level,
                double scaledValue,
                @Nullable EquipmentSlot slot,
                boolean isHeld
        ) {
            super(enchantment, level, scaledValue);
            this.slot = slot != null ? slot : EquipmentSlot.HAND;
            this.isHeld = isHeld;
            this.player = null;
            this.item = new ItemStack(org.bukkit.Material.AIR);
            this.tickCount = 0;
            this.heldDuration = 0;
        }

        @Override
        @NotNull
        public Player getPlayer() {
            if (player == null) {
                throw new IllegalStateException("Tick context not associated with a player");
            }
            return player;
        }

        @Override
        @NotNull
        public ItemStack getItem() {
            return item;
        }

        @Override
        @NotNull
        public EquipmentSlot getSlot() {
            return slot;
        }

        @Override
        @NotNull
        public Location getLocation() {
            return player != null ? player.getLocation() : new Location(null, 0, 0, 0);
        }

        @Override
        public boolean isHeld() {
            return isHeld;
        }

        @Override
        public boolean isArmor() {
            return !isHeld;
        }

        @Override
        public int getTickCount() {
            return tickCount;
        }

        @Override
        public long getHeldDuration() {
            return heldDuration;
        }
    }

    /**
     * Implementation of ItemContext for item-related events.
     */
    private static class ItemContextImpl extends AbstractContext implements ItemContext, org.bukkit.event.Cancellable {
        private final Player player;
        private final ItemStack item;
        private final EquipmentSlot slot;
        private final boolean isDrop;
        private final boolean isPickup;
        private int currentDurability;
        private int maxDurability;
        private int damageTaken;
        private boolean cancelled;

        ItemContextImpl(
                @NotNull EnchantmentDefinition enchantment,
                int level,
                double scaledValue,
                @NotNull ItemStack item,
                @Nullable EquipmentSlot slot,
                boolean isDrop,
                boolean isPickup
        ) {
            super(enchantment, level, scaledValue);
            this.player = null;
            this.item = item.clone();
            this.slot = slot;
            this.isDrop = isDrop;
            this.isPickup = isPickup;
            this.currentDurability = item.getType().getMaxDurability() - item.getDurability();
            this.maxDurability = item.getType().getMaxDurability();
            this.damageTaken = 0;
            this.cancelled = false;
        }

        @Override
        @Nullable
        public Player getPlayer() {
            return player;
        }

        @Override
        @NotNull
        public ItemStack getItem() {
            return item;
        }

        @Override
        @Nullable
        public EquipmentSlot getSlot() {
            return slot;
        }

        @Override
        @NotNull
        public Location getLocation() {
            return player != null ? player.getLocation() : new Location(null, 0, 0, 0);
        }

        @Override
        public int getCurrentDurability() {
            return currentDurability;
        }

        @Override
        public int getMaxDurability() {
            return maxDurability;
        }

        @Override
        public int getDamageTaken() {
            return damageTaken;
        }

        @Override
        public void setDamageTaken(int damage) {
            this.damageTaken = Math.max(0, damage);
        }

        @Override
        public void reduceDamage(int reduction) {
            this.damageTaken = Math.max(0, damageTaken - reduction);
        }

        @Override
        public boolean willBreak() {
            return currentDurability - damageTaken <= 0;
        }

        @Override
        public boolean isDrop() {
            return isDrop;
        }

        @Override
        public boolean isPickup() {
            return isPickup;
        }

        @Override
        public boolean isCraftingIngredient() {
            return false;
        }

        @Override
        public boolean isAnvilCombination() {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    /**
     * Implementation of ConsumableContext for consumable item events.
     */
    private static class ConsumableContextImpl extends AbstractContext implements ConsumableContext, org.bukkit.event.Cancellable {
        private final Player player;
        private final ItemStack item;
        private final org.bukkit.Material material;
        private int foodLevel;
        private float saturation;
        private double healthRestored;
        private int consumptionTime;
        private boolean cancelled;

        ConsumableContextImpl(
                @NotNull EnchantmentDefinition enchantment,
                int level,
                double scaledValue,
                @NotNull PlayerItemConsumeEvent consumeEvent,
                @Nullable EquipmentSlot slot
        ) {
            super(enchantment, level, scaledValue);
            this.player = consumeEvent.getPlayer();
            this.item = consumeEvent.getItem().clone();
            this.material = item.getType();
            this.foodLevel = player.getFoodLevel();
            this.saturation = player.getSaturation();
            this.healthRestored = 0;
            this.consumptionTime = material.isEdible() ? 32 : 0;
            this.cancelled = consumeEvent.isCancelled();
        }

        @Override
        @NotNull
        public Player getPlayer() {
            return player;
        }

        @Override
        @NotNull
        public ItemStack getItem() {
            return item;
        }

        @Override
        @NotNull
        public Location getLocation() {
            return player.getLocation();
        }

        @Override
        public int getFoodLevel() {
            return foodLevel;
        }

        @Override
        public void setFoodLevel(int level) {
            this.foodLevel = Math.max(0, Math.min(20, level));
        }

        @Override
        public float getSaturation() {
            return saturation;
        }

        @Override
        public void setSaturation(float saturation) {
            this.saturation = Math.max(0, Math.min(player.getFoodLevel(), saturation));
        }

        @Override
        public double getHealthRestored() {
            return healthRestored;
        }

        @Override
        public void setHealthRestored(double health) {
            this.healthRestored = Math.max(0, health);
        }

        @Override
        public boolean isFood() {
            return material.isEdible() && !isPotion();
        }

        @Override
        public boolean isPotion() {
            return material == org.bukkit.Material.POTION
                    || material == org.bukkit.Material.SPLASH_POTION
                    || material == org.bukkit.Material.LINGERING_POTION;
        }

        @Override
        public boolean isDrink() {
            return isPotion() || material == org.bukkit.Material.MILK_BUCKET
                    || material == org.bukkit.Material.HONEY_BOTTLE;
        }

        @Override
        public boolean isAlwaysEdible() {
            return material == org.bukkit.Material.GOLDEN_APPLE
                    || material == org.bukkit.Material.ENCHANTED_GOLDEN_APPLE
                    || material == org.bukkit.Material.CHORUS_FRUIT
                    || material == org.bukkit.Material.MILK_BUCKET
                    || isPotion();
        }

        @Override
        public int getConsumptionTime() {
            return consumptionTime;
        }

        @Override
        public void setConsumptionTime(int ticks) {
            this.consumptionTime = Math.max(0, ticks);
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    /**
     * Implementation of WeaponContext for weapon-related events.
     */
    private static class WeaponContextImpl extends AbstractContext implements WeaponContext {
        private final Player player;
        private final ItemStack weapon;
        private final Location location;
        private final LivingEntity target;
        private final Projectile projectile;
        private final boolean isBow;
        private final boolean isShooting;
        private float force;
        private boolean critical;

        WeaponContextImpl(
                @NotNull EnchantmentDefinition enchantment,
                int level,
                double scaledValue,
                @NotNull EntityShootBowEvent bowEvent,
                @Nullable EquipmentSlot slot
        ) {
            super(enchantment, level, scaledValue);
            this.player = (Player) bowEvent.getEntity();
            this.weapon = bowEvent.getBow() != null ? bowEvent.getBow() : new ItemStack(org.bukkit.Material.BOW);
            this.location = player.getLocation();
            this.target = null;
            this.projectile = bowEvent.getProjectile() instanceof Projectile proj ? proj : null;
            this.isBow = weapon.getType() == org.bukkit.Material.BOW
                    || weapon.getType() == org.bukkit.Material.CROSSBOW;
            this.isShooting = true;
            this.force = bowEvent.getForce();
            this.critical = false;
        }

        WeaponContextImpl(
                @NotNull EnchantmentDefinition enchantment,
                int level,
                double scaledValue,
                @NotNull Player player,
                @NotNull ItemStack weapon,
                @NotNull Projectile projectile,
                @Nullable EquipmentSlot slot
        ) {
            super(enchantment, level, scaledValue);
            this.player = player;
            this.weapon = weapon;
            this.location = player.getLocation();
            this.target = null;
            this.projectile = projectile;
            this.isBow = false;
            this.isShooting = false;
            this.force = 1.0f;
            this.critical = false;
        }

        @Override
        @Nullable
        public Player getPlayer() {
            return player;
        }

        @Override
        @NotNull
        public ItemStack getWeapon() {
            return weapon;
        }

        @Override
        @NotNull
        public Location getLocation() {
            return location;
        }

        @Override
        @Nullable
        public LivingEntity getTarget() {
            return target;
        }

        @Override
        @Nullable
        public Projectile getProjectile() {
            return projectile;
        }

        @Override
        public boolean isBow() {
            return isBow && weapon.getType() == org.bukkit.Material.BOW;
        }

        @Override
        public boolean isCrossbow() {
            return isBow && weapon.getType() == org.bukkit.Material.CROSSBOW;
        }

        @Override
        public boolean isTrident() {
            return weapon.getType() == org.bukkit.Material.TRIDENT;
        }

        @Override
        public boolean isShooting() {
            return isShooting;
        }

        @Override
        public boolean isThrowing() {
            return isTrident();
        }

        @Override
        public float getForce() {
            return force;
        }

        @Override
        public void setForce(float force) {
            this.force = Math.max(0, Math.min(1, force));
        }

        @Override
        public boolean isCritical() {
            return critical;
        }

        @Override
        public void setCritical(boolean critical) {
            this.critical = critical;
        }

        @Override
        public int getPierceLevel() {
            if (projectile instanceof org.bukkit.entity.AbstractArrow arrow) {
                return arrow.getPierceLevel();
            }
            return 0;
        }

        @Override
        public void setPierceLevel(int level) {
            if (projectile instanceof org.bukkit.entity.AbstractArrow arrow) {
                arrow.setPierceLevel(level);
            }
        }
    }
}
