package me.spaff.tradecenter;

import me.spaff.tradecenter.cmd.TCCommands;
import me.spaff.tradecenter.config.Config;
import me.spaff.tradecenter.listener.PlayerListener;
import me.spaff.tradecenter.listener.ServerListener;
import me.spaff.tradecenter.nms.PacketReader;
import me.spaff.tradecenter.tradecenter.TradeCenter;
import me.spaff.tradecenter.utils.RecipesUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    public static final String version = "1.0.2";
    private static Main instance;

    public Main() {
        instance = this;
    }

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        registerListeners();
        registerCommands();

        Bukkit.getOnlinePlayers().forEach(player -> {
            PacketReader.uninjectPlayer(player);
            PacketReader.injectPlayer(player);
        });

        Config.load();
        registerRecipes();
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    // Registers

    private void registerListeners() {
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        Bukkit.getServer().getPluginManager().registerEvents(new ServerListener(), this);
    }

    private void registerCommands() {
        this.getCommand("tradecenter").setExecutor(new TCCommands());
    }

    private void registerRecipes() {
        if (!Config.readBool("trade-center.item.can-craft")) return;

        NamespacedKey key = new NamespacedKey(Main.getInstance(), "trade_center");
        ShapedRecipe recipe = new ShapedRecipe(key, TradeCenter.getTradeCenterItem());

        recipe.shape(
                " E ",
                "DCD",
                "S S"
        );

        recipe.setIngredient('E', Material.EMERALD);
        recipe.setIngredient('D', Material.DARK_OAK_PLANKS);
        recipe.setIngredient('S', Material.STICK);
        recipe.setIngredient('C', Material.RED_CARPET);

        Bukkit.addRecipe(recipe);
        RecipesUtils.addRecipeNamespaceKey(key);
    }
}