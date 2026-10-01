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
 * - Real Guemes hydrography / coastline
 * - Natural water columns
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
         * ========================================================
         * GENERATE CHUNK
         * ========================================================
         *
         * Process every X/Z column in the chunk.
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
                 * real-world latitude and longitude.
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
                 * TIGERweb hydrography determines whether
                 * this real-world coordinate is water.
                 */

                if (
                        EarthWaterGenerator.isWater(
                                latitude,
                                longitude
                        )
                ) {


                    /*
                     * Generate the water column.
                     *
                     * Latitude and longitude are passed so
                     * EarthWaterGenerator can use the real
                     * USGS terrain elevation for the
                     * underwater floor.
                     */

                    EarthWaterGenerator.generateWaterColumn(
                            chunkData,
                            localX,
                            localZ,
                            latitude,
                            longitude
                    );


                    /*
                     * Water has already been generated for
                     * this column, so do not generate land.
                     */

                    continue;
                }


                /*
                 * =================================================
                 * LAND
                 * =================================================
                 *
                 * Get the smoothed real-world USGS
                 * terrain elevation.
                 */

                int terrainHeight =
                        EarthTerrainGenerator.getTerrainHeight(
                                latitude,
                                longitude
                        );


                /*
                 * Generate the natural land column.
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


        /*
         * Confirm successful completion of the chunk.
         */

        System.out.println(
                "[EarthBound] FINISHED CHUNK: "
                        + chunkX
                        + ", "
                        + chunkZ
        );


        return chunkData;
    }
}
