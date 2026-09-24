package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {

    // Guemes Ferry Terminal reference
    private static final int START_X = -624;
    private static final int START_Z = -544;

    // 1 block = 2 meters
    // Approximate Guemes area
    private static final int ISLAND_RADIUS = 2000;

    private static final int SEA_LEVEL = 63;


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
                        Math.pow(worldX - START_X, 2) +
                        Math.pow(worldZ - START_Z, 2)
                );


                // Coastline variation
                double coastline =
                        Math.sin(worldX * 0.003) * 150 +
                        Math.cos(worldZ * 0.004) * 120 +
                        Math.sin((worldX + worldZ) * 0.002) * 100;


                double islandEdge =
                        ISLAND_RADIUS + coastline;


                // Default ocean floor
                for (int y = 0; y < SEA_LEVEL - 5; y++) {

                    chunkData.setBlock(
                            x,
                            y,
                            z,
                            Material.STONE
                    );
                }


                // Ocean
                for (int y = SEA_LEVEL - 5; y <= SEA_LEVEL; y++) {

                    chunkData.setBlock(
                            x,
                            y,
                            z,
                            Material.WATER
                    );
                }


                // Land generation
                if (distance < islandEdge) {


                    int height = SEA_LEVEL;


                    // Gentle hills inland
                    if (distance < 1400) {

                        height +=
                                (int)((1400 - distance) / 40);
                    }


                    // Small terrain variation
                    height += random.nextInt(3);


                    // Replace ocean with land
                    for (int y = 0; y < height - 5; y++) {

                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.STONE
                        );
                    }


                    // Soil layer
                    for (
                            int y = height - 5;
                            y < height;
                            y++) {

                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.DIRT
                        );
                    }


                    // Beach edge
                    if (distance > islandEdge - 50) {

                        chunkData.setBlock(
                                x,
                                height,
                                z,
                                Material.SAND
                        );

                    } else {

                        chunkData.setBlock(
                                x,
                                height,
                                z,
                                Material.GRASS_BLOCK
                        );
                    }
                }
            }
        }
    }
}
