package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;

public class EarthResidentialGenerator {

    /*
     * ============================================================
     * EARTHBOUND - GUEMES RESIDENTIAL TEST BLOCK
     * ============================================================
     *
     * Area A from the Guemes Island test-area plan.
     *
     * VERSION 1:
     * - Establishes the residential district.
     * - Creates a small neighborhood road.
     * - Creates 8 individual test house plots.
     * - Adds plot markers so placement is easy to inspect.
     *
     * Later versions will add:
     * - Procedural houses
     * - Furnished interiors
     * - NPC residents
     * - Property ownership
     * - Buying/selling
     * - Remodeling permissions
     *
     * IMPORTANT:
     * This class does NOT modify the General Store.
     * ============================================================
     */


    /*
     * Temporary center of Residential Test Block A.
     *
     * This can be moved after our first in-game inspection.
     */
    private static final int CENTER_X = -900;
    private static final int CENTER_Z = -150;


    /*
     * Neighborhood dimensions.
     */
    private static final int DISTRICT_HALF_WIDTH = 45;
    private static final int DISTRICT_HALF_LENGTH = 55;


    /*
     * Internal neighborhood road.
     */
    private static final int ROAD_HALF_WIDTH = 3;


    /*
     * House plot dimensions.
     */
    private static final int PLOT_WIDTH = 16;
    private static final int PLOT_DEPTH = 20;


    /*
     * Distance between neighboring plots.
     */
    private static final int PLOT_SPACING = 4;


    /*
     * Four plots north of the road
     * and four plots south of the road.
     *
     * Total = 8 properties.
     */
    private static final int PLOTS_PER_SIDE = 4;


    /*
     * Temporary materials used to make the
     * residential test area easy to see.
     */
    private static final Material ROAD_MATERIAL =
            Material.GRAY_CONCRETE;

    private static final Material SIDEWALK_MATERIAL =
            Material.SMOOTH_STONE;

    private static final Material PLOT_MARKER =
            Material.YELLOW_CONCRETE;


    private EarthResidentialGenerator() {
    }


    /*
     * ============================================================
     * MAIN GENERATION METHOD
     * ============================================================
     */

    public static void generate(
            ChunkData chunkData,
            int chunkX,
            int chunkZ
    ) {

        int chunkMinX = chunkX << 4;
        int chunkMinZ = chunkZ << 4;

        int chunkMaxX = chunkMinX + 15;
        int chunkMaxZ = chunkMinZ + 15;


        /*
         * Ignore chunks that are nowhere near
         * the Residential Test Block.
         */
        if (chunkMaxX < CENTER_X - DISTRICT_HALF_WIDTH
                || chunkMinX > CENTER_X + DISTRICT_HALF_WIDTH
                || chunkMaxZ < CENTER_Z - DISTRICT_HALF_LENGTH
                || chunkMinZ > CENTER_Z + DISTRICT_HALF_LENGTH) {

            return;
        }


        /*
         * Generate the neighborhood road first.
         */
        generateNeighborhoodRoad(
                chunkData,
                chunkMinX,
                chunkMinZ
        );


        /*
         * Then generate the eight test plots.
         */
        generateHousePlots(
                chunkData,
                chunkMinX,
                chunkMinZ
        );
    }


    /*
     * ============================================================
     * NEIGHBORHOOD ROAD
     * ============================================================
     *
     * The first test road runs east/west through Area A.
     *
     * It follows the EarthBound terrain rather than creating
     * a giant artificial platform.
     */

