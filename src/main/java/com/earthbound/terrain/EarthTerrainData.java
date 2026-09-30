package com.earthbound.terrain;


/*
 * ============================================================
 * EARTHBOUND TERRAIN DATA
 *
 * Stores elevation raster information.
 *
 * Loaded by:
 * EarthTerrainDownloader
 *
 * Used by:
 * EarthTerrainLoader
 *
 * ============================================================
 */


public class EarthTerrainData {


    private final int width;
    private final int height;

    private final double[][] elevation;



    public EarthTerrainData(
            int width,
            int height
    ) {


        this.width = width;
        this.height = height;


        this.elevation =
                new double[width][height];

    }



    /*
     * Store elevation value
     */

    public void setElevation(
            int x,
            int z,
            double value
    ) {


        if (x < 0 || x >= width) {
            return;
        }


        if (z < 0 || z >= height) {
            return;
        }


        elevation[x][z] = value;

    }



    /*
     * Get elevation value
     */

    public double getElevation(
            int x,
            int z
    ) {


        if (x < 0 || x >= width) {

            return 0;

        }


        if (z < 0 || z >= height) {

            return 0;

        }


        return elevation[x][z];

    }



    public int getWidth() {

        return width;

    }



    public int getHeight() {

        return height;

    }


}
