package com.earthbound;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.generator.ChunkGenerator.ChunkData;

public final class EarthAlphaHouseDesigns {

    /*
     * =========================================================
     * EARTHBOUND - FRIENDS ALPHA HOUSE DESIGNS
     * =========================================================
     *
     * Six residential designs.
     *
     * Standard property: 24 x 32 blocks.
     *
     * Architecture only.
     *
     * Terrain preparation, foundations, driveways,
     * landscaping and property placement are handled
     * by EarthAlphaResidentialGenerator.
     *
     * All block placement is restricted to the current chunk.
     * =========================================================
     */

    public static final int PLOT_WIDTH = 24;
    public static final int PLOT_LENGTH = 32;

    private EarthAlphaHouseDesigns() {
    }

    /*
     * =========================================================
     * HOUSE DESIGN CATALOG
     * =========================================================
     */

    public enum HouseDesign {

        STARTER(
                "guemes_home_01",
                "Starter House",
                11, 13, 1,
                Material.OAK_PLANKS,
                Material.SPRUCE_STAIRS,
                Material.SPRUCE_PLANKS,
                Material.OAK_PLANKS,
                Material.STRIPPED_OAK_LOG,
                Material.OAK_DOOR
        ),

        FAMILY(
                "guemes_home_02",
                "Two-Story Family Home",
                15, 17, 2,
                Material.WHITE_CONCRETE,
                Material.SPRUCE_STAIRS,
                Material.SPRUCE_PLANKS,
                Material.BIRCH_PLANKS,
                Material.STRIPPED_SPRUCE_LOG,
                Material.SPRUCE_DOOR
        ),

        COTTAGE(
                "guemes_home_03",
                "Cottage",
                11, 13, 1,
                Material.SANDSTONE,
                Material.BIRCH_STAIRS,
                Material.BIRCH_PLANKS,
                Material.BIRCH_PLANKS,
                Material.STRIPPED_BIRCH_LOG,
                Material.BIRCH_DOOR
        ),

        FARMHOUSE(
                "guemes_home_04",
                "Two-Story Farmhouse",
                15, 17, 2,
                Material.WHITE_TERRACOTTA,
                Material.DARK_OAK_STAIRS,
                Material.DARK_OAK_PLANKS,
                Material.SPRUCE_PLANKS,
                Material.STRIPPED_DARK_OAK_LOG,
                Material.DARK_OAK_DOOR
        ),

        CABIN(
                "guemes_home_05",
                "Cabin",
                13, 13, 1,
                Material.SPRUCE_PLANKS,
                Material.DARK_OAK_STAIRS,
                Material.DARK_OAK_PLANKS,
                Material.SPRUCE_PLANKS,
                Material.SPRUCE_LOG,
                Material.SPRUCE_DOOR
        ),

        TRADITIONAL(
                "guemes_home_06",
                "Two-Story Traditional",
                15, 17, 2,
                Material.BRICKS,
                Material.DARK_OAK_STAIRS,
                Material.DARK_OAK_PLANKS,
                Material.OAK_PLANKS,
                Material.STRIPPED_DARK_OAK_LOG,
                Material.DARK_OAK_DOOR
        );

        private final String propertyId;
        private final String displayName;

        private final int width;
        private final int depth;
        private final int stories;

        private final Material wall;
        private final Material roofStairs;
        private final Material roofBlock;
        private final Material floor;
        private final Material trim;
        private final Material door;

        HouseDesign(
                String propertyId,
                String displayName,
                int width,
                int depth,
                int stories,
                Material wall,
                Material roofStairs,
                Material roofBlock,
                Material floor,
                Material trim,
                Material door
        ) {

            this.propertyId = propertyId;
            this.displayName = displayName;

            this.width = width;
            this.depth = depth;
            this.stories = stories;

            this.wall = wall;
            this.roofStairs = roofStairs;
            this.roofBlock = roofBlock;
            this.floor = floor;
            this.trim = trim;
            this.door = door;
        }

        public String getPropertyId() {
            return propertyId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getWidth() {
            return width;
        }

        public int getDepth() {
            return depth;
        }

        public int getStories() {
            return stories;
        }

        public int getRoofBaseHeight() {
            return stories * 5 + 1;
        }

        public int getMaximumHeight() {
            return getRoofBaseHeight() + (width + 1) / 2;
        }
    }

