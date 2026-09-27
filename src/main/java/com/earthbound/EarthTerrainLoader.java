package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {

    private static final int SEA_LEVEL = 63;

    @Override
    public void generateNoise(
            WorldInfo worldInfo,
            Random random,
            int chunkX,
            int chunkZ,
            ChunkData chunkData) {

        if (!EarthTerrainLoader.isGuemesTerrainLoaded()) {
            EarthTerrainLoader.loadGuemesTerrainTile();
        }

        for (int x = 0; x < 16; x++) {

            for (int z = 0; z < 16; z++) {

                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;

                double[] coords =
                        EarthCoordinates.minecraftToLatLon(
                                worldX,
                                worldZ
                        );

                Double elevation =
                        EarthTerrainLoader.getGuemesElevation(
                                coords[0],
                                coords[1]
                        );


                int height = SEA_LEVEL;


                if (elevation != null) {

                    height =
                        EarthElevation.getMinecraftHeight(
                                elevation
                        );

                }


                for (int y = 0; y <= height; y++) {

                    if (y < height - 4) {

                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.STONE
                        );

                    } else {

                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.GRASS_BLOCK
                        );
                    }
                }


                if (height < SEA_LEVEL) {

                    for (
                        int y = height + 1;
                        y <= SEA_LEVEL;
                        y++
                    ) {

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
