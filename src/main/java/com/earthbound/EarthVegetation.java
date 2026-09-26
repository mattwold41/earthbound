package com.earthbound;

import org.bukkit.Material;

import java.util.Random;

public class EarthVegetation {


    public static Material getVegetation(
            Random random,
            int height,
            boolean road,
            boolean beach) {


        /*
         * No plants on roads
         */
        if (road) {
            return Material.AIR;
        }


        /*
         * No plants on beaches
         */
        if (beach) {
            return Material.AIR;
        }


        /*
         * Only above sea level
         */
        if (height < 68) {
            return Material.AIR;
        }



        int chance =
                random.nextInt(100);



        /*
         * Pacific Northwest forest floor
         */
        if (chance < 12) {

            return Material.FERN;

        }


        if (chance < 25) {

            return Material.TALL_GRASS;

        }


        /*
         * Tree marker
         * Full trees will be added
         * in the next decorator step.
         */
        if (chance == 99) {

            return Material.SPRUCE_SAPLING;

        }


        return Material.AIR;
    }
}
