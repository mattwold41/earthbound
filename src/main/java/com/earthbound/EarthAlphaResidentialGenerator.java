package com.earthbound;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import com.earthbound.roads.EarthRoadData;
import com.earthbound.roads.EarthRoadData.RoadType;
import com.earthbound.water.EarthWaterGenerator;
import com.earthbound.water.EarthWaterData;

import com.earthbound.EarthAlphaHouseDesigns.HouseDesign;

public final class EarthAlphaResidentialGenerator {

    /*
     * =========================================================
     * EARTHBOUND - FRIENDS ALPHA RESIDENTIAL DEVELOPMENT
     * =========================================================
     *
     * Standard plot:
     *
     * 24 blocks wide
     * 32 blocks deep
     *
     * Includes:
     *
     * - Six residential house designs
     * - Prepared building foundations
     * - Natural terrain transitions
     * - Residential driveways
     * - Front walkways
     * - Flower gardens
     * - Decorative hedges
     * - Outdoor lighting
     * - Sale marker location helpers
     *
     * Existing roads, water and the General Store
     * must remain protected.
     *
     * Property coordinates must be validated before use.
     * =========================================================
     */

    public static final int PLOT_WIDTH = 24;
    public static final int PLOT_LENGTH = 32;

    private static final int HOUSE_FRONT_OFFSET = 10;

    private static final int TERRAIN_BLEND_MARGIN = 3;

    private static final int MAX_TERRAIN_CHANGE = 8;

    private static final double METERS_PER_BLOCK = 2.0;

    private static final int DRIVEWAY_MIN_X = 18;
    private static final int DRIVEWAY_MAX_X = 22;
    private static final int DRIVEWAY_LENGTH = 8;

    private EarthAlphaResidentialGenerator() {
    }

    /*
     * =========================================================
     * VALIDATED RESIDENTIAL SITE
     * =========================================================
     */

    public static final class ResidentialSite {

        private final HouseDesign design;

        private final int frontX;
        private final int frontZ;

        private final int groundY;

        private final BlockFace facing;
        private final BlockFace right;
        private final BlockFace inward;

        public ResidentialSite(
                String propertyId,
                int frontX,
                int frontZ,
                int groundY,
                BlockFace facing
        ) {

            this.design =
                    EarthAlphaHouseDesigns.getDesign(
                            propertyId
                    );

            if (design == null) {

                throw new IllegalArgumentException(
                        "Unknown Alpha house: "
                                + propertyId
                );
            }

            if (!isCardinal(facing)) {

                throw new IllegalArgumentException(
                        "Invalid residential direction"
                );
            }

            this.frontX = frontX;
            this.frontZ = frontZ;
            this.groundY = groundY;

            this.facing = facing;

            this.right = getRight(facing);

            this.inward =
                    facing.getOppositeFace();
        }

        public String getPropertyId() {
            return design.getPropertyId();
        }

        public HouseDesign getDesign() {
            return design;
        }

        public int getGroundY() {
            return groundY;
        }

        public BlockFace getFacing() {
            return facing;
        }

        public int getWorldX(
                int x,
                int z
        ) {

            return frontX
                    + right.getModX() * x
                    + inward.getModX() * z;
        }

        public int getWorldZ(
                int x,
                int z
        ) {

            return frontZ
                    + right.getModZ() * x
                    + inward.getModZ() * z;
        }

        public int getMinX() {

            return Math.min(
                    frontX,
                    getWorldX(
                            PLOT_WIDTH - 1,
                            PLOT_LENGTH - 1
                    )
            );
        }

        public int getMaxX() {

            return Math.max(
                    frontX,
                    getWorldX(
                            PLOT_WIDTH - 1,
                            PLOT_LENGTH - 1
                    )
            );
        }

        public int getMinZ() {

            return Math.min(
                    frontZ,
                    getWorldZ(
                            PLOT_WIDTH - 1,
                            PLOT_LENGTH - 1
                    )
            );
        }

