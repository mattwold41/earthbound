package com.earthbound;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.block.BlockFace;

import com.earthbound.EarthAlphaResidentialGenerator
        .ResidentialSite;

import com.earthbound.roads.EarthRoadData;
import com.earthbound.roads.EarthRoadData.RoadType;

import com.earthbound.roads.EarthRoadSpatialIndex;
import com.earthbound.roads.EarthRoadSpatialIndex.RoadColumn;

import com.earthbound.terrain.EarthTerrainLoader;

import com.earthbound.water.EarthWaterData;
import com.earthbound.water.EarthWaterGenerator;

/*
 * ============================================================
 * EARTHBOUND AUTOMATIC RESIDENTIAL SITE FINDER
 * Friends Alpha
 * ============================================================
 *
 * Searches real-world geographic data for suitable
 * residential development locations.
 *
 * This class does not:
 *
 * - Modify Minecraft blocks
 * - Generate chunks
 * - Register property ownership
 * - Charge players
 * - Enable residential construction
 * - Automatically modify config.yml
 *
 * All discovered locations require review before use.
 * ============================================================
 */

public final class EarthAlphaSiteFinder {

    private static final int SEARCH_RADIUS = 512;

    private static final int SEARCH_STEP = 4;

    private static final int MAX_ACCESS_DISTANCE = 16;

    private static final int MINIMUM_PROPERTY_GAP = 8;

    private static final int MINIMUM_BUILD_HEIGHT =
            EarthWaterData.SEA_LEVEL + 3;

    private static final int MAXIMUM_BUILD_HEIGHT = 280;

    private static final int DRIVEWAY_MIN_X = 18;

    private static final int DRIVEWAY_MAX_X = 22;

    private static final int MAX_CANDIDATE_VALIDATIONS =
            20000;

    private static final int PLOT_WIDTH =
            EarthAlphaResidentialGenerator.PLOT_WIDTH;

    private static final int PLOT_LENGTH =
            EarthAlphaResidentialGenerator.PLOT_LENGTH;

    private static final String[] PROPERTY_IDS = {

            "guemes_home_01",
            "guemes_home_02",
            "guemes_home_03",
            "guemes_home_04",
            "guemes_home_05",
            "guemes_home_06"
    };

    /*
     * North and south facing properties only.
     *
     * The current ownership configuration expects
     * world-aligned 24 x 32 block boundaries.
     */

    private static final BlockFace[] ALLOWED_FACING = {

            BlockFace.NORTH,
            BlockFace.SOUTH
    };

    private EarthAlphaSiteFinder() {
    }

    /*
     * ========================================================
     * FIND FRIENDS ALPHA RESIDENTIAL LOCATIONS
     * ========================================================
     */

    public static List<ResidentialSite> findSites() {

        List<ResidentialSite> selected =
                new ArrayList<>();

        if (!EarthTerrainLoader.isLoaded()
                || !EarthRoadData.isLoaded()
                || !EarthRoadSpatialIndex.isLoaded()) {

            System.out.println(
                    "[EarthBound] Residential search cancelled: "
                    + "geographic data is unavailable."
            );

            return List.of();
        }

        int centerX =
                EarthBuildingGenerator.getGuemesStoreX();

        int centerZ =
                EarthBuildingGenerator.getGuemesStoreZ();

        Set<Long> examined =
                new HashSet<>();

        int validations = 0;

        System.out.println(
                "[EarthBound] Searching for Alpha "
                + "residential properties..."
        );

        /*
         * Search outward from the General Store.
         */

        for (int radius = SEARCH_STEP;
             radius <= SEARCH_RADIUS;
             radius += SEARCH_STEP) {

            for (int offsetX = -radius;
                 offsetX <= radius;
                 offsetX += SEARCH_STEP) {

                for (int offsetZ = -radius;
                     offsetZ <= radius;
                     offsetZ += SEARCH_STEP) {

                    /*
                     * Search the perimeter of each ring.
                     */

                    if (Math.abs(offsetX) != radius
                            && Math.abs(offsetZ) != radius) {

                        continue;
                    }

                    int frontX = centerX + offsetX;
                    int frontZ = centerZ + offsetZ;

                    long key =
                            coordinateKey(frontX, frontZ);

                    if (!examined.add(key)) {
                        continue;
                    }

                    if (EarthBuildingGenerator
                            .isInsideGuemesStoreBlendArea(
                                    frontX,
                                    frontZ
                            )) {

                        continue;
                    }

                    for (BlockFace facing : ALLOWED_FACING) {

                        if (selected.size()
                                >= PROPERTY_IDS.length) {

                            return finishSearch(selected);
                        }

                        String propertyId =
                                PROPERTY_IDS[selected.size()];

                        int groundY =
                                estimateGroundHeight(
                                        frontX,
                                        frontZ,
                                        facing
                                );

                        if (groundY < MINIMUM_BUILD_HEIGHT
                                || groundY
                                        > MAXIMUM_BUILD_HEIGHT) {

                            continue;
                        }

                        ResidentialSite candidate =
                                new ResidentialSite(
                                        propertyId,
                                        frontX,
                                        frontZ,
                                        groundY,
                                        facing
                                );

                        /*
                         * Avoid overlapping previously
                         * selected residential properties.
                         */

                        if (!hasPropertyClearance(
                                candidate,
                                selected
                        )) {

                            continue;
                        }

                        /*
                         * Quickly reject locations
                         * without nearby indexed roads.
                         */

                        if (!hasRoadAccess(candidate)) {

                            continue;
                        }

                        validations++;

                        if (validations
                                > MAX_CANDIDATE_VALIDATIONS) {

                            System.out.println(
                                    "[EarthBound] Residential "
                                    + "validation limit reached."
                            );

                            return finishSearch(selected);
                        }

                        /*
                         * Protect smoothed road terrain.
                         */

                        if (!hasProtectedRoadClearance(
                                candidate
                        )) {

                            continue;
                        }

                        /*
                         * Run existing residential
                         * terrain and water validation.
                         */

                        if (!EarthAlphaResidentialGenerator
                                .isSuitableSite(candidate)) {

                            continue;
                        }

                        /*
                         * A candidate has passed
                         * the initial geographic checks.
                         */

                        selected.add(candidate);

                        printSite(candidate);

                        if (selected.size()
                                == PROPERTY_IDS.length) {

                            return finishSearch(selected);
                        }
                    }
                }
            }
        }

        return finishSearch(selected);
    }

