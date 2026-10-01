package com.earthbound;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;


/*
 * ============================================================
 * EARTHBOUND WORLD GENERATOR
 *
 * REAL GUEMES TERRAIN TEST
 *
 * Generates terrain using the real Guemes Island
 * elevation data already loaded by EarthBound.
 *
 * Water, roads, buildings, vegetation, etc.
 * are intentionally NOT added yet.
 *
 * Chunk logging remains enabled temporarily so
 * we can verify Paper continues generating chunks.
 *
 * ============================================================
 */

public class EarthGenerator extends ChunkGenerator {


    public EarthGenerator() {

        System.out.println(
                "=== EARTHBOUND GENERATOR ACTIVE ==="
        );

        System.out.println(
                "[EarthBound] Real Guemes terrain generator loaded"
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
         * Log every new chunk Paper asks EarthBound
         * to generate.
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
         * Generate all 256 columns in this chunk.
         */

        for (int localX = 0; localX < 16; localX++) {

            for (int localZ = 0; localZ < 16; localZ++) {


                /*
                 * Convert the block's position inside the
                 * chunk into its global Minecraft X/Z.
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
                        EarthCoordinates.getLatitude(worldZ);

                double longitude =
                        EarthCoordinates.getLongitude(worldX);


                /*
                 * Ask the EarthBound terrain system for
                 * the real terrain height at this location.
                 */

                int terrainHeight =
                        EarthTerrainGenerator.getTerrainHeight(
                                latitude,
                                longitude
                        );


                /*
                 * Build the natural terrain column.
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
