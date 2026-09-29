package com.earthbound;

import java.util.HashMap;

import org.bukkit.Material;

public class EarthStore {


    private static final HashMap<Material, Double> buyPrices =
            new HashMap<>();


    private static final HashMap<Material, Double> sellPrices =
            new HashMap<>();



    /*
     * ============================================================
     * LOAD STORE ITEMS
     * ============================================================
     */

    public static void setup() {


        /*
         * ==========================
         * ITEMS PLAYERS CAN BUY
         * ==========================
         */


        buyPrices.put(
                Material.BREAD,
                10.0
        );


        buyPrices.put(
                Material.WHEAT_SEEDS,
                2.0
        );


        buyPrices.put(
                Material.OAK_LOG,
                8.0
        );



        /*
         * ==========================
         * ITEMS PLAYERS CAN SELL
         * ==========================
         */


        sellPrices.put(
                Material.OAK_LOG,
                4.0
        );


        sellPrices.put(
                Material.WHEAT,
                3.0
        );


        sellPrices.put(
                Material.STONE,
                1.0
        );

    }



    /*
     * ============================================================
     * GET BUY PRICE
     * ============================================================
     */

    public static double getBuyPrice(
            Material material
    ) {


        return buyPrices.getOrDefault(
                material,
                -1.0
        );

    }



    /*
     * ============================================================
     * GET SELL PRICE
     * ============================================================
     */

    public static double getSellPrice(
            Material material
    ) {


        return sellPrices.getOrDefault(
                material,
                -1.0
        );

    }

}