    /*
     * =========================================================
     * PROPERTY DESIGN LOOKUP
     * =========================================================
     */

    public static HouseDesign getDesign(
            String propertyId
    ) {

        if (propertyId == null) {
            return null;
        }

        for (HouseDesign design : HouseDesign.values()) {

            if (design.getPropertyId().equals(propertyId)) {
                return design;
            }
        }

        return null;
    }

    /*
     * =========================================================
     * MAIN HOUSE GENERATION
     * =========================================================
     *
     * frontX and frontZ identify the front-left corner
     * of the house in its local coordinate system.
     *
     * groundY is the prepared terrain surface.
     *
     * facing identifies the direction toward the road.
     *
     * Supported:
     *
     * NORTH
     * SOUTH
     * EAST
     * WEST
     * =========================================================
     */

    public static void generate(
            ChunkData chunk,
            int chunkX,
            int chunkZ,
            HouseDesign design,
            int frontX,
            int groundY,
            int frontZ,
            BlockFace facing
    ) {

        if (chunk == null || design == null) {
            return;
        }

        if (!isCardinal(facing)) {
            return;
        }

        if (groundY < chunk.getMinHeight()
                || (long) groundY
                + design.getMaximumHeight()
                >= chunk.getMaxHeight()) {

            return;
        }

        HouseWriter writer = new HouseWriter(
                chunk,
                chunkX,
                chunkZ,
                frontX,
                groundY,
                frontZ,
                facing
        );

        generateFoundation(writer, design);
        generateFloors(writer, design);
        generateWalls(writer, design);
        generateWindows(writer, design);
        generateFrontDoor(writer, design);
        generateRoof(writer, design);
        generatePorch(writer, design);
        generateInterior(writer, design);

        if (design.stories == 2) {

            generateStaircase(writer, design);
            generateUpperInterior(writer, design);
        }

        generateStyleDetails(writer, design);
    }

    /*
     * =========================================================
     * FOUNDATION
     * =========================================================
     */

    private static void generateFoundation(
            HouseWriter writer,
            HouseDesign design
    ) {

        for (int x = 0; x < design.width; x++) {

            for (int z = 0; z < design.depth; z++) {

                writer.set(
                        x, 0, z,
                        Material.STONE_BRICKS
                );
            }
        }
    }

    /*
     * =========================================================
     * INTERIOR FLOORS
     * =========================================================
     */

    private static void generateFloors(
            HouseWriter writer,
            HouseDesign design
    ) {

        for (int story = 0;
             story < design.stories;
             story++) {

            int floorY = 1 + story * 5;

            for (int x = 0; x < design.width; x++) {

                for (int z = 0; z < design.depth; z++) {

                    writer.set(
                            x,
                            floorY,
                            z,
                            design.floor
                    );
                }
            }
        }
    }

    /*
     * =========================================================
     * EXTERIOR WALLS
     * =========================================================
     */

    private static void generateWalls(
            HouseWriter writer,
            HouseDesign design
    ) {

        int wallTop = design.stories * 5;

        for (int x = 0; x < design.width; x++) {

            for (int z = 0; z < design.depth; z++) {

                boolean side =
                        x == 0
                        || x == design.width - 1;

                boolean end =
                        z == 0
                        || z == design.depth - 1;

                if (!side && !end) {
                    continue;
                }

                Material material =
                        side && end
                                ? design.trim
                                : design.wall;

                for (int y = 2; y <= wallTop; y++) {

                    writer.set(
                            x, y, z, material
                    );
                }
            }
        }

        /*
         * Exterior trim between stories.
         */

        if (design.stories == 2) {

            for (int x = 0; x < design.width; x++) {

                for (int z = 0;
                     z < design.depth;
                     z++) {

                    if (x == 0
                            || x == design.width - 1
                            || z == 0
                            || z == design.depth - 1) {

                        writer.set(
                                x, 6, z,
                                design.trim
                        );
                    }
                }
            }
        }
    }

    /*
     * =========================================================
     * WINDOWS
     * =========================================================
     */

