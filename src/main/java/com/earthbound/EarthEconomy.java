
package com.earthbound;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.earthbound.EarthAlphaResidentialGenerator.ResidentialSite;
import com.earthbound.EarthResidentialInstallation.InstallationStatus;
import com.earthbound.EarthPropertyManager.PurchaseEligibility;

public class EarthEconomy {

    private static EarthBound plugin;

    private static File economyFile;

    private static YamlConfiguration data;

    private static boolean ready = false;

    /*
     * All internal amounts are stored as integer cents.
     *
     * Maximum individual transaction: $1 billion.
     * Maximum account balance: $1 trillion.
     */

    private static final long MAX_TRANSACTION =
            100_000_000_000L;

    private static final long MAX_BALANCE =
            100_000_000_000_000L;

    private static final String STORE =
            "accounts.general-store";

    private static final String RESIDENTIAL_WORLD =
            "earthbound";

    public enum Treasury {
        ANACORTES,
        WASHINGTON,
        USA
    }


    /*
     * ============================================================
     * INITIALIZE ECONOMY
     * ============================================================
     */

    public static synchronized void setup(
            EarthBound earthBound
    ) {

        plugin = earthBound;
        ready = false;
        data = null;

        File folder = plugin.getDataFolder();

        economyFile = new File(
                folder,
                "economy-v2.yml"
        );

        try {

            Files.createDirectories(folder.toPath());

            if (economyFile.exists()) {

                YamlConfiguration loaded =
                        loadFile(economyFile);

                validateData(loaded);

                data = loaded;

            } else {

                YamlConfiguration migrated =
                        migrateLegacyData(folder);

                validateData(migrated);

                writeAtomically(migrated);

                data = migrated;
            }

            ready = true;

            plugin.getLogger().info(
                    "EarthBound secure economy loaded!"
            );

            plugin.getLogger().info(
                    "Independent residential purchase "
                    + "transaction protection enabled."
            );

        } catch (Exception exception) {

            ready = false;

            plugin.getLogger().severe(
                    "Economy initialization failed: "
                    + exception.getMessage()
            );

            plugin.getLogger().severe(
                    "Financial transactions are disabled "
                    + "until the economy is repaired."
            );
        }
    }


    /*
     * LOAD YAML WITHOUT SILENTLY IGNORING ERRORS
     */

    private static YamlConfiguration loadFile(
            File file
    ) throws Exception {

        YamlConfiguration result =
                new YamlConfiguration();

        if (file.exists()) {
            result.load(file);
        }

        return result;
    }


    /*
     * ============================================================
     * MONEY CONVERSION
     * ============================================================
     */

    private static long toCents(double amount) {

        if (!Double.isFinite(amount)
                || amount < 0.0) {

            throw new IllegalArgumentException(
                    "Invalid money amount."
            );
        }

        long cents = BigDecimal.valueOf(amount)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        if (cents < 0 || cents > MAX_BALANCE) {

            throw new IllegalArgumentException(
                    "Money amount exceeds account limits."
            );
        }

        return cents;
    }


    private static long transactionCents(
            double amount
    ) {

        try {

            long cents = toCents(amount);

            if (cents <= 0
                    || cents > MAX_TRANSACTION) {

                return -1;
            }

            return cents;

        } catch (RuntimeException exception) {

            return -1;
        }
    }


    /*
     * Property purchases must use exact monetary amounts.
     *
     * Do not accept fractional cents that would otherwise
     * be rounded to the registered property price.
     */

    private static long exactPropertyPriceCents(
            double amount
    ) {

        if (!Double.isFinite(amount)
                || amount <= 0.0) {

            return -1;
        }

        try {

            long cents =
                    BigDecimal.valueOf(amount)
                            .movePointRight(2)
                            .longValueExact();

            if (cents <= 0
                    || cents > MAX_TRANSACTION) {

                return -1;
            }

            return cents;

        } catch (RuntimeException exception) {

            return -1;
        }
    }


    private static double toDollars(long cents) {

        return cents / 100.0;
    }


    private static String playerPath(UUID uuid) {

        return "players." + uuid;
    }


