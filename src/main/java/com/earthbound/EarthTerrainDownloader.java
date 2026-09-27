package com.earthbound;

public class EarthTerrainDownloader {

    /*
     * USGS 3DEP elevation service.
     *
     * This class will be responsible for loading
     * real-world elevation data before Minecraft
     * generates terrain.
     */
    public static final String USGS_3DEP_SERVICE =
            "https://elevation.nationalmap.gov/"
            + "arcgis/rest/services/"
            + "3DEPElevation/ImageServer";


    /*
     * Guemes Island terrain bounds.
     *
     * These coordinates cover Guemes Island
     * and a small surrounding area.
     */
    public static final double WEST =
            -122.70;

    public static final double SOUTH =
            48.47;

    public static final double EAST =
            -122.55;

    public static final double NORTH =
            48.60;


    /*
     * Size of the elevation tile.
     */
    public static final int TILE_WIDTH =
            512;

    public static final int TILE_HEIGHT =
            512;


    private EarthTerrainDownloader() {
    }


    /*
     * Starts loading the Guemes elevation tile.
     *
     * The actual raster download and decoding
     * will be added in the next step.
     */
    public static void loadGuemesIsland() {

        System.out.println(
                "=== Preparing real USGS Guemes terrain ==="
        );

        System.out.println(
                "Bounds: "
                + WEST
                + ", "
                + SOUTH
                + " to "
                + EAST
                + ", "
                + NORTH
        );

        System.out.println(
                "Elevation tile size: "
                + TILE_WIDTH
                + "x"
                + TILE_HEIGHT
        );
    }
}
