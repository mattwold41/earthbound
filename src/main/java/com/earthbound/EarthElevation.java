package com.earthbound;

import java.util.HashMap;
import java.util.Map;

public class EarthElevation {

    /*
     * Stores elevation results.
     *
     * Key = Minecraft X,Z
     * Value = elevation meters
     */
    private static final Map<String, Double> elevationCache =
            new HashMap<>();


    /*
     * Gets elevation for EarthBound terrain.
     */
    public static double getElevation(
            double x,
            double z
    ) {

        int blockX = (int) x;
        int blockZ = (int) z;


        String key =
                blockX + "," + blockZ;


        /*
         * Check cache first.
         */
        if (elevationCache.containsKey(key)) {

            return elevationCache.get(key);

        }


        /*
         * Convert Minecraft position
         * into Earth coordinates.
         */
        double latitude =
                EarthCoordinates.minecraftToLatitude(
                        blockZ
                );


        double longitude =
                EarthCoordinates.minecraftToLongitude(
                        blockX
                );


        double elevationMeters =
                getUSGSElevation(
                        latitude,
                        longitude
                );


        /*
         * Save result.
         */
        elevationCache.put(
                key,
                elevationMeters
        );


        return elevationMeters;
    }



    /*
     * USGS elevation lookup.
     *
     * Temporary until we load
     * real elevation tiles.
     */
    private static double getUSGSElevation(
            double latitude,
            double longitude
    ) {

        /*
         * Temporary test height.
         *
         * Replace later with
         * real USGS tile data.
         */
        return 20.0;
    }



    /*
     * Converts real Earth elevation
     * into Minecraft height.
     */
    public static int getMinecraftHeight(
            double elevationMeters
    ) {

        int seaLevel = 64;

        double verticalScale = 1.5;


        return seaLevel
                + (int) Math.round(
                        elevationMeters
                                / verticalScale
                );
    }

}
