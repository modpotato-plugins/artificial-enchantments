package io.artificial.enchantments.api.context;

import io.artificial.enchantments.api.EnchantmentDefinition;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

public interface EffectContext {

    @NotNull
    EnchantmentDefinition getEnchantment();

    int getLevel();

    double getScaledValue();

    boolean isCancellable();

    default boolean tryCancel() {
        if (this instanceof Cancellable cancellable) {
            cancellable.setCancelled(true);
            return true;
        }
        return false;
    }

    default boolean isCancelled() {
        return this instanceof Cancellable cancellable && cancellable.isCancelled();
    }
}
