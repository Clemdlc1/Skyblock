package fr.skyblock.managers;

import fr.skyblock.CustomSkyblock;
import fr.skyblock.models.Island;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.mvplugins.multiverse.core.locale.message.Message;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;
import org.mvplugins.multiverse.core.world.options.DeleteWorldOptions;
import org.mvplugins.multiverse.core.world.reasons.CreateFailureReason;
import org.mvplugins.multiverse.core.world.reasons.DeleteFailureReason;
import org.mvplugins.multiverse.external.jetbrains.annotations.NotNull;
import org.mvplugins.multiverse.external.vavr.control.Option;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class WorldManager {

    private final CustomSkyblock plugin;
    private final Map<UUID, String> islandWorlds = new ConcurrentHashMap<>();
    private final String worldPrefix = "island_";

    public WorldManager(CustomSkyblock plugin) {
        this.plugin = plugin;
        loadExistingWorlds();
    }

    /**
     * Crée un nouveau monde pour une île
     */
    public World createIslandWorld(Island island) {
        String worldName = worldPrefix + island.getId().toString();

        try {
            org.mvplugins.multiverse.core.world.@NotNull WorldManager mvWorldManager = plugin.getMultiverseCoreApi().getWorldManager();

            CreateWorldOptions options = CreateWorldOptions.worldName(worldName)
                    .environment(World.Environment.NORMAL)
                    .worldType(WorldType.FLAT)
                    .generateStructures(false)
                    .generator("VoidWorldGenerator");

            Attempt<LoadedMultiverseWorld, CreateFailureReason> result = mvWorldManager.createWorld(options);

            if (result.isFailure()) {
                Message failureMessage = result.getFailureMessage();
                return null;
            }

            // CORRECTION : Utiliser get() pour récupérer la valeur en cas de succès.
            LoadedMultiverseWorld loadedWorld = result.get();
            World world = loadedWorld.getBukkitWorld().getOrNull();

            if (world == null) {
                plugin.getLogger().severe("Monde " + worldName + " créé mais introuvable !");
                return null;
            }

            setupWorldSettings(world);
            islandWorlds.put(island.getId(), worldName);
            Location newCenter = new Location(world, 0, 64, 0);
            island.setCenter(newCenter);
            plugin.getLogger().info("Monde créé pour l'île " + island.getId() + " : " + worldName);

            return world;

        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de la création du monde " + worldName + " : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Supprime le monde d'une île
     */
    public boolean deleteIslandWorld(Island island) {
        String worldName = islandWorlds.get(island.getId());
        if (worldName == null) {
            plugin.getLogger().warning("Aucun monde trouvé pour l'île " + island.getId());
            return false;
        }

        org.mvplugins.multiverse.core.world.@NotNull WorldManager mvWorldManager = plugin.getMultiverseCoreApi().getWorldManager();

        Option<MultiverseWorld> mvWorldOption = mvWorldManager.getWorld(worldName);
        if (mvWorldOption.isEmpty()) {
            plugin.getLogger().warning("Le monde '" + worldName + "' n'est pas connu de Multiverse. Nettoyage de l'entrée invalide.");
            islandWorlds.remove(island.getId());
            return false;
        }
        MultiverseWorld mvWorld = mvWorldOption.get();

        // CORRECTION : Vérifier si le monde est chargé avant de tenter d'accéder aux joueurs.
        if (mvWorld.isLoaded()) {
            // Obtenir la version CHARGÉE du monde pour pouvoir accéder au monde Bukkit.
            Option<LoadedMultiverseWorld> loadedWorldOption = mvWorldManager.getLoadedWorld(mvWorld);
            if (loadedWorldOption.isDefined()) {
                // L'objet `loadedWorld` a accès au monde Bukkit.
                LoadedMultiverseWorld loadedWorld = loadedWorldOption.get();
                // getBukkitWorld() retourne une Option<World>, nous utilisons peek pour agir si elle est présente.
                loadedWorld.getBukkitWorld().peek(bukkitWorld -> {
                    // Maintenant nous avons l'objet World de Bukkit et nous pouvons appeler getPlayers()
                    for (Player player : bukkitWorld.getPlayers()) {
                        Location spawnLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
                        player.teleport(spawnLocation);
                        player.sendMessage("§cVous avez été téléporté car le monde de l'île a été supprimé.");
                    }
                });
            }
        }

        try {
            DeleteWorldOptions options = DeleteWorldOptions.world(mvWorld);
            Attempt<String, DeleteFailureReason> result = mvWorldManager.deleteWorld(options);

            if (result.isSuccess()) {
                islandWorlds.remove(island.getId());
                plugin.getLogger().info("Monde " + worldName + " et ses fichiers supprimés avec succès via Multiverse.");
                return true;
            } else {
                Message failureMessage = result.getFailureMessage();
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur lors de la suppression du monde " + worldName + " : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Obtient le monde d'une île
     */
    public World getIslandWorld(Island island) {
        String worldName = islandWorlds.get(island.getId());
        if (worldName == null) {
            return null;
        }
        return Bukkit.getWorld(worldName);
    }

    /**
     * Obtient le nom du monde d'une île
     */
    public String getIslandWorldName(Island island) {
        return islandWorlds.get(island.getId());
    }

    /**
     * Vérifie si un monde appartient à une île
     */
    public boolean isIslandWorld(String worldName) {
        return worldName.startsWith(worldPrefix);
    }

    /**
     * Obtient l'ID de l'île à partir du nom du monde
     */
    public UUID getIslandIdFromWorldName(String worldName) {
        if (!isIslandWorld(worldName)) {
            return null;
        }

        try {
            String uuidString = worldName.substring(worldPrefix.length());
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Nom de monde d'île invalide : " + worldName);
            return null;
        }
    }

    /**
     * Configure les paramètres d'un monde d'île
     */
    private void setupWorldSettings(World world) {
        // Paramètres de base
        world.setDifficulty(org.bukkit.Difficulty.NORMAL);
        world.setSpawnLocation(0, 64, 0);
        world.setKeepSpawnInMemory(false);
        world.setAutoSave(true);

        // Règles de jeu
        world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, true);
        world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, true);
        world.setGameRule(org.bukkit.GameRule.KEEP_INVENTORY, false);
        world.setGameRule(org.bukkit.GameRule.MOB_GRIEFING, false);
        world.setGameRule(org.bukkit.GameRule.DO_FIRE_TICK, true);
        world.setGameRule(org.bukkit.GameRule.RANDOM_TICK_SPEED, 3);

        // World border par défaut
        org.bukkit.WorldBorder border = world.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(100); // Taille par défaut
        border.setWarningDistance(5);
        border.setWarningTime(15);
        border.setDamageAmount(0.2);
        border.setDamageBuffer(5);
    }

    /**
     * Met à jour la world border d'une île
     */
    public void updateWorldBorder(Island island) {
        World world = getIslandWorld(island);
        if (world == null) return;

        org.bukkit.WorldBorder border = world.getWorldBorder();
        border.setCenter(island.getCenter().getX(), island.getCenter().getZ());
        border.setSize(island.getSize());
    }

    /**
     * Charge les mondes existants au démarrage
     */
    private void loadExistingWorlds() {
        // Parcourir tous les mondes chargés pour trouver les mondes d'îles
        for (World world : Bukkit.getWorlds()) {
            String worldName = world.getName();
            if (isIslandWorld(worldName)) {
                UUID islandId = getIslandIdFromWorldName(worldName);
                if (islandId != null) {
                    islandWorlds.put(islandId, worldName);
                    plugin.getLogger().info("Monde d'île chargé : " + worldName + " -> " + islandId);
                }
            }
        }

        // Vérifier les mondes dans Multiverse
        if (plugin.getMultiverseCoreApi() != null) {
            org.mvplugins.multiverse.core.world.@NotNull WorldManager mvWorldManager = plugin.getMultiverseCoreApi().getWorldManager();
            for (MultiverseWorld mvWorld : mvWorldManager.getWorlds()) {
                String worldName = mvWorld.getName();
                if (isIslandWorld(worldName) && !islandWorlds.containsValue(worldName)) {
                    UUID islandId = getIslandIdFromWorldName(worldName);
                    if (islandId != null) {
                        islandWorlds.put(islandId, worldName);
                        plugin.getLogger().info("Monde d'île Multiverse chargé : " + worldName + " -> " + islandId);
                    }
                }
            }
        }

        plugin.getLogger().info("Chargement terminé : " + islandWorlds.size() + " mondes d'îles trouvés");
    }

    /**
     * Supprime physiquement les fichiers d'un monde
     */
    private void deleteWorldFiles(String worldName) {
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists() && worldFolder.isDirectory()) {
            try {
                deleteDirectory(worldFolder);
                plugin.getLogger().info("Fichiers du monde " + worldName + " supprimés");
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors de la suppression des fichiers du monde " + worldName + " : " + e.getMessage());
            }
        }
    }

    /**
     * Supprime récursivement un dossier
     */
    private boolean deleteDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            return false;
        }

        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }

        return directory.delete();
    }

    /**
     * Obtient le nombre de mondes d'îles
     */
    public int getIslandWorldCount() {
        return islandWorlds.size();
    }

    /**
     * Nettoie les références aux mondes supprimés
     */
    public void cleanupMissingWorlds() {
        islandWorlds.entrySet().removeIf(entry -> {
            String worldName = entry.getValue();
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().info("Nettoyage de la référence au monde manquant : " + worldName);
                return true;
            }
            return false;
        });
    }

    /**
     * Sauvegarde tous les mondes d'îles
     */
    public void saveAllIslandWorlds() {
        for (String worldName : islandWorlds.values()) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                world.save();
            }
        }
    }

    /**
     * Vérifie si une île a un monde associé
     */
    public boolean hasWorld(Island island) {
        return islandWorlds.containsKey(island.getId()) &&
                Bukkit.getWorld(islandWorlds.get(island.getId())) != null;
    }

    /**
     * Récupère le monde d'une île ou le crée s'il n'existe pas
     */
    public World getOrCreateIslandWorld(Island island) {
        World world = getIslandWorld(island);
        if (world == null) {
            world = createIslandWorld(island);
        }
        return world;
    }
}