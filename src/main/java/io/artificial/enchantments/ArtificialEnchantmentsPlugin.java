package io.artificial.enchantments;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class ArtificialEnchantmentsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Artificial Enchantments library enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("Artificial Enchantments library disabled");
    }
}
