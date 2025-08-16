package fr.skyblock.listeners;

import fr.skyblock.CustomSkyblock;
import fr.skyblock.models.Island;
import fr.skyblock.models.MoneyPrinter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

public class PrinterListener implements Listener {

    private final CustomSkyblock plugin;

    public PrinterListener(CustomSkyblock plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.DROPPER) return;

        if (!plugin.getIslandManager().isIslandWorld(block.getWorld())) return;

        Player player = event.getPlayer();
        Island island = plugin.getIslandManager().getIslandAtLocation(block.getLocation());
        if (island == null) return;

        // Vérifier item imprimante
        if (!plugin.getPrinterManager().isPrinterItem(event.getItemInHand())) return;

        int tier = plugin.getPrinterManager().getPrinterTier(event.getItemInHand());
        if (tier <= 0) {
            player.sendMessage(ChatColor.RED + "Tier d'imprimante invalide");
            event.setCancelled(true);
            return;
        }

        if (!island.isMember(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Vous devez être membre de l'île pour poser une imprimante");
            event.setCancelled(true);
            return;
        }

        boolean ok = plugin.getPrinterManager().placePrinter(player, island, block, tier);
        if (!ok) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.DROPPER) return;
        if (!plugin.getIslandManager().isIslandWorld(block.getWorld())) return;

        Island island = plugin.getIslandManager().getIslandAtLocation(block.getLocation());
        if (island == null) return;

        MoneyPrinter printer = island.findPrinterAt(block.getX(), block.getY(), block.getZ());
        if (printer == null) return; // pas une imprimante, dropper normal

        Player player = event.getPlayer();
        if (!plugin.getPrinterManager().canBreakPrinter(player, island, printer)) {
            player.sendMessage(ChatColor.RED + "Vous ne pouvez pas retirer cette imprimante.");
            event.setCancelled(true);
            return;
        }

        // Éviter les drops par défaut du bloc
        event.setDropItems(false);

        if (!plugin.getPrinterManager().breakPrinter(player, island, block, printer)) {
            event.setCancelled(true);
            return;
        }

        // Supprimer le nametag associé
        plugin.getPrinterManager().removeNametag(island.getId(), block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.DROPPER) return;
        if (!plugin.getIslandManager().isIslandWorld(block.getWorld())) return;

        Island island = plugin.getIslandManager().getIslandAtLocation(block.getLocation());
        if (island == null) return;
        MoneyPrinter printer = island.findPrinterAt(block.getX(), block.getY(), block.getZ());
        if (printer == null) return;

        Player player = event.getPlayer();
        if (player.getUniqueId().equals(printer.getOwnerUuid())) {
            event.setCancelled(true);
            plugin.getMenuManager().openPrinterUpgradeMenu(player, printer);
            return;
        }

        // Sinon, afficher des infos
        event.setCancelled(true);
        long value = plugin.getConfig().getLong("printers." + printer.getTier() + ".value", printer.getTier() * 10L);
        player.sendMessage(ChatColor.GOLD + "=== Imprimante ===");
        player.sendMessage(ChatColor.YELLOW + "Tier: " + ChatColor.WHITE + printer.getTier());
        player.sendMessage(ChatColor.YELLOW + "Valeur du billet: " + ChatColor.WHITE + value + "$");
        player.sendMessage(ChatColor.YELLOW + "Propriétaire: " + ChatColor.WHITE + getPlayerName(printer.getOwnerUuid()));
    }

    private String getPlayerName(java.util.UUID uuid) {
        org.bukkit.entity.Player p = plugin.getServer().getPlayer(uuid);
        if (p != null) return p.getName();
        fr.skyblock.models.SkyblockPlayer sp = plugin.getDatabaseManager().loadPlayer(uuid);
        return sp != null ? sp.getName() : "Inconnu";
    }
}


