package fr.skyblock.managers;

import fr.skyblock.CustomSkyblock;
import fr.skyblock.models.Island;
import fr.skyblock.models.SkyblockPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {

    private final CustomSkyblock plugin;
    private final File dataFolder;
    private final File islandsFile;
    private final File playersFile;

    private YamlConfiguration islandsConfig;
    private YamlConfiguration playersConfig;

    // Cache en mémoire
    private final Map<UUID, Island> islandsCache = new ConcurrentHashMap<>();
    private final Map<UUID, SkyblockPlayer> playersCache = new ConcurrentHashMap<>();

    public DatabaseManager(CustomSkyblock plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        this.islandsFile = new File(dataFolder, "islands.yml");
        this.playersFile = new File(dataFolder, "players.yml");

        setupFiles();
        loadAll();
    }

    private void setupFiles() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        if (!islandsFile.exists()) {
            try {
                islandsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de créer le fichier islands.yml: " + e.getMessage());
            }
        }

        if (!playersFile.exists()) {
            try {
                playersFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de créer le fichier players.yml: " + e.getMessage());
            }
        }

        islandsConfig = YamlConfiguration.loadConfiguration(islandsFile);
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }

    // === GESTION DES ÎLES ===

    public void saveIsland(Island island) {
        islandsCache.put(island.getId(), island);

        ConfigurationSection section = islandsConfig.createSection("islands." + island.getId().toString());
        island.saveToYaml(section);

        saveIslandsConfig();
    }

    public Island loadIsland(UUID islandId) {
        // Vérifier le cache d'abord
        if (islandsCache.containsKey(islandId)) {
            return islandsCache.get(islandId);
        }

        ConfigurationSection section = islandsConfig.getConfigurationSection("islands." + islandId.toString());
        if (section == null) {
            return null;
        }

        Island island = Island.loadFromYaml(section);
        islandsCache.put(islandId, island);
        return island;
    }

    public void deleteIsland(UUID islandId) {
        islandsCache.remove(islandId);
        islandsConfig.set("islands." + islandId.toString(), null);
        saveIslandsConfig();
    }

    public Collection<Island> getAllIslands() {
        // Charger toutes les îles si pas encore fait
        if (islandsConfig.getConfigurationSection("islands") != null) {
            for (String key : islandsConfig.getConfigurationSection("islands").getKeys(false)) {
                try {
                    UUID islandId = UUID.fromString(key);
                    if (!islandsCache.containsKey(islandId)) {
                        loadIsland(islandId);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("ID d'île invalide dans la base de données: " + key);
                }
            }
        }
        return new ArrayList<>(islandsCache.values());
    }

    public Island getIslandByOwner(UUID ownerUuid) {
        for (Island island : getAllIslands()) {
            if (island.getOwner().equals(ownerUuid)) {
                return island;
            }
        }
        return null;
    }

    // === GESTION DES JOUEURS ===

    public void savePlayer(SkyblockPlayer player) {
        playersCache.put(player.getUuid(), player);

        ConfigurationSection section = playersConfig.createSection("players." + player.getUuid().toString());
        player.saveToYaml(section);

        savePlayersConfig();
    }

    public SkyblockPlayer loadPlayer(UUID playerUuid) {
        // Vérifier le cache d'abord
        if (playersCache.containsKey(playerUuid)) {
            return playersCache.get(playerUuid);
        }

        ConfigurationSection section = playersConfig.getConfigurationSection("players." + playerUuid.toString());
        if (section == null) {
            return null;
        }

        SkyblockPlayer player = SkyblockPlayer.loadFromYaml(section);
        playersCache.put(playerUuid, player);
        return player;
    }

    public SkyblockPlayer getOrCreatePlayer(UUID playerUuid, String playerName) {
        SkyblockPlayer player = loadPlayer(playerUuid);
        if (player == null) {
            player = new SkyblockPlayer(playerUuid, playerName);
            savePlayer(player);
        } else {
            // Mettre à jour le nom si différent
            if (!player.getName().equals(playerName)) {
                player.setName(playerName);
                savePlayer(player);
            }
        }
        return player;
    }

    public Collection<SkyblockPlayer> getAllPlayers() {
        // Charger tous les joueurs si pas encore fait
        if (playersConfig.getConfigurationSection("players") != null) {
            for (String key : playersConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID playerUuid = UUID.fromString(key);
                    if (!playersCache.containsKey(playerUuid)) {
                        loadPlayer(playerUuid);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("UUID de joueur invalide dans la base de données: " + key);
                }
            }
        }
        return new ArrayList<>(playersCache.values());
    }

    // === MÉTHODES DE SAUVEGARDE ===

    private void saveIslandsConfig() {
        try {
            islandsConfig.save(islandsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde des îles: " + e.getMessage());
        }
    }

    private void savePlayersConfig() {
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde des joueurs: " + e.getMessage());
        }
    }

    public void saveAll() {
        plugin.getLogger().info("Sauvegarde de toutes les données...");

        // Sauvegarder toutes les îles du cache
        for (Island island : islandsCache.values()) {
            ConfigurationSection section = islandsConfig.createSection("islands." + island.getId().toString());
            island.saveToYaml(section);
        }
        saveIslandsConfig();

        // Sauvegarder tous les joueurs du cache
        for (SkyblockPlayer player : playersCache.values()) {
            ConfigurationSection section = playersConfig.createSection("players." + player.getUuid().toString());
            player.saveToYaml(section);
        }
        savePlayersConfig();

        plugin.getLogger().info("Sauvegarde terminée !");
    }

    private void loadAll() {
        plugin.getLogger().info("Chargement des données...");

        // Le chargement se fait à la demande pour optimiser les performances
        // Les méthodes getAllIslands() et getAllPlayers() chargent tout si nécessaire

        plugin.getLogger().info("Données chargées !");
    }

    // === MÉTHODES DE RECHERCHE AVANCÉES ===

    public List<Island> getIslandsByLevel(int minLevel) {
        return getAllIslands().stream()
                .filter(island -> island.getLevel() >= minLevel)
                .sorted((a, b) -> Integer.compare(b.getLevel(), a.getLevel()))
                .toList();
    }

    public List<Island> getTopIslandsByLevel(int limit) {
        return getAllIslands().stream()
                .sorted((a, b) -> Integer.compare(b.getLevel(), a.getLevel()))
                .limit(limit)
                .toList();
    }

    public List<Island> getInactiveIslands(long inactiveDays) {
        long threshold = System.currentTimeMillis() - (inactiveDays * 24 * 60 * 60 * 1000);
        return getAllIslands().stream()
                .filter(island -> island.getLastActivity() < threshold)
                .toList();
    }

    // === CACHE MANAGEMENT ===

    public void clearCache() {
        islandsCache.clear();
        playersCache.clear();
    }

    public void reloadFromDisk() {
        clearCache();
        islandsConfig = YamlConfiguration.loadConfiguration(islandsFile);
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        loadAll();
    }

    // === STATISTIQUES ===

    public int getTotalIslands() {
        return getAllIslands().size();
    }

    public int getTotalPlayers() {
        return getAllPlayers().size();
    }

    public int getActiveIslands(long activeDays) {
        long threshold = System.currentTimeMillis() - (activeDays * 24 * 60 * 60 * 1000);
        return (int) getAllIslands().stream()
                .filter(island -> island.getLastActivity() >= threshold)
                .count();
    }
}