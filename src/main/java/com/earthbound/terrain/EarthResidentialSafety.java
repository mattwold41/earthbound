package com.earthbound;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import com.earthbound.EarthAlphaResidentialGenerator
        .ResidentialSite;

import com.earthbound.roads.EarthRoadData;
import com.earthbound.roads.EarthRoadSpatialIndex;
import com.earthbound.terrain.EarthTerrainLoader;
import com.earthbound.water.EarthWaterGenerator;

/*
 * ============================================================
 * EARTHBOUND RESIDENTIAL CONSTRUCTION SAFETY
 * Friends Alpha
 * ============================================================
 *
 * Read-only construction safety inspection.
 *
 * Protects:
 *
 * - Existing generated chunks
 * - Existing loaded chunks
 * - Existing player development
 * - General Store terrain
 * - Road surfaces
 * - Road shoulders
 * - Water areas
 * - Residential property boundaries
 *
 * This class does not modify blocks, load chunks,
 * generate terrain or approve construction.
 * ============================================================
 */

public final class EarthResidentialSafety {

    private static final String RESIDENTIAL_WORLD =
            "earthbound";

    private static final int REQUIRED_HOMES = 6;

    private static final int PLOT_WIDTH = 24;

    private static final int PLOT_LENGTH = 32;

    private static final int MINIMUM_BUILD_HEIGHT = 66;

    private static final int MAXIMUM_BUILD_HEIGHT = 280;

    private static final int MINIMUM_PROPERTY_GAP = 8;

    private static final int MAXIMUM_WORLD_COORDINATE =
            29999900;

    /*
     * Conservative terrain and structure clearance.
     */

    private static final int LOWER_BUILD_CLEARANCE = 12;

    private static final int UPPER_BUILD_CLEARANCE = 12;

    private EarthResidentialSafety() {
    }

    /*
     * ========================================================
     * SAFETY INSPECTION RESULT
     * ========================================================
     */

    public enum SafetyStatus {

        SAFE_UNGENERATED_SITE,

        INVALID_SITE,

        INVALID_WORLD,

        MAIN_THREAD_REQUIRED,

        GEOGRAPHIC_DATA_UNAVAILABLE,

        INVALID_PROPERTY_COUNT,

        DUPLICATE_PROPERTY,

        PROPERTY_OVERLAP,

        INSUFFICIENT_PROPERTY_CLEARANCE,

        INVALID_PROPERTY_COORDINATES,

        INVALID_PROPERTY_DIRECTION,

        INVALID_PROPERTY_DIMENSIONS,

        INVALID_BUILD_HEIGHT,

        STORE_PROTECTION_CONFLICT,

        ROAD_PROTECTION_CONFLICT,

        WATER_CONFLICT,

        UNSUITABLE_TERRAIN,

        EXISTING_LOADED_CHUNK,

        EXISTING_GENERATED_CHUNK,

        INSPECTION_FAILED
    }

    public static final class SafetyResult {

        private final SafetyStatus status;

        private final String message;

        private SafetyResult(
                SafetyStatus status,
                String message
        ) {

            this.status = status;
            this.message = message;
        }

        public SafetyStatus getStatus() {

            return status;
        }

        public String getMessage() {

            return message;
        }

        public boolean isSafe() {

            return status
                    == SafetyStatus.SAFE_UNGENERATED_SITE;
        }
    }

    /*
     * ========================================================
     * INSPECT COMPLETE ALPHA DEVELOPMENT
     * ========================================================
     */

