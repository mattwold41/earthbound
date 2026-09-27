package com.earthbound;

public class EarthElevation {

    private EarthElevation() {
    }


    /*
     * Returns the elevation for a Minecraft
     * X/Z coordinate.
     *
     * This keeps EarthCommand and EarthLocation
     * compatible with the terrain system.
     */
    public static double getElevation(
            double x,
            double z
    ) {

        return EarthTerrainLoader.getGuemesElevation(
                x,
                z
        );
    }


    /*
     * Converts real-world elevation in meters
     * into Minecraft Y height.
     *
     * Low elevations keep high detail.
     * Taller mountains are progressively
     * compressed to stay below build limit.
     */
    public static int getMinecraftHeight(
            double elevationMeters
    ) {

        if (elevationMeters <= 0.0) {
            return 63;
        }

        double minecraftHeight;

        if (elevationMeters <= 50.0) {

            minecraftHeight =
                    63 + elevationMeters;

        } else if (elevationMeters <= 100.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            50, 100,
                            113, 153
                    );

        } else if (elevationMeters <= 250.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            100, 250,
                            153, 180
                    );

        } else if (elevationMeters <= 500.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            250, 500,
                            180, 200
                    );

        } else if (elevationMeters <= 1000.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            500, 1000,
                            200, 220
                    );

        } else if (elevationMeters <= 2000.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            1000, 2000,
                            220, 240
                    );

        } else if (elevationMeters <= 3286.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            2000, 3286,
                            240, 258
                    );

        } else if (elevationMeters <= 4392.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            3286, 4392,
                            258, 270
                    );

        } else if (elevationMeters <= 6000.0) {

            minecraftHeight =
                    interpolate(
                            elevationMeters,
                            4392, 6000,
                            270, 288
                    );

        } else {

            double limitedElevation =
                    Math.min(
                            elevationMeters,
                            8848.86
                    );

            minecraftHeight =
                    interpolate(
                            limitedElevation,
                            6000, 8848.86,
                            288, 315
                    );
        }

        return Math.min(
                (int) Math.round(minecraftHeight),
                315
        );
    }


    private static double interpolate(
            double value,
            double inputMin,
            double inputMax,
            double outputMin,
            double outputMax
    ) {

        double percentage =
                (value - inputMin)
                        / (inputMax - inputMin);

        return outputMin
                + percentage
                * (outputMax - outputMin);
    }
}
