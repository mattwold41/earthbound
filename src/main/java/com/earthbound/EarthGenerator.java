package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {

    // Guemes Ferry Terminal spawn reference
    private static final int START_X = -624;
    private static final int START_Z = -544;

    // Approximate Guemes Island size
    // 1 block = 2 meters
    private static final int ISLAND_RADIUS = 2000;


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


                // Distance from ferry terminal
                double distance = Math.sqrt(
                        Math.pow(worldX - START_X, 2) +
                        Math.pow(worldZ - START_Z, 2)
                );


                // Natural coastline variation
                double coastline =
                        Math.sin(worldX * 0.003) * 150 +
                        Math.cos(worldZ * 0.004) * 120 +
                        Math.sin((worldX + worldZ) * 0.002) * 100;


                double islandEdge =
                        ISLAND_RADIUS + coastline;


                if (distance < islandEdge) {


                    // Base elevation
                    int height = 62;


                    // Interior hills
                    if (distance < 1200) {

                        height +=
                                (int)((1200 - distance) / 30);
                    }


                    // Natural variation
                    height += random.nextInt(3);


                    // Stone foundation
                    for (int y = 0; y < height - 6; y++) {

                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.STONE
                        );
                    }


                    // Soil
                    for (
                            int y = height - 6;
                            y < height;
                            y++) {

                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.DIRT
                        );
                    }


                    // Beaches near shoreline
                    if (distance > islandEdge - 40) {

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
