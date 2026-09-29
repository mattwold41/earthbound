package com.earthbound;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EarthEconomyCommand implements CommandExecutor {


    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {


        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    "This command is for players only."
            );

            return true;

        }


        Player player =
                (Player) sender;


        double balance =
                EarthEconomy.getBalance(
                        player
                );


        player.sendMessage(
                ChatColor.GOLD
                        + "EarthBound Balance: "
                        + ChatColor.GREEN
                        + "$"
                        + String.format(
                                "%.2f",
                                balance
                        )
        );


        return true;

    }

}
