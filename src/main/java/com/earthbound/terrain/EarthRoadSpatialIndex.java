package com.earthbound.roads;

import com.earthbound.EarthCoordinates;
import com.earthbound.roads.EarthRoadData.RoadType;
import com.earthbound.roads.EarthRoadElevationProfiles.RoadProfile;
import com.earthbound.roads.EarthRoadElevationProfiles.RoadSample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * ============================================================
 * EARTHBOUND ROAD SPATIAL INDEX
 * Friends Alpha
 * ============================================================
 *
 * Provides efficient access to smoothed road elevations.
 *
 * Uses absolute Minecraft coordinates to ensure that
 * road lookups remain consistent across chunk boundaries.
 *
 * Does not modify Minecraft world blocks directly.
 * ============================================================
 */

public final class EarthRoadSpatialIndex {

    /*
     * Spatial index grid size in Minecraft blocks.
     */

    private static final int CELL_SIZE = 32;

    private static final double ROAD_BORDER_WIDTH = 1.0;

    private static final double METERS_PER_DEGREE_LATITUDE =
            111320.0;

    private static final double METERS_PER_DEGREE_LONGITUDE =
            METERS_PER_DEGREE_LATITUDE
                    * Math.cos(
                            Math.toRadians(
                                    EarthCoordinates.START_LAT
                            )
                    );

    private static volatile Map<Long, List<IndexedSegment>>
            index = Map.of();

    private static volatile boolean loaded = false;


    private EarthRoadSpatialIndex() {
    }


    /*
     * ========================================================
     * INDEXED ROAD SEGMENT
     * ========================================================
     */

    private static final class IndexedSegment {

        private final RoadType roadType;

        private final double startX;

        private final double startZ;

        private final double endX;

        private final double endZ;

        private final double startY;

        private final double endY;


        private IndexedSegment(
                RoadType roadType,
                RoadSample first,
                RoadSample second
        ) {

            this.roadType = roadType;

            this.startX =
                    longitudeToBlockX(
                            first.getLongitude()
                    );

            this.startZ =
                    latitudeToBlockZ(
                            first.getLatitude()
                    );

            this.endX =
                    longitudeToBlockX(
                            second.getLongitude()
                    );

            this.endZ =
                    latitudeToBlockZ(
                            second.getLatitude()
                    );

            this.startY =
                    first.getSmoothedElevation();

            this.endY =
                    second.getSmoothedElevation();

        }

    }


    /*
     * ========================================================
     * ROAD COLUMN RESULT
     * ========================================================
     */

    public static final class RoadColumn {

        private final RoadType roadType;

        private final double distance;

        private final double roadElevation;


        private RoadColumn(
                RoadType roadType,
                double distance,
                double roadElevation
        ) {

            this.roadType = roadType;

            this.distance = distance;

            this.roadElevation = roadElevation;

        }


        public RoadType getRoadType() {

            return roadType;

        }


        public double getDistanceBlocks() {

            return distance;

        }


        public double getRoadElevation() {

            return roadElevation;

        }


        public int getRoadHeight() {

            return (int) Math.round(
                    roadElevation
            );

        }


        public double getDrivingRadius() {

            return roadType.getRadius();

        }


        public double getBorderRadius() {

            return getDrivingRadius()
                    + ROAD_BORDER_WIDTH;

        }


        public boolean isDrivingSurface() {

            return distance
                    <= getDrivingRadius();

        }


        public boolean isRoadBorder() {

            return distance
                    > getDrivingRadius()
                    &&
                    distance
                            <= getBorderRadius();

        }


        public boolean isRoadSurface() {

            return distance
                    <= getBorderRadius();

        }


        public double getDistanceFromRoadEdge() {

            return Math.max(
                    0.0,
                    distance - getBorderRadius()
            );

        }


        public int getBlendedTerrainHeight(
                int naturalHeight
        ) {

            return EarthRoadSmoothing
                    .getBlendedTerrainHeight(
                            naturalHeight,
                            getRoadHeight(),
                            getDistanceFromRoadEdge()
                    );

        }

    }


    /*
     * ========================================================
     * BUILD SPATIAL INDEX
     * ========================================================
     */

