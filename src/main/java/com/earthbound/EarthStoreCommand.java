package com.earthbound;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EarthStoreCommand implements CommandExecutor {


    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {


        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    "Players only."
            );

            return true;
        }


        Player player =
                (Player) sender;



        if (args.length == 0) {

            player.sendMessage(
                    ChatColor.GOLD
                    + "===== EarthBound Store ====="
            );

            player.sendMessage(
                    ChatColor.YELLOW
                    + "Buy:"
            );

            player.sendMessage(
                    ChatColor.WHITE
                    + "/store buy bread 10"
            );

            player.sendMessage(
                    ChatColor.WHITE
                    + "/store buy oak_planks 10"
            );


            player.sendMessage(
                    ChatColor.YELLOW
                    + "Sell:"
            );

            player.sendMessage(
                    ChatColor.WHITE
                    + "/store sell oak_log 10"
            );

            player.sendMessage(
                    ChatColor.WHITE
                    + "/store sell stone 10"
            );


            return true;
        }



        if (args.length < 3) {

            player.sendMessage(
                    ChatColor.RED
                    + "Use: /store buy/sell item amount"
            );

            return true;
        }



        String action =
                args[0].toLowerCase();



        Material material;


        try {

            material =
                    Material.valueOf(
                            args[1].toUpperCase()
                    );


        } catch (Exception e) {

            player.sendMessage(
                    ChatColor.RED
                    + "Unknown item: "
                    + args[1]
            );

            return true;
        }



        int amount;


        try {

            amount =
                    Integer.parseInt(
                            args[2]
                    );


        } catch (Exception e) {

            player.sendMessage(
                    ChatColor.RED
                    + "Invalid amount."
            );

            return true;
        }



        /*
         * ==========================
         * SELL
         * ==========================
         */

        if (action.equals("sell")) {


            double price =
                    EarthStore.getSellPrice(
                            material
                    );


            if (price < 0) {

                player.sendMessage(
                        ChatColor.RED
                        + "Store does not buy "
                        + material
                );

                return true;
            }



            int count = 0;


            for (org.bukkit.inventory.ItemStack item :
                    player.getInventory().getContents()) {


                if (item != null
                        && item.getType() == material) {


                    count += item.getAmount();

                }

            }



            if (count < amount) {

                player.sendMessage(
                        ChatColor.RED
                        + "You only have "
                        + count
                        + " "
                        + material
                );

                return true;
            }



            player.getInventory()
                    .removeItem(
                            new org.bukkit.inventory.ItemStack(
                                    material,
                                    amount
                            )
                    );



            double money =
                    price * amount;



            EarthEconomy.addMoney(
                    player,
                    money
            );



            player.sendMessage(
                    ChatColor.GREEN
                    + "Sold "
                    + amount
                    + " "
                    + material
                    + " for $"
                    + money
            );


            return true;

        }



        /*
         * ==========================
         * BUY
         * ==========================
         */

        if (action.equals("buy")) {


            double price =
                    EarthStore.getBuyPrice(
                            material
                    );


            if (price < 0) {

                player.sendMessage(
                        ChatColor.RED
                        + "Store does not sell "
                        + material
                );

                return true;

            }


            double total =
                    price * amount;



            if (!EarthEconomy.removeMoney(
                    player,
                    total
            )) {


                player.sendMessage(
                        ChatColor.RED
                        + "Not enough money."
                );


                return true;

            }



            player.getInventory()
                    .addItem(
                            new org.bukkit.inventory.ItemStack(
                                    material,
                                    amount
                            )
                    );


            player.sendMessage(
                    ChatColor.GREEN
                    + "Bought "
                    + amount
                    + " "
                    + material
                    + " for $"
                    + total
            );


            return true;

        }



        player.sendMessage(
                ChatColor.RED
                + "Use buy or sell."
        );


        return true;

    }

}