    private static void generateWindows(
            HouseWriter writer,
            HouseDesign design
    ) {

        for (int story = 0;
             story < design.stories;
             story++) {

            int windowY = 3 + story * 5;

            /*
             * Front windows.
             */

            window(
                    writer,
                    2, windowY, 0
            );

            window(
                    writer,
                    3, windowY, 0
            );

            window(
                    writer,
                    design.width - 4,
                    windowY, 0
            );

            window(
                    writer,
                    design.width - 3,
                    windowY, 0
            );

            /*
             * Rear windows.
             */

            window(
                    writer,
                    3,
                    windowY,
                    design.depth - 1
            );

            window(
                    writer,
                    design.width - 4,
                    windowY,
                    design.depth - 1
            );

            /*
             * Side windows.
             */

            for (int z = 3;
                 z < design.depth - 2;
                 z += 5) {

                window(
                        writer,
                        0, windowY, z
                );

                window(
                        writer,
                        design.width - 1,
                        windowY, z
                );
            }
        }
    }

    private static void window(
            HouseWriter writer,
            int x,
            int y,
            int z
    ) {

        /*
         * Full glass blocks avoid disconnected pane
         * states along chunk boundaries.
         */

        writer.set(
                x, y, z,
                Material.GLASS
        );

        writer.set(
                x, y + 1, z,
                Material.GLASS
        );
    }

    /*
     * =========================================================
     * FRONT DOOR
     * =========================================================
     */

    private static void generateFrontDoor(
            HouseWriter writer,
            HouseDesign design
    ) {

        int doorX = design.width / 2;

        Door bottom = (Door)
                Bukkit.createBlockData(design.door);

        bottom.setFacing(writer.facing);
        bottom.setHalf(Bisected.Half.BOTTOM);
        bottom.setOpen(false);

        Door top = (Door) bottom.clone();

        top.setHalf(Bisected.Half.TOP);

        writer.setData(
                doorX, 2, 0, bottom
        );

        writer.setData(
                doorX, 3, 0, top
        );
    }

    /*
     * =========================================================
     * PITCHED ROOF AND GABLES
     * =========================================================
     */

    private static void generateRoof(
            HouseWriter writer,
            HouseDesign design
    ) {

        int roofBase = design.getRoofBaseHeight();

        int centerX = design.width / 2;

        for (int x = -1;
             x <= design.width;
             x++) {

            int rise = Math.min(
                    x + 1,
                    design.width - x
            );

            int roofY = roofBase + rise;

            for (int z = -1;
                 z <= design.depth;
                 z++) {

                if (x == centerX) {

                    writer.set(
                            x, roofY, z,
                            design.roofBlock
                    );

                } else {

                    Stairs roof =
                            (Stairs) Bukkit.createBlockData(
                                    design.roofStairs
                            );

                    roof.setFacing(
                            x < centerX
                                    ? writer.right
                                    : writer.right.getOppositeFace()
                    );

                    roof.setHalf(Bisected.Half.BOTTOM);
                    roof.setShape(Stairs.Shape.STRAIGHT);

                    writer.setData(
                            x, roofY, z, roof
                    );
                }
            }

            /*
             * Close the front and rear gables.
             */

            if (x >= 0 && x < design.width) {

                for (int y = roofBase;
                     y < roofY;
                     y++) {

                    writer.set(
                            x, y, 0,
                            design.wall
                    );

                    writer.set(
                            x, y,
                            design.depth - 1,
                            design.wall
                    );
                }
            }
        }

        /*
         * Interior ceiling.
         */

        int ceilingY = design.stories * 5 + 1;

        for (int x = 0; x < design.width; x++) {

            for (int z = 0; z < design.depth; z++) {

                writer.set(
                        x, ceilingY, z,
                        design.floor
                );
            }
        }
    }

    /*
     * =========================================================
     * FRONT PORCH
     * =========================================================
     */

