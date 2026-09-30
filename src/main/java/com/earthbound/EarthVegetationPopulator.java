package com.earthbound;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.Chunk;


public class EarthVegetationPopulator extends BlockPopulator {


    /*
     * ============================================================
     * EARTHBOUND REGIONAL VEGETATION
     *
     * Guemes Island / Pacific Northwest
     *
     * Evergreen:
     * - Douglas Fir
     * - Cedar style
     *
     * Mixed forest:
     * - Maple
     * - Alder style
     *
     * ============================================================
     */


    @Override
    public void populate(
            World world,
            Random random,
            Chunk chunk
    ) {


        int trees =
                2 + random.nextInt(4);



        for (int i = 0; i < trees; i++) {


            int x =
                    chunk.getX() * 16
                    + random.nextInt(16);


            int z =
                    chunk.getZ() * 16
                    + random.nextInt(16);



            int y =
                    world.getHighestBlockYAt(
                            x,
                            z
                    );



            Block ground =
                    world.getBlockAt(
                            x,
                            y - 1,
                            z
                    );



            if (ground.getType()
                    != Material.GRASS_BLOCK) {

                continue;

            }



            Location treeLocation =
                    new Location(
                            world,
                            x,
                            y,
                            z
                    );



            /*
             * Pacific Northwest forest mix
             *
             * 70% evergreen
             * 30% mixed trees
             */

            if (random.nextDouble() < 0.70) {


                world.generateTree(
                        treeLocation,
                        TreeType.REDWOOD
                );


            } else {


                world.generateTree(
                        treeLocation,
                        TreeType.TREE
                );


            }

        }

    }

}
