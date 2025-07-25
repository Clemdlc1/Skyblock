package fr.skyblock.menus;

import fr.skyblock.CustomSkyblock;
import fr.skyblock.managers.MenuManager;
import fr.skyblock.models.Island;
import fr.skyblock.models.SkyblockPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;

public class UpgradeMenu extends BaseMenu {

    public UpgradeMenu(CustomSkyblock plugin, MenuManager menuManager) {
        super(plugin, menuManager);
    }

    @Override
    public void open(Player player) {
        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().getOrCreatePlayer(player.getUniqueId(), player.getName());
        if (!skyblockPlayer.hasIsland()) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas d'île à améliorer !");
            player.closeInventory();
            return;
        }

        Island island = plugin.getDatabaseManager().loadIsland(skyblockPlayer.getIslandId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "Erreur lors du chargement de votre île.");
            player.closeInventory();
            return;
        }

        Inventory inv = createInventory(36, ChatColor.DARK_AQUA + "Améliorations de l'Île");

        // Amélioration de la taille de l'île
        int currentSize = island.getSize();
        int nextSize = currentSize + 16;
        double sizeUpgradeCost = currentSize * 1000;
        inv.setItem(11, createItem(Material.GRASS_BLOCK, ChatColor.GREEN + "Agrandir l'Île",
                ChatColor.GRAY + "Taille actuelle: " + ChatColor.WHITE + currentSize + "x" + currentSize,
                ChatColor.GRAY + "Prochaine taille: " + ChatColor.WHITE + nextSize + "x" + nextSize,
                "",
                ChatColor.GRAY + "Coût: " + ChatColor.GOLD + String.format("%.2f $", sizeUpgradeCost),
                "",
                ChatColor.YELLOW + "Clic pour acheter"));


        // Amélioration du nombre de membres
        int currentMembers = island.getMaxMembers();
        int nextMembers = currentMembers + 2;
        double membersUpgradeCost = currentMembers * 500;
        inv.setItem(13, createItem(Material.PLAYER_HEAD, ChatColor.AQUA + "Plus de Membres",
                ChatColor.GRAY + "Membres max actuels: " + ChatColor.WHITE + currentMembers,
                ChatColor.GRAY + "Prochain max: " + ChatColor.WHITE + nextMembers,
                "",
                ChatColor.GRAY + "Coût: " + ChatColor.GOLD + String.format("%.2f $", membersUpgradeCost),
                "",
                ChatColor.YELLOW + "Clic pour acheter"));

        // Amélioration du nombre de warps
        int currentWarps = island.getMaxWarps();
        int nextWarps = currentWarps + 1;
        double warpsUpgradeCost = 1000 * nextWarps;
        inv.setItem(15, createItem(Material.COMPASS, ChatColor.LIGHT_PURPLE + "Plus de Warps",
                ChatColor.GRAY + "Warps max actuels: " + ChatColor.WHITE + currentWarps,
                ChatColor.GRAY + "Prochain max: " + ChatColor.WHITE + nextWarps,
                "",
                ChatColor.GRAY + "Coût: " + ChatColor.GOLD + String.format("%.2f $", warpsUpgradeCost),
                "",
                ChatColor.YELLOW + "Clic pour acheter"));


        // Retour et décoration
        inv.setItem(31, createBackButton());
        fillEmptySlots(inv, Material.BLACK_STAINED_GLASS_PANE);

        player.openInventory(inv);
        setPlayerMenu(player, getMenuType());
    }

    @Override
    public void handleClick(Player player, int slot) {
        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
        if (skyblockPlayer == null || !skyblockPlayer.hasIsland()) {
            player.closeInventory();
            return;
        }
        Island island = plugin.getDatabaseManager().loadIsland(skyblockPlayer.getIslandId());
        if (island == null) {
            player.closeInventory();
            return;
        }

        switch (slot) {
            case 11: // Agrandir l'île
                handleSizeUpgrade(player, island);
                break;
            case 13: // Plus de membres
                handleMembersUpgrade(player, island);
                break;
            case 15: // Plus de warps
                handleWarpsUpgrade(player, island);
                break;
            case 31: // Retour
                menuManager.openMainMenu(player);
                break;
        }
    }

    private void handleSizeUpgrade(Player player, Island island) {
        int currentSize = island.getSize();
        double cost = currentSize * 1000;

        if (plugin.getEconomyManager().hasEnough(player.getUniqueId(), cost)) {
            plugin.getEconomyManager().withdrawPlayer(player.getUniqueId(), cost);
            island.setSize(currentSize + 16);
            plugin.getDatabaseManager().saveIsland(island);
            player.sendMessage(ChatColor.GREEN + "Vous avez agrandi votre île ! Nouvelle taille : " + island.getSize() + "x" + island.getSize());
            open(player); // reopen menu
        } else {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas assez d'argent !");
        }
    }

    private void handleMembersUpgrade(Player player, Island island) {
        int currentMembers = island.getMaxMembers();
        double cost = currentMembers * 500;

        if (plugin.getEconomyManager().hasEnough(player.getUniqueId(), cost)) {
            plugin.getEconomyManager().withdrawPlayer(player.getUniqueId(), cost);
            island.setMaxMembers(currentMembers + 2);
            plugin.getDatabaseManager().saveIsland(island);
            player.sendMessage(ChatColor.GREEN + "Vous avez augmenté le nombre maximum de membres !");
            open(player); // reopen menu
        } else {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas assez d'argent !");
        }
    }

    private void handleWarpsUpgrade(Player player, Island island) {
        int currentWarps = island.getMaxWarps();
        int nextWarps = currentWarps + 1;
        double cost = 1000 * nextWarps;

        if (plugin.getEconomyManager().hasEnough(player.getUniqueId(), cost)) {
            plugin.getEconomyManager().withdrawPlayer(player.getUniqueId(), cost);
            island.setMaxWarps(nextWarps);
            plugin.getDatabaseManager().saveIsland(island);
            player.sendMessage(ChatColor.GREEN + "Vous avez augmenté le nombre maximum de warps !");
            open(player); // reopen menu
        } else {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas assez d'argent !");
        }
    }


    @Override
    public String getMenuType() {
        return "upgrade";
    }
}
