public static int getMinecraftHeight(
            double elevationMeters
    ) {


        int seaLevel = 64;


        double verticalScale = 2.0;


        return seaLevel
                + (int)Math.round(
                        elevationMeters
                        / verticalScale
                );

    }
