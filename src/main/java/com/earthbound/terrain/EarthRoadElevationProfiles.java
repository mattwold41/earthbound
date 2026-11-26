package com.earthbound.roads;

import com.earthbound.EarthCoordinates;
import com.earthbound.roads.EarthRoadData.RoadPoint;
import com.earthbound.roads.EarthRoadData.RoadType;
import com.earthbound.terrain.EarthTerrainLoader;

import java.util.ArrayList;
import java.util.List;

/*
 * ============================================================
 * EARTHBOUND ROAD ELEVATION PROFILES
 * Friends Alpha
 * ============================================================
 *
 * Samples real-world terrain along road centerlines.
 *
 * Uses the EarthBound road smoothing engine to create
 * continuous elevation profiles.
 *
 * Does not directly modify Minecraft world blocks.
 * ============================================================
 */

public final class EarthRoadElevationProfiles {

    private static final double WEST = -122.70;
    private static final double SOUTH = 48.47;
    private static final double EAST = -122.55;
    private static final double NORTH = 48.60;

    private static final double SEA_LEVEL = 63.0;

    private static final double SAMPLE_INTERVAL =
            EarthRoadSmoothing.SAMPLE_INTERVAL;

    private static final double METERS_PER_DEGREE_LATITUDE =
            111320.0;

    private static final double METERS_PER_DEGREE_LONGITUDE =
            METERS_PER_DEGREE_LATITUDE
                    * Math.cos(
                            Math.toRadians(
                                    EarthCoordinates.START_LAT
                            )
                    );

    private static final int MAX_PROFILE_SAMPLES =
            100000;

    private static volatile List<RoadProfile> profiles =
            List.of();

    private static volatile boolean loaded = false;


    private EarthRoadElevationProfiles() {
    }


    /*
     * ========================================================
     * ELEVATION SAMPLE
     * ========================================================
     */

    public static final class RoadSample {

        private final double latitude;

        private final double longitude;

        private final double distance;

        private final double naturalElevation;

        private final double smoothedElevation;


        private RoadSample(
                double latitude,
                double longitude,
                double distance,
                double naturalElevation,
                double smoothedElevation
        ) {

            this.latitude = latitude;

            this.longitude = longitude;

            this.distance = distance;

            this.naturalElevation = naturalElevation;

            this.smoothedElevation = smoothedElevation;

        }


        public double getLatitude() {

            return latitude;

        }


        public double getLongitude() {

            return longitude;

        }


        public double getDistance() {

            return distance;

        }


        public double getNaturalElevation() {

            return naturalElevation;

        }


        public double getSmoothedElevation() {

            return smoothedElevation;

        }


        public int getMinecraftHeight() {

            return (int) Math.round(
                    smoothedElevation
            );

        }

    }


    /*
     * ========================================================
     * CONTINUOUS ROAD PROFILE
     * ========================================================
     */

    public static final class RoadProfile {

        private final RoadType roadType;

        private final List<RoadSample> samples;

        private final double originalMaximumGrade;

        private final double smoothedMaximumGrade;


        private RoadProfile(
                RoadType roadType,
                List<RoadSample> samples,
                double originalMaximumGrade,
                double smoothedMaximumGrade
        ) {

            this.roadType = roadType;

            this.samples = List.copyOf(samples);

            this.originalMaximumGrade =
                    originalMaximumGrade;

            this.smoothedMaximumGrade =
                    smoothedMaximumGrade;

        }


        public RoadType getRoadType() {

            return roadType;

        }


        public List<RoadSample> getSamples() {

            return samples;

        }


        public double getOriginalMaximumGrade() {

            return originalMaximumGrade;

        }


        public double getSmoothedMaximumGrade() {

            return smoothedMaximumGrade;

        }

    }


    /*
     * ========================================================
     * BUILD ALL ROAD ELEVATION PROFILES
     * ========================================================
     */

