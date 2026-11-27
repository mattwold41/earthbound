
package com.earthbound;

import java.lang.reflect.Proxy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import com.earthbound.EarthAlphaResidentialGenerator.ResidentialSite;
import com.earthbound.EarthResidentialInstaller.ConstructionChunk;
import com.earthbound.EarthResidentialInstallation.InstallationStatus;

import com.earthbound.terrain.EarthTerrainLoader;
import com.earthbound.roads.EarthRoadData;
import com.earthbound.roads.EarthRoadSpatialIndex;

public final class EarthResidentialVerification {

    /*
     * =========================================================
     * EARTHBOUND - PHYSICAL RESIDENTIAL VERIFICATION
     * =========================================================
     *
     * Read-only verification of completed construction output.
     *
     * Reuse the existing residential generator to create
     * an expected block layout entirely in memory.
     *
     * Compare the expected layout with actual world blocks.
     *
     * This class must never:
     *
     * - Generate real world chunks.
     * - Load missing chunks.
     * - Modify actual world blocks.
     * - Automatically repair houses.
     * - Authorize construction.
     * - Mark installations as completed.
     * - Activate property sale markers.
     * - Transfer property ownership.
     *
     * Verification is not installation completion.
     * =========================================================
     */

    private static final String WORLD_NAME = "earthbound";

    private static final int REQUIRED_HOMES = 6;

    private static final int CHUNK_SIZE = 16;

    private static final int MAX_EXPECTED_BLOCKS = 100000;

    private static final int MAX_REPORTED_MISMATCHES = 20;

    private static final int MINIMUM_EXPECTED_BLOCKS = 1000;

    private static final int MAX_WORLD_COORDINATE = 29999900;

    private EarthResidentialVerification() {
    }

    /*
     * =========================================================
     * VERIFICATION STATUS
     * =========================================================
     */

    public enum VerificationStatus {

        VERIFIED,

        INVALID_PLUGIN,

        MAIN_THREAD_REQUIRED,

        INVALID_WORLD,

        INVALID_PROPERTY,

        GEOGRAPHIC_DATA_UNAVAILABLE,

        INSTALLATION_STORAGE_UNAVAILABLE,

        ECONOMY_UNAVAILABLE,

        PROPERTY_SYSTEM_UNAVAILABLE,

        INVALID_INSTALLATION_STATUS,

        INSTALLATION_IDENTITY_MISMATCH,

        PROPERTY_ALREADY_OWNED,

        PROPERTY_BOUNDARY_MISMATCH,

        UNSAFE_GEOGRAPHIC_SITE,

        UNSAFE_DEVELOPMENT,

        INVALID_BUILD_HEIGHT,

        INVALID_CHUNK_PLAN,

        CONSTRUCTION_STILL_ACTIVE,

        REQUIRED_CHUNK_NOT_GENERATED,

        REQUIRED_CHUNK_NOT_LOADED,

        INVALID_BLUEPRINT,

        STRUCTURE_MISMATCH,

        VERIFICATION_FAILED
    }

    /*
     * =========================================================
     * IMMUTABLE VERIFICATION RESULT
     * =========================================================
     */

    public static final class VerificationResult {

        private final VerificationStatus status;

        private final String message;

        private final UUID worldId;

        private final String propertyId;

        private final int expectedBlocks;

        private final int checkedBlocks;

        private final int mismatchedBlocks;

        private VerificationResult(
                VerificationStatus status,
                String message,
                UUID worldId,
                String propertyId,
                int expectedBlocks,
                int checkedBlocks,
                int mismatchedBlocks
        ) {

            this.status = status;
            this.message = message;

            this.worldId = worldId;
            this.propertyId = propertyId;

            this.expectedBlocks = expectedBlocks;
            this.checkedBlocks = checkedBlocks;
            this.mismatchedBlocks = mismatchedBlocks;
        }

        public VerificationStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public UUID getWorldId() {
            return worldId;
        }

        public String getPropertyId() {
            return propertyId;
        }

        public int getExpectedBlocks() {
            return expectedBlocks;
        }

