package com.earthbound;

import java.io.File;
import java.io.IOException;

import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import java.time.Instant;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.earthbound.EarthAlphaResidentialGenerator
        .ResidentialSite;

/*
 * ============================================================
 * EARTHBOUND RESIDENTIAL INSTALLATION TRACKING
 * Friends Alpha
 * ============================================================
 *
 * Persistent installation records.
 *
 * Responsibilities:
 *
 * - Record approved property locations.
 * - Record the associated Minecraft world UUID.
 * - Record house designs and orientations.
 * - Track construction state.
 * - Preserve installation records across restarts.
 * - Reject duplicate installation requests.
 * - Detect interrupted construction.
 * - Require manual review after a failure.
 *
 * This class does not construct buildings,
 * modify Minecraft blocks or enable property sales.
 * ============================================================
 */

public final class EarthResidentialInstallation {

    private static final String FILE_NAME =
            "residential-installations.yml";

    private static final String WORLD_NAME =
            "earthbound";

    private static final int SCHEMA_VERSION = 1;

    private static final int REQUIRED_HOMES = 6;

    private static final int DESIGN_REVISION = 1;

    private static EarthBound plugin;

    private static Path installationFile;

    private static YamlConfiguration records;

    private static boolean ready = false;

    private EarthResidentialInstallation() {
    }

    /*
     * ========================================================
     * INSTALLATION STATES
     * ========================================================
     */

    public enum InstallationStatus {

        NOT_STARTED,

        APPROVED,

        IN_PROGRESS,

        COMPLETED,

        FAILED
    }

    /*
     * ========================================================
     * INITIALIZE INSTALLATION STORAGE
     * ========================================================
     */

