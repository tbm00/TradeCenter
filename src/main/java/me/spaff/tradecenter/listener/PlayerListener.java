package me.spaff.tradecenter.listener;

import me.spaff.tradecenter.Constants;
import me.spaff.tradecenter.nms.PacketReader;
import me.spaff.tradecenter.tradecenter.TradeCenter;
import me.spaff.tradecenter.utils.*;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {
    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        PacketReader.injectPlayer(e.getPlayer());
        TradeCenter.clearPlayerData(e.getPlayer());
        RecipesUtils.discoverRecipes(e.getPlayer());
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent e) {
        PacketReader.uninjectPlayer(e.getPlayer());
        TradeCenter.clearPlayerData(e.getPlayer());
    }

    @EventHandler
    public void onInventoryCloseEvent(InventoryCloseEvent e) {
        TradeCenter.clearPlayerData((Player) e.getPlayer());
    }

    @EventHandler
    public void onTradeSelectEvent(TradeSelectEvent e) {
        Player player = (Player) e.getWhoClicked();
        if (TradeCenter.getPlayerData(player) == null) return;  // Player did not selected trade in trade center menu
        TradeCenter.getPlayerData(player).setSelectedTrade(e.getIndex());
    }

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent e) {
        TradeCenter.handleClickEvent(e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlaceEvent(BlockPlaceEvent e) {
        ItemStack itemUsed = e.getItemInHand();
        Block block = e.getBlock();

        if (e.isCancelled()) return;

        if (!ItemUtils.isNull(itemUsed) && TradeCenter.isTradeCenterItem(itemUsed)) {
            block.setType(Constants.TRADE_CENTER_BLOCK_TYPE);
            new TradeCenter(block.getLocation()).onPlace();
        }
    }

    @EventHandler
    public void onBlockBreakEvent(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();

        if (TradeCenter.isTradeCenter(block)) {
            e.setDropItems(false);
            new TradeCenter(block.getLocation()).onBreak();

            if (player.getGameMode().equals(GameMode.SURVIVAL))
                block.getWorld().dropItem(block.getLocation().clone().add(0.5, 0.5, 0.5), TradeCenter.getTradeCenterItem());
        }
    }

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Action action = e.getAction();
        Block block = e.getClickedBlock();

        if (block == null) return;
        if (!action.equals(Action.RIGHT_CLICK_BLOCK)) return;
        if (!e.getHand().equals(EquipmentSlot.HAND)) return;

        if (TradeCenter.isTradeCenter(block)) {
            if (e.isBlockInHand() && player.isSneaking()) return;
            e.setCancelled(true);

            new TradeCenter(block.getLocation()).open(player);
        }
    }
}