        public int getMaxZ() {

            return Math.max(
                    frontZ,
                    getWorldZ(
                            PLOT_WIDTH - 1,
                            PLOT_LENGTH - 1
                    )
            );
        }

        /*
         * Banner and sign positions.
         *
         * Actual marker placement remains the responsibility
         * of EarthPropertySaleMarkers.
         */

        public int getBannerX() {
            return getWorldX(16, 1);
        }

        public int getBannerY() {
            return getSurfaceY(this, 16, 1) + 1;
        }

        public int getBannerZ() {
            return getWorldZ(16, 1);
        }

        public int getSignX() {
            return getWorldX(17, 1);
        }

        public int getSignY() {
            return getSurfaceY(this, 17, 1) + 1;
        }

        public int getSignZ() {
            return getWorldZ(17, 1);
        }

        public boolean intersectsChunk(
                int chunkX,
                int chunkZ
        ) {

            int minX = chunkX * 16;
            int minZ = chunkZ * 16;

            return minX <= getMaxX()
                    && minX + 15 >= getMinX()
                    && minZ <= getMaxZ()
                    && minZ + 15 >= getMinZ();
        }

        public boolean overlaps(
                ResidentialSite other
        ) {

            if (other == null) {
                return false;
            }

            return getMinX() <= other.getMaxX()
                    && getMaxX() >= other.getMinX()
                    && getMinZ() <= other.getMaxZ()
                    && getMaxZ() >= other.getMinZ();
        }
    }

    /*
     * =========================================================
     * DEVELOPMENT SITE VALIDATION
     * =========================================================
     *
     * Reject entire plots that overlap:
     *
     * - Water
     * - Existing roads
     * - General Store terrain blending area
     * - Excessively steep terrain
     *
     * Validation must run before registering a site
     * for generation.
     * =========================================================
     */

    public static boolean isSuitableSite(
            ResidentialSite site
    ) {

        if (site == null) {
            return false;
        }

        if (!EarthRoadData.isLoaded()) {
            return false;
        }

        if (site.groundY
                < EarthWaterData.SEA_LEVEL + 3) {

            return false;
        }

        for (int x = 0; x < PLOT_WIDTH; x++) {

            for (int z = 0; z < PLOT_LENGTH; z++) {

                int worldX =
                        site.getWorldX(x, z);

                int worldZ =
                        site.getWorldZ(x, z);

                if (EarthBuildingGenerator
                        .isInsideGuemesStoreBlendArea(
                                worldX,
                                worldZ
                        )) {

                    return false;
                }

                double latitude =
                        EarthCoordinates.getLatitude(
                                worldX,
                                worldZ
                        );

                double longitude =
                        EarthCoordinates.getLongitude(
                                worldX,
                                worldZ
                        );

                if (EarthWaterGenerator.isWater(
                        latitude,
                        longitude
                )) {

                    return false;
                }

                if (isRoad(
                        latitude,
                        longitude
                )) {

                    return false;
                }

                int naturalY =
                        EarthTerrainGenerator
                                .getTerrainHeight(
                                        latitude,
                                        longitude
                                );

                if (naturalY
                        < EarthWaterData.SEA_LEVEL + 3) {

                    return false;
                }

                if (Math.abs(
                        (long) naturalY
                                - site.groundY
                ) > MAX_TERRAIN_CHANGE) {

                    return false;
                }
            }
        }

        /*
         * Ensure the driveway can be walked
         * without jumps greater than one block.
         */

        for (int x = DRIVEWAY_MIN_X;
             x <= DRIVEWAY_MAX_X;
             x++) {

            for (int z = 0;
                 z < DRIVEWAY_LENGTH - 1;
                 z++) {

                int first =
                        getSurfaceY(site, x, z);

                int second =
                        getSurfaceY(site, x, z + 1);

                if (Math.abs(first - second) > 1) {

                    return false;
                }
            }
        }

        /*
         * Check the main front walkway.
         */

        int centerX =
                getHouseOffsetX(site)
                        + site.design.getWidth() / 2;

        for (int z = 0;
             z < HOUSE_FRONT_OFFSET - 1;
             z++) {

            if (Math.abs(
                    getSurfaceY(site, centerX, z)
                            - getSurfaceY(
                                    site,
                                    centerX,
                                    z + 1
                            )
            ) > 1) {

                return false;
            }
        }

        return true;
    }

