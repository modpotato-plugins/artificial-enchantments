package io.artificial.enchantments.internal;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Helper for managing attribute modifiers on entities and items.
 * 
 * <p>Provides pure functions for adding/removing attribute modifiers
 * with proper UUID tracking. All operations are thread-safe and
 * idempotent where possible.</p>
 * 
 * <p>Common attributes:
 * <ul>
 *   <li>ATTACK_DAMAGE - Melee damage</li>
 *   <li>ATTACK_SPEED - Attack cooldown speed</li>
 *   <li>ARMOR - Damage reduction</li>
 *   <li>ARMOR_TOUGHNESS - Armor effectiveness</li>
 *   <li>MAX_HEALTH - Maximum health</li>
 *   <li>MOVEMENT_SPEED - Movement speed</li>
 *   <li>KNOCKBACK_RESISTANCE - Knockback reduction</li>
 * </ul></p>
 */
public final class AttributeModifierHelper {

    private AttributeModifierHelper() {
        throw new AssertionError("Utility class");
    }

    /**
     * Adds a temporary attribute modifier to a living entity.
     * 
     * <p>If a modifier with the same UUID already exists, it will be removed
     * and replaced with the new value.</p>
     *
     * @param entity the entity to modify
     * @param attribute the attribute to modify
     * @param uuid unique identifier for this modifier (for tracking/removal)
     * @param amount the amount to add
     * @param operation the operation type (ADD_NUMBER, ADD_SCALAR, MULTIPLY_SCALAR_1)
     * @return true if the modifier was applied successfully
     */
    public static boolean addModifier(
            @NotNull LivingEntity entity,
            @NotNull Attribute attribute,
            @NotNull UUID uuid,
            double amount,
            @NotNull AttributeModifier.Operation operation) {
        
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return false;
        }

        removeModifier(entity, attribute, uuid);

        AttributeModifier modifier = new AttributeModifier(
            uuid,
            "artificial_enchantment_" + uuid.toString().substring(0, 8),
            amount,
            operation
        );
        
