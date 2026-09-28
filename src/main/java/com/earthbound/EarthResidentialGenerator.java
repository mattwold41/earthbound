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
     * VERSION 2:
     * - Keeps the neighborhood road.
     * - Keeps sidewalks.
     * - Keeps 8 test house plots.
     * - Adds terrain grading information for EarthGenerator.
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
     * Main neighborhood dimensions.
     */
    private static final int DISTRICT_HALF_WIDTH = 45;
    private static final int DISTRICT_HALF_LENGTH = 55;


    /*
     * Extra distance outside Area A used for
     * gradual terrain blending.
     */
    private static final int TERRAIN_BLEND_MARGIN = 24;


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


    /*
     * The neighborhood uses the real USGS height
     * at its center as its reference elevation.
     *
     * This is NOT hard-coded to an artificial Y level.
     */
    private static final int DISTRICT_BASE_Y =
            getNaturalSurfaceHeight(
                    CENTER_X,
                    CENTER_Z
            );


    private EarthResidentialGenerator() {
    }


    /*
     * ============================================================
     * TERRAIN GRADING API
     * ============================================================
     *
     * EarthGenerator will use these methods BEFORE
     * placing the surface blocks.
     */


    public static boolean isInsideTerrainBlendArea(
            int worldX,
            int worldZ
    ) {

        return worldX >=
                CENTER_X
                        - DISTRICT_HALF_WIDTH
                        - TERRAIN_BLEND_MARGIN

                && worldX <=
                CENTER_X
                        + DISTRICT_HALF_WIDTH
                        + TERRAIN_BLEND_MARGIN

                && worldZ >=
                CENTER_Z
                        - DISTRICT_HALF_LENGTH
                        - TERRAIN_BLEND_MARGIN

                && worldZ <=
                CENTER_Z
                        + DISTRICT_HALF_LENGTH
                        + TERRAIN_BLEND_MARGIN;
    }


    public static boolean isInsideDistrict(
            int worldX,
            int worldZ
    ) {

        return worldX >=
                CENTER_X - DISTRICT_HALF_WIDTH

                && worldX <=
                CENTER_X + DISTRICT_HALF_WIDTH

                && worldZ >=
                CENTER_Z - DISTRICT_HALF_LENGTH

                && worldZ <=
                CENTER_Z + DISTRICT_HALF_LENGTH;
    }


    /*
     * Returns the terrain height Area A wants.
     *
     * Inside the neighborhood we gently reduce
     * the natural terrain variation instead of
     * forcing the whole neighborhood onto one
     * giant flat platform.
     *
     * Outside the neighborhood, the terrain
     * gradually transitions back to untouched
     * USGS terrain.
     */
    public static int getGradedTerrainHeight(
            int worldX,
            int worldZ,
            int naturalHeight
    ) {

        if (!isInsideTerrainBlendArea(
                worldX,
                worldZ
        )) {

            return naturalHeight;
        }


        /*
         * --------------------------------------------------------
         * INSIDE AREA A
         * --------------------------------------------------------
         */

        if (isInsideDistrict(
                worldX,
                worldZ
        )) {

            /*
             * Preserve a little natural variation.
             *
             * This prevents Area A from looking like
             * a completely artificial flat platform.
             */
            int difference =
                    naturalHeight
                            - DISTRICT_BASE_Y;


            /*
             * Reduce the terrain difference to 25%.
             */
            int softenedDifference =
                    (int) Math.round(
                            difference * 0.25
                    );


            /*
             * Limit the remaining variation.
             */
            softenedDifference =
                    Math.max(
                            -4,
                            Math.min(
                                    4,
                                    softenedDifference
                            )
                    );


            return DISTRICT_BASE_Y
                    + softenedDifference;
        }


        /*
         * --------------------------------------------------------
         * OUTSIDE AREA A - BLEND BACK TO USGS
         * --------------------------------------------------------
         */

        int distanceX = 0;

        if (worldX <
                CENTER_X - DISTRICT_HALF_WIDTH) {

            distanceX =
                    (CENTER_X
                            - DISTRICT_HALF_WIDTH)
                            - worldX;

        } else if (worldX >
                CENTER_X + DISTRICT_HALF_WIDTH) {

            distanceX =
                    worldX
                            - (CENTER_X
                            + DISTRICT_HALF_WIDTH);
        }


        int distanceZ = 0;

        if (worldZ <
                CENTER_Z - DISTRICT_HALF_LENGTH) {

            distanceZ =
                    (CENTER_Z
                            - DISTRICT_HALF_LENGTH)
                            - worldZ;

        } else if (worldZ >
                CENTER_Z + DISTRICT_HALF_LENGTH) {

            distanceZ =
                    worldZ
                            - (CENTER_Z
                            + DISTRICT_HALF_LENGTH);
        }


        double distance =
                Math.sqrt(
                        (double) distanceX * distanceX
                                + (double) distanceZ * distanceZ
                );


        double blend =
                distance
                        / TERRAIN_BLEND_MARGIN;


        blend =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                blend
                        )
                );


        /*
         * Smoothstep gives us a gentler transition.
         */
        double smoothBlend =
                blend
                        * blend
                        * (3.0 - 2.0 * blend);


        /*
         * Terrain height at the Area A edge.
         */
        int edgeHeight =
                getDistrictHeightFromNatural(
                        naturalHeight
                );


        double result =
                edgeHeight
                        + (naturalHeight - edgeHeight)
                        * smoothBlend;


        return (int) Math.round(result);
    }


    /*
     * Applies the same softened terrain rule used
     * inside the district.
     */
    private static int getDistrictHeightFromNatural(
            int naturalHeight
    ) {

        int difference =
                naturalHeight
                        - DISTRICT_BASE_Y;


        int softenedDifference =
                (int) Math.round(
                        difference * 0.25
                );


        softenedDifference =
                Math.max(
                        -4,
                        Math.min(
                                4,
                                softenedDifference
                        )
                );


        return DISTRICT_BASE_Y
                + softenedDifference;
    }


    /*
     * ============================================================
     * MAIN PHYSICAL GENERATION
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
         * Ignore chunks nowhere near Area A.
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
         * Road.
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
                        getAreaSurfaceHeight(
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
         * Keep this style.
         *
         * This is the sidewalk appearance approved
         * during the first Area A visual test.
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


        int northPlotMinZ =
                CENTER_Z
                        + ROAD_HALF_WIDTH
                        + 5;


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
             * North property.
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
             * South property.
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
         * North/south boundaries.
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
         * East/west boundaries.
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
     * AREA A SURFACE HEIGHT
     * ============================================================
     *
     * Once EarthGenerator is connected to the terrain
     * grading API, this will match the actual generated
     * terrain underneath the roads and plots.
     */

    private static int getAreaSurfaceHeight(
            int worldX,
            int worldZ
    ) {

        int naturalHeight =
                getNaturalSurfaceHeight(
                        worldX,
                        worldZ
                );


        return getGradedTerrainHeight(
                worldX,
                worldZ,
                naturalHeight
        );
    }


    /*
     * ============================================================
     * NATURAL USGS TERRAIN HEIGHT
     * ============================================================
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
                getAreaSurfaceHeight(
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
