package com.earthbound;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthBound extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("EarthBound is now online!");

        getLifecycleManager().registerEventHandler(
            LifecycleEvents.COMMANDS,
            event -> event.registrar().register(
                Commands.literal("earth")
                    .then(
                        Commands.literal("locate")
                            .executes(context -> {
                                CommandSourceStack source = context.getSource();

                                if (!(source.getSender() instanceof Player player)) {
                                    source.getSender().sendPlainMessage(
                                        "This command can only be used by a player."
                                    );
                                    return 1;
                                }

                                double x = player.getLocation().getX();
                                double y = player.getLocation().getY();
                                double z = player.getLocation().getZ();

                                player.sendMessage("§6§lEARTHBOUND");
                                player.sendMessage("§7Minecraft Coordinates:");
                                player.sendMessage("§fX: §e" + String.format("%.2f", x));
                                player.sendMessage("§fY: §e" + String.format("%.2f", y));
                                player.sendMessage("§fZ: §e" + String.format("%.2f", z));
                                player.sendMessage("§7Earth coordinates: §cComing soon");
                                player.sendMessage("§7Real-world location: §cComing soon");

                                return 1;
                            })
                    )
            )
        );
    }
