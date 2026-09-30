package com.earthbound.terrain;


/*
 * ============================================================
 * EARTHBOUND ELEVATION CONVERTER
 *
 * Converts real world elevation meters
 * into Minecraft block heights.
 *
 * ============================================================
 */


public class EarthElevation {


    /*
     * Minecraft sea level
     */

    private static final int SEA_LEVEL = 63;



    /*
     * Vertical scale.
     *
     * 1.0 = real elevation
     * 0.5 = half scale
     *
     * We can adjust this later
     * for 1:1 or 1:2 Earth.
     */

    private static final double ELEVATION_SCALE = 1.0;



    /*
     * Convert meters to Minecraft Y level
     */

    public static int getMinecraftHeight(
            double elevationMeters
    ) {


        int height =
                SEA_LEVEL
                +
                (int) Math.round(
                        elevationMeters
                        * ELEVATION_SCALE
                );


        return height;

    }



    /*
     * Convert Minecraft height back to meters
     */

    public static double getRealElevation(
            int minecraftHeight
    ) {


        return
                (minecraftHeight - SEA_LEVEL)
                /
                ELEVATION_SCALE;

    }



}