    private static void generateNeighborhoodRoad(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ
    ) {

        int roadStartX =
                CENTER_X - DISTRICT_HALF_WIDTH;

        int roadEndX =
                CENTER_X + DISTRICT_HALF_WIDTH;


        /*
         * Main road surface.
         */
        for (int worldX = roadStartX;
             worldX <= roadEndX;
             worldX++) {

            for (int worldZ =
                 CENTER_Z - ROAD_HALF_WIDTH;
                 worldZ <= CENTER_Z + ROAD_HALF_WIDTH;
                 worldZ++) {


                if (!belongsToChunk(
                        worldX,
                        worldZ,
                        chunkMinX,
                        chunkMinZ
                )) {

                    continue;
                }


                int surfaceY =
                        getSurfaceHeight(
                                worldX,
                                worldZ
                        );


                setWorldBlock(
                        chunkData,
                        chunkMinX,
                        chunkMinZ,
                        worldX,
                        surfaceY,
                        worldZ,
                        ROAD_MATERIAL
                );
            }
        }


        /*
         * Sidewalk along both sides of the road.
         */
        int northSidewalkZ =
                CENTER_Z + ROAD_HALF_WIDTH + 1;

        int southSidewalkZ =
                CENTER_Z - ROAD_HALF_WIDTH - 1;


        for (int worldX = roadStartX;
             worldX <= roadEndX;
             worldX++) {

            placeSurfaceBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    worldX,
                    northSidewalkZ,
                    SIDEWALK_MATERIAL
            );


            placeSurfaceBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    worldX,
                    southSidewalkZ,
                    SIDEWALK_MATERIAL
            );
        }
    }


    /*
     * ============================================================
     * HOUSE PLOTS
     * ============================================================
     *
     * Four properties are placed on the north side.
     * Four properties are placed on the south side.
     *
     * Version 1 only marks the boundaries.
     *
     * This allows us to inspect:
     *
     * - location
     * - terrain
     * - spacing
     * - road position
     * - property sizes
     *
     * before generating actual houses.
     */

    private static void generateHousePlots(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ
    ) {

        int totalWidth =
                (PLOTS_PER_SIDE * PLOT_WIDTH)
                        + ((PLOTS_PER_SIDE - 1)
                        * PLOT_SPACING);


        int firstPlotX =
                CENTER_X - (totalWidth / 2);


        /*
         * North row begins several blocks
         * beyond the north sidewalk.
         */
        int northPlotMinZ =
                CENTER_Z
                        + ROAD_HALF_WIDTH
                        + 5;


        /*
         * South row ends several blocks
         * beyond the south sidewalk.
         */
        int southPlotMaxZ =
                CENTER_Z
                        - ROAD_HALF_WIDTH
                        - 5;

        int southPlotMinZ =
                southPlotMaxZ
                        - PLOT_DEPTH;


        for (int plot = 0;
             plot < PLOTS_PER_SIDE;
             plot++) {


            int minX =
                    firstPlotX
                            + plot
                            * (PLOT_WIDTH + PLOT_SPACING);


            int maxX =
                    minX + PLOT_WIDTH;


            /*
             * NORTH PROPERTY
             */

            int northMinZ =
                    northPlotMinZ;

            int northMaxZ =
                    northMinZ + PLOT_DEPTH;


            markPlot(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    minX,
                    maxX,
                    northMinZ,
                    northMaxZ
            );


            /*
             * SOUTH PROPERTY
             */

            int southMinZ =
                    southPlotMinZ;

            int southMaxZ =
                    southPlotMaxZ;


            markPlot(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    minX,
                    maxX,
                    southMinZ,
                    southMaxZ
            );
        }
    }


    /*
     * ============================================================
     * PROPERTY MARKERS
     * ============================================================
     *
     * Yellow concrete outlines each test property.
     *
     * These are temporary development markers.
     * They can be removed when the actual residential
     * neighborhood is generated.
     */

    private static void markPlot(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ,
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {


        /*
         * North and south boundaries.
         */
        for (int x = minX;
             x <= maxX;
             x++) {


            placeSurfaceBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    x,
                    minZ,
                    PLOT_MARKER
            );


            placeSurfaceBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    x,
                    maxZ,
                    PLOT_MARKER
            );
        }


        /*
         * East and west boundaries.
         */
        for (int z = minZ;
             z <= maxZ;
             z++) {


            placeSurfaceBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    minX,
                    z,
                    PLOT_MARKER
            );


            placeSurfaceBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    maxX,
                    z,
                    PLOT_MARKER
            );
        }
    }


    /*
     * ============================================================
     * EARTHBOUND TERRAIN HEIGHT
     * ============================================================
     *
     * Uses the SAME real USGS elevation system as
     * the rest of EarthBound.
     */

    private static int getSurfaceHeight(
            int worldX,
            int worldZ
    ) {


        /*
         * Convert Minecraft coordinates back
         * into real Earth latitude/longitude.
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
         * Read the real USGS elevation.
         */
        double elevationMeters =
                EarthTerrainLoader.getGuemesElevation(
                        latitude,
                        longitude
                );


        /*
         * Convert real-world meters into the
         * EarthBound Minecraft Y scale.
         *
         * This is the correct method from
         * EarthElevation.java.
         */
        return EarthElevation.getMinecraftHeight(
                elevationMeters
        );
    }


    /*
     * ============================================================
     * SURFACE BLOCK HELPER
     * ============================================================
     */

    private static void placeSurfaceBlock(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ,
            int worldX,
            int worldZ,
            Material material
    ) {


        if (!belongsToChunk(
                worldX,
                worldZ,
                chunkMinX,
                chunkMinZ
        )) {

            return;
        }


        int surfaceY =
                getSurfaceHeight(
                        worldX,
                        worldZ
                );


        setWorldBlock(
                chunkData,
                chunkMinX,
                chunkMinZ,
                worldX,
                surfaceY,
                worldZ,
                material
        );
    }


    /*
     * ============================================================
     * CHUNK CHECK
     * ============================================================
     */

    private static boolean belongsToChunk(
            int worldX,
            int worldZ,
            int chunkMinX,
            int chunkMinZ
    ) {

        return worldX >= chunkMinX
                && worldX <= chunkMinX + 15
                && worldZ >= chunkMinZ
                && worldZ <= chunkMinZ + 15;
    }


    /*
     * ============================================================
     * WORLD BLOCK -> CHUNK BLOCK
     * ============================================================
     */

    private static void setWorldBlock(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ,
            int worldX,
            int y,
            int worldZ,
            Material material
    ) {


        /*
         * Protect against invalid Y coordinates.
         */
        if (y < chunkData.getMinHeight()
                || y >= chunkData.getMaxHeight()) {

            return;
        }


        /*
         * Convert world coordinates into
         * local chunk coordinates.
         */
        int localX =
                worldX - chunkMinX;

        int localZ =
                worldZ - chunkMinZ;


        /*
         * Extra safety check.
         */
        if (localX < 0
                || localX > 15
                || localZ < 0
                || localZ > 15) {

            return;
        }


        /*
         * Place the block.
         */
        chunkData.setBlock(
                localX,
                y,
                localZ,
                material
        );
    }
}
