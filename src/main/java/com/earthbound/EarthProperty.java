package com.earthbound;

import java.util.UUID;

public class EarthProperty {

    private final String id;
    private final String name;
    private final String worldName;

    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;

    private final double price;

    private UUID owner;

    public EarthProperty(
            String id,
            String name,
            String worldName,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            double price
    ) {

        this.id = id;
        this.name = name;
        this.worldName = worldName;

        this.minX = Math.min(minX, maxX);
        this.maxX = Math.max(minX, maxX);

        this.minZ = Math.min(minZ, maxZ);
        this.maxZ = Math.max(minZ, maxZ);

        if (!Double.isFinite(price) || price < 0) {
            throw new IllegalArgumentException(
                    "Property price must be finite and nonnegative."
            );
        }

        this.price = price;
        this.owner = null;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public double getPrice() {
        return price;
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean isOwned() {
        return owner != null;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public boolean isOwnedBy(UUID playerId) {
        return playerId != null
                && playerId.equals(owner);
    }

    public boolean contains(
            String world,
            int x,
            int z
    ) {

        return worldName.equals(world)
                && x >= minX
                && x <= maxX
                && z >= minZ
                && z <= maxZ;
    }

    public int getWidth() {
        return maxX - minX + 1;
    }

    public int getLength() {
        return maxZ - minZ + 1;
    }
}
