package fr.skyblock.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Location;

import java.util.*;

public class Island {

    private static final Gson gson = new GsonBuilder().create();

    private UUID id;
    private UUID owner;
    private String name;
    private Location center;
    private int size;
    private int level;
    private double bank;
    private Set<UUID> members;
    private Set<UUID> visitors;
    private Map<IslandFlag, Boolean> flags;
    private long creationTime;
    private long lastActivity;

    // === Améliorations d'île ===
    private int hopperLimit;               // Nombre max de hoppers autorisés
    private int currentHoppers;            // Compteur courant de hoppers dans le monde de l'île

    // Améliorations (partiellement utilisées: GUI + data)
    private int maxDepositChests;          // Nombre max de caisses de dépôt posables
    private int billGenerationSpeed;       // Vitesse de génération de billets (unité arbitraire / s)
    private int maxPrinters;               // Nombre max d'imprimantes sur l'île

    // Imprimantes stockées dans l'île (persistées en JSON)
    private List<MoneyPrinter> printers;

    public enum IslandFlag {
        PVP("Autoriser le PvP"),
        MOB_SPAWNING("Spawn des mobs"),
        ANIMAL_SPAWNING("Spawn des animaux"),
        FIRE_SPREAD("Propagation du feu"),
        EXPLOSION_DAMAGE("Dégâts d'explosion"),
        VISITOR_INTERACT("Interaction des visiteurs"),
        VISITOR_PLACE("Placement des visiteurs"),
        VISITOR_BREAK("Casse des visiteurs"),
        VISITOR_CHEST("Accès coffres visiteurs");

        private final String description;

        IslandFlag(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public Island(UUID id, UUID owner, String name, Location center) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.center = center;
        this.size = 50; // Taille par défaut
        this.level = 1;
        this.bank = 0.0;
        this.members = new HashSet<>();
        this.visitors = new HashSet<>();
        this.flags = new HashMap<>();
        this.creationTime = System.currentTimeMillis();
        this.lastActivity = System.currentTimeMillis();

        // Valeurs par défaut des améliorations
        this.hopperLimit = 10;
        this.currentHoppers = 0;

        this.maxDepositChests = 1;
        this.billGenerationSpeed = 1;
        this.maxPrinters = 0;

        this.printers = new ArrayList<>();

        // Initialisation des flags par défaut
        initializeDefaultFlags();
    }

    private void initializeDefaultFlags() {
        flags.put(IslandFlag.PVP, false);
        flags.put(IslandFlag.MOB_SPAWNING, true);
        flags.put(IslandFlag.ANIMAL_SPAWNING, true);
        flags.put(IslandFlag.FIRE_SPREAD, false);
        flags.put(IslandFlag.EXPLOSION_DAMAGE, false);
        flags.put(IslandFlag.VISITOR_INTERACT, false);
        flags.put(IslandFlag.VISITOR_PLACE, false);
        flags.put(IslandFlag.VISITOR_BREAK, false);
        flags.put(IslandFlag.VISITOR_CHEST, false);
    }

    // Méthodes utiles
    public boolean isMember(UUID player) {
        return owner.equals(player) || members.contains(player);
    }

    public boolean isVisitor(UUID player) {
        return visitors.contains(player);
    }

    public boolean canInteract(UUID player) {
        if (isMember(player)) return true;
        return isVisitor(player) && flags.get(IslandFlag.VISITOR_INTERACT);
    }

    public void addMember(UUID player) {
        members.add(player);
        visitors.remove(player); // Retire des visiteurs s'il était visiteur
    }

    public void removeMember(UUID player) {
        members.remove(player);
    }

    public void addVisitor(UUID player) {
        if (!isMember(player)) {
            visitors.add(player);
        }
    }

    public void removeVisitor(UUID player) {
        visitors.remove(player);
    }

    public void updateActivity() {
        this.lastActivity = System.currentTimeMillis();
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static Island fromJson(String json) {
        return gson.fromJson(json, Island.class);
    }

    // Getters et Setters
    public UUID getId() { return id; }
    public UUID getOwner() { return owner; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Location getCenter() { return center; }
    public void setCenter(Location center) { this.center = center; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public double getBank() { return bank; }
    public void setBank(double bank) { this.bank = bank; }
    public void addToBank(double amount) { this.bank += amount; }
    public boolean removeFromBank(double amount) {
        if (this.bank >= amount) {
            this.bank -= amount;
            return true;
        }
        return false;
    }
    public Set<UUID> getMembers() { return new HashSet<>(members); }
    public Set<UUID> getVisitors() { return new HashSet<>(visitors); }
    public Map<IslandFlag, Boolean> getFlags() { return new HashMap<>(flags); }
    public void setFlag(IslandFlag flag, boolean value) {
        if (flags == null) {
            flags = new HashMap<>();
            initializeDefaultFlags();
        }
        flags.put(flag, value);
    }
    public boolean getFlag(IslandFlag flag) {
        if (flags == null) {
            flags = new HashMap<>();
            initializeDefaultFlags();
        }
        return flags.getOrDefault(flag, false);
    }
    public long getCreationTime() { return creationTime; }
    public long getLastActivity() { return lastActivity; }
    public void setLastActivity(long lastActivity) { this.lastActivity = lastActivity; }

    // === Getters/Setters Améliorations ===
    public int getHopperLimit() { return hopperLimit; }
    public void setHopperLimit(int hopperLimit) { this.hopperLimit = Math.max(0, hopperLimit); }

    public int getCurrentHoppers() { return currentHoppers; }
    public void setCurrentHoppers(int currentHoppers) { this.currentHoppers = Math.max(0, currentHoppers); }
    public void incrementHoppers() { this.currentHoppers = Math.max(0, this.currentHoppers + 1); }
    public void decrementHoppers() { this.currentHoppers = Math.max(0, this.currentHoppers - 1); }

    public int getMaxDepositChests() { return maxDepositChests; }
    public void setMaxDepositChests(int maxDepositChests) { this.maxDepositChests = Math.max(0, maxDepositChests); }

    public int getBillGenerationSpeed() { return billGenerationSpeed; }
    public void setBillGenerationSpeed(int billGenerationSpeed) { this.billGenerationSpeed = Math.max(0, billGenerationSpeed); }

    public int getMaxPrinters() { return maxPrinters; }
    public void setMaxPrinters(int maxPrinters) { this.maxPrinters = Math.max(0, maxPrinters); }

    // === Imprimantes ===
    public List<MoneyPrinter> getPrinters() {
        return new ArrayList<>(printers);
    }

    public void setPrinters(List<MoneyPrinter> printers) {
        this.printers = (printers != null) ? new ArrayList<>(printers) : new ArrayList<>();
    }

    public void addPrinter(MoneyPrinter printer) {
        if (this.printers == null) this.printers = new ArrayList<>();
        this.printers.add(printer);
    }

    public boolean removePrinterById(UUID printerId) {
        if (this.printers == null) return false;
        return this.printers.removeIf(p -> p.getId().equals(printerId));
    }

    public MoneyPrinter findPrinterAt(int x, int y, int z) {
        if (this.printers == null) return null;
        for (MoneyPrinter p : printers) {
            if (p.getX() == x && p.getY() == y && p.getZ() == z) {
                return p;
            }
        }
        return null;
    }

    public int countPrintersOwnedBy(UUID ownerUuid) {
        if (this.printers == null) return 0;
        int count = 0;
        for (MoneyPrinter p : printers) {
            if (p.getOwnerUuid().equals(ownerUuid)) count++;
        }
        return count;
    }
}