    private static String treasuryPath(
            Treasury treasury
    ) {

        switch (treasury) {

            case ANACORTES:
                return "accounts.anacortes";

            case WASHINGTON:
                return "accounts.washington";

            case USA:
                return "accounts.usa";

            default:
                throw new IllegalArgumentException(
                        "Unknown treasury."
                );
        }
    }


    /*
     * ============================================================
     * MIGRATE EXISTING ECONOMY
     * ============================================================
     */

    private static YamlConfiguration migrateLegacyData(
            File folder
    ) throws Exception {

        YamlConfiguration result =
                new YamlConfiguration();

        File legacyEconomyFile =
                new File(folder, "economy.yml");

        File legacyTransactionsFile =
                new File(folder, "transactions.yml");

        File legacyPropertiesFile =
                new File(folder, "properties.yml");

        YamlConfiguration legacy =
                loadFile(legacyEconomyFile);

        YamlConfiguration oldTransactions =
                loadFile(legacyTransactionsFile);

        YamlConfiguration oldProperties =
                loadFile(legacyPropertiesFile);

        result.set("version", 2);

        result.createSection("players");
        result.createSection("accounts");
        result.createSection("property-owners");
        result.createSection("history");


        /*
         * MIGRATE EXISTING PLAYER BALANCES
         */

        for (String key : legacy.getKeys(false)) {

            UUID uuid;

            try {

                uuid = UUID.fromString(key);

            } catch (IllegalArgumentException ignored) {

                continue;
            }

            result.set(
                    playerPath(uuid),
                    readLegacyMoney(legacy, key, 0.0)
            );
        }


        /*
         * MIGRATE GOVERNMENT AND STORE BALANCES
         */

        long oldTreasury = readLegacyMoney(
                legacy,
                "accounts.treasury",
                0.0
        );

        result.set(
                "accounts.anacortes",
                readLegacyMoney(
                        legacy,
                        "accounts.anacortes",
                        toDollars(oldTreasury)
                )
        );

        result.set(
                "accounts.washington",
                readLegacyMoney(
                        legacy,
                        "accounts.washington",
                        0.0
                )
        );

        result.set(
                "accounts.usa",
                readLegacyMoney(
                        legacy,
                        "accounts.usa",
                        0.0
                )
        );

        result.set(
                STORE,
                readLegacyMoney(
                        legacy,
                        "accounts.general-store",
                        10000.0
                )
        );


        /*
         * PRESERVE EXISTING PROPERTY OWNERSHIP
         */

        ConfigurationSection properties =
                oldProperties.getConfigurationSection(
                        "properties"
                );

        if (properties != null) {

            for (String id : properties.getKeys(false)) {

                String owner = properties.getString(
                        id + ".owner"
                );

                if (owner == null
                        || owner.isBlank()) {

                    continue;
                }

                if (!validPropertyId(id)) {

                    throw new IllegalArgumentException(
                            "Invalid legacy property ID: "
                            + id
                    );
                }

                UUID uuid = UUID.fromString(owner);

                result.set(
                        "property-owners." + id,
                        uuid.toString()
                );
            }
        }


        /*
         * PRESERVE HISTORICAL TRANSACTION RECORDS
         */

        long nextId = Math.max(
                0L,
                oldTransactions.getLong(
                        "next-id",
                        0L
                )
        );

        ConfigurationSection history =
                oldTransactions.getConfigurationSection(
                        "history"
                );

        if (history != null) {

            for (String id : history.getKeys(false)) {

                ConfigurationSection entry =
                        history.getConfigurationSection(id);

                if (entry == null) {

                    throw new IllegalArgumentException(
                            "Invalid legacy transaction: "
                            + id
                    );
                }

                for (String field :
                        entry.getKeys(true)) {

                    if (!entry.isConfigurationSection(
                            field)) {

                        result.set(
                                "legacy-history."
                                + id + "." + field,
                                entry.get(field)
                        );
                    }
                }

                try {

                    nextId = Math.max(
                            nextId,
                            Long.parseLong(id)
                    );

                } catch (NumberFormatException ignored) {
                    // Preserve the historical record.
                }
            }
        }

        result.set("next-id", nextId);

        plugin.getLogger().info(
                "Legacy economy migration prepared."
        );

        return result;
    }


