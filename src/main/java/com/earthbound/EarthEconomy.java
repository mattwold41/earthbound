package com.earthbound;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class EarthEconomy {

    private static EarthBound plugin;
    private static File file;
    private static File transactionsFile;
    private static YamlConfiguration data;
    private static YamlConfiguration transactions;

    private static final Map<UUID, Double> balances =
            new HashMap<>();

    private static double treasuryBalance = 0.0;
    private static double storeBalance = 10000.0;
    private static long transactionId = 0;

    public static void setup(EarthBound earthBound) {

        plugin = earthBound;

        balances.clear();
        treasuryBalance = 0.0;
        storeBalance = 10000.0;
        transactionId = 0;

        File folder = plugin.getDataFolder();

        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning(
                    "Could not create EarthBound data folder."
            );
        }

        file = new File(folder, "economy.yml");
        transactionsFile =
                new File(folder, "transactions.yml");

        data = YamlConfiguration.loadConfiguration(file);
        transactions = YamlConfiguration.loadConfiguration(
                transactionsFile
        );

        // Legacy accounts are stored directly under UUID keys.
        // Preserve them when upgrading.
        for (String key : data.getKeys(false)) {

            try {
                UUID uuid = UUID.fromString(key);
                double amount = data.getDouble(key);

                if (validBalance(amount)) {
                    balances.put(uuid, amount);
                } else {
                    plugin.getLogger().warning(
                            "Invalid balance for " + key
                    );
                }

            } catch (IllegalArgumentException ignored) {
                // Non-UUID configuration keys are not player accounts.
            }
        }

        treasuryBalance = readBalance(
                "accounts.treasury", 0.0
        );

        storeBalance = readBalance(
                "accounts.general-store", 10000.0
        );

        transactionId = Math.max(
                0L,
                transactions.getLong("next-id", 0L)
        );

        save();

        plugin.getLogger().info(
                "Economy loaded: " + balances.size()
                + " player accounts."
        );
    }

    private static double readBalance(
            String path,
            double fallback
    ) {
        if (!data.contains(path)) {
            return fallback;
        }

        double amount = data.getDouble(path, fallback);

        return validBalance(amount) ? amount : fallback;
    }

    private static boolean validBalance(double amount) {
        return Double.isFinite(amount) && amount >= 0.0;
    }

    private static boolean validAmount(double amount) {
        return Double.isFinite(amount) && amount > 0.0;
    }

    private static double roundMoney(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    public static void save() {

        if (data == null || file == null) {
            return;
        }

        for (Map.Entry<UUID, Double> entry :
                balances.entrySet()) {

            data.set(
                    entry.getKey().toString(),
                    entry.getValue()
            );
        }

        data.set("accounts.treasury", treasuryBalance);
        data.set("accounts.general-store", storeBalance);

        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe(
                    "Could not save economy.yml: "
                    + e.getMessage()
            );
        }

        saveTransactions();
    }

    private static void saveTransactions() {

        if (transactions == null
                || transactionsFile == null) {
            return;
        }

        transactions.set("next-id", transactionId);

        try {
            transactions.save(transactionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe(
                    "Could not save transactions.yml: "
                    + e.getMessage()
            );
        }
    }

    private static void record(
            String from,
            String to,
            double amount,
            String reason
    ) {

        transactionId++;

        String path = "history." + transactionId;

        transactions.set(
                path + ".timestamp",
                System.currentTimeMillis()
        );

        transactions.set(path + ".from", from);
        transactions.set(path + ".to", to);
        transactions.set(path + ".amount", amount);
        transactions.set(path + ".reason", reason);

        save();
    }

    public static void createAccount(Player player) {

        UUID uuid = player.getUniqueId();

        if (balances.containsKey(uuid)) {
            return;
        }

        double startingMoney = plugin.getConfig()
                .getDouble(
                        "economy.starting-money",
                        1000.0
                );

        if (!validBalance(startingMoney)) {
            startingMoney = 1000.0;
        }

        startingMoney = roundMoney(startingMoney);

        balances.put(uuid, startingMoney);

        record(
                "SYSTEM",
                uuid.toString(),
                startingMoney,
                "New player starting balance"
        );
    }

    public static double getBalance(Player player) {

        createAccount(player);

        return balances.get(player.getUniqueId());
    }

    public static double getTreasuryBalance() {
        return treasuryBalance;
    }

    public static double getStoreBalance() {
        return storeBalance;
    }

    // Retained for compatibility with existing plugin code.
    // Only use this for legitimate money creation, such as
    // approved job rewards.
    public static void addMoney(
            Player player,
            double amount
    ) {

        if (!validAmount(amount)) {
            return;
        }

        amount = roundMoney(amount);

        if (amount <= 0.0) {
            return;
        }

        createAccount(player);

        UUID uuid = player.getUniqueId();
        double updated = balances.get(uuid) + amount;

        if (!validBalance(updated)) {
            return;
        }

        balances.put(uuid, roundMoney(updated));

        record(
                "SYSTEM",
                uuid.toString(),
                amount,
                "Money credited"
        );
    }

    // Retained for compatibility with existing plugin code.
    // This is a money sink, not a treasury payment.
    public static boolean removeMoney(
            Player player,
            double amount
    ) {

        if (!validAmount(amount)) {
            return false;
        }

        amount = roundMoney(amount);

        if (amount <= 0.0) {
            return false;
        }

        createAccount(player);

        UUID uuid = player.getUniqueId();
        double current = balances.get(uuid);

        if (current < amount) {
            return false;
        }

        balances.put(uuid, roundMoney(current - amount));

        record(
                uuid.toString(),
                "SYSTEM",
                amount,
                "Money removed"
        );

        return true;
    }

    public static boolean payStore(
            Player player,
            double amount,
            String reason
    ) {

        if (!validAmount(amount)) {
            return false;
        }

        amount = roundMoney(amount);

        if (amount <= 0.0) {
            return false;
        }

        createAccount(player);

        UUID uuid = player.getUniqueId();
        double current = balances.get(uuid);
        double updatedStore = storeBalance + amount;

        if (current < amount
                || !validBalance(updatedStore)) {
            return false;
        }

        balances.put(uuid, roundMoney(current - amount));
        storeBalance = roundMoney(updatedStore);

        record(
                uuid.toString(),
                "GENERAL_STORE",
                amount,
                reason
        );

        return true;
    }

    public static boolean storePaysPlayer(
            Player player,
            double amount,
            String reason
    ) {

        if (!validAmount(amount)) {
            return false;
        }

        amount = roundMoney(amount);

        if (amount <= 0.0) {
            return false;
        }

        createAccount(player);

        UUID uuid = player.getUniqueId();
        double updated = balances.get(uuid) + amount;

        if (storeBalance < amount
                || !validBalance(updated)) {
            return false;
        }

        storeBalance = roundMoney(storeBalance - amount);
        balances.put(uuid, roundMoney(updated));

        record(
                "GENERAL_STORE",
                uuid.toString(),
                amount,
                reason
        );

        return true;
    }

    public static boolean payTreasury(
            Player player,
            double amount,
            String reason
    ) {

        if (!validAmount(amount)) {
            return false;
        }

        amount = roundMoney(amount);

        if (amount <= 0.0) {
            return false;
        }

        createAccount(player);

        UUID uuid = player.getUniqueId();
        double current = balances.get(uuid);
        double updatedTreasury = treasuryBalance + amount;

        if (current < amount
                || !validBalance(updatedTreasury)) {
            return false;
        }

        balances.put(uuid, roundMoney(current - amount));
        treasuryBalance = roundMoney(updatedTreasury);

        record(
                uuid.toString(),
                "GOVERNMENT_TREASURY",
                amount,
                reason
        );

        return true;
    }

    public static boolean transfer(
            Player sender,
            Player recipient,
            double amount
    ) {

        if (sender.getUniqueId().equals(
                recipient.getUniqueId()
        )) {
            return false;
        }

        if (!validAmount(amount)) {
            return false;
        }

        amount = roundMoney(amount);

        if (amount <= 0.0) {
            return false;
        }

        createAccount(sender);
        createAccount(recipient);

        UUID from = sender.getUniqueId();
        UUID to = recipient.getUniqueId();

        double fromBalance = balances.get(from);
        double updatedRecipient =
                balances.get(to) + amount;

        if (fromBalance < amount
                || !validBalance(updatedRecipient)) {
            return false;
        }

        balances.put(
                from,
                roundMoney(fromBalance - amount)
        );

        balances.put(
                to,
                roundMoney(updatedRecipient)
        );

        record(
                from.toString(),
                to.toString(),
                amount,
                "Player transfer"
        );

        return true;
    }
}
