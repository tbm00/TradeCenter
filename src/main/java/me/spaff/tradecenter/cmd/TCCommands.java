package me.spaff.tradecenter.cmd;

import me.spaff.tradecenter.Constants;
import me.spaff.tradecenter.Main;
import me.spaff.tradecenter.config.Config;
import me.spaff.tradecenter.tradecenter.TradeCenter;
import me.spaff.tradecenter.utils.BukkitUtils;
import me.spaff.tradecenter.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class TCCommands implements CommandExecutor {
    private final String prefix = StringUtils.getHexColor("#FDDC5C") + "[TradeCenter] ";

    private void sendHelp(Player player) {
        BukkitUtils.sendMessage(player, "");
        BukkitUtils.sendMessage(player, StringUtils.getHexColor("#FDDC5C") + "                TradeCenter");
        BukkitUtils.sendMessage(player, StringUtils.getHexColor("#fff2c2") + "- /tc give <player> - gives player a trade");
        BukkitUtils.sendMessage(player, StringUtils.getHexColor("#fff2c2") + "center place item.");
        BukkitUtils.sendMessage(player, StringUtils.getHexColor("#fff2c2") + "- /tc reload - reloads the config.");
        BukkitUtils.sendMessage(player, StringUtils.getHexColor("#fff2c2") + "- /tc version - shows version of the plugin.");
        BukkitUtils.sendMessage(player, "");
    }

    private void sendPluginMessage(Player player, String message) {
        BukkitUtils.sendMessage(player, prefix + message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;

        Player player = (Player) sender;
        if (args.length == 0) {
            sendHelp(player);
            return false;
        }

        if (!player.hasPermission(Constants.COMMAND_PERMISSION)) {
            BukkitUtils.sendMessage(player, Config.readString("trade-center.message.execute-cmd-no-permission"));
            return false;
        }

        //---------------- Commands ----------------//

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) {
                Player otherPlayer = Bukkit.getPlayer(args[1]);
                if (otherPlayer == null) {
                    sendPluginMessage(player, "&cInvalid player!");
                    return false;
                }

                otherPlayer.getInventory().addItem(TradeCenter.getTradeCenterItem());
                sendPluginMessage(player, "&7Gave " + otherPlayer.getName() + " trade center item.");
            }
            else {
                player.getInventory().addItem(TradeCenter.getTradeCenterItem());
                sendPluginMessage(player, "&7Gave yourself trade center item.");
            }
        }
        else if (args[0].equalsIgnoreCase("reload")) {
            HashMap<String, Object> oldData = Config.getRawData();
            Config.reload();

            if (oldData == null || oldData.isEmpty() || Config.getRawData() == null || Config.getRawData().isEmpty()) {
                sendPluginMessage(player, "&cSomething went wrong when reloading the config!");
            }
            else
                sendPluginMessage(player, "&7Config reloaded successfully!");
        }
        else if (args[0].equalsIgnoreCase("version")) {
            sendPluginMessage(player, "&7Version: &fv" + Main.version);
        }
        else
            sendHelp(player);

        return true;
    }
}