package me.spaff.tradecenter.tradecenter;

import net.minecraft.world.entity.npc.Villager;

public class VillagerTradeData {
    private final Villager villager;

    public VillagerTradeData(Villager villager) {
        this.villager = villager;
    }

    public Villager getVillager() {
        return villager;
    }
}
