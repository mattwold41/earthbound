package com.earthbound;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import com.earthbound.water.EarthWaterGenerator;


/*
 * ============================================================
 * EARTHBOUND WORLD GENERATOR
 *
 * GUEMES TERRAIN + WATER
 *
 * Generates:
 *
 * - Real USGS elevation terrain
 * - Smoothed terrain slopes
 * - Real Guemes hydrography / water
 *
 * Roads, buildings, vegetation, and other systems
 * remain disabled until later restoration steps.
 *
 * ============================================================
 */

public class EarthGenerator extends ChunkGenerator {


    public EarthGenerator() {

        System.out.println(
                "=== EARTHBOUND GENERATOR ACTIVE ==="
        );

        System.out.println(
                "[EarthBound] Guemes terrain + water generator loaded"
        );
    }


    @Override
    public ChunkData generateChunkData(
            World world,
            Random random,
            int chunkX,
            int chunkZ,
            BiomeGrid biome
    ) {


        /*
         * Confirm that Paper is asking EarthBound
         * to generate this chunk.
         */

        System.out.println(
                "[EarthBound] GENERATING CHUNK: "
                        + chunkX
                        + ", "
                        + chunkZ
        );


        ChunkData chunkData =
                createChunkData(world);


        /*
         * Generate every block column in this chunk.
         */

        for (int localX = 0; localX < 16; localX++) {

            for (int localZ = 0; localZ < 16; localZ++) {


                /*
                 * Convert chunk-local coordinates into
                 * global Minecraft coordinates.
                 */

                int worldX =
                        (chunkX * 16) + localX;

                int worldZ =
                        (chunkZ * 16) + localZ;


                /*
                 * Convert Minecraft coordinates into
                 * real-world latitude / longitude.
                 */

                double latitude =
                        EarthCoordinates.getLatitude(
                                worldX,
                                worldZ
                        );

                double longitude =
                        EarthCoordinates.getLongitude(
                                worldX,
                                worldZ
                        );


                /*
                 * =================================================
                 * WATER
                 * =================================================
                 *
                 * Check the real Guemes hydrography mask first.
                 *
                 * If this coordinate is mapped as water,
                 * generate a water column instead of land.
                 */

                if (
                        EarthWaterGenerator.isWater(
                                latitude,
                                longitude
                        )
                ) {


                    EarthWaterGenerator.generateWaterColumn(
                            chunkData,
                            localX,
                            localZ
                    );


                    continue;
                }


                /*
                 * =================================================
                 * LAND
                 * =================================================
                 *
                 * Get the smoothed real-world elevation.
                 */

                int terrainHeight =
                        EarthTerrainGenerator.getTerrainHeight(
                                latitude,
                                longitude
                        );


                /*
                 * Generate the natural terrain column.
                 */

                EarthTerrainGenerator
                        .generateNaturalLandColumn(
                                chunkData,
                                localX,
                                localZ,
                                terrainHeight
                        );
            }
        }


        System.out.println(
                "[EarthBound] FINISHED CHUNK: "
                        + chunkX
                        + ", "
                        + chunkZ
        );


        return chunkData;
    }
}
