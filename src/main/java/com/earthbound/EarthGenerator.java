package com.earthbound;

import org.bukkit.generator.ChunkGenerator.ChunkData;

public final class EarthGuemesGenerator {

    /*
     * ============================================================
     * EARTHBOUND - GUEMES ISLAND DEVELOPMENT CONTROLLER
     * ============================================================
     *
     * PURPOSE:
     *
     * This class controls development outside the approved
     * Area A neighborhood and General Store.
     *
     * It does NOT replace:
     *
     * - USGS terrain
     * - Census roads
     * - coastline/water generation
     * - Area A
     * - General Store
     *
     * Development must pass safety checks before structures
     * are generated.
     * ============================================================
     */

    /*
     * Current Guemes geographic bounds.
     */
    private static final double WEST_LONGITUDE =
            -122.70;

    private static final double EAST_LONGITUDE =
            -122.55;

    private static final double SOUTH_LATITUDE =
            48.47;

    private static final double NORTH_LATITUDE =
            48.60;

    /*
     * Approximate Guemes Mountain protection center.
     *
     * This remains protected from residential generation.
     */
    private static final double GUEMES_MOUNTAIN_LATITUDE =
            48.5445;

    private static final double GUEMES_MOUNTAIN_LONGITUDE =
            -122.5945;

    /*
     * Real-world protection radius.
     */
    private static final double GUEMES_MOUNTAIN_PROTECTION_METERS =
            850.0;

    /*
     * Keep structures away from very low shoreline terrain.
     */
    private static final int MINIMUM_BUILD_HEIGHT =
            EarthWaterData.SEA_LEVEL + 3;

    /*
     * Development must be reasonably close to a real
     * Census road.
     *
     * This is measured in real-world meters.
     */
    private static final double MAXIMUM_ROAD_DISTANCE_METERS =
            45.0;

    /*
     * ============================================================
     * MASTER DEVELOPMENT SWITCH
     * ============================================================
     *
     * TRUE:
     * New Guemes development may generate.
     *
     * FALSE:
     * Only the already-approved systems generate.
     *
     * We are enabling this for the controlled first Guemes
     * development test.
     */
    private static final boolean ENABLE_ISLAND_HOMES =
            true;

    /*
     * ============================================================
     * FIRST CONTROLLED GUEMES RESIDENTIAL CLUSTER
     * ============================================================
     *
     * This is intentionally small.
     *
     * We are NOT automatically filling every Guemes road
     * with houses.
     *
     * The cluster is positioned relative to EarthBound
     * coordinates and must still pass all development checks.
     *
     * This first batch gives us something visible to inspect
     * on Xbox before we expand the system farther.
     * ============================================================
     */

    /*
     * Center of the first controlled district.
     *
     * This is south/central Guemes development territory,
     * separate from Area A.
     *
     * The road-finding routine below searches around this
     * starting point instead of blindly assuming the road
     * is exactly at one Z coordinate.
     */
    private static final double FIRST_DISTRICT_LATITUDE =
            48.5350;

    private static final double FIRST_DISTRICT_LONGITUDE =
            -122.6245;

    /*
     * Number of homes on each side of the road.
     *
     * 3 north + 3 south = 6 total.
     */
    private static final int HOMES_PER_SIDE =
            3;

    /*
     * Existing Area A property dimensions.
     *
     * These match the reusable house generator.
     */
    private static final int PLOT_WIDTH =
            16;

    private static final int PLOT_DEPTH =
            20;

    private static final int PLOT_SPACING =
            4;

    /*
     * Existing residential generator assumes an
     * east-west road.
     *
     * We therefore only generate this first district when
     * the real road near the district appears predominantly
     * east-west.
     */
    private static final int ROAD_DIRECTION_SAMPLE_BLOCKS =
            12;

    /*
     * Search distance for finding the real road near the
     * district center.
     */
    private static final int ROAD_SEARCH_BLOCKS =
            30;

    private EarthGuemesGenerator() {
    }

    /*
     * ============================================================
     * MAIN GUEMES GENERATION
     * ============================================================
     */

    public static void generate(
            ChunkData chunkData,
            int chunkX,
            int chunkZ
    ) {

        if (!ENABLE_ISLAND_HOMES) {
            return;
        }

        if (!EarthRoadData.isLoaded()) {
            return;
        }

        generateFirstResidentialDistrict(
                chunkData,
                chunkX,
                chunkZ
        );
    }

    /*
     * ============================================================
     * FIRST RESIDENTIAL DISTRICT
     * ============================================================
     */

