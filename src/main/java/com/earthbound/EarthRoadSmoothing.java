package com.earthbound.roads;

/*
 * ============================================================
 * EARTHBOUND ROAD SMOOTHING ENGINE
 * Friends Alpha
 * ============================================================
 *
 * Smooths road elevation profiles without changing
 * real-world road locations.
 *
 * Road distances are measured in Minecraft blocks.
 * Elevations are measured in Minecraft Y coordinates.
 *
 * This class does not modify world blocks directly.
 * ============================================================
 */

public final class EarthRoadSmoothing {

    /*
     * Initial Friends Alpha engineering settings.
     */

    public static final int SAMPLE_INTERVAL = 4;

    public static final int SMOOTHING_RADIUS = 16;

    public static final double TARGET_MAX_GRADE = 0.125;

    public static final int TERRAIN_BLEND_DISTANCE = 8;

    public static final double MAX_TERRAIN_ADJUSTMENT = 4.0;


    private EarthRoadSmoothing() {
    }


    /*
     * ========================================================
     * SMOOTH ROAD ELEVATION PROFILE
     * ========================================================
     *
     * Input:
     * distances = cumulative distances along the road
     * elevations = original terrain elevations
     *
     * Output:
     * a new array containing smoothed elevations.
     *
     * The original arrays are never modified.
     */

    public static double[] smoothProfile(
            double[] distances,
            double[] elevations
    ) {

        validateProfile(
                distances,
                elevations
        );

        int count = elevations.length;

        double[] result = elevations.clone();

        if (count < 3) {

            return result;

        }


        /*
         * Apply a weighted moving average.
         *
         * Nearby samples have more influence than
         * distant samples.
         */

        int left = 0;
        int right = 0;

        for (int i = 1; i < count - 1; i++) {

            while (
                    left < i
                            &&
                    distances[i] - distances[left]
                            > SMOOTHING_RADIUS
            ) {

                left++;

            }

            while (
                    right + 1 < count
                            &&
                    distances[right + 1] - distances[i]
                            <= SMOOTHING_RADIUS
            ) {

                right++;

            }

            double weightedTotal = 0.0;

            double totalWeight = 0.0;


            for (int j = left; j <= right; j++) {

                double distance = Math.abs(
                        distances[j] - distances[i]
                );

                double weight =
                        1.0
                                -
                        (distance / SMOOTHING_RADIUS);

                weight = Math.max(
                        0.0,
                        weight
                );

                weightedTotal +=
                        elevations[j] * weight;

                totalWeight += weight;

            }


            if (totalWeight > 0.0) {

                double average =
                        weightedTotal / totalWeight;

                result[i] = clamp(
                        average,
                        elevations[i]
                                - MAX_TERRAIN_ADJUSTMENT,
                        elevations[i]
                                + MAX_TERRAIN_ADJUSTMENT
                );

            }

        }


        /*
         * Preserve the original elevations at both ends.
         */

        result[0] = elevations[0];

        result[count - 1] = elevations[count - 1];


        /*
         * Reduce excessive elevation changes.
         *
         * Multiple passes allow neighboring samples
         * to gradually influence each other.
         *
         * The target grade is not guaranteed when
         * terrain adjustment limits prevent it.
         */

        for (int pass = 0; pass < 8; pass++) {

            boolean changed = false;


            /*
             * Forward pass.
             */

            for (int i = 1; i < count - 1; i++) {

                double distance =
                        distances[i] - distances[i - 1];

                double allowedChange =
                        distance * TARGET_MAX_GRADE;

                double adjusted = clamp(
                        result[i],
                        result[i - 1] - allowedChange,
                        result[i - 1] + allowedChange
                );

                adjusted = clamp(
                        adjusted,
                        elevations[i]
                                - MAX_TERRAIN_ADJUSTMENT,
                        elevations[i]
                                + MAX_TERRAIN_ADJUSTMENT
                );

                if (
                        Math.abs(adjusted - result[i])
                                > 0.000001
                ) {

                    changed = true;

                    result[i] = adjusted;

                }

            }


            /*
             * Backward pass.
             */

            for (int i = count - 2; i > 0; i--) {

                double distance =
                        distances[i + 1] - distances[i];

                double allowedChange =
                        distance * TARGET_MAX_GRADE;

                double adjusted = clamp(
                        result[i],
                        result[i + 1] - allowedChange,
                        result[i + 1] + allowedChange
                );

                adjusted = clamp(
                        adjusted,
                        elevations[i]
                                - MAX_TERRAIN_ADJUSTMENT,
                        elevations[i]
                                + MAX_TERRAIN_ADJUSTMENT
                );

                if (
                        Math.abs(adjusted - result[i])
                                > 0.000001
                ) {

                    changed = true;

                    result[i] = adjusted;

                }

            }


            if (!changed) {

                break;

            }

        }

        return result;

    }


