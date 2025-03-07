package me.spaff.tradecenter.tradecenter;

import org.apache.commons.lang3.Validate;
import org.bukkit.Location;

import java.util.*;

public class PlayerTradeCenterData {
    private final Location tradeCenterLocation;
    private List<VillagerTradeData> tradeData;
    private int selectedTrade = 0;

    public PlayerTradeCenterData(Location tradeCenter) {
        Validate.isTrue(TradeCenter.isTradeCenter(tradeCenter.getBlock()), "This location has no trade center!");
        this.tradeCenterLocation = tradeCenter;
        this.tradeData = new ArrayList<>();
    }

    // Trade Data
    public void setTradeData(List<VillagerTradeData> trades) {
        tradeData.addAll(trades);
    }

    public void clearTradeData() {
        tradeData.clear();
    }

    public List<VillagerTradeData> getTradeData() {
        return tradeData;
    }

    public void setSelectedTrade(int tradeIndex) {
        selectedTrade = tradeIndex;
    }

    public int getSelectedTrade() {
        return selectedTrade;
    }

    // Getters
    public Location getTradeLocation() {
        return tradeCenterLocation;
    }
}
