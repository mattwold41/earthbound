package com.earthbound;

public class EarthCoordinates {

    /*
     * EarthBound geographic anchor.
     *
     * Guemes Island - south side ferry terminal.
     *
     * Minecraft:
     * X = -624
     * Z = -544
     *
     * Real Earth:
     * Latitude  = 48.528523
     * Longitude = -122.625106
     */

    private static final double START_LAT =
            48.528523;

    private static final double START_LON =
            -122.625106;

    private static final int START_X =
            -624;

    private static final int START_Z =
            -544;

    /*
     * EarthBound horizontal scale.
     *
     * 1 Minecraft block = 1 real-world meter.
     *
     * This is a 1:1 horizontal scale.
     *
     * If we ever decide to change EarthBound
     * to 1:2 later, this can be changed to:
     *
     * 2.0
     */
    private static final double METERS_PER_BLOCK =
            1.0;

    private EarthCoordinates() {
    }

    /*
     * Converts Minecraft coordinates
     * into real-world latitude.
     */
    public static double getLatitude(
            int x,
            int z) {

        /*
         * Minecraft Z decreases when
         * traveling north.
         */
        double metersNorth =
                (START_Z - z)
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
     */
    public static double getLongitude(
            int x,
            int z) {

        /*
         * Minecraft X increases when
         * traveling east.
         */
        double metersEast =
                (x - START_X)
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
