package com.earthbound;

import java.util.HashMap;
import java.util.Map;

public class GuemesElevation {

    /*
     * Guemes Island elevation data.
     *
     * Temporary local cache system.
     *
     * Later this will be replaced with
     * a generated elevation tile from
     * USGS data.
     */

    private static final Map<String, Double> elevationMap =
            new HashMap<>();


    static {

        /*
         * Format:
         *
         * "x,z" = elevation in meters
         *
         * These are starter values only.
         * They prove the terrain pipeline works.
         */

        elevationMap.put("0,0", 5.0);
        elevationMap.put("100,0", 8.0);
        elevationMap.put("200,0", 12.0);

        elevationMap.put("0,100", 10.0);
        elevationMap.put("100,100", 15.0);
        elevationMap.put("200,100", 20.0);

        elevationMap.put("0,200", 15.0);
        elevationMap.put("100,200", 25.0);
        elevationMap.put("200,200", 40.0);

    }


    public static double getElevation(
            int x,
            int z
    ) {

        String key =
                x + "," + z;


        /*
         * If we have a cached value,
         * return it.
         */

        if (elevationMap.containsKey(key)) {

            return elevationMap.get(key);

        }


        /*
         * Temporary interpolation/fallback.
         *
         * Prevents empty chunks.
         */

        double distance =
                Math.sqrt(
                        (x * x) +
                        (z * z)
                );


        return Math.max(
                2.0,
                40.0 - (distance / 100.0)
        );

    }
}