    /*
     * ========================================================
     * INTERPOLATE ROAD ELEVATION
     * ========================================================
     *
     * Calculates the elevation between two road samples.
     */

    public static double interpolateElevation(
            double firstElevation,
            double secondElevation,
            double progress
    ) {

        if (
                !Double.isFinite(firstElevation)
                        ||
                !Double.isFinite(secondElevation)
                        ||
                !Double.isFinite(progress)
        ) {

            throw new IllegalArgumentException(
                    "Invalid road elevation interpolation."
            );

        }

        progress = clamp(
                progress,
                0.0,
                1.0
        );

        return firstElevation
                +
                (
                        (secondElevation - firstElevation)
                                * progress
                );

    }


    /*
     * ========================================================
     * ROADSIDE TERRAIN BLENDING
     * ========================================================
     *
     * distanceFromRoadEdge:
     *
     * 0 = road edge
     * 8 = natural terrain
     */

    public static int getBlendedTerrainHeight(
            int naturalHeight,
            int roadHeight,
            double distanceFromRoadEdge
    ) {

        if (!Double.isFinite(distanceFromRoadEdge)) {

            return naturalHeight;

        }

        if (distanceFromRoadEdge <= 0.0) {

            return roadHeight;

        }

        if (
                distanceFromRoadEdge
                        >= TERRAIN_BLEND_DISTANCE
        ) {

            return naturalHeight;

        }

        double progress =
                distanceFromRoadEdge
                        / TERRAIN_BLEND_DISTANCE;


        /*
         * Smoothstep interpolation.
         */

        progress =
                progress
                        * progress
                        * (3.0 - 2.0 * progress);

        return (int) Math.round(
                roadHeight
                        +
                (
                        ((double) naturalHeight - roadHeight)
                                * progress
                )
        );

    }


    /*
     * ========================================================
     * MEASURE ROAD GRADE
     * ========================================================
     */

    public static double getMaximumGrade(
            double[] distances,
            double[] elevations
    ) {

        validateProfile(
                distances,
                elevations
        );

        double maximumGrade = 0.0;

        for (int i = 1; i < elevations.length; i++) {

            double distance =
                    distances[i] - distances[i - 1];

            double elevationChange = Math.abs(
                    elevations[i] - elevations[i - 1]
            );

            double grade =
                    elevationChange / distance;

            maximumGrade = Math.max(
                    maximumGrade,
                    grade
            );

        }

        return maximumGrade;

    }


    /*
     * ========================================================
     * PROFILE VALIDATION
     * ========================================================
     */

    private static void validateProfile(
            double[] distances,
            double[] elevations
    ) {

        if (
                distances == null
                        ||
                elevations == null
                        ||
                distances.length != elevations.length
                        ||
                distances.length == 0
        ) {

            throw new IllegalArgumentException(
                    "Invalid road elevation profile."
            );

        }

        for (int i = 0; i < distances.length; i++) {

            if (
                    !Double.isFinite(distances[i])
                            ||
                    !Double.isFinite(elevations[i])
                            ||
                    distances[i] < 0.0
            ) {

                throw new IllegalArgumentException(
                        "Invalid road elevation sample."
                );

            }

            if (
                    i > 0
                            &&
                    distances[i] <= distances[i - 1]
            ) {

                throw new IllegalArgumentException(
                        "Road distances must increase."
                );

            }

        }

    }


    /*
     * ========================================================
     * VALUE LIMITING
     * ========================================================
     */

    private static double clamp(
            double value,
            double minimum,
            double maximum
    ) {

        return Math.max(
                minimum,
                Math.min(maximum, value)
        );

    }

}
