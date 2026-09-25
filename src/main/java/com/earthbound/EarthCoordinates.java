package com.earthbound;

public class EarthCoordinates {


    /*
     * EarthBound scale:
     *
     * 1 Minecraft block = 2 real-world meters
     */
    private static final double METERS_PER_BLOCK = 2.0;


    /*
     * Guemes Island spawn point
     */
    private static final double SPAWN_LAT =
            48.5265;

    private static final double SPAWN_LON =
            -122.6165;



    public static double minecraftToLatitude(
            int z) {

        return SPAWN_LAT -
                (z * METERS_PER_BLOCK
                        / 111320.0);
    }



    public static double minecraftToLongitude(
            int x) {


        double metersPerLongitude =
                111320.0 *
                        Math.cos(
                                Math.toRadians(
                                        SPAWN_LAT
                                )
                        );


        return SPAWN_LON +
                (x * METERS_PER_BLOCK
                        / metersPerLongitude);
    }
}
