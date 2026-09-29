package com.earthbound;

import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerPortalEvent;

public class EarthBoundWorldRules implements Listener {


    private final EarthBound plugin;


    public EarthBoundWorldRules(
            EarthBound plugin
    ) {

        this.plugin = plugin;

    }



    /*
     * ============================================================
     * BLOCK HOSTILE MOB SPAWNING
     * ============================================================
     */

    @EventHandler
    public void onMobSpawn(
            EntitySpawnEvent event
    ) {


        boolean hostileMobs =
                plugin.getConfig()
                        .getBoolean(
                                "world.hostile-mobs",
                                false
                        );


        if (hostileMobs) {

            return;

        }


        if (event.getEntity() instanceof Monster) {


            event.setCancelled(true);


            plugin.getLogger().info(
                    "Blocked hostile mob: "
                            + event.getEntityType()
            );

        }

    }




    /*
     * ============================================================
     * BLOCK NETHER AND END
     * ============================================================
     */

    @EventHandler
    public void onPortal(
            PlayerPortalEvent event
    ) {


        if (event.getTo() == null) {

            return;

        }


        World destination =
                event.getTo()
                        .getWorld();


        if (destination == null) {

            return;

        }


        Environment environment =
                destination.getEnvironment();



        boolean nether =
                plugin.getConfig()
                        .getBoolean(
                                "world.nether",
                                false
                        );


        boolean end =
                plugin.getConfig()
                        .getBoolean(
                                "world.end",
                                false
                        );


        Player player =
                event.getPlayer();



        if (environment == Environment.NETHER
                && !nether) {


            event.setCancelled(true);


            player.sendMessage(
                    "§cThe Nether is disabled on EarthBound."
            );


        }



        if (environment == Environment.THE_END
                && !end) {


            event.setCancelled(true);


            player.sendMessage(
                    "§cThe End is disabled on EarthBound."
            );

        }

    }




    /*
     * ============================================================
     * HUNGER CONTROL
     * ============================================================
     */

    @EventHandler
    public void onFoodChange(
            FoodLevelChangeEvent event
    ) {


        if (!(event.getEntity() instanceof Player)) {

            return;

        }


        double hungerRate =
                plugin.getConfig()
                        .getDouble(
                                "player.hunger-rate",
                                0.5
                        );



        Player player =
                (Player) event.getEntity();



        int oldFood =
                player.getFoodLevel();


        int newFood =
                event.getFoodLevel();



        if (newFood < oldFood) {


            int loss =
                    oldFood - newFood;


            int adjustedLoss =
                    Math.max(
                            1,
                            (int)
                            Math.round(
                                    loss * hungerRate
                            )
                    );


            event.setFoodLevel(
                    oldFood - adjustedLoss
            );

        }

    }

}
