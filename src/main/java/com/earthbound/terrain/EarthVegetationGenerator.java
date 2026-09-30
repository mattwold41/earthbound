package com.earthbound.vegetation;


/*
 * ============================================================
 * EARTHBOUND VEGETATION GENERATOR
 *
 * Handles vegetation rules.
 *
 * Actual block placement is handled by:
 *
 * EarthVegetationPopulator
 *
 * ============================================================
 */


public class EarthVegetationGenerator {



    /*
     * Determine if a chunk should have trees
     */


    public static boolean shouldGenerateTrees(
            int chunkX,
            int chunkZ
    ) {


        /*
         * Future improvements:
         *
         * - elevation
         * - soil type
         * - biome
         * - climate
         *
         */


        return true;

    }




    /*
     * Get tree amount for an area
     */


    public static int getTreeAmount(
            int chunkX,
            int chunkZ
    ) {


        return EarthVegetation.getTreeDensity();

    }



    /*
     * Evergreen decision
     */


    public static boolean isEvergreen(
            double randomValue
    ) {


        return randomValue
                < EarthVegetation.EVERGREEN_CHANCE;

    }


}
