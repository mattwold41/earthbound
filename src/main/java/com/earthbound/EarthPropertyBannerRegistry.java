package com.earthbound;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class EarthPropertyBannerRegistry {

    private static final Map<String, Location> banners =
            new HashMap<>();


    /*
     * ==========================================================
     * INITIALIZE PROPERTY BANNER REGISTRY
     * ==========================================================
     */

    public static void setup(EarthBound plugin) {

        clear();

        ConfigurationSection homes =
                plugin.getConfig().getConfigurationSection(
                        "properties.alpha-homes"
                );

        if (homes == null) {

            plugin.getLogger().warning(
                    "No Alpha home configuration found."
            );

            return;
        }

        for (String propertyId : homes.getKeys(false)) {

            ConfigurationSection home =
                    homes.getConfigurationSection(
                            propertyId
                    );

            if (home == null) {
                continue;
            }

            if (!home.getBoolean("enabled", false)) {
                continue;
            }

            EarthProperty property =
                    EarthPropertyManager.getProperty(
                            propertyId
                    );

            if (property == null) {

                plugin.getLogger().warning(
                        "Property not registered: "
                        + propertyId
                );

                continue;
            }

            String worldName =
                    home.getString(
                            "world",
                            "earthbound"
                    );

            World world =
                    Bukkit.getWorld(worldName);

            if (world == null) {

                plugin.getLogger().warning(
                        "Banner world not found: "
                        + worldName
                );

                continue;
            }

            if (!home.isInt("banner-x")
                    || !home.isInt("banner-y")
                    || !home.isInt("banner-z")) {

                plugin.getLogger().warning(
                        "Missing banner coordinates: "
                        + propertyId
                );

                continue;
            }

            int x = home.getInt("banner-x");
            int y = home.getInt("banner-y");
            int z = home.getInt("banner-z");

            if (y < world.getMinHeight()
                    || y >= world.getMaxHeight()) {

                plugin.getLogger().warning(
                        "Invalid banner height: "
                        + propertyId
                );

                continue;
            }

            if (!property.contains(worldName, x, z)) {

                plugin.getLogger().warning(
                        "Banner is outside property: "
                        + propertyId
                );

                continue;
            }

            Location location =
                    new Location(
                            world,
                            x,
                            y,
                            z
                    );

            boolean duplicate = false;

            for (Location existing : banners.values()) {

                if (sameBlock(existing, location)) {

                    duplicate = true;
                    break;
                }
            }

            if (duplicate) {

                plugin.getLogger().warning(
                        "Duplicate banner location: "
                        + propertyId
                );

                continue;
            }

            registerBanner(
                    propertyId,
                    location
            );

            plugin.getLogger().info(
                    "Registered property banner: "
                    + propertyId
            );
        }

        plugin.getLogger().info(
                "EarthBound registered "
                + getBannerCount()
                + " official property banners."
        );
    }


    /*
     * ==========================================================
     * REGISTER OFFICIAL BANNER
     * ==========================================================
     */

    public static void registerBanner(
            String propertyId,
            Location location
    ) {

        if (propertyId == null
                || location == null
                || location.getWorld() == null) {
            return;
        }

        Location normalized =
                new Location(
                        location.getWorld(),
                        location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ()
                );

        banners.put(
                propertyId,
                normalized
        );
    }


    /*
     * ==========================================================
     * CHECK OFFICIAL BANNER
     * ==========================================================
     */

    public static boolean isOfficialBanner(
            String propertyId,
            Location location
    ) {

        if (propertyId == null
                || location == null) {
            return false;
        }

        Location registered =
                banners.get(propertyId);

        return sameBlock(
                registered,
                location
        );
    }


    /*
     * ==========================================================
     * COMPARE BLOCK LOCATIONS
     * ==========================================================
     */

    private static boolean sameBlock(
            Location first,
            Location second
    ) {

        if (first == null
                || second == null
                || first.getWorld() == null
                || second.getWorld() == null) {
            return false;
        }

        return first.getWorld().getUID().equals(
                    second.getWorld().getUID()
                )
                && first.getBlockX()
                    == second.getBlockX()
                && first.getBlockY()
                    == second.getBlockY()
                && first.getBlockZ()
                    == second.getBlockZ();
    }


    /*
     * ==========================================================
     * GET OFFICIAL BANNER LOCATION
     * ==========================================================
     */

    public static Location getBannerLocation(
            String propertyId
    ) {

        Location location =
                banners.get(propertyId);

        if (location == null) {
            return null;
        }

        return location.clone();
    }


    /*
     * ==========================================================
     * GET REGISTERED BANNER COUNT
     * ==========================================================
     */

    public static int getBannerCount() {

        return banners.size();
    }


    /*
     * ==========================================================
     * CLEAR REGISTRY
     * ==========================================================
     */

    public static void clear() {

        banners.clear();
    }

}
