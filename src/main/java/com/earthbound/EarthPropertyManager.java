
package com.earthbound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.earthbound.EarthAlphaResidentialGenerator.ResidentialSite;
import com.earthbound.EarthResidentialInstallation.InstallationStatus;

public class EarthPropertyManager {

    private static JavaPlugin plugin;

    private static boolean ownershipLoaded = false;

    private static final String RESIDENTIAL_WORLD = "earthbound";

    private static final Map<String, EarthProperty> properties =
            new LinkedHashMap<>();

    /*
     * INITIALIZE PROPERTY SYSTEM
     */

    public static void setup(JavaPlugin earthBoundPlugin) {

        plugin = earthBoundPlugin;
        ownershipLoaded = false;

        properties.clear();

        registerAlphaProperties();

        if (!EarthEconomy.isReady()) {

            plugin.getLogger().severe(
                    "Property ownership could not be loaded "
                    + "because the economy is unavailable."
            );

            plugin.getLogger().severe(
                    "Property purchases are disabled. "
                    + "Registered plots remain protected."
            );

            return;
        }

        ownershipLoaded = loadOwners();

        plugin.getLogger().info(
                "EarthBound properties registered: "
                + properties.size()
        );

        if (ownershipLoaded) {

            plugin.getLogger().info(
                    "Secure property ownership loaded!"
            );

        } else {

            plugin.getLogger().severe(
                    "Property ownership initialization failed. "
                    + "Purchases are disabled."
            );
        }

        plugin.getLogger().info(
                "Verified residential installation "
                + "purchase restrictions are active."
        );
    }

    public static boolean isReady() {

        return ownershipLoaded
                && EarthEconomy.isReady();
    }

    /*
     * LOAD ALPHA PROPERTIES FROM CONFIG.YML
     *
     * Registration remains independent of sale authorization
     * so unfinished properties remain protected.
     */

    private static void registerAlphaProperties() {

        ConfigurationSection section =
                plugin.getConfig().getConfigurationSection(
                        "properties.alpha-homes"
                );

        if (section == null) {

            plugin.getLogger().warning(
                    "No Alpha homes configured. "
                    + "Property purchases are disabled."
            );

            return;
        }

        for (String id : section.getKeys(false)) {

            ConfigurationSection home =
                    section.getConfigurationSection(id);

            if (home == null
                    || !home.getBoolean("enabled", false)) {

                continue;
            }

            try {

                if (!validPropertyId(id)) {

                    plugin.getLogger().warning(
                            "Invalid property ID: " + id
                    );

                    continue;
                }

                String name =
                        home.getString("name", id);

                String world =
                        home.getString("world", "earthbound");

                if (!home.isInt("min-x")
                        || !home.isInt("min-z")
                        || !home.contains("price")) {

                    plugin.getLogger().warning(
                            "Missing or invalid coordinates "
                            + "or price for " + id
                    );

                    continue;
                }

                int minX = home.getInt("min-x");
                int minZ = home.getInt("min-z");

                int width = home.getInt("width", 24);
                int length = home.getInt("length", 32);

                if (width <= 0
                        || length <= 0
                        || width > 256
                        || length > 256) {

                    plugin.getLogger().warning(
                            "Invalid property dimensions: " + id
                    );

                    continue;
                }

                double price = home.getDouble("price");

                if (!validPrice(price)) {

                    plugin.getLogger().warning(
                            "Invalid property price: " + id
                    );

                    continue;
                }

                int maxX =
                        Math.addExact(minX, width - 1);

                int maxZ =
                        Math.addExact(minZ, length - 1);

                EarthProperty property =
                        new EarthProperty(
                                id,
                                name,
                                world,
                                minX,
                                maxX,
                                minZ,
                                maxZ,
                                price
                        );

                if (registerProperty(property)) {

                    plugin.getLogger().info(
                            "Registered property "
                            + id
                            + " ("
                            + width
                            + "x"
                            + length
                            + ")"
                    );
                }

            } catch (RuntimeException exception) {

                plugin.getLogger().warning(
                        "Could not register property "
                        + id
                        + ": "
                        + exception.getMessage()
                );
            }
        }
    }

    /*
     * PROPERTY VALIDATION
     */