    public static SafetyResult inspectDevelopment(
            World world,
            List<ResidentialSite> sites,
            boolean geographicDataReady
    ) {

        if (!Bukkit.isPrimaryThread()) {

            return failure(
                    SafetyStatus.MAIN_THREAD_REQUIRED,
                    "Construction inspection requires "
                    + "the server main thread."
            );
        }

        if (world == null
                || !RESIDENTIAL_WORLD.equals(
                        world.getName()
                )) {

            return failure(
                    SafetyStatus.INVALID_WORLD,
                    "Residential world is unavailable "
                    + "or incorrect."
            );
        }

        if (!geographicDataReady
                || !isGeographicDataReady()) {

            return failure(
                    SafetyStatus.GEOGRAPHIC_DATA_UNAVAILABLE,
                    "Terrain, water and road initialization "
                    + "must succeed before inspection."
            );
        }

        if (sites == null
                || sites.size() != REQUIRED_HOMES) {

            return failure(
                    SafetyStatus.INVALID_PROPERTY_COUNT,
                    "Exactly six Alpha properties "
                    + "are required."
            );
        }

        Set<String> propertyIds =
                new HashSet<>();

        for (int i = 0; i < sites.size(); i++) {

            ResidentialSite site =
                    sites.get(i);

            if (site == null) {

                return failure(
                        SafetyStatus.INVALID_SITE,
                        "Residential site is missing."
                );
            }

            if (!propertyIds.add(
                    site.getPropertyId()
            )) {

                return failure(
                        SafetyStatus.DUPLICATE_PROPERTY,
                        "Duplicate property: "
                        + site.getPropertyId()
                );
            }

            /*
             * Inspect the individual property before
             * comparing it with other properties.
             */

            SafetyResult result =
                    inspectSite(
                            world,
                            site,
                            geographicDataReady
                    );

            if (!result.isSafe()) {

                return result;
            }

            /*
             * Prevent overlapping or excessively
             * close residential properties.
             */

            for (int j = 0; j < i; j++) {

                ResidentialSite previous =
                        sites.get(j);

                if (site.overlaps(previous)) {

                    return failure(
                            SafetyStatus.PROPERTY_OVERLAP,
                            site.getPropertyId()
                            + " overlaps "
                            + previous.getPropertyId()
                    );
                }

                if (!hasPropertyClearance(
                        site,
                        previous
                )) {

                    return failure(
                            SafetyStatus
                                    .INSUFFICIENT_PROPERTY_CLEARANCE,
                            site.getPropertyId()
                            + " is too close to "
                            + previous.getPropertyId()
                    );
                }
            }
        }

        return success(
                "All six Alpha properties passed "
                + "the read-only safety inspection."
        );
    }

    /*
     * ========================================================
     * INSPECT INDIVIDUAL RESIDENTIAL PROPERTY
     * ========================================================
     */

    public static SafetyResult inspectSite(
            World world,
            ResidentialSite site,
            boolean geographicDataReady
    ) {

        if (!Bukkit.isPrimaryThread()) {

            return failure(
                    SafetyStatus.MAIN_THREAD_REQUIRED,
                    "Residential inspection must run "
                    + "on the server main thread."
            );
        }

        if (world == null
                || !RESIDENTIAL_WORLD.equals(
                        world.getName()
                )) {

            return failure(
                    SafetyStatus.INVALID_WORLD,
                    "Incorrect residential world."
            );
        }

        if (site == null) {

            return failure(
                    SafetyStatus.INVALID_SITE,
                    "Residential property is missing."
            );
        }

        if (!geographicDataReady
                || !isGeographicDataReady()) {

            return failure(
                    SafetyStatus.GEOGRAPHIC_DATA_UNAVAILABLE,
                    "Geographic systems are unavailable."
            );
        }

        try {

            SafetyResult geography =
                    inspectGeography(site);

            if (!geography.isSafe()) {

                return geography;
            }

            SafetyResult height =
                    inspectBuildHeight(
                            world,
                            site
                    );

            if (!height.isSafe()) {

                return height;
            }

            SafetyResult chunks =
                    inspectExistingChunks(
                            world,
                            site
                    );

            if (!chunks.isSafe()) {

                return chunks;
            }

            return success(
                    site.getPropertyId()
                    + " passed residential safety inspection."
            );

        } catch (RuntimeException exception) {

            return failure(
                    SafetyStatus.INSPECTION_FAILED,
                    site.getPropertyId()
                    + " inspection failed: "
                    + exception.getClass().getSimpleName()
                    + " - "
                    + exception.getMessage()
            );
        }
    }

    /*
     * ========================================================
     * GEOGRAPHIC SAFETY INSPECTION
     * ========================================================
     */