    private static long readLegacyMoney(
            YamlConfiguration source,
            String path,
            double fallback
    ) {

        if (!source.contains(path)) {

            return toCents(fallback);
        }

        Object value = source.get(path);

        if (!(value instanceof Number)) {

            throw new IllegalArgumentException(
                    "Invalid legacy balance: " + path
            );
        }

        return toCents(
                ((Number) value).doubleValue()
        );
    }


    /*
     * ============================================================
     * VALIDATE SAVED DATA
     * ============================================================
     */

    private static long readCents(
            YamlConfiguration source,
            String path
    ) {

        Object value = source.get(path);

        if (!(value instanceof Integer)
                && !(value instanceof Long)) {

            throw new IllegalArgumentException(
                    "Invalid account: " + path
            );
        }

        long cents = ((Number) value).longValue();

        if (cents < 0 || cents > MAX_BALANCE) {

            throw new IllegalArgumentException(
                    "Invalid balance: " + path
            );
        }

        return cents;
    }


    private static boolean validPropertyId(
            String id
    ) {

        return id != null
                && id.matches("[A-Za-z0-9_-]{1,128}");
    }


    private static void validateData(
            YamlConfiguration source
    ) {

        if (source.getInt("version") != 2) {

            throw new IllegalArgumentException(
                    "Unsupported economy version."
            );
        }

        readCents(source, STORE);

        for (Treasury treasury : Treasury.values()) {

            readCents(
                    source,
                    treasuryPath(treasury)
            );
        }

        ConfigurationSection players =
                source.getConfigurationSection(
                        "players"
                );

        if (players == null) {

            throw new IllegalArgumentException(
                    "Missing player accounts."
            );
        }

        for (String key : players.getKeys(false)) {

            UUID.fromString(key);

            readCents(
                    source,
                    "players." + key
            );
        }

        ConfigurationSection owners =
                source.getConfigurationSection(
                        "property-owners"
                );

        if (owners == null) {

            throw new IllegalArgumentException(
                    "Missing property ownership records."
            );
        }

        for (String id : owners.getKeys(false)) {

            if (!validPropertyId(id)) {

                throw new IllegalArgumentException(
                        "Invalid property ID: " + id
                );
            }

            UUID.fromString(
                    owners.getString(id, "")
            );
        }

        long nextId = source.getLong("next-id", -1);

        if (nextId < 0
                || nextId == Long.MAX_VALUE) {

            throw new IllegalArgumentException(
                    "Invalid transaction ID."
            );
        }

        ConfigurationSection history =
                source.getConfigurationSection("history");

        if (history == null) {

            throw new IllegalArgumentException(
                    "Missing transaction history."
            );
        }

        for (String id : history.getKeys(false)) {

            long number = Long.parseLong(id);

            if (number <= 0 || number > nextId) {

                throw new IllegalArgumentException(
                        "Invalid transaction history ID."
                );
            }
        }
    }


    /*
     * ============================================================
     * ATOMIC FINANCIAL STORAGE
     * ============================================================
     */

    private static void writeAtomically(
            YamlConfiguration proposed
    ) throws IOException {

        Path destination = economyFile.toPath();

        Path temporary = Files.createTempFile(
                destination.getParent(),
                "earthbound-economy-",
                ".tmp"
        );

        boolean moved = false;

        try {

            byte[] bytes = proposed.saveToString()
                    .getBytes(StandardCharsets.UTF_8);

            try (FileChannel channel =
                    FileChannel.open(
                            temporary,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING
                    )) {

                ByteBuffer buffer =
                        ByteBuffer.wrap(bytes);

                while (buffer.hasRemaining()) {

                    channel.write(buffer);
                }

                channel.force(true);
            }

            Files.move(
                    temporary,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );

            moved = true;

        } finally {

            if (!moved) {

                try {

                    Files.deleteIfExists(temporary);

                } catch (IOException ignored) {
                    // Preserve the original failure.
                }
            }
        }
    }


    private static YamlConfiguration copyData()
            throws Exception {

        YamlConfiguration copy =
                new YamlConfiguration();

        copy.loadFromString(
                data.saveToString()
        );

        return copy;
    }


