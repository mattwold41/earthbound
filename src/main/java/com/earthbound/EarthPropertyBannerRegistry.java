package com.earthbound;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class EarthPropertyBannerRegistry {

    private static final Map<String, Location> banners =
            new HashMap<>();

    /*
     * REGISTER AN OFFICIAL FOR SALE BANNER
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

        banners.put(
                propertyId,
                location.getBlock().getLocation()
        );
    }

    /*
     * CHECK WHETHER A BANNER IS OFFICIAL
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

        if (registered == null) {
            return false;
        }

        return registered.getWorld() != null
                && location.getWorld() != null
                && registered.getWorld().getUID().equals(
                        location.getWorld().getUID()
                )
                && registered.getBlockX()
                        == location.getBlockX()
                && registered.getBlockY()
                        == location.getBlockY()
                && registered.getBlockZ()
                        == location.getBlockZ();
    }

    /*
     * GET REGISTERED BANNER LOCATION
     */

    public static Location getBannerLocation(
            String propertyId
    ) {

        Location location =
                banners.get(propertyId);

        return location == null
                ? null
                : location.clone();
    }

    /*
     * CLEAR REGISTRY
     */

    public static void clear() {
        banners.clear();
    }
}
