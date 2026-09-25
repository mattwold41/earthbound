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

        for (int x = 0; x < 16; x++) {

            for (int z = 0; z < 16; z++) {

                int worldX =
                        chunkX * 16 + x;

                int worldZ =
                        chunkZ * 16 + z;

                /*
                 * Convert Minecraft coordinates
                 * using the EarthBound coordinate system.
                 *
                 * Guemes Ferry Terminal:
                 * Minecraft X = -624
                 * Minecraft Z = -544
                 *
                 * Latitude  = 48.5228603
                 * Longitude = -122.624694
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
                 * IMPORTANT:
                 *
                 * This ONLY reads the Guemes raster
                 * already stored in memory.
                 *
                 * It NEVER makes an Internet request.
                 */
                Double elevation =
                        EarthTerrainLoader.getGuemesElevation(
                                latitude,
                                longitude
                        );

                int height;

                if (elevation != null
                        && Double.isFinite(elevation)) {

                    height =
                            EarthElevation.getMinecraftHeight(
                                    elevation
                            );

                } else {

                    /*
                     * Safe temporary fallback.
                     *
                     * If the Guemes raster has not loaded yet,
                     * or we're outside the Guemes tile,
                     * generate flat terrain instead of
                     * contacting USGS.
                     */
                    height = SEA_LEVEL;
                }

                height =
                        Math.max(
                                worldInfo.getMinHeight(),
                                Math.min(
                                        worldInfo.getMaxHeight() - 1,
                                        height
                                )
                        );

                /*
                 * Basic terrain materials for now.
                 *
                 * We'll improve coastlines, water,
                 * beaches, soil and vegetation later.
                 */
                for (int y = worldInfo.getMinHeight();
                     y <= height;
                     y++) {

                    if (y == height) {

                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.GRASS_BLOCK
                        );

                    } else if (y >= height - 3) {

                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.DIRT
                        );

                    } else {

                        chunkData.setBlock(
                                x,
                                y,
                                z,
                                Material.STONE
                        );
                    }
                }
            }
        }
    }
}
