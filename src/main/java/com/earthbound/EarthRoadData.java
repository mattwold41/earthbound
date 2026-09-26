package com.earthbound;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class EarthRoadData {

    /*
     * Guemes Island geographic bounds.
     */
    private static final double WEST = -122.70;
    private static final double SOUTH = 48.47;
    private static final double EAST = -122.55;
    private static final double NORTH = 48.60;

    /*
     * Resolution of the downloaded road mask.
     */
    private static final int MASK_WIDTH = 1024;
    private static final int MASK_HEIGHT = 1024;

    /*
     * EarthBound road-width standards.
     *
     * Freeway / Interstate = 10 blocks
     * Highway              = 8 blocks
     * City / Local Street  = 6 blocks
     */
    public static final int FREEWAY_WIDTH = 10;
    public static final int HIGHWAY_WIDTH = 8;
    public static final int LOCAL_WIDTH = 6;

    /*
     * U.S. Census TIGERweb transportation service.
     */
    private static final String ROAD_SERVICE =
            "https://tigerweb.geo.census.gov/arcgis/rest/services/"
                    + "TIGERweb/Transportation/MapServer";

    /*
     * Downloaded Guemes road mask.
     */
    private static boolean[][] roadMask;

    private static boolean loaded = false;

    private EarthRoadData() {
    }

    /*
     * Road types supported by EarthBound.
     */
    public enum RoadType {

        NONE(0),

        LOCAL(LOCAL_WIDTH),

        HIGHWAY(HIGHWAY_WIDTH),

        FREEWAY(FREEWAY_WIDTH);

        private final int width;

        RoadType(int width) {
            this.width = width;
        }

        public int getWidth() {
            return width;
        }
    }

    /*
     * Downloads the Guemes Island road mask.
     */
    public static synchronized boolean loadGuemesRoadMask() {

        if (loaded && roadMask != null) {
            return true;
        }

        try {

            System.out.println(
                    "[EarthBound] Downloading Guemes road mask..."
            );

            String bbox =
                    WEST + ","
                            + SOUTH + ","
                            + EAST + ","
                            + NORTH;

            String request =
                    ROAD_SERVICE
                            + "/export"
                            + "?bbox="
                            + encode(bbox)
                            + "&bboxSR=4326"
                            + "&imageSR=4326"
                            + "&size="
                            + MASK_WIDTH
                            + ","
                            + MASK_HEIGHT
                            + "&layers="
                            + encode("show:2,6,8")
                            + "&transparent=true"
                            + "&format=png32"
                            + "&f=image";

            URL url =
                    URI.create(request).toURL();

            HttpURLConnection connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setConnectTimeout(
                    15000
            );

            connection.setReadTimeout(
                    30000
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "EarthBound-Minecraft/0.1.0"
            );

            int responseCode =
                    connection.getResponseCode();

            if (responseCode != 200) {

                System.out.println(
                        "[EarthBound] Road service returned HTTP "
                                + responseCode
                );

                connection.disconnect();

                return false;
            }

            BufferedImage image;

            try (InputStream input =
                         connection.getInputStream()) {

                image =
                        ImageIO.read(input);

            } finally {

                connection.disconnect();
            }

            if (image == null) {

                System.out.println(
                        "[EarthBound] Could not decode road mask."
                );

                return false;
            }

            boolean[][] newMask =
                    new boolean[
                            image.getHeight()
                            ][
                            image.getWidth()
                            ];

            int roadPixels = 0;

            for (int y = 0;
                 y < image.getHeight();
                 y++) {

                for (int x = 0;
                     x < image.getWidth();
                     x++) {

                    int argb =
                            image.getRGB(
                                    x,
                                    y
                            );

                    int alpha =
                            (argb >>> 24)
                                    & 0xFF;

                    boolean road =
                            alpha > 20;

                    newMask[y][x] =
                            road;

                    if (road) {
                        roadPixels++;
                    }
                }
            }

            roadMask =
                    newMask;

            loaded =
                    true;

            System.out.println(
                    "[EarthBound] Guemes road mask loaded: "
                            + image.getWidth()
                            + "x"
                            + image.getHeight()
                            + ", road pixels="
                            + roadPixels
            );

            System.out.println(
                    "[EarthBound] Road widths configured: "
                            + "freeway="
                            + FREEWAY_WIDTH
                            + ", highway="
                            + HIGHWAY_WIDTH
                            + ", local="
                            + LOCAL_WIDTH
            );

            return true;

        } catch (Exception exception) {

            System.out.println(
                    "[EarthBound] Failed to load Guemes road mask: "
                            + exception.getMessage()
            );

            exception.printStackTrace();

            roadMask = null;
            loaded = false;

            return false;
        }
    }

    /*
     * Determines the road type at a
     * real-world latitude/longitude.
     *
     * For the current Guemes test area,
     * roads are classified as LOCAL.
     *
     * Highway and freeway classification
     * will be connected when those road
     * datasets are added.
     */
    public static RoadType getRoadType(
            double latitude,
            double longitude) {

        if (!isRawRoad(
                latitude,
                longitude)) {

            return RoadType.NONE;
        }

        return RoadType.LOCAL;
    }

    /*
     * Compatibility method for the
     * current EarthGenerator.
     */
    public static boolean isRoad(
            double latitude,
            double longitude) {

        return getRoadType(
                latitude,
                longitude
        ) != RoadType.NONE;
    }

    /*
     * Returns the configured width
     * of the road at this location.
     */
    public static int getRoadWidth(
            double latitude,
            double longitude) {

        return getRoadType(
                latitude,
                longitude
        ).getWidth();
    }

    /*
     * Checks the downloaded road mask.
     */
    private static boolean isRawRoad(
            double latitude,
            double longitude) {

        if (!loaded
                || roadMask == null) {

            return false;
        }

        if (longitude < WEST
                || longitude > EAST
                || latitude < SOUTH
                || latitude > NORTH) {

            return false;
        }

        double xFraction =
                (longitude - WEST)
                        / (EAST - WEST);

        double yFraction =
                (NORTH - latitude)
                        / (NORTH - SOUTH);

        int x =
                (int) Math.round(
                        xFraction
                                * (roadMask[0].length - 1)
                );

        int y =
                (int) Math.round(
                        yFraction
                                * (roadMask.length - 1)
                );

        x =
                Math.max(
                        0,
                        Math.min(
                                roadMask[0].length - 1,
                                x
                        )
                );

        y =
                Math.max(
                        0,
                        Math.min(
                                roadMask.length - 1,
                                y
                        )
                );

        /*
         * Do not artificially expand the
         * TIGERweb image here.
         *
         * EarthBound's generator will handle
         * the final road widths.
         */
        return roadMask[y][x];
    }

    /*
     * Returns true after the road
     * data has successfully loaded.
     */
    public static boolean isLoaded() {

        return loaded
                && roadMask != null;
    }

    /*
     * URL-encodes parameters used
     * by the TIGERweb request.
     */
    private static String encode(
            String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}
