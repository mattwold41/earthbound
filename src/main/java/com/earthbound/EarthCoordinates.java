package com.earthbound;

public class EarthCoordinates {

    // Real world anchor point
    // Guemes Island Ferry Terminal
    private static final double START_LAT = 48.5228603;
    private static final double START_LON = -122.624694;


    // Minecraft location of the anchor
    private static final int START_X = -789;
    private static final int START_Z = -501;


    // EarthBound scale
    // 1 block = 2 meters
    private static final double METERS_PER_BLOCK = 2.0;


    public static double getLatitude(int x, int z) {

        double metersNorth =
                (START_Z - z) * METERS_PER_BLOCK;


        double latitudeChange =
                metersNorth / 111320.0;


        return START_LAT + latitudeChange;
    }


    public static double getLongitude(int x, int z) {

        double metersEast =
                (x - START_X) * METERS_PER_BLOCK;


        double longitudeChange =
                metersEast /
                (111320.0 *
                Math.cos(Math.toRadians(START_LAT)));


        return START_LON + longitudeChange;
    }
}