    public static SafetyResult inspectGeography(
            ResidentialSite site
    ) {

        if (site == null) {

            return failure(
                    SafetyStatus.INVALID_SITE,
                    "Residential property is missing."
            );
        }

        if (!isGeographicDataReady()) {

            return failure(
                    SafetyStatus.GEOGRAPHIC_DATA_UNAVAILABLE,
                    "Geographic systems are unavailable."
            );
        }

        String propertyId =
                site.getPropertyId();

        /*
         * Current ownership configuration supports
         * north and south facing residential plots.
         */

        if (site.getFacing() != BlockFace.NORTH
                && site.getFacing() != BlockFace.SOUTH) {

            return failure(
                    SafetyStatus.INVALID_PROPERTY_DIRECTION,
                    propertyId
                    + " has an unsupported facing direction."
            );
        }

        int frontX =
                site.getWorldX(0, 0);

        int frontZ =
                site.getWorldZ(0, 0);

        /*
         * Reject unreasonable coordinates before
         * calculating complete property boundaries.
         */

        if (Math.abs((long) frontX)
                        > MAXIMUM_WORLD_COORDINATE

                || Math.abs((long) frontZ)
                        > MAXIMUM_WORLD_COORDINATE) {

            return failure(
                    SafetyStatus.INVALID_PROPERTY_COORDINATES,
                    propertyId
                    + " has invalid world coordinates."
            );
        }

        long width =
                (long) site.getMaxX()
                - site.getMinX()
                + 1L;

        long length =
                (long) site.getMaxZ()
                - site.getMinZ()
                + 1L;

        if (width != PLOT_WIDTH
                || length != PLOT_LENGTH) {

            return failure(
                    SafetyStatus.INVALID_PROPERTY_DIMENSIONS,
                    propertyId
                    + " does not match required plot dimensions."
            );
        }

        if (site.getGroundY() < MINIMUM_BUILD_HEIGHT
                || site.getGroundY()
                        > MAXIMUM_BUILD_HEIGHT) {

            return failure(
                    SafetyStatus.INVALID_BUILD_HEIGHT,
                    propertyId
                    + " has an invalid foundation elevation."
            );
        }

        /*
         * Inspect every column in the residential plot.
         *
         * Protect the complete store blending area,
         * indexed road surfaces and road shoulders.
         */

        for (int x = 0; x < PLOT_WIDTH; x++) {

            for (int z = 0; z < PLOT_LENGTH; z++) {

                int worldX =
                        site.getWorldX(x, z);

                int worldZ =
                        site.getWorldZ(x, z);

                if (EarthBuildingGenerator
                        .isInsideGuemesStoreBlendArea(
                                worldX,
                                worldZ
                        )) {

                    return failure(
                            SafetyStatus
                                    .STORE_PROTECTION_CONFLICT,
                            propertyId
                            + " intersects protected store terrain "
                            + "at "
                            + worldX + ", " + worldZ
                    );
                }

                if (EarthRoadSpatialIndex
                        .getRoadColumn(
                                worldX,
                                worldZ
                        ) != null) {

                    return failure(
                            SafetyStatus
                                    .ROAD_PROTECTION_CONFLICT,
                            propertyId
                            + " intersects a protected road "
                            + "or road shoulder at "
                            + worldX + ", " + worldZ
                    );
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

                if (EarthWaterGenerator.isWater(
                        latitude,
                        longitude
                )) {

                    return failure(
                            SafetyStatus.WATER_CONFLICT,
                            propertyId
                            + " intersects water at "
                            + worldX + ", " + worldZ
                    );
                }
            }
        }

        /*
         * Preserve the existing residential terrain
         * validation system, including original roads.
         */

        if (!EarthAlphaResidentialGenerator
                .isSuitableSite(site)) {

            return failure(
                    SafetyStatus.UNSUITABLE_TERRAIN,
                    propertyId
                    + " failed residential terrain validation."
            );
        }

        return success(
                propertyId
                + " passed geographic inspection only."
        );
    }

    /*
     * ========================================================
     * WORLD BUILD HEIGHT INSPECTION
     * ========================================================
     */

    private static SafetyResult inspectBuildHeight(
            World world,
            ResidentialSite site
    ) {

        long lowestRequiredY =
                (long) site.getGroundY()
                - LOWER_BUILD_CLEARANCE;

        long highestRequiredY =
                (long) site.getGroundY()
                + site.getDesign().getMaximumHeight()
                + UPPER_BUILD_CLEARANCE;

        if (lowestRequiredY < world.getMinHeight()
                || highestRequiredY
                        >= world.getMaxHeight()) {

            return failure(
                    SafetyStatus.INVALID_BUILD_HEIGHT,
                    site.getPropertyId()
                    + " exceeds the world's build height limits."
            );
        }

        return success(
                site.getPropertyId()
                + " passed build height inspection only."
        );
    }

    /*
     * ========================================================
     * EXISTING CHUNK PROTECTION
     * ========================================================
     *
     * Do not call getChunkAt().
     *
     * Do not load or generate chunks during inspection.
     *
     * Reject the entire property if any required chunk
     * is already loaded or previously generated.
     * ========================================================
     */

    public static SafetyResult inspectExistingChunks(
            World world,
            ResidentialSite site
    ) {

        if (!Bukkit.isPrimaryThread()) {

            return failure(
                    SafetyStatus.MAIN_THREAD_REQUIRED,
                    "Chunk inspection requires "
                    + "the server main thread."
            );
        }

        if (world == null
                || !RESIDENTIAL_WORLD.equals(
                        world.getName()
                )) {

            return failure(
                    SafetyStatus.INVALID_WORLD,
                    "Incorrect residential world."
            );
        }

        if (site == null) {

            return failure(
                    SafetyStatus.INVALID_SITE,
                    "Residential property is missing."
            );
        }

        long width =
                (long) site.getMaxX()
                - site.getMinX()
                + 1L;

        long length =
                (long) site.getMaxZ()
                - site.getMinZ()
                + 1L;

        if (width != PLOT_WIDTH
                || length != PLOT_LENGTH) {

            return failure(
                    SafetyStatus.INVALID_PROPERTY_DIMENSIONS,
                    "Invalid residential chunk boundaries."
            );
        }

        /*
         * floorDiv is important because EarthBound
         * uses negative Minecraft coordinates.
         */

        int minChunkX =
                Math.floorDiv(
                        site.getMinX(),
                        16
                );

        int maxChunkX =
                Math.floorDiv(
                        site.getMaxX(),
                        16
                );

        int minChunkZ =
                Math.floorDiv(
                        site.getMinZ(),
                        16
                );

        int maxChunkZ =
                Math.floorDiv(
                        site.getMaxZ(),
                        16
                );

        try {

            for (int chunkX = minChunkX;
                 chunkX <= maxChunkX;
                 chunkX++) {

                for (int chunkZ = minChunkZ;
                     chunkZ <= maxChunkZ;
                     chunkZ++) {

                    if (world.isChunkLoaded(
                            chunkX,
                            chunkZ
                    )) {

                        return failure(
                                SafetyStatus
                                        .EXISTING_LOADED_CHUNK,
                                site.getPropertyId()
                                + " intersects loaded chunk "
                                + chunkX + ", " + chunkZ
                        );
                    }

                    if (world.isChunkGenerated(
                            chunkX,
                            chunkZ
                    )) {

                        return failure(
                                SafetyStatus
                                        .EXISTING_GENERATED_CHUNK,
                                site.getPropertyId()
                                + " intersects previously "
                                + "generated chunk "
                                + chunkX + ", " + chunkZ
                        );
                    }
                }
            }

        } catch (RuntimeException exception) {

            return failure(
                    SafetyStatus.INSPECTION_FAILED,
                    "Unable to verify existing chunks for "
                    + site.getPropertyId()
                    + ": "
                    + exception.getMessage()
            );
        }

        return success(
                site.getPropertyId()
                + " passed existing chunk inspection only."
        );
    }

    /*
     * ========================================================
     * PROPERTY CLEARANCE
     * ========================================================
     */

    private static boolean hasPropertyClearance(
            ResidentialSite first,
            ResidentialSite second
    ) {

        boolean intersectsProtectedBuffer =

                (long) first.getMinX()
                        <= (long) second.getMaxX()
                                + MINIMUM_PROPERTY_GAP

                && (long) first.getMaxX()
                        >= (long) second.getMinX()
                                - MINIMUM_PROPERTY_GAP

                && (long) first.getMinZ()
                        <= (long) second.getMaxZ()
                                + MINIMUM_PROPERTY_GAP

                && (long) first.getMaxZ()
                        >= (long) second.getMinZ()
                                - MINIMUM_PROPERTY_GAP;

        return !intersectsProtectedBuffer;
    }

    /*
     * ========================================================
     * GEOGRAPHIC READINESS
     * ========================================================
     *
     * Water loading success is supplied separately by
     * EarthBound's geographic initialization system.
     * ========================================================
     */

    private static boolean isGeographicDataReady() {

        return EarthTerrainLoader.isLoaded()
                && EarthRoadData.isLoaded()
                && EarthRoadSpatialIndex.isLoaded();
    }

    /*
     * ========================================================
     * SAFETY RESULT HELPERS
     * ========================================================
     */

    private static SafetyResult success(
            String message
    ) {

        return new SafetyResult(
                SafetyStatus.SAFE_UNGENERATED_SITE,
                message
        );
    }

    private static SafetyResult failure(
            SafetyStatus status,
            String message
    ) {

        return new SafetyResult(
                status,
                message
        );
    }
}