        public int getCheckedBlocks() {
            return checkedBlocks;
        }

        public int getMismatchedBlocks() {
            return mismatchedBlocks;
        }

        public boolean isVerified() {

            return status == VerificationStatus.VERIFIED
                    && expectedBlocks >= MINIMUM_EXPECTED_BLOCKS
                    && checkedBlocks == expectedBlocks
                    && mismatchedBlocks == 0;
        }
    }

    /*
     * =========================================================
     * BLOCK COORDINATE
     * =========================================================
     */

    private record BlockPosition(
            int x,
            int y,
            int z
    ) {
    }

    /*
     * =========================================================
     * EXPECTED BLOCK
     * =========================================================
     */

    private static final class ExpectedBlock {

        private final Material material;

        private final BlockData blockData;

        private ExpectedBlock(
                Material material,
                BlockData blockData
        ) {

            if (material == null || !material.isBlock()) {

                throw new IllegalArgumentException(
                        "Invalid expected block material."
                );
            }

            this.material = material;

            this.blockData =
                    blockData == null
                            ? null
                            : blockData.clone();
        }
    }

    /*
     * =========================================================
     * MAIN VERIFICATION ENTRY POINT
     * =========================================================
     */

    public static VerificationResult verify(
            EarthBound plugin,
            World world,
            String propertyId,
            boolean geographicDataReady
    ) {

        if (plugin == null || !plugin.isEnabled()) {

            return failure(
                    VerificationStatus.INVALID_PLUGIN,
                    "EarthBound is unavailable.",
                    null,
                    propertyId
            );
        }

        if (!Bukkit.isPrimaryThread()) {

            return failure(
                    VerificationStatus.MAIN_THREAD_REQUIRED,
                    "Verification must run on the server main thread.",
                    null,
                    propertyId
            );
        }

        if (world == null
                || !WORLD_NAME.equals(world.getName())
                || Bukkit.getWorld(world.getUID()) != world) {

            return failure(
                    VerificationStatus.INVALID_WORLD,
                    "The loaded EarthBound world is required.",
                    null,
                    propertyId
            );
        }

        UUID worldId = world.getUID();

        if (!isValidPropertyId(propertyId)) {

            return failure(
                    VerificationStatus.INVALID_PROPERTY,
                    "Invalid Alpha residential property ID.",
                    worldId,
                    propertyId
            );
        }

        try {

            return verifyInternal(
                    plugin,
                    world,
                    propertyId,
                    geographicDataReady
            );

        } catch (RuntimeException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Physical residential verification failed: "
                            + propertyId,
                    exception
            );

            return failure(
                    VerificationStatus.VERIFICATION_FAILED,
                    "Verification encountered an unexpected error.",
                    worldId,
                    propertyId
            );
        }
    }

    /*
     * =========================================================
     * INTERNAL VERIFICATION
     * =========================================================
     */

    private static VerificationResult verifyInternal(
            EarthBound plugin,
            World world,
            String propertyId,
            boolean geographicDataReady
    ) {

        UUID worldId = world.getUID();

        /*
         * REQUIRE GEOGRAPHIC SYSTEMS
         */

        if (!geographicDataReady
                || !EarthTerrainLoader.isLoaded()
                || !EarthRoadData.isLoaded()
                || !EarthRoadSpatialIndex.isLoaded()) {

            return failure(
                    VerificationStatus.GEOGRAPHIC_DATA_UNAVAILABLE,
                    "Required geographic data is unavailable.",
                    worldId,
                    propertyId
            );
        }

        /*
         * REQUIRE PERSISTENT INSTALLATION STORAGE
         */

        if (!EarthResidentialInstallation.isReady()) {

            return failure(
                    VerificationStatus.INSTALLATION_STORAGE_UNAVAILABLE,
                    "Installation storage is unavailable.",
                    worldId,
                    propertyId
            );
        }

        /*
         * REQUIRE ECONOMY AND PROPERTY SYSTEMS
         */

        if (!EarthEconomy.isReady()) {

            return failure(
                    VerificationStatus.ECONOMY_UNAVAILABLE,
                    "The economy system is unavailable.",
                    worldId,
                    propertyId
            );
        }

        if (!EarthPropertyManager.isReady()) {

            return failure(
                    VerificationStatus.PROPERTY_SYSTEM_UNAVAILABLE,
                    "The property system is unavailable.",
                    worldId,
                    propertyId
            );
        }

        /*
         * CONSTRUCTION MUST NOT STILL BE GENERATING
         */

        if (EarthGuemesGenerator
                .isResidentialGenerationEnabled()) {

            return failure(
                    VerificationStatus.CONSTRUCTION_STILL_ACTIVE,
                    "Residential construction is still enabled.",
                    worldId,
                    propertyId
            );
        }

        if (EarthResidentialExecution.hasActiveExecution()) {

            if (!propertyId.equals(
                    EarthResidentialExecution.getActivePropertyId()
            ) || EarthResidentialExecution.getExecutionStatus()
                    != EarthResidentialExecution.ExecutionStatus
                        .AWAITING_VERIFICATION) {

                return failure(
                        VerificationStatus.CONSTRUCTION_STILL_ACTIVE,
                        "Execution is not awaiting verification "
                                + "for this property.",
                        worldId,
                        propertyId
                );
            }
        }

        /*
         * FIND CURRENT REGISTERED PROPERTY
         */

        List<ResidentialSite> sites =
                List.copyOf(
                        EarthGuemesGenerator.getResidentialSites()
                );

        if (sites.size() != REQUIRED_HOMES
                || !EarthAlphaSiteFinder
                        .hasDevelopmentChunkClearance(sites)) {

            return failure(
                    VerificationStatus.UNSAFE_DEVELOPMENT,
                    "Residential development clearance is invalid.",
                    worldId,
                    propertyId
            );
        }

        ResidentialSite site = null;

        for (ResidentialSite candidate : sites) {

            if (propertyId.equals(candidate.getPropertyId())) {

                site = candidate;
                break;
            }
        }

        if (site == null || !isValidSite(site)) {

            return failure(
                    VerificationStatus.INVALID_PROPERTY,
                    "The requested residential site is invalid.",
                    worldId,
                    propertyId
            );
        }

        /*
         * REQUIRE IN-PROGRESS INSTALLATION RECORD
         *
         * This verification is intended for initial
         * government-owned property installation.
         *
         * Do not use it to inspect player-owned houses
         * that may have been legitimately modified.
         */

        InstallationStatus installationStatus =
                EarthResidentialInstallation.getStatus(propertyId);

        if (installationStatus != InstallationStatus.IN_PROGRESS) {

            return failure(
                    VerificationStatus.INVALID_INSTALLATION_STATUS,
                    "Installation must be IN_PROGRESS.",
                    worldId,
                    propertyId
            );
        }

        /*
         * VERIFY PERSISTENT INSTALLATION IDENTITY
         */

        if (!EarthResidentialInstallation
                .matchesApprovedSite(world, site)) {

            return failure(
                    VerificationStatus.INSTALLATION_IDENTITY_MISMATCH,
                    "Saved installation identity does not match.",
                    worldId,
                    propertyId
            );
        }

        /*
         * REQUIRE GOVERNMENT OWNERSHIP
         */

        if (EarthEconomy.getPropertyOwner(propertyId) != null) {

            return failure(
                    VerificationStatus.PROPERTY_ALREADY_OWNED,
                    "Property already has a player owner.",
                    worldId,
                    propertyId
            );
        }

        /*
         * INSPECT ALL 768 PROPERTY COLUMNS
         */

        if (!verifyPropertyBoundaries(world, site)) {

            return failure(
                    VerificationStatus.PROPERTY_BOUNDARY_MISMATCH,
                    "Registered property boundaries do not match.",
                    worldId,
                    propertyId
            );
        }

        /*
         * RECHECK GEOGRAPHIC CONDITIONS
         *
         * Do not call the undeveloped-land inspection.
         *
         * Generated chunks are expected at this stage.
         */

        if (!EarthResidentialSafety
                .inspectGeography(site)
                .isSafe()) {

            return failure(
                    VerificationStatus.UNSAFE_GEOGRAPHIC_SITE,
                    "Residential geography failed verification.",
                    worldId,
                    propertyId
            );
        }

        /*
         * VERIFY BUILD HEIGHT
         */

        if ((long) site.getGroundY() - 12
                    < world.getMinHeight()
                || (long) site.getGroundY()
                    + site.getDesign().getMaximumHeight()
                    + 12 >= world.getMaxHeight()) {

            return failure(
                    VerificationStatus.INVALID_BUILD_HEIGHT,
                    "Residential structure exceeds world height.",
                    worldId,
                    propertyId
            );
        }

        /*
         * CALCULATE REQUIRED CHUNKS
         */

        List<ConstructionChunk> requiredChunks =
                EarthResidentialInstaller
                        .calculateRequiredChunks(site);

        if (requiredChunks == null
                || requiredChunks.isEmpty()
                || requiredChunks.size() > 16) {

            return failure(
                    VerificationStatus.INVALID_CHUNK_PLAN,
                    "Invalid residential construction chunk plan.",
                    worldId,
                    propertyId
            );
        }

        /*
         * ALL REQUIRED CHUNKS MUST ALREADY EXIST
         * AND MUST ALREADY BE LOADED.
         */

        for (ConstructionChunk required : requiredChunks) {

            int chunkX = required.getChunkX();
            int chunkZ = required.getChunkZ();

            if (!world.isChunkGenerated(chunkX, chunkZ)) {

                return failure(
                        VerificationStatus.REQUIRED_CHUNK_NOT_GENERATED,
                        "Required chunk has not been generated: "
                                + chunkX + ", " + chunkZ,
                        worldId,
                        propertyId
                );
            }

            if (!world.isChunkLoaded(chunkX, chunkZ)) {

                return failure(
                        VerificationStatus.REQUIRED_CHUNK_NOT_LOADED,
                        "Required chunk is not loaded: "
                                + chunkX + ", " + chunkZ,
                        worldId,
                        propertyId
                );
            }
        }

        /*
         * =====================================================
         * CREATE IN-MEMORY RESIDENTIAL BLUEPRINT
         * =====================================================
         */

        Map<BlockPosition, ExpectedBlock> expected =
                new HashMap<>();

        for (ConstructionChunk required : requiredChunks) {

            int chunkX = required.getChunkX();

            int chunkZ = required.getChunkZ();

            ChunkData recorder =
                    createRecordingChunk(
                            world,
                            site,
                            chunkX,
                            chunkZ,
                            expected
                    );

            /*
             * This is an in-memory recording object.
             *
             * It is not an actual Minecraft world chunk.
             */

            EarthAlphaResidentialGenerator.generate(
                    recorder,
                    chunkX,
                    chunkZ,
                    site
            );
        }

        /*
         * REJECT EMPTY OR SUSPICIOUS BLUEPRINTS
         */

        if (expected.size() < MINIMUM_EXPECTED_BLOCKS
                || expected.size() > MAX_EXPECTED_BLOCKS
                || !validateBlueprintCoverage(site, expected)) {

            return failure(
                    VerificationStatus.INVALID_BLUEPRINT,
                    "Residential blueprint is incomplete or invalid.",
                    worldId,
                    propertyId
            );
        }

        /*
         * =====================================================
         * COMPARE ACTUAL WORLD BLOCKS
         * =====================================================
         */

        plugin.getLogger().info(
                "========================================"
        );

        plugin.getLogger().info(
                "EARTHBOUND PHYSICAL STRUCTURE VERIFICATION"
        );

        plugin.getLogger().info(
                "Property: " + propertyId
        );

        plugin.getLogger().info(
                "Design: "
                        + site.getDesign().getDisplayName()
        );

        plugin.getLogger().info(
                "World UUID: " + worldId
        );

        plugin.getLogger().info(
                "Required chunks: " + requiredChunks.size()
        );

        plugin.getLogger().info(
                "Expected final block placements: "
                        + expected.size()
        );

        long startTime = System.nanoTime();

        int checked = 0;

        int mismatches = 0;

        for (Map.Entry<BlockPosition, ExpectedBlock> entry
                : expected.entrySet()) {

            BlockPosition position = entry.getKey();

            ExpectedBlock expectedBlock = entry.getValue();

            int chunkX =
                    Math.floorDiv(position.x(), CHUNK_SIZE);

            int chunkZ =
                    Math.floorDiv(position.z(), CHUNK_SIZE);

            /*
             * Never deliberately load a missing chunk.
             *
             * Recheck before accessing actual blocks.
             */

            if (!world.isChunkLoaded(chunkX, chunkZ)) {

                return new VerificationResult(
                        VerificationStatus.REQUIRED_CHUNK_NOT_LOADED,
                        "A required chunk became unavailable.",
                        worldId,
                        propertyId,
                        expected.size(),
                        checked,
                        mismatches
                );
            }

            Block actual =
                    world.getBlockAt(
                            position.x(),
                            position.y(),
                            position.z()
                    );

            checked++;

            if (!matchesExpectedBlock(actual, expectedBlock)) {

                mismatches++;

                if (mismatches <= MAX_REPORTED_MISMATCHES) {

                    plugin.getLogger().warning(
                            "Residential block mismatch at "
                                    + position.x()
                                    + ", " + position.y()
                                    + ", " + position.z()
                                    + " | Expected: "
                                    + describeExpected(expectedBlock)
                                    + " | Actual: "
                                    + actual.getBlockData().getAsString()
                    );
                }
            }
        }

        long elapsedMilliseconds =
                (System.nanoTime() - startTime)
                        / 1000000L;

        plugin.getLogger().info(
                "Blocks checked: " + checked
        );

        plugin.getLogger().info(
                "Block mismatches: " + mismatches
        );

        plugin.getLogger().info(
                "Physical comparison duration: "
                        + elapsedMilliseconds + " ms"
        );

        if (mismatches > MAX_REPORTED_MISMATCHES) {

            plugin.getLogger().warning(
                    "Additional mismatches not individually logged: "
                            + (mismatches - MAX_REPORTED_MISMATCHES)
            );
        }

        /*
         * REJECT ANY PHYSICAL MISMATCH
         */

        if (mismatches != 0
                || checked != expected.size()) {

            plugin.getLogger().warning(
                    "Residential physical verification FAILED."
            );

            plugin.getLogger().warning(
                    "Property must remain unavailable for sale."
            );

            plugin.getLogger().warning(
                    "Automatic structure repair is prohibited."
            );

            plugin.getLogger().info(
                    "========================================"
            );

            return new VerificationResult(
                    VerificationStatus.STRUCTURE_MISMATCH,
                    "Actual construction does not match the blueprint.",
                    worldId,
                    propertyId,
                    expected.size(),
                    checked,
                    mismatches
            );
        }

        /*
         * FINAL INSTALLATION STATE CHECK
         */

        if (!EarthResidentialInstallation.isReady()
                || EarthResidentialInstallation.getStatus(propertyId)
                    != InstallationStatus.IN_PROGRESS
                || !EarthResidentialInstallation
                        .matchesApprovedSite(world, site)
                || !EarthEconomy.isReady()
                || EarthEconomy.getPropertyOwner(propertyId) != null
                || EarthGuemesGenerator
                        .isResidentialGenerationEnabled()) {

            return new VerificationResult(
                    VerificationStatus.VERIFICATION_FAILED,
                    "Installation conditions changed during verification.",
                    worldId,
                    propertyId,
                    expected.size(),
                    checked,
                    mismatches
            );
        }

        /*
         * VERIFICATION SUCCESS
         *
         * Do not update the persistent installation record.
         *
         * Completion authorization is a separate step.
         */

        plugin.getLogger().info(
                "Residential physical verification PASSED."
        );

        plugin.getLogger().info(
                "All recorded expected block placements match."
        );

        plugin.getLogger().info(
                "Installation completion has not been authorized."
        );

        plugin.getLogger().info(
                "Property sale markers remain disabled."
        );

        plugin.getLogger().info(
                "========================================"
        );

        return new VerificationResult(
                VerificationStatus.VERIFIED,
                "Recorded residential structure verified.",
                worldId,
                propertyId,
                expected.size(),
                checked,
                mismatches
        );
    }

    /*
     * =========================================================
     * IN-MEMORY CHUNK RECORDER
     * =========================================================
     *
     * Implements only the ChunkData operations needed by
     * our current residential and architectural generators.
     *
     * Unsupported operations fail closed.
     *
     * No actual ChunkData object is requested from a world.
     * No real chunk is generated or modified.
     */

    private static ChunkData createRecordingChunk(
            World world,
            ResidentialSite site,
            int chunkX,
            int chunkZ,
            Map<BlockPosition, ExpectedBlock> expected
    ) {

        final int minHeight = world.getMinHeight();

        final int maxHeight = world.getMaxHeight();

        return (ChunkData) Proxy.newProxyInstance(

                ChunkData.class.getClassLoader(),

                new Class<?>[]{ChunkData.class},

                (proxy, method, arguments) -> {

                    String methodName = method.getName();

                    switch (methodName) {

                        case "getMinHeight":

                            return minHeight;

                        case "getMaxHeight":

                            return maxHeight;

                        case "setBlock":

                            if (arguments == null
                                    || arguments.length != 4) {

                                throw new UnsupportedOperationException(
                                        "Unsupported blueprint setBlock."
                                );
                            }

                            int localX = (Integer) arguments[0];

                            int y = (Integer) arguments[1];

                            int localZ = (Integer) arguments[2];

                            Object blockValue = arguments[3];

                            recordExpectedBlock(
                                    site,
                                    chunkX,
                                    chunkZ,
                                    minHeight,
                                    maxHeight,
                                    localX,
                                    y,
                                    localZ,
                                    blockValue,
                                    expected
                            );

                            return null;

                        case "toString":

                            return "EarthBoundRecordingChunk["
                                    + chunkX + ","
                                    + chunkZ + "]";

                        case "hashCode":

                            return System.identityHashCode(proxy);

                        case "equals":

                            return arguments != null
                                    && arguments.length == 1
                                    && proxy == arguments[0];

                        default:

                            throw new UnsupportedOperationException(
                                    "Unsupported blueprint operation: "
                                            + methodName
                            );
                    }
                }
        );
    }

    /*
     * =========================================================
     * RECORD EXPECTED BLOCK PLACEMENT
     * =========================================================
     *
     * Later placements replace earlier placements
     * at the same coordinate.
     *
     * This correctly handles:
     *
     * - Windows replacing exterior walls.
     * - Doors replacing wall blocks.
     * - Staircase openings replacing flooring.
     * - Chimneys replacing roof blocks.
     * - Landscaping replacing prepared grass.
     */

    private static void recordExpectedBlock(
            ResidentialSite site,
            int chunkX,
            int chunkZ,
            int minHeight,
            int maxHeight,
            int localX,
            int y,
            int localZ,
            Object blockValue,
            Map<BlockPosition, ExpectedBlock> expected
    ) {

        if (localX < 0 || localX >= CHUNK_SIZE
                || localZ < 0 || localZ >= CHUNK_SIZE
                || y < minHeight || y >= maxHeight) {

            throw new IllegalStateException(
                    "Blueprint placement exceeds chunk boundaries."
            );
        }

        int worldX =
                Math.addExact(
                        Math.multiplyExact(chunkX, CHUNK_SIZE),
                        localX
                );

        int worldZ =
                Math.addExact(
                        Math.multiplyExact(chunkZ, CHUNK_SIZE),
                        localZ
                );

        /*
         * Do not accept recorded placements
         * outside the residential property.
         */

        if (worldX < site.getMinX()
                || worldX > site.getMaxX()
                || worldZ < site.getMinZ()
                || worldZ > site.getMaxZ()) {

            throw new IllegalStateException(
                    "Blueprint placement exceeds property boundaries."
            );
        }

        if (y < site.getGroundY() - 12
                || y > site.getGroundY()
                    + site.getDesign().getMaximumHeight() + 12) {

            throw new IllegalStateException(
                    "Blueprint placement exceeds construction height."
            );
        }

        ExpectedBlock block;

        if (blockValue instanceof Material material) {

            block = new ExpectedBlock(
                    material,
                    null
            );

        } else if (blockValue instanceof BlockData data) {

            block = new ExpectedBlock(
                    data.getMaterial(),
                    data
            );

        } else {

            throw new UnsupportedOperationException(
                    "Unsupported blueprint block value."
            );
        }

        BlockPosition position =
                new BlockPosition(worldX, y, worldZ);

        if (!expected.containsKey(position)
                && expected.size() >= MAX_EXPECTED_BLOCKS) {

            throw new IllegalStateException(
                    "Residential blueprint exceeds safety limit."
            );
        }

        /*
         * Preserve only the final expected state.
         */

        expected.put(position, block);
    }

    /*
     * =========================================================
     * BLUEPRINT COVERAGE VALIDATION
     * =========================================================
     *
     * Confirm all property columns contain recorded output.
     *
     * Independently require expected foundation,
     * door and roof materials.
     *
     * These checks help reject empty or partial recordings.
     */

    private static boolean validateBlueprintCoverage(
            ResidentialSite site,
            Map<BlockPosition, ExpectedBlock> expected
    ) {

        boolean[][] covered =
                new boolean[
                        site.getMaxX() - site.getMinX() + 1
                ][
                        site.getMaxZ() - site.getMinZ() + 1
                ];

        int foundationBlocks = 0;

        int lowerDoors = 0;

        int upperDoors = 0;

        int roofStairs = 0;

        for (Map.Entry<BlockPosition, ExpectedBlock> entry
                : expected.entrySet()) {

            BlockPosition position = entry.getKey();

            ExpectedBlock block = entry.getValue();

            int offsetX =
                    position.x() - site.getMinX();

            int offsetZ =
                    position.z() - site.getMinZ();

            covered[offsetX][offsetZ] = true;

            if (position.y() == site.getGroundY()
                    && block.material == Material.STONE_BRICKS) {

                foundationBlocks++;
            }

            if (block.blockData instanceof Door door) {

                if (door.getFacing() != site.getFacing()) {

                    return false;
                }

                if (door.getHalf() == Bisected.Half.BOTTOM) {

                    lowerDoors++;

                } else {

                    upperDoors++;
                }
            }

            if (block.blockData instanceof Stairs
                    && position.y()
                        >= site.getGroundY()
                            + site.getDesign().getRoofBaseHeight()) {

                roofStairs++;
            }
        }

        for (boolean[] column : covered) {

            for (boolean present : column) {

                if (!present) {

                    return false;
                }
            }
        }

        int requiredFoundation =
                site.getDesign().getWidth()
                        * site.getDesign().getDepth();

        return foundationBlocks >= requiredFoundation
                && lowerDoors == 1
                && upperDoors == 1
                && roofStairs > 0;
    }

    /*
     * =========================================================
     * ACTUAL BLOCK COMPARISON
     * =========================================================
     */

    private static boolean matchesExpectedBlock(
            Block actual,
            ExpectedBlock expected
    ) {

        if (actual == null || expected == null) {

            return false;
        }

        /*
         * Accept Minecraft air variants as empty space.
         */

        if (expected.material.isAir()) {

            return actual.getType().isAir();
        }

        /*
         * Require exact construction material.
         */

        if (actual.getType() != expected.material) {

            return false;
        }

        /*
         * Material-only placements do not require
         * exact dynamic BlockData.
         *
         * For example, fence connections may change
         * when neighboring chunks become available.
         */

        if (expected.blockData == null) {

            return true;
        }

        BlockData actualData = actual.getBlockData();

        /*
         * Preserve structural door requirements.
         *
         * Opening a door does not change its identity.
         */

        if (expected.blockData instanceof Door expectedDoor) {

            if (!(actualData instanceof Door actualDoor)) {

                return false;
            }

            return actualDoor.getFacing()
                        == expectedDoor.getFacing()

                    && actualDoor.getHalf()
                        == expectedDoor.getHalf()

                    && actualDoor.getHinge()
                        == expectedDoor.getHinge();
        }

        /*
         * Preserve essential stair orientation.
         *
         * Minecraft may update stair corner shapes
         * after neighboring blocks are available.
         */

        if (expected.blockData instanceof Stairs expectedStairs) {

            if (!(actualData instanceof Stairs actualStairs)) {

                return false;
            }

            return actualStairs.getFacing()
                        == expectedStairs.getFacing()

                    && actualStairs.getHalf()
                        == expectedStairs.getHalf()

                    && actualStairs.isWaterlogged()
                        == expectedStairs.isWaterlogged();
        }

        /*
         * Require exact state for any other explicitly
         * recorded BlockData type.
         */

        return actualData.equals(expected.blockData);
    }

    /*
     * =========================================================
     * PROPERTY BOUNDARY VERIFICATION
     * =========================================================
     */

    private static boolean verifyPropertyBoundaries(
            World world,
            ResidentialSite site
    ) {

        for (int x = 0;
                x < EarthAlphaResidentialGenerator.PLOT_WIDTH;
                x++) {

            for (int z = 0;
                    z < EarthAlphaResidentialGenerator.PLOT_LENGTH;
                    z++) {

                Location location =
                        new Location(
                                world,
                                site.getWorldX(x, z),
                                site.getGroundY(),
                                site.getWorldZ(x, z)
                        );

                var property =
                        EarthPropertyManager.getPropertyAt(location);

                if (property == null
                        || !site.getPropertyId().equals(
                                property.getId()
                        )
                        || property.isOwned()

                        || property.getMinX() != site.getMinX()
                        || property.getMaxX() != site.getMaxX()

                        || property.getMinZ() != site.getMinZ()
                        || property.getMaxZ() != site.getMaxZ()) {

                    return false;
                }
            }
        }

        return true;
    }

    /*
     * =========================================================
     * SITE VALIDATION
     * =========================================================
     */

    private static boolean isValidSite(
            ResidentialSite site
    ) {

        if (site == null
                || site.getDesign() == null
                || !isValidPropertyId(site.getPropertyId())) {

            return false;
        }

        if (site.getFacing() != BlockFace.NORTH
                && site.getFacing() != BlockFace.SOUTH) {

            return false;
        }

        if (site.getGroundY() < 66
                || site.getGroundY() > 280) {

            return false;
        }

        if (site.getMinX() < -MAX_WORLD_COORDINATE
                || site.getMaxX() > MAX_WORLD_COORDINATE
                || site.getMinZ() < -MAX_WORLD_COORDINATE
                || site.getMaxZ() > MAX_WORLD_COORDINATE) {

            return false;
        }

        long width =
                (long) site.getMaxX()
                        - site.getMinX() + 1;

        long length =
                (long) site.getMaxZ()
                        - site.getMinZ() + 1;

        return width == EarthAlphaResidentialGenerator.PLOT_WIDTH
                && length == EarthAlphaResidentialGenerator.PLOT_LENGTH;
    }

    /*
     * =========================================================
     * EXPECTED BLOCK DESCRIPTION
     * =========================================================
     */

    private static String describeExpected(
            ExpectedBlock expected
    ) {

        if (expected.blockData != null) {

            return expected.blockData.getAsString();
        }

        return expected.material.name();
    }

    /*
     * =========================================================
     * PROPERTY ID VALIDATION
     * =========================================================
     */

    private static boolean isValidPropertyId(
            String propertyId
    ) {

        return propertyId != null
                && propertyId.matches("guemes_home_0[1-6]");
    }

    /*
     * =========================================================
     * FAILURE RESULT HELPER
     * =========================================================
     */

    private static VerificationResult failure(
            VerificationStatus status,
            String message,
            UUID worldId,
            String propertyId
    ) {

        return new VerificationResult(
                status,
                message,
                worldId,
                propertyId,
                0,
                0,
                0
        );
    }
}
