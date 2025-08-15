package fr.skyblock.managers;

import fr.skyblock.CustomSkyblock;
import fr.skyblock.models.Island;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Directional;
import org.bukkit.block.data.type.Hopper as HopperData; // alias not supported in Java; we'll handle below
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HopperTransferManager {

    private final CustomSkyblock plugin;
    private final Map<UUID, Set<Location>> islandIdToHoppers = new ConcurrentHashMap<>();

    public HopperTransferManager(CustomSkyblock plugin) {
        this.plugin = plugin;
        startScheduler();
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

    private void startScheduler() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    processAllIslands();
                } catch (Exception ignored) {
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // toutes les 20 ticks
    }

    private void processAllIslands() {
        for (Map.Entry<UUID, Set<Location>> entry : islandIdToHoppers.entrySet()) {
            UUID islandId = entry.getKey();
            Island island = plugin.getDatabaseManager().loadIsland(islandId);
            if (island == null) continue;

            int perHopperQuota = Math.max(1, island.getHopperTransferRate()); // items par 20 ticks

            for (Location loc : new ArrayList<>(entry.getValue())) {
                // Valider chunk et bloc
                World world = loc.getWorld();
                if (world == null) continue;
                if (!world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;

                Block block = world.getBlockAt(loc);
                if (block.getType() != org.bukkit.Material.HOPPER) {
                    unregisterHopper(loc);
                    continue;
                }

                // Inventaires
                Hopper hopperState = (Hopper) block.getState();
                Inventory hopperInv = hopperState.getInventory();

                Inventory aboveInv = getInventoryAbove(loc);
                Inventory destInv = getInventoryInFacing(block);

                int remaining = perHopperQuota;
                if (aboveInv != null && remaining > 0) {
                    remaining -= pullIntoHopper(aboveInv, hopperInv, remaining);
                }
                if (destInv != null && remaining > 0) {
                    remaining -= pushFromHopper(hopperInv, destInv, remaining);
                }
            }
        }
    }

    private Inventory getInventoryAbove(Location hopperLoc) {
        Location above = hopperLoc.clone().add(0, 1, 0);
        Block block = above.getBlock();
        if (!(block.getState() instanceof InventoryHolder holder)) return null;
        return holder.getInventory();
    }

    private Inventory getInventoryInFacing(Block hopperBlock) {
        BlockData data = hopperBlock.getBlockData();
        BlockFace face = BlockFace.DOWN;
        try {
            if (data instanceof org.bukkit.block.data.type.Hopper hd) {
                face = hd.getFacing();
            } else if (data instanceof Directional dir) {
                face = dir.getFacing();
            }
        } catch (Throwable ignored) {}

        Block target = hopperBlock.getRelative(face);
        if (!(target.getState() instanceof InventoryHolder holder)) return null;
        return holder.getInventory();
    }

    private int pullIntoHopper(Inventory from, Inventory hopperInv, int max) {
        int moved = 0;
        for (int slot = 0; slot < from.getSize() && moved < max; slot++) {
            ItemStack stack = from.getItem(slot);
            if (stack == null) continue;

            int allowed = Math.min(stack.getAmount(), max - moved);
            ItemStack toInsert = stack.clone();
            toInsert.setAmount(allowed);
            Map<Integer, ItemStack> leftover = hopperInv.addItem(toInsert);
            int notFit = 0;
            for (ItemStack l : leftover.values()) notFit += l.getAmount();
            int inserted = allowed - notFit;
            if (inserted <= 0) continue;

            // Retirer exactement ce qui a été inséré
            int newAmount = stack.getAmount() - inserted;
            if (newAmount <= 0) {
                from.clear(slot);
            } else {
                stack.setAmount(newAmount);
                from.setItem(slot, stack);
            }
            moved += inserted;
        }
        return moved;
    }

    private int pushFromHopper(Inventory hopperInv, Inventory dest, int max) {
        int moved = 0;
        for (int slot = 0; slot < hopperInv.getSize() && moved < max; slot++) {
            ItemStack stack = hopperInv.getItem(slot);
            if (stack == null) continue;
            int allowed = Math.min(stack.getAmount(), max - moved);
            ItemStack toInsert = stack.clone();
            toInsert.setAmount(allowed);
            Map<Integer, ItemStack> leftover = dest.addItem(toInsert);
            int notFit = 0;
            for (ItemStack l : leftover.values()) notFit += l.getAmount();
            int inserted = allowed - notFit;
            if (inserted <= 0) continue;

            // Retirer exactement ce qui a été inséré
            int newAmount = stack.getAmount() - inserted;
            if (newAmount <= 0) {
                hopperInv.clear(slot);
            } else {
                stack.setAmount(newAmount);
                hopperInv.setItem(slot, stack);
            }
            moved += inserted;
        }
        return moved;
    }
}

 