    /*
     * ========================================================
     * ESTIMATE PROPERTY FOUNDATION HEIGHT
     * ========================================================
     */

    private static int estimateGroundHeight(
            int frontX,
            int frontZ,
            BlockFace facing
    ) {

        BlockFace right =
                facing == BlockFace.NORTH
                        ? BlockFace.EAST
                        : BlockFace.WEST;

        BlockFace inward =
                facing.getOppositeFace();

        int totalHeight = 0;
        int sampleCount = 0;

        /*
         * Sample nine positions across the
         * central portion of the property.
         */

        for (int x = 4; x <= 20; x += 8) {

            for (int z = 6; z <= 26; z += 10) {

                int worldX =
                        frontX
                        + right.getModX() * x
                        + inward.getModX() * z;

                int worldZ =
                        frontZ
                        + right.getModZ() * x
                        + inward.getModZ() * z;

                if (isWater(worldX, worldZ)) {

                    return -1;
                }

                int height =
                        getNaturalHeight(
                                worldX,
                                worldZ
                        );

                if (height < MINIMUM_BUILD_HEIGHT
                        || height
                                > MAXIMUM_BUILD_HEIGHT) {

                    return -1;
                }

                totalHeight += height;
                sampleCount++;
            }
        }

        if (sampleCount == 0) {

            return -1;
        }

        return (int) Math.round(
                totalHeight / (double) sampleCount
        );
    }

    /*
     * ========================================================
     * PROPERTY SEPARATION
     * ========================================================
     */

    private static boolean hasPropertyClearance(
            ResidentialSite candidate,
            List<ResidentialSite> selected
    ) {

        for (ResidentialSite existing : selected) {

            boolean overlapsWithBuffer =

                    candidate.getMinX()
                            <= existing.getMaxX()
                                    + MINIMUM_PROPERTY_GAP

                    && candidate.getMaxX()
                            >= existing.getMinX()
                                    - MINIMUM_PROPERTY_GAP

                    && candidate.getMinZ()
                            <= existing.getMaxZ()
                                    + MINIMUM_PROPERTY_GAP

                    && candidate.getMaxZ()
                            >= existing.getMinZ()
                                    - MINIMUM_PROPERTY_GAP;

            if (overlapsWithBuffer) {

                return false;
            }

            /*
             * Protect the potential road access
             * corridor of both properties.
             */

            if (accessCorridorIntersects(
                    candidate,
                    existing
            ) || accessCorridorIntersects(
                    existing,
                    candidate
            )) {

                return false;
            }
        }

        return true;
    }

    private static boolean accessCorridorIntersects(
            ResidentialSite source,
            ResidentialSite other
    ) {

        for (int x = DRIVEWAY_MIN_X;
             x <= DRIVEWAY_MAX_X;
             x++) {

            for (int distance = 1;
                 distance <= MAX_ACCESS_DISTANCE;
                 distance++) {

                int worldX =
                        source.getWorldX(
                                x,
                                -distance
                        );

                int worldZ =
                        source.getWorldZ(
                                x,
                                -distance
                        );

                if (worldX >= other.getMinX()
                        && worldX <= other.getMaxX()
                        && worldZ >= other.getMinZ()
                        && worldZ <= other.getMaxZ()) {

                    return true;
                }
            }
        }

        return false;
    }