    /*
     * =========================================================
     * EXISTING ROAD DETECTION
     * =========================================================
     *
     * Uses the same width calculation as EarthGenerator.
     *
     * The driving surface and its stone brick borders
     * remain untouched.
     */

    private static boolean isRoad(
            double latitude,
            double longitude
    ) {

        RoadType type =
                EarthRoadData.getNearestRoadType(
                        latitude,
                        longitude
                );

        if (type == RoadType.NONE) {
            return false;
        }

        double distance =
                EarthRoadData
                        .getDistanceToRoadTypeMeters(
                                type,
                                latitude,
                                longitude
                        );

        double drivingRadius =
                type.getWidth()
                        * METERS_PER_BLOCK
                        / 2.0;

        double borderRadius =
                drivingRadius
                        + METERS_PER_BLOCK;

        return distance <= borderRadius;
    }

    /*
     * =========================================================
     * MAIN RESIDENTIAL GENERATION
     * =========================================================
     *
     * Call only for an approved and validated site.
     *
     * This method does not register properties,
     * charge money or place sale markers.
     * =========================================================
     */

    public static void generate(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            ResidentialSite site
    ) {

        if (chunk == null || site == null) {
            return;
        }

        if (!site.intersectsChunk(
                chunkX,
                chunkZ
        )) {

            return;
        }

        /*
         * Reserve enough vertical space for
         * the entire house.
         */

        if (site.groundY
                - MAX_TERRAIN_CHANGE
                - 4 < chunk.getMinHeight()) {

            return;
        }

        if ((long) site.groundY
                + site.design.getMaximumHeight()
                + MAX_TERRAIN_CHANGE
                + 4 >= chunk.getMaxHeight()) {

            return;
        }

        prepareTerrain(
                chunk,
                chunkX,
                chunkZ,
                site
        );

        generateDriveway(
                chunk,
                chunkX,
                chunkZ,
                site
        );

        generateWalkway(
                chunk,
                chunkX,
                chunkZ,
                site
        );

        generateHouse(
                chunk,
                chunkX,
                chunkZ,
                site
        );

        generateLandscaping(
                chunk,
                chunkX,
                chunkZ,
                site
        );
    }

    /*
     * =========================================================
     * NATURAL TERRAIN HEIGHT
     * =========================================================
     */

    private static int getNaturalHeight(
            ResidentialSite site,
            int x,
            int z
    ) {

        int worldX =
                site.getWorldX(x, z);

        int worldZ =
                site.getWorldZ(x, z);

        double latitude =
                EarthCoordinates.getLatitude(
                        worldX,
                        worldZ
                );

        double longitude =
                EarthCoordinates.getLongitude(
                        worldX,
                        worldZ
                );

        return EarthTerrainGenerator
                .getTerrainHeight(
                        latitude,
                        longitude
                );
    }

    /*
     * =========================================================
     * TERRAIN BLENDING
     * =========================================================
     *
     * The central property is level.
     *
     * Outer edges transition back to natural terrain.
     *
     * No terrain outside the plot is modified.
     */

    public static int getSurfaceY(
            ResidentialSite site,
            int x,
            int z
    ) {

        int naturalY =
                getNaturalHeight(site, x, z);

        /*
         * Maintain natural elevation at property edges.
         */

        int edgeDistance =
                Math.min(
                        Math.min(
                                x,
                                PLOT_WIDTH - 1 - x
                        ),
                        Math.min(
                                z,
                                PLOT_LENGTH - 1 - z
                        )
                );

        if (edgeDistance >= TERRAIN_BLEND_MARGIN) {

            return site.groundY;
        }

        double t =
                Math.max(0, edgeDistance)
                        / (double) TERRAIN_BLEND_MARGIN;

        /*
         * Smoothstep transition.
         */

        t = t * t * (3.0 - 2.0 * t);

        return (int) Math.round(
                naturalY
                        + (site.groundY - naturalY) * t
        );
    }