    private static void generatePorch(
            HouseWriter writer,
            HouseDesign design
    ) {

        int centerX = design.width / 2;

        int minX = centerX - 2;
        int maxX = centerX + 2;

        int porchDepth = 2;

        if (design == HouseDesign.FARMHOUSE) {

            minX = 0;
            maxX = design.width - 1;
            porchDepth = 3;

        } else if (design == HouseDesign.CABIN) {

            minX = 1;
            maxX = design.width - 2;
            porchDepth = 3;
        }

        for (int x = minX; x <= maxX; x++) {

            for (int z = -porchDepth; z < 0; z++) {

                writer.set(
                        x, 0, z,
                        Material.STONE_BRICKS
                );

                writer.set(
                        x, 1, z,
                        design.floor
                );

                writer.set(
                        x, 5, z,
                        design.roofBlock
                );
            }
        }

        for (int y = 2; y <= 4; y++) {

            writer.set(
                    minX, y, -porchDepth,
                    design.trim
            );

            writer.set(
                    maxX, y, -porchDepth,
                    design.trim
            );
        }

        /*
         * Entrance steps.
         */

        for (int x = centerX - 1;
             x <= centerX + 1;
             x++) {

            Stairs step =
                    (Stairs) Bukkit.createBlockData(
                            Material.STONE_BRICK_STAIRS
                    );

            step.setFacing(
                    writer.facing.getOppositeFace()
            );

            step.setHalf(Bisected.Half.BOTTOM);

            writer.setData(
                    x,
                    1,
                    -porchDepth - 1,
                    step
            );
        }

        writer.set(
                centerX,
                4,
                -1,
                Material.LANTERN
        );
    }

    /*
     * =========================================================
     * GROUND FLOOR INTERIOR
     * =========================================================
     */

    private static void generateInterior(
            HouseWriter writer,
            HouseDesign design
    ) {

        int rightX = design.width - 2;

        /*
         * Kitchen and crafting area.
         */

        writer.set(
                1, 2, 2,
                Material.CRAFTING_TABLE
        );

        writer.set(
                1, 2, 3,
                Material.FURNACE
        );

        writer.set(
                1, 2, 4,
                Material.BARREL
        );

        writer.set(
                1, 2, 5,
                Material.SMOOTH_STONE
        );

        /*
         * Storage.
         */

        writer.set(
                1, 2,
                design.depth - 2,
                Material.BARREL
        );

        writer.set(
                2, 2,
                design.depth - 2,
                Material.BARREL
        );

        /*
         * Bookshelves.
         */

        writer.set(
                rightX, 2, 2,
                Material.BOOKSHELF
        );

        writer.set(
                rightX, 3, 2,
                Material.BOOKSHELF
        );

        /*
         * Living room seating.
         */

        for (int z = 3; z <= 5; z++) {

            Stairs chair =
                    (Stairs) Bukkit.createBlockData(
                            Material.SPRUCE_STAIRS
                    );

            chair.setFacing(writer.right);

            writer.setData(
                    rightX, 2, z, chair
            );
        }

        /*
         * Dining table.
         */

        int tableX = design.width / 2;
        int tableZ = 5;

        writer.set(
                tableX, 2, tableZ,
                Material.SPRUCE_FENCE
        );

        writer.set(
                tableX, 3, tableZ,
                Material.SPRUCE_PRESSURE_PLATE
        );

        Stairs leftChair =
                (Stairs) Bukkit.createBlockData(
                        Material.SPRUCE_STAIRS
                );

        leftChair.setFacing(
                writer.right.getOppositeFace()
        );

        writer.setData(
                tableX - 1,
                2, tableZ,
                leftChair
        );

        Stairs rightChair =
                (Stairs) Bukkit.createBlockData(
                        Material.SPRUCE_STAIRS
                );

        rightChair.setFacing(writer.right);

        writer.setData(
                tableX + 1,
                2, tableZ,
                rightChair
        );

        /*
         * Living room rug.
         */

        for (int x = 3; x <= 5; x++) {

            for (int z = 8; z <= 9; z++) {

                writer.set(
                        x, 2, z,
                        Material.RED_CARPET
                );
            }
        }

        /*
         * Ceiling lighting.
         */

        writer.set(
                3, 5, 3,
                Material.GLOWSTONE
        );

        writer.set(
                design.width - 4,
                5,
                design.depth - 3,
                Material.GLOWSTONE
        );
    }

    /*
     * =========================================================
     * FUNCTIONAL SECOND-FLOOR STAIRCASE
     * =========================================================
     */

