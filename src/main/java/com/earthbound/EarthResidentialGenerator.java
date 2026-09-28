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
     * - Adds plot corner markers so placement is easy to inspect.
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
     *
     * We intentionally keep the district separate from the
     * General Store/spawn area.
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
     * Number of plots on each side of the road.
     *
     * 4 north + 4 south = 8 test properties.
     */
    private static final int PLOTS_PER_SIDE = 4;


    /*
     * Materials used for the first placement test.
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
         * Skip chunks nowhere near Area A.
         */
        if (chunkMaxX < CENTER_X - DISTRICT_HALF_WIDTH
                || chunkMinX > CENTER_X + DISTRICT_HALF_WIDTH
                || chunkMaxZ < CENTER_Z - DISTRICT_HALF_LENGTH
                || chunkMinZ > CENTER_Z + DISTRICT_HALF_LENGTH) {

            return;
        }


        generateNeighborhoodRoad(
                chunkData,
                chunkMinX,
                chunkMinZ
        );


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
     * We place it on top of the existing EarthBound terrain rather
     * than creating a giant flat platform.
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
         * Sidewalk along both sides of the neighborhood road.
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
     * Four plots north of the road.
     * Four plots south of the road.
     *
     * For Version 1 we mark the four corners of each property.
     * This lets us inspect spacing and terrain before houses are
     * generated.
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
         * North row.
         */
        int northPlotMinZ =
                CENTER_Z
                        + ROAD_HALF_WIDTH
                        + 5;


        /*
         * South row.
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
             * North property.
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
             * South property.
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
     * PLOT MARKERS
     * ============================================================
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
         * Outline the property with yellow blocks.
         */
        for (int x = minX; x <= maxX; x++) {

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


        for (int z = minZ; z <= maxZ; z++) {

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
     * TERRAIN HEIGHT
     * ============================================================
     *
     * Uses the same real Earth elevation system already powering
     * Guemes Island.
     */

    private static int getSurfaceHeight(
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


        return EarthElevation.minecraftY(
                elevationMeters
        );
    }


    /*
     * ============================================================
     * BLOCK HELPERS
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


    private static void setWorldBlock(
            ChunkData chunkData,
            int chunkMinX,
            int chunkMinZ,
            int worldX,
            int y,
            int worldZ,
            Material material
    ) {

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
