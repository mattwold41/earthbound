package com.earthbound;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.scheduler.BukkitTask;

import com.earthbound.EarthResidentialInstaller.ConstructionChunk;
import com.earthbound.EarthResidentialInstaller.DevelopmentPlan;
import com.earthbound.EarthResidentialInstaller.PropertyPlan;
import com.earthbound.EarthResidentialInstaller.PreflightResult;
import com.earthbound.EarthResidentialInstallation.InstallationStatus;
import com.earthbound.EarthResidentialAuthorization.AuthorizationResult;
import com.earthbound.EarthResidentialVerification.VerificationResult;

public final class EarthResidentialExecution {

    private static final boolean EXECUTION_APPROVED = false;

    private static final String WORLD_NAME = "earthbound";

    private static final String START_CONFIRMATION =
            "START-GUEMES-INSTALLATION";

    private static final long CHUNK_TIMEOUT_NANOS =
            120_000_000_000L;

    private static ExecutionSession activeSession;

    private EarthResidentialExecution() {
    }

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
        COMPLETED,
        FAILED
    }

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
     * INDIVIDUAL EXECUTION SESSION
     */

    private static final class ExecutionSession {

        private final EarthBound plugin;

        private final World world;

        private final UUID worldId;

        private final String propertyId;

        private final PropertyPlan property;

        private final DevelopmentPlan development;

        private final List<ConstructionChunk> chunks;

        private final boolean geographicDataReady;

        private final List<ConstructionChunk> retainedChunks =
                new ArrayList<>();

        private ExecutionStatus status =
                ExecutionStatus.GENERATING;

        private BukkitTask monitorTask;

        private CompletableFuture<Chunk> pendingChunk;

        private long requestStartedAt;

        private int nextChunkIndex;

        private boolean authorizationPending;

        private boolean installationStarted;

        private boolean permitIssued;

        private ExecutionSession(
                EarthBound plugin,
                World world,
                DevelopmentPlan development,
                PropertyPlan property,
                boolean geographicDataReady
        ) {

            this.plugin = plugin;
            this.world = world;
            this.worldId = world.getUID();
            this.propertyId = property.getPropertyId();
            this.property = property;
            this.development = development;
            this.chunks = List.copyOf(property.getChunks());
            this.geographicDataReady = geographicDataReady;
        }
    }

    /*
     * START CONTROLLED RESIDENTIAL EXECUTION
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

        if (activeSession != null) {

            return result(
                    ExecutionStatus.EXECUTION_ALREADY_ACTIVE,
                    "An installation already requires attention."
            );
        }

        ExecutionSession session = null;

        try {

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
                    preflight.getProperty(),
                    geographicDataReady
            );

            if (session.chunks.isEmpty()) {

                return result(
                        ExecutionStatus.PREFLIGHT_REJECTED,
                        "No construction chunks were approved."
                );
            }

            /*
             * Reserve execution before recording authorization.
             */

            activeSession = session;

            /*
             * Open the restricted authorization window.
             *
             * Permission is withdrawn immediately after
             * the synchronous authorization call.
             */

            AuthorizationResult authorization;

            session.authorizationPending = true;

            try {

                authorization =
                        EarthResidentialAuthorization
                                .authorizeInstallationStart(
                                        plugin,
                                        sender,
                                        world,
                                        propertyId,
                                        geographicDataReady,
                                        confirmation
                                );

            } finally {

                session.authorizationPending = false;
            }

            if (!authorization.isAuthorized()) {

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
             * Verify persistent installation start.
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
             * Issue permission only for the approved property.
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
     * MAIN THREAD EXECUTION MONITOR
     *
     * Never wait synchronously for an incomplete future.
     * Only one construction chunk request may be pending.
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

                fail(session, invalidReason);

                return;
            }

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

                if (!retainConstructionChunk(
                        session,
                        expected
                )) {

                    fail(
                            session,
                            "Construction chunk retention failed: "
                            + expected
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

            if (session.nextChunkIndex
                    >= session.chunks.size()) {

                finishGeneration(session);

                return;
            }

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
     * RETAIN COMPLETED CONSTRUCTION CHUNKS
     */

    private static boolean retainConstructionChunk(
            ExecutionSession session,
            ConstructionChunk chunk
    ) {

        if (!Bukkit.isPrimaryThread()
                || activeSession != session
                || !session.plugin.isEnabled()
                || Bukkit.getWorld(session.worldId)
                        != session.world) {

            return false;
        }

        int chunkX = chunk.getChunkX();

        int chunkZ = chunk.getChunkZ();

        if (!session.world.isChunkLoaded(chunkX, chunkZ)
                || !session.world.isChunkGenerated(
                        chunkX,
                        chunkZ
                )) {

            return false;
        }

        for (ConstructionChunk retained
                : session.retainedChunks) {

            if (retained.getKey() == chunk.getKey()) {

                return false;
            }
        }

        /*
         * Never take ownership of another system's ticket.
         */

        if (session.world.getPluginChunkTickets(
                chunkX,
                chunkZ
        ).contains(session.plugin)) {

            return false;
        }

        boolean added =
                session.world.addPluginChunkTicket(
                        chunkX,
                        chunkZ,
                        session.plugin
                );

        if (!added) {

            return false;
        }

        session.retainedChunks.add(chunk);

        if (!session.world.isChunkLoaded(chunkX, chunkZ)
                || !session.world.getPluginChunkTickets(
                        chunkX,
                        chunkZ
                ).contains(session.plugin)) {

            return false;
        }

        session.plugin.getLogger().info(
                "Construction chunk retained: " + chunk
        );

        return true;
    }

    /*
     * CHECK RETAINED CONSTRUCTION CHUNKS
     */

    private static boolean verifyRetainedChunks(
            ExecutionSession session
    ) {

        if (Bukkit.getWorld(session.worldId)
                != session.world) {

            return false;
        }

        for (ConstructionChunk retained
                : session.retainedChunks) {

            int chunkX = retained.getChunkX();

            int chunkZ = retained.getChunkZ();

            if (!session.world.isChunkLoaded(
                    chunkX,
                    chunkZ
            )) {

                return false;
            }

            if (!session.world.getPluginChunkTickets(
                    chunkX,
                    chunkZ
            ).contains(session.plugin)) {

                return false;
            }
        }

        return true;
    }

    private static boolean allConstructionChunksRetained(
            ExecutionSession session
    ) {

        if (session.retainedChunks.size()
                != session.chunks.size()) {

            return false;
        }

        for (ConstructionChunk required
                : session.chunks) {

            boolean found = false;

            for (ConstructionChunk retained
                    : session.retainedChunks) {

                if (required.getKey()
                        == retained.getKey()) {

                    found = true;

                    break;
                }
            }

            if (!found) {

                return false;
            }
        }

        return verifyRetainedChunks(session);
    }

    /*
     * RELEASE SESSION CHUNK TICKETS
     */

    private static boolean releaseConstructionChunks(
            ExecutionSession session
    ) {

        boolean releasedAll = true;

        for (int index =
                    session.retainedChunks.size() - 1;
                index >= 0;
                index--) {

            ConstructionChunk chunk =
                    session.retainedChunks.get(index);

            try {

                int chunkX = chunk.getChunkX();

                int chunkZ = chunk.getChunkZ();

                if (Bukkit.getWorld(session.worldId)
                        != session.world) {

                    releasedAll = false;

                    continue;
                }

                if (session.world.getPluginChunkTickets(
                        chunkX,
                        chunkZ
                ).contains(session.plugin)) {

                    session.world.removePluginChunkTicket(
                            chunkX,
                            chunkZ,
                            session.plugin
                    );
                }

                if (session.world.getPluginChunkTickets(
                        chunkX,
                        chunkZ
                ).contains(session.plugin)) {

                    releasedAll = false;

                    session.plugin.getLogger().warning(
                            "Construction chunk ticket remains: "
                            + chunk
                    );

                    continue;
                }

                session.retainedChunks.remove(index);

            } catch (RuntimeException exception) {

                releasedAll = false;

                session.plugin.getLogger().log(
                        Level.SEVERE,
                        "Unable to release construction chunk: "
                        + chunk,
                        exception
                );
            }
        }

        return releasedAll
                && session.retainedChunks.isEmpty();
    }

    /*
     * REVALIDATE ACTIVE EXECUTION
     */

    private static String validateActiveExecution(
            ExecutionSession session
    ) {

        String invalidReason =
                validateInstallationConditions(session);

        if (invalidReason != null) {

            return invalidReason;
        }

        if (EarthGuemesGenerator.hasConstructionFailed(
                session.worldId,
                session.propertyId
        )) {

            return "Construction permit missing or failed.";
        }

        if (!verifyRetainedChunks(session)) {

            return "Previously retained construction "
                    + "chunks are unavailable.";
        }

        return null;
    }

    /*
     * COMMON INSTALLATION CONDITIONS
     */

    private static String validateInstallationConditions(
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

        if (activeSession != session) {

            return "Construction session is no longer active.";
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

        var currentSites =
                List.copyOf(
                        EarthGuemesGenerator.getResidentialSites()
                );

        if (!EarthAlphaSiteFinder
                .hasDevelopmentChunkClearance(
                        currentSites
                )) {

            return "Residential chunk separation invalid.";
        }

        for (var currentSite : currentSites) {

            if (!EarthResidentialInstallation
                    .matchesApprovedSite(
                            session.world,
                            currentSite
                    )) {

                return "Registered residential identity changed: "
                        + currentSite.getPropertyId();
            }
        }

        PlanResultAdapter boundaryResult =
                inspectCurrentProperty(session);

        if (!boundaryResult.safe) {

            return boundaryResult.message;
        }

        /*
         * Neighboring unfinished properties must remain
         * unowned and ungenerated.
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
     * ACTIVE PROPERTY BOUNDARY INSPECTION
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

    private static PlanResultAdapter inspectCurrentProperty(
            ExecutionSession session
    ) {

        var site = session.property.getSite();

        for (int x = site.getMinX();
                x <= site.getMaxX();
                x++) {

            for (int z = site.getMinZ();
                    z <= site.getMaxZ();
                    z++) {

                Location location =
                        new Location(
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
     * FINISH CHUNK GENERATION
     *
     * Generation alone does not establish successful
     * installation completion.
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

        if (session.pendingChunk != null
                || session.nextChunkIndex
                        != session.chunks.size()) {

            fail(
                    session,
                    "Construction chunk requests remain unresolved."
            );

            return;
        }

        if (!allConstructionChunksRetained(session)) {

            fail(
                    session,
                    "Required construction chunks "
                    + "are not fully retained."
            );

            return;
        }

        EarthGuemesGenerator.revokeConstructionPermit();

        session.permitIssued = false;

        cancelMonitor(session);

        session.status =
                ExecutionStatus.AWAITING_VERIFICATION;

        session.plugin.getLogger().info(
                "Residential generation requests completed: "
                + session.propertyId
        );

        session.plugin.getLogger().info(
                "Construction permit withdrawn."
        );

        session.plugin.getLogger().info(
                "Beginning physical structure verification."
        );

        verifyAndComplete(session);
    }

    /*
     * VERIFY AND COMPLETE INSTALLATION
     *
     * World saving and installation ledger persistence
     * are not a single atomic transaction.
     */

    private static void verifyAndComplete(
            ExecutionSession session
    ) {

        if (!Bukkit.isPrimaryThread()
                || activeSession != session
                || session.status
                        != ExecutionStatus.AWAITING_VERIFICATION) {

            return;
        }

        try {

            String invalidReason =
                    validateCompletionConditions(session);

            if (invalidReason != null) {

                fail(session, invalidReason);

                return;
            }

            VerificationResult verification =
                    EarthResidentialVerification.verify(
                            session.plugin,
                            session.world,
                            session.propertyId,
                            session.geographicDataReady
                    );

            if (!isMatchingVerification(
                    session,
                    verification
            )) {

                fail(
                        session,
                        "Physical structure verification rejected: "
                        + verification.getMessage()
                );

                return;
            }

            session.plugin.getLogger().info(
                    "Physical residential verification passed: "
                    + session.propertyId
            );

            /*
             * Request world save while chunks remain retained.
             */

            session.plugin.getLogger().info(
                    "Requesting world save before "
                    + "installation completion."
            );

            session.world.save();

            invalidReason =
                    validateCompletionConditions(session);

            if (invalidReason != null) {

                fail(session, invalidReason);

                return;
            }

            /*
             * Repeat physical verification after saving.
             */

            VerificationResult finalVerification =
                    EarthResidentialVerification.verify(
                            session.plugin,
                            session.world,
                            session.propertyId,
                            session.geographicDataReady
                    );

            if (!isMatchingVerification(
                    session,
                    finalVerification
            )) {

                fail(
                        session,
                        "Final residential verification rejected: "
                        + finalVerification.getMessage()
                );

                return;
            }

            invalidReason =
                    validateCompletionConditions(session);

            if (invalidReason != null) {

                fail(session, invalidReason);

                return;
            }

            /*
             * Persist verified installation completion.
             */

            boolean completed =
                    EarthResidentialInstallation
                            .completeInstallation(
                                    session.world,
                                    session.property.getSite(),
                                    finalVerification.isVerified()
                            );

            if (!completed) {

                fail(
                        session,
                        "Verified installation completion "
                        + "could not be persisted."
                );

                return;
            }

            if (!EarthResidentialInstallation.isReady()

                    || EarthResidentialInstallation
                            .getStatus(session.propertyId)
                            != InstallationStatus.COMPLETED

                    || !EarthResidentialInstallation
                            .matchesApprovedSite(
                                    session.world,
                                    session.property.getSite()
                            )) {

                fail(
                        session,
                        "Persistent installation completion "
                        + "could not be confirmed."
                );

                return;
            }

            session.status = ExecutionStatus.COMPLETED;

            boolean ticketsReleased =
                    releaseConstructionChunks(session);

            session.plugin.getLogger().info(
                    "========================================"
            );

            session.plugin.getLogger().info(
                    "EARTHBOUND RESIDENTIAL INSTALLATION"
            );

            session.plugin.getLogger().info(
                    "Property: " + session.propertyId
            );

            session.plugin.getLogger().info(
                    "Installation status: COMPLETED"
            );

            session.plugin.getLogger().info(
                    "World UUID: " + session.worldId
            );

            session.plugin.getLogger().info(
                    "Verified blocks: "
                    + finalVerification.getCheckedBlocks()
            );

            session.plugin.getLogger().info(
                    "Block mismatches: "
                    + finalVerification.getMismatchedBlocks()
            );

            session.plugin.getLogger().info(
                    "Construction tickets released: "
                    + ticketsReleased
            );

            session.plugin.getLogger().info(
                    "World save requested before ledger completion."
            );

            session.plugin.getLogger().info(
                    "Property sale activation remains disabled."
            );

            session.plugin.getLogger().info(
                    "========================================"
            );

            if (!ticketsReleased) {

                session.plugin.getLogger().severe(
                        "Completed installation requires "
                        + "chunk ticket cleanup review."
                );

                return;
            }

            activeSession = null;

        } catch (RuntimeException exception) {

            session.plugin.getLogger().log(
                    Level.SEVERE,
                    "Residential verification or completion failed.",
                    exception
            );

            if (session.status == ExecutionStatus.COMPLETED) {

                releaseConstructionChunks(session);

                session.plugin.getLogger().severe(
                        "Completed installation requires "
                        + "manual cleanup review."
                );

            } else {

                fail(
                        session,
                        "Unexpected verification or completion failure."
                );
            }
        }
    }

    /*
     * VALIDATE COMPLETION CONDITIONS
     */

    private static String validateCompletionConditions(
            ExecutionSession session
    ) {

        if (session.status
                != ExecutionStatus.AWAITING_VERIFICATION) {

            return "Installation is not awaiting verification.";
        }

        if (session.permitIssued
                || EarthGuemesGenerator
                        .isResidentialGenerationEnabled()) {

            return "Construction permission remains active.";
        }

        if (session.pendingChunk != null
                || session.nextChunkIndex
                        != session.chunks.size()) {

            return "Construction requests remain unresolved.";
        }

        if (!allConstructionChunksRetained(session)) {

            return "Required construction chunks are unavailable.";
        }

        return validateInstallationConditions(session);
    }

    /*
     * CONFIRM VERIFICATION RESULT IDENTITY
     */

    private static boolean isMatchingVerification(
            ExecutionSession session,
            VerificationResult verification
    ) {

        return verification != null

                && verification.isVerified()

                && session.worldId.equals(
                        verification.getWorldId()
                )

                && session.propertyId.equals(
                        verification.getPropertyId()
                )

                && verification.getCheckedBlocks()
                        == verification.getExpectedBlocks()

                && verification.getMismatchedBlocks() == 0;
    }

    /*
     * FAILURE HANDLING
     */

    private static void fail(
            ExecutionSession session,
            String reason
    ) {

        session.status = ExecutionStatus.FAILED;

        if (activeSession == session) {

            EarthGuemesGenerator.revokeConstructionPermit();

            session.permitIssued = false;
        }

        cancelMonitor(session);

        /*
         * A pending asynchronous request may still finish.
         */

        if (session.pendingChunk != null
                && !session.pendingChunk.isDone()) {

            session.plugin.getLogger().warning(
                    "A pending chunk request may still finish."
            );
        }

        boolean ticketsReleased =
                releaseConstructionChunks(session);

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

        session.plugin.getLogger().severe(
                "Construction tickets released: "
                + ticketsReleased
        );

        if (!failureRecorded) {

            session.plugin.getLogger().severe(
                    "Installation ledger requires manual review."
            );
        }

        if (!ticketsReleased) {

            session.plugin.getLogger().severe(
                    "Construction chunk tickets require manual review."
            );
        }

        session.plugin.getLogger().severe(
                "Further residential execution is suspended."
        );
    }

    /*
     * CANCEL EXECUTION MONITOR
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
     * RESTRICTED INSTALLATION AUTHORIZATION GATE
     *
     * Only valid during the synchronous authorization
     * call belonging to the reserved startup session.
     *
     * This method does not authorize construction,
     * issue permits or modify installation records.
     */

    static boolean isInstallationAuthorizationPending(
            EarthBound plugin,
            World world,
            String propertyId
    ) {

        if (!Bukkit.isPrimaryThread()

                || !isExecutionEnabled()

                || plugin == null

                || !plugin.isEnabled()

                || world == null

                || propertyId == null) {

            return false;
        }

        ExecutionSession session = activeSession;

        return session != null

                && session.authorizationPending

                && session.plugin == plugin

                && session.world == world

                && session.worldId.equals(world.getUID())

                && WORLD_NAME.equals(world.getName())

                && Bukkit.getWorld(session.worldId) == world

                && session.propertyId.equals(propertyId)

                && session.status == ExecutionStatus.GENERATING

                && !session.installationStarted

                && !session.permitIssued

                && session.monitorTask == null

                && session.pendingChunk == null

                && session.nextChunkIndex == 0

                && !session.chunks.isEmpty()

                && session.retainedChunks.isEmpty();
    }

    /*
     * EXECUTION STATUS ACCESS
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

    public static int getRetainedConstructionChunkCount() {

        if (!Bukkit.isPrimaryThread()
                || activeSession == null) {

            return 0;
        }

        return activeSession.retainedChunks.size();
    }

    /*
     * EXECUTION SHUTDOWN
     *
     * Never mark interrupted installations completed.
     */

    public static void shutdown() {

        if (!Bukkit.isPrimaryThread()) {

            return;
        }

        ExecutionSession session = activeSession;

        if (session == null) {

            EarthGuemesGenerator.revokeConstructionPermit();

            return;
        }

        try {

            if (session.status == ExecutionStatus.COMPLETED) {

                cancelMonitor(session);

            } else if (
                    session.status != ExecutionStatus.FAILED
            ) {

                fail(
                        session,
                        "Server shutdown before residential "
                        + "installation completion."
                );

            } else {

                cancelMonitor(session);
            }

        } finally {

            EarthGuemesGenerator.revokeConstructionPermit();

            session.permitIssued = false;

            if (!releaseConstructionChunks(session)) {

                session.plugin.getLogger().severe(
                        "Residential chunk ticket cleanup "
                        + "was incomplete during shutdown."
                );
            }

            activeSession = null;
        }
    }

    /*
     * EXECUTION INTEGRATION LOCK STATUS
     */

    public static boolean isExecutionEnabled() {

        return EXECUTION_APPROVED

                && EarthResidentialInstaller
                        .isPhysicalConstructionEnabled();
    }

    /*
     * RESULT HELPER
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
