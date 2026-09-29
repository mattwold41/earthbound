package com.earthbound;

import java.io.File;
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



    /*
     * ============================================================
     * START ECONOMY SYSTEM
     * ============================================================
     */

    public static void setup(
            EarthBound earthBound
    ) {


        plugin = earthBound;


        file =
                new File(
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
                YamlConfiguration
                        .loadConfiguration(
                                file
                        );


        loadBalances();


    }



    /*
     * ============================================================
     * LOAD SAVED MONEY
     * ============================================================
     */

    private static void loadBalances() {


        for (String key :
                data.getKeys(false)) {


            UUID uuid =
                    UUID.fromString(
                            key
                    );


            double amount =
                    data.getDouble(
                            key
                    );


            balances.put(
                    uuid,
                    amount
            );

        }

    }



    /*
     * ============================================================
     * SAVE MONEY
     * ============================================================
     */

    public static void save() {


        for (UUID uuid :
                balances.keySet()) {


            data.set(
                    uuid.toString(),
                    balances.get(uuid)
            );

        }


        try {

            data.save(
                    file
            );

        } catch (IOException e) {

            e.printStackTrace();

        }

    }



    /*
     * ============================================================
     * CREATE PLAYER ACCOUNT
     * ============================================================
     */

    public static void createAccount(
            Player player
    ) {


        UUID uuid =
                player.getUniqueId();



        if (!balances.containsKey(uuid)) {


            double startingMoney =
                    plugin.getConfig()
                            .getDouble(
                                    "economy.starting-money",
                                    100000
                            );


            balances.put(
                    uuid,
                    startingMoney
            );


            save();

        }

    }



    /*
     * ============================================================
     * GET BALANCE
     * ============================================================
     */

    public static double getBalance(
            Player player
    ) {


        createAccount(
                player
        );


        return balances.get(
                player.getUniqueId()
        );

    }



    /*
     * ============================================================
     * ADD MONEY
     * ============================================================
     */

    public static void addMoney(
            Player player,
            double amount
    ) {


        createAccount(
                player
        );


        UUID uuid =
                player.getUniqueId();



        double current =
                balances.get(uuid);



        balances.put(
                uuid,
                current + amount
        );


        save();

    }



    /*
     * ============================================================
     * REMOVE MONEY
     * ============================================================
     */

    public static boolean removeMoney(
            Player player,
            double amount
    ) {


        createAccount(
                player
        );


        UUID uuid =
                player.getUniqueId();



        double current =
                balances.get(uuid);



        if (current < amount) {

            return false;

        }



        balances.put(
                uuid,
                current - amount
        );


        save();


        return true;

    }

}