    public static synchronized boolean load() {

        if (loaded) {

            return true;

        }

        if (!EarthRoadElevationProfiles.isLoaded()) {

            System.out.println(
                    "[EarthBound] Road spatial index requires "
                            + "loaded elevation profiles."
            );

            return false;

        }

        System.out.println(
                "[EarthBound] Building road spatial index..."
        );

        Map<Long, List<IndexedSegment>> building =
                new HashMap<>();

        long segmentCount = 0;

        try {

            for (
                    RoadProfile profile
                    : EarthRoadElevationProfiles.getProfiles()
            ) {

                List<RoadSample> samples =
                        profile.getSamples();

                RoadType type =
                        profile.getRoadType();

                if (type == null
                        || type == RoadType.NONE) {

                    continue;

                }

                for (
                        int i = 1;
                        i < samples.size();
                        i++
                ) {

                    IndexedSegment segment =
                            new IndexedSegment(
                                    type,
                                    samples.get(i - 1),
                                    samples.get(i)
                            );

                    if (!isValidSegment(segment)) {

                        continue;

                    }

                    registerSegment(
                            building,
                            segment
                    );

                    segmentCount++;

                }

            }

            if (segmentCount == 0) {

                System.out.println(
                        "[EarthBound] No road segments "
                                + "available for indexing."
                );

                return false;

            }


            /*
             * Publish immutable index data.
             */

            Map<Long, List<IndexedSegment>> completed =
                    new HashMap<>();

            for (
                    Map.Entry<Long, List<IndexedSegment>> entry
                    : building.entrySet()
            ) {

                completed.put(
                        entry.getKey(),
                        List.copyOf(entry.getValue())
                );

            }

            index = Map.copyOf(completed);

            loaded = true;

            System.out.println(
                    "[EarthBound] Road spatial index loaded!"
            );

            System.out.println(
                    "[EarthBound] Indexed segments: "
                            + segmentCount
                            + ", indexed cells: "
                            + index.size()
            );

            return true;

        } catch (RuntimeException exception) {

            index = Map.of();

            loaded = false;

            System.out.println(
                    "[EarthBound] Road spatial index failed: "
                            + exception.getMessage()
            );

            return false;

        }

    }


    /*
     * ========================================================
     * REGISTER ROAD SEGMENT IN NEARBY GRID CELLS
     * ========================================================
     */

    private static void registerSegment(
            Map<Long, List<IndexedSegment>> destination,
            IndexedSegment segment
    ) {

        double searchRadius =
                segment.roadType.getRadius()
                        + ROAD_BORDER_WIDTH
                        + EarthRoadSmoothing.TERRAIN_BLEND_DISTANCE;

        double minimumX =
                Math.min(
                        segment.startX,
                        segment.endX
                ) - searchRadius;

        double maximumX =
                Math.max(
                        segment.startX,
                        segment.endX
                ) + searchRadius;

        double minimumZ =
                Math.min(
                        segment.startZ,
                        segment.endZ
                ) - searchRadius;

        double maximumZ =
                Math.max(
                        segment.startZ,
                        segment.endZ
                ) + searchRadius;

        int minimumCellX =
                getCellCoordinate(minimumX);

        int maximumCellX =
                getCellCoordinate(maximumX);

        int minimumCellZ =
                getCellCoordinate(minimumZ);

        int maximumCellZ =
                getCellCoordinate(maximumZ);

        for (
                int cellX = minimumCellX;
                cellX <= maximumCellX;
                cellX++
        ) {

            for (
                    int cellZ = minimumCellZ;
                    cellZ <= maximumCellZ;
                    cellZ++
            ) {

                long key =
                        getCellKey(
                                cellX,
                                cellZ
                        );

                destination.computeIfAbsent(
                        key,
                        ignored -> new ArrayList<>()
                ).add(segment);

            }

        }

    }


    /*
     * ========================================================
     * FIND ROAD ELEVATION AT MINECRAFT COORDINATES
     * ========================================================
     */

    public static RoadColumn getRoadColumn(
            int worldX,
            int worldZ
    ) {

        if (!loaded) {

            return null;

        }

        Map<Long, List<IndexedSegment>> currentIndex =
                index;

        int cellX =
                getCellCoordinate(worldX);

        int cellZ =
                getCellCoordinate(worldZ);

        List<IndexedSegment> candidates =
                currentIndex.get(
                        getCellKey(
                                cellX,
                                cellZ
                        )
                );

        if (candidates == null
                || candidates.isEmpty()) {

            return null;

        }

        RoadColumn best = null;

        for (IndexedSegment segment : candidates) {

            RoadColumn candidate =
                    evaluateSegment(
                            worldX,
                            worldZ,
                            segment
                    );

            if (candidate == null) {

                continue;

            }

            if (isBetterCandidate(
                    candidate,
                    best
            )) {

                best = candidate;

            }

        }

        return best;

    }


    /*
     * ========================================================
     * CALCULATE NEAREST POINT AND ROAD ELEVATION
     * ========================================================
     */

