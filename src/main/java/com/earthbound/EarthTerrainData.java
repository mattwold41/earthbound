package com.earthbound;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EarthTerrainData {

    /*
     * EarthBound terrain cache.
     *
     * Minecraft terrain generation must be fast.
     * We therefore keep downloaded terrain information
     * separate from the chunk generator.
     *
     * Later this class will load USGS 3DEP terrain tiles
     * covering Guemes Island.
     */

    private static final Map<String, Double> ELEVATION_CACHE =
            new ConcurrentHashMap<>();


    private EarthTerrainData() {
        // Utility class - do not create instances.
    }


    public static void storeElevation(
            double latitude,
            double longitude,
            double elevationMeters) {

        String key =
                createKey(
                        latitude,
                        longitude
                );

        ELEVATION_CACHE.put(
                key,
                elevationMeters
        );
    }


    public static Double getElevation(
            double latitude,
            double longitude) {

        String key =
                createKey(
                        latitude,
                        longitude
                );

        return ELEVATION_CACHE.get(key);
    }


    public static boolean hasElevation(
            double latitude,
            double longitude) {

        String key =
                createKey(
                        latitude,
                        longitude
                );

        return ELEVATION_CACHE.containsKey(key);
    }


    public static int getCachedPointCount() {

        return ELEVATION_CACHE.size();
    }


    public static void clearCache() {

        ELEVATION_CACHE.clear();
    }


    private static String createKey(
            double latitude,
            double longitude) {

        double roundedLat =
                Math.round(latitude * 100000.0)
                        / 100000.0;

        double roundedLon =
                Math.round(longitude * 100000.0)
                        / 100000.0;

        return roundedLat
                + ","
                + roundedLon;
    }
}