    private static void generateFirstResidentialDistrict(
            ChunkData chunkData,
            int chunkX,
            int chunkZ
    ) {

        int districtCenterX =
                EarthCoordinates.longitudeToMinecraftX(
                        FIRST_DISTRICT_LONGITUDE
                );

        int approximateCenterZ =
                EarthCoordinates.latitudeToMinecraftZ(
                        FIRST_DISTRICT_LATITUDE
                );

        int roadCenterZ =
                findRoadCenterZ(
                        districtCenterX,
                        approximateCenterZ
                );

        /*
         * No real Census road was found close enough.
         */
        if (roadCenterZ == Integer.MIN_VALUE) {
            return;
        }

        /*
         * Our current approved house design faces
         * north/south.
         *
         * Therefore this first reusable district should only
         * generate beside a road that appears primarily
         * east-west.
         */
        if (!isApproximatelyEastWestRoad(
                districtCenterX,
                roadCenterZ
        )) {
            return;
        }

        double roadLatitude =
                EarthCoordinates.getLatitude(
                        districtCenterX,
                        roadCenterZ
                );

        double roadLongitude =
                EarthCoordinates.getLongitude(
                        districtCenterX,
                        roadCenterZ
                );

        double roadDistance =
                EarthRoadData
                        .getDistanceToNearestRoadMeters(
                                roadLatitude,
                                roadLongitude
                        );

        if (roadDistance
                > MAXIMUM_ROAD_DISTANCE_METERS) {

            return;
        }

        /*
         * Determine total district width.
         */
        int totalWidth =
                (HOMES_PER_SIDE * PLOT_WIDTH)
                        + ((HOMES_PER_SIDE - 1)
                        * PLOT_SPACING);

        int firstPlotX =
                districtCenterX
                        - (totalWidth / 2);

        /*
         * Keep properties beyond the actual road surface.
         *
         * The reusable residential house system handles
         * the walkway toward roadCenterZ.
         */
        int northPlotMinZ =
                roadCenterZ + 8;

        int southPlotMaxZ =
                roadCenterZ - 8;

        int southPlotMinZ =
                southPlotMaxZ - PLOT_DEPTH;

        for (int plot = 0;
             plot < HOMES_PER_SIDE;
             plot++) {

            int plotMinX =
                    firstPlotX
                            + plot
                            * (PLOT_WIDTH
                            + PLOT_SPACING);

            /*
             * NORTH PROPERTY
             */
            generateResidentialHome(
                    chunkData,
                    chunkX,
                    chunkZ,
                    plotMinX,
                    northPlotMinZ,
                    true,
                    100 + plot,
                    roadCenterZ
            );

            /*
             * SOUTH PROPERTY
             */
            generateResidentialHome(
                    chunkData,
                    chunkX,
                    chunkZ,
                    plotMinX,
                    southPlotMinZ,
                    false,
                    200 + plot,
                    roadCenterZ
            );
        }
    }

    /*
     * ============================================================
     * FIND REAL ROAD CENTER
     * ============================================================
     *
     * Search north/south from the approximate district
     * coordinate.
     *
     * We collect road blocks and use the middle of the
     * detected road as the working center.
     * ============================================================
     */

    private static int findRoadCenterZ(
            int worldX,
            int approximateZ
    ) {

        int firstRoadZ =
                Integer.MIN_VALUE;

        int lastRoadZ =
                Integer.MIN_VALUE;

        for (int offset = -ROAD_SEARCH_BLOCKS;
             offset <= ROAD_SEARCH_BLOCKS;
             offset++) {

            int worldZ =
                    approximateZ + offset;

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

            if (EarthRoadData.isRoad(
                    latitude,
                    longitude
            )) {

                if (firstRoadZ
                        == Integer.MIN_VALUE) {

                    firstRoadZ =
                            worldZ;
                }

                lastRoadZ =
                        worldZ;
            }
        }

        if (firstRoadZ
                == Integer.MIN_VALUE) {

            return Integer.MIN_VALUE;
        }

        return (firstRoadZ + lastRoadZ) / 2;
    }

    /*
     * ============================================================
     * ROAD DIRECTION CHECK
     * ============================================================
     *
     * The current reusable homes face north/south.
     *
     * We therefore verify that the road continues farther
     * east/west than north/south before generating this
     * first district.
     * ============================================================
     */

    private static boolean isApproximatelyEastWestRoad(
            int roadX,
            int roadZ
    ) {

        int eastWestHits =
                0;

        int northSouthHits =
                0;

        for (int offset =
                     -ROAD_DIRECTION_SAMPLE_BLOCKS;
             offset <= ROAD_DIRECTION_SAMPLE_BLOCKS;
             offset += 2) {

            /*
             * East / west sample.
             */
            double eastWestLatitude =
                    EarthCoordinates.getLatitude(
                            roadX + offset,
                            roadZ
                    );

            double eastWestLongitude =
                    EarthCoordinates.getLongitude(
                            roadX + offset,
                            roadZ
                    );

            if (EarthRoadData.isRoad(
                    eastWestLatitude,
                    eastWestLongitude
            )) {

                eastWestHits++;
            }

            /*
             * North / south sample.
             */
            double northSouthLatitude =
                    EarthCoordinates.getLatitude(
                            roadX,
                            roadZ + offset
                    );

            double northSouthLongitude =
                    EarthCoordinates.getLongitude(
                            roadX,
                            roadZ + offset
                    );

            if (EarthRoadData.isRoad(
                    northSouthLatitude,
                    northSouthLongitude
            )) {

                northSouthHits++;
            }
        }

        return eastWestHits
                >= northSouthHits;
    }