    public static synchronized boolean load() {

        if (loaded) {

            return true;

        }

        if (!EarthTerrainLoader.isLoaded()
                || !EarthRoadData.isLoaded()) {

            System.out.println(
                    "[EarthBound] Road profiles require "
                            + "terrain and road data."
            );

            return false;

        }

        System.out.println(
                "[EarthBound] Building road elevation profiles..."
        );

        List<RoadProfile> generated =
                new ArrayList<>();

        int skipped = 0;

        long totalSamples = 0;

        try {

            RoadType[] types = {

                    RoadType.FREEWAY,

                    RoadType.HIGHWAY,

                    RoadType.LOCAL

            };

            for (RoadType type : types) {

                List<List<RoadPoint>> centerlines =
                        EarthRoadData.getRoadCenterlines(
                                type
                        );

                for (List<RoadPoint> centerline : centerlines) {

                    RoadProfile profile =
                            buildProfile(
                                    type,
                                    centerline
                            );

                    if (profile == null) {

                        skipped++;

                        continue;

                    }

                    generated.add(profile);

                    totalSamples +=
                            profile.getSamples().size();

                }

            }

            if (generated.isEmpty()) {

                System.out.println(
                        "[EarthBound] No valid road profiles."
                );

                return false;

            }

            profiles = List.copyOf(generated);

            loaded = true;

            System.out.println(
                    "[EarthBound] Road elevation profiles loaded!"
            );

            System.out.println(
                    "[EarthBound] Profiles: "
                            + profiles.size()
                            + ", samples: "
                            + totalSamples
                            + ", skipped: "
                            + skipped
            );

            return true;

        } catch (RuntimeException exception) {

            profiles = List.of();

            loaded = false;

            System.out.println(
                    "[EarthBound] Road profile generation failed: "
                            + exception.getMessage()
            );

            return false;

        }

    }


    /*
     * ========================================================
     * BUILD INDIVIDUAL ROAD PROFILE
     * ========================================================
     */

    private static RoadProfile buildProfile(
            RoadType type,
            List<RoadPoint> centerline
    ) {

        if (centerline == null
                || centerline.size() < 2) {

            return null;

        }

        List<Double> latitudes =
                new ArrayList<>();

        List<Double> longitudes =
                new ArrayList<>();

        List<Double> distances =
                new ArrayList<>();

        List<Double> elevations =
                new ArrayList<>();

        RoadPoint first =
                centerline.get(0);

        if (!addSample(
                first.getLatitude(),
                first.getLongitude(),
                0.0,
                latitudes,
                longitudes,
                distances,
                elevations
        )) {

            return null;

        }

        double cumulativeDistance = 0.0;


        /*
         * Sample each original road segment.
         *
         * Preserve original vertices so corners and road
         * geometry remain geographically accurate.
         */

        for (int i = 1; i < centerline.size(); i++) {

            RoadPoint start =
                    centerline.get(i - 1);

            RoadPoint end =
                    centerline.get(i);

            double segmentLength =
                    getDistanceBlocks(
                            start,
                            end
                    );

            if (!Double.isFinite(segmentLength)) {

                return null;

            }

            if (segmentLength <= 0.000001) {

                continue;

            }

            double requiredSteps =
                    Math.ceil(
                            segmentLength
                                    / SAMPLE_INTERVAL
                    );

            if (requiredSteps > MAX_PROFILE_SAMPLES
                    || distances.size() + requiredSteps
                    > MAX_PROFILE_SAMPLES) {

                return null;

            }

            int steps =
                    Math.max(
                            1,
                            (int) requiredSteps
                    );

            for (int step = 1; step <= steps; step++) {

                double progress =
                        (double) step / steps;

                double latitude =
                        start.getLatitude()
                                +
                        (
                                end.getLatitude()
                                        - start.getLatitude()
                        ) * progress;

                double longitude =
                        start.getLongitude()
                                +
                        (
                                end.getLongitude()
                                        - start.getLongitude()
                        ) * progress;

                double distance =
                        cumulativeDistance
                                + segmentLength * progress;

                if (!addSample(
                        latitude,
                        longitude,
                        distance,
                        latitudes,
                        longitudes,
                        distances,
                        elevations
                )) {

                    return null;

                }

            }

            cumulativeDistance += segmentLength;

        }

        int count = distances.size();

        if (count < 2) {

            return null;

        }


        /*
         * Convert samples into smoothing engine arrays.
         */

        double[] distanceArray =
                new double[count];

        double[] naturalArray =
                new double[count];

        for (int i = 0; i < count; i++) {

            distanceArray[i] =
                    distances.get(i);

            naturalArray[i] =
                    elevations.get(i);

        }


        /*
         * Smooth the actual road elevation profile.
         */

        double[] smoothedArray =
                EarthRoadSmoothing.smoothProfile(
                        distanceArray,
                        naturalArray
                );


        /*
         * Preserve minimum land clearance for this
         * initial surface-road implementation.
         */

        for (int i = 0; i < count; i++) {

            smoothedArray[i] =
                    Math.max(
                            SEA_LEVEL + 1.0,
                            smoothedArray[i]
                    );

        }


        /*
         * Measure the actual resulting profile grade.
         */

        double originalGrade =
                EarthRoadSmoothing.getMaximumGrade(
                        distanceArray,
                        naturalArray
                );

        double smoothedGrade =
                EarthRoadSmoothing.getMaximumGrade(
                        distanceArray,
                        smoothedArray
                );


        /*
         * Publish immutable road samples.
         */

        List<RoadSample> samples =
                new ArrayList<>(count);

        for (int i = 0; i < count; i++) {

            samples.add(
                    new RoadSample(
                            latitudes.get(i),
                            longitudes.get(i),
                            distanceArray[i],
                            naturalArray[i],
                            smoothedArray[i]
                    )
            );

        }

        return new RoadProfile(
                type,
                samples,
                originalGrade,
                smoothedGrade
        );

    }