    private static boolean commit(
            YamlConfiguration proposed
    ) {

        try {

            validateData(proposed);

            writeAtomically(proposed);

            data = proposed;

            return true;

        } catch (Exception exception) {

            ready = false;

            plugin.getLogger().severe(
                    "Financial commit failed: "
                    + exception.getMessage()
            );

            plugin.getLogger().severe(
                    "Economy disabled pending administrator review."
            );

            return false;
        }
    }


    public static synchronized void save() {

        /*
         * Every successful transaction is already saved.
         * Never overwrite committed financial data during
         * shutdown following a failed transaction.
         */

        if (!ready || data == null) {

            return;
        }
    }


    public static synchronized boolean isReady() {

        return ready;
    }


    /*
     * ============================================================
     * TRANSACTION HISTORY
     * ============================================================
     */

    private static void record(
            YamlConfiguration proposed,
            String from,
            String to,
            long cents,
            String reason
    ) {

        long id = Math.addExact(
                proposed.getLong("next-id"),
                1L
        );

        proposed.set("next-id", id);

        String path = "history." + id;

        proposed.set(
                path + ".timestamp",
                System.currentTimeMillis()
        );

        proposed.set(path + ".from", from);
        proposed.set(path + ".to", to);
        proposed.set(path + ".amount-cents", cents);

        proposed.set(
                path + ".reason",
                reason == null ? "" : reason
        );
    }


    /*
     * ============================================================
     * PLAYER ACCOUNTS
     * ============================================================
     */

    private static boolean ensureAccount(
            Player player
    ) {

        if (!ready || player == null) {

            return false;
        }

        UUID uuid = player.getUniqueId();

        String path = playerPath(uuid);

        if (data.contains(path)) {

            return true;
        }

        try {

            long startingMoney = toCents(
                    plugin.getConfig().getDouble(
                            "economy.starting-money",
                            100000.0
                    )
            );

            YamlConfiguration proposed =
                    copyData();

            proposed.set(path, startingMoney);

            record(
                    proposed,
                    "SYSTEM",
                    uuid.toString(),
                    startingMoney,
                    "New player starting balance"
            );

            return commit(proposed);

        } catch (Exception exception) {

            plugin.getLogger().severe(
                    "Account creation failed: "
                    + exception.getMessage()
            );

            return false;
        }
    }


    public static synchronized void createAccount(
            Player player
    ) {

        ensureAccount(player);
    }


    public static synchronized double getBalance(
            Player player
    ) {

        if (!ensureAccount(player)) {

            return 0.0;
        }

        return toDollars(
                readCents(
                        data,
                        playerPath(player.getUniqueId())
                )
        );
    }


    public static synchronized double getStoreBalance() {

        if (!ready) {

            return 0.0;
        }

        return toDollars(
                readCents(data, STORE)
        );
    }


    public static synchronized double getTreasuryBalance(
            Treasury treasury
    ) {

        if (!ready || treasury == null) {

            return 0.0;
        }

        return toDollars(
                readCents(
                        data,
                        treasuryPath(treasury)
                )
        );
    }


    public static synchronized double getTreasuryBalance() {

        return getTreasuryBalance(Treasury.ANACORTES)
                + getTreasuryBalance(Treasury.WASHINGTON)
                + getTreasuryBalance(Treasury.USA);
    }


    /*
     * ============================================================
     * SECURE MONEY TRANSFER ENGINE
     * ============================================================
     */

    private static boolean moveMoney(
            String fromPath,
            String toPath,
            String fromName,
            String toName,
            long cents,
            String reason
    ) {

        if (!ready
                || cents <= 0
                || cents > MAX_TRANSACTION
                || (fromPath == null && toPath == null)) {

            return false;
        }

        if (fromPath != null
                && fromPath.equals(toPath)) {

            return false;
        }

        try {

            YamlConfiguration proposed =
                    copyData();

            if (fromPath != null) {

                long available = readCents(
                        proposed,
                        fromPath
                );

                if (available < cents) {

                    return false;
                }

                proposed.set(
                        fromPath,
                        available - cents
                );
            }

            if (toPath != null) {

                long updated = Math.addExact(
                        readCents(proposed, toPath),
                        cents
                );

                if (updated > MAX_BALANCE) {

                    return false;
                }

                proposed.set(toPath, updated);
            }

            record(
                    proposed,
                    fromName,
                    toName,
                    cents,
                    reason
            );

            return commit(proposed);

        } catch (Exception exception) {

            plugin.getLogger().severe(
                    "Transaction rejected: "
                    + exception.getMessage()
            );

            return false;
        }
    }


