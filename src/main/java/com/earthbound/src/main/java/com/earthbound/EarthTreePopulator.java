package com.earthbound;

import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.World;

import java.util.Random;

public class EarthTreePopulator {


    public static void placeTree(
            World world,
            int x,
            int y,
            int z,
            Random random) {


        Location location =
                new Location(
                        world,
                        x,
                        y,
                        z
                );


        int chance =
                random.nextInt(100);


        /*
         * Pacific Northwest forest:
         *
         * Mostly evergreen
         * Some mixed trees
         */


        if (chance < 85) {


            world.generateTree(
                    location,
                    TreeType.REDWOOD
            );


        } else {


            world.generateTree(
                    location,
                    TreeType.TREE
            );
        }
    }
}
