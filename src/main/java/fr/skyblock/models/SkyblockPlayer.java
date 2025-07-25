package fr.skyblock.models;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class SkyblockPlayer {

    private UUID uuid;
    private String name;
    private UUID islandId; // ID de l'île dont il est propriétaire
    private Set<UUID> memberOfIslands; // Îles dont il est membre
    private long firstJoin;
    private long lastSeen;
    private boolean hasIsland;
    private int islandResets; // Nombre de fois qu'il a reset son île
    private Map<String, Object> data; // Données supplémentaires

    public SkyblockPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.islandId = null;
        this.memberOfIslands = new HashSet<>();
        this.firstJoin = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
        this.hasIsland = false;
        this.islandResets = 0;
        this.data = new HashMap<>();
    }

    // Méthodes utiles
    public void createIsland(UUID islandId) {
        this.islandId = islandId;
        this.hasIsland = true;
    }

    public void deleteIsland() {
        this.islandId = null;
        this.hasIsland = false;
        this.islandResets++;
    }

    public void joinIsland(UUID islandId) {
        memberOfIslands.add(islandId);
    }

    public void leaveIsland(UUID islandId) {
        memberOfIslands.remove(islandId);
    }

    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }

    // Sauvegarde et chargement YAML
    public void saveToYaml(ConfigurationSection section) {
        section.set("uuid", uuid.toString());
        section.set("name", name);
        section.set("island-id", islandId != null ? islandId.toString() : null);
        section.set("has-island", hasIsland);
        section.set("first-join", firstJoin);
        section.set("last-seen", lastSeen);
        section.set("island-resets", islandResets);

        // Sauvegarde des îles dont il est membre
        List<String> memberOfList = new ArrayList<>();
        for (UUID id : memberOfIslands) {
            memberOfList.add(id.toString());
        }
        section.set("member-of-islands", memberOfList);

        // Sauvegarde des données supplémentaires
        if (!data.isEmpty()) {
            ConfigurationSection dataSection = section.createSection("data");
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                dataSection.set(entry.getKey(), entry.getValue());
            }
        }
    }

    public static SkyblockPlayer loadFromYaml(ConfigurationSection section) {
        UUID uuid = UUID.fromString(section.getString("uuid"));
        String name = section.getString("name");

        SkyblockPlayer player = new SkyblockPlayer(uuid, name);

        String islandIdStr = section.getString("island-id");
        if (islandIdStr != null) {
            player.islandId = UUID.fromString(islandIdStr);
        }

        player.hasIsland = section.getBoolean("has-island", false);
        player.firstJoin = section.getLong("first-join", System.currentTimeMillis());
        player.lastSeen = section.getLong("last-seen", System.currentTimeMillis());
        player.islandResets = section.getInt("island-resets", 0);

        // Chargement des îles dont il est membre
        List<String> memberOfList = section.getStringList("member-of-islands");
        for (String idStr : memberOfList) {
            player.memberOfIslands.add(UUID.fromString(idStr));
        }

        // Chargement des données supplémentaires
        ConfigurationSection dataSection = section.getConfigurationSection("data");
        if (dataSection != null) {
            for (String key : dataSection.getKeys(false)) {
                player.data.put(key, dataSection.get(key));
            }
        }

        return player;
    }

    // Getters et Setters
    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getIslandId() { return islandId; }
    public boolean hasIsland() { return hasIsland; }
    public Set<UUID> getMemberOfIslands() { return new HashSet<>(memberOfIslands); }
    public long getFirstJoin() { return firstJoin; }
    public long getLastSeen() { return lastSeen; }
    public int getIslandResets() { return islandResets; }

    // Gestion des données supplémentaires
    public void setData(String key, Object value) {
        data.put(key, value);
    }

    public Object getData(String key) {
        return data.get(key);
    }

    public <T> T getData(String key, Class<T> type) {
        Object value = data.get(key);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    public boolean hasData(String key) {
        return data.containsKey(key);
    }

    public void removeData(String key) {
        data.remove(key);
    }
}