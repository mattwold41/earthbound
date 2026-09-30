package com.earthbound;


/*
 * ============================================================
 * EARTHBOUND VEGETATION GENERATOR
 *
 * Handles regional vegetation rules.
 *
 * Actual placement is done by:
 *
 * EarthVegetationPopulator
 *
 * ============================================================
 */


public class EarthVegetationGenerator {



    /*
     * Guemes Island vegetation rules
     *
     * true = evergreen
     * false = mixed woodland
     */


    public static boolean useEvergreen(
            double chance
    ) {


        return chance < 0.70;

    }




    /*
     * Tree density control
     */


    public static int getTreeAmount(
            int chunkX,
            int chunkZ
    ) {


        /*
         * Later this can use:
         *
         * - biome
         * - elevation
         * - land type
         * - climate
         *
         */


        return 2;

    }



}
