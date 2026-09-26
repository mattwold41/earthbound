package com.earthbound;

import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;

import java.util.Random;

public class EarthTreeListener implements Listener {


    @EventHandler
    public void onChunkPopulate(
            ChunkPopulateEvent event) {


        World world =
                event.getWorld();


        Random random =
                new Random();


        int baseX =
                event.getChunk().getX() * 16;


        int baseZ =
                event.getChunk().getZ() * 16;



        /*
         * Try several tree locations
         * in each chunk.
         */
        for (int i = 0; i < 3; i++) {


            int x =
                    baseX + random.nextInt(16);


            int z =
                    baseZ + random.nextInt(16);


            int y =
                    world.getHighestBlockYAt(
                            x,
                            z
                    );



            Material ground =
                    world.getBlockAt(
                            x,
                            y - 1,
                            z
                    ).getType();



            /*
             * Only grow trees on grass.
             */
            if (ground != Material.GRASS_BLOCK) {

                continue;
            }



            /*
             * Avoid roads.
             */
            if (world.getBlockAt(
                    x,
                    y - 1,
                    z
            ).getType() == Material.GRAY_CONCRETE) {

                continue;
            }



            EarthTreePopulator.placeTree(
                    world,
                    x,
                    y,
                    z,
                    random
            );
        }
    }
}