    private static RoadColumn evaluateSegment(
            int worldX,
            int worldZ,
            IndexedSegment segment
    ) {

        double dx =
                segment.endX - segment.startX;

        double dz =
                segment.endZ - segment.startZ;

        double lengthSquared =
                dx * dx + dz * dz;

        if (lengthSquared <= 0.00000001) {

            return null;

        }

        double progress =
                (
                        (worldX - segment.startX) * dx
                                +
                        (worldZ - segment.startZ) * dz
                ) / lengthSquared;

        progress =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                progress
                        )
                );

        double nearestX =
                segment.startX
                        + dx * progress;

        double nearestZ =
                segment.startZ
                        + dz * progress;

        double distance =
                Math.hypot(
                        worldX - nearestX,
                        worldZ - nearestZ
                );

        double searchRadius =
                segment.roadType.getRadius()
                        + ROAD_BORDER_WIDTH
                        + EarthRoadSmoothing.TERRAIN_BLEND_DISTANCE;

        if (distance > searchRadius) {

            return null;

        }

        double elevation =
                EarthRoadSmoothing.interpolateElevation(
                        segment.startY,
                        segment.endY,
                        progress
                );

        if (!Double.isFinite(elevation)) {

            return null;

        }

        return new RoadColumn(
                segment.roadType,
                distance,
                elevation
        );

    }


    /*
     * ========================================================
     * ROAD SELECTION PRIORITY
     * ========================================================
     *
     * Driving surface beats roadside terrain.
     * Road border beats roadside terrain.
     * Within the same surface category, higher road
     * classes take priority.
     */

    private static boolean isBetterCandidate(
            RoadColumn candidate,
            RoadColumn current
    ) {

        if (current == null) {

            return true;

        }

        int candidateSurface =
                getSurfacePriority(candidate);

        int currentSurface =
                getSurfacePriority(current);

        if (candidateSurface != currentSurface) {

            return candidateSurface
                    > currentSurface;

        }

        if (!candidate.isRoadSurface()) {

            return candidate.getDistanceFromRoadEdge()
                    < current.getDistanceFromRoadEdge();

        }

        int candidateClass =
                candidate.getRoadType().getWidth();

        int currentClass =
                current.getRoadType().getWidth();

        if (candidateClass != currentClass) {

            return candidateClass
                    > currentClass;

        }

        return candidate.getDistanceBlocks()
                < current.getDistanceBlocks();

    }


    private static int getSurfacePriority(
            RoadColumn column
    ) {

        if (column.isDrivingSurface()) {

            return 3;

        }

        if (column.isRoadBorder()) {

            return 2;

        }

        return 1;

    }


    /*
     * ========================================================
     * SEGMENT VALIDATION
     * ========================================================
     */

    private static boolean isValidSegment(
            IndexedSegment segment
    ) {

        if (!Double.isFinite(segment.startX)
                || !Double.isFinite(segment.startZ)
                || !Double.isFinite(segment.endX)
                || !Double.isFinite(segment.endZ)
                || !Double.isFinite(segment.startY)
                || !Double.isFinite(segment.endY)) {

            return false;

        }

        return Math.hypot(
                segment.endX - segment.startX,
                segment.endZ - segment.startZ
        ) > 0.0001;

    }


    /*
     * ========================================================
     * FRACTIONAL MINECRAFT COORDINATE CONVERSION
     * ========================================================
     *
     * Preserves the existing EarthCoordinates anchor
     * without rounding road vertices to whole blocks.
     */

    private static double longitudeToBlockX(
            double longitude
    ) {

        return EarthCoordinates.START_X
                +
                (
                        (longitude - EarthCoordinates.START_LON)
                                * METERS_PER_DEGREE_LONGITUDE
                ) / EarthCoordinates.METERS_PER_BLOCK;

    }


    private static double latitudeToBlockZ(
            double latitude
    ) {

        return EarthCoordinates.START_Z
                -
                (
                        (latitude - EarthCoordinates.START_LAT)
                                * METERS_PER_DEGREE_LATITUDE
                ) / EarthCoordinates.METERS_PER_BLOCK;

    }


    /*
     * ========================================================
     * GRID COORDINATES
     * ========================================================
     */

    private static int getCellCoordinate(
            double coordinate
    ) {

        return (int) Math.floor(
                coordinate / CELL_SIZE
        );

    }


    private static long getCellKey(
            int cellX,
            int cellZ
    ) {

        return ((long) cellX << 32)
                | (cellZ & 0xffffffffL);

    }


    /*
     * ========================================================
     * STATUS
     * ========================================================
     */

    public static boolean isLoaded() {

        return loaded;

    }


    public static int getIndexedCellCount() {

        return index.size();

    }


    public static synchronized void clear() {

        loaded = false;

        index = Map.of();

    }

}