    private static void generateStaircase(
            HouseWriter writer,
            HouseDesign design
    ) {

        int stairX = design.width - 3;
        int startZ = design.depth - 8;

        /*
         * Five steps connect floor Y=1
         * to upper floor Y=6.
         */

        for (int step = 0; step < 5; step++) {

            int stepY = 2 + step;
            int stepZ = startZ + step;

            /*
             * Clear headroom and the second-floor
             * opening above the staircase.
             */

            for (int y = stepY + 1;
                 y <= stepY + 3;
                 y++) {

                writer.set(
                        stairX, y, stepZ,
                        Material.AIR
                );
            }

            /*
             * Structural staircase supports.
             */

            for (int y = 2; y < stepY; y++) {

                writer.set(
                        stairX, y, stepZ,
                        design.floor
                );
            }

            Stairs stairs =
                    (Stairs) Bukkit.createBlockData(
                            Material.SPRUCE_STAIRS
                    );

            stairs.setFacing(
                    writer.facing.getOppositeFace()
            );

            stairs.setHalf(Bisected.Half.BOTTOM);

            writer.setData(
                    stairX,
                    stepY,
                    stepZ,
                    stairs
            );
        }

        /*
         * Upper-floor safety railing.
         */

        for (int z = startZ + 1;
             z <= startZ + 3;
             z++) {

            writer.set(
                    stairX - 1,
                    7, z,
                    Material.SPRUCE_FENCE
            );
        }
    }

    /*
     * =========================================================
     * SECOND FLOOR INTERIOR
     * =========================================================
     */

    private static void generateUpperInterior(
            HouseWriter writer,
            HouseDesign design
    ) {

        writer.set(
                1, 7, 2,
                Material.BOOKSHELF
        );

        writer.set(
                1, 8, 2,
                Material.BOOKSHELF
        );

        writer.set(
                1, 7, 4,
                Material.BARREL
        );

        writer.set(
                2, 7, 4,
                Material.BARREL
        );

        /*
         * Upstairs rug.
         */

        for (int x = 3; x <= 6; x++) {

            for (int z = 3; z <= 5; z++) {

                writer.set(
                        x, 7, z,
                        Material.BLUE_CARPET
                );
            }
        }

        /*
         * Upstairs study.
         */

        writer.set(
                2, 7,
                design.depth - 3,
                Material.SPRUCE_PLANKS
        );

        writer.set(
                3, 7,
                design.depth - 3,
                Material.SPRUCE_PLANKS
        );

        writer.set(
                2, 8,
                design.depth - 3,
                Material.FLOWER_POT
        );

        Stairs chair =
                (Stairs) Bukkit.createBlockData(
                        Material.SPRUCE_STAIRS
                );

        chair.setFacing(writer.facing);

        writer.setData(
                2, 7,
                design.depth - 4,
                chair
        );

        /*
         * Upper-floor lighting.
         */

        writer.set(
                3, 10, 3,
                Material.GLOWSTONE
        );

        writer.set(
                4, 10,
                design.depth - 3,
                Material.GLOWSTONE
        );
    }

    /*
     * =========================================================
     * UNIQUE ARCHITECTURAL DETAILS
     * =========================================================
     */

    private static void generateStyleDetails(
            HouseWriter writer,
            HouseDesign design
    ) {

        switch (design) {

            case STARTER -> {

                frontPlanter(writer, 2);
                frontPlanter(writer, design.width - 3);
            }

            case FAMILY -> {

                generateUpperWindowTrim(
                        writer,
                        design,
                        Material.WHITE_CONCRETE
                );
            }

            case COTTAGE -> {

                frontPlanter(writer, 2);
                frontPlanter(writer, 3);

                frontPlanter(
                        writer,
                        design.width - 4
                );

                frontPlanter(
                        writer,
                        design.width - 3
                );
            }

            case FARMHOUSE -> {

                for (int x = 0;
                     x < design.width;
                     x++) {

                    if (x >= design.width / 2 - 1
                            && x <= design.width / 2 + 1) {

                        continue;
                    }

                    writer.set(
                            x, 2, -3,
                            Material.SPRUCE_FENCE
                    );
                }

                generateUpperWindowTrim(
                        writer,
                        design,
                        Material.DARK_OAK_PLANKS
                );
            }

            case CABIN -> {

                generateChimney(
                        writer,
                        design,
                        Material.COBBLESTONE
                );
            }

            case TRADITIONAL -> {

                generateChimney(
                        writer,
                        design,
                        Material.BRICKS
                );

                generateUpperWindowTrim(
                        writer,
                        design,
                        Material.SMOOTH_STONE
                );
            }
        }
    }

