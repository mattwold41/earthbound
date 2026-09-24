package com.earthbound;

public class EarthCoordinates {

    // Real world anchor point
    // Guemes Island Ferry Terminal
    private static final double START_LAT = 48.5228603;
    private static final double START_LON = -122.624694;


    // Minecraft coordinates of the spawn point
    // This location matches the ferry terminal
    private static final int START_X = -624;
    private static final int START_Z = -544;


    // EarthBound scale
    // 1 Minecraft block = 2 meters
    private static final double METERS_PER_BLOCK = 2.0;


    public static double getLatitude(int x, int z) {

        // Positive Z goes south in Minecraft
        // Negative Z goes north
        double metersNorth =
                (START_Z - z) * METERS_PER_BLOCK;


        double latitudeChange =
                metersNorth / 111320.0;


        return START_LAT + latitudeChange;
    }


    public static double getLongitude(int x, int z) {

        // Positive X goes east
        // Negative X goes west
        double metersEast =
                (x - START_X) * METERS_PER_BLOCK;


        double longitudeChange =
                metersEast /
                (111320.0 *
                Math.cos(Math.toRadians(START_LAT)));


        return START_LON + longitudeChange;
    }
}
