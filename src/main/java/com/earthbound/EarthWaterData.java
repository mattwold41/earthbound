package com.earthbound;

/*
 * EarthBound land/water data.
 *
 * This class will hold the coastline and water mask
 * separately from the USGS elevation system.
 *
 * Keeping this separate means we can improve or replace
 * the coastline source later without changing the
 * working elevation raster code.
 */
public class EarthWaterData {

    /*
     * Minecraft ocean level.
     */
    public static final int SEA_LEVEL = 63;


    private EarthWaterData() {
        // Utility class
    }


    /*
     * Returns whether a real-world coordinate
     * should be treated as water.
     *
     * IMPORTANT:
     * This is only the framework right now.
     * We will connect the real coastline data
     * in the next step.
     */
    public static boolean isWater(
            double latitude,
            double longitude) {

        return false;
    }
}
