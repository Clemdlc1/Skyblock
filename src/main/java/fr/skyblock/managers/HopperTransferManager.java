package fr.skyblock.managers;

import fr.skyblock.CustomSkyblock;
import fr.skyblock.models.Island;
import org.bukkit.Location;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HopperTransferManager {

    private final CustomSkyblock plugin;
    private final Map<UUID, Set<Location>> islandIdToHoppers = new ConcurrentHashMap<>();

    public HopperTransferManager(CustomSkyblock plugin) {
        this.plugin = plugin;
    }

    public void registerHopper(Location location) {
        Island island = plugin.getIslandManager().getIslandAtLocation(location);
        if (island == null) return;
        islandIdToHoppers.computeIfAbsent(island.getId(), k -> ConcurrentHashMap.newKeySet()).add(location.clone());
    }

    public void unregisterHopper(Location location) {
        Island island = plugin.getIslandManager().getIslandAtLocation(location);
        if (island == null) return;
        Set<Location> set = islandIdToHoppers.get(island.getId());
        if (set != null) {
            set.remove(location);
            if (set.isEmpty()) {
                islandIdToHoppers.remove(island.getId());
            }
        }
    }
}
