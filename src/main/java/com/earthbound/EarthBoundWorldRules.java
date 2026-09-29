package com.earthbound;

import org.bukkit.World.Environment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerPortalEvent;

public class EarthBoundWorldRules implements Listener {

    private final EarthBound plugin;


    public EarthBoundWorldRules(EarthBound plugin) {
        this.plugin = plugin;
    }


    @EventHandler
    public void onMobSpawn(EntitySpawnEvent event) {

        boolean hostileMobs =
                plugin.getConfig()
                        .getBoolean(
                                "world.hostile-mobs",
                                false
                        );

        if (hostileMobs) {
            return;
        }


        EntityType type =
                event.getEntityType();


        switch (type) {

            case CREEPER:
            case ZOMBIE:
            case HUSK:
            case DROWNED:
            case SKELETON:
            case STRAY:
            case SPIDER:
            case CAVE_SPIDER:
            case ENDERMAN:
            case WITCH:
            case PHANTOM:

                event.setCancelled(true);
                break;

            default:
                break;
        }
    }



    @EventHandler
    public void onPortal(PlayerPortalEvent event) {

        if (event.getTo() == null) {
            return;
        }


        Environment destination =
                event.getTo()
                        .getWorld()
                        .getEnvironment();


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


        if (destination == Environment.NETHER
                && !nether) {

            event.setCancelled(true);

            player.sendMessage(
                    "§cThe Nether is disabled on EarthBound."
            );
        }


        if (destination == Environment.THE_END
                && !end) {

            event.setCancelled(true);

            player.sendMessage(
                    "§cThe End is disabled on EarthBound."
            );
        }
    }



    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {

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


            int adjusted =
                    (int) Math.max(
                            1,
                            Math.round(
                                    loss * hungerRate
                            )
                    );


            event.setFoodLevel(
                    oldFood - adjusted
            );
        }
    }
}
