package com.earthbound;

import org.bukkit.Material;

import java.util.Random;

public class EarthVegetation {


    /*
     * Returns the type of vegetation
     * that belongs at this location.
     *
     * Roads and beaches stay clear.
     */


    public static Material getVegetation(
            Random random,
            int height,
            boolean road,
            boolean beach) {


        /*
         * Keep roads clear
         */
        if (road) {

            return Material.AIR;
        }


        /*
         * Keep beaches open
         */
        if (beach) {

            return Material.AIR;
        }


        /*
         * Too close to sea level
         * for forest
         */
        if (height < 70) {

            return Material.AIR;
        }



        int chance =
                random.nextInt(100);



        /*
         * Forest floor plants
         */
        if (chance < 15) {

            return Material.FERN;
        }



        if (chance < 30) {

            return Material.TALL_GRASS;
        }



        /*
         * Tree marker removed.
         *
         * Trees will now be handled
         * by EarthTreeGenerator.
         */
        return Material.AIR;
    }
}