        instance.addModifier(modifier);
        return true;
    }

    /**
     * Removes an attribute modifier by UUID.
     *
     * @param entity the entity to modify
     * @param attribute the attribute to modify
     * @param uuid the UUID of the modifier to remove
     * @return true if a modifier was found and removed
     */
    public static boolean removeModifier(
            @NotNull LivingEntity entity,
            @NotNull Attribute attribute,
            @NotNull UUID uuid) {
        
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return false;
        }

        for (AttributeModifier modifier : instance.getModifiers()) {
            if (modifier.getUniqueId().equals(uuid)) {
                instance.removeModifier(modifier);
                return true;
            }
        }
        
        return false;
    }

    /**
     * Removes all modifiers created by this helper from an entity.
     * 
     * <p>Only removes modifiers whose names start with "artificial_enchantment_"</p>
     *
     * @param entity the entity to clean up
     * @return number of modifiers removed
     */
    public static int removeAllModifiers(@NotNull LivingEntity entity) {
        int removed = 0;
        
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            
            for (AttributeModifier modifier : instance.getModifiers()) {
                if (modifier.getName().startsWith("artificial_enchantment_")) {
                    instance.removeModifier(modifier);
                    removed++;
                }
            }
        }
        
        return removed;
    }

    /**
     * Gets the total value of an attribute including all modifiers.
     *
     * @param entity the entity to check
     * @param attribute the attribute to check
     * @return the total attribute value, or 0 if the entity doesn't have the attribute
     */
    public static double getTotalValue(@NotNull LivingEntity entity, @NotNull Attribute attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance != null ? instance.getValue() : 0.0;
    }

    /**
     * Gets the base value of an attribute without modifiers.
     *
     * @param entity the entity to check
     * @param attribute the attribute to check
     * @return the base attribute value, or 0 if the entity doesn't have the attribute
     */
    public static double getBaseValue(@NotNull LivingEntity entity, @NotNull Attribute attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance != null ? instance.getBaseValue() : 0.0;
    }

    /**
     * Adds an attribute modifier to an item stack.
     * 
     * <p>Item modifiers persist when the item is held/worn in the specified slot.</p>
     *
     * @param item the item to modify
     * @param attribute the attribute to modify
     * @param uuid unique identifier for this modifier
     * @param amount the amount to add
     * @param operation the operation type
     * @param slot the equipment slot this modifier applies to
     * @return true if the modifier was applied
     */
    public static boolean addItemModifier(
            @NotNull ItemStack item,
            @NotNull Attribute attribute,
            @NotNull UUID uuid,
            double amount,
            @NotNull AttributeModifier.Operation operation,
            @Nullable EquipmentSlot slot) {
        
        if (!item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        removeItemModifier(item, attribute, uuid);

        AttributeModifier modifier = new AttributeModifier(
            uuid,
            "artificial_enchantment_" + uuid.toString().substring(0, 8),
            amount,
            operation,
            slot
        );
        
        meta.addAttributeModifier(attribute, modifier);
        item.setItemMeta(meta);
        
        return true;
    }

    /**
     * Removes an attribute modifier from an item stack.
     *
     * @param item the item to modify
     * @param attribute the attribute to modify
     * @param uuid the UUID of the modifier to remove
     * @return true if a modifier was found and removed
     */
    public static boolean removeItemModifier(
            @NotNull ItemStack item,
            @NotNull Attribute attribute,
            @NotNull UUID uuid) {
        
        if (!item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        for (AttributeModifier modifier : meta.getAttributeModifiers(attribute)) {
            if (modifier.getUniqueId().equals(uuid)) {
                meta.removeAttributeModifier(attribute, modifier);
                item.setItemMeta(meta);
                return true;
            }
        }
        
        return false;
    }

    /**
     * Generates a unique UUID for attribute modifiers.
     * 
     * <p>The UUID is derived from the enchantment key and target to ensure
     * consistent IDs across server restarts.</p>
     *
     * @param enchantmentKey the enchantment identifier
     * @param target the target attribute or context
     * @return a deterministic UUID for the modifier
     */
    @NotNull
    public static UUID generateModifierUuid(@NotNull String enchantmentKey, @NotNull String target) {
        return UUID.nameUUIDFromBytes((enchantmentKey + ":" + target).getBytes());
    }

    /**
     * Creates a modifier UUID for damage modifiers.
     *
     * @param enchantmentKey the enchantment identifier
     * @return a deterministic UUID for damage modifiers
     */
    @NotNull
    public static UUID damageModifierUuid(@NotNull String enchantmentKey) {
        return generateModifierUuid(enchantmentKey, "damage");
    }

    /**
     * Creates a modifier UUID for attack speed modifiers.
     *
     * @param enchantmentKey the enchantment identifier
     * @return a deterministic UUID for attack speed modifiers
     */
    @NotNull
    public static UUID attackSpeedModifierUuid(@NotNull String enchantmentKey) {
        return generateModifierUuid(enchantmentKey, "attack_speed");
    }

    /**
     * Creates a modifier UUID for armor modifiers.
     *
     * @param enchantmentKey the enchantment identifier
     * @return a deterministic UUID for armor modifiers
     */
    @NotNull
    public static UUID armorModifierUuid(@NotNull String enchantmentKey) {
        return generateModifierUuid(enchantmentKey, "armor");
    }

    /**
     * Creates a modifier UUID for movement speed modifiers.
     *
     * @param enchantmentKey the enchantment identifier
     * @return a deterministic UUID for movement speed modifiers
     */
    @NotNull
    public static UUID movementSpeedModifierUuid(@NotNull String enchantmentKey) {
        return generateModifierUuid(enchantmentKey, "movement_speed");
    }

    /**
     * Applies attack damage modifier to an entity.
     *
     * @param entity the entity to modify
     * @param enchantmentKey the enchantment identifier
     * @param damageBonus the damage amount to add
     * @return true if applied successfully
     */
    public static boolean applyDamageBonus(
            @NotNull LivingEntity entity,
            @NotNull String enchantmentKey,
            double damageBonus) {
        return addModifier(
            entity,
            Attribute.ATTACK_DAMAGE,
            damageModifierUuid(enchantmentKey),
            damageBonus,
            AttributeModifier.Operation.ADD_NUMBER
        );
    }

    /**
     * Applies attack speed modifier to an entity.
     *
     * @param entity the entity to modify
     * @param enchantmentKey the enchantment identifier
     * @param speedBonus the speed amount to add (negative for slower)
     * @return true if applied successfully
     */
    public static boolean applyAttackSpeedBonus(
            @NotNull LivingEntity entity,
            @NotNull String enchantmentKey,
            double speedBonus) {
        return addModifier(
            entity,
            Attribute.ATTACK_SPEED,
            attackSpeedModifierUuid(enchantmentKey),
            speedBonus,
            AttributeModifier.Operation.ADD_NUMBER
        );
    }

    /**
     * Applies armor bonus modifier to an entity.
     *
     * @param entity the entity to modify
     * @param enchantmentKey the enchantment identifier
     * @param armorBonus the armor amount to add
     * @return true if applied successfully
     */
    public static boolean applyArmorBonus(
            @NotNull LivingEntity entity,
            @NotNull String enchantmentKey,
            double armorBonus) {
        return addModifier(
            entity,
            Attribute.ARMOR,
            armorModifierUuid(enchantmentKey),
            armorBonus,
            AttributeModifier.Operation.ADD_NUMBER
        );
    }

    /**
     * Applies movement speed modifier to an entity.
     *
     * @param entity the entity to modify
     * @param enchantmentKey the enchantment identifier
     * @param speedMultiplier the speed multiplier (e.g., 0.1 for +10%)
     * @return true if applied successfully
     */
    public static boolean applyMovementSpeedBonus(
            @NotNull LivingEntity entity,
            @NotNull String enchantmentKey,
            double speedMultiplier) {
        return addModifier(
            entity,
            Attribute.MOVEMENT_SPEED,
            movementSpeedModifierUuid(enchantmentKey),
            speedMultiplier,
            AttributeModifier.Operation.ADD_SCALAR
        );
    }

    /**
     * Gets all active modifiers created by this helper on an entity.
     *
     * @param entity the entity to check
     * @return map of attribute to list of modifier UUIDs
     */
    @NotNull
    public static Map<Attribute, java.util.List<UUID>> getActiveModifiers(@NotNull LivingEntity entity) {
        Map<Attribute, java.util.List<UUID>> result = new HashMap<>();
        
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            
            java.util.List<UUID> uuids = new java.util.ArrayList<>();
            for (AttributeModifier modifier : instance.getModifiers()) {
                if (modifier.getName().startsWith("artificial_enchantment_")) {
                    uuids.add(modifier.getUniqueId());
                }
            }
            
            if (!uuids.isEmpty()) {
                result.put(attribute, uuids);
            }
        }
        
        return result;
    }
}