    /*
     * SYSTEM CREDITS
     */

    public static synchronized void addMoney(
            Player player,
            double amount
    ) {

        long cents = transactionCents(amount);

        if (cents <= 0 || !ensureAccount(player)) {

            return;
        }

        UUID uuid = player.getUniqueId();

        moveMoney(
                null,
                playerPath(uuid),
                "SYSTEM",
                uuid.toString(),
                cents,
                "Money credited"
        );
    }


    /*
     * SYSTEM DEBITS
     */

    public static synchronized boolean removeMoney(
            Player player,
            double amount
    ) {

        long cents = transactionCents(amount);

        if (cents <= 0 || !ensureAccount(player)) {

            return false;
        }

        UUID uuid = player.getUniqueId();

        return moveMoney(
                playerPath(uuid),
                null,
                uuid.toString(),
                cents,
                "SYSTEM",
                "Money removed"
        );
    }


    /*
     * ============================================================
     * GENERAL STORE
     * ============================================================
     */

    public static synchronized boolean payStore(
            Player player,
            double amount,
            String reason
    ) {

        long cents = transactionCents(amount);

        if (cents <= 0 || !ensureAccount(player)) {

            return false;
        }

        UUID uuid = player.getUniqueId();

        return moveMoney(
                playerPath(uuid),
                STORE,
                uuid.toString(),
                "GENERAL_STORE",
                cents,
                reason
        );
    }


    public static synchronized boolean storePaysPlayer(
            Player player,
            double amount,
            String reason
    ) {

        long cents = transactionCents(amount);

        if (cents <= 0 || !ensureAccount(player)) {

            return false;
        }

        UUID uuid = player.getUniqueId();

        return moveMoney(
                STORE,
                playerPath(uuid),
                "GENERAL_STORE",
                uuid.toString(),
                cents,
                reason
        );
    }


    /*
     * ============================================================
     * PROPERTY OWNERSHIP LOOKUP
     * ============================================================
     */

    public static synchronized UUID getPropertyOwner(
            String propertyId
    ) {

        if (!ready || data == null
                || !validPropertyId(propertyId)) {

            throw new IllegalStateException(
                    "Property ownership data unavailable."
            );
        }

        String owner = data.getString(
                "property-owners." + propertyId
        );

        if (owner == null) {

            return null;
        }

        return UUID.fromString(owner);
    }


    /*
     * ============================================================
     * INDEPENDENT RESIDENTIAL TRANSACTION PROTECTION
     * ============================================================
     *
     * The economy must never trust:
     *
     * - A property sale banner.
     * - An open purchase menu.
     * - Prior purchase eligibility checks.
     * - A caller's requested property price.
     * - Successful generation callbacks alone.
     *
     * The authoritative installation record must be
     * COMPLETED and match the approved residential site.
     *
     * This inspection must run on the main thread
     * while the economy transaction lock is held.
     *
     * No financial changes occur during this inspection.
     * ============================================================
     */

