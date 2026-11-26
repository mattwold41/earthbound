
package com.earthbound;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.scheduler.BukkitTask;

import com.earthbound.EarthResidentialInstaller
        .ConstructionChunk;

import com.earthbound.EarthResidentialInstaller
        .DevelopmentPlan;

import com.earthbound.EarthResidentialInstaller
        .PropertyPlan;

import com.earthbound.EarthResidentialInstaller
        .PreflightResult;

import com.earthbound.EarthResidentialInstallation
        .InstallationStatus;

import com.earthbound.EarthResidentialAuthorization
        .AuthorizationResult;

/*
 * ============================================================
 * EARTHBOUND RESIDENTIAL EXECUTION
 * Friends Alpha
 * ============================================================
 *
 * Coordinates controlled residential chunk generation.
 *
 * Responsibilities:
 *
 * - Final installation preflight.
 * - Explicit console authorization.
 * - Persistent installation start.
 * - Individual construction permit.
 * - Sequential asynchronous chunk requests.
 * - Main-thread progress monitoring.
 * - Construction failure handling.
 * - Construction permit withdrawal.
 *
 * Successful generation does NOT mean a completed house.
 *
 * Physical structure verification must occur separately.
 *
 * Physical construction remains locked.
 * ============================================================
 */

public final class EarthResidentialExecution {

    /*
     * Additional execution integration safety lock.
     *
     * Must remain false until lifecycle integration
     * and physical verification are implemented.
     */

    private static final boolean EXECUTION_APPROVED =
            false;

    private static final String WORLD_NAME =
            "earthbound";

    private static final String START_CONFIRMATION =
            "START-GUEMES-INSTALLATION";

    private static final long CHUNK_TIMEOUT_NANOS =
            120_000_000_000L;

    /*
     * Execution state is accessed exclusively
     * on the server main thread.
     */

    private static ExecutionSession activeSession;

    private EarthResidentialExecution() {
    }

    /*
     * ========================================================
     * EXECUTION STATUS
     * ========================================================
     */

    public enum ExecutionStatus {

        STARTED,

        EXECUTION_LOCKED,

        INVALID_PLUGIN,

        MAIN_THREAD_REQUIRED,

        ADMINISTRATOR_REQUIRED,

        CONFIRMATION_REQUIRED,

        INVALID_WORLD,

        EXECUTION_ALREADY_ACTIVE,

        PREFLIGHT_REJECTED,

        AUTHORIZATION_REJECTED,

        PERMIT_REJECTED,

        EXECUTION_FAILED,

        GENERATING,

        AWAITING_VERIFICATION,

        FAILED
    }

    /*
     * ========================================================
     * EXECUTION RESULT
     * ========================================================
     */

    public static final class ExecutionResult {

        private final ExecutionStatus status;

        private final String message;

        private ExecutionResult(
                ExecutionStatus status,
                String message
        ) {

            this.status = status;
            this.message = message;
        }

        public ExecutionStatus getStatus() {

            return status;
        }

        public String getMessage() {

            return message;
        }

        public boolean isStarted() {

            return status == ExecutionStatus.STARTED;
        }
    }

    /*
     * ========================================================
     * INDIVIDUAL EXECUTION SESSION
     * ========================================================
     */

    private static final class ExecutionSession {

        private final EarthBound plugin;

        private final World world;

        private final UUID worldId;

        private final String propertyId;

        private final PropertyPlan property;

        private final DevelopmentPlan development;

        private final List<ConstructionChunk> chunks;

        private ExecutionStatus status =
                ExecutionStatus.GENERATING;

        private BukkitTask monitorTask;

        private CompletableFuture<Chunk> pendingChunk;

        private long requestStartedAt;

        private int nextChunkIndex;

        private boolean installationStarted;

        private boolean permitIssued;

        private ExecutionSession(
                EarthBound plugin,
                World world,
                DevelopmentPlan development,
                PropertyPlan property
        ) {

            this.plugin = plugin;

            this.world = world;

            this.worldId = world.getUID();

            this.propertyId =
                    property.getPropertyId();

            this.property = property;

            this.development = development;

            this.chunks =
                    List.copyOf(property.getChunks());
        }
    }