    /*
     * =========================================================
     * PREPARE PROPERTY TERRAIN
     * =========================================================
     *
     * Fill low terrain.
     * Remove excess terrain.
     * Finish with dirt and grass.
     *
     * Only intended for initial chunk generation.
     * =========================================================
     */

    private static void prepareTerrain(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            ResidentialSite site
    ) {

        for (int x = 0; x < PLOT_WIDTH; x++) {

            for (int z = 0; z < PLOT_LENGTH; z++) {

                if (!belongsToChunk(
                        site,
                        chunkX,
                        chunkZ,
                        x,
                        z
                )) {

                    continue;
                }

                int naturalY =
                        getNaturalHeight(
                                site,
                                x,
                                z
                        );

                int targetY =
                        getSurfaceY(
                                site,
                                x,
                                z
                        );

                int bottomY =
                        Math.min(
                                naturalY,
                                targetY
                        ) - 3;

                /*
                 * Clear terrain above the prepared surface.
                 */

                for (int y = targetY + 1;
                     y <= naturalY;
                     y++) {

                    setBlock(
                            chunk,
                            chunkX,
                            chunkZ,
                            site,
                            x,
                            y,
                            z,
                            Material.AIR
                    );
                }

                /*
                 * Solid supporting terrain.
                 */

                for (int y = bottomY;
                     y < targetY;
                     y++) {

                    Material material =
                            y >= targetY - 3
                                    ? Material.DIRT
                                    : Material.STONE;

                    setBlock(
                            chunk,
                            chunkX,
                            chunkZ,
                            site,
                            x,
                            y,
                            z,
                            material
                    );
                }

                /*
                 * Finished lawn surface.
                 */

                setBlock(
                        chunk,
                        chunkX,
                        chunkZ,
                        site,
                        x,
                        targetY,
                        z,
                        Material.GRASS_BLOCK
                );
            }
        }
    }

    /*
     * =========================================================
     * DRIVEWAY
     * =========================================================
     *
     * Five blocks wide.
     *
     * Polished andesite driving surface.
     *
     * Stone brick edge detailing.
     *
     * Stops at the property boundary.
     */

    private static void generateDriveway(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            ResidentialSite site
    ) {

        for (int x = DRIVEWAY_MIN_X;
             x <= DRIVEWAY_MAX_X;
             x++) {

            for (int z = 0;
                 z < DRIVEWAY_LENGTH;
                 z++) {

                boolean edge =
                        x == DRIVEWAY_MIN_X
                                || x == DRIVEWAY_MAX_X;

                Material material =
                        edge
                                ? Material.STONE_BRICKS
                                : Material.POLISHED_ANDESITE;

                placeSurfaceBlock(
                        chunk,
                        chunkX,
                        chunkZ,
                        site,
                        x,
                        z,
                        material
                );
            }
        }
    }

    /*
     * =========================================================
     * FRONT WALKWAY
     * =========================================================
     */

    private static void generateWalkway(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            ResidentialSite site
    ) {

        int centerX =
                getHouseOffsetX(site)
                        + site.design.getWidth() / 2;

        /*
         * Main entrance walkway.
         */

        for (int x = centerX - 1;
             x <= centerX + 1;
             x++) {

            for (int z = 0;
                 z < HOUSE_FRONT_OFFSET;
                 z++) {

                placeSurfaceBlock(
                        chunk,
                        chunkX,
                        chunkZ,
                        site,
                        x,
                        z,
                        Material.STONE_BRICKS
                );
            }
        }

        /*
         * Connect driveway to entrance walkway.
         */

        for (int x = centerX + 2;
             x <= DRIVEWAY_MIN_X;
             x++) {

            for (int z = 6; z <= 7; z++) {

                placeSurfaceBlock(
                        chunk,
                        chunkX,
                        chunkZ,
                        site,
                        x,
                        z,
                        Material.STONE_BRICKS
                );
            }
        }
    }

