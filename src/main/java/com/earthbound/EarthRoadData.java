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
     * Guemes Island / Anacortes geographic bounds.
     */
    private static final double WEST = -122.70;
    private static final double SOUTH = 48.47;
    private static final double EAST = -122.55;
    private static final double NORTH = 48.60;

    /*
     * Road-mask resolution.
     */
    private static final int MASK_WIDTH = 1024;
    private static final int MASK_HEIGHT = 1024;

    /*
     * EarthBound road-width standards.
     *
     * Interstate / Freeway = 10 blocks
     * Highway              = 8 blocks
     * Local / City Street  = 6 blocks
     */
    public static final int FREEWAY_WIDTH = 10;
    public static final int HIGHWAY_WIDTH = 8;
    public static final int LOCAL_WIDTH = 6;

    /*
     * Download reliability.
     */
    private static final int MAX_DOWNLOAD_ATTEMPTS = 3;

    private static final int CONNECT_TIMEOUT_MS = 20000;

    private static final int READ_TIMEOUT_MS = 60000;

    private static final long RETRY_DELAY_MS = 3000L;

    /*
     * Census TIGERweb Transportation service.
     */
    private static final String ROAD_SERVICE =
            "https://tigerweb.geo.census.gov/arcgis/rest/services/"
                    + "TIGERweb/Transportation/MapServer";

    /*
     * Road data stored in memory.
     */
    private static boolean[][] roadMask;

    private static boolean loaded = false;

    private EarthRoadData() {
    }

    /*
     * EarthBound road classifications.
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
     * Load the Guemes / Anacortes road mask.
     *
     * If TIGERweb temporarily fails,
     * EarthBound automatically retries.
     */
    public static synchronized boolean loadGuemesRoadMask() {

        if (loaded && roadMask != null) {
            return true;
        }

        for (int attempt = 1;
             attempt <= MAX_DOWNLOAD_ATTEMPTS;
             attempt++) {

            System.out.println(
                    "[EarthBound] Downloading Guemes road mask..."
            );

            System.out.println(
                    "[EarthBound] Road download attempt "
                            + attempt
                            + " of "
                            + MAX_DOWNLOAD_ATTEMPTS
            );

            try {

                boolean success =
                        downloadRoadMask();

                if (success) {

                    loaded = true;

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
                }

            } catch (Exception exception) {

                System.out.println(
                        "[EarthBound] Road download attempt "
                                + attempt
                                + " failed: "
                                + exception.getClass().getSimpleName()
                                + ": "
                                + exception.getMessage()
                );
            }

            /*
             * Wait before another attempt.
             */
            if (attempt < MAX_DOWNLOAD_ATTEMPTS) {

                System.out.println(
                        "[EarthBound] Waiting "
                                + (RETRY_DELAY_MS / 1000)
                                + " seconds before retrying roads..."
                );

                try {

                    Thread.sleep(
                            RETRY_DELAY_MS
                    );

                } catch (InterruptedException exception) {

                    Thread.currentThread().interrupt();

                    System.out.println(
                            "[EarthBound] Road retry interrupted."
                    );

                    break;
                }
            }
        }

        /*
         * All attempts failed.
         */
        roadMask = null;
        loaded = false;

        System.out.println(
                "[EarthBound] Guemes road mask could not be loaded "
                        + "after "
                        + MAX_DOWNLOAD_ATTEMPTS
                        + " attempts."
        );

        return false;
    }

    /*
     * Performs one download attempt.
     */
    private static boolean downloadRoadMask()
            throws Exception {

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
                URI.create(
                        request
                ).toURL();

        HttpURLConnection connection =
                (HttpURLConnection)
                        url.openConnection();

        try {

            connection.setConnectTimeout(
                    CONNECT_TIMEOUT_MS
            );

            connection.setReadTimeout(
                    READ_TIMEOUT_MS
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "EarthBound-Minecraft/0.1.0"
            );

            connection.setRequestProperty(
                    "Accept",
                    "image/png"
            );

            int responseCode =
                    connection.getResponseCode();

            if (responseCode != 200) {

                System.out.println(
                        "[EarthBound] Road service returned HTTP "
                                + responseCode
                );

                return false;
            }

            BufferedImage image;

            try (InputStream input =
                         connection.getInputStream()) {

                image =
                        ImageIO.read(
                                input
                        );
            }

            if (image == null) {

                System.out.println(
                        "[EarthBound] Could not decode road mask image."
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

            System.out.println(
                    "[EarthBound] Guemes road mask loaded: "
                            + image.getWidth()
                            + "x"
                            + image.getHeight()
                            + ", road pixels="
                            + roadPixels
            );

            return true;

        } finally {

            connection.disconnect();
        }
    }

    /*
     * Get the road classification at
     * a real-world coordinate.
     *
     * For this first phase, roads detected
     * in the Guemes / Anacortes mask are
     * classified as LOCAL.
     *
     * Later we will separate Highway 20
     * and freeways such as I-5.
     */
    public static RoadType getRoadType(
            double latitude,
            double longitude) {

        if (!isRawRoad(
                latitude,
                longitude
        )) {

            return RoadType.NONE;
        }

        return RoadType.LOCAL;
    }

    /*
     * Compatibility method used by
     * other EarthBound code.
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
     * Return the configured width for
     * the road at this coordinate.
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
     * Check the downloaded TIGERweb mask.
     */
    private static boolean isRawRoad(
            double latitude,
            double longitude) {

        if (!loaded
                || roadMask == null) {

            return false;
        }

        /*
         * Outside our current data area.
         */
        if (longitude < WEST
                || longitude > EAST
                || latitude < SOUTH
                || latitude > NORTH) {

            return false;
        }

        /*
         * Convert longitude to image X.
         */
        double xFraction =
                (longitude - WEST)
                        / (EAST - WEST);

        /*
         * Image Y runs downward while
         * latitude runs northward.
         */
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

        /*
         * Keep coordinates safely inside
         * the downloaded image.
         */
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

        return roadMask[y][x];
    }

    /*
     * Returns true after road data
     * has successfully loaded.
     */
    public static boolean isLoaded() {

        return loaded
                && roadMask != null;
    }

    /*
     * Encode TIGERweb URL parameters.
     */
    private static String encode(
            String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}
