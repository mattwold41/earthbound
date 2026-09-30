package com.earthbound.terrain;


/*
 * ============================================================
 * EARTHBOUND ELEVATION CONVERTER
 *
 * Converts real world elevation meters
 * into Minecraft block heights.
 *
 * Also provides coordinate-based elevation lookup
 * for older EarthBound systems.
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
     * 1.0 = full elevation
     * 0.5 = reduced elevation
     *
     * Adjustable later for:
     *
     * 1:1 Earth
     * 1:2 Earth
     *
     */

    private static final double ELEVATION_SCALE = 1.0;




    /*
     * Convert real elevation meters
     * into Minecraft Y height
     */

    public static int getMinecraftHeight(
            double elevationMeters
    ) {


        return SEA_LEVEL
                +
                (int) Math.round(
                        elevationMeters
                                * ELEVATION_SCALE
                );

    }




    /*
     * Convert Minecraft height
     * back into real elevation meters
     */

    public static double getRealElevation(
            int minecraftHeight
    ) {


        return
                (minecraftHeight - SEA_LEVEL)
                        /
                        ELEVATION_SCALE;

    }




    /*
     * Get real elevation from coordinates
     *
     * Used by:
     *
     * EarthCommand
     * EarthLocation
     *
     */

    public static double getElevation(
            double latitude,
            double longitude
    ) {


        return EarthTerrainLoader.getGuemesElevation(
                latitude,
                longitude
        );

    }




}