    /*
     * =========================================================
     * HOUSE PLACEMENT
     * =========================================================
     */

    private static int getHouseOffsetX(
            ResidentialSite site
    ) {

        return (PLOT_WIDTH
                - site.design.getWidth()) / 2;
    }

    private static void generateHouse(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            ResidentialSite site
    ) {

        int houseOffsetX =
                getHouseOffsetX(site);

        int houseX =
                site.getWorldX(
                        houseOffsetX,
                        HOUSE_FRONT_OFFSET
                );

        int houseZ =
                site.getWorldZ(
                        houseOffsetX,
                        HOUSE_FRONT_OFFSET
                );

        EarthAlphaHouseDesigns.generate(
                chunk,
                chunkX,
                chunkZ,
                site.design,
                houseX,
                site.groundY,
                houseZ,
                site.facing
        );
    }

    /*
     * =========================================================
     * RESIDENTIAL LANDSCAPING
     * =========================================================
     */

    private static void generateLandscaping(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            ResidentialSite site
    ) {

        generateFlowerGarden(
                chunk,
                chunkX,
                chunkZ,
                site
        );

        generateHedges(
                chunk,
                chunkX,
                chunkZ,
                site
        );

        generateOutdoorLighting(
                chunk,
                chunkX,
                chunkZ,
                site
        );
    }

    /*
     * =========================================================
     * FLOWER GARDEN
     * =========================================================
     */

    private static void generateFlowerGarden(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            ResidentialSite site
    ) {

        Material[] flowers =
                switch (site.design) {

                    case STARTER ->
                            new Material[]{
                                    Material.POPPY,
                                    Material.DANDELION
                            };

                    case FAMILY ->
                            new Material[]{
                                    Material.WHITE_TULIP,
                                    Material.AZURE_BLUET
                            };

                    case COTTAGE ->
                            new Material[]{
                                    Material.PINK_TULIP,
                                    Material.ALLIUM
                            };

                    case FARMHOUSE ->
                            new Material[]{
                                    Material.DANDELION,
                                    Material.OXEYE_DAISY
                            };

                    case CABIN ->
                            new Material[]{
                                    Material.FERN,
                                    Material.POPPY
                            };

                    case TRADITIONAL ->
                            new Material[]{
                                    Material.RED_TULIP,
                                    Material.WHITE_TULIP
                            };
                };

        for (int x = 3; x <= 7; x++) {

            for (int z = 4; z <= 6; z++) {

                int surfaceY =
                        getSurfaceY(site, x, z);

                setBlock(
                        chunk,
                        chunkX,
                        chunkZ,
                        site,
                        x,
                        surfaceY,
                        z,
                        Material.GRASS_BLOCK
                );

                Material flower =
                        flowers[(x + z)
                                % flowers.length];

                setBlock(
                        chunk,
                        chunkX,
                        chunkZ,
                        site,
                        x,
                        surfaceY + 1,
                        z,
                        flower
                );
            }
        }
    }

    /*
     * =========================================================
     * DECORATIVE HEDGES
     * =========================================================
     *
     * Solid moss blocks are used instead of leaves
     * to prevent unsupported leaf decay.
     */

    private static void generateHedges(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            ResidentialSite site
    ) {

        /*
         * Side garden hedges.
         */

        for (int z = 10;
             z <= 27;
             z++) {

            placeAboveSurface(
                    chunk,
                    chunkX,
                    chunkZ,
                    site,
                    2,
                    z,
                    Material.MOSS_BLOCK
            );

            placeAboveSurface(
                    chunk,
                    chunkX,
                    chunkZ,
                    site,
                    PLOT_WIDTH - 3,
                    z,
                    Material.MOSS_BLOCK
            );
        }

        /*
         * Rear garden hedge.
         */

        for (int x = 3;
             x <= PLOT_WIDTH - 4;
             x++) {

            placeAboveSurface(
                    chunk,
                    chunkX,
                    chunkZ,
                    site,
                    x,
                    PLOT_LENGTH - 3,
                    Material.MOSS_BLOCK
            );
        }
    }