    private static String inspectPropertyTransaction(
            Player player,
            String propertyId,
            long cents,
            EarthProperty property
    ) {

        if (!Bukkit.isPrimaryThread()) {

            return "Main-thread execution required.";
        }

        if (!ready
                || data == null
                || plugin == null
                || !plugin.isEnabled()) {

            return "Economy unavailable.";
        }

        if (player == null || !player.isOnline()) {

            return "Purchasing player unavailable.";
        }

        if (propertyId == null
                || !propertyId.matches("guemes_home_0[1-6]")) {

            return "Invalid Friends Alpha property identity.";
        }

        if (cents <= 0 || cents > MAX_TRANSACTION) {

            return "Invalid purchase amount.";
        }

        if (!EarthPropertyManager.isReady()) {

            return "Property ownership system unavailable.";
        }

        if (property == null

                || !propertyId.equals(property.getId())

                || EarthPropertyManager.getProperty(
                        propertyId
                ) != property) {

            return "Registered property unavailable or changed.";
        }

        if (property.isOwned()) {

            return "Property already has an owner.";
        }

        /*
         * Independently validate the exact purchase price.
         */

        if (exactPropertyPriceCents(
                property.getPrice()
        ) != cents) {

            return "Registered property price mismatch.";
        }

        /*
         * Respect the current residential configuration.
         */

        if (!plugin.getConfig().getBoolean(
                "residential.enabled",
                false
        )) {

            return "Residential development disabled.";
        }

        if (!RESIDENTIAL_WORLD.equals(
                plugin.getConfig().getString(
                        "residential.world",
                        RESIDENTIAL_WORLD
                )
        )) {

            return "Invalid configured residential world.";
        }

        ConfigurationSection home =
                plugin.getConfig().getConfigurationSection(
                        "properties.alpha-homes."
                        + propertyId
                );

        if (home == null
                || !home.getBoolean("enabled", false)) {

            return "Residential property disabled.";
        }

        if (!home.contains("price")

                || exactPropertyPriceCents(
                        home.getDouble("price")
                ) != cents) {

            return "Configured property price mismatch.";
        }

        /*
         * Suspend transactions during active construction
         * or unresolved execution review.
         */

        if (EarthResidentialExecution.hasActiveExecution()) {

            return "Residential execution requires attention.";
        }

        /*
         * Independently inspect installation storage.
         */

        if (!EarthResidentialInstallation.isReady()) {

            return "Installation storage unavailable.";
        }

        InstallationStatus installationStatus =
                EarthResidentialInstallation.getStatus(
                        propertyId
                );

        if (installationStatus
                != InstallationStatus.COMPLETED) {

            return "Installation is not COMPLETED: "
                    + installationStatus;
        }

        /*
         * Require the authoritative loaded world.
         *
         * Never load a missing world as part of a purchase.
         */

        if (!RESIDENTIAL_WORLD.equals(
                property.getWorldName()
        )

                || !RESIDENTIAL_WORLD.equals(
                        home.getString(
                                "world",
                                RESIDENTIAL_WORLD
                        )
                )) {

            return "Residential property world mismatch.";
        }

        World world =
                Bukkit.getWorld(RESIDENTIAL_WORLD);

        if (world == null

                || !RESIDENTIAL_WORLD.equals(
                        world.getName()
                )

                || Bukkit.getWorld(world.getUID()) != world) {

            return "Residential world unavailable.";
        }

        /*
         * Independently resolve the registered
         * residential construction site.
         */

        ResidentialSite site =
                EarthGuemesGenerator.getResidentialSite(
                        propertyId
                );

        if (site == null

                || !propertyId.equals(
                        site.getPropertyId()
                )

                || site.getDesign() == null) {

            return "Registered residential site unavailable.";
        }

        /*
         * Check world UUID, design revision, coordinates,
         * orientation, ground height and approved boundaries.
         */

        if (!EarthResidentialInstallation
                .matchesApprovedSite(world, site)) {

            return "Persistent installation identity mismatch.";
        }

        /*
         * Independently verify the property boundaries
         * against the approved residential site.
         */

        if (property.getMinX() != site.getMinX()

                || property.getMaxX() != site.getMaxX()

                || property.getMinZ() != site.getMinZ()

                || property.getMaxZ() != site.getMaxZ()) {

            return "Approved property boundary mismatch.";
        }

        /*
         * Check authoritative persistent ownership.
         */

        String ownerPath =
                "property-owners." + propertyId;

        if (data.contains(ownerPath)) {

            return "Persistent property ownership already exists.";
        }

        /*
         * Request the full property management eligibility
         * inspection again from inside the economy.
         *
         * This adds the complete development separation,
         * configuration and property-column inspections.
         *
         * The manager performs read-only economy checks.
         * Java synchronized locks are reentrant, allowing
         * this same-thread inspection.
         *
         * Never call purchaseProperty() from here.
         */

        PurchaseEligibility eligibility =
                EarthPropertyManager
                        .checkPurchaseEligibility(propertyId);

        if (eligibility != PurchaseEligibility.ELIGIBLE) {

            return "Property eligibility rejected: "
                    + eligibility.name();
        }

        /*
         * Final storage and installation confirmation.
         */

        if (!ready

                || data == null

                || !EarthResidentialInstallation.isReady()

                || EarthResidentialInstallation.getStatus(
                        propertyId
                ) != InstallationStatus.COMPLETED) {

            return "Final installation readiness check failed.";
        }

        return null;
    }