    /*
     * ========================================================
     * VERIFY POTENTIAL DRIVEWAY ROAD ACCESS
     * ========================================================
     */

    private static boolean hasRoadAccess(
            ResidentialSite site
    ) {

        /*
         * Check all five driveway lanes.
         *
         * Each lane must approach an indexed
         * road without crossing water or
         * excessively steep terrain.
         */

        for (int x = DRIVEWAY_MIN_X;
             x <= DRIVEWAY_MAX_X;
             x++) {

            int previousHeight =
                    EarthAlphaResidentialGenerator
                            .getSurfaceY(
                                    site,
                                    x,
                                    0
                            );

            boolean connected = false;

            for (int distance = 1;
                 distance <= MAX_ACCESS_DISTANCE;
                 distance++) {

                int worldX =
                        site.getWorldX(
                                x,
                                -distance
                        );

                int worldZ =
                        site.getWorldZ(
                                x,
                                -distance
                        );

                if (EarthBuildingGenerator
                        .isInsideGuemesStoreBlendArea(
                                worldX,
                                worldZ
                        )) {

                    return false;
                }

                if (isWater(worldX, worldZ)) {

                    return false;
                }

                int naturalHeight =
                        getNaturalHeight(
                                worldX,
                                worldZ
                        );

                if (naturalHeight < 64
                        || naturalHeight
                                > MAXIMUM_BUILD_HEIGHT) {

                    return false;
                }

                RoadColumn road =
                        EarthRoadSpatialIndex
                                .getRoadColumn(
                                        worldX,
                                        worldZ
                                );

                int currentHeight =
                        naturalHeight;

                RoadType fallbackType =
                        getOriginalRoadType(
                                worldX,
                                worldZ
                        );

                boolean originalRoad =
                        fallbackType != RoadType.NONE;

                boolean originalDrivingSurface =
                        isOriginalDrivingSurface(
                                worldX,
                                worldZ,
                                fallbackType
                        );

                boolean reachedRoad = false;

                if (road != null
                        && road.getRoadHeight() >= 64
                        && road.getRoadHeight()
                                <= MAXIMUM_BUILD_HEIGHT) {

                    if (road.isDrivingSurface()) {

                        currentHeight =
                                road.getRoadHeight();

                        reachedRoad = true;

                    } else if (road.isRoadBorder()) {

                        if (!originalDrivingSurface) {

                            currentHeight =
                                    road.getRoadHeight();
                        }

                        reachedRoad = true;

                    } else if (!originalRoad) {

                        currentHeight =
                                road.getBlendedTerrainHeight(
                                        naturalHeight
                                );

                        currentHeight =
                                Math.max(
                                        64,
                                        currentHeight
                                );
                    }
                }

                /*
                 * Require a walkable connection
                 * with no vertical steps above
                 * one Minecraft block.
                 */

                if (Math.abs(
                        currentHeight - previousHeight
                ) > 1) {

                    return false;
                }

                previousHeight =
                        currentHeight;

                if (reachedRoad) {

                    connected = true;

                    break;
                }

                /*
                 * Do not cross an unrelated
                 * fallback road to reach an
                 * indexed road.
                 */

                if (originalRoad) {

                    return false;
                }
            }

            if (!connected) {

                return false;
            }
        }

        return true;
    }

    /*
     * ========================================================
     * PROTECT SMOOTHED ROADS AND SHOULDERS
     * ========================================================
     */

    private static boolean hasProtectedRoadClearance(
            ResidentialSite site
    ) {

        for (int x = 0; x < PLOT_WIDTH; x++) {

            for (int z = 0; z < PLOT_LENGTH; z++) {

                int worldX =
                        site.getWorldX(x, z);

                int worldZ =
                        site.getWorldZ(x, z);

                RoadColumn road =
                        EarthRoadSpatialIndex
                                .getRoadColumn(
                                        worldX,
                                        worldZ
                                );

                /*
                 * Preserve the complete indexed
                 * road surface and terrain
                 * blending area.
                 */

                if (road != null) {

                    return false;
                }
            }
        }

        return true;
    }

    /*
     * ========================================================
     * ORIGINAL ROAD DETECTION
     * ========================================================
     */

