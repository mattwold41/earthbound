package com.earthbound;

import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EarthPayCommand implements CommandExecutor {

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        /*
         * PLAYERS ONLY
         */

        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    "Only players can send money."
            );

            return true;
        }

        Player player = (Player) sender;

        /*
         * COMMAND FORMAT
         */

        if (args.length != 2) {

            player.sendMessage(
                    ChatColor.YELLOW
                    + "Usage: /pay <player> <amount>"
            );

            return true;
        }

        /*
         * FIND RECIPIENT
         */

        Player recipient =
                Bukkit.getPlayerExact(args[0]);

        if (recipient == null) {

            player.sendMessage(
                    ChatColor.RED
                    + "That player is not online."
            );

            return true;
        }

        if (player.getUniqueId().equals(
                recipient.getUniqueId()
        )) {

            player.sendMessage(
                    ChatColor.RED
                    + "You cannot pay yourself."
            );

            return true;
        }

        /*
         * VALIDATE AMOUNT
         */

        double amount;

        try {

            amount = Double.parseDouble(args[1]);

        } catch (NumberFormatException e) {

            player.sendMessage(
                    ChatColor.RED
                    + "Please enter a valid amount."
            );

            return true;
        }

        if (!Double.isFinite(amount)
                || amount <= 0.0
                || amount > 1000000000.0) {

            player.sendMessage(
                    ChatColor.RED
                    + "Enter an amount between "
                    + "$0.01 and $1,000,000,000."
            );

            return true;
        }

        amount = Math.round(amount * 100.0) / 100.0;

        if (amount < 0.01) {

            player.sendMessage(
                    ChatColor.RED
                    + "Minimum payment is $0.01."
            );

            return true;
        }

        /*
         * CHECK BALANCE
         */

        if (EarthEconomy.getBalance(player) < amount) {

            player.sendMessage(
                    ChatColor.RED
                    + "You do not have enough money."
            );

            return true;
        }

        /*
         * TRANSFER MONEY
         */

        boolean success =
                EarthEconomy.transfer(
                        player,
                        recipient,
                        amount
                );

        if (!success) {

            player.sendMessage(
                    ChatColor.RED
                    + "Payment failed."
            );

            return true;
        }

        /*
         * CONFIRM PAYMENT
         */

        String formattedAmount =
                String.format(
                        Locale.US,
                        "$%,.2f",
                        amount
                );

        player.sendMessage(
                ChatColor.GREEN
                + "You paid "
                + recipient.getName()
                + " "
                + formattedAmount
                + "."
        );

        recipient.sendMessage(
                ChatColor.GREEN
                + "You received "
                + formattedAmount
                + " from "
                + player.getName()
                + "."
        );

        return true;
    }
}
