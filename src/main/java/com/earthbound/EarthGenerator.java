package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {

    private static final int ISLAND_RADIUS = 180;

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

                // Natural coastline variation
                double variation =
                        Math.sin(worldX * 0.05) * 12 +
                        Math.cos(worldZ * 0.04) * 10 +
                        Math.sin((worldX + worldZ) * 0.03) * 8;

                double islandEdge = ISLAND_RADIUS + variation;

                if (distance < islandEdge) {

                    // Base island height
                    int height = 62;

                    // Raise center of island
                    if (distance < 120) {
                        height += (int)((120 - distance) / 8);
                    }

                    // More hills
                    height += random.nextInt(3);

                    // Deep stone base
                    for (int y = 0; y < height - 6; y++) {
                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.STONE
                        );
                    }

                    // Dirt layer
                    for (int y = height - 6; y < height; y++) {
                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.DIRT
                        );
                    }

                    // Beach near shoreline
                    if (distance > islandEdge - 18) {

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
