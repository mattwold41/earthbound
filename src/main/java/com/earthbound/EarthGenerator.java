package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {


    private static final int SEA_LEVEL = 63;

    private boolean announced = false;



    @Override
    public void generateNoise(
            WorldInfo worldInfo,
            Random random,
            int chunkX,
            int chunkZ,
            ChunkData chunkData) {


        /*
         * GENERATOR TEST MESSAGE
         *
         * If you see this in console,
         * Paper is using EarthGenerator.
         */
        if (!announced) {

            System.out.println(
                    "=== EARTHBOUND GENERATOR ACTIVE ==="
            );

            announced = true;
        }



        for (int x = 0; x < 16; x++) {


            for (int z = 0; z < 16; z++) {


                /*
                 * TEMPORARY TEST TERRAIN
                 *
                 * If this works, the entire
                 * generator pipeline works.
                 */
                int height = 70;



                for (int y = worldInfo.getMinHeight();
                     y <= height;
                     y++) {


                    if (y == height) {

                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.GRASS_BLOCK
                        );

                    } else if (y >= height - 3) {

                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.DIRT
                        );

                    } else {

                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.STONE
                        );
                    }
                }
            }
        }
    }
}