    /*
     * =========================================================
     * FLOWER PLANTERS
     * =========================================================
     */

    private static void frontPlanter(
            HouseWriter writer,
            int x
    ) {

        writer.set(
                x, 1, -1,
                Material.SPRUCE_PLANKS
        );

        writer.set(
                x, 2, -1,
                Material.POTTED_POPPY
        );
    }

    /*
     * =========================================================
     * UPPER WINDOW TRIM
     * =========================================================
     */

    private static void generateUpperWindowTrim(
            HouseWriter writer,
            HouseDesign design,
            Material material
    ) {

        for (int x = 1;
             x < design.width - 1;
             x++) {

            writer.set(
                    x, 7, 0,
                    material
            );
        }
    }

    /*
     * =========================================================
     * DECORATIVE CHIMNEY
     * =========================================================
     */

    private static void generateChimney(
            HouseWriter writer,
            HouseDesign design,
            Material material
    ) {

        int chimneyX = design.width - 2;
        int chimneyZ = design.depth - 2;

        int topY = design.getMaximumHeight();

        for (int y = 2; y <= topY; y++) {

            writer.set(
                    chimneyX,
                    y,
                    chimneyZ,
                    material
            );
        }
    }

    /*
     * =========================================================
     * CARDINAL DIRECTION VALIDATION
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
                    "Invalid house direction"
            );
        };
    }

    /*
     * =========================================================
     * ROTATABLE CHUNK-SAFE BLOCK WRITER
     * =========================================================
     *
     * Local X = house width.
     *
     * Local Z = distance from front toward rear.
     *
     * Local Y = height above prepared terrain.
     *
     * Houses can face any cardinal direction without
     * duplicating their architectural designs.
     * =========================================================
     */

    private static final class HouseWriter {

        private final ChunkData chunk;

        private final int chunkMinX;
        private final int chunkMinZ;

        private final int originX;
        private final int originY;
        private final int originZ;

        private final BlockFace facing;
        private final BlockFace right;

        private final BlockFace inward;

        private HouseWriter(
                ChunkData chunk,
                int chunkX,
                int chunkZ,
                int originX,
                int originY,
                int originZ,
                BlockFace facing
        ) {

            this.chunk = chunk;

            this.chunkMinX = chunkX * 16;
            this.chunkMinZ = chunkZ * 16;

            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;

            this.facing = facing;

            this.right = getRight(facing);

            this.inward =
                    facing.getOppositeFace();
        }

        private int worldX(
                int x,
                int z
        ) {

            return originX
                    + right.getModX() * x
                    + inward.getModX() * z;
        }

        private int worldZ(
                int x,
                int z
        ) {

            return originZ
                    + right.getModZ() * x
                    + inward.getModZ() * z;
        }

        private boolean valid(
                int x,
                int y,
                int z
        ) {

            return x >= 0
                    && x < 16
                    && z >= 0
                    && z < 16
                    && y >= chunk.getMinHeight()
                    && y < chunk.getMaxHeight();
        }

        private void set(
                int x,
                int y,
                int z,
                Material material
        ) {

            int localX =
                    worldX(x, z) - chunkMinX;

            int localZ =
                    worldZ(x, z) - chunkMinZ;

            int worldY = originY + y;

            if (!valid(
                    localX,
                    worldY,
                    localZ)) {

                return;
            }

            chunk.setBlock(
                    localX,
                    worldY,
                    localZ,
                    material
            );
        }

        private void setData(
                int x,
                int y,
                int z,
                BlockData data
        ) {

            int localX =
                    worldX(x, z) - chunkMinX;

            int localZ =
                    worldZ(x, z) - chunkMinZ;

            int worldY = originY + y;

            if (!valid(
                    localX,
                    worldY,
                    localZ)) {

                return;
            }

            chunk.setBlock(
                    localX,
                    worldY,
                    localZ,
                    data
            );
        }
    }
}
