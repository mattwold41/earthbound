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
     * Guemes / Anacortes test area.
     */
    private static final double WEST = -122.70;
    private static final double SOUTH = 48.47;
    private static final double EAST = -122.55;
    private static final double NORTH = 48.60;

    private static final int MASK_WIDTH = 1024;
    private static final int MASK_HEIGHT = 1024;

    /*
     * EarthBound target road widths.
     */
    public static final int FREEWAY_WIDTH = 10;
    public static final int HIGHWAY_WIDTH = 8;
    public static final int LOCAL_WIDTH = 6;

    /*
     * Download retry settings.
     */
    private static final int MAX_DOWNLOAD_ATTEMPTS = 3;

    private static final int CONNECT_TIMEOUT_MS = 20000;

    private static final int READ_TIMEOUT_MS = 60000;

    private static final long RETRY_DELAY_MS = 3000L;

    /*
     * Current Census TIGERweb Transportation service.
     *
     * Relevant layers:
     *
     * 2 = Primary Roads
     * 6 = Secondary Roads
     * 8 = Local Roads
     */
    private static final String ROAD_SERVICE =
            "https://tigerweb.geo.census.gov/arcgis/rest/services/"
                    + "TIGERweb/Transportation/MapServer";

    /*
     * Separate masks allow EarthBound to
     * distinguish road classes.
     */
    private static boolean[][] primaryMask;

    private static boolean[][] secondaryMask;

    private static boolean[][] localMask;

    private static boolean loaded = false;

    private EarthRoadData() {
    }

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
     * Load all three road classes.
     */
    public static synchronized boolean loadGuemesRoadMask() {

        if (loaded
                && primaryMask != null
                && secondaryMask != null
                && localMask != null) {

            return true;
        }

        System.out.println(
                "[EarthBound] Loading classified road data..."
        );

        primaryMask =
                downloadLayerWithRetry(
                        2,
                        "primary roads"
                );

        secondaryMask =
                downloadLayerWithRetry(
                        6,
                        "secondary roads"
                );

        localMask =
                downloadLayerWithRetry(
                        8,
                        "local roads"
                );

        if (primaryMask == null
                || secondaryMask == null
                || localMask == null) {

            loaded = false;

            System.out.println(
                    "[EarthBound] Classified road data could not "
                            + "be completely loaded."
            );

            return false;
        }

        loaded = true;

        System.out.println(
                "[EarthBound] Classified Guemes/Anacortes "
                        + "road data loaded!"
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
    }

    /*
     * Download one TIGERweb layer,
     * automatically retrying failures.
     */
    private static boolean[][] downloadLayerWithRetry(
            int layer,
            String layerName) {

        for (int attempt = 1;
             attempt <= MAX_DOWNLOAD_ATTEMPTS;
             attempt++) {

            System.out.println(
                    "[EarthBound] Downloading "
                            + layerName
                            + " - attempt "
                            + attempt
                            + " of "
                            + MAX_DOWNLOAD_ATTEMPTS
            );

            try {

                boolean[][] mask =
                        downloadLayer(
                                layer,
                                layerName
                        );

                if (mask != null) {

                    return mask;
                }

            } catch (Exception exception) {

                System.out.println(
                        "[EarthBound] "
                                + layerName
                                + " attempt "
                                + attempt
                                + " failed: "
                                + exception
                                .getClass()
                                .getSimpleName()
                                + ": "
                                + exception
                                .getMessage()
                );
            }

            if (attempt < MAX_DOWNLOAD_ATTEMPTS) {

                System.out.println(
                        "[EarthBound] Waiting "
                                + (RETRY_DELAY_MS / 1000)
                                + " seconds before retrying "
                                + layerName
                                + "..."
                );

                try {

                    Thread.sleep(
                            RETRY_DELAY_MS
                    );

                } catch (InterruptedException exception) {

                    Thread.currentThread().interrupt();

                    return null;
                }
            }
        }

        return null;
    }

    /*
     * Download a single TIGERweb road layer.
     */
    private static boolean[][] downloadLayer(
            int layer,
            String layerName)
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
                        + encode(
                                "show:" + layer
                        )
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
                        "[EarthBound] "
                                + layerName
                                + " returned HTTP "
                                + responseCode
                );

                return null;
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
                        "[EarthBound] Could not decode "
                                + layerName
                                + " image."
                );

                return null;
            }

            boolean[][] mask =
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

                    mask[y][x] =
                            road;

                    if (road) {
                        roadPixels++;
                    }
                }
            }

            System.out.println(
                    "[EarthBound] "
                            + layerName
                            + " loaded: "
                            + image.getWidth()
                            + "x"
                            + image.getHeight()
                            + ", road pixels="
                            + roadPixels
            );

            return mask;

        } finally {

            connection.disconnect();
        }
    }

    /*
     * Determine road class.
     *
     * Priority matters:
     *
     * Primary beats secondary.
     * Secondary beats local.
     */
    public static RoadType getRoadType(
            double latitude,
            double longitude) {

        if (!loaded) {
            return RoadType.NONE;
        }

        /*
         * Primary roads.
         */
        if (isOnMask(
                primaryMask,
                latitude,
                longitude
        )) {

            return RoadType.FREEWAY;
        }

        /*
         * Secondary roads.
         */
        if (isOnMask(
                secondaryMask,
                latitude,
                longitude
        )) {

            return RoadType.HIGHWAY;
        }

        /*
         * Local streets.
         */
        if (isOnMask(
                localMask,
                latitude,
                longitude
        )) {

            return RoadType.LOCAL;
        }

        return RoadType.NONE;
    }

    public static boolean isRoad(
            double latitude,
            double longitude) {

        return getRoadType(
                latitude,
                longitude
        ) != RoadType.NONE;
    }

    public static int getRoadWidth(
            double latitude,
            double longitude) {

        return getRoadType(
                latitude,
                longitude
        ).getWidth();
    }

    /*
     * Test a latitude/longitude against
     * one particular road-class mask.
     */
    private static boolean isOnMask(
            boolean[][] mask,
            double latitude,
            double longitude) {

        if (mask == null) {
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
                                * (mask[0].length - 1)
                );

        int y =
                (int) Math.round(
                        yFraction
                                * (mask.length - 1)
                );

        x =
                Math.max(
                        0,
                        Math.min(
                                mask[0].length - 1,
                                x
                        )
                );

        y =
                Math.max(
                        0,
                        Math.min(
                                mask.length - 1,
                                y
                        )
                );

        return mask[y][x];
    }

    public static boolean isLoaded() {

        return loaded
                && primaryMask != null
                && secondaryMask != null
                && localMask != null;
    }

    private static String encode(
            String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}
