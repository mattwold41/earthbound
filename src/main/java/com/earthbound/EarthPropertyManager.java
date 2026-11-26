package com.earthbound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthPropertyManager {

    private static JavaPlugin plugin;

    private static boolean ownershipLoaded = false;

    private static final Map<String, EarthProperty> properties =
            new LinkedHashMap<>();


    /*
     * ============================================================
     * INITIALIZE PROPERTY SYSTEM
     * ============================================================
     */

    public static void setup(JavaPlugin earthBoundPlugin) {

        plugin = earthBoundPlugin;

        ownershipLoaded = false;

        properties.clear();

        registerAlphaProperties();

        if (!EarthEconomy.isReady()) {

            plugin.getLogger().severe(
                    "Property ownership could not be loaded "
                    + "because the economy is unavailable."
            );

            plugin.getLogger().severe(
                    "Property purchases are disabled. "
                    + "Registered plots remain protected."
            );

            return;
        }

        ownershipLoaded = loadOwners();

        plugin.getLogger().info(
                "EarthBound properties registered: "
                + properties.size()
        );

        if (ownershipLoaded) {

            plugin.getLogger().info(
                    "Secure property ownership loaded!"
            );

        } else {

            plugin.getLogger().severe(
                    "Property ownership initialization failed. "
                    + "Purchases are disabled."
            );
        }
    }


    public static boolean isReady() {

        return ownershipLoaded
                && EarthEconomy.isReady();
    }


    /*
     * ============================================================
     * LOAD ALPHA PROPERTIES FROM CONFIG.YML
     * ============================================================
     */

    private static void registerAlphaProperties() {

        ConfigurationSection section =
                plugin.getConfig().getConfigurationSection(
                        "properties.alpha-homes"
                );

        if (section == null) {

            plugin.getLogger().warning(
                    "No Alpha homes configured. "
                    + "Property purchases are disabled."
            );

            return;
        }

        for (String id : section.getKeys(false)) {

            ConfigurationSection home =
                    section.getConfigurationSection(id);

            if (home == null
                    || !home.getBoolean("enabled", false)) {

                continue;
            }

            try {

                if (!validPropertyId(id)) {

                    plugin.getLogger().warning(
                            "Invalid property ID: " + id
                    );

                    continue;
                }

                String name =
                        home.getString("name", id);

                String world =
                        home.getString("world", "earthbound");

                if (!home.isInt("min-x")
                        || !home.isInt("min-z")
                        || !home.contains("price")) {

                    plugin.getLogger().warning(
                            "Missing or invalid coordinates "
                            + "or price for " + id
                    );

                    continue;
                }

                int minX = home.getInt("min-x");

                int minZ = home.getInt("min-z");

                int width = home.getInt("width", 24);

                int length = home.getInt("length", 32);

                if (width <= 0 || length <= 0
                        || width > 256 || length > 256) {

                    plugin.getLogger().warning(
                            "Invalid property dimensions: "
                            + id
                    );

                    continue;
                }

                double price = home.getDouble("price");

                if (!validPrice(price)) {

                    plugin.getLogger().warning(
                            "Invalid property price: "
                            + id
                    );

                    continue;
                }

                int maxX =
                        Math.addExact(minX, width - 1);

                int maxZ =
                        Math.addExact(minZ, length - 1);

                EarthProperty property =
                        new EarthProperty(
                                id,
                                name,
                                world,
                                minX,
                                maxX,
                                minZ,
                                maxZ,
                                price
                        );

                if (registerProperty(property)) {

                    plugin.getLogger().info(
                            "Registered property "
                            + id
                            + " ("
                            + width
                            + "x"
                            + length
                            + ")"
                    );
                }

            } catch (RuntimeException exception) {

                plugin.getLogger().warning(
                        "Could not register property "
                        + id
                        + ": "
                        + exception.getMessage()
                );
            }
        }
    }


    /*
     * ============================================================
     * PROPERTY VALIDATION
     * ============================================================
     */

    private static boolean validPropertyId(String id) {

        return id != null
                && id.matches("[A-Za-z0-9_-]{1,128}");
    }


    private static boolean validPrice(double price) {

        if (!Double.isFinite(price)
                || price < 0.01
                || price > 1000000000.0) {

            return false;
        }

        double rounded =
                Math.round(price * 100.0) / 100.0;

        return Double.compare(price, rounded) == 0;
    }


    /*
     * ============================================================
     * REGISTER A PROPERTY
     * ============================================================
     */

    public static boolean registerProperty(
            EarthProperty property
    ) {

        if (property == null
                || !validPropertyId(property.getId())
                || !validPrice(property.getPrice())
                || properties.containsKey(property.getId())) {

            return false;
        }

        for (EarthProperty existing : properties.values()) {

            if (overlaps(existing, property)) {

                plugin.getLogger().warning(
                        "Overlapping property rejected: "
                        + property.getId()
                );

                return false;
            }
        }

        /*
         * Restore ownership for properties registered
         * after the initial startup process.
         */

        if (ownershipLoaded) {

            if (!EarthEconomy.isReady()) {

                return false;
            }

            try {

                property.setOwner(
                        EarthEconomy.getPropertyOwner(
                                property.getId()
                        )
                );

            } catch (RuntimeException exception) {

                plugin.getLogger().severe(
                        "Could not restore ownership for "
                        + property.getId()
                );

                return false;
            }

        } else {

            /*
             * Ownership will be restored during setup.
             */

            property.setOwner(null);
        }

        properties.put(property.getId(), property);

        return true;
    }


    private static boolean overlaps(
            EarthProperty first,
            EarthProperty second
    ) {

        if (!first.getWorldName().equals(
                second.getWorldName()
        )) {

            return false;
        }

        return first.getMinX() <= second.getMaxX()
                && first.getMaxX() >= second.getMinX()
                && first.getMinZ() <= second.getMaxZ()
                && first.getMaxZ() >= second.getMinZ();
    }


    /*
     * ============================================================
     * PROPERTY LOOKUPS
     * ============================================================
     */

    public static EarthProperty getProperty(String id) {

        if (id == null) {

            return null;
        }

        return properties.get(id);
    }


    public static Collection<EarthProperty> getProperties() {

        return Collections.unmodifiableCollection(
                properties.values()
        );
    }


    public static EarthProperty getPropertyAt(
            Location location
    ) {

        if (location == null
                || location.getWorld() == null) {

            return null;
        }

        for (EarthProperty property : properties.values()) {

            if (property.contains(
                    location.getWorld().getName(),
                    location.getBlockX(),
                    location.getBlockZ()
            )) {

                return property;
            }
        }

        return null;
    }


    public static List<EarthProperty> getOwnedProperties(
            UUID playerId
    ) {

        List<EarthProperty> owned = new ArrayList<>();

        if (playerId == null || !ownershipLoaded) {

            return owned;
        }

        for (EarthProperty property : properties.values()) {

            if (property.isOwnedBy(playerId)) {

                owned.add(property);
            }
        }

        return owned;
    }


    public static boolean ownsProperty(
            Player player,
            EarthProperty property
    ) {

        return ownershipLoaded
                && player != null
                && property != null
                && property.isOwnedBy(
                        player.getUniqueId()
                );
    }


    /*
     * ============================================================
     * PURCHASE GOVERNMENT PROPERTY
     * ============================================================
     */

    public static boolean purchaseProperty(
            Player player,
            String propertyId
    ) {

        if (player == null || propertyId == null) {

            return false;
        }

        /*
         * All purchases must run on the server thread.
         */

        if (!Bukkit.isPrimaryThread()) {

            plugin.getLogger().warning(
                    "Asynchronous property purchase rejected."
            );

            return false;
        }

        if (!player.isOnline()) {

            return false;
        }

        if (!isReady()) {

            player.sendMessage(
                    "Property purchases are temporarily "
                    + "unavailable."
            );

            return false;
        }

        EarthProperty property =
                getProperty(propertyId);

        if (property == null) {

            player.sendMessage(
                    "Property not found."
            );

            return false;
        }

        /*
         * CHECK EXISTING OWNERSHIP
         */

        if (property.isOwned()) {

            player.sendMessage(
                    "This property already has an owner."
            );

            return false;
        }

        /*
         * FRIENDS ALPHA:
         * ONE PROPERTY PER PLAYER
         */

        if (!getOwnedProperties(
                player.getUniqueId()
        ).isEmpty()) {

            player.sendMessage(
                    "You already own an Alpha property."
            );

            return false;
        }

        double price = property.getPrice();

        if (!validPrice(price)) {

            player.sendMessage(
                    "This property has an invalid price."
            );

            return false;
        }

        /*
         * CHECK PLAYER BALANCE
         */

        double balance =
                EarthEconomy.getBalance(player);

        if (!EarthEconomy.isReady()) {

            player.sendMessage(
                    "The economy is temporarily unavailable."
            );

            return false;
        }

        if (balance < price) {

            player.sendMessage(
                    "You do not have enough money."
            );

            return false;
        }

        /*
         * ATOMIC PROPERTY TRANSACTION
         *
         * EarthEconomy commits all of the following:
         *
         * 1. Player balance deduction
         * 2. Anacortes treasury deposit
         * 3. Washington treasury deposit
         * 4. USA treasury deposit
         * 5. Property ownership
         * 6. Transaction history
         */

        boolean purchased =
                EarthEconomy.purchaseGovernmentProperty(
                        player,
                        price,
                        property.getId()
                );

        if (!purchased) {

            if (!EarthEconomy.isReady()) {

                player.sendMessage(
                        "Financial storage is unavailable. "
                        + "Contact an administrator before retrying."
                );

            } else {

                player.sendMessage(
                        "Property purchase could not "
                        + "be completed."
                );
            }

            return false;
        }

        /*
         * The purchase has already been committed.
         *
         * Update the in-memory property owner.
         * No additional ownership save is required.
         */

        property.setOwner(
                player.getUniqueId()
        );

        plugin.getLogger().info(
                "Property purchased: "
                + property.getId()
                + " by "
                + player.getName()
                + " ("
                + player.getUniqueId()
                + ") for $"
                + String.format(
                        Locale.US,
                        "%.2f",
                        price
                )
        );

        player.sendMessage(
                "You purchased "
                + property.getName()
                + " for $"
                + String.format(
                        Locale.US,
                        "%.2f",
                        price
                )
                + "!"
        );

        return true;
    }


    /*
     * ============================================================
     * RESTORE SAVED PROPERTY OWNERS
     * ============================================================
     */

    private static boolean loadOwners() {

        Map<String, UUID> restoredOwners =
                new LinkedHashMap<>();

        try {

            /*
             * Read every owner before changing any
             * in-memory ownership records.
             */

            for (EarthProperty property : properties.values()) {

                UUID owner =
                        EarthEconomy.getPropertyOwner(
                                property.getId()
                        );

                restoredOwners.put(
                        property.getId(),
                        owner
                );
            }

        } catch (RuntimeException exception) {

            plugin.getLogger().severe(
                    "Could not load property ownership: "
                    + exception.getMessage()
            );

            return false;
        }

        int ownedCount = 0;

        for (EarthProperty property : properties.values()) {

            UUID owner =
                    restoredOwners.get(
                            property.getId()
                    );

            property.setOwner(owner);

            if (owner != null) {

                ownedCount++;
            }
        }

        plugin.getLogger().info(
                "Restored "
                + ownedCount
                + " owned properties."
        );

        return true;
    }


    /*
     * ============================================================
     * VERIFY SAVED PROPERTY OWNERSHIP
     * ============================================================
     */

    public static boolean save() {

        /*
         * Compatibility with EarthBound.onDisable().
         *
         * Ownership is saved automatically by EarthEconomy
         * when the financial transaction is committed.
         *
         * This method verifies that the current property
         * owners match the authoritative financial records.
         *
         * Never overwrite economy-v2.yml using a separate
         * property ownership file.
         */

        if (!isReady()) {

            return false;
        }

        try {

            for (EarthProperty property : properties.values()) {

                UUID savedOwner =
                        EarthEconomy.getPropertyOwner(
                                property.getId()
                        );

                if (!Objects.equals(
                        savedOwner,
                        property.getOwner()
                )) {

                    plugin.getLogger().severe(
                            "Property ownership mismatch: "
                            + property.getId()
                    );

                    return false;
                }
            }

            return true;

        } catch (RuntimeException exception) {

            plugin.getLogger().severe(
                    "Property ownership verification failed: "
                    + exception.getMessage()
            );

            return false;
        }
    }

}
