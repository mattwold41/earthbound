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

    // Guemes Island Phase 1 bounds
    private static final double WEST = -122.70;
    private static final double SOUTH = 48.47;
    private static final double EAST = -122.55;
    private static final double NORTH = 48.60;

    private static final int MASK_WIDTH = 1024;
    private static final int MASK_HEIGHT = 1024;

    /*
     * U.S. Census Bureau TIGERweb Transportation service.
     *
     * Layers:
     * 2 = Primary Roads
     * 6 = Secondary Roads
     * 8 = Local Roads
     */
    private static final String ROAD_SERVICE =
            "https://tigerweb.geo.census.gov/arcgis/rest/services/"
                    + "TIGERweb/Transportation/MapServer";

    private static boolean[][] roadMask;
    private static boolean loaded = false;

    private EarthRoadData() {
    }

    /**
     * Downloads a road image covering Guemes Island and converts
     * visible road pixels into a fast local boolean mask.
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
                    (HttpURLConnection) url.openConnection();

            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            connection.setRequestProperty(
                    "User-Agent",
                    "EarthBound-Minecraft/0.1.0"
            );

            int responseCode =
                    connection.getResponseCode();

            if (responseCode
                    != HttpURLConnection.HTTP_OK) {

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
                            image.getRGB(x, y);

                    int alpha =
                            (argb >>> 24) & 0xFF;

                    /*
                     * Transparent background = no road.
                     *
                     * Any sufficiently visible TIGERweb
                     * road pixel becomes part of our
                     * Minecraft road mask.
                     */
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

    /**
     * Returns true when the supplied real-world coordinate
     * falls on a road in the downloaded Guemes road mask.
     */
    public static boolean isRoad(
            double latitude,
            double longitude) {

        if (!loaded
                || roadMask == null) {

            return false;
        }

        /*
         * Outside the current Guemes test area.
         */
        if (longitude < WEST
                || longitude > EAST
                || latitude < SOUTH
                || latitude > NORTH) {

            return false;
        }

        /*
         * Convert longitude into horizontal
         * road-mask position.
         */
        double xFraction =
                (longitude - WEST)
                        / (EAST - WEST);

        /*
         * Image Y runs downward,
         * while latitude runs northward.
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
         * Keep the pixel safely inside
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

        /*
         * IMPORTANT:
         *
         * Radius is now ZERO.
         *
         * Previously this was 2, which expanded
         * every TIGER road pixel outward and made
         * the Minecraft roads much too wide.
         *
         * Radius 0 uses only the actual road-mask
         * pixel at this geographic position.
         */
        int radius = 0;

        for (int offsetY = -radius;
             offsetY <= radius;
             offsetY++) {

            for (int offsetX = -radius;
                 offsetX <= radius;
                 offsetX++) {

                int sampleX =
                        x + offsetX;

                int sampleY =
                        y + offsetY;

                if (sampleX < 0
                        || sampleX
                        >= roadMask[0].length
                        || sampleY < 0
                        || sampleY
                        >= roadMask.length) {

                    continue;
                }

                if (roadMask[sampleY][sampleX]) {

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns whether the Guemes road
     * data has successfully loaded.
     */
    public static boolean isLoaded() {

        return loaded
                && roadMask != null;
    }

    /**
     * URL-encodes values used in the
     * TIGERweb request.
     */
    private static String encode(
            String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