    public static synchronized boolean setup(
            EarthBound earthBound
    ) {

        ready = false;

        records = null;

        installationFile = null;

        plugin = earthBound;

        if (plugin == null
                || !Bukkit.isPrimaryThread()) {

            return false;
        }

        try {

            File dataFolder =
                    plugin.getDataFolder();

            Files.createDirectories(
                    dataFolder.toPath()
            );

            installationFile =
                    dataFolder.toPath()
                            .resolve(FILE_NAME);

            /*
             * Load existing records without silently
             * replacing an unreadable installation file.
             */

            if (Files.exists(installationFile)) {

                YamlConfiguration loaded =
                        new YamlConfiguration();

                loaded.load(
                        installationFile.toFile()
                );

                if (!validateRecords(loaded)) {

                    plugin.getLogger().severe(
                            "Residential installation records "
                            + "failed validation."
                    );

                    return false;
                }

                records = loaded;

            } else {

                /*
                 * Create a new empty installation ledger.
                 */

                YamlConfiguration initial =
                        new YamlConfiguration();

                initial.set(
                        "schema-version",
                        SCHEMA_VERSION
                );

                initial.createSection(
                        "installations"
                );

                if (!persist(initial)) {

                    return false;
                }

                records = initial;
            }

            /*
             * Interrupted installations must never
             * automatically resume construction.
             */

            YamlConfiguration recovered =
                    copyRecords();

            ConfigurationSection installations =
                    recovered.getConfigurationSection(
                            "installations"
                    );

            boolean changed = false;

            for (String propertyId
                    : installations.getKeys(false)) {

                String path =
                        propertyPath(propertyId);

                String status =
                        recovered.getString(
                                path + ".status"
                        );

                if (InstallationStatus.IN_PROGRESS
                        .name().equals(status)) {

                    recovered.set(
                            path + ".status",
                            InstallationStatus.FAILED.name()
                    );

                    recovered.set(
                            path + ".failure-reason",
                            "Interrupted installation. "
                            + "Manual review required."
                    );

                    recovered.set(
                            path + ".failed-at",
                            Instant.now().toString()
                    );

                    changed = true;

                    plugin.getLogger().warning(
                            "Interrupted residential installation: "
                            + propertyId
                    );
                }
            }

            if (changed) {

                if (!persist(recovered)) {

                    return false;
                }

                records = recovered;
            }

            ready = true;

            plugin.getLogger().info(
                    "Residential installation tracking ready."
            );

            plugin.getLogger().info(
                    "Existing installation records: "
                    + installations.getKeys(false).size()
            );

            return true;

        } catch (Exception exception) {

            ready = false;

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Unable to initialize residential "
                    + "installation tracking.",
                    exception
            );

            return false;
        }
    }

    /*
     * ========================================================
     * STORAGE READINESS
     * ========================================================
     */

    public static synchronized boolean isReady() {

        return ready
                && records != null
                && installationFile != null;
    }

    /*
     * ========================================================
     * REGISTER APPROVED INSTALLATION PLAN
     * ========================================================
     *
     * Explicit approval must be supplied by a future
     * controlled server administration system.
     *
     * Approval records do not activate construction.
     * ========================================================
     */

    public static synchronized boolean approveDevelopment(
            World world,
            List<ResidentialSite> sites,
            boolean geographicDataReady,
            boolean explicitlyApproved
    ) {

        if (!isReady()
                || !Bukkit.isPrimaryThread()
                || !explicitlyApproved) {

            return false;
        }

        if (world == null
                || !WORLD_NAME.equals(world.getName())
                || sites == null
                || sites.size() != REQUIRED_HOMES) {

            return false;
        }

        try {

            /*
             * Repeat the complete safety inspection
             * before recording approval.
             */

            EarthResidentialSafety.SafetyResult safety =
                    EarthResidentialSafety.inspectDevelopment(
                            world,
                            sites,
                            geographicDataReady
                    );

            if (!safety.isSafe()) {

                plugin.getLogger().warning(
                        "Installation approval rejected: "
                        + safety.getMessage()
                );

                return false;
            }

            YamlConfiguration updated =
                    copyRecords();

            Set<String> propertyIds =
                    new HashSet<>();

            /*
             * Validate the complete plan before
             * adding any persistent records.
             */

            for (ResidentialSite site : sites) {

                if (site == null
                        || !isValidPropertyId(
                                site.getPropertyId()
                        )) {

                    return false;
                }

                String propertyId =
                        site.getPropertyId();

                if (!propertyIds.add(propertyId)) {

                    return false;
                }

                /*
                 * Never replace an existing installation
                 * record, regardless of its state.
                 */

                if (updated.contains(
                        propertyPath(propertyId)
                )) {

                    plugin.getLogger().warning(
                            "Installation approval rejected: "
                            + propertyId
                            + " already has an installation record."
                    );

                    return false;
                }
            }

            String approvedAt =
                    Instant.now().toString();

            for (ResidentialSite site : sites) {

                String path =
                        propertyPath(
                                site.getPropertyId()
                        );

                updated.set(
                        path + ".status",
                        InstallationStatus.APPROVED.name()
                );

                updated.set(
                        path + ".world-name",
                        world.getName()
                );

                updated.set(
                        path + ".world-uuid",
                        world.getUID().toString()
                );

                updated.set(
                        path + ".design",
                        site.getDesign().name()
                );

                updated.set(
                        path + ".design-revision",
                        DESIGN_REVISION
                );

                updated.set(
                        path + ".front-x",
                        site.getWorldX(0, 0)
                );

                updated.set(
                        path + ".front-z",
                        site.getWorldZ(0, 0)
                );

                updated.set(
                        path + ".ground-y",
                        site.getGroundY()
                );

                updated.set(
                        path + ".min-x",
                        site.getMinX()
                );

                updated.set(
                        path + ".max-x",
                        site.getMaxX()
                );

                updated.set(
                        path + ".min-z",
                        site.getMinZ()
                );

                updated.set(
                        path + ".max-z",
                        site.getMaxZ()
                );

                updated.set(
                        path + ".facing",
                        site.getFacing().name()
                );

                updated.set(
                        path + ".approved-at",
                        approvedAt
                );
            }

            /*
             * Persist all six approvals together.
             */

            if (!commit(updated)) {

                return false;
            }

            plugin.getLogger().info(
                    "Six residential installation "
                    + "approvals recorded."
            );

            plugin.getLogger().info(
                    "Residential construction remains "
                    + "controlled by the generator safety lock."
            );

            return true;

        } catch (Exception exception) {

            plugin.getLogger().log(
                    Level.WARNING,
                    "Residential installation approval failed.",
                    exception
            );

            return false;
        }
    }

    /*
     * ========================================================
     * READ INSTALLATION STATUS
     * ========================================================
     */

    public static synchronized InstallationStatus getStatus(
            String propertyId
    ) {

        /*
         * Unavailable storage must not be interpreted
         * as permission to begin a new installation.
         */

        if (!isReady()
                || !isValidPropertyId(propertyId)) {

            throw new IllegalStateException(
                    "Residential installation status unavailable."
            );
        }

        String path =
                propertyPath(propertyId);

        if (!records.contains(path)) {

            return InstallationStatus.NOT_STARTED;
        }

        return InstallationStatus.valueOf(
                records.getString(
                        path + ".status",
                        ""
                )
        );
    }

    /*
     * ========================================================
     * VERIFY INSTALLATION IDENTITY
     * ========================================================
     */

    public static synchronized boolean matchesApprovedSite(
            World world,
            ResidentialSite site
    ) {

        if (!isReady()
                || !Bukkit.isPrimaryThread()
                || world == null
                || site == null
                || !isValidPropertyId(
                        site.getPropertyId()
                )) {

            return false;
        }

        String path =
                propertyPath(
                        site.getPropertyId()
                );

        if (!records.isConfigurationSection(path)) {

            return false;
        }

        return world.getName().equals(
                        records.getString(
                                path + ".world-name"
                        )
                )

                && world.getUID().toString().equals(
                        records.getString(
                                path + ".world-uuid"
                        )
                )

                && site.getDesign().name().equals(
                        records.getString(
                                path + ".design"
                        )
                )

                && records.getInt(
                        path + ".design-revision"
                ) == DESIGN_REVISION

                && site.getFacing().name().equals(
                        records.getString(
                                path + ".facing"
                        )
                )

                && matchesInteger(
                        path + ".front-x",
                        site.getWorldX(0, 0)
                )

                && matchesInteger(
                        path + ".front-z",
                        site.getWorldZ(0, 0)
                )

                && matchesInteger(
                        path + ".ground-y",
                        site.getGroundY()
                )

                && matchesInteger(
                        path + ".min-x",
                        site.getMinX()
                )

                && matchesInteger(
                        path + ".max-x",
                        site.getMaxX()
                )

                && matchesInteger(
                        path + ".min-z",
                        site.getMinZ()
                )

                && matchesInteger(
                        path + ".max-z",
                        site.getMaxZ()
                );
    }

    /*
     * ========================================================
     * BEGIN INSTALLATION
     * ========================================================
     *
     * Record IN_PROGRESS before the future installer
     * is permitted to modify the world.
     *
     * A successful return does not generate blocks.
     * ========================================================
     */

    public static synchronized boolean beginInstallation(
            World world,
            ResidentialSite site,
            boolean geographicDataReady,
            boolean explicitlyApproved
    ) {

        if (!isReady()
                || !Bukkit.isPrimaryThread()
                || !explicitlyApproved
                || !matchesApprovedSite(world, site)) {

            return false;
        }

        String propertyId =
                site.getPropertyId();

        if (getStatus(propertyId)
                != InstallationStatus.APPROVED) {

            return false;
        }

        /*
         * Recheck the site immediately before
         * recording the construction start.
         */

        EarthResidentialSafety.SafetyResult safety =
                EarthResidentialSafety.inspectSite(
                        world,
                        site,
                        geographicDataReady
                );

        if (!safety.isSafe()) {

            plugin.getLogger().warning(
                    "Installation start rejected: "
                    + safety.getMessage()
            );

            return false;
        }

        try {

            YamlConfiguration updated =
                    copyRecords();

            String path =
                    propertyPath(propertyId);

            updated.set(
                    path + ".status",
                    InstallationStatus.IN_PROGRESS.name()
            );

            updated.set(
                    path + ".started-at",
                    Instant.now().toString()
            );

            return commit(updated);

        } catch (Exception exception) {

            return false;
        }
    }

    /*
     * ========================================================
     * RECORD VERIFIED COMPLETION
     * ========================================================
     *
     * Completion must be supplied by a future
     * completed-structure verification system.
     *
     * Never mark an installation completed merely
     * because its construction was requested.
     * ========================================================
     */

    public static synchronized boolean completeInstallation(
            World world,
            ResidentialSite site,
            boolean structureVerified
    ) {

        if (!isReady()
                || !Bukkit.isPrimaryThread()
                || !structureVerified
                || !matchesApprovedSite(world, site)) {

            return false;
        }

        String propertyId =
                site.getPropertyId();

        if (getStatus(propertyId)
                != InstallationStatus.IN_PROGRESS) {

            return false;
        }

        try {

            YamlConfiguration updated =
                    copyRecords();

            String path =
                    propertyPath(propertyId);

            updated.set(
                    path + ".status",
                    InstallationStatus.COMPLETED.name()
            );

            updated.set(
                    path + ".completed-at",
                    Instant.now().toString()
            );

            return commit(updated);

        } catch (Exception exception) {

            return false;
        }
    }

    /*
     * ========================================================
     * RECORD INSTALLATION FAILURE
     * ========================================================
     */

    public static synchronized boolean failInstallation(
            String propertyId,
            String reason
    ) {

        if (!isReady()
                || !Bukkit.isPrimaryThread()
                || !isValidPropertyId(propertyId)
                || reason == null
                || reason.isBlank()
                || reason.length() > 512) {

            return false;
        }

        InstallationStatus status =
                getStatus(propertyId);

        if (status != InstallationStatus.APPROVED
                && status != InstallationStatus.IN_PROGRESS) {

            return false;
        }

        try {

            YamlConfiguration updated =
                    copyRecords();

            String path =
                    propertyPath(propertyId);

            updated.set(
                    path + ".status",
                    InstallationStatus.FAILED.name()
            );

            updated.set(
                    path + ".failed-at",
                    Instant.now().toString()
            );

            updated.set(
                    path + ".failure-reason",
                    reason
            );

            return commit(updated);

        } catch (Exception exception) {

            return false;
        }
    }

    /*
     * ========================================================
     * INSTALLATION COUNTS
     * ========================================================
     */

    public static synchronized int countByStatus(
            InstallationStatus status
    ) {

        if (!isReady() || status == null) {

            throw new IllegalStateException(
                    "Residential installation records unavailable."
            );
        }

        ConfigurationSection installations =
                records.getConfigurationSection(
                        "installations"
                );

        int count = 0;

        for (String propertyId
                : installations.getKeys(false)) {

            if (getStatus(propertyId) == status) {

                count++;
            }
        }

        return count;
    }

    /*
     * ========================================================
     * VALIDATE PERSISTENT INSTALLATION RECORDS
     * ========================================================
     */

    private static boolean validateRecords(
            YamlConfiguration data
    ) {

        if (data == null
                || !data.isInt("schema-version")
                || data.getInt("schema-version")
                        != SCHEMA_VERSION) {

            return false;
        }

        ConfigurationSection installations =
                data.getConfigurationSection(
                        "installations"
                );

        if (installations == null
                || installations.getKeys(false).size()
                        > REQUIRED_HOMES) {

            return false;
        }

        for (String propertyId
                : installations.getKeys(false)) {

            if (!isValidPropertyId(propertyId)) {

                return false;
            }

            ConfigurationSection entry =
                    installations.getConfigurationSection(
                            propertyId
                    );

            if (entry == null) {

                return false;
            }

            try {

                InstallationStatus status =
                        InstallationStatus.valueOf(
                                entry.getString(
                                        "status",
                                        ""
                                )
                        );

                /*
                 * NOT_STARTED is represented by the
                 * absence of an installation record.
                 */

                if (status
                        == InstallationStatus.NOT_STARTED) {

                    return false;
                }

                if (!WORLD_NAME.equals(
                        entry.getString("world-name")
                )) {

                    return false;
                }

                UUID.fromString(
                        entry.getString(
                                "world-uuid",
                                ""
                        )
                );

                if (!entry.isInt("design-revision")
                        || entry.getInt("design-revision")
                                != DESIGN_REVISION) {

                    return false;
                }

                var design =
                        EarthAlphaHouseDesigns
                                .getDesign(propertyId);

                if (design == null
                        || !design.name().equals(
                                entry.getString("design")
                        )) {

                    return false;
                }

                String facing =
                        entry.getString("facing", "");

                if (!"NORTH".equals(facing)
                        && !"SOUTH".equals(facing)) {

                    return false;
                }

                String[] requiredCoordinates = {

                        "front-x",
                        "front-z",
                        "ground-y",
                        "min-x",
                        "max-x",
                        "min-z",
                        "max-z"
                };

                for (String coordinate
                        : requiredCoordinates) {

                    if (!entry.isInt(coordinate)) {

                        return false;
                    }
                }

                int frontX =
                        entry.getInt("front-x");

                int frontZ =
                        entry.getInt("front-z");

                int groundY =
                        entry.getInt("ground-y");

                if (Math.abs((long) frontX) > 29999900
                        || Math.abs((long) frontZ) > 29999900
                        || groundY < 66
                        || groundY > 280) {

                    return false;
                }

                ResidentialSite reconstructed =
                        new ResidentialSite(
                                propertyId,
                                frontX,
                                frontZ,
                                groundY,
                                org.bukkit.block.BlockFace
                                        .valueOf(facing)
                        );

                if (entry.getInt("min-x")
                            != reconstructed.getMinX()

                        || entry.getInt("max-x")
                            != reconstructed.getMaxX()

                        || entry.getInt("min-z")
                            != reconstructed.getMinZ()

                        || entry.getInt("max-z")
                            != reconstructed.getMaxZ()) {

                    return false;
                }

                Instant approvedAt =
                        Instant.parse(
                                entry.getString(
                                        "approved-at",
                                        ""
                                )
                        );

                if (status == InstallationStatus.IN_PROGRESS
                        || status == InstallationStatus.COMPLETED) {

                    Instant startedAt =
                            Instant.parse(
                                    entry.getString(
                                            "started-at",
                                            ""
                                    )
                            );

                    if (startedAt.isBefore(approvedAt)) {

                        return false;
                    }

                    if (status
                            == InstallationStatus.COMPLETED) {

                        Instant completedAt =
                                Instant.parse(
                                        entry.getString(
                                                "completed-at",
                                                ""
                                        )
                                );

                        if (completedAt.isBefore(startedAt)) {

                            return false;
                        }
                    }
                }

                if (status == InstallationStatus.FAILED) {

                    Instant failedAt =
                            Instant.parse(
                                    entry.getString(
                                            "failed-at",
                                            ""
                                    )
                            );

                    if (failedAt.isBefore(approvedAt)) {

                        return false;
                    }

                    String reason =
                            entry.getString(
                                    "failure-reason",
                                    ""
                            );

                    if (reason.isBlank()
                            || reason.length() > 512) {

                        return false;
                    }
                }

            } catch (RuntimeException exception) {

                return false;
            }
        }

        return true;
    }

    /*
     * ========================================================
     * ATOMIC INSTALLATION RECORD COMMIT
     * ========================================================
     */

    private static boolean commit(
            YamlConfiguration updated
    ) {

        if (!validateRecords(updated)) {

            return false;
        }

        if (!persist(updated)) {

            /*
             * Stop further installation operations
             * after a persistent storage failure.
             */

            ready = false;

            return false;
        }

        records = updated;

        return true;
    }

    /*
     * ========================================================
     * PERSIST INSTALLATION RECORDS
     * ========================================================
     *
     * Save to a temporary file first.
     *
     * Flush file contents before attempting an
     * atomic replacement of the installation ledger.
     *
     * If atomic replacement is unavailable,
     * fail closed rather than overwrite directly.
     * ========================================================
     */

    private static boolean persist(
            YamlConfiguration data
    ) {

        Path temporaryFile = null;

        try {

            temporaryFile =
                    Files.createTempFile(
                            installationFile.getParent(),
                            "residential-installations-",
                            ".tmp"
                    );

            data.save(
                    temporaryFile.toFile()
            );

            try (FileChannel channel =
                         FileChannel.open(
                                 temporaryFile,
                                 StandardOpenOption.WRITE
                         )) {

                channel.force(true);
            }

            Files.move(
                    temporaryFile,
                    installationFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return true;

        } catch (IOException
                 | RuntimeException exception) {

            ready = false;

            if (plugin != null) {

                plugin.getLogger().log(
                        Level.SEVERE,
                        "Unable to persist residential "
                        + "installation records.",
                        exception
                );
            }

            return false;

        } finally {

            if (temporaryFile != null) {

                try {

                    Files.deleteIfExists(
                            temporaryFile
                    );

                } catch (IOException exception) {

                    if (plugin != null) {

                        plugin.getLogger().warning(
                                "Unable to remove temporary "
                                + "installation record file."
                        );
                    }
                }
            }
        }
    }

    /*
     * ========================================================
     * RECORD HELPERS
     * ========================================================
     */

    private static YamlConfiguration copyRecords()
            throws Exception {

        YamlConfiguration copy =
                new YamlConfiguration();

        copy.loadFromString(
                records.saveToString()
        );

        return copy;
    }

    private static boolean matchesInteger(
            String path,
            int expectedValue
    ) {

        return records.isInt(path)
                && records.getInt(path)
                        == expectedValue;
    }

    private static boolean isValidPropertyId(
            String propertyId
    ) {

        return propertyId != null
                && propertyId.matches(
                        "guemes_home_0[1-6]"
                );
    }

    private static String propertyPath(
            String propertyId
    ) {

        return "installations." + propertyId;
    }

    /*
     * ========================================================
     * SHUTDOWN
     * ========================================================
     *
     * Every successful state change is saved immediately.
     *
     * No deferred installation save is required.
     * ========================================================
     */

    public static synchronized void shutdown() {

        ready = false;

        records = null;

        installationFile = null;

        plugin = null;
    }
}
