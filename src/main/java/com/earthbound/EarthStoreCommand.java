package com.earthbound;

import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class EarthStoreCommand implements CommandExecutor {

    private static final int MAX_ITEMS = 2304;

    /*
     * ============================================================
     * STORE COMMAND
     * ============================================================
     */

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

        if (!Bukkit.isPrimaryThread()) {

            player.sendMessage(
                    ChatColor.RED
                    + "Store transaction unavailable."
            );

            return true;
        }

        if (args.length == 0) {

            showStore(player);

            return true;
        }

        if (!EarthEconomy.isReady()) {

            player.sendMessage(
                    ChatColor.RED
                    + "The General Store is temporarily unavailable."
            );

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

        if (material == null
                || !material.isItem()
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

        } catch (NumberFormatException exception) {

            player.sendMessage(
                    ChatColor.RED
                    + "Invalid amount."
            );

            return true;
        }

        if (amount <= 0 || amount > MAX_ITEMS) {

            player.sendMessage(
                    ChatColor.RED
                    + "Amount must be between 1 and "
                    + MAX_ITEMS + "."
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


    /*
     * ============================================================
     * DISPLAY GENERAL STORE
     * ============================================================
     */

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

        if (!EarthEconomy.isReady()) {

            player.sendMessage(
                    ChatColor.RED
                    + "Transactions temporarily unavailable."
            );
        }
    }


    /*
     * ============================================================
     * BUY ITEMS FROM GENERAL STORE
     * ============================================================
     */

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

        double total =
                transactionTotal(unitPrice, amount);

        if (!validPrice(total)) {

            player.sendMessage(
                    ChatColor.RED
                    + "Invalid transaction total."
            );

            return;
        }

        double balance =
                EarthEconomy.getBalance(player);

        if (!EarthEconomy.isReady()) {

            player.sendMessage(
                    ChatColor.RED
                    + "Financial storage is unavailable."
            );

            return;
        }

        if (balance < total) {

            player.sendMessage(
                    ChatColor.RED
                    + "Not enough money."
            );

            return;
        }

        PlayerInventory inventory =
                player.getInventory();

        /*
         * Create independent inventory snapshots.
         */

        ItemStack[] original =
                copyContents(
                        inventory.getStorageContents()
                );

        ItemStack[] proposed =
                copyContents(original);

        /*
         * Prepare the entire purchase without changing
         * the player's actual inventory.
         */

        if (!insertItems(
                proposed,
                material,
                amount,
                inventory.getMaxStackSize()
        )) {

            player.sendMessage(
                    ChatColor.RED
                    + "Not enough inventory space."
            );

            return;
        }

        /*
         * Apply inventory changes before committing
         * the financial transaction.
         */

        if (!applyInventory(
                player,
                inventory,
                original,
                proposed
        )) {

            return;
        }

        boolean paid =
                EarthEconomy.payStore(
                        player,
                        total,
                        "Store purchase: "
                        + amount
                        + " "
                        + material.name()
                );

        if (!paid) {

            rollbackInventory(
                    player,
                    inventory,
                    original
            );

            return;
        }

        player.sendMessage(
                ChatColor.GREEN
                + "Bought "
                + amount
                + " "
                + material.name()
                + " for $"
                + money(total)
        );
    }


    /*
     * ============================================================
     * SELL ITEMS TO GENERAL STORE
     * ============================================================
     */

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

        double total =
                transactionTotal(unitPrice, amount);

        if (!validPrice(total)) {

            player.sendMessage(
                    ChatColor.RED
                    + "Invalid transaction total."
            );

            return;
        }

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

        ItemStack[] original =
                copyContents(
                        inventory.getStorageContents()
                );

        ItemStack[] proposed =
                copyContents(original);

        int owned =
                countItems(original, material);

        if (owned < amount) {

            player.sendMessage(
                    ChatColor.RED
                    + "You only have "
                    + owned
                    + " "
                    + material.name()
            );

            return;
        }

        /*
         * Prepare item removal without modifying
         * the original inventory.
         */

        if (!removeItems(
                proposed,
                material,
                amount
        )) {

            player.sendMessage(
                    ChatColor.RED
                    + "Could not prepare sale."
            );

            return;
        }

        if (!applyInventory(
                player,
                inventory,
                original,
                proposed
        )) {

            return;
        }

        /*
         * Commit payment from the General Store.
         */

        boolean paid =
                EarthEconomy.storePaysPlayer(
                        player,
                        total,
                        "Store purchase from player: "
                        + amount
                        + " "
                        + material.name()
                );

        if (!paid) {

            rollbackInventory(
                    player,
                    inventory,
                    original
            );

            return;
        }

        player.sendMessage(
                ChatColor.GREEN
                + "Sold "
                + amount
                + " "
                + material.name()
                + " for $"
                + money(total)
        );
    }


    /*
     * ============================================================
     * DEEP INVENTORY COPY
     * ============================================================
     */

    private ItemStack[] copyContents(
            ItemStack[] contents
    ) {

        ItemStack[] copy =
                new ItemStack[contents.length];

        for (int i = 0; i < contents.length; i++) {

            if (contents[i] != null) {

                copy[i] = contents[i].clone();
            }
        }

        return copy;
    }


    /*
     * ============================================================
     * PREPARE ITEM INSERTION
     * ============================================================
     */

    private boolean insertItems(
            ItemStack[] contents,
            Material material,
            int amount,
            int inventoryLimit
    ) {

        int remaining = amount;

        ItemStack plain =
                new ItemStack(material);

        int stackLimit = Math.min(
                material.getMaxStackSize(),
                inventoryLimit
        );

        if (stackLimit <= 0) {

            return false;
        }

        /*
         * Fill existing compatible item stacks first.
         */

        for (int i = 0;
                i < contents.length && remaining > 0;
                i++) {

            ItemStack item = contents[i];

            if (item == null
                    || item.getType().isAir()
                    || !item.isSimilar(plain)) {

                continue;
            }

            int limit = Math.min(
                    stackLimit,
                    item.getMaxStackSize()
            );

            int available =
                    limit - item.getAmount();

            if (available <= 0) {

                continue;
            }

            int added = Math.min(
                    available,
                    remaining
            );

            item.setAmount(
                    item.getAmount() + added
            );

            remaining -= added;
        }

        /*
         * Fill empty storage slots.
         */

        for (int i = 0;
                i < contents.length && remaining > 0;
                i++) {

            ItemStack item = contents[i];

            if (item != null
                    && !item.getType().isAir()) {

                continue;
            }

            int added = Math.min(
                    remaining,
                    stackLimit
            );

            contents[i] =
                    new ItemStack(material, added);

            remaining -= added;
        }

        return remaining == 0;
    }


    /*
     * ============================================================
     * COUNT SELLABLE ITEMS
     * ============================================================
     */

    private int countItems(
            ItemStack[] contents,
            Material material
    ) {

        int count = 0;

        ItemStack plain =
                new ItemStack(material);

        for (ItemStack item : contents) {

            if (item != null
                    && item.isSimilar(plain)
                    && item.getAmount() > 0) {

                count += item.getAmount();
            }
        }

        return count;
    }


    /*
     * ============================================================
     * PREPARE ITEM REMOVAL
     * ============================================================
     */

    private boolean removeItems(
            ItemStack[] contents,
            Material material,
            int amount
    ) {

        int remaining = amount;

        ItemStack plain =
                new ItemStack(material);

        for (int i = 0;
                i < contents.length && remaining > 0;
                i++) {

            ItemStack item = contents[i];

            if (item == null
                    || !item.isSimilar(plain)
                    || item.getAmount() <= 0) {

                continue;
            }

            int taken = Math.min(
                    remaining,
                    item.getAmount()
            );

            int left =
                    item.getAmount() - taken;

            if (left == 0) {

                contents[i] = null;

            } else {

                item.setAmount(left);
            }

            remaining -= taken;
        }

        return remaining == 0;
    }


    /*
     * ============================================================
     * APPLY INVENTORY CHANGES
     * ============================================================
     */

    private boolean applyInventory(
            Player player,
            PlayerInventory inventory,
            ItemStack[] original,
            ItemStack[] proposed
    ) {

        try {

            inventory.setStorageContents(proposed);

            /*
             * Verify that the inventory matches
             * the prepared transaction.
             */

            if (!sameContents(
                    proposed,
                    inventory.getStorageContents()
            )) {

                throw new IllegalStateException(
                        "Inventory verification failed."
                );
            }

            return true;

        } catch (RuntimeException exception) {

            Bukkit.getLogger().severe(
                    "[EarthBound] Inventory update failed for "
                    + player.getUniqueId()
                    + ": "
                    + exception.getMessage()
            );

            rollbackInventory(
                    player,
                    inventory,
                    original
            );

            return false;
        }
    }


    /*
     * ============================================================
     * RESTORE INVENTORY AFTER FAILED TRANSACTION
     * ============================================================
     */

    private void rollbackInventory(
            Player player,
            PlayerInventory inventory,
            ItemStack[] original
    ) {

        try {

            inventory.setStorageContents(
                    copyContents(original)
            );

            if (!sameContents(
                    original,
                    inventory.getStorageContents()
            )) {

                throw new IllegalStateException(
                        "Inventory restoration verification failed."
                );
            }

            player.sendMessage(
                    ChatColor.RED
                    + "Transaction failed. Inventory restored."
            );

            if (!EarthEconomy.isReady()) {

                player.sendMessage(
                        ChatColor.RED
                        + "Financial storage requires "
                        + "administrator review."
                );
            }

        } catch (RuntimeException exception) {

            Bukkit.getLogger().severe(
                    "[EarthBound] CRITICAL: Inventory rollback "
                    + "failed for "
                    + player.getName()
                    + " ("
                    + player.getUniqueId()
                    + "): "
                    + exception.getMessage()
            );

            player.sendMessage(
                    ChatColor.RED
                    + "Inventory recovery failed. "
                    + "Contact an administrator immediately."
            );
        }
    }


    /*
     * ============================================================
     * VERIFY INVENTORY CONTENTS
     * ============================================================
     */

    private boolean sameContents(
            ItemStack[] first,
            ItemStack[] second
    ) {

        if (first.length != second.length) {

            return false;
        }

        for (int i = 0; i < first.length; i++) {

            ItemStack a = first[i];

            ItemStack b = second[i];

            boolean emptyA =
                    a == null || a.getType().isAir();

            boolean emptyB =
                    b == null || b.getType().isAir();

            if (emptyA && emptyB) {

                continue;
            }

            if (emptyA || emptyB) {

                return false;
            }

            if (!a.equals(b)) {

                return false;
            }
        }

        return true;
    }


    /*
     * ============================================================
     * MONEY VALIDATION
     * ============================================================
     */

    private boolean validPrice(double amount) {

        return Double.isFinite(amount)
                && amount >= 0.01
                && amount <= 1000000000.0;
    }


    private double transactionTotal(
            double unitPrice,
            int amount
    ) {

        double total = unitPrice * amount;

        if (!validPrice(total)) {

            return -1.0;
        }

        return Math.round(total * 100.0) / 100.0;
    }


    private String money(double amount) {

        return String.format(
                Locale.US,
                "%,.2f",
                amount
        );
    }

}
