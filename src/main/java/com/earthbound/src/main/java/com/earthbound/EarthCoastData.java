package com.earthbound;

public class EarthCoastData {

    /*
     * Coastline distance system.
     * Will later connect to real shoreline data.
     */

    public static boolean isBeach(
            double latitude,
            double longitude) {

        return false;
    }


    public static int getBeachLevel(
            double latitude,
            double longitude) {

        /*
         * 0 = normal land
         * 1 = closest beach
         * 2 = beach area
         * 3 = coastal transition
         */

        return 0;
    }
}
