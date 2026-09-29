package com.earthbound;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class EarthEconomy {


    private static EarthBound plugin;

    private static File file;

    private static YamlConfiguration data;

    private static HashMap<UUID, Double> balances =
            new HashMap<>();



    public static void setup(EarthBound earthBound) {

        plugin = earthBound;


        file = new File(
                plugin.getDataFolder(),
                "economy.yml"
        );


        if (!plugin.getDataFolder().exists()) {

            plugin.getDataFolder().mkdirs();

        }


        if (!file.exists()) {

            try {

                file.createNewFile();

            } catch (IOException e) {

                e.printStackTrace();

            }

        }


        data =
                YamlConfiguration.loadConfiguration(file);


        loadBalances();

    }



    private static void loadBalances() {


        for (String key :
                data.getKeys(false)) {


            UUID uuid =
                    UUID.fromString(key);


            double money =
                    data.getDouble(key);


            balances.put(
                    uuid,
                    money
            );

        }

    }



    public static void save() {


        for (UUID uuid :
                balances.keySet()) {


            data.set(
                    uuid.toString(),
                    balances.get(uuid)
            );

        }


        try {

            data.save(file);

        } catch (IOException e) {

            e.printStackTrace();

        }

    }



    public static void createAccount(
            Player player
    ) {


        if (!balances.containsKey(
                player.getUniqueId()
        )) {


            double startingMoney =
                    plugin.getConfig()
                            .getDouble(
                                    "economy.starting-money",
                                    100000
                            );


            balances.put(
                    player.getUniqueId(),
                    startingMoney
            );


            save();

        }

    }



    public static double getBalance(
            Player player
    ) {

        createAccount(player);


        return balances.get(
                player.getUniqueId()
        );

    }

}
