package com.earthbound;

import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.World;

import java.util.Random;

public class EarthTreeGenerator {


    public static void generateTree(
            World world,
            int x,
            int y,
            int z,
            Random random) {


        /*
         * Random tree selection
         *
         * Washington style:
         * mostly spruce,
         * some oak mixed in.
         */

        int type =
                random.nextInt(100);


        Location location =
                new Location(
                        world,
                        x,
                        y,
                        z
                );


        if (type < 80) {


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
