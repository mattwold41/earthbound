package com.earthbound.terrain;


/*
 * ============================================================
 * EARTHBOUND TERRAIN DOWNLOADER
 *
 * Handles terrain data loading.
 *
 * Future:
 * - USGS elevation downloads
 * - DEM files
 * - raster processing
 *
 * ============================================================
 */


public class EarthTerrainDownloader {


    private static boolean loaded = false;



    /*
     * Load Guemes Island terrain
     */

    public static void loadGuemesIsland() {


        /*
         * Temporary starter terrain.
         *
         * Later this will:
         *
         * 1. Download USGS DEM data
         * 2. Decode raster
         * 3. Store elevation values
         * 4. Send data to EarthTerrainLoader
         *
         */


        EarthTerrainData data =
                new EarthTerrainData(
                        512,
                        512
                );



        /*
         * Temporary elevation fill.
         *
         * This keeps testing working
         * until the USGS loader is connected.
         */

        for (
                int x = 0;
                x < 512;
                x++
        ) {


            for (
                    int z = 0;
                    z < 512;
                    z++
            ) {


                data.setElevation(
                        x,
                        z,
                        30
                );

            }

        }



        EarthTerrainLoader.load(
                data
        );


        loaded = true;



        System.out.println(
                "[EarthBound] Guemes terrain loaded"
        );


    }




    public static boolean isLoaded() {


        return loaded;


    }


}
