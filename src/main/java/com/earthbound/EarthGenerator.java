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
         * No vegetation on roads
         */
        if (road) {
            return Material.AIR;
        }


        /*
         * No vegetation on beaches
         */
        if (beach) {
            return Material.AIR;
        }


        /*
         * Keep vegetation above coastline
         */
        if (height <= 68) {
            return Material.AIR;
        }


        int chance =
                random.nextInt(100);



        /*
         * Ferns
         */
        if (chance < 15) {

            return Material.FERN;

        }


        /*
         * Grass
         */
        if (chance < 30) {

            return Material.TALL_GRASS;

        }


        /*
         * Temporary tree marker
         * Full tree generation comes next.
         */
        if (chance == 99) {

            return Material.SPRUCE_SAPLING;

        }


        return Material.AIR;
    }
}
