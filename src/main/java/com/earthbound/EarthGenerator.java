package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {


    @Override
    public ChunkData generateChunkData(
            World world,
            Random random,
            int chunkX,
            int chunkZ,
            BiomeGrid biome
    ) {


        ChunkData chunk =
                createChunkData(world);


        for (int x = 0; x < 16; x++) {

            for (int z = 0; z < 16; z++) {


                int blockX =
                        chunkX * 16 + x;

                int blockZ =
                        chunkZ * 16 + z;


                double elevation =
                        EarthTerrainLoader.getGuemesElevation(
                                blockX,
                                blockZ
                        );


                int height =
                        (int)elevation;


                for (int y = 0; y <= height; y++) {


                    if (y == height) {

                        chunk.setBlock(
                                x,
                                y,
                                z,
                                Material.GRASS_BLOCK
                        );

                    } else if (y > height - 4) {

                        chunk.setBlock(
                                x,
                                y,
                                z,
                                Material.DIRT
                        );

                    } else {

                        chunk.setBlock(
                                x,
                                y,
                                z,
                                Material.STONE
                        );
                    }
                }
            }
        }


        return chunk;
    }
}
