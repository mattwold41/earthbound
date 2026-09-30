package com.earthbound;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;


/*
 * ============================================================
 * EARTHBOUND WORLD GENERATOR
 *
 * CHUNK DEBUG VERSION
 *
 * This version logs every chunk that Paper asks
 * EarthBound to generate.
 *
 * Terrain is temporarily flat at Y=70.
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
                "[EarthBound] Chunk debug generator loaded"
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
         * IMPORTANT DEBUG MESSAGE
         *
         * If Paper asks EarthBound to create a new chunk,
         * this message MUST appear in the console.
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
         * Generate simple flat terrain.
         */

        for (int x = 0; x < 16; x++) {

            for (int z = 0; z < 16; z++) {


                EarthTerrainGenerator
                        .generateNaturalLandColumn(
                                chunkData,
                                x,
                                z,
                                TEST_TERRAIN_HEIGHT
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
