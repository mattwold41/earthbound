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
                "EarthBound properties loaded: "
                + properties.size()
        );
    }

    /*
     * REGISTER SIX ALPHA PROPERTIES
     *
     * Coordinates are deliberately not assigned yet.
     * We must verify the actual road and house locations
     * before enabling purchases.
     */

    private static void registerAlphaProperties() {

        // Property boundaries will be registered
        // after we verify their exact locations.
    }

    /*
     * REGISTER A PROPERTY
     */

    public static boolean registerProperty(
            EarthProperty property
    ) {

        if (property == null) {
            return false;
        }

        if (properties.containsKey(property.getId())) {
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

    /*
     * CHECK PROPERTY OVERLAP
     */

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
     * FIND PROPERTY BY ID
     */

    public static EarthProperty getProperty(String id) {

        if (id == null) {
            return null;
        }

        return properties.get(id);
    }

    /*
     * GET ALL PROPERTIES
     */

    public static Collection<EarthProperty> getProperties() {

        return Collections.unmodifiableCollection(
                properties.values()
        );
    }

    /*
     * FIND PROPERTY AT LOCATION
     */

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

    /*
     * GET PLAYER'S OWNED PROPERTIES
     */

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

    /*
     * CHECK PLAYER OWNERSHIP
     */

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
     *
     * Government revenue is divided:
     *
     * 60% Anacortes
     * 20% Washington
     * 20% USA
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

        /*
         * ALPHA RULE:
         * ONE HOME PER PLAYER
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

        if (price <= 0.0) {

            player.sendMessage(
                    "This property is not available for purchase."
            );

            return false;
        }

        if (EarthEconomy.getBalance(player) < price) {

            player.sendMessage(
                    "You do not have enough money."
            );

            return false;
        }

        /*
         * PROCESS GOVERNMENT PURCHASE
         */

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

        /*
         * ASSIGN OWNERSHIP
         */

        property.setOwner(
                player.getUniqueId()
        );

        save();

        player.sendMessage(
                "You purchased "
                + property.getName()
                + " for $"
                + String.format("%.2f", price)
                + "!"
        );

        return true;
    }

    /*
     * LOAD PROPERTY OWNERS
     */

    private static void loadOwners() {

        for (EarthProperty property : properties.values()) {

            String path =
                    "properties."
                    + property.getId()
                    + ".owner";

            String savedOwner =
                    propertyData.getString(path);

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
                    "Failed to save property data: "
                    + exception.getMessage()
            );

            return false;
        }
    }
}
