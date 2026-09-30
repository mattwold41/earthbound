package com.earthbound;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;


/*
 * ============================================================
 * EARTHBOUND WORLD GENERATOR
 *
 * DIAGNOSTIC VERSION
 *
 * Purpose:
 * Test whether Paper can generate new chunks normally
 * without calling the elevation raster.
 *
 * If chunks load with this version, the problem is in the
 * elevation lookup/data path rather than the Paper generator.
 *
 * ============================================================
 */

public class EarthGenerator extends ChunkGenerator {


    private static final int TEST_TERRAIN_HEIGHT = 70;


    public EarthGenerator() {

        System.out.println(
                "=== EARTHBOUND GENERATOR ACTIVE ==="
        );

        System.out.println(
                "[EarthBound] Diagnostic terrain generator loaded"
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

        ChunkData chunkData =
                createChunkData(world);


        /*
         * Convert the chunk position into
         * its first world block position.
         */

        int startX =
                chunkX << 4;

        int startZ =
                chunkZ << 4;


        /*
         * Generate all 256 columns
         * inside this chunk.
         */

        for (int x = 0; x < 16; x++) {

            for (int z = 0; z < 16; z++) {


                int worldX =
                        startX + x;

                int worldZ =
                        startZ + z;


                /*
                 * Verify that our coordinate conversion
                 * can still be called successfully.
                 *
                 * We are intentionally NOT using the
                 * elevation raster during this test.
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
                 * Keep these values referenced so this
                 * diagnostic still exercises the
                 * coordinate conversion.
                 */

                if (Double.isNaN(latitude)
                        || Double.isNaN(longitude)) {

                    continue;
                }


                /*
                 * Temporary flat terrain.
                 *
                 * If new chunks now load, we know
                 * EarthTerrainGenerator /
                 * EarthTerrainLoader is where we
                 * need to investigate next.
                 */

                EarthTerrainGenerator
                        .generateNaturalLandColumn(
                                chunkData,
                                x,
                                z,
                                TEST_TERRAIN_HEIGHT
                        );
            }
        }


        return chunkData;
    }
}
