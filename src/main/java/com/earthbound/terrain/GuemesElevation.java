package com.earthbound.terrain;


/*
 * ============================================================
 * EARTHBOUND GUEMES ELEVATION
 *
 * Stores Guemes Island elevation settings.
 *
 * ============================================================
 */


public class GuemesElevation {


    /*
     * Guemes Island bounds
     */

    public static final double WEST =
            -122.70;

    public static final double EAST =
            -122.55;


    public static final double SOUTH =
            48.47;

    public static final double NORTH =
            48.60;



    /*
     * Raster size
     */

    public static final int RASTER_WIDTH =
            512;


    public static final int RASTER_HEIGHT =
            512;



    /*
     * Convert latitude to raster X
     */

    public static int latitudeToX(
            double latitude
    ) {


        double percent =
                (latitude - SOUTH)
                /
                (NORTH - SOUTH);


        return (int)
                Math.round(
                        percent
                        * (RASTER_WIDTH - 1)
                );

    }



    /*
     * Convert longitude to raster Z
     */

    public static int longitudeToZ(
            double longitude
    ) {


        double percent =
                (longitude - WEST)
                /
                (EAST - WEST);


        return (int)
                Math.round(
                        percent
                        * (RASTER_HEIGHT - 1)
                );

    }


}
