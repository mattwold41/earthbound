package com.earthbound.terrain;


/*
 * ============================================================
 * EARTHBOUND TERRAIN LOADER
 *
 * Stores the loaded Guemes elevation raster and converts
 * real-world latitude / longitude into raster coordinates.
 *
 * Guemes raster bounds must match EarthTerrainDownloader:
 *
 * West:  -122.70
 * South:   48.47
 * East:  -122.55
 * North:   48.60
 *
 * ============================================================
 */


public class EarthTerrainLoader {


    private static EarthTerrainData terrainData;


    /*
     * Guemes Island geographic bounds.
     *
     * These are taken directly from
     * EarthTerrainDownloader so the downloader and
     * lookup system always use the same area.
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
     * DIRECT RASTER ELEVATION LOOKUP
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
     * REAL-WORLD GUEMES ELEVATION LOOKUP
     *
     * Converts:
     *
     * latitude / longitude
     *
     * into:
     *
     * raster X / raster Z
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
         * If the requested point is outside the Guemes
         * elevation raster, return sea-level elevation.
         *
         * Later, when EarthBound expands beyond Guemes,
         * this can route the request to neighboring
         * elevation tiles.
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
         * ----------------------------------------------------
         * LONGITUDE -> RASTER X
         *
         * WEST  = pixel 0
         * EAST  = pixel width - 1
         * ----------------------------------------------------
         */

        double normalizedX =
                (longitude - WEST)
                        /
                (EAST - WEST);


        int rasterX =
                (int) Math.round(
                        normalizedX
                                *
                        (width - 1)
                );



        /*
         * ----------------------------------------------------
         * LATITUDE -> RASTER Z
         *
         * Image rasters start at the TOP.
         *
         * NORTH = pixel 0
         * SOUTH = pixel height - 1
         *
         * This is intentionally reversed compared with
         * normal mathematical Y coordinates.
         * ----------------------------------------------------
         */

        double normalizedZ =
                (NORTH - latitude)
                        /
                (NORTH - SOUTH);


        int rasterZ =
                (int) Math.round(
                        normalizedZ
                                *
                        (height - 1)
                );



        /*
         * Protect against floating-point rounding at
         * the exact geographic boundaries.
         */

        rasterX =
                clamp(
                        rasterX,
                        0,
                        width - 1
                );


        rasterZ =
                clamp(
                        rasterZ,
                        0,
                        height - 1
                );



        return terrainData.getElevation(
                rasterX,
                rasterZ
        );

    }



    /*
     * ========================================================
     * SMALL UTILITY
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
