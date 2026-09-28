package com.earthbound;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;

public class EarthResidentialGenerator {

    /*
     * ============================================================
     * EARTHBOUND - GUEMES RESIDENTIAL TEST BLOCK
     * ============================================================
     *
     * Area A
     *
     * VERSION 3:
     * - Uses the existing natural USGS terrain.
     * - Does NOT raise the district.
     * - Does NOT flatten the district.
     * - Does NOT grade surrounding terrain.
     * - Keeps the neighborhood road.
     * - Keeps sidewalks.
     * - Keeps 8 test house plots.
     *
     * IMPORTANT:
     * This class does NOT modify the General Store.
     * ============================================================
     */


    /*
     * Temporary Area A center.
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
     * House plots.
     */
    private static final int PLOT_WIDTH = 16;
    private static final int PLOT_DEPTH = 20;
    private static final int PLOT_SPACING = 4;
    private static final int PLOTS_PER_SIDE = 4;


    /*
     * Development materials.
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

        int chunkMinX =
                chunkX << 4;

        int chunkMinZ =
                chunkZ << 4;

        int chunkMaxX =
                chunkMinX + 15;

        int chunkMaxZ =
                chunkMinZ + 15;


        /*
         * Ignore chunks that are nowhere near Area A.
         */
        if (chunkMaxX <
                CENTER_X - DISTRICT_HALF_WIDTH

                || chunkMinX >
                CENTER_X + DISTRICT_HALF_WIDTH

                || chunkMaxZ <
                CENTER_Z - DISTRICT_HALF_LENGTH

                || chunkMinZ >
                CENTER_Z + DISTRICT_HALF_LENGTH) {

            return;
        }


        /*
         * Generate the neighborhood road.
         */
        generateNeighborhoodRoad(
                chunkData,
                chunkMinX,
                chunkMinZ
        );


        /*
         * Generate the eight test properties.
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
     * The road follows the existing USGS terrain.
     *
     * No land is raised or flattened.
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
         * Main road.
         */
        for (int worldX = roadStartX;
             worldX <= roadEndX;
             worldX++) {

            for (int worldZ =
                 CENTER_Z - ROAD_HALF_WIDTH;
                 worldZ <=
                         CENTER_Z + ROAD_HALF_WIDTH;
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
                        getNaturalSurfaceHeight(
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
         * ========================================================
         * SIDEWALKS
         * ========================================================
         *
         * Keep the Area A smooth-stone sidewalk style.
         *
         * Sidewalks follow the existing natural land.
         */

        int northSidewalkZ =
                CENTER_Z
                        + ROAD_HALF_WIDTH
                        + 1;

        int southSidewalkZ =
                CENTER_Z
                        - ROAD_HALF_WIDTH
                        - 1;


        for (int worldX = roadStartX;
             worldX <= roadEndX;
             worldX++) {

            placeNaturalSurfaceBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    worldX,
                    northSidewalkZ,
                    SIDEWALK_MATERIAL
            );


            placeNaturalSurfaceBlock(
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
     * Four properties north of the road.
     * Four properties south of the road.
     *
     * The plot markers follow the existing land.
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
                CENTER_X
                        - (totalWidth / 2);


        /*
         * North plots.
         */
        int northPlotMinZ =
                CENTER_Z
                        + ROAD_HALF_WIDTH
                        + 5;


        /*
         * South plots.
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
                            * (PLOT_WIDTH
                            + PLOT_SPACING);


            int maxX =
                    minX
                            + PLOT_WIDTH;


            /*
             * NORTH PROPERTY
             */

            int northMinZ =
                    northPlotMinZ;

            int northMaxZ =
                    northMinZ
                            + PLOT_DEPTH;


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

            markPlot(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    minX,
                    maxX,
                    southPlotMinZ,
                    southPlotMaxZ
            );
        }
    }


    /*
     * ============================================================
     * PROPERTY MARKERS
     * ============================================================
     *
     * Yellow concrete outlines each temporary test property.
     *
     * Every marker is placed directly on the existing
     * natural USGS terrain.
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

            placeNaturalSurfaceBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    x,
                    minZ,
                    PLOT_MARKER
            );


            placeNaturalSurfaceBlock(
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

            placeNaturalSurfaceBlock(
                    chunkData,
                    chunkMinX,
                    chunkMinZ,
                    minX,
                    z,
                    PLOT_MARKER
            );


            placeNaturalSurfaceBlock(
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
     * NATURAL USGS TERRAIN HEIGHT
     * ============================================================
     *
     * This reads the same real elevation system used
     * by the rest of EarthBound.
     *
     * IMPORTANT:
     *
     * We return this height directly.
     *
     * There is:
     * - no district base Y
     * - no terrain flattening
     * - no terrain compression
     * - no terrain blending
     * - no artificial Area A elevation
     */

    private static int getNaturalSurfaceHeight(
            int worldX,
            int worldZ
    ) {

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
                EarthTerrainLoader.getGuemesElevation(
                        latitude,
                        longitude
                );


        return EarthElevation.getMinecraftHeight(
                elevationMeters
        );
    }


    /*
     * ============================================================
     * NATURAL SURFACE BLOCK HELPER
     * ============================================================
     */

    private static void placeNaturalSurfaceBlock(
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
                getNaturalSurfaceHeight(
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


        int localX =
                worldX - chunkMinX;

        int localZ =
                worldZ - chunkMinZ;


        if (localX < 0
                || localX > 15
                || localZ < 0
                || localZ > 15) {

            return;
        }


        chunkData.setBlock(
                localX,
                y,
                localZ,
                material
        );
    }
}
