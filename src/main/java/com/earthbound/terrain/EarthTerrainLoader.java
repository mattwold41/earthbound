package com.earthbound.terrain;


/*
 * ============================================================
 * EARTHBOUND TERRAIN LOADER
 *
 * Stores the loaded Guemes elevation raster and converts
 * real-world latitude / longitude into raster coordinates.
 *
 * Uses bilinear interpolation between elevation samples
 * to produce smoother terrain.
 *
 * ============================================================
 */


public class EarthTerrainLoader {


    private static EarthTerrainData terrainData;


    /*
     * Guemes Island geographic bounds
     */

    private static final double WEST =
            EarthTerrainDownloader.WEST;

    private static final double SOUTH =
            EarthTerrainDownloader.SOUTH;

    private static final double EAST =
            EarthTerrainDownloader.EAST;

    private static final double NORTH =
            EarthTerrainDownloader.NORTH;



    /*
     * ========================================================
     * LOAD TERRAIN DATA
     * ========================================================
     */

    public static void load(
            EarthTerrainData data
    ) {


        terrainData = data;


        if (terrainData != null) {


            System.out.println(
                    "[EarthBound] Terrain data registered: "
                            + terrainData.getWidth()
                            + "x"
                            + terrainData.getHeight()
            );

        }

    }



    /*
     * ========================================================
     * CHECK WHETHER TERRAIN IS LOADED
     * ========================================================
     */

    public static boolean isLoaded() {


        return terrainData != null;

    }



    /*
     * ========================================================
     * DIRECT RASTER LOOKUP
     * ========================================================
     */

    public static double getElevation(
            int x,
            int z
    ) {


        if (!isLoaded()) {


            return 0;

        }


        return terrainData.getElevation(
                x,
                z
        );

    }



    /*
     * ========================================================
     * GUEMES REAL-WORLD ELEVATION LOOKUP
     *
     * Converts latitude / longitude to a fractional
     * raster position and interpolates between the
     * four surrounding USGS elevation samples.
     *
     * ========================================================
     */

    public static double getGuemesElevation(
            double latitude,
            double longitude
    ) {


        if (!isLoaded()) {


            return 0;

        }



        /*
         * Outside the currently loaded Guemes raster.
         */

        if (
                longitude < WEST
                        ||
                longitude > EAST
                        ||
                latitude < SOUTH
                        ||
                latitude > NORTH
        ) {


            return 0;

        }



        int width =
                terrainData.getWidth();

        int height =
                terrainData.getHeight();



        /*
         * Longitude:
         *
         * WEST -> 0
         * EAST -> width - 1
         */

        double rasterX =
                ((longitude - WEST)
                        /
                        (EAST - WEST))
                        *
                        (width - 1);



        /*
         * Latitude:
         *
         * NORTH -> 0
         * SOUTH -> height - 1
         *
         * Raster images run from top to bottom.
         */

        double rasterZ =
                ((NORTH - latitude)
                        /
                        (NORTH - SOUTH))
                        *
                        (height - 1);



        /*
         * Find the four surrounding raster cells.
         */

        int x0 =
                (int) Math.floor(rasterX);

        int z0 =
                (int) Math.floor(rasterZ);


        int x1 =
                x0 + 1;

        int z1 =
                z0 + 1;



        /*
         * Keep all four samples inside the raster.
         */

        x0 = clamp(
                x0,
                0,
                width - 1
        );

        x1 = clamp(
                x1,
                0,
                width - 1
        );

        z0 = clamp(
                z0,
                0,
                height - 1
        );

        z1 = clamp(
                z1,
                0,
                height - 1
        );



        /*
         * Fractional position between the samples.
         */

        double fractionX =
                rasterX
                        -
                        Math.floor(rasterX);

        double fractionZ =
                rasterZ
                        -
                        Math.floor(rasterZ);



        /*
         * Read the four real USGS elevations.
         *
         * q00 ----- q10
         *  |         |
         *  |         |
         * q01 ----- q11
         */

        double q00 =
                terrainData.getElevation(
                        x0,
                        z0
                );

        double q10 =
                terrainData.getElevation(
                        x1,
                        z0
                );

        double q01 =
                terrainData.getElevation(
                        x0,
                        z1
                );

        double q11 =
                terrainData.getElevation(
                        x1,
                        z1
                );



        /*
         * Interpolate west -> east.
         */

        double northElevation =
                lerp(
                        q00,
                        q10,
                        fractionX
                );


        double southElevation =
                lerp(
                        q01,
                        q11,
                        fractionX
                );



        /*
         * Interpolate north -> south.
         */

        return lerp(
                northElevation,
                southElevation,
                fractionZ
        );

    }



    /*
     * ========================================================
     * LINEAR INTERPOLATION
     * ========================================================
     */

    private static double lerp(
            double start,
            double end,
            double amount
    ) {


        return start
                +
                ((end - start)
                        * amount);

    }



    /*
     * ========================================================
     * INTEGER CLAMP
     * ========================================================
     */

    private static int clamp(
            int value,
            int minimum,
            int maximum
    ) {


        if (value < minimum) {


            return minimum;

        }


        if (value > maximum) {


            return maximum;

        }


        return value;

    }


}