    /*
     * ============================================================
     * AUTHORITATIVE ONE-PROPERTY-PER-PLAYER CHECK
     * ============================================================
     *
     * Include all persisted property ownership records,
     * even properties currently disabled in configuration.
     * ============================================================
     */

    private static boolean playerMayPurchaseProperty(
            UUID uuid
    ) {

        if (uuid == null || !ready || data == null) {

            return false;
        }

        ConfigurationSection owners =
                data.getConfigurationSection(
                        "property-owners"
                );

        if (owners == null) {

            return false;
        }

        for (String id : owners.getKeys(false)) {

            String savedOwner =
                    owners.getString(id);

            if (savedOwner == null) {

                return false;
            }

            UUID existingOwner =
                    UUID.fromString(savedOwner);

            if (uuid.equals(existingOwner)) {

                return false;
            }
        }

        return true;
    }


    /*
     * ============================================================
     * ATOMIC GOVERNMENT PROPERTY PURCHASE
     * ============================================================
     *
     * The financial transaction independently requires:
     *
     * - Main-thread execution.
     * - An available online player.
     * - Available financial storage.
     * - A valid registered Alpha property.
     * - Exact configured and registered pricing.
     * - Persistent COMPLETED installation status.
     * - Matching installation identity.
     * - Matching property boundaries.
     * - Full property purchase eligibility.
     * - No current property owner.
     * - One property per player.
     * - Sufficient available funds.
     *
     * Property balances, ownership and history are
     * committed together in the existing economy file.
     * ============================================================
     */

