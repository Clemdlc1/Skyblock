package fr.skyblock.menus;

import fr.skyblock.CustomSkyblock;
import fr.skyblock.managers.MenuManager;
import fr.skyblock.models.Island;
import fr.skyblock.models.IslandWarp;
import fr.skyblock.models.SkyblockPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WarpMenu extends BaseMenu {

    private static final int ITEMS_PER_PAGE = 28; // 4 lignes de 7 items

    public WarpMenu(CustomSkyblock plugin, MenuManager menuManager) {
        super(plugin, menuManager);
    }

    @Override
    public void open(Player player) {
        openMainWarpMenu(player, 0);
    }

    @Override
    public String getMenuType() {
        return "warp";
    }

    public void openMainWarpMenu(Player player, int page) {
        Inventory inv = createInventory(54, ChatColor.DARK_BLUE + "Warps d'îles - Page " + (page + 1));

        // Obtenir tous les warps publics
        List<IslandWarp> promotedWarps = plugin.getWarpManager().getPromotedWarps();
        List<IslandWarp> popularWarps = plugin.getWarpManager().getPopularWarps();

        // Combiner les listes (promotions en premier)
        List<IslandWarp> allWarps = new ArrayList<>();
        allWarps.addAll(promotedWarps);
        allWarps.addAll(popularWarps);

        // Pagination
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allWarps.size());
        List<IslandWarp> pageWarps = allWarps.subList(startIndex, endIndex);

        // Afficher les warps
        int slot = 10;
        for (IslandWarp warp : pageWarps) {
            Island island = plugin.getDatabaseManager().loadIsland(warp.getIslandId());
            if (island == null) continue;

            String ownerName = getIslandOwnerName(island);
            boolean isPromoted = plugin.getWarpManager().isIslandPromoted(warp.getIslandId());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Propriétaire: " + ChatColor.WHITE + ownerName);
            lore.add(ChatColor.GRAY + "Description: " + ChatColor.WHITE + warp.getDescription());
            lore.add(ChatColor.GRAY + "Niveau île: " + ChatColor.WHITE + island.getLevel());
            lore.add(ChatColor.GRAY + "Visites: " + ChatColor.WHITE + warp.getVisits());

            if (isPromoted) {
                lore.add("");
                lore.add(ChatColor.GOLD + "⭐ ÎLE PROMUE ⭐");
            }

            lore.add("");
            lore.add(ChatColor.YELLOW + "Clic pour se téléporter");

            Material displayMaterial = isPromoted ? Material.DIAMOND : Material.ENDER_PEARL;
            String displayName = (isPromoted ? ChatColor.GOLD : ChatColor.GREEN) + warp.getName();

            ItemStack warpItem = createItem(displayMaterial, displayName, lore);
            inv.setItem(slot, warpItem);

            slot++;
            if (slot % 9 == 8) slot += 2; // Passer à la ligne suivante
            if (slot >= 44) break; // Limite d'affichage
        }

        // Boutons de navigation
        if (page > 0) {
            inv.setItem(45, createPreviousPageButton());
        }

        if (endIndex < allWarps.size()) {
            inv.setItem(53, createNextPageButton());
        }

        // Informations et actions
        inv.setItem(4, createItem(Material.COMPASS, ChatColor.AQUA + "Navigation des Warps",
                ChatColor.GRAY + "Total warps publics: " + ChatColor.WHITE + allWarps.size(),
                ChatColor.GRAY + "Warps promus: " + ChatColor.WHITE + promotedWarps.size(),
                "",
                ChatColor.YELLOW + "Explorez les îles du serveur !"));

        // Mes warps
        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
        if (skyblockPlayer != null && skyblockPlayer.hasIsland()) {
            inv.setItem(48, createItem(Material.BOOK, ChatColor.GREEN + "Mes Warps",
                    ChatColor.GRAY + "Gérer les warps de votre île",
                    "",
                    ChatColor.YELLOW + "Clic pour ouvrir"));
        }

        // Promotion
        inv.setItem(49, createItem(Material.BEACON, ChatColor.GOLD + "Promouvoir mon île",
                ChatColor.GRAY + "Placez votre île en haut",
                ChatColor.GRAY + "de la liste pendant 24h",
                ChatColor.GRAY + "Coût: " + ChatColor.AQUA + plugin.getWarpManager().calculatePromotionCost(1) + " beacons",
                "",
                ChatColor.YELLOW + "Clic pour promouvoir"));

        // Bouton retour
        inv.setItem(50, createBackButton());

        fillEmptySlots(inv, Material.GRAY_STAINED_GLASS_PANE);

        player.openInventory(inv);
        setPlayerMenu(player, "warp");
        setMenuData(player, "page", page);
        setMenuData(player, "warps", allWarps);
    }

    public void openPlayerWarps(Player player, String targetPlayerName) {
        Player target = Bukkit.getPlayer(targetPlayerName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Joueur introuvable !");
            return;
        }

        SkyblockPlayer targetPlayer = plugin.getDatabaseManager().loadPlayer(target.getUniqueId());
        if (targetPlayer == null || !targetPlayer.hasIsland()) {
            player.sendMessage(ChatColor.RED + "Ce joueur n'a pas d'île !");
            return;
        }

        Island targetIsland = plugin.getDatabaseManager().loadIsland(targetPlayer.getIslandId());
        if (targetIsland == null) {
            player.sendMessage(ChatColor.RED + "Impossible de charger l'île de ce joueur !");
            return;
        }

        List<IslandWarp> warps = plugin.getWarpManager().getIslandWarps(targetIsland.getId());

        // Filtrer les warps publics pour les non-membres
        if (!targetIsland.isMember(player.getUniqueId())) {
            warps = warps.stream().filter(IslandWarp::isPublic).collect(Collectors.toList());
        }

        if (warps.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Cette île n'a pas de warps publics !");
            return;
        }

        Inventory inv = createInventory(36, ChatColor.DARK_GREEN + "Warps de " + target.getName());

        // Afficher les warps
        int slot = 10;
        for (IslandWarp warp : warps) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Description: " + ChatColor.WHITE + warp.getDescription());
            lore.add(ChatColor.GRAY + "Visites: " + ChatColor.WHITE + warp.getVisits());
            lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + (warp.isPublic() ? "Public" : "Privé"));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Clic pour se téléporter");

            ItemStack warpItem = createItem(Material.ENDER_PEARL, ChatColor.GREEN + warp.getName(), lore);
            inv.setItem(slot, warpItem);

            slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= 26) break;
        }

        // Informations de l'île
        inv.setItem(4, createPlayerHead(target.getName(), ChatColor.AQUA + "Île de " + target.getName(),
                ChatColor.GRAY + "Niveau: " + ChatColor.WHITE + targetIsland.getLevel(),
                ChatColor.GRAY + "Taille: " + ChatColor.WHITE + targetIsland.getSize() + "x" + targetIsland.getSize(),
                ChatColor.GRAY + "Warps disponibles: " + ChatColor.WHITE + warps.size(),
                "",
                ChatColor.YELLOW + "Explorez cette île !"));

        // Bouton retour
        inv.setItem(31, createItem(Material.ARROW, ChatColor.YELLOW + "Retour aux warps",
                ChatColor.GRAY + "Retourner à la liste principale"));

        fillEmptySlots(inv, Material.GREEN_STAINED_GLASS_PANE);

        player.openInventory(inv);
        setPlayerMenu(player, "player_warps");
        setMenuData(player, "target_player", target.getName());
        setMenuData(player, "warps", warps);
    }

    public void openMyWarpsMenu(Player player) {
        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
        if (skyblockPlayer == null || !skyblockPlayer.hasIsland()) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas d'île !");
            return;
        }

        Island island = plugin.getDatabaseManager().loadIsland(skyblockPlayer.getIslandId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "Impossible de charger votre île !");
            return;
        }

        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Seul le propriétaire peut gérer les warps !");
            return;
        }

        List<IslandWarp> warps = plugin.getWarpManager().getIslandWarps(island.getId());
        int maxWarps = plugin.getWarpManager().getMaxWarpsForIsland(island);

        Inventory inv = createInventory(45, ChatColor.DARK_GREEN + "Mes Warps (" + warps.size() + "/" + maxWarps + ")");

        // Afficher les warps existants
        int slot = 10;
        for (IslandWarp warp : warps) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Description: " + ChatColor.WHITE + warp.getDescription());
            lore.add(ChatColor.GRAY + "Visites: " + ChatColor.WHITE + warp.getVisits());
            lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + (warp.isPublic() ? "Public" : "Privé"));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Clic gauche: Téléporter");
            lore.add(ChatColor.RED + "Clic droit: Supprimer");

            ItemStack warpItem = createItem(Material.ENDER_PEARL, ChatColor.GREEN + warp.getName(), lore);
            inv.setItem(slot, warpItem);

            slot++;
            if (slot % 9 == 7) slot += 3;
            if (slot >= 34) break;
        }

        // Bouton créer warp
        if (plugin.getWarpManager().canCreateWarp(island)) {
            inv.setItem(22, createItem(Material.EMERALD, ChatColor.GREEN + "Créer un nouveau warp",
                    ChatColor.GRAY + "Créez un warp à votre position",
                    ChatColor.GRAY + "actuelle sur votre île",
                    "",
                    ChatColor.WHITE + "Warps disponibles: " + ChatColor.GREEN + (maxWarps - warps.size()),
                    "",
                    ChatColor.YELLOW + "Clic pour créer"));
        } else {
            inv.setItem(22, createItem(Material.BARRIER, ChatColor.RED + "Limite de warps atteinte",
                    ChatColor.GRAY + "Vous avez atteint la limite",
                    ChatColor.GRAY + "de warps pour votre île",
                    "",
                    ChatColor.WHITE + "Améliorez votre île pour débloquer plus de warps:",
                    ChatColor.GRAY + "• Niveau 10: " + ChatColor.GREEN + "1 warp",
                    ChatColor.GRAY + "• Niveau 100: " + ChatColor.GREEN + "2 warps",
                    ChatColor.GRAY + "• Niveau 1000: " + ChatColor.GREEN + "3 warps",
                    ChatColor.GRAY + "• Permission VIP: " + ChatColor.GOLD + "+1 warp"));
        }

        // État de l'île
        boolean isOpen = plugin.getWarpManager().isIslandOpen(island);
        inv.setItem(20, createItem(isOpen ? Material.LIME_DYE : Material.RED_DYE,
                (isOpen ? ChatColor.GREEN + "Île ouverte" : ChatColor.RED + "Île fermée"),
                ChatColor.GRAY + "Les visiteurs " + (isOpen ? "peuvent" : "ne peuvent pas"),
                ChatColor.GRAY + "se téléporter sur votre île",
                "",
                ChatColor.YELLOW + "Clic pour " + (isOpen ? "fermer" : "ouvrir")));

        // Promotion
        boolean isPromoted = plugin.getWarpManager().isIslandPromoted(island.getId());
        inv.setItem(24, createItem(isPromoted ? Material.BEACON : Material.GRAY_DYE,
                isPromoted ? ChatColor.GOLD + "Île promue ⭐" : ChatColor.GRAY + "Promouvoir l'île",
                isPromoted ?
                        new String[]{ChatColor.GRAY + "Votre île est actuellement", ChatColor.GRAY + "en haut de la liste des warps"} :
                        new String[]{ChatColor.GRAY + "Placez votre île en haut", ChatColor.GRAY + "de la liste pendant 24h",
                                ChatColor.GRAY + "Coût: " + ChatColor.AQUA + plugin.getWarpManager().calculatePromotionCost(1) + " beacons",
                                "", ChatColor.YELLOW + "Clic pour promouvoir"}));

        // Bouton retour
        inv.setItem(40, createItem(Material.ARROW, ChatColor.YELLOW + "Retour aux warps",
                ChatColor.GRAY + "Retourner à la liste principale"));

        fillEmptySlots(inv, Material.BLUE_STAINED_GLASS_PANE);

        player.openInventory(inv);
        setPlayerMenu(player, "my_warps");
        setMenuData(player, "island", island);
        setMenuData(player, "warps", warps);
    }

    @Override
    public void handleClick(Player player, int slot) {
        int page = (Integer) getMenuData(player, "page");
        List<IslandWarp> warps = (List<IslandWarp>) getMenuData(player, "warps");

        switch (slot) {
            case 45 -> { // Page précédente
                if (page > 0) {
                    openMainWarpMenu(player, page - 1);
                }
            }
            case 53 -> { // Page suivante
                openMainWarpMenu(player, page + 1);
            }
            case 48 -> { // Mes warps
                openMyWarpsMenu(player);
            }
            case 49 -> { // Promouvoir île
                handlePromoteIsland(player);
            }
            case 50 -> { // Retour
                openMainMenu(player);
            }
            default -> {
                // Clic sur un warp
                if (slot >= 10 && slot < 44 && warps != null) {
                    int warpIndex = calculateWarpIndex(slot, page);
                    if (warpIndex < warps.size()) {
                        IslandWarp warp = warps.get(warpIndex);
                        player.closeInventory();
                        plugin.getWarpManager().teleportToWarp(player, warp.getId());
                    }
                }
            }
        }
    }

    public void handlePlayerWarpsClick(Player player, int slot) {
        switch (slot) {
            case 31 -> { // Retour
                openMainWarpMenu(player, 0);
            }
            default -> {
                // Clic sur un warp
                List<IslandWarp> warps = (List<IslandWarp>) getMenuData(player, "warps");
                if (slot >= 10 && slot < 26 && warps != null) {
                    int warpIndex = calculatePlayerWarpIndex(slot);
                    if (warpIndex < warps.size()) {
                        IslandWarp warp = warps.get(warpIndex);
                        player.closeInventory();
                        plugin.getWarpManager().teleportToWarp(player, warp.getId());
                    }
                }
            }
        }
    }

    public void handleMyWarpsClick(Player player, int slot) {
        Island island = (Island) getMenuData(player, "island");
        List<IslandWarp> warps = (List<IslandWarp>) getMenuData(player, "warps");

        switch (slot) {
            case 20 -> { // Ouvrir/Fermer île
                boolean isOpen = plugin.getWarpManager().isIslandOpen(island);
                plugin.getWarpManager().setIslandOpen(island, !isOpen);
                player.sendMessage((isOpen ? ChatColor.RED + "Île fermée" : ChatColor.GREEN + "Île ouverte") +
                        " aux visiteurs !");
                openMyWarpsMenu(player); // Rafraîchir
            }
            case 22 -> { // Créer warp
                if (plugin.getWarpManager().canCreateWarp(island)) {
                    player.closeInventory();
                    startWarpCreation(player);
                }
            }
            case 24 -> { // Promouvoir île
                handlePromoteIsland(player);
            }
            case 40 -> { // Retour
                openMainWarpMenu(player, 0);
            }
            default -> {
                // Clic sur un warp existant
                if (slot >= 10 && slot < 34 && warps != null) {
                    int warpIndex = calculateMyWarpIndex(slot);
                    if (warpIndex < warps.size()) {
                        IslandWarp warp = warps.get(warpIndex);
                        // TODO: Gérer clic gauche (téléporter) vs clic droit (supprimer)
                        player.closeInventory();
                        plugin.getWarpManager().teleportToWarp(player, warp.getId());
                    }
                }
            }
        }
    }

    // === MÉTHODES UTILITAIRES ===

    private void handlePromoteIsland(Player player) {
        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
        if (skyblockPlayer == null || !skyblockPlayer.hasIsland()) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas d'île !");
            return;
        }

        Island island = plugin.getDatabaseManager().loadIsland(skyblockPlayer.getIslandId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "Impossible de charger votre île !");
            return;
        }

        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Seul le propriétaire peut promouvoir l'île !");
            return;
        }

        if (plugin.getWarpManager().isIslandPromoted(island.getId())) {
            player.sendMessage(ChatColor.YELLOW + "Votre île est déjà promue !");
            return;
        }

        player.closeInventory();
        if (plugin.getWarpManager().promoteIsland(island.getId(), player, 1)) {
            plugin.getLogger().info(player.getName() + " a promu son île pour 1 jour");
        }
    }

    private void startWarpCreation(Player player) {
        // TODO: Implémenter la création de warp via conversation
        player.sendMessage(ChatColor.YELLOW + "Création de warp temporairement indisponible.");
        player.sendMessage(ChatColor.GRAY + "Utilisez /is setwarp <nom> <description> pour créer un warp.");
    }

    private int calculateWarpIndex(int slot, int page) {
        // Calcul basé sur la disposition: ligne 1 (slots 10-16), ligne 2 (19-25), etc.
        int row = (slot - 10) / 9;
        int col = (slot - 10) % 9;

        if (col > 6) return -1; // Hors limites

        return (page * ITEMS_PER_PAGE) + (row * 7) + col;
    }

    private int calculatePlayerWarpIndex(int slot) {
        // Disposition similaire mais pour menu plus petit
        if (slot < 10 || slot > 25) return -1;

        int row = (slot - 10) / 9;
        int col = (slot - 10) % 9;

        if (col > 6) return -1;

        return (row * 7) + col;
    }

    private int calculateMyWarpIndex(int slot) {
        // Disposition similaire pour mes warps
        if (slot < 10 || slot > 33) return -1;

        int row = (slot - 10) / 9;
        int col = (slot - 10) % 9;

        if (col > 5) return -1; // 6 items par ligne max

        return (row * 6) + col;
    }

    private String getIslandOwnerName(Island island) {
        Player owner = Bukkit.getPlayer(island.getOwner());
        if (owner != null) {
            return owner.getName();
        }

        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(island.getOwner());
        return skyblockPlayer != null ? skyblockPlayer.getName() : "Joueur inconnu";
    }
}