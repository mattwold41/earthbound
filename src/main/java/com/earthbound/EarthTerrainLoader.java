package com.earthbound;

public class EarthTerrainLoader {

    private EarthTerrainLoader() {
        // Utility class - do not create instances.
    }

    /*
     * Loads one real-world elevation point and stores it
     * in the EarthBound terrain cache.
     *
     * This is our bridge between the existing USGS
     * elevation downloader and the new terrain system.
     *
     * We will expand this to load terrain areas/tiles
     * instead of individual points.
     */
    public static double loadElevation(
            double latitude,
            double longitude) {

        Double cachedElevation =
                EarthTerrainData.getElevation(
                        latitude,
                        longitude
                );

        if (cachedElevation != null) {
            return cachedElevation;
        }

        double elevation =
                EarthElevation.getElevation(
                        latitude,
                        longitude
                );

        EarthTerrainData.storeElevation(
                latitude,
                longitude,
                elevation
        );

        return elevation;
    }


    /*
     * Converts downloaded real-world elevation
     * into EarthBound's progressive Minecraft height.
     */
    public static int loadMinecraftHeight(
            double latitude,
            double longitude) {

        double elevation =
                loadElevation(
                        latitude,
                        longitude
                );

        return EarthElevation.getMinecraftHeight(
                elevation
        );
    }
}