    public static synchronized boolean
            purchaseGovernmentProperty(
                    Player player,
                    double amount,
                    String propertyId
            ) {

        /*
         * No Bukkit world or property inspection
         * may run asynchronously.
         */

        if (!Bukkit.isPrimaryThread()) {

            return false;
        }

        if (!ready
                || data == null
                || plugin == null
                || !plugin.isEnabled()

                || player == null
                || !player.isOnline()

                || propertyId == null
                || !propertyId.matches("guemes_home_0[1-6]")) {

            return false;
        }

        long cents =
                exactPropertyPriceCents(amount);

        if (cents <= 0) {

            return false;
        }

        try {

            EarthProperty property =
                    EarthPropertyManager.getProperty(
                            propertyId
                    );

            /*
             * Reject unauthorized purchases before
             * creating an account or preparing a transaction.
             */

            String rejected =
                    inspectPropertyTransaction(
                            player,
                            propertyId,
                            cents,
                            property
                    );

            if (rejected != null) {

                plugin.getLogger().warning(
                        "Residential financial purchase rejected: "
                        + propertyId
                        + " - "
                        + rejected
                );

                return false;
            }

            UUID uuid = player.getUniqueId();

            String ownerPath =
                    "property-owners." + propertyId;

            if (data.contains(ownerPath)) {

                return false;
            }

            /*
             * FRIENDS ALPHA:
             * ONE PROPERTY PER PLAYER
             */

            if (!playerMayPurchaseProperty(uuid)) {

                return false;
            }

            /*
             * Only an otherwise eligible request
             * may initialize a missing player account.
             */

            if (!ensureAccount(player)) {

                return false;
            }

            if (!ready || data == null) {

                return false;
            }

            /*
             * Recheck persistent ownership after
             * account initialization.
             */

            if (data.contains(ownerPath)
                    || !playerMayPurchaseProperty(uuid)) {

                return false;
            }

            YamlConfiguration proposed =
                    copyData();

            String account =
                    playerPath(uuid);

            long balance = readCents(
                    proposed,
                    account
            );

            if (balance < cents) {

                return false;
            }

            /*
             * GOVERNMENT REVENUE DISTRIBUTION
             *
             * 60% ANACORTES
             * 20% WASHINGTON
             * 20% USA
             *
             * Federal share receives any remaining cents
             * so the total is always exact.
             */

            long cityShare = cents * 60 / 100;

            long stateShare = cents * 20 / 100;

            long federalShare =
                    cents - cityShare - stateShare;

            long cityBalance = Math.addExact(
                    readCents(
                            proposed,
                            "accounts.anacortes"
                    ),
                    cityShare
            );

            long stateBalance = Math.addExact(
                    readCents(
                            proposed,
                            "accounts.washington"
                    ),
                    stateShare
            );

            long federalBalance = Math.addExact(
                    readCents(
                            proposed,
                            "accounts.usa"
                    ),
                    federalShare
            );

            if (cityBalance > MAX_BALANCE
                    || stateBalance > MAX_BALANCE
                    || federalBalance > MAX_BALANCE) {

                return false;
            }

            proposed.set(
                    account,
                    balance - cents
            );

            proposed.set(
                    "accounts.anacortes",
                    cityBalance
            );

            proposed.set(
                    "accounts.washington",
                    stateBalance
            );

            proposed.set(
                    "accounts.usa",
                    federalBalance
            );

            /*
             * SAVE OWNERSHIP IN THE SAME TRANSACTION
             */

            proposed.set(
                    ownerPath,
                    uuid.toString()
            );

            String reason =
                    "Property purchase: " + propertyId;

            record(
                    proposed,
                    uuid.toString(),
                    "ANACORTES_TREASURY",
                    cityShare,
                    reason
            );

            record(
                    proposed,
                    uuid.toString(),
                    "WASHINGTON_TREASURY",
                    stateShare,
                    reason
            );

            record(
                    proposed,
                    uuid.toString(),
                    "USA_TREASURY",
                    federalShare,
                    reason
            );

            /*
             * FINAL INDEPENDENT PURCHASE INSPECTION
             *
             * Proposed balances and ownership remain
             * separate from the authoritative economy.
             *
             * Reject the proposal without committing
             * if any installation conditions changed.
             */

            rejected =
                    inspectPropertyTransaction(
                            player,
                            propertyId,
                            cents,
                            property
                    );

            if (rejected != null) {

                plugin.getLogger().warning(
                        "Final residential financial "
                        + "purchase rejected: "
                        + propertyId
                        + " - "
                        + rejected
                );

                return false;
            }

            if (data.contains(ownerPath)

                    || !playerMayPurchaseProperty(uuid)

                    || readCents(data, account) != balance) {

                return false;
            }

            /*
             * ALL BALANCES AND OWNERSHIP ARE
             * COMMITTED TOGETHER.
             */

            if (!commit(proposed)) {

                return false;
            }

            /*
             * Financial ownership is now persistent.
             *
             * Synchronize the registered property owner
             * here as well as in EarthPropertyManager.
             *
             * This protects ownership consistency when
             * a valid purchase calls this method directly.
             */

            property.setOwner(uuid);

            return true;

        } catch (Exception exception) {

            plugin.getLogger().severe(
                    "Property purchase rejected: "
                    + exception.getMessage()
            );

            return false;
        }
    }


    /*
     * ============================================================
     * GOVERNMENT TREASURY PAYMENTS
     * ============================================================
     */

    public static synchronized boolean treasuryPaysPlayer(
            Treasury treasury,
            Player player,
            double amount,
            String reason
    ) {

        long cents = transactionCents(amount);

        if (treasury == null
                || cents <= 0
                || reason == null
                || reason.isBlank()
                || reason.length() > 512
                || !ensureAccount(player)) {

            return false;
        }

        UUID uuid = player.getUniqueId();

        return moveMoney(
                treasuryPath(treasury),
                playerPath(uuid),
                treasury.name() + "_TREASURY",
                uuid.toString(),
                cents,
                reason
        );
    }


    /*
     * ============================================================
     * PLAYER-TO-PLAYER PAYMENTS
     * ============================================================
     */

    public static synchronized boolean transfer(
            Player sender,
            Player recipient,
            double amount
    ) {

        if (sender == null || recipient == null) {

            return false;
        }

        UUID from = sender.getUniqueId();

        UUID to = recipient.getUniqueId();

        if (from.equals(to)) {

            return false;
        }

        long cents = transactionCents(amount);

        if (cents <= 0
                || !ensureAccount(sender)
                || !ensureAccount(recipient)) {

            return false;
        }

        return moveMoney(
                playerPath(from),
                playerPath(to),
                from.toString(),
                to.toString(),
                cents,
                "Player transfer"
        );
    }

}
