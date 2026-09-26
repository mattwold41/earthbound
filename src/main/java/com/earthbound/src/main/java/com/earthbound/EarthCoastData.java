package com.earthbound;

public class EarthCoastData {


    /*
     * Beach system
     *
     * 0 = normal land
     * 1 = beach
     */


    public static int getBeachLevel(
            double latitude,
            double longitude) {


        /*
         * No beach underwater
         */
        if (EarthWaterData.isWater(
                latitude,
                longitude)) {

            return 0;
        }


        Double elevation =
                EarthTerrainLoader.getGuemesElevation(
                        latitude,
                        longitude
                );


        if (elevation == null) {

            return 0;
        }


        /*
         * Only very low coastal areas
         * become beach.
         *
         * Prevents whole island
         * becoming sand.
         */
        if (elevation <= 1.5) {

            return 1;
        }


        return 0;
    }
}
