package com.earthbound;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.util.Random;

public class EarthGenerator extends ChunkGenerator {

    /*
     * ============================================================
     * EARTHBOUND ROAD SETTINGS
     * ============================================================
     *
     * FINAL ROAD STANDARD
     *
     * LOCAL:
     * Stone Brick | Polished Blackstone Bricks x5 | Stone Brick
     *
     * HIGHWAY:
     * Stone Brick | Polished Blackstone Bricks x8 | Stone Brick
     *
     * FREEWAY:
     * Stone Brick | Polished Blackstone Bricks x10 | Stone Brick
     *
     * The widths stored in EarthRoadData are the driving-surface
     * widths only.
     *
     * One additional block is used on each side for the
     * Stone Brick border.
     */

    private static final int ROAD_SMOOTH_RADIUS = 3;

    /*
     * One-block border outside each driving surface.
     */
    private static final double ROAD_BORDER_WIDTH = 1.0;


    /*
     * Main road materials.
     */

    private static final Material ROAD_SURFACE =
            Material.POLISHED_BLACKSTONE_BRICKS;

    private static final Material ROAD_SURFACE_SLAB =
            Material.POLISHED_BLACKSTONE_BRICK_SLAB;

    private static final Material ROAD_BORDER =
            Material.STONE_BRICKS;

    private static final Material ROAD_BORDER_SLAB =
            Material.STONE_BRICK_SLAB;


    /*
     * ============================================================
     * ROAD SLOPE SETTINGS
     * ============================================================
     *
     * Roads use half-block vertical transitions.
     *
     * This is especially useful for Bedrock / Xbox players:
     * instead of repeatedly encountering full vertical block
     * faces, the road can rise and fall in half-block increments.
     *
     * A larger smoothing radius also prevents the road from
     * copying every tiny terrain bump.
     */

    private static final double HALF_BLOCK =
            0.5;


    /*
     * ============================================================
     * GENERAL STORE TERRAIN SETTINGS
     * ============================================================
     */

    private static final double STORE_BLEND_DISTANCE =
            32.0;

    private static final double STORE_SLOPE_RUN =
            4.0;


