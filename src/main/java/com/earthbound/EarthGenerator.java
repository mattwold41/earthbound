package com.earthbound;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {


    public EarthGenerator() {

        EarthTerrainLoader.loadGuemesTerrainTile();

    }


    @Override
    public void generateNoise(
            WorldInfo worldInfo,
            Random random,
            int chunkX,
            int chunkZ,
            ChunkData data) {


        for (int x = 0; x < 16; x++) {

            for (int z = 0; z < 16; z++) {


                int blockX = chunkX * 16 + x;
                int blockZ = chunkZ * 16 + z;


                double elevation =
                        EarthTerrainLoader.getGuemesElevation(
                                blockX,
                                blockZ
                        );


                int height = (int)elevation;


                for (int y = 0; y <= height; y++) {

                    if (y == height) {

                        data.setBlock(
                                x,
                                y,
                                z,
                                org.bukkit.Material.GRASS_BLOCK
                        );

                    } else {

                        data.setBlock(
                                x,
                                y,
                                z,
                                org.bukkit.Material.DIRT
                        );

                    }
                }
            }
        }
    }
}
