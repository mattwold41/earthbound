package com.earthbound;

import java.util.Objects;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;

import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;

import org.bukkit.inventory.EquipmentSlot;


/*
 * ============================================================
 * EARTHBOUND PROPERTY PROTECTION
 * ============================================================
 */

public class EarthPropertyProtection implements Listener {

    /*
     * FIND PROTECTED PROPERTY
     */

    private EarthProperty getProperty(Location location) {

        return EarthPropertyManager.getPropertyAt(location);
    }

    private boolean isProtected(Block block) {

        return block != null
                && getProperty(block.getLocation()) != null;
    }


    /*
     * VERIFY PLAYER OWNERSHIP
     */

    private boolean canModify(
            Player player,
            Location location
    ) {

        EarthProperty property = getProperty(location);

        if (property == null) {
            return true;
        }

        return property.isOwnedBy(
                player.getUniqueId()
        );
    }


    /*
     * PROPERTY ACCESS DENIED MESSAGE
     */

    private void deny(Player player) {

        player.sendMessage(
                ChatColor.RED
                + "You do not have permission "
                + "to modify this property."
        );
    }


    /*
     * PROTECT BLOCK BREAKING
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockBreak(BlockBreakEvent event) {

        if (!canModify(
                event.getPlayer(),
                event.getBlock().getLocation())) {

            event.setCancelled(true);

            deny(event.getPlayer());
        }
    }


    /*
     * PROTECT BLOCK PLACEMENT
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockPlace(BlockPlaceEvent event) {

        if (!canModify(
                event.getPlayer(),
                event.getBlockPlaced().getLocation())) {

            event.setCancelled(true);

            deny(event.getPlayer());

            return;
        }

        if (event instanceof BlockMultiPlaceEvent multi) {

            for (org.bukkit.block.BlockState state
                    : multi.getReplacedBlockStates()) {

                if (!canModify(
                        event.getPlayer(),
                        state.getLocation())) {

                    event.setCancelled(true);

                    deny(event.getPlayer());

                    return;
                }
            }
        }
    }


    /*
     * PROTECT DOORS, CHESTS, FURNACES,
     * BUTTONS AND OTHER BLOCK INTERACTIONS
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = false
    )
    public void onInteract(PlayerInteractEvent event) {

        Block block = event.getClickedBlock();

        if (block == null) {
            return;
        }

        EarthProperty property =
                getProperty(block.getLocation());

        if (property == null) {
            return;
        }

        Player player = event.getPlayer();

        /*
         * ALLOW OFFICIAL HOME PURCHASE BANNERS
         */

        if (event.getAction()
                == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                && !property.isOwned()
                && EarthPropertySaleMarkers.isOfficialBanner(
                        block,
                        property.getId())) {

            return;
        }

