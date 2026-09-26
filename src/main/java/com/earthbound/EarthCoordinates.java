package com.earthbound;

public class EarthCoordinates {

    /*
     * EarthBound geographic anchor.
     *
     * Guemes Island ferry landing.
     *
     * Minecraft:
     * X = -660
     * Z = -446
     *
     * Real Earth:
     * Latitude  = 48.528160
     * Longitude = -122.624600
     */

    private static final double START_LAT =
            48.528160;

    private static final double START_LON =
            -122.624600;

    private static final int START_X =
            -660;

    private static final int START_Z =
            -446;

    /*
     * EarthBound horizontal scale.
     *
     * 1 Minecraft block = 1 real-world meter.
     */
    private static final double METERS_PER_BLOCK =
            1.0;

    private EarthCoordinates() {
    }

    /*
     * Converts Minecraft coordinates
     * into real-world latitude.
     *
     * EarthBound orientation:
     *
     * Z increasing = north
     * Z decreasing = south
     */
    public static double getLatitude(
            int x,
            int z) {

        double metersNorth =
                (z - START_Z)
                        * METERS_PER_BLOCK;

        double latitudeChange =
                metersNorth
                        / 111320.0;

        return START_LAT
                + latitudeChange;
    }

    /*
     * Converts Minecraft coordinates
     * into real-world longitude.
     *
     * EarthBound orientation:
     *
     * X decreasing = east
     * X increasing = west
     */
    public static double getLongitude(
            int x,
            int z) {

        double metersEast =
                (START_X - x)
                        * METERS_PER_BLOCK;

        double metersPerLongitudeDegree =
                111320.0
                        * Math.cos(
                                Math.toRadians(
                                        START_LAT
                                )
                        );

        double longitudeChange =
                metersEast
                        / metersPerLongitudeDegree;

        return START_LON
                + longitudeChange;
    }

    /*
     * Compatibility method used by
     * EarthLocation.java.
     */
    public static double minecraftToLatitude(
            int z) {

        return getLatitude(
                START_X,
                z
        );
    }

    /*
     * Compatibility method used by
     * EarthLocation.java.
     */
    public static double minecraftToLongitude(
            int x) {

        return getLongitude(
                x,
                START_Z
        );
    }
}
