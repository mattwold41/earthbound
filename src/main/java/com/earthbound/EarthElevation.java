package com.earthbound;

import java.util.HashMap;
import java.util.Map;

public class EarthElevation {

    /*
     * Simple elevation cache.
     *
     * Later this will hold real USGS elevation tiles.
     * For now it prevents repeated calculations.
     */
    private static final Map<String, Double> elevationCache =
            new HashMap<>();


    /*
     * Main terrain elevation method.
     *
     * All terrain generators call this.
     */
    public static double getElevation(
            double x,
            double z
    ) {

        String key =
                ((int)x) + ":" + ((int)z);


        if (elevationCache.containsKey(key)) {

            return elevationCache.get(key);

        }


        double latitude =
                EarthCoordinates.minecraftToLatitude(
                        (int) z
                );


        double longitude =
                EarthCoordinates.minecraftToLongitude(
                        (int) x
                );


        /*
         * Debug removed.
         *
         * Do NOT print every block.
         */


        double elevation =
                getUSGSElevation(
                        latitude,
                        longitude
                );


        elevationCache.put(
                key,
                elevation
        );


        return elevation;
    }



    /*
     * Temporary elevation source.
     *
     * Replace later with
     * Guemes Island elevation tiles.
     */
    private static double getUSGSElevation(
            double latitude,
            double longitude
    ) {


        /*
         * Current test terrain.
         *
         * Sea level.
         */
        return 0.0;

    }



    /*
     * Converts meters to Minecraft height.
     */
    public static int getMinecraftHeight(
            double elevationMeters
    ) {


        int seaLevel = 64;


        double verticalScale = 1.5;


        return seaLevel
                + (int)Math.round(
                        elevationMeters
                                / verticalScale
                );

    }



    /*
     * Clears cache if needed.
     */
    public static void clearCache() {

        elevationCache.clear();

    }

}
