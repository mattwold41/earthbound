package com.earthbound;

public class EarthCoastData {


    /*
     * Coastline beach system
     *
     * Returns:
     *
     * 0 = normal land
     * 1 = beach
     * 2 = wider beach area
     * 3 = coastal transition
     *
     */


    public static int getBeachLevel(
            double latitude,
            double longitude) {


        /*
         * Never create beach underwater
         */
        if (EarthWaterData.isWater(
                latitude,
                longitude)) {

            return 0;
        }


        /*
         * Temporary Guemes coastline system.
         *
         * This will later be replaced
         * with real shoreline distance data.
         *
         * For now we use elevation
         * and proximity checks.
         */


        Double elevation =
                EarthTerrainLoader.getGuemesElevation(
                        latitude,
                        longitude
                );


        if (elevation == null) {

            return 0;
        }



        /*
         * Low coastal areas become beach.
         *
         * Higher areas remain grass.
         */
        if (elevation <= 3) {

            return 1;

        } else if (elevation <= 6) {

            return 2;

        } else if (elevation <= 10) {

            return 3;
        }


        return 0;
    }
}
