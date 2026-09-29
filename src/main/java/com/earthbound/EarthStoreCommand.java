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
                    "This command is for players only."
            );

            return true;

        }


        Player player =
                (Player) sender;



        /*
         * ========================================================
         * OPEN STORE MENU
         * ========================================================
         */

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
                            + "/store buy bread amount"
            );


            player.sendMessage(
                    ChatColor.WHITE
                            + "/store buy wheat_seeds amount"
            );


            player.sendMessage(
                    ChatColor.WHITE
                            + "/store buy oak_log amount"
            );



            player.sendMessage(
                    ChatColor.YELLOW
                            + "Sell:"
            );


            player.sendMessage(
                    ChatColor.WHITE
                            + "/store sell oak_log amount"
            );


            player.sendMessage(
                    ChatColor.WHITE
                            + "/store sell wheat amount"
            );


            player.sendMessage(
                    ChatColor.WHITE
                            + "/store sell stone amount"
            );


            return true;

        }



        if (args.length < 3) {


            player.sendMessage(
                    ChatColor.RED
                            + "Usage: /store buy/sell item amount"
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
                            + "Unknown item."
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
         * ========================================================
         * BUY
         * ========================================================
         */

        if (action.equals("buy")) {


            double price =
                    EarthStore.getBuyPrice(
                            material
                    );



            if (price < 0) {


                player.sendMessage(
                        ChatColor.RED
                                + "This item is not sold here."
                );


                return true;

            }



            double total =
                    price * amount;



            double balance =
                    EarthEconomy.getBalance(
                            player
                    );



            if (balance < total) {


                player.sendMessage(
                        ChatColor.RED
                                + "You do not have enough money."
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


            EarthEconomy.removeMoney(
                    player,
                    total
            );



            player.sendMessage(
                    ChatColor.GREEN
                            + "Purchased "
                            + amount
                            + " "
                            + material
                            + " for $"
                            + total
            );


            return true;

        }



        /*
         * ========================================================
         * SELL
         * ========================================================
         */

        if (action.equals("sell")) {


            double price =
                    EarthStore.getSellPrice(
                            material
                    );



            if (price < 0) {


                player.sendMessage(
                        ChatColor.RED
                                + "The store does not buy this item."
                );


                return true;

            }



            int amountOwned =
                    player.getInventory()
                            .containsAtLeast(
                                    new org.bukkit.inventory.ItemStack(
                                            material
                                    ),
                                    amount
                            )
                    ? amount
                    : 0;



            if (amountOwned == 0) {


                player.sendMessage(
                        ChatColor.RED
                                + "You do not have enough items."
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



            double payment =
                    price * amount;



            EarthEconomy.addMoney(
                    player,
                    payment
            );



            player.sendMessage(
                    ChatColor.GREEN
                            + "Sold "
                            + amount
                            + " "
                            + material
                            + " for $"
                            + payment
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