        if (property.isOwnedBy(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        if (event.getHand() == EquipmentSlot.HAND) {

            deny(player);
        }
    }


    /*
     * PROTECT ENTITY INTERACTIONS
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityInteract(
            PlayerInteractEntityEvent event
    ) {

        if (!canModify(
                event.getPlayer(),
                event.getRightClicked().getLocation())) {

            event.setCancelled(true);

            deny(event.getPlayer());
        }
    }


    /*
     * PROTECT CONTAINER INVENTORIES
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onInventoryOpen(
            InventoryOpenEvent event
    ) {

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Location location =
                event.getInventory().getLocation();

        if (location == null) {
            return;
        }

        if (!canModify(player, location)) {

            event.setCancelled(true);

            deny(player);
        }
    }


    /*
     * BLOCK AUTOMATED ITEM TRANSFERS
     * ACROSS PROPERTY BOUNDARIES
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onInventoryMove(
            InventoryMoveItemEvent event
    ) {

        EarthProperty source = getProperty(
                event.getSource().getLocation()
        );

        EarthProperty destination = getProperty(
                event.getDestination().getLocation()
        );

        if (!Objects.equals(source, destination)) {

            event.setCancelled(true);
        }
    }


    /*
     * PROTECT WATER AND LAVA BUCKET PLACEMENT
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBucketEmpty(
            PlayerBucketEmptyEvent event
    ) {

        Block target = event.getBlock();

        if (!canModify(
                event.getPlayer(),
                target.getLocation())) {

            event.setCancelled(true);

            deny(event.getPlayer());
        }
    }


    /*
     * PROTECT WATER AND LAVA COLLECTION
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBucketFill(
            PlayerBucketFillEvent event
    ) {

        if (!canModify(
                event.getPlayer(),
                event.getBlock().getLocation())) {

            event.setCancelled(true);

            deny(event.getPlayer());
        }
    }


    /*
     * PROTECT AGAINST FLOWING LIQUIDS
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onLiquidFlow(
            BlockFromToEvent event
    ) {

        Material material = event.getBlock().getType();

        if (material != Material.WATER
                && material != Material.LAVA) {

            return;
        }

        EarthProperty source = getProperty(
                event.getBlock().getLocation()
        );

        EarthProperty destination = getProperty(
                event.getToBlock().getLocation()
        );

        if (!Objects.equals(source, destination)) {

            event.setCancelled(true);
        }
    }


    /*
     * PROTECT AGAINST TNT AND ENTITY EXPLOSIONS
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityExplosion(
            EntityExplodeEvent event
    ) {

        event.blockList().removeIf(
                this::isProtected
        );
    }


    /*
     * PROTECT AGAINST BLOCK EXPLOSIONS
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockExplosion(
            BlockExplodeEvent event
    ) {

        event.blockList().removeIf(
                this::isProtected
        );
    }


    /*
     * PREVENT PROTECTED BUILDINGS FROM BURNING
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockBurn(BlockBurnEvent event) {

        if (isProtected(event.getBlock())) {

            event.setCancelled(true);
        }
    }


    /*
     * PREVENT FIRE IGNITION ON PROPERTIES
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockIgnite(
            BlockIgniteEvent event
    ) {

        if (isProtected(event.getBlock())) {

            event.setCancelled(true);
        }
    }


    /*
     * PREVENT FIRE SPREAD
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onBlockSpread(
            BlockSpreadEvent event
    ) {

        Material material =
                event.getNewState().getType();

        if (material == Material.FIRE
                || material == Material.SOUL_FIRE) {

            if (isProtected(event.getBlock())) {

                event.setCancelled(true);
            }
        }
    }


    /*
     * PROTECT AGAINST PISTONS
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onPistonExtend(
            BlockPistonExtendEvent event
    ) {

        EarthProperty origin = getProperty(
                event.getBlock().getLocation()
        );

        EarthProperty head = getProperty(
                event.getBlock()
                        .getRelative(event.getDirection())
                        .getLocation()
        );

        if (!Objects.equals(origin, head)) {

            event.setCancelled(true);
            return;
        }

        for (Block block : event.getBlocks()) {

            EarthProperty source = getProperty(
                    block.getLocation()
            );

            EarthProperty destination = getProperty(
                    block.getRelative(
                            event.getDirection()
                    ).getLocation()
            );

            if (!Objects.equals(origin, source)
                    || !Objects.equals(origin, destination)) {

                event.setCancelled(true);

                return;
            }
        }
    }


    /*
     * PROTECT AGAINST STICKY PISTON RETRACTION
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onPistonRetract(
            BlockPistonRetractEvent event
    ) {

        EarthProperty origin = getProperty(
                event.getBlock().getLocation()
        );

        EarthProperty head = getProperty(
                event.getBlock()
                        .getRelative(event.getDirection())
                        .getLocation()
        );

        if (!Objects.equals(origin, head)) {

            event.setCancelled(true);
            return;
        }

        for (Block block : event.getBlocks()) {

            EarthProperty source = getProperty(
                    block.getLocation()
            );

            EarthProperty destination = getProperty(
                    block.getRelative(
                            event.getDirection()
                                    .getOppositeFace()
                    ).getLocation()
            );

            if (!Objects.equals(origin, source)
                    || !Objects.equals(origin, destination)) {

                event.setCancelled(true);

                return;
            }
        }
    }


    /*
     * PROTECT AGAINST ENTITY BLOCK DAMAGE
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityChangeBlock(
            EntityChangeBlockEvent event
    ) {

        if (isProtected(event.getBlock())) {

            event.setCancelled(true);
        }
    }


    /*
     * PROTECT ITEM FRAMES AND PAINTINGS
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onHangingBreak(
            HangingBreakEvent event
    ) {

        EarthProperty property = getProperty(
                event.getEntity().getLocation()
        );

        if (property == null) {
            return;
        }

        if (event instanceof HangingBreakByEntityEvent byEntity
                && byEntity.getRemover() instanceof Player player
                && property.isOwnedBy(player.getUniqueId())) {

            return;
        }

        event.setCancelled(true);
    }


    /*
     * PROTECT HANGING ENTITY PLACEMENT
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onHangingPlace(
            HangingPlaceEvent event
    ) {

        Player player = event.getPlayer();

        if (player == null) {

            if (isProtected(event.getBlock())
                    || getProperty(
                            event.getEntity().getLocation()
                    ) != null) {

                event.setCancelled(true);
            }

            return;
        }

        if (!canModify(
                player,
                event.getBlock().getLocation())
                || !canModify(
                        player,
                        event.getEntity().getLocation())) {

            event.setCancelled(true);

            deny(player);
        }
    }


    /*
     * PROTECT NON-PLAYER ENTITIES
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityDamage(
            EntityDamageByEntityEvent event
    ) {

        if (event.getEntity() instanceof Player) {
            return;
        }

        EarthProperty property = getProperty(
                event.getEntity().getLocation()
        );

        if (property == null) {
            return;
        }

        if (event.getDamager() instanceof Player player
                && property.isOwnedBy(player.getUniqueId())) {

            return;
        }

        if (event.getDamager()
                instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Player shooter
                && property.isOwnedBy(shooter.getUniqueId())) {

            return;
        }

        event.setCancelled(true);
    }


    /*
     * PROTECT AGAINST EXTERNAL PROJECTILES
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onProjectileHit(
            ProjectileHitEvent event
    ) {

        Block block = event.getHitBlock();

        if (block == null) {
            return;
        }

        EarthProperty property =
                getProperty(block.getLocation());

        if (property == null) {
            return;
        }

        if (event.getEntity().getShooter()
                instanceof Player player
                && property.isOwnedBy(
                        player.getUniqueId())) {

            return;
        }

        event.setCancelled(true);
    }

}
