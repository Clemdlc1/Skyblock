package fr.skyblock.menus;

import fr.skyblock.CustomSkyblock;
import fr.skyblock.managers.MenuManager;
import fr.skyblock.models.Island;
import fr.skyblock.models.MoneyPrinter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public class PrinterUpgradeMenu extends BaseMenu {

    public PrinterUpgradeMenu(CustomSkyblock plugin, MenuManager menuManager) {
        super(plugin, menuManager);
    }

    @Override
    public void open(Player player) {
        // Ce menu s'ouvre pour une imprimante précise via setMenuData("printer", MoneyPrinter)
        MoneyPrinter printer = (MoneyPrinter) getMenuData(player, "printer");
        if (printer == null) {
            player.sendMessage(ChatColor.RED + "Aucune imprimante associée.");
            return;
        }
        Inventory inv = Bukkit.createInventory(player, InventoryType.DROPPER, ChatColor.DARK_AQUA + "Upgrade Imprimante");

        long nextTier = printer.getTier() + 1L;
        long cost = plugin.getConfig().getLong("printers." + nextTier + ".upgrade-cost", nextTier * 200);

        inv.setItem(4, createItem(Material.NETHER_STAR, ChatColor.GOLD + "Améliorer vers T" + nextTier,
                ChatColor.GRAY + "Coût: " + ChatColor.YELLOW + cost + " coins"));

        inv.setItem(8, createBackButton());

        fillEmptySlots(inv, Material.CYAN_STAINED_GLASS_PANE);

        player.openInventory(inv);
        setPlayerMenu(player, getMenuType());
    }

    @Override
    public void handleClick(Player player, int slot) {
        MoneyPrinter printer = (MoneyPrinter) getMenuData(player, "printer");
        if (printer == null) return;

        if (slot == 8) {
            player.closeInventory();
            return;
        }

        if (slot == 4) {
            long nextTier = printer.getTier() + 1L;
            long cost = plugin.getConfig().getLong("printers." + nextTier + ".upgrade-cost", nextTier * 200);

            // Vérifier argent (coins) via PrisonTycoonHook
            if (!plugin.getPrisonTycoonHook().hasCoins(player.getUniqueId(), cost)) {
                player.sendMessage(ChatColor.RED + "Coins insuffisants.");
                return;
            }

            // Charger l'île et vérifier ownership
            Island island = plugin.getIslandManager().getIslandAtLocation(player.getLocation());
            if (island == null || !player.getUniqueId().equals(printer.getOwnerUuid())) {
                player.sendMessage(ChatColor.RED + "Action non autorisée.");
                return;
            }

            // Débiter et up tier
            if (!plugin.getPrisonTycoonHook().removeCoins(player.getUniqueId(), cost)) {
                player.sendMessage(ChatColor.RED + "Paiement échoué.");
                return;
            }

            printer.setTier((int) nextTier);
            plugin.getDatabaseManager().saveIsland(island);
            player.sendMessage(ChatColor.GREEN + "Imprimante améliorée vers Tier " + nextTier + " !");
            plugin.getPrinterManager().maybeShowNametag(island, plugin.getIslandManager().getIslandWorld(island), printer);
            open(player);
        }
    }

    @Override
    public String getMenuType() {
        return "printer_upgrade";
    }
}