    /*
     * =========================================================
     * OUTDOOR LIGHTING
     * =========================================================
     */

    private static void generateOutdoorLighting(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            ResidentialSite site
    ) {

        int centerX =
                getHouseOffsetX(site)
                        + site.design.getWidth() / 2;

        generateLamp(
                chunk,
                chunkX,
                chunkZ,
                site,
                centerX - 2,
                2
        );

        generateLamp(
                chunk,
                chunkX,
                chunkZ,
                site,
                centerX + 2,
                2
        );

        generateLamp(
                chunk,
                chunkX,
                chunkZ,
                site,
                4,
                PLOT_LENGTH - 5
        );
    }

    private static void generateLamp(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            ResidentialSite site,
            int x,
            int z
    ) {

        int surfaceY =
                getSurfaceY(site, x, z);

        setBlock(
                chunk,
                chunkX,
                chunkZ,
                site,
                x,
                surfaceY + 1,
                z,
                Material.STONE_BRICKS
        );

        setBlock(
                chunk,
                chunkX,
                chunkZ,
                site,
                x,
                surfaceY + 2,
                z,
                Material.LANTERN
        );
    }

    /*
     * =========================================================
     * SURFACE PLACEMENT HELPERS
     * =========================================================
     */

    private static void placeSurfaceBlock(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            ResidentialSite site,
            int x,
            int z,
            Material material
    ) {

        int surfaceY =
                getSurfaceY(site, x, z);

        setBlock(
                chunk,
                chunkX,
                chunkZ,
                site,
                x,
                surfaceY,
                z,
                material
        );
    }

    private static void placeAboveSurface(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            ResidentialSite site,
            int x,
            int z,
            Material material
    ) {

        int surfaceY =
                getSurfaceY(site, x, z);

        setBlock(
                chunk,
                chunkX,
                chunkZ,
                site,
                x,
                surfaceY + 1,
                z,
                material
        );
    }

    /*
     * =========================================================
     * CHUNK BOUNDARY CHECK
     * =========================================================
     */

    private static boolean belongsToChunk(
            ResidentialSite site,
            int chunkX,
            int chunkZ,
            int x,
            int z
    ) {

        int localX =
                site.getWorldX(x, z)
                        - chunkX * 16;

        int localZ =
                site.getWorldZ(x, z)
                        - chunkZ * 16;

        return localX >= 0
                && localX < 16
                && localZ >= 0
                && localZ < 16;
    }

    /*
     * =========================================================
     * SAFE BLOCK PLACEMENT
     * =========================================================
     */

    private static void setBlock(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            ResidentialSite site,
            int x,
            int y,
            int z,
            Material material
    ) {

        /*
         * Never modify land outside this property.
         */

        if (x < 0 || x >= PLOT_WIDTH
                || z < 0 || z >= PLOT_LENGTH) {

            return;
        }

        if (y < chunk.getMinHeight()
                || y >= chunk.getMaxHeight()) {

            return;
        }

        int localX =
                site.getWorldX(x, z)
                        - chunkX * 16;

        int localZ =
                site.getWorldZ(x, z)
                        - chunkZ * 16;

        if (localX < 0 || localX >= 16
                || localZ < 0 || localZ >= 16) {

            return;
        }

        chunk.setBlock(
                localX,
                y,
                localZ,
                material
        );
    }

    /*
     * =========================================================
     * CARDINAL DIRECTION HELPERS
     * =========================================================
     */

    private static boolean isCardinal(
            BlockFace facing
    ) {

        return facing == BlockFace.NORTH
                || facing == BlockFace.SOUTH
                || facing == BlockFace.EAST
                || facing == BlockFace.WEST;
    }

    private static BlockFace getRight(
            BlockFace facing
    ) {

        return switch (facing) {

            case NORTH -> BlockFace.EAST;
            case SOUTH -> BlockFace.WEST;
            case EAST -> BlockFace.SOUTH;
            case WEST -> BlockFace.NORTH;

            default -> throw new IllegalArgumentException(
                    "Invalid residential direction"
            );
        };
    }
}
