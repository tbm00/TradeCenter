package me.spaff.tradecenter.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventoryUtils {
    public static boolean canFitItem(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() != -1) {
            return true;
        }
        else {
            for (int i = 0; i < player.getInventory().getSize(); ++i) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!ItemUtils.isNull(stack) && stack.isSimilar(item)) {
                    int stackAmount = stack.getAmount();
                    int itemAmount = item.getAmount();
                    if (itemAmount + stackAmount <= item.getMaxStackSize()) {
                        return true;
                    }
                }
            }

            return false;
        }
    }
}
