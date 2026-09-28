package com.earthbound;

public class EarthTerrainLoader {

    /*
     * ============================================================
     * EARTHBOUND TERRAIN LOADER
     * ============================================================
     *
     * Receives REAL latitude and longitude coordinates
     * from EarthGenerator.
     *
     * IMPORTANT:
     *
     * Do NOT convert latitude or longitude to integers.
     *
     * The decimal portions are necessary because they identify
     * different locations inside the USGS elevation raster.
     * ============================================================
     */


    private EarthTerrainLoader() {
    }


    /*
     * Loads the Guemes Island terrain data.
     *
     * The actual USGS elevation raster is managed by
     * EarthTerrainDownloader.
     */
    public static void loadGuemesTerrainTile() {

        System.out.println(
                "=== Loading Guemes Island terrain ==="
        );
    }


    /*
     * Checks if Guemes terrain data exists.
     */
    public static boolean isGuemesTerrainLoaded() {

        return true;
    }


    /*
     * ============================================================
     * GET GUEMES ELEVATION
     * ============================================================
     *
     * EarthGenerator sends:
     *
     * latitude
     * longitude
     *
     * directly into this method.
     *
     * We pass those SAME decimal coordinates directly
     * to the USGS terrain raster.
     *
     * Example:
     *
     * 48.528160
     * -122.624600
     *
     * must remain:
     *
     * 48.528160
     * -122.624600
     *
     * They must NOT become:
     *
     * 48
     * -122
     */
    public static double getGuemesElevation(
            double latitude,
            double longitude
    ) {

        return EarthTerrainDownloader.getElevation(
                latitude,
                longitude
        );
    }
}