    /*
     * ========================================================
     * START CONTROLLED RESIDENTIAL EXECUTION
     * ========================================================
     *
     * Must be called on the server main thread.
     *
     * No construction is allowed without:
     *
     * - Execution integration approval.
     * - Installer physical construction approval.
     * - Explicit console authorization.
     * - Successful installation preflight.
     * - Persistent IN_PROGRESS record.
     * - Individual construction permit.
     * ========================================================
     */

    public static ExecutionResult start(
            EarthBound plugin,
            CommandSender sender,
            World world,
            DevelopmentPlan development,
            String propertyId,
            boolean geographicDataReady,
            String confirmation
    ) {

        if (!EXECUTION_APPROVED

                || !EarthResidentialInstaller
                        .isPhysicalConstructionEnabled()) {

            return result(
                    ExecutionStatus.EXECUTION_LOCKED,
                    "Physical residential execution is locked."
            );
        }

        if (plugin == null || !plugin.isEnabled()) {

            return result(
                    ExecutionStatus.INVALID_PLUGIN,
                    "EarthBound is unavailable."
            );
        }

        if (!Bukkit.isPrimaryThread()) {

            return result(
                    ExecutionStatus.MAIN_THREAD_REQUIRED,
                    "Execution requires the server main thread."
            );
        }

        if (!(sender instanceof ConsoleCommandSender)) {

            return result(
                    ExecutionStatus.ADMINISTRATOR_REQUIRED,
                    "Installation must be authorized "
                    + "from the server console."
            );
        }

        if (!START_CONFIRMATION.equals(confirmation)) {

            return result(
                    ExecutionStatus.CONFIRMATION_REQUIRED,
                    "Explicit installation confirmation required."
            );
        }

        if (world == null

                || !WORLD_NAME.equals(world.getName())

                || Bukkit.getWorld(world.getUID()) != world) {

            return result(
                    ExecutionStatus.INVALID_WORLD,
                    "Invalid EarthBound world."
            );
        }

        /*
         * Never allow simultaneous installations.
         */

        if (activeSession != null) {

            return result(
                    ExecutionStatus.EXECUTION_ALREADY_ACTIVE,
                    "An installation already requires attention."
            );
        }

        ExecutionSession session = null;

        try {

            /*
             * Final read-only preflight.
             */

            PreflightResult preflight =
                    EarthResidentialInstaller
                            .checkExecutionReadiness(
                                    plugin,
                                    world,
                                    development,
                                    propertyId,
                                    geographicDataReady
                            );

            if (!preflight.isReady()) {

                return result(
                        ExecutionStatus.PREFLIGHT_REJECTED,
                        preflight.getMessage()
                );
            }

            session = new ExecutionSession(
                    plugin,
                    world,
                    development,
                    preflight.getProperty()
            );

            if (session.chunks.isEmpty()) {

                return result(
                        ExecutionStatus.PREFLIGHT_REJECTED,
                        "No construction chunks were approved."
                );
            }

            /*
             * Reserve the execution coordinator before
             * attempting persistent authorization.
             */

            activeSession = session;

            /*
             * Record explicit administrator authorization
             * and persist IN_PROGRESS before issuing permission.
             */

            AuthorizationResult authorization =
                    EarthResidentialAuthorization
                            .authorizeInstallationStart(
                                    plugin,
                                    sender,
                                    world,
                                    propertyId,
                                    geographicDataReady,
                                    confirmation
                            );

            if (!authorization.isAuthorized()) {

                /*
                 * Check whether authorization left an
                 * unresolved persistent installation.
                 */

                if (!EarthResidentialInstallation.isReady()) {

                    fail(
                            session,
                            "Installation storage unavailable "
                            + "after authorization."
                    );

                } else if (
                        EarthResidentialInstallation
                                .getStatus(propertyId)
                                == InstallationStatus.IN_PROGRESS
                ) {

                    session.installationStarted = true;

                    fail(
                            session,
                            "Authorization returned an "
                            + "unresolved installation."
                    );

                } else {

                    activeSession = null;
                }

                return result(
                        ExecutionStatus.AUTHORIZATION_REJECTED,
                        authorization.getMessage()
                );
            }

            session.installationStarted = true;

            /*
             * Independently verify persistent start.
             */

            if (!EarthResidentialInstallation.isReady()

                    || EarthResidentialInstallation
                            .getStatus(propertyId)
                            != InstallationStatus.IN_PROGRESS) {

                fail(
                        session,
                        "Persistent installation start "
                        + "could not be verified."
                );

                return result(
                        ExecutionStatus.EXECUTION_FAILED,
                        "Persistent installation start failed."
                );
            }

            /*
             * Issue permission for the exact property
             * and its exact construction chunks.
             */

            boolean permitIssued =
                    EarthGuemesGenerator
                            .issueConstructionPermit(
                                    plugin,
                                    world,
                                    session.property,
                                    geographicDataReady
                            );

            if (!permitIssued) {

                fail(
                        session,
                        "Individual construction permit rejected."
                );

                return result(
                        ExecutionStatus.PERMIT_REJECTED,
                        "Construction permit could not be issued."
                );
            }

            session.permitIssued = true;

            /*
             * All monitoring takes place on the main thread.
             *
             * Chunk generation itself is requested through
             * Paper's asynchronous chunk loading API.
             */

            final ExecutionSession monitoredSession =
                    session;

            session.monitorTask =
                    Bukkit.getScheduler().runTaskTimer(
                            plugin,
                            () -> monitor(monitoredSession),
                            1L,
                            1L
                    );

            plugin.getLogger().info(
                    "Residential execution started: "
                    + propertyId
            );

            plugin.getLogger().info(
                    "Construction world UUID: "
                    + session.worldId
            );

            plugin.getLogger().info(
                    "Required construction chunks: "
                    + session.chunks.size()
            );

            return result(
                    ExecutionStatus.STARTED,
                    "Controlled residential execution started."
            );

        } catch (RuntimeException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Residential execution could not start.",
                    exception
            );

            if (session != null
                    && activeSession == session) {

                fail(
                        session,
                        "Unexpected execution startup failure."
                );
            }

            return result(
                    ExecutionStatus.EXECUTION_FAILED,
                    "Residential execution startup failed."
            );
        }
    }

    /*
     * ========================================================
     * MAIN-THREAD EXECUTION MONITOR
     * ========================================================
     *
     * Never waits synchronously for an incomplete future.
     *
     * Only one explicit chunk request may be pending.
     *
     * Paper may independently generate additional
     * neighboring chunks.
     * ========================================================
     */

    private static void monitor(
            ExecutionSession session
    ) {

        if (!Bukkit.isPrimaryThread()) {

            return;
        }

        if (activeSession != session

                || session.status
                        != ExecutionStatus.GENERATING) {

            cancelMonitor(session);

            return;
        }

        try {

            String invalidReason =
                    validateActiveExecution(session);

            if (invalidReason != null) {

                fail(
                        session,
                        invalidReason
                );

                return;
            }

            /*
             * Inspect the pending asynchronous request.
             */

            if (session.pendingChunk != null) {

                if (!session.pendingChunk.isDone()) {

                    long elapsed =
                            System.nanoTime()

                            - session.requestStartedAt;

                    if (elapsed >= CHUNK_TIMEOUT_NANOS) {

                        fail(
                                session,
                                "Construction chunk request timed out."
                        );
                    }

                    return;
                }

                /*
                 * The future is already complete.
                 *
                 * join() does not wait for unfinished generation.
                 * Exceptional completion is handled below.
                 */

                Chunk completedChunk =
                        session.pendingChunk.join();

                ConstructionChunk expected =
                        session.chunks.get(
                                session.nextChunkIndex
                        );

                if (completedChunk == null

                        || !session.worldId.equals(
                                completedChunk.getWorld().getUID()
                        )

                        || completedChunk.getX()
                                != expected.getChunkX()

                        || completedChunk.getZ()
                                != expected.getChunkZ()) {

                    fail(
                            session,
                            "Unexpected construction chunk returned."
                    );

                    return;
                }

                session.pendingChunk = null;

                session.nextChunkIndex++;

                session.plugin.getLogger().info(
                        "Residential chunk request completed: "

                        + session.propertyId

                        + " ("

                        + session.nextChunkIndex

                        + "/"

                        + session.chunks.size()

                        + ")"
                );
            }

            /*
             * Every required chunk request has completed.
             */

            if (session.nextChunkIndex
                    >= session.chunks.size()) {

                finishGeneration(session);

                return;
            }

            /*
             * Request the next authorized chunk.
             */

            ConstructionChunk next =
                    session.chunks.get(
                            session.nextChunkIndex
                    );

            session.requestStartedAt =
                    System.nanoTime();

            session.pendingChunk =
                    session.world.getChunkAtAsync(
                            next.getChunkX(),
                            next.getChunkZ(),
                            true
                    );

            if (session.pendingChunk == null) {

                fail(
                        session,
                        "Paper did not return a chunk future."
                );
            }

        } catch (RuntimeException exception) {

            session.plugin.getLogger().log(
                    Level.SEVERE,
                    "Residential generation monitor failed.",
                    exception
            );

            fail(
                    session,
                    "Unexpected construction monitoring failure."
            );
        }
    }

    /*
     * ========================================================
     * REVALIDATE ACTIVE EXECUTION
     * ========================================================
     */

    private static String validateActiveExecution(
            ExecutionSession session
    ) {

        if (!EXECUTION_APPROVED

                || !EarthResidentialInstaller
                        .isPhysicalConstructionEnabled()) {

            return "Physical construction approval withdrawn.";
        }

        if (!session.plugin.isEnabled()) {

            return "EarthBound plugin is unavailable.";
        }

        if (Bukkit.getWorld(session.worldId)
                != session.world) {

            return "Construction world is unavailable.";
        }

        if (!EarthResidentialAuthorization
                .isAuthorizationEnabled(session.plugin)) {

            return "Installation authorization withdrawn.";
        }

        if (!EarthResidentialInstallation.isReady()) {

            return "Installation storage is unavailable.";
        }

        if (!EarthEconomy.isReady()

                || !EarthPropertyManager.isReady()) {

            return "Property ownership systems unavailable.";
        }

        if (EarthResidentialInstallation
                .getStatus(session.propertyId)

                != InstallationStatus.IN_PROGRESS) {

            return "Installation no longer IN_PROGRESS.";
        }

        if (!EarthResidentialInstallation
                .matchesApprovedSite(
                        session.world,
                        session.property.getSite()
                )) {

            return "Persistent installation identity changed.";
        }

        if (EarthEconomy.getPropertyOwner(
                session.propertyId
        ) != null) {

            return "Construction property has an owner.";
        }

        if (!EarthAlphaSiteFinder
                .hasDevelopmentChunkClearance(
                        EarthGuemesGenerator.getResidentialSites()
                )) {

            return "Residential chunk separation invalid.";
        }

        PlanResultAdapter boundaryResult =
                inspectCurrentProperty(session);

        if (!boundaryResult.safe) {

            return boundaryResult.message;
        }

        if (EarthGuemesGenerator.hasConstructionFailed(
                session.worldId,
                session.propertyId
        )) {

            return "Construction permit missing or failed.";
        }

        /*
         * Verify neighboring unfinished property chunks
         * have not been generated during this installation.
         */

        for (PropertyPlan neighboring
                : session.development.getProperties()) {

            String neighboringId =
                    neighboring.getPropertyId();

            if (session.propertyId.equals(neighboringId)) {

                continue;
            }

            InstallationStatus status =
                    EarthResidentialInstallation
                            .getStatus(neighboringId);

            if (!EarthResidentialInstallation
                    .matchesApprovedSite(
                            session.world,
                            neighboring.getSite()
                    )) {

                return "Neighboring installation identity changed: "

                        + neighboringId;
            }

            if (status == InstallationStatus.COMPLETED) {

                continue;
            }

            if (status != InstallationStatus.APPROVED) {

                return "Unresolved neighboring installation: "

                        + neighboringId;
            }

            if (EarthEconomy.getPropertyOwner(
                    neighboringId
            ) != null) {

                return "Unfinished neighboring property is owned: "

                        + neighboringId;
            }

            for (ConstructionChunk chunk
                    : neighboring.getChunks()) {

                if (session.world.isChunkLoaded(
                        chunk.getChunkX(),
                        chunk.getChunkZ()
                )

                        || session.world.isChunkGenerated(
                                chunk.getChunkX(),
                                chunk.getChunkZ()
                        )) {

                    return "Unexpected neighboring chunk generation: "

                            + neighboringId

                            + " at "

                            + chunk;
                }
            }
        }

        return null;
    }

    /*
     * ========================================================
     * ACTIVE PROPERTY BOUNDARY INSPECTION
     * ========================================================
     */

    private static final class PlanResultAdapter {

        private final boolean safe;

        private final String message;

        private PlanResultAdapter(
                boolean safe,
                String message
        ) {

            this.safe = safe;

            this.message = message;
        }
    }

    private static PlanResultAdapter
            inspectCurrentProperty(
                    ExecutionSession session
            ) {

        var site = session.property.getSite();

        for (int x = site.getMinX();
                x <= site.getMaxX();
                x++) {

            for (int z = site.getMinZ();
                    z <= site.getMaxZ();
                    z++) {

                var location =
                        new org.bukkit.Location(
                                session.world,
                                x,
                                site.getGroundY(),
                                z
                        );

                var property =
                        EarthPropertyManager
                                .getPropertyAt(location);

                if (property == null

                        || !session.propertyId.equals(
                                property.getId()
                        )

                        || property.isOwned()

                        || property.getMinX()
                                != site.getMinX()

                        || property.getMaxX()
                                != site.getMaxX()

                        || property.getMinZ()
                                != site.getMinZ()

                        || property.getMaxZ()
                                != site.getMaxZ()) {

                    return new PlanResultAdapter(
                            false,
                            "Active construction property "
                            + "protection changed."
                    );
                }
            }
        }

        return new PlanResultAdapter(
                true,
                "Property boundaries verified."
        );
    }

    /*
     * ========================================================
     * FINISH CHUNK GENERATION
     * ========================================================
     *
     * This is not installation completion.
     *
     * All required callbacks must have been observed.
     *
     * Physical verification and persistent COMPLETED
     * recording will be implemented separately.
     * ========================================================
     */

    private static void finishGeneration(
            ExecutionSession session
    ) {

        if (!EarthGuemesGenerator
                .hasProcessedAllConstructionChunks(
                        session.worldId,
                        session.propertyId
                )) {

            fail(
                    session,
                    "Required residential generation "
                    + "callbacks were not observed."
            );

            return;
        }

        /*
         * Withdraw permission before further verification.
         */

        EarthGuemesGenerator.revokeConstructionPermit();

        session.permitIssued = false;

        cancelMonitor(session);

        session.status =
                ExecutionStatus.AWAITING_VERIFICATION;

        /*
         * Retain the session to prevent another installation
         * from starting before verification is resolved.
         *
         * The persistent status intentionally remains
         * IN_PROGRESS.
         */

        session.plugin.getLogger().info(
                "Residential generation requests completed: "

                + session.propertyId
        );

        session.plugin.getLogger().info(
                "Construction permit withdrawn."
        );

        session.plugin.getLogger().warning(
                "Physical structure verification required."
        );

        session.plugin.getLogger().warning(
                "Property remains IN_PROGRESS and must "
                + "not be released for sale."
        );
    }

    /*
     * ========================================================
     * FAILURE HANDLING
     * ========================================================
     *
     * Withdraw construction permission.
     *
     * Stop issuing further requests.
     *
     * Record persistent FAILED status when possible.
     *
     * Retain the failed session and suspend further
     * installation until manual review.
     * ========================================================
     */

    private static void fail(
            ExecutionSession session,
            String reason
    ) {

        session.status = ExecutionStatus.FAILED;

        /*
         * Prevent subsequent residential callbacks.
         */

        if (session.permitIssued) {

            EarthGuemesGenerator.revokeConstructionPermit();

            session.permitIssued = false;
        }

        cancelMonitor(session);

        /*
         * Do not assume cancellation of a CompletableFuture
         * cancels Paper's underlying chunk generation.
         *
         * The pending request may still finish.
         */

        if (session.pendingChunk != null

                && !session.pendingChunk.isDone()) {

            session.plugin.getLogger().warning(
                    "A pending chunk request may still finish."
            );
        }

        String failureReason =
                reason == null || reason.isBlank()

                        ? "Unknown residential execution failure."

                        : reason;

        if (failureReason.length() > 512) {

            failureReason =
                    failureReason.substring(0, 512);
        }

        boolean failureRecorded = false;

        try {

            if (EarthResidentialInstallation.isReady()) {

                InstallationStatus current =
                        EarthResidentialInstallation
                                .getStatus(
                                        session.propertyId
                                );

                if (current == InstallationStatus.IN_PROGRESS) {

                    failureRecorded =
                            EarthResidentialInstallation
                                    .failInstallation(
                                            session.propertyId,
                                            failureReason
                                    );

                } else if (
                        current == InstallationStatus.FAILED
                ) {

                    failureRecorded = true;
                }
            }

        } catch (RuntimeException exception) {

            session.plugin.getLogger().log(
                    Level.SEVERE,
                    "Unable to persist residential failure.",
                    exception
            );
        }

        session.plugin.getLogger().severe(
                "Residential execution suspended: "

                + session.propertyId

                + " - "

                + failureReason
        );

        session.plugin.getLogger().severe(
                "Persistent failure recorded: "

                + failureRecorded
        );

        if (!failureRecorded) {

            session.plugin.getLogger().severe(
                    "Installation ledger requires manual review."
            );
        }

        session.plugin.getLogger().severe(
                "Further residential execution is suspended."
        );
    }

    /*
     * ========================================================
     * CANCEL EXECUTION MONITOR
     * ========================================================
     */

    private static void cancelMonitor(
            ExecutionSession session
    ) {

        if (session.monitorTask != null) {

            session.monitorTask.cancel();

            session.monitorTask = null;
        }
    }

    /*
     * ========================================================
     * EXECUTION STATUS ACCESS
     * ========================================================
     */

    public static boolean hasActiveExecution() {

        return Bukkit.isPrimaryThread()

                && activeSession != null;
    }

    public static ExecutionStatus getExecutionStatus() {

        if (!Bukkit.isPrimaryThread()

                || activeSession == null) {

            return null;
        }

        return activeSession.status;
    }

    public static String getActivePropertyId() {

        if (!Bukkit.isPrimaryThread()

                || activeSession == null) {

            return null;
        }

        return activeSession.propertyId;
    }

    public static int getCompletedChunkRequests() {

        if (!Bukkit.isPrimaryThread()

                || activeSession == null) {

            return 0;
        }

        return activeSession.nextChunkIndex;
    }

    public static int getRequiredChunkRequests() {

        if (!Bukkit.isPrimaryThread()

                || activeSession == null) {

            return 0;
        }

        return activeSession.chunks.size();
    }

    /*
     * ========================================================
     * EXECUTION SHUTDOWN
     * ========================================================
     *
     * Future EarthBound shutdown integration must call this
     * before shutting down the installation ledger.
     * ========================================================
     */

    public static void shutdown() {

        if (!Bukkit.isPrimaryThread()) {

            return;
        }

        ExecutionSession session = activeSession;

        if (session == null) {

            return;
        }

        if (session.status != ExecutionStatus.FAILED) {

            fail(
                    session,
                    "Server shutdown before residential "
                    + "installation verification."
            );

        } else {

            cancelMonitor(session);
        }

        EarthGuemesGenerator.revokeConstructionPermit();

        activeSession = null;
    }

    /*
     * ========================================================
     * EXECUTION INTEGRATION LOCK STATUS
     * ========================================================
     */

    public static boolean isExecutionEnabled() {

        return EXECUTION_APPROVED

                && EarthResidentialInstaller
                        .isPhysicalConstructionEnabled();
    }

    /*
     * ========================================================
     * RESULT HELPER
     * ========================================================
     */

    private static ExecutionResult result(
            ExecutionStatus status,
            String message
    ) {

        return new ExecutionResult(
                status,
                message
        );
    }
}
