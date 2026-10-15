package com.earthbound;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthPropertyManager {

    private static JavaPlugin plugin;
    private static File propertyFile;
    private static FileConfiguration propertyData;

    private static final Map<String, EarthProperty> properties =
            new LinkedHashMap<>();

    /*
     * INITIALIZE PROPERTY SYSTEM
     */

    public static void setup(JavaPlugin earthBoundPlugin) {

        plugin = earthBoundPlugin;
        properties.clear();

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        propertyFile = new File(
                plugin.getDataFolder(),
                "properties.yml"
        );

        propertyData =
                YamlConfiguration.loadConfiguration(propertyFile);

        registerAlphaProperties();
        loadOwners();

        plugin.getLogger().info(
                "EarthBound properties registered: "
                + properties.size()
        );
    }

    /*
     * LOAD ALPHA PROPERTIES FROM CONFIG.YML
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

                String name =
                        home.getString("name", id);

                String world =
                        home.getString("world", "earthbound");

                if (!home.contains("min-x")
                        || !home.contains("min-z")
                        || !home.contains("price")) {

                    plugin.getLogger().warning(
                            "Missing coordinates or price for "
                            + id
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

                double price =
                        home.getDouble("price");

                if (!Double.isFinite(price)
                        || price <= 0.0) {

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
     * REGISTER A PROPERTY
     */

    public static boolean registerProperty(
            EarthProperty property
    ) {

        if (property == null
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
     * PROPERTY LOOKUPS
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

        if (playerId == null) {
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

        return player != null
                && property != null
                && property.isOwnedBy(
                        player.getUniqueId()
                );
    }

    /*
     * PURCHASE GOVERNMENT PROPERTY
     */

    public static boolean purchaseProperty(
            Player player,
            String propertyId
    ) {

        if (player == null || propertyId == null) {
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

        if (property.isOwned()) {

            player.sendMessage(
                    "This property already has an owner."
            );

            return false;
        }

        if (!getOwnedProperties(
                player.getUniqueId()
        ).isEmpty()) {

            player.sendMessage(
                    "You already own an Alpha property."
            );

            return false;
        }

        double price = property.getPrice();

        if (EarthEconomy.getBalance(player) < price) {

            player.sendMessage(
                    "You do not have enough money."
            );

            return false;
        }

        boolean paid =
                EarthEconomy.purchaseGovernmentProperty(
                        player,
                        price,
                        property.getId()
                );

        if (!paid) {

            player.sendMessage(
                    "Property payment failed."
            );

            return false;
        }

        property.setOwner(player.getUniqueId());

        if (!save()) {

            plugin.getLogger().severe(
                    "Property ownership save failed after "
                    + "payment for " + propertyId
                    + ". Administrator review required."
            );

            player.sendMessage(
                    "Your purchase needs administrator "
                    + "verification. Do not purchase again."
            );

            return false;
        }

        player.sendMessage(
                "You purchased "
                + property.getName()
                + " for $"
                + String.format(
                        java.util.Locale.US,
                        "%.2f",
                        price
                )
                + "!"
        );

        return true;
    }

    /*
     * LOAD SAVED OWNERS
     */

    private static void loadOwners() {

        for (EarthProperty property : properties.values()) {

            String savedOwner =
                    propertyData.getString(
                            "properties."
                            + property.getId()
                            + ".owner"
                    );

            if (savedOwner == null
                    || savedOwner.isBlank()) {
                continue;
            }

            try {

                property.setOwner(
                        UUID.fromString(savedOwner)
                );

            } catch (IllegalArgumentException exception) {

                plugin.getLogger().warning(
                        "Invalid owner UUID for "
                        + property.getId()
                );
            }
        }
    }

    /*
     * SAVE PROPERTY OWNERSHIP
     */

    public static boolean save() {

        if (propertyData == null
                || propertyFile == null) {
            return false;
        }

        for (EarthProperty property : properties.values()) {

            String path =
                    "properties."
                    + property.getId();

            propertyData.set(
                    path + ".name",
                    property.getName()
            );

            propertyData.set(
                    path + ".world",
                    property.getWorldName()
            );

            propertyData.set(
                    path + ".price",
                    property.getPrice()
            );

            propertyData.set(
                    path + ".minX",
                    property.getMinX()
            );

            propertyData.set(
                    path + ".maxX",
                    property.getMaxX()
            );

            propertyData.set(
                    path + ".minZ",
                    property.getMinZ()
            );

            propertyData.set(
                    path + ".maxZ",
                    property.getMaxZ()
            );

            propertyData.set(
                    path + ".owner",
                    property.getOwner() == null
                            ? null
                            : property.getOwner().toString()
            );
        }

        try {

            propertyData.save(propertyFile);
            return true;

        } catch (IOException exception) {

            plugin.getLogger().severe(
                    "Failed to save properties: "
                    + exception.getMessage()
            );

            return false;
        }
    }
}
