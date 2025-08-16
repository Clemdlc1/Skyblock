package fr.skyblock.models;

import java.util.UUID;

/**
 * Représente une imprimante à argent posée sur une île.
 * Stockée dans l'objet Island (persistée en JSON via DatabaseManager).
 */
public class MoneyPrinter {

    private UUID id;
    private UUID ownerUuid;
    private int tier;

    // Position bloc dans le monde d'île (le monde est celui de l'île)
    private int x;
    private int y;
    private int z;

    // Horodatage pour la génération (ms epoch)
    private long lastGeneratedAt;

    public MoneyPrinter() {}

    public MoneyPrinter(UUID id, UUID ownerUuid, int tier, int x, int y, int z) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.tier = tier;
        this.x = x;
        this.y = y;
        this.z = z;
        this.lastGeneratedAt = 0L;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(UUID ownerUuid) { this.ownerUuid = ownerUuid; }

    public int getTier() { return tier; }
    public void setTier(int tier) { this.tier = tier; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getZ() { return z; }
    public void setZ(int z) { this.z = z; }

    public long getLastGeneratedAt() { return lastGeneratedAt; }
    public void setLastGeneratedAt(long lastGeneratedAt) { this.lastGeneratedAt = lastGeneratedAt; }
}


