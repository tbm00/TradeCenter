package me.spaff.tradecenter.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RecipesUtils {
    private static List<NamespacedKey> recipeNamespaces = new ArrayList<>();

    public static void discoverRecipes(Player player) {
        for (NamespacedKey key : recipeNamespaces) {
            player.discoverRecipe(key);
        }
    }

    public static void addRecipeNamespaceKey(NamespacedKey key) {
        recipeNamespaces.add(key);
    }
}
