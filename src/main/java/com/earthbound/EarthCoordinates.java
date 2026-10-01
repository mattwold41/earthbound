package com.earthbound;

/**
 * Converts between Minecraft coordinates and real-world
 * latitude / longitude for EarthBound.
 *
 * EarthBound scale:
 * 1 Minecraft block = 2 real-world meters.
 *
 * Orientation:
 *
 * Minecraft -Z = North
 * Minecraft +Z = South
 *
 * Minecraft +X = East
 * Minecraft -X = West
 *
 * Therefore:
 *
 * Moving north:
 *     Minecraft Z decreases
 *     Latitude increases
 *
 * Moving south:
 *     Minecraft Z increases
 *     Latitude decreases
 *
 * Moving east:
 *     Minecraft X increases
 *     Longitude increases
 *
 * Moving west:
 *     Minecraft X decreases
 *     Longitude decreases
 */
public final class EarthCoordinates {

    /*
     * EarthBound geographic anchor.
     *
     * This point anchors the Minecraft world to Guemes Island.
     */
    public static final double START_LAT = 48.528160;
    public static final double START_LON = -122.624600;

    /*
     * Minecraft coordinates corresponding to the anchor above.
     */
    public static final int START_X = -660;
    public static final int START_Z = -446;

    /*
     * EarthBound world scale.
     *
     * 1 Minecraft block = 2 real-world meters.
     */
    public static final double METERS_PER_BLOCK = 2.0;

    /*
     * Approximate number of meters in one degree of latitude.
     */
    private static final double METERS_PER_DEGREE_LATITUDE = 111320.0;

    /*
     * Longitude distance depends on latitude.
     */
    private static final double METERS_PER_DEGREE_LONGITUDE =
            METERS_PER_DEGREE_LATITUDE *
            Math.cos(Math.toRadians(START_LAT));

    private EarthCoordinates() {
        // Utility class
    }

    /**
     * Convert Minecraft X/Z into real-world latitude.
     *
     * North is negative Minecraft Z.
     *
     * Therefore, when Z decreases,
     * latitude increases.
     */
    public static double getLatitude(int x, int z) {

        double metersNorth =
                (START_Z - z) * METERS_PER_BLOCK;

        return START_LAT +
                (metersNorth / METERS_PER_DEGREE_LATITUDE);
    }

    /**
     * Convert Minecraft X/Z into real-world longitude.
     *
     * East is positive Minecraft X.
     *
     * Therefore, when X increases,
     * longitude increases.
     */
    public static double getLongitude(int x, int z) {

        double metersEast =
                (x - START_X) * METERS_PER_BLOCK;

        return START_LON +
                (metersEast / METERS_PER_DEGREE_LONGITUDE);
    }

    /**
     * Convert real-world latitude into Minecraft Z.
     *
     * Higher latitude = farther north = more negative Z.
     */
    public static int latitudeToMinecraftZ(double latitude) {

        double metersNorth =
                (latitude - START_LAT) *
                METERS_PER_DEGREE_LATITUDE;

        return (int) Math.round(
                START_Z -
                (metersNorth / METERS_PER_BLOCK)
        );
    }

    /**
     * Convert real-world longitude into Minecraft X.
     *
     * Higher longitude = farther east = more positive X.
     */
    public static int longitudeToMinecraftX(double longitude) {

        double metersEast =
                (longitude - START_LON) *
                METERS_PER_DEGREE_LONGITUDE;

        return (int) Math.round(
                START_X +
                (metersEast / METERS_PER_BLOCK)
        );
    }

    /**
     * Convert latitude / longitude into Minecraft X/Z.
     *
     * result[0] = X
     * result[1] = Z
     */
    public static int[] earthToMinecraft(
            double latitude,
            double longitude) {

        int x = longitudeToMinecraftX(longitude);
        int z = latitudeToMinecraftZ(latitude);

        return new int[] { x, z };
    }

    /**
     * Convert Minecraft Z directly into latitude.
     */
    public static double minecraftToLatitude(int z) {

        double metersNorth =
                (START_Z - z) * METERS_PER_BLOCK;

        return START_LAT +
                (metersNorth / METERS_PER_DEGREE_LATITUDE);
    }

    /**
     * Convert Minecraft X directly into longitude.
     */
    public static double minecraftToLongitude(int x) {

        double metersEast =
                (x - START_X) * METERS_PER_BLOCK;

        return START_LON +
                (metersEast / METERS_PER_DEGREE_LONGITUDE);
    }
}
