package com.earthbound;

public class EarthElevation {


    /*
     * Gets elevation for EarthBound terrain.
     *
     * This is the central elevation system.
     * All terrain generators should use this.
     */
    public static double getElevation(
            double x,
            double z
    ) {


        // Convert Minecraft position
        // into Earth coordinates

        double latitude =
                EarthCoordinates.minecraftToLatitude(
                        (int) z
                );


        double longitude =
                EarthCoordinates.minecraftToLongitude(
                        (int) x
                );


        /*
         * Temporary fallback:
         *
         * If USGS lookup is unavailable,
         * keep terrain near sea level.
         *
         * Later this can be replaced
         * with cached elevation tiles.
         */

        double elevationMeters =
                getUSGSElevation(
                        latitude,
                        longitude
                );


        return elevationMeters;
    }



    /*
     * USGS elevation lookup.
     *
     * Placeholder connection point.
     *
     * Later this will use cached
     * Guemes Island elevation data
     * instead of requesting every block.
     */
    private static double getUSGSElevation(
            double latitude,
            double longitude
    ) {


        /*
         * Current safe test value.
         *
         * Prevents terrain generation
         * from crashing while the
         * elevation database is built.
         */

        return 0.0;
    }



    /*
     * Converts real Earth elevation
     * into Minecraft terrain height.
     *
     * Minecraft sea level = 63/64
     */
    public static int getMinecraftHeight(
            double elevationMeters
    ) {


        int seaLevel = 64;


        /*
         * Vertical compression.
         *
         * Real mountains are too tall
         * if converted 1:1.
         *
         * Example:
         * Mount Baker:
         * 3286m / 1.5 ≈ 2190 blocks
         *
         * This keeps terrain playable.
         */
        double verticalScale = 1.5;


        return seaLevel
                + (int) Math.round(
                        elevationMeters
                                / verticalScale
                );
    }

}
