package com.earthbound;

public class EarthBuildingGenerator {

    /*
     * EarthBound building system.
     *
     * Buildings are stored using real-world
     * latitude and longitude instead of
     * permanent Minecraft X/Z coordinates.
     *
     * This allows EarthBound to reposition
     * buildings if the horizontal map scale
     * changes in the future.
     */

    private EarthBuildingGenerator() {
    }


    /*
     * Guemes Island General Store
     *
     * This will be EarthBound's first
     * scale-independent real-world building.
     *
     * Building construction and placement
     * will be added in the next steps.
     */
    public static final double
            GUEMES_STORE_LATITUDE =
            48.529460;

    public static final double
            GUEMES_STORE_LONGITUDE =
            -122.624110;
}
