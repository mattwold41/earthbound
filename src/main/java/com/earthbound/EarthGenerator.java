@Override
public void generateSurface(
        WorldInfo worldInfo,
        Random random,
        int chunkX,
        int chunkZ,
        ChunkData chunkData) {

    for (int x = 0; x < 16; x++) {

        for (int z = 0; z < 16; z++) {


            int worldX = chunkX * 16 + x;
            int worldZ = chunkZ * 16 + z;


            double latitude =
                    EarthCoordinates.getLatitude(
                            worldX,
                            worldZ
                    );

            double longitude =
                    EarthCoordinates.getLongitude(
                            worldX,
                            worldZ
                    );


            boolean water =
                    EarthWaterData.isWater(
                            latitude,
                            longitude
                    );


            boolean road =
                    EarthRoadData.isRoad(
                            latitude,
                            longitude
                    );


            if (water || road) {
                continue;
            }


            int highest =
                    chunkData.getHighestBlockYAt(
                            x,
                            z
                    );


            Material surface =
                    chunkData.getType(
                            x,
                            highest,
                            z
                    );


            /*
             * Grassland vegetation
             */
            if (surface == Material.GRASS_BLOCK) {


                if (random.nextInt(8) == 0) {

                    chunkData.setBlock(
                            x,
                            highest + 1,
                            z,
                            Material.TALL_GRASS
                    );
                }


                /*
                 * Pacific Northwest forest
                 */
                if (random.nextInt(40) == 0) {

                    chunkData.setBlock(
                            x,
                            highest + 1,
                            z,
                            Material.SPRUCE_SAPLING
                    );
                }
            }
        }
    }
}
