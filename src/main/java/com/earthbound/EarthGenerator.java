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
 * Uses EarthBound's real Guemes elevation data.
 *
 * Water, roads, buildings, vegetation, and other
 * systems are intentionally disabled for this test.
 *
 * Chunk logging remains enabled so we can confirm
 * that new chunks continue generating correctly.
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
         * Generate every block column in the chunk.
         */

        for (int localX = 0; localX < 16; localX++) {

            for (int localZ = 0; localZ < 16; localZ++) {


                /*
                 * Convert local chunk coordinates into
                 * global Minecraft coordinates.
                 */

                int worldX =
                        (chunkX * 16) + localX;

                int worldZ =
                        (chunkZ * 16) + localZ;


                /*
                 * Convert Minecraft coordinates into
                 * real-world latitude / longitude.
                 *
                 * EarthCoordinates expects BOTH X and Z.
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
                 * Get the terrain height from the
                 * Guemes elevation system.
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
