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
     * 1 Minecraft block = 2 real-world meters.
     * EarthBound scale = 1:2.
     *
     * Changing this value later allows
     * EarthBound geographic locations
     * to be recalculated for another scale.
     */
    private static final double METERS_PER_BLOCK =
            2.0;


    private EarthCoordinates() {
    }


    /*
     * Minecraft X/Z -> latitude.
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
     * Minecraft X/Z -> longitude.
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
                getMetersPerLongitudeDegree();

        double longitudeChange =
                metersEast
                        / metersPerLongitudeDegree;

        return START_LON
                + longitudeChange;
    }


    /*
     * Real-world latitude -> Minecraft Z.
     *
     * This is the reverse of getLatitude().
     *
     * Because EarthBound uses real-world
     * coordinates for buildings, this lets
     * structures automatically move to the
     * correct Minecraft Z coordinate if the
     * horizontal scale changes later.
     */
    public static int latitudeToMinecraftZ(
            double latitude) {

        double latitudeDifference =
                latitude - START_LAT;

        double metersNorth =
                latitudeDifference
                        * 111320.0;

        double blocksNorth =
                metersNorth
                        / METERS_PER_BLOCK;

        return START_Z
                + (int) Math.round(
                        blocksNorth
                );
    }


    /*
     * Real-world longitude -> Minecraft X.
     *
     * This is the reverse of getLongitude().
     *
     * EarthBound orientation:
     *
     * East = X decreases
     * West = X increases
     */
    public static int longitudeToMinecraftX(
            double longitude) {

        double longitudeDifference =
                longitude - START_LON;

        double metersEast =
                longitudeDifference
                        * getMetersPerLongitudeDegree();

        double blocksEast =
                metersEast
                        / METERS_PER_BLOCK;

        return START_X
                - (int) Math.round(
                        blocksEast
                );
    }


    /*
     * Convert a complete real-world location
     * into Minecraft X/Z coordinates.
     *
     * Result:
     *
     * [0] = Minecraft X
     * [1] = Minecraft Z
     */
    public static int[] earthToMinecraft(
            double latitude,
            double longitude) {

        int minecraftX =
                longitudeToMinecraftX(
                        longitude
                );

        int minecraftZ =
                latitudeToMinecraftZ(
                        latitude
                );

        return new int[]{
                minecraftX,
                minecraftZ
        };
    }


    /*
     * Approximate number of real-world
     * meters in one degree of longitude
     * at the EarthBound anchor latitude.
     */
    private static double
    getMetersPerLongitudeDegree() {

        return 111320.0
                * Math.cos(
                        Math.toRadians(
                                START_LAT
                        )
                );
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
