package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {

    private static final int ISLAND_SIZE = 160;

    @Override
    public void generateNoise(
            WorldInfo worldInfo,
            Random random,
            int chunkX,
            int chunkZ,
            ChunkData chunkData) {

        int startX = chunkX * 16;
        int startZ = chunkZ * 16;

        for (int x = 0; x < 16; x++) {

            for (int z = 0; z < 16; z++) {

                int worldX = startX + x;
                int worldZ = startZ + z;

                double distance = Math.sqrt(
                        worldX * worldX +
                        worldZ * worldZ
                );


                // Island
                if (distance < ISLAND_SIZE) {

                    int height = 62;


                    // Hills
                    if (distance < 100) {
                        height += (int)((100 - distance) / 10);
                    }


                    // Stone base
                    for (int y = 0; y < height - 5; y++) {

                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.STONE
                        );
                    }


                    // Dirt
                    for (int y = height - 5; y < height; y++) {

                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.DIRT
                        );
                    }


                    // Grass
                    chunkData.setBlock(
                            x,
                            height,
                            z,
                            Material.GRASS_BLOCK
                    );


                    // Beach
                    if (distance > 120) {

                        chunkData.setBlock(
                                x,
                                height,
                                z,
                                Material.SAND
                        );
                    }


                } else {


                    // Ocean
                    for (int y = 0; y < 62; y++) {

                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.WATER
                        );
                    }
                }
            }
        }
    }
}
