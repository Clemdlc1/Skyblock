package fr.skyblock.models;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class Island {

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

    // Sauvegarde et chargement YAML
    public void saveToYaml(ConfigurationSection section) {
        section.set("id", id.toString());
        section.set("owner", owner.toString());
        section.set("name", name);
        section.set("center.world", center.getWorld().getName());
        section.set("center.x", center.getX());
        section.set("center.y", center.getY());
        section.set("center.z", center.getZ());
        section.set("size", size);
        section.set("level", level);
        section.set("bank", bank);
        section.set("creation-time", creationTime);
        section.set("last-activity", lastActivity);

        // Sauvegarde des membres
        List<String> membersList = new ArrayList<>();
        for (UUID member : members) {
            membersList.add(member.toString());
        }
        section.set("members", membersList);

        // Sauvegarde des visiteurs
        List<String> visitorsList = new ArrayList<>();
        for (UUID visitor : visitors) {
            visitorsList.add(visitor.toString());
        }
        section.set("visitors", visitorsList);

        // Sauvegarde des flags
        ConfigurationSection flagsSection = section.createSection("flags");
        for (Map.Entry<IslandFlag, Boolean> entry : flags.entrySet()) {
            flagsSection.set(entry.getKey().name(), entry.getValue());
        }
    }

    public static Island loadFromYaml(ConfigurationSection section) {
        UUID id = UUID.fromString(section.getString("id"));
        UUID owner = UUID.fromString(section.getString("owner"));
        String name = section.getString("name");

        // Reconstruction de la location
        String worldName = section.getString("center.world");
        double x = section.getDouble("center.x");
        double y = section.getDouble("center.y");
        double z = section.getDouble("center.z");
        Location center = new Location(org.bukkit.Bukkit.getWorld(worldName), x, y, z);

        Island island = new Island(id, owner, name, center);
        island.size = section.getInt("size", 50);
        island.level = section.getInt("level", 1);
        island.bank = section.getDouble("bank", 0.0);
        island.creationTime = section.getLong("creation-time", System.currentTimeMillis());
        island.lastActivity = section.getLong("last-activity", System.currentTimeMillis());

        // Chargement des membres
        List<String> membersList = section.getStringList("members");
        for (String memberStr : membersList) {
            island.members.add(UUID.fromString(memberStr));
        }

        // Chargement des visiteurs
        List<String> visitorsList = section.getStringList("visitors");
        for (String visitorStr : visitorsList) {
            island.visitors.add(UUID.fromString(visitorStr));
        }

        // Chargement des flags
        ConfigurationSection flagsSection = section.getConfigurationSection("flags");
        if (flagsSection != null) {
            for (String key : flagsSection.getKeys(false)) {
                try {
                    IslandFlag flag = IslandFlag.valueOf(key);
                    island.flags.put(flag, flagsSection.getBoolean(key));
                } catch (IllegalArgumentException e) {
                    // Flag inconnu, ignorer
                }
            }
        }

        return island;
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
    public void setFlag(IslandFlag flag, boolean value) { flags.put(flag, value); }
    public boolean getFlag(IslandFlag flag) { return flags.getOrDefault(flag, false); }
    public long getCreationTime() { return creationTime; }
    public long getLastActivity() { return lastActivity; }
}