    private static boolean validPropertyId(String id) {

        return id != null
                && id.matches("[A-Za-z0-9_-]{1,128}");
    }

    private static boolean validPrice(double price) {

        if (!Double.isFinite(price)
                || price < 0.01
                || price > 1000000000.0) {

            return false;
        }

        double rounded =
                Math.round(price * 100.0) / 100.0;

        return Double.compare(price, rounded) == 0;
    }

    /*
     * REGISTER A PROPERTY
     */

    public static boolean registerProperty(
            EarthProperty property
    ) {

        if (property == null
                || !validPropertyId(property.getId())
                || !validPrice(property.getPrice())
                || properties.containsKey(property.getId())) {

            return false;
        }

        for (EarthProperty existing : properties.values()) {

            if (overlaps(existing, property)) {

                plugin.getLogger().warning(
                        "Overlapping property rejected: "
                        + property.getId()
                );

                return false;
            }
        }

        /*
         * Restore ownership for properties registered
         * after the initial startup process.
         */

        if (ownershipLoaded) {

            if (!EarthEconomy.isReady()) {

                return false;
            }

            try {

                property.setOwner(
                        EarthEconomy.getPropertyOwner(
                                property.getId()
                        )
                );

            } catch (RuntimeException exception) {

                plugin.getLogger().severe(
                        "Could not restore ownership for "
                        + property.getId()
                );

                return false;
            }

        } else {

            property.setOwner(null);
        }

        properties.put(property.getId(), property);

        return true;
    }

    private static boolean overlaps(
            EarthProperty first,
            EarthProperty second
    ) {

        if (!first.getWorldName().equals(
                second.getWorldName()
        )) {

            return false;
        }

        return first.getMinX() <= second.getMaxX()
                && first.getMaxX() >= second.getMinX()
                && first.getMinZ() <= second.getMaxZ()
                && first.getMaxZ() >= second.getMinZ();
    }

    /*
     * PROPERTY LOOKUPS
     */

    public static EarthProperty getProperty(String id) {

        if (id == null) {

            return null;
        }

        return properties.get(id);
    }

    public static Collection<EarthProperty> getProperties() {

        return Collections.unmodifiableCollection(
                properties.values()
        );
    }

    public static EarthProperty getPropertyAt(
            Location location
    ) {

        if (location == null
                || location.getWorld() == null) {

            return null;
        }

        for (EarthProperty property : properties.values()) {

            if (property.contains(
                    location.getWorld().getName(),
                    location.getBlockX(),
                    location.getBlockZ()
            )) {

                return property;
            }
        }

        return null;
    }

    public static List<EarthProperty> getOwnedProperties(
            UUID playerId
    ) {

        List<EarthProperty> owned = new ArrayList<>();

        if (playerId == null || !ownershipLoaded) {

            return owned;
        }

        for (EarthProperty property : properties.values()) {

            if (property.isOwnedBy(playerId)) {

                owned.add(property);
            }
        }

        return owned;
    }

    public static boolean ownsProperty(
            Player player,
            EarthProperty property
    ) {

        return ownershipLoaded
                && player != null
                && property != null
                && property.isOwnedBy(
                        player.getUniqueId()
                );
    }

    /*
     * RESIDENTIAL PURCHASE ELIGIBILITY
     *
     * This inspection does not modify ownership,
     * generate chunks, place blocks or commit money.
     *
     * Missing or inconsistent information fails closed.
     */

    public enum PurchaseEligibility {

        ELIGIBLE,

        SALE_ACTIVATION_LOCKED,

        MAIN_THREAD_REQUIRED,

        PLUGIN_UNAVAILABLE,

        PROPERTY_SYSTEM_UNAVAILABLE,

        INSTALLATION_STORAGE_UNAVAILABLE,

        INVALID_PROPERTY,

        RESIDENTIAL_DISABLED,

        PROPERTY_DISABLED,

        INVALID_WORLD,

        INVALID_RESIDENTIAL_SITE,

        INVALID_INSTALLATION_STATUS,

        INSTALLATION_IDENTITY_MISMATCH,

        PROPERTY_BOUNDARY_MISMATCH,

        PROPERTY_PRICE_MISMATCH,

        PROPERTY_ALREADY_OWNED,

        ACTIVE_RESIDENTIAL_EXECUTION,

        ELIGIBILITY_CHECK_FAILED
    }

