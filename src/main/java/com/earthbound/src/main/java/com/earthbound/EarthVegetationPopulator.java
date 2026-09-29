package com.earthbound;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.generator.BlockPopulator;


public class EarthVegetationPopulator extends BlockPopulator {


    /*
     * ============================================================
     * EARTHBOUND REGIONAL VEGETATION
     *
     * Guemes Island Phase 1
     *
     * Spruce = Douglas Fir / Cedar
     * Oak    = Maple / Alder
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
                2 + random.nextInt(5);



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



            /*
             * Only place vegetation on land.
             */

            if (ground.getType()
                    != Material.GRASS_BLOCK) {

                continue;

            }



            /*
             * Leave roads and buildings alone.
             */

            Block surface =
                    world.getBlockAt(
                            x,
                            y,
                            z
                    );


            if (surface.getType()
                    != Material.AIR) {

                continue;

            }



            Location location =
                    new Location(
                            world,
                            x,
                            y,
                            z
                    );



            /*
             * Regional tree mix:
             *
             * 70% evergreen
             * 30% mixed woodland
             */

            if (random.nextDouble() < 0.70) {


                world.generateTree(
                        location,
                        TreeType.TALL_REDWOOD
                );


            } else {


                world.generateTree(
                        location,
                        TreeType.TREE
                );

            }



            /*
             * Add small ground vegetation.
             */

            if (random.nextDouble() < 0.35) {


                Block grass =
                        world.getBlockAt(
                                x,
                                y,
                                z
                        );


                if (grass.getType()
                        == Material.AIR) {


                    grass.setType(
                            Material.SHORT_GRASS
                    );

                }

            }

        }

    }

}
