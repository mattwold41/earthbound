package com.earthbound;

public class GuemesElevation {

    private GuemesElevation() {
    }


    /*
     * Gets the real USGS elevation for a
     * Minecraft X/Z coordinate.
     */
    public static double getElevation(
            int x,
            int z
    ) {

        /*
         * Convert Minecraft coordinates
         * into real Earth coordinates.
         */
        double latitude =
                EarthCoordinates.minecraftToLatitude(
                        z
                );


        double longitude =
                EarthCoordinates.minecraftToLongitude(
                        x
                );


        /*
         * Make sure the real USGS elevation
         * tile has been loaded.
         */
        if (!EarthTerrainDownloader.isLoaded()) {

            return 0.0;

        }


        /*
         * Look up the elevation in meters
         * from the downloaded USGS raster.
         */
        double elevation =
                EarthTerrainDownloader.getElevation(
                        latitude,
                        longitude
                );


        /*
         * Protect terrain generation from
         * invalid negative/no-data values.
         */
        if (Double.isNaN(elevation)
                || Double.isInfinite(elevation)) {

            return 0.0;

        }


        return elevation;
    }
}