    /*
     * READ-ONLY PURCHASE ELIGIBILITY INSPECTION
     */

    public static PurchaseEligibility checkPurchaseEligibility(
            String propertyId
    ) {

        if (!Bukkit.isPrimaryThread()) {

            return PurchaseEligibility.MAIN_THREAD_REQUIRED;
        }

        if (plugin == null || !plugin.isEnabled()) {

            return PurchaseEligibility.PLUGIN_UNAVAILABLE;
        }

        /*
         * EXPLICIT PROPERTY SALE ACTIVATION LOCK
         *
         * A completed installation does not independently
         * authorize a residential property sale.
         */

        if (!(plugin instanceof EarthBound earthBound)
                || !EarthPropertySaleMarkers
                        .isSaleActivationEnabled(earthBound)) {

            return PurchaseEligibility.SALE_ACTIVATION_LOCKED;
        }

        if (!isReady()) {

            return PurchaseEligibility
                    .PROPERTY_SYSTEM_UNAVAILABLE;
        }

        /*
         * Friends Alpha has exactly six approved
         * residential property identities.
         */

        if (propertyId == null
                || !propertyId.matches("guemes_home_0[1-6]")) {

            return PurchaseEligibility.INVALID_PROPERTY;
        }

        EarthProperty property =
                getProperty(propertyId);

        if (property == null
                || !propertyId.equals(property.getId())
                || !validPrice(property.getPrice())) {

            return PurchaseEligibility.INVALID_PROPERTY;
        }

        try {

            return inspectResidentialPurchase(
                    propertyId,
                    property
            );

        } catch (RuntimeException exception) {

            plugin.getLogger().warning(
                    "Residential purchase eligibility "
                    + "inspection failed for "
                    + propertyId
                    + ": "
                    + exception.getMessage()
            );

            return PurchaseEligibility
                    .ELIGIBILITY_CHECK_FAILED;
        }
    }

    /*
     * INTERNAL RESIDENTIAL PURCHASE INSPECTION
     */

