package me.spaff.tradecenter.tradecenter;

import me.spaff.tradecenter.Constants;
import me.spaff.tradecenter.Main;
import me.spaff.tradecenter.chunkdata.ChunkData;
import me.spaff.tradecenter.config.Config;
import me.spaff.tradecenter.nms.Packets;
import me.spaff.tradecenter.utils.BukkitUtils;
import me.spaff.tradecenter.utils.InventoryUtils;
import me.spaff.tradecenter.utils.ItemUtils;
import me.spaff.tradecenter.utils.ReflectionUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.item.trading.MerchantOffer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_21_R3.inventory.CraftItemStack;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TradeCenter {
    private final Location location;
    private static Map<UUID, PlayerTradeCenterData> playerData = new HashMap<>();

    public TradeCenter(Location location) {
        this.location = location;
    }

    public void onPlace() {
        // Save persistent data
        saveData();
    }

    public void onBreak() {
        // Close trade menu for player using it
        beingUsedBy().ifPresent((player) -> player.closeInventory());

        // Clear persistent data
        clearData();
    }

    public void open(Player player) {
        if (isBeingUsed()) {
            BukkitUtils.sendMessage(player, Config.readString("trade-center.message.already-in-use"));
            return;
        }

        // Clear previous player trade data
        getPlayerData(player).ifPresent(data -> data.clearTradeData());

        // Store trade index and a villager, so we can validate them and
        // get which villager selected trade is from.
        List<VillagerTradeData> tradeData = new ArrayList<>();

        // List to store trade offers from all nearby villagers
        List<MerchantRecipe> offers = new ArrayList<>();

        // Iterate through all entities and only exclude villagers
        for (Entity entity : Packets.getServerLevel(location.getWorld()).getEntities().getAll()) {
            if (!entity.getType().equals(EntityType.VILLAGER)) continue; // Skip if not villager

            Villager villager = (Villager) entity;
            if (villager.isTrading()) continue; // Skip any villagers that are currently trading

            // Check if villager is within 5 blocks from trading center location
            if (location.distance(villager.getBukkitEntity().getLocation()) > 5) continue;

            // Get all trade offers from a villager
            for (MerchantOffer villagerOffer : villager.getOffers()) {
                MerchantOffer offerCopy = villagerOffer.copy();

                // Update special prices
                int i = villager.getPlayerReputation(Packets.getCraftPlayer(player).getHandle());
                if (i != 0)
                    offerCopy.addToSpecialPriceDiff(-Mth.floor((float) i * villagerOffer.getPriceMultiplier()));

                if (player.hasPotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE)) {
                    int j = player.getPotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE).getAmplifier();

                    double d0 = 0.3 + (double) 0.0625F * (double) j;
                    int k = (int) Math.floor(d0 * (double) villagerOffer.getBaseCostA().getCount());
                    offerCopy.addToSpecialPriceDiff(-Math.max(k, 1));
                }

                // Copy trade offer and it's attributes
                MerchantRecipe newOffer = new MerchantRecipe(
                        CraftItemStack.asBukkitCopy(offerCopy.result),
                        offerCopy.uses,
                        offerCopy.maxUses,
                        offerCopy.rewardExp,
                        offerCopy.xp,
                        offerCopy.priceMultiplier,
                        offerCopy.demand,
                        offerCopy.specialPriceDiff
                );

                // Add ingredients to the copied offer
                newOffer.addIngredient(CraftItemStack.asBukkitCopy(villagerOffer.baseCostA.itemStack()));
                if (villagerOffer.costB.isPresent()) // Check for secondary ingredient
                    newOffer.addIngredient(CraftItemStack.asBukkitCopy(villagerOffer.costB.get().itemStack()));

                offers.add(newOffer);
            }

            // Add this villager to a player data container
            tradeData.add(new VillagerTradeData(villager));
        }

        // Don't open the menu if there are no nearby villagers
        if (offers.isEmpty()) {
            BukkitUtils.sendMessage(player, Config.readString("trade-center.message.no-villagers-nearby"));
            return;
        }

        // Create new player data instance
        playerData.put(player.getUniqueId(), new PlayerTradeCenterData(location));

        // Set player trade data
        getPlayerData(player).ifPresent(playerData -> playerData.setTradeData(tradeData));

        // Set villagers' trading player to this player
        getPlayerData(player).ifPresent(playerData -> {
            playerData.getTradeData().forEach((td) -> {
                td.villager().setTradingPlayer(Packets.getCraftPlayer(player).getHandle());
            });
        });

        // Open trade menu with all the trades
        Merchant merchant = Bukkit.createMerchant();
        merchant.setRecipes(offers);
        player.openMerchant(merchant, true);
    }

    public static void handleClickEvent(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        if (e.getClickedInventory() == null) return;

        Inventory clickedInventory = e.getClickedInventory();
        if (!clickedInventory.getType().equals(InventoryType.MERCHANT)) return;

        int RESULT_SLOT = 2;
        boolean isShiftClick = e.getClick().equals(ClickType.SHIFT_LEFT) || e.getClick().equals(ClickType.SHIFT_RIGHT);

        if (e.getSlot() != RESULT_SLOT) return; // slot 2 = result

        ItemStack resultItem = clickedInventory.getItem(RESULT_SLOT);
        if (ItemUtils.isNull(resultItem)) return;
        if (isShiftClick && !InventoryUtils.canFitItem(player, resultItem)) return;

        Optional<PlayerTradeCenterData> optionalData = TradeCenter.getPlayerData(player);
        if (!optionalData.isPresent()) {
            System.out.println("optional data not present for player " + player.getName());
            return;
        }

        PlayerTradeCenterData tradeCenterData = optionalData.get();

        // Get selected trade from the menu
        int selectedTradeIndex = tradeCenterData.getSelectedTrade(); // getSelectedTrade()

        // Get player's TRADED_WITH_VILLAGER statistic so we can later tell
        // how many times player traded something
        int playerTradeCount = player.getStatistic(Statistic.TRADED_WITH_VILLAGER);

        // Get trade center location so we can later drop
        // experience orbs after a trade
        Location tradeCenterLoc = tradeCenterData.getTradeLocation();

        System.out.println("player data: " + tradeCenterData.getTradeData());

        boolean breakOut = false;
        int tradesAmount = 0;
        for (VillagerTradeData data :tradeCenterData.getTradeData()) {
            Villager villager = data.villager();

            System.out.println("player data");

            // Check if villager is valid
            Entity villagerEntity = Packets.getServerLevel(tradeCenterLoc.getWorld()).getEntity(villager.getId());
            if (villagerEntity == null) {
                e.setCancelled(true);
                player.closeInventory();

                BukkitUtils.sendMessage(player, Config.readString("trade-center.message.invalid-trade"));
                return;
            }

            // Since our trade menu has more trades then a usual trade menu
            // we have to adjust the selected index to match the correct trade with
            // the correct villager
            int correctIndex = selectedTradeIndex;

            // Get how many trades current villager has
            int villagerTradeSize = villager.getOffers().size();

            // Check if the trades amount is below selected index
            // if so subtract the trade amount from the selected index
            // to get the correct index that corresponds to the correct villager trade
            if (selectedTradeIndex < (tradesAmount + villagerTradeSize))
                correctIndex = selectedTradeIndex - tradesAmount;

            // Track current villager trade index
            int villagerTradeIndex = 0;
            for (MerchantOffer offer : villager.getOffers()) {
                // Check if the corrected index is the same as the current
                // villager trade index
                if (villagerTradeIndex == correctIndex) {
                    // Delay trade action so we can get the updated TRADED_WITH_VILLAGER statistic
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            // Subtract the previously stored trade count statistic with the current one
                            // and we get the amount of trades player has done
                            // this has to be done since there is no event for player trading
                            int tradedAmount = player.getStatistic(Statistic.TRADED_WITH_VILLAGER) - playerTradeCount;

                            System.out.println("player traded with villager ");

                            // Iterate the traded amount and update uses, villager xp etc
                            for (int j = 0; j < tradedAmount; j++) {
                                offer.increaseUses();

                                // Update villager trading info after trade
                                int i = 3 + villager.random.nextInt(4);
                                villager.setVillagerXp(villager.getVillagerXp() + offer.getXp());
                                ReflectionUtils.setField(villager, "cp", Packets.getCraftPlayer(player).getHandle()); // cp -> lastTradedPlayer

                                int villagerLevel = villager.getVillagerData().getLevel();
                                if (VillagerData.canLevelUp(villagerLevel) && villager.getVillagerXp() >= VillagerData.getMaxXpPerLevel(villagerLevel)) { // shouldIncreaseLevel()
                                    ReflectionUtils.setField(villager, "cn", 40); // cn -> updateMerchantTimer
                                    ReflectionUtils.setField(villager, "co", true); // co -> increaseProfessionLevelOnUpdate
                                    i += 5;
                                }

                                if (offer.shouldRewardExp()) {
                                    ExperienceOrb orb = (ExperienceOrb) tradeCenterLoc.getWorld().
                                            spawnEntity(tradeCenterLoc.add(0.5, 1, 0.5), org.bukkit.entity.EntityType.EXPERIENCE_ORB);
                                    orb.setExperience(i);
                                }
                            }
                        }
                    }.runTaskLater(Main.getInstance(), 1);

                    breakOut = true;
                    break;
                }

                villagerTradeIndex++;
            }

            if (breakOut)
                break;

            // Add the villager trade size to the trades amount
            tradesAmount += villagerTradeSize;
        }
    }

    public void clearData() {
        new ChunkData(location.getBlock()).clearData();
    }

    public void saveData() {
        if (isTradeCenter(location.getBlock())) return;
        new ChunkData(location.getBlock()).saveData(Constants.TRADE_CENTER_DATA_ID);
    }

    public static boolean isTradeCenter(Block block) {
        return Constants.TRADE_CENTER_DATA_ID.equals(new ChunkData(block).getData());
    }

    // Player Data
    public static Optional<PlayerTradeCenterData> getPlayerData(Player player) {
        return Optional.ofNullable(playerData.get(player.getUniqueId()));
    }

    public static void clearPlayerData(Player player) {
        Optional<PlayerTradeCenterData> playerTradeCenterData = getPlayerData(player);

        playerTradeCenterData.ifPresent((playerData) -> {
            playerData.getTradeData().forEach((data) -> {
                data.villager().setTradingPlayer(null);
            });
        });

        playerData.remove(player.getUniqueId());
    }

    public static Location getPlayerTradeLocation(Player player) {
        return playerData.get(player.getUniqueId()).getTradeLocation();
    }

    public boolean isBeingUsed() {
        return beingUsedBy().isPresent();
    }

    public Optional<Player> beingUsedBy() {
        for (var data : playerData.entrySet()) {
            Player player = Bukkit.getPlayer(data.getKey());
            if (player == null) continue;

            if (BukkitUtils.isSameLocation(location, data.getValue().getTradeLocation()))
                return Optional.of(player);
        }
        return Optional.empty();
    }

    public static boolean isTradeCenterItem(ItemStack item) {
        String itemId = (String) ItemUtils.getPersistentData(
                item, Main.getInstance().getName().toLowerCase(),
                Constants.TRADE_CENTER_NAMESPACE_KEY,
                PersistentDataType.STRING
        );
        return itemId != null && itemId.equals(Constants.TRADE_CENTER_ITEM_ID);
    }

    public static ItemStack getTradeCenterItem() {
        ItemStack item = ItemUtils.getHead(Constants.TRADE_CENTER_ICON);
        ItemUtils.setName(item, Config.readString("trade-center.item.name"));
        ItemUtils.setLore(item, Config.readString("trade-center.item.lore"));
        ItemUtils.setPersistentData(item, Main.getInstance().getName().toLowerCase(),
                Constants.TRADE_CENTER_NAMESPACE_KEY, PersistentDataType.STRING, Constants.TRADE_CENTER_ITEM_ID);
        return item;
    }
}