package com.earthbound;

public class EarthCoordinates {

    /*
     * EarthBound geographic anchor.
     *
     * Guemes Island Ferry Terminal
     * Minecraft: X -624, Z -544
     * Earth: 48.5228603, -122.624694
     */

    private static final double START_LAT =
            48.5228603;

    private static final double START_LON =
            -122.624694;

    private static final int START_X =
            -624;

    private static final int START_Z =
            -544;

    /*
     * EarthBound horizontal scale:
     * 1 Minecraft block = 2 real meters.
     */
    private static final double METERS_PER_BLOCK =
            2.0;


    private EarthCoordinates() {
    }


    public static double getLatitude(
            int x,
            int z) {

        double metersNorth =
                (START_Z - z)
                        * METERS_PER_BLOCK;

        double latitudeChange =
                metersNorth
                        / 111320.0;

        return START_LAT
                + latitudeChange;
    }


    public static double getLongitude(
            int x,
            int z) {

        double metersEast =
                (x - START_X)
                        * METERS_PER_BLOCK;

        double longitudeChange =
                metersEast
                        /
                        (111320.0
                                * Math.cos(
                                        Math.toRadians(
                                                START_LAT
                                        )
                                ));

        return START_LON
                + longitudeChange;
    }


    /*
     * Compatibility methods used by
     * EarthLocation.java.
     */

    public static double minecraftToLatitude(
            int z) {

        return getLatitude(
                START_X,
                z
        );
    }


    public static double minecraftToLongitude(
            int x) {

        return getLongitude(
                x,
                START_Z
        );
    }
}
