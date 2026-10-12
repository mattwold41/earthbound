package com.earthbound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class EarthStoreCommand implements CommandExecutor {

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showStore(player);
            return true;
        }

        if (args.length != 3) {
            player.sendMessage(
                    ChatColor.RED
                    + "Use: /store buy/sell item amount"
            );
            return true;
        }

        String action =
                args[0].toLowerCase(Locale.ROOT);

        if (!action.equals("buy")
                && !action.equals("sell")) {
            player.sendMessage(
                    ChatColor.RED
                    + "Use buy or sell."
            );
            return true;
        }

        Material material =
                Material.matchMaterial(args[1]);

        if (material == null || !material.isItem()
                || material.isAir()) {
            player.sendMessage(
                    ChatColor.RED
                    + "Unknown item: " + args[1]
            );
            return true;
        }

        int amount;

        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(
                    ChatColor.RED + "Invalid amount."
            );
            return true;
        }

        if (amount <= 0 || amount > 2304) {
            player.sendMessage(
                    ChatColor.RED
                    + "Amount must be between 1 and 2304."
            );
            return true;
        }

        if (action.equals("buy")) {
            buy(player, material, amount);
        } else {
            sell(player, material, amount);
        }

        return true;
    }

    private void showStore(Player player) {

        player.sendMessage(
                ChatColor.GOLD
                + "===== EarthBound General Store ====="
        );

        player.sendMessage(
                ChatColor.YELLOW + "Buy:"
        );
        player.sendMessage(
                ChatColor.WHITE + "Bread: $10 each"
        );
        player.sendMessage(
                ChatColor.WHITE + "Wheat seeds: $2 each"
        );
        player.sendMessage(
                ChatColor.WHITE + "Oak logs: $8 each"
        );

        player.sendMessage(
                ChatColor.YELLOW + "Sell:"
        );
        player.sendMessage(
                ChatColor.WHITE + "Oak logs: $4 each"
        );
        player.sendMessage(
                ChatColor.WHITE + "Wheat: $3 each"
        );
        player.sendMessage(
                ChatColor.WHITE + "Stone: $1 each"
        );

        player.sendMessage(
                ChatColor.GRAY
                + "Example: /store buy bread 10"
        );
    }

    private void buy(
            Player player,
            Material material,
            int amount
    ) {

        double unitPrice =
                EarthStore.getBuyPrice(material);

        if (!validPrice(unitPrice)) {
            player.sendMessage(
                    ChatColor.RED
                    + "The store does not sell that item."
            );
            return;
        }

        double total = unitPrice * amount;

        if (!validPrice(total)) {
            player.sendMessage(
                    ChatColor.RED
                    + "Invalid transaction total."
            );
            return;
        }

        total = Math.round(total * 100.0) / 100.0;

        if (EarthEconomy.getBalance(player) < total) {
            player.sendMessage(
                    ChatColor.RED
                    + "Not enough money."
            );
            return;
        }

        PlayerInventory inventory =
                player.getInventory();

        if (!canFit(inventory, material, amount)) {
            player.sendMessage(
                    ChatColor.RED
                    + "Not enough inventory space."
            );
            return;
        }

        if (!EarthEconomy.payStore(
                player,
                total,
                "Store purchase: "
                + amount + " " + material.name()
        )) {
            player.sendMessage(
                    ChatColor.RED
                    + "Payment could not be completed."
            );
            return;
        }

        // Minecraft item stacks cannot exceed their
        // maximum stack size.
        int remaining = amount;

        while (remaining > 0) {

            int stackSize = Math.min(
                    remaining,
                    material.getMaxStackSize()
            );

            inventory.addItem(
                    new ItemStack(material, stackSize)
            );

            remaining -= stackSize;
        }

        player.sendMessage(
                ChatColor.GREEN
                + "Bought " + amount + " "
                + material.name()
                + " for $" + money(total)
        );
    }

    private void sell(
            Player player,
            Material material,
            int amount
    ) {

        double unitPrice =
                EarthStore.getSellPrice(material);

        if (!validPrice(unitPrice)) {
            player.sendMessage(
                    ChatColor.RED
                    + "The store does not buy that item."
            );
            return;
        }

        double total = unitPrice * amount;

        if (!validPrice(total)) {
            player.sendMessage(
                    ChatColor.RED
                    + "Invalid transaction total."
            );
            return;
        }

        total = Math.round(total * 100.0) / 100.0;

        if (EarthEconomy.getStoreBalance() < total) {
            player.sendMessage(
                    ChatColor.RED
                    + "The General Store cannot afford "
                    + "this purchase right now."
            );
            return;
        }

        PlayerInventory inventory =
                player.getInventory();

        int owned = countItems(inventory, material);

        if (owned < amount) {
            player.sendMessage(
                    ChatColor.RED
                    + "You only have " + owned
                    + " " + material.name()
            );
            return;
        }

        // Save a snapshot so inventory can be restored
        // if the financial transaction fails.
        ItemStack[] snapshot =
                inventory.getStorageContents().clone();

        removeItems(inventory, material, amount);

        if (!EarthEconomy.storePaysPlayer(
                player,
                total,
                "Store purchase from player: "
                + amount + " " + material.name()
        )) {

            inventory.setStorageContents(snapshot);

            player.sendMessage(
                    ChatColor.RED
                    + "Payment failed. Items restored."
            );
            return;
        }

        player.sendMessage(
                ChatColor.GREEN
                + "Sold " + amount + " "
                + material.name()
                + " for $" + money(total)
        );
    }

    private boolean validPrice(double amount) {
        return Double.isFinite(amount) && amount > 0.0;
    }

    private String money(double amount) {
        return String.format(
                Locale.US, "%.2f", amount
        );
    }

    private int countItems(
            PlayerInventory inventory,
            Material material
    ) {

        int count = 0;

        for (ItemStack item :
                inventory.getStorageContents()) {

            if (item != null
                    && item.getType() == material) {
                count += item.getAmount();
            }
        }

        return count;
    }

    private void removeItems(
            PlayerInventory inventory,
            Material material,
            int amount
    ) {

        ItemStack[] contents =
                inventory.getStorageContents();

        int remaining = amount;

        for (int i = 0;
                i < contents.length && remaining > 0;
                i++) {

            ItemStack item = contents[i];

            if (item == null
                    || item.getType() != material) {
                continue;
            }

            int taken = Math.min(
                    remaining, item.getAmount()
            );

            int left = item.getAmount() - taken;

            if (left == 0) {
                contents[i] = null;
            } else {
                item.setAmount(left);
            }

            remaining -= taken;
        }

        inventory.setStorageContents(contents);
    }

    private boolean canFit(
            PlayerInventory inventory,
            Material material,
            int amount
    ) {

        int available = 0;
        int maxStack = material.getMaxStackSize();

        for (ItemStack item :
                inventory.getStorageContents()) {

            if (item == null || item.getType().isAir()) {
                available += maxStack;
            } else if (item.getType() == material
                    && item.getAmount() < maxStack) {

                // Only count stacks that can merge with
                // an ordinary store-issued item.
                ItemStack plain = new ItemStack(material);

                if (item.isSimilar(plain)) {
                    available +=
                            maxStack - item.getAmount();
                }
            }

            if (available >= amount) {
                return true;
            }
        }

        return false;
    }
}