    private static PurchaseEligibility inspectResidentialPurchase(
            String propertyId,
            EarthProperty property
    ) {

        /*
         * Require persistent installation storage.
         */

        if (!EarthResidentialInstallation.isReady()) {

            return PurchaseEligibility
                    .INSTALLATION_STORAGE_UNAVAILABLE;
        }

        /*
         * Respect residential configuration.
         */

        if (!plugin.getConfig().getBoolean(
                "residential.enabled",
                false
        )) {

            return PurchaseEligibility.RESIDENTIAL_DISABLED;
        }

        if (!RESIDENTIAL_WORLD.equals(
                plugin.getConfig().getString(
                        "residential.world",
                        RESIDENTIAL_WORLD
                )
        )) {

            return PurchaseEligibility.INVALID_WORLD;
        }

        /*
         * Require an enabled property configuration.
         */

        ConfigurationSection home =
                plugin.getConfig().getConfigurationSection(
                        "properties.alpha-homes."
                        + propertyId
                );

        if (home == null
                || !home.getBoolean("enabled", false)) {

            return PurchaseEligibility.PROPERTY_DISABLED;
        }

        /*
         * Validate world identity without loading a world.
         */

        if (!RESIDENTIAL_WORLD.equals(
                property.getWorldName()

        ) || !RESIDENTIAL_WORLD.equals(
                home.getString("world", RESIDENTIAL_WORLD)
        )) {

            return PurchaseEligibility.INVALID_WORLD;
        }

        World world =
                Bukkit.getWorld(RESIDENTIAL_WORLD);

        if (world == null
                || !RESIDENTIAL_WORLD.equals(world.getName())
                || Bukkit.getWorld(world.getUID()) != world) {

            return PurchaseEligibility.INVALID_WORLD;
        }

        /*
         * Suspend purchases during active construction
         * or unresolved execution review.
         */

        if (EarthResidentialExecution.hasActiveExecution()) {

            return PurchaseEligibility
                    .ACTIVE_RESIDENTIAL_EXECUTION;
        }

        /*
         * Only COMPLETED installations qualify.
         */

        InstallationStatus installationStatus =
                EarthResidentialInstallation.getStatus(
                        propertyId
                );

        if (installationStatus
                != InstallationStatus.COMPLETED) {

            return PurchaseEligibility
                    .INVALID_INSTALLATION_STATUS;
        }

        /*
         * Verify the residential development and
         * construction separation safeguards.
         */

        List<ResidentialSite> sites =
                List.copyOf(
                        EarthGuemesGenerator.getResidentialSites()
                );

        if (sites.size() != 6
                || !EarthAlphaSiteFinder
                        .hasDevelopmentChunkClearance(sites)) {

            return PurchaseEligibility
                    .INVALID_RESIDENTIAL_SITE;
        }

        ResidentialSite site = null;

        for (ResidentialSite candidate : sites) {

            if (propertyId.equals(
                    candidate.getPropertyId()
            )) {

                site = candidate;

                break;
            }
        }

        if (site == null || site.getDesign() == null) {

            return PurchaseEligibility
                    .INVALID_RESIDENTIAL_SITE;
        }

        /*
         * Verify persistent installation identity:
         *
         * World UUID, property ID, design revision,
         * orientation, elevation and approved boundaries.
         */

        if (!EarthResidentialInstallation
                .matchesApprovedSite(world, site)) {

            return PurchaseEligibility
                    .INSTALLATION_IDENTITY_MISMATCH;
        }

        /*
         * Verify registered property boundaries.
         */

        if (property.getMinX() != site.getMinX()
                || property.getMaxX() != site.getMaxX()
                || property.getMinZ() != site.getMinZ()
                || property.getMaxZ() != site.getMaxZ()) {

            return PurchaseEligibility
                    .PROPERTY_BOUNDARY_MISMATCH;
        }

        long width =
                (long) property.getMaxX()
                - property.getMinX() + 1;

        long length =
                (long) property.getMaxZ()
                - property.getMinZ() + 1;

        if (width != EarthAlphaResidentialGenerator.PLOT_WIDTH
                || length
                    != EarthAlphaResidentialGenerator.PLOT_LENGTH) {

            return PurchaseEligibility
                    .PROPERTY_BOUNDARY_MISMATCH;
        }

        /*
         * Check current configuration against
         * the approved property boundaries.
         */

        if (!home.isInt("min-x")
                || !home.isInt("min-z")

                || home.getInt("min-x")
                    != property.getMinX()

                || home.getInt("min-z")
                    != property.getMinZ()

                || home.getInt("width", 24)
                    != EarthAlphaResidentialGenerator.PLOT_WIDTH

                || home.getInt("length", 32)
                    != EarthAlphaResidentialGenerator.PLOT_LENGTH) {

            return PurchaseEligibility
                    .PROPERTY_BOUNDARY_MISMATCH;
        }

        /*
         * Verify registered and configured prices.
         */

        if (!home.contains("price")) {

            return PurchaseEligibility.PROPERTY_PRICE_MISMATCH;
        }

        double configuredPrice =
                home.getDouble("price");

        if (!validPrice(configuredPrice)
                || Double.compare(
                        configuredPrice,
                        property.getPrice()
                ) != 0) {

            return PurchaseEligibility.PROPERTY_PRICE_MISMATCH;
        }

        /*
         * Check in-memory and persistent ownership.
         */

        if (property.isOwned()
                || EarthEconomy.getPropertyOwner(
                        propertyId
                ) != null) {

            return PurchaseEligibility.PROPERTY_ALREADY_OWNED;
        }

        /*
         * Verify all 768 property columns.
         *
         * This does not load Minecraft chunks.
         */

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

                EarthProperty registered =
                        getPropertyAt(location);

                if (registered != property

                        || !propertyId.equals(
                                registered.getId()
                        )

                        || registered.isOwned()

                        || registered.getMinX()
                            != site.getMinX()

                        || registered.getMaxX()
                            != site.getMaxX()

                        || registered.getMinZ()
                            != site.getMinZ()

                        || registered.getMaxZ()
                            != site.getMaxZ()) {

                    return PurchaseEligibility
                            .PROPERTY_BOUNDARY_MISMATCH;
                }
            }
        }

        /*
         * FINAL READINESS CONFIRMATION
         */

        if (!EarthResidentialInstallation.isReady()) {

            return PurchaseEligibility
                    .INSTALLATION_STORAGE_UNAVAILABLE;
        }

        /*
         * Repeat the explicit sale activation check.
         */

        if (!(plugin instanceof EarthBound earthBound)
                || !EarthPropertySaleMarkers
                        .isSaleActivationEnabled(earthBound)) {

            return PurchaseEligibility.SALE_ACTIVATION_LOCKED;
        }

        if (!isReady()) {

            return PurchaseEligibility
                    .PROPERTY_SYSTEM_UNAVAILABLE;
        }

        if (EarthResidentialInstallation.getStatus(propertyId)
                != InstallationStatus.COMPLETED) {

            return PurchaseEligibility
                    .INVALID_INSTALLATION_STATUS;
        }

        return PurchaseEligibility.ELIGIBLE;
    }

    /*
     * PURCHASE ELIGIBILITY HELPER
     */

    public static boolean isPropertyPurchaseEligible(
            String propertyId
    ) {

        return checkPurchaseEligibility(propertyId)
                == PurchaseEligibility.ELIGIBLE;
    }

    /*
     * PURCHASE REJECTION MESSAGE
     */

    private static void rejectPurchase(
            Player player,
            String propertyId,
            PurchaseEligibility eligibility
    ) {

        if (eligibility
                == PurchaseEligibility.SALE_ACTIVATION_LOCKED) {

            player.sendMessage(
                    "Residential property sales are currently locked."
            );

        } else if (eligibility
                == PurchaseEligibility.PROPERTY_ALREADY_OWNED) {

            player.sendMessage(
                    "This property already has an owner."
            );

        } else if (eligibility
                == PurchaseEligibility.INVALID_INSTALLATION_STATUS) {

            player.sendMessage(
                    "This home's installation has not "
                    + "been successfully completed."
            );

            player.sendMessage(
                    "The property is not available for purchase."
            );

        } else {

            player.sendMessage(
                    "This property is currently "
                    + "unavailable for purchase."
            );
        }

        if (plugin != null) {

            plugin.getLogger().warning(
                    "Residential purchase rejected: "
                    + propertyId
                    + " | Eligibility: "
                    + eligibility.name()
            );
        }
    }

    /*
     * PURCHASE GOVERNMENT PROPERTY
     */

    public static boolean purchaseProperty(
            Player player,
            String propertyId
    ) {

        if (player == null || propertyId == null) {

            return false;
        }

        /*
         * All purchases must run on the server thread.
         */

        if (!Bukkit.isPrimaryThread()) {

            if (plugin != null) {

                plugin.getLogger().warning(
                        "Asynchronous property purchase rejected."
                );
            }

            return false;
        }

        if (plugin == null || !plugin.isEnabled()) {

            return false;
        }

        if (!player.isOnline()) {

            return false;
        }

        if (!isReady()) {

            player.sendMessage(
                    "Property purchases are temporarily "
                    + "unavailable."
            );

            return false;
        }

        EarthProperty property =
                getProperty(propertyId);

        if (property == null) {

            player.sendMessage(
                    "Property not found."
            );

            return false;
        }

        /*
         * INDEPENDENT RESIDENTIAL PURCHASE SAFETY LOCK
         *
         * Includes explicit sale authorization and
         * complete residential installation eligibility.
         *
         * Never trust a banner or purchase menu alone.
         */

        PurchaseEligibility eligibility =
                checkPurchaseEligibility(propertyId);

        if (eligibility
                != PurchaseEligibility.ELIGIBLE) {

            rejectPurchase(
                    player,
                    propertyId,
                    eligibility
            );

            return false;
        }

        /*
         * CHECK EXISTING OWNERSHIP
         */

        if (property.isOwned()) {

            player.sendMessage(
                    "This property already has an owner."
            );

            return false;
        }

        /*
         * FRIENDS ALPHA:
         * ONE PROPERTY PER PLAYER
         */

        if (!getOwnedProperties(
                player.getUniqueId()
        ).isEmpty()) {

            player.sendMessage(
                    "You already own an Alpha property."
            );

            return false;
        }

        double price = property.getPrice();

        if (!validPrice(price)) {

            player.sendMessage(
                    "This property has an invalid price."
            );

            return false;
        }

        /*
         * CHECK PLAYER BALANCE
         */

        double balance =
                EarthEconomy.getBalance(player);

        if (!EarthEconomy.isReady()) {

            player.sendMessage(
                    "The economy is temporarily unavailable."
            );

            return false;
        }

        if (balance < price) {

            player.sendMessage(
                    "You do not have enough money."
            );

            return false;
        }

        /*
         * FINAL PURCHASE ELIGIBILITY CHECK
         *
         * Repeat sale authorization and installation
         * eligibility before calling the economy.
         */

        PurchaseEligibility finalEligibility =
                checkPurchaseEligibility(propertyId);

        if (finalEligibility
                != PurchaseEligibility.ELIGIBLE) {

            rejectPurchase(
                    player,
                    propertyId,
                    finalEligibility
            );

            return false;
        }

        if (getProperty(propertyId) != property
                || Double.compare(
                        property.getPrice(),
                        price
                ) != 0) {

            player.sendMessage(
                    "Property information changed. "
                    + "Purchase cancelled."
            );

            return false;
        }

        /*
         * ATOMIC PROPERTY TRANSACTION
         *
         * The economy independently checks sale authorization
         * and commits the following together:
         *
         * Player balance deduction.
         * Anacortes treasury deposit.
         * Washington treasury deposit.
         * USA treasury deposit.
         * Property ownership.
         * Transaction history.
         */

        boolean purchased =
                EarthEconomy.purchaseGovernmentProperty(
                        player,
                        price,
                        property.getId()
                );

        if (!purchased) {

            if (!EarthEconomy.isReady()) {

                player.sendMessage(
                        "Financial storage is unavailable. "
                        + "Contact an administrator before retrying."
                );

            } else {

                player.sendMessage(
                        "Property purchase could not "
                        + "be completed."
                );
            }

            return false;
        }

        /*
         * Purchase has already been committed.
         *
         * Synchronize in-memory ownership.
         * No additional ownership save is required.
         */

        property.setOwner(
                player.getUniqueId()
        );

        plugin.getLogger().info(
                "Property purchased: "
                + property.getId()
                + " by "
                + player.getName()
                + " ("
                + player.getUniqueId()
                + ") for $"
                + String.format(
                        Locale.US,
                        "%.2f",
                        price
                )
        );

        player.sendMessage(
                "You purchased "
                + property.getName()
                + " for $"
                + String.format(
                        Locale.US,
                        "%.2f",
                        price
                )
                + "!"
        );

        return true;
    }

    /*
     * RESTORE SAVED PROPERTY OWNERS
     */

    private static boolean loadOwners() {

        Map<String, UUID> restoredOwners =
                new LinkedHashMap<>();

        try {

            /*
             * Read every owner before changing
             * in-memory ownership records.
             */

            for (EarthProperty property : properties.values()) {

                UUID owner =
                        EarthEconomy.getPropertyOwner(
                                property.getId()
                        );

                restoredOwners.put(
                        property.getId(),
                        owner
                );
            }

        } catch (RuntimeException exception) {

            plugin.getLogger().severe(
                    "Could not load property ownership: "
                    + exception.getMessage()
            );

            return false;
        }

        int ownedCount = 0;

        for (EarthProperty property : properties.values()) {

            UUID owner =
                    restoredOwners.get(
                            property.getId()
                    );

            property.setOwner(owner);

            if (owner != null) {

                ownedCount++;
            }
        }

        plugin.getLogger().info(
                "Restored "
                + ownedCount
                + " owned properties."
        );

        return true;
    }

    /*
     * VERIFY SAVED PROPERTY OWNERSHIP
     *
     * Ownership is saved automatically by EarthEconomy.
     *
     * Never overwrite economy-v2.yml using a separate
     * property ownership file.
     */

    public static boolean save() {

        if (!isReady()) {

            return false;
        }

        try {

            for (EarthProperty property : properties.values()) {

                UUID savedOwner =
                        EarthEconomy.getPropertyOwner(
                                property.getId()
                        );

                if (!Objects.equals(
                        savedOwner,
                        property.getOwner()
                )) {

                    plugin.getLogger().severe(
                            "Property ownership mismatch: "
                            + property.getId()
                    );

                    return false;
                }
            }

            return true;

        } catch (RuntimeException exception) {

            plugin.getLogger().severe(
                    "Property ownership verification failed: "
                    + exception.getMessage()
            );

            return false;
        }
    }

}