    /*
     * ============================================================
     * RESIDENTIAL HOME GENERATION
     * ============================================================
     */

    public static void generateResidentialHome(
            ChunkData chunkData,
            int chunkX,
            int chunkZ,
            int plotMinX,
            int plotMinZ,
            boolean northSide,
            int houseNumber,
            int roadCenterZ
    ) {

        int propertyCenterX =
                plotMinX + (PLOT_WIDTH / 2);

        int propertyCenterZ =
                plotMinZ + (PLOT_DEPTH / 2);

        if (!isSuitableForDevelopment(
                propertyCenterX,
                propertyCenterZ
        )) {
            return;
        }

        int chunkMinX =
                chunkX << 4;

        int chunkMinZ =
                chunkZ << 4;

        EarthResidentialGenerator.generateHome(
                chunkData,
                chunkMinX,
                chunkMinZ,
                plotMinX,
                plotMinZ,
                northSide,
                houseNumber,
                roadCenterZ
        );
    }

    /*
     * ============================================================
     * DEVELOPMENT SUITABILITY
     * ============================================================
     */

    public static boolean isSuitableForDevelopment(
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

        /*
         * Must remain inside current Guemes bounds.
         */
        if (!isInsideGuemesBounds(
                latitude,
                longitude
        )) {

            return false;
        }

        /*
         * Protect Guemes Mountain.
         */
        if (isInsideGuemesMountainProtection(
                latitude,
                longitude
        )) {

            return false;
        }

        /*
         * Keep development close to real roads.
         */
        double roadDistance =
                EarthRoadData
                        .getDistanceToNearestRoadMeters(
                                latitude,
                                longitude
                        );

        if (roadDistance
                > MAXIMUM_ROAD_DISTANCE_METERS) {

            return false;
        }

        /*
         * Check real USGS elevation.
         */
        double elevationMeters =
                EarthTerrainLoader
                        .getGuemesElevation(
                                latitude,
                                longitude
                        );

        int minecraftHeight =
                EarthElevation
                        .getMinecraftHeight(
                                elevationMeters
                        );

        /*
         * Protect shoreline / very low terrain.
         */
        if (minecraftHeight
                < MINIMUM_BUILD_HEIGHT) {

            return false;
        }

        return true;
    }

    /*
     * ============================================================
     * GUEMES BOUNDS
     * ============================================================
     */

    public static boolean isInsideGuemesBounds(
            double latitude,
            double longitude
    ) {

        return latitude
                >= SOUTH_LATITUDE
                && latitude
                <= NORTH_LATITUDE
                && longitude
                >= WEST_LONGITUDE
                && longitude
                <= EAST_LONGITUDE;
    }

    /*
     * ============================================================
     * GUEMES MOUNTAIN PROTECTION
     * ============================================================
     */

    public static boolean isInsideGuemesMountainProtection(
            double latitude,
            double longitude
    ) {

        double distanceMeters =
                distanceMeters(
                        latitude,
                        longitude,
                        GUEMES_MOUNTAIN_LATITUDE,
                        GUEMES_MOUNTAIN_LONGITUDE
                );

        return distanceMeters
                <= GUEMES_MOUNTAIN_PROTECTION_METERS;
    }

    /*
     * ============================================================
     * DISTANCE HELPER
     * ============================================================
     */

    private static double distanceMeters(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2
    ) {

        double earthRadiusMeters =
                6371000.0;

        double latitude1Radians =
                Math.toRadians(
                        latitude1
                );

        double latitude2Radians =
                Math.toRadians(
                        latitude2
                );

        double latitudeDifference =
                Math.toRadians(
                        latitude2
                                - latitude1
                );

        double longitudeDifference =
                Math.toRadians(
                        longitude2
                                - longitude1
                );

        double a =
                Math.sin(
                        latitudeDifference / 2.0
                )
                        * Math.sin(
                        latitudeDifference / 2.0
                )
                        + Math.cos(
                        latitude1Radians
                )
                        * Math.cos(
                        latitude2Radians
                )
                        * Math.sin(
                        longitudeDifference / 2.0
                )
                        * Math.sin(
                        longitudeDifference / 2.0
                );

        double c =
                2.0
                        * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1.0 - a)
                );

        return earthRadiusMeters * c;
    }
}
