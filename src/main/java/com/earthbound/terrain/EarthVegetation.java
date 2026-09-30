package com.earthbound.vegetation;


/*
 * ============================================================
 * EARTHBOUND VEGETATION DATA
 *
 * Regional plant rules.
 *
 * Guemes Island:
 *
 * - Douglas Fir
 * - Cedar
 * - Maple
 * - Alder
 *
 * ============================================================
 */


public class EarthVegetation {


    /*
     * Evergreen tree chance
     */

    public static final double EVERGREEN_CHANCE =
            0.70;



    /*
     * Mixed woodland chance
     */

    public static final double MIXED_TREE_CHANCE =
            0.30;



    /*
     * Tree density
     */

    public static int getTreeDensity() {


        return 2;


    }


}