    /*
     * ========================================================
     * SAMPLE REAL-WORLD TERRAIN
     * ========================================================
     */

    private static boolean addSample(
            double latitude,
            double longitude,
            double distance,
            List<Double> latitudes,
            List<Double> longitudes,
            List<Double> distances,
            List<Double> elevations
    ) {

        if (!EarthTerrainLoader.isLoaded()) {

            return false;

        }

        if (!Double.isFinite(latitude)
                || !Double.isFinite(longitude)
                || !Double.isFinite(distance)) {

            return false;

        }

        if (latitude < SOUTH
                || latitude > NORTH
                || longitude < WEST
                || longitude > EAST) {

            return false;

        }

        if (!distances.isEmpty()
                && distance <= distances.get(
                        distances.size() - 1
                )) {

            return false;

        }

        double realElevation =
                EarthTerrainLoader.getGuemesElevation(
                        latitude,
                        longitude
                );

        if (!Double.isFinite(realElevation)) {

            return false;

        }


        /*
         * Skip sea-level and underwater samples.
         *
         * Bridges and underwater road segments require
         * a separate engineering system.
         */

        if (realElevation < 1.0) {

            return false;

        }


        /*
         * Preserve the existing EarthBound vertical scale:
         * one elevation meter equals one vertical block.
         *
         * Keep fractional heights until final placement.
         */

        double minecraftElevation =
                SEA_LEVEL + realElevation;

        latitudes.add(latitude);

        longitudes.add(longitude);

        distances.add(distance);

        elevations.add(minecraftElevation);

        return true;

    }


    /*
     * ========================================================
     * GEOGRAPHIC DISTANCE IN MINECRAFT BLOCKS
     * ========================================================
     */

    private static double getDistanceBlocks(
            RoadPoint first,
            RoadPoint second
    ) {

        double northMeters =
                (
                        second.getLatitude()
                                - first.getLatitude()
                ) * METERS_PER_DEGREE_LATITUDE;

        double eastMeters =
                (
                        second.getLongitude()
                                - first.getLongitude()
                ) * METERS_PER_DEGREE_LONGITUDE;

        return Math.hypot(
                northMeters,
                eastMeters
        ) / EarthCoordinates.METERS_PER_BLOCK;

    }


    /*
     * ========================================================
     * PUBLIC PROFILE ACCESS
     * ========================================================
     */

    public static boolean isLoaded() {

        return loaded;

    }


    public static List<RoadProfile> getProfiles() {

        return profiles;

    }


    public static int getProfileCount() {

        return profiles.size();

    }


    /*
     * ========================================================
     * CLEAR PROFILE DATA
     * ========================================================
     */

    public static synchronized void clear() {

        loaded = false;

        profiles = List.of();

    }

}