    public EarthGenerator() {

        System.out.println(
                "=== EARTHBOUND REAL TERRAIN GENERATOR ACTIVE ==="
        );

        System.out.println(
                "[EarthBound] Road design: "
                        + "Polished Blackstone Bricks + Stone Brick borders"
        );

        System.out.println(
                "[EarthBound] Road widths: local="
                        + EarthRoadData.LOCAL_WIDTH
                        + ", highway="
                        + EarthRoadData.HIGHWAY_WIDTH
                        + ", freeway="
                        + EarthRoadData.FREEWAY_WIDTH
        );

        System.out.println(
                "[EarthBound] Half-block road slope transitions enabled."
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


        int chunkMinX =
                chunkX << 4;

        int chunkMinZ =
                chunkZ << 4;


        /*
         * ========================================================
         * GENERATE EARTH TERRAIN
         * ========================================================
         */

        for (int localX = 0;
             localX < 16;
             localX++) {

            for (int localZ = 0;
                 localZ < 16;
                 localZ++) {


                int worldX =
                        chunkMinX + localX;

                int worldZ =
                        chunkMinZ + localZ;


                /*
                 * Convert Minecraft coordinates into
                 * real Earth coordinates.
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
                 * REAL USGS ELEVATION
                 * =================================================
                 */

                double elevationMeters =
                        EarthTerrainLoader.getGuemesElevation(
                                latitude,
                                longitude
                        );


                int naturalHeight =
                        EarthElevation.getMinecraftHeight(
                                elevationMeters
                        );


                /*
                 * =================================================
                 * WATER
                 * =================================================
                 */

                boolean water =
                        EarthWaterData.isWater(
                                latitude,
                                longitude
                        );


                if (water) {

                    generateWaterColumn(
                            chunkData,
                            localX,
                            localZ
                    );

                    continue;
                }


                /*
                 * Start with the real USGS terrain.
                 */

                int terrainHeight =
                        naturalHeight;


                /*
                 * =================================================
                 * GENERAL STORE TERRAIN BLEND
                 * =================================================
                 *
                 * Preserve the existing approved store terrain.
                 */

                if (EarthBuildingGenerator
                        .isInsideGuemesStoreBlendArea(
                                worldX,
                                worldZ
                        )) {


                    int storeHeight =
                            EarthBuildingGenerator
                                    .getGuemesStoreGroundY();


                    double distance =
                            EarthBuildingGenerator
                                    .getDistanceFromGuemesStore(
                                            worldX,
                                            worldZ
                                    );


                    /*
                     * Actual store foundation stays flat.
                     */

                    if (EarthBuildingGenerator
                            .isInsideGuemesStoreFoundation(
                                    worldX,
                                    worldZ
                            )) {


                        terrainHeight =
                                storeHeight;


                    } else {


                        double blend =
                                distance
                                        / STORE_BLEND_DISTANCE;


                        blend =
                                Math.max(
                                        0.0,
                                        Math.min(
                                                1.0,
                                                blend
                                        )
                                );


                        double smoothBlend =
                                blend
                                        * blend
                                        * (3.0
                                        - 2.0
                                        * blend);


                        double blendedHeight =
                                storeHeight
                                        + (naturalHeight
                                        - storeHeight)
                                        * smoothBlend;


                        int allowedDifference =
                                Math.max(
                                        1,
                                        (int) Math.floor(
                                                distance
                                                        / STORE_SLOPE_RUN
                                        )
                                );


                        int minimumHeight =
                                storeHeight
                                        - allowedDifference;


                        int maximumHeight =
                                storeHeight
                                        + allowedDifference;


                        terrainHeight =
                                (int) Math.round(
                                        blendedHeight
                                );


                        terrainHeight =
                                Math.max(
                                        minimumHeight,
                                        Math.min(
                                                maximumHeight,
                                                terrainHeight
                                        )
                                );
                    }
                }


                /*
                 * =================================================
                 * EARTHBOUND ROAD CLASSIFICATION
                 * =================================================
                 *
                 * Check each real Census/TIGER road class
                 * separately.
                 *
                 * This allows:
                 *
                 * Local   = 5 blocks
                 * Highway = 8 blocks
                 * Freeway = 10 blocks
                 *
                 * Higher classes take priority when roads overlap.
                 */

                RoadInfo roadInfo =
                        getRoadInfo(
                                latitude,
                                longitude
                        );


                /*
                 * =================================================
                 * GENERAL STORE ROAD PROTECTION
                 * =================================================
                 *
                 * IMPORTANT:
                 *
                 * Roads ARE allowed through the large store
                 * terrain-blend zone.
                 *
                 * Only the actual building foundation is protected.
                 */

                if (EarthBuildingGenerator
                        .isInsideGuemesStoreFoundation(
                                worldX,
                                worldZ
                        )) {


                    roadInfo =
                            RoadInfo.none();
                }


                /*
                 * =================================================
                 * ROAD HEIGHT
                 * =================================================
                 */

                if (roadInfo.isRoad()) {


                    double roadHeight =
                            getSmoothedRoadHeightPrecise(
                                    worldX,
                                    worldZ
                            );


                    /*
                     * Keep the road compatible with the
                     * General Store terrain transition.
                     */

                    if (EarthBuildingGenerator
                            .isInsideGuemesStoreBlendArea(
                                    worldX,
                                    worldZ
                            )) {


                        roadHeight =
                                getStoreCompatibleRoadHeightPrecise(
                                        worldX,
                                        worldZ,
                                        roadHeight,
                                        naturalHeight
                                );
                    }


                    /*
                     * Quantize road elevation to half-block
                     * increments.
                     *
                     * Examples:
                     *
                     * 65.0
                     * 65.5
                     * 66.0
                     * 66.5
                     *
                     * This eliminates many of the abrupt
                     * full-block jumps seen in the test road.
                     */

                    roadHeight =
                            quantizeToHalfBlock(
                                    roadHeight
                            );


                    generateRoadColumn(
                            chunkData,
                            localX,
                            localZ,
                            roadHeight,
                            roadInfo.surface,
                            roadInfo.border
                    );


                } else {


                    /*
                     * Normal non-road land.
                     */

                    generateNaturalLandColumn(
                            chunkData,
                            localX,
                            localZ,
                            terrainHeight
                    );
                }
            }
        }


        /*
         * ========================================================
         * GUEMES GENERAL STORE
         * ========================================================
         *
         * Keep the existing approved building.
         *
         * Ground Y = 65
         * Front = WEST
         */

        EarthBuildingGenerator.generateGuemesStore(
                chunkData,
                chunkX,
                chunkZ
        );


        /*
         * ========================================================
         * RESIDENTIAL TEST AREA
         * ========================================================
         */

        EarthResidentialGenerator.generate(
                chunkData,
                chunkX,
                chunkZ
        );


        /*
         * ========================================================
         * GUEMES DEVELOPMENT CONTROLLER
         * ========================================================
         */

        EarthGuemesGenerator.generate(
                chunkData,
                chunkX,
                chunkZ
        );


        return chunkData;
    }


    /*
     * ============================================================
     * ROAD INFORMATION
     * ============================================================
     */

    private RoadInfo getRoadInfo(
            double latitude,
            double longitude
    ) {

        /*
         * --------------------------------------------------------
         * FREEWAY
         * --------------------------------------------------------
         */

        double freewayDistance =
                EarthRoadData
                        .getDistanceToNearestFreewayMeters(
                                latitude,
                                longitude
                        );


        double freewayRadius =
                EarthRoadData.FREEWAY_WIDTH
                        / 2.0;


        if (freewayDistance
                <= freewayRadius) {


            return RoadInfo.surface(
                    EarthRoadData.RoadType.FREEWAY
            );
        }


        if (freewayDistance
                <= freewayRadius
                + ROAD_BORDER_WIDTH) {


            return RoadInfo.border(
                    EarthRoadData.RoadType.FREEWAY
            );
        }


        /*
         * --------------------------------------------------------
         * HIGHWAY
         * --------------------------------------------------------
         */

        double highwayDistance =
                EarthRoadData
                        .getDistanceToNearestHighwayMeters(
                                latitude,
                                longitude
                        );


        double highwayRadius =
                EarthRoadData.HIGHWAY_WIDTH
                        / 2.0;


        if (highwayDistance
                <= highwayRadius) {


            return RoadInfo.surface(
                    EarthRoadData.RoadType.HIGHWAY
            );
        }


        if (highwayDistance
                <= highwayRadius
                + ROAD_BORDER_WIDTH) {


            return RoadInfo.border(
                    EarthRoadData.RoadType.HIGHWAY
            );
        }


        /*
         * --------------------------------------------------------
         * LOCAL ROAD
         * --------------------------------------------------------
         */

        double localDistance =
                EarthRoadData
                        .getDistanceToNearestLocalRoadMeters(
                                latitude,
                                longitude
                        );


        double localRadius =
                EarthRoadData.LOCAL_WIDTH
                        / 2.0;


        if (localDistance
                <= localRadius) {


            return RoadInfo.surface(
                    EarthRoadData.RoadType.LOCAL
            );
        }


        if (localDistance
                <= localRadius
                + ROAD_BORDER_WIDTH) {


            return RoadInfo.border(
                    EarthRoadData.RoadType.LOCAL
            );
        }


        return RoadInfo.none();
    }


    /*
     * ============================================================
     * ROAD INFO HOLDER
     * ============================================================
     */

    private static final class RoadInfo {

        private final EarthRoadData.RoadType type;

        private final boolean surface;

        private final boolean border;


        private RoadInfo(
                EarthRoadData.RoadType type,
                boolean surface,
                boolean border
        ) {

            this.type =
                    type;

            this.surface =
                    surface;

            this.border =
                    border;
        }


        private static RoadInfo none() {

            return new RoadInfo(
                    EarthRoadData.RoadType.NONE,
                    false,
                    false
            );
        }


        private static RoadInfo surface(
                EarthRoadData.RoadType type
        ) {

            return new RoadInfo(
                    type,
                    true,
                    false
            );
        }


        private static RoadInfo border(
                EarthRoadData.RoadType type
        ) {

            return new RoadInfo(
                    type,
                    false,
                    true
            );
        }


        private boolean isRoad() {

            return type
                    != EarthRoadData.RoadType.NONE;
        }
    }


    /*
     * ============================================================
     * NATURAL LAND GENERATION
     * ============================================================
     */

    private void generateNaturalLandColumn(
            ChunkData chunkData,
            int localX,
            int localZ,
            int surfaceY
    ) {


        int minHeight =
                chunkData.getMinHeight();


        /*
         * Stone base.
         */

        for (int y = minHeight;
             y < surfaceY - 3;
             y++) {


            chunkData.setBlock(
                    localX,
                    y,
                    localZ,
                    Material.STONE
            );
        }


        /*
         * Dirt.
         */

        for (int y =
             Math.max(
                     minHeight,
                     surfaceY - 3
             );
             y < surfaceY;
             y++) {


            chunkData.setBlock(
                    localX,
                    y,
                    localZ,
                    Material.DIRT
            );
        }


        /*
         * Grass surface.
         */

        chunkData.setBlock(
                localX,
                surfaceY,
                localZ,
                Material.GRASS_BLOCK
        );
    }


    /*
     * ============================================================
     * ROAD COLUMN GENERATION
     * ============================================================
     *
     * Road elevation may end in:
     *
     * .0 = full block surface
     * .5 = half-block slab transition
     */

    private void generateRoadColumn(
            ChunkData chunkData,
            int localX,
            int localZ,
            double roadHeight,
            boolean roadSurface,
            boolean roadBorder
    ) {


        int minHeight =
                chunkData.getMinHeight();


        int baseY =
                (int) Math.floor(
                        roadHeight
                );


        boolean halfStep =
                Math.abs(
                        roadHeight
                                - baseY
                                - HALF_BLOCK
                ) < 0.01;


        /*
         * Fill below road with stone.
         */

        for (int y = minHeight;
             y < baseY - 3;
             y++) {


            chunkData.setBlock(
                    localX,
                    y,
                    localZ,
                    Material.STONE
            );
        }


        /*
         * Dirt support.
         */

        for (int y =
             Math.max(
                     minHeight,
                     baseY - 3
             );
             y < baseY;
             y++) {


            chunkData.setBlock(
                    localX,
                    y,
                    localZ,
                    Material.DIRT
            );
        }


        /*
         * ========================================================
         * HALF-BLOCK ROAD TRANSITION
         * ========================================================
         */

        if (halfStep) {


            /*
             * A solid support block underneath the slab.
             */

            chunkData.setBlock(
                    localX,
                    baseY,
                    localZ,
                    Material.DIRT
            );


            if (roadSurface) {


                setBottomSlab(
                        chunkData,
                        localX,
                        baseY + 1,
                        localZ,
                        ROAD_SURFACE_SLAB
                );


            } else if (roadBorder) {


                setBottomSlab(
                        chunkData,
                        localX,
                        baseY + 1,
                        localZ,
                        ROAD_BORDER_SLAB
                );
            }


            /*
             * Clear headroom over the transition.
             */

            clearRoadHeadroom(
                    chunkData,
                    localX,
                    baseY + 2,
                    localZ
            );


            return;
        }


        /*
         * ========================================================
         * FULL-BLOCK ROAD SURFACE
         * ========================================================
         */

        if (roadSurface) {


            chunkData.setBlock(
                    localX,
                    baseY,
                    localZ,
                    ROAD_SURFACE
            );


        } else if (roadBorder) {


            chunkData.setBlock(
                    localX,
                    baseY,
                    localZ,
                    ROAD_BORDER
            );
        }


        /*
         * Clear enough room above road surface for player.
         */

        clearRoadHeadroom(
                chunkData,
                localX,
                baseY + 1,
                localZ
        );
    }


    /*
     * ============================================================
     * BOTTOM SLAB
     * ============================================================
     */

    private void setBottomSlab(
            ChunkData chunkData,
            int localX,
            int y,
            int localZ,
            Material slabMaterial
    ) {


        BlockData blockData =
                slabMaterial
                        .createBlockData();


        if (blockData instanceof Slab) {


            Slab slab =
                    (Slab) blockData;


            slab.setType(
                    Slab.Type.BOTTOM
            );


            chunkData.setBlock(
                    localX,
                    y,
                    localZ,
                    slab
            );


        } else {


            /*
             * Safety fallback.
             */

            chunkData.setBlock(
                    localX,
                    y,
                    localZ,
                    slabMaterial
            );
        }
    }


    /*
     * ============================================================
     * ROAD HEADROOM
     * ============================================================
     */

    private void clearRoadHeadroom(
            ChunkData chunkData,
            int localX,
            int firstAirY,
            int localZ
    ) {


        int maxHeight =
                chunkData.getMaxHeight();


        for (int offset = 0;
             offset < 3;
             offset++) {


            int y =
                    firstAirY
                            + offset;


            if (y >= maxHeight) {

                break;
            }


            chunkData.setBlock(
                    localX,
                    y,
                    localZ,
                    Material.AIR
            );
        }
    }


    /*
     * ============================================================
     * WATER
     * ============================================================
     */

    private void generateWaterColumn(
            ChunkData chunkData,
            int localX,
            int localZ
    ) {


        int seaLevel =
                EarthWaterData.SEA_LEVEL;


        int seabed =
                seaLevel - 8;


        int minHeight =
                chunkData.getMinHeight();


        /*
         * Stone below seabed.
         */

        for (int y = minHeight;
             y < seabed - 3;
             y++) {


            chunkData.setBlock(
                    localX,
                    y,
                    localZ,
                    Material.STONE
            );
        }


        /*
         * Sand seabed.
         */

        for (int y =
             Math.max(
                     minHeight,
                     seabed - 3
             );
             y <= seabed;
             y++) {


            chunkData.setBlock(
                    localX,
                    y,
                    localZ,
                    Material.SAND
            );
        }


        /*
         * Water up to sea level.
         */

        for (int y =
             seabed + 1;
             y <= seaLevel;
             y++) {


            chunkData.setBlock(
                    localX,
                    y,
                    localZ,
                    Material.WATER
            );
        }
    }


    /*
     * ============================================================
     * PRECISE ROAD HEIGHT SMOOTHING
     * ============================================================
     *
     * IMPORTANT:
     *
     * The old generator returned an integer here.
     *
     * That forced every road elevation onto whole blocks.
     *
     * This version preserves the decimal average so the final
     * road can be quantized to half-block increments.
     */

    private double getSmoothedRoadHeightPrecise(
            int worldX,
            int worldZ
    ) {


        double totalHeight =
                0.0;


        int samples =
                0;


        for (int offsetX =
             -ROAD_SMOOTH_RADIUS;
             offsetX <= ROAD_SMOOTH_RADIUS;
             offsetX++) {


            for (int offsetZ =
                 -ROAD_SMOOTH_RADIUS;
                 offsetZ <= ROAD_SMOOTH_RADIUS;
                 offsetZ++) {


                int sampleX =
                        worldX
                                + offsetX;


                int sampleZ =
                        worldZ
                                + offsetZ;


                double latitude =
                        EarthCoordinates.getLatitude(
                                sampleX,
                                sampleZ
                        );


                double longitude =
                        EarthCoordinates.getLongitude(
                                sampleX,
                                sampleZ
                        );


                double elevationMeters =
                        EarthTerrainLoader
                                .getGuemesElevation(
                                        latitude,
                                        longitude
                                );


                int height =
                        EarthElevation
                                .getMinecraftHeight(
                                        elevationMeters
                                );


                totalHeight +=
                        height;


                samples++;
            }
        }


        if (samples == 0) {


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


            double elevationMeters =
                    EarthTerrainLoader
                            .getGuemesElevation(
                                    latitude,
                                    longitude
                            );


            return EarthElevation
                    .getMinecraftHeight(
                            elevationMeters
                    );
        }


        return totalHeight
                / samples;
    }


    /*
     * ============================================================
     * HALF-BLOCK QUANTIZATION
     * ============================================================
     */

    private double quantizeToHalfBlock(
            double height
    ) {


        return Math.round(
                height * 2.0
        ) / 2.0;
    }


    /*
     * ============================================================
     * STORE-COMPATIBLE PRECISE ROAD HEIGHT
     * ============================================================
     */

    private double getStoreCompatibleRoadHeightPrecise(
            int worldX,
            int worldZ,
            double roadHeight,
            int naturalHeight
    ) {


        int storeHeight =
                EarthBuildingGenerator
                        .getGuemesStoreGroundY();


        double distance =
                EarthBuildingGenerator
                        .getDistanceFromGuemesStore(
                                worldX,
                                worldZ
                        );


        double blend =
                distance
                        / STORE_BLEND_DISTANCE;


        blend =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                blend
                        )
                );


        double smoothBlend =
                blend
                        * blend
                        * (3.0
                        - 2.0
                        * blend);


        double blendedTerrain =
                storeHeight
                        + (naturalHeight
                        - storeHeight)
                        * smoothBlend;


        int allowedDifference =
                Math.max(
                        1,
                        (int) Math.floor(
                                distance
                                        / STORE_SLOPE_RUN
                        )
                );


        int minimumHeight =
                storeHeight
                        - allowedDifference;


        int maximumHeight =
                storeHeight
                        + allowedDifference;


        double storeTerrainHeight =
                Math.max(
                        minimumHeight,
                        Math.min(
                                maximumHeight,
                                blendedTerrain
                        )
                );


        /*
         * Blend road elevation toward the approved
         * General Store terrain.
         */

        double storeInfluence =
                1.0
                        - blend;


        return roadHeight
                * (1.0
                - storeInfluence)

                + storeTerrainHeight
                * storeInfluence;
    }
}