    private static RoadType getOriginalRoadType(
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

        RoadType type =
                EarthRoadData.getNearestRoadType(
                        latitude,
                        longitude
                );

        if (type == RoadType.NONE) {

            return RoadType.NONE;
        }

        double distance =
                EarthRoadData
                        .getDistanceToRoadTypeMeters(
                                type,
                                latitude,
                                longitude
                        );

        double borderRadius =
                type.getWidth()
                        * EarthCoordinates.METERS_PER_BLOCK
                        / 2.0
                + EarthCoordinates.METERS_PER_BLOCK;

        if (!Double.isFinite(distance)
                || distance > borderRadius) {

            return RoadType.NONE;
        }

        return type;
    }

    private static boolean isOriginalDrivingSurface(
            int worldX,
            int worldZ,
            RoadType type
    ) {

        if (type == RoadType.NONE) {

            return false;
        }

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

        double distance =
                EarthRoadData
                        .getDistanceToRoadTypeMeters(
                                type,
                                latitude,
                                longitude
                        );

        double drivingRadius =
                type.getWidth()
                        * EarthCoordinates.METERS_PER_BLOCK
                        / 2.0;

        return Double.isFinite(distance)
                && distance <= drivingRadius;
    }

    /*
     * ========================================================
     * GEOGRAPHIC DATA HELPERS
     * ========================================================
     */

    private static boolean isWater(
            int worldX,
            int worldZ
    ) {

        return EarthWaterGenerator.isWater(
                EarthCoordinates.getLatitude(
                        worldX,
                        worldZ
                ),
                EarthCoordinates.getLongitude(
                        worldX,
                        worldZ
                )
        );
    }

    private static int getNaturalHeight(
            int worldX,
            int worldZ
    ) {

        return EarthTerrainGenerator
                .getTerrainHeight(
                        EarthCoordinates.getLatitude(
                                worldX,
                                worldZ
                        ),
                        EarthCoordinates.getLongitude(
                                worldX,
                                worldZ
                        )
                );
    }

    private static long coordinateKey(
            int x,
            int z
    ) {

        return ((long) x << 32)
                | (z & 0xffffffffL);
    }

    /*
     * ========================================================
     * RESIDENTIAL SEARCH RESULTS
     * ========================================================
     */

    private static List<ResidentialSite> finishSearch(
            List<ResidentialSite> selected
    ) {

        System.out.println(
                "[EarthBound] Residential search finished."
        );

        System.out.println(
                "[EarthBound] Candidate properties found: "
                + selected.size()
                + " / "
                + PROPERTY_IDS.length
        );

        if (selected.size() != PROPERTY_IDS.length) {

            System.out.println(
                    "[EarthBound] WARNING: Not all six "
                    + "residential properties were located."
            );
        }

        System.out.println(
                "[EarthBound] All locations require "
                + "review before construction."
        );

        return List.copyOf(selected);
    }

    /*
     * ========================================================
     * PRINT CONFIGURATION COORDINATES
     * ========================================================
     */

    private static void printSite(
            ResidentialSite site
    ) {

        System.out.println(
                "================================================"
        );

        System.out.println(
                "[EarthBound] Residential candidate found!"
        );

        System.out.println(
                "Property: " + site.getPropertyId()
        );

        System.out.println(
                "Design: " + site.getDesign().name()
        );

        System.out.println(
                "Latitude: "
                + EarthCoordinates.getLatitude(
                        site.getWorldX(0, 0),
                        site.getWorldZ(0, 0)
                )
        );

        System.out.println(
                "Longitude: "
                + EarthCoordinates.getLongitude(
                        site.getWorldX(0, 0),
                        site.getWorldZ(0, 0)
                )
        );

        System.out.println(
                "Configuration coordinates:"
        );

        System.out.println(
                "enabled: false"
        );

        System.out.println(
                "world: earthbound"
        );

        System.out.println(
                "width: " + PLOT_WIDTH
        );

        System.out.println(
                "length: " + PLOT_LENGTH
        );

        System.out.println(
                "design: " + site.getDesign().name()
        );

        System.out.println(
                "min-x: " + site.getMinX()
        );

        System.out.println(
                "min-z: " + site.getMinZ()
        );

        System.out.println(
                "front-x: " + site.getWorldX(0, 0)
        );

        System.out.println(
                "front-z: " + site.getWorldZ(0, 0)
        );

        System.out.println(
                "ground-y: " + site.getGroundY()
        );

        System.out.println(
                "facing: " + site.getFacing().name()
        );

        System.out.println(
                "banner-x: " + site.getBannerX()
        );

        System.out.println(
                "banner-y: " + site.getBannerY()
        );

        System.out.println(
                "banner-z: " + site.getBannerZ()
        );

        System.out.println(
                "sign-x: " + site.getSignX()
        );

        System.out.println(
                "sign-y: " + site.getSignY()
        );

        System.out.println(
                "sign-z: " + site.getSignZ()
        );

        System.out.println(
                "marker-facing: "
                + site.getFacing().name()
        );

        System.out.println(
                "================================================"
        );
    }
}
