package me.spaff.tradecenter.listener;

import me.spaff.tradecenter.tradecenter.TradeCenter;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class ServerListener implements Listener {
    @EventHandler
    public void onBlockPistonExtendEvent(BlockPistonExtendEvent e) {
        for (Block block : e.getBlocks()) {
            if (!TradeCenter.isTradeCenter(block)) continue;
            e.setCancelled(true);
            break;
        }
    }

    @EventHandler
    public void onBlockPistonRetractEvent(BlockPistonRetractEvent e) {
        for (Block block : e.getBlocks()) {
            if (!TradeCenter.isTradeCenter(block)) continue;
            e.setCancelled(true);
            break;
        }
    }

    @EventHandler
    public void onEntityExplodeEvent(EntityExplodeEvent e) {
        for (Block block : e.blockList()) {
            if (!TradeCenter.isTradeCenter(block)) continue;
            new TradeCenter(block.getLocation()).clearData();
        }
    }
}