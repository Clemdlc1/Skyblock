package fr.skyblock.managers;

import fr.skyblock.CustomSkyblock;
import fr.skyblock.models.Island;
import fr.skyblock.models.SkyblockPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MenuManager implements Listener {

    private final CustomSkyblock plugin;
    private final Map<UUID, String> playerMenus = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Object>> menuData = new ConcurrentHashMap<>();

    public MenuManager(CustomSkyblock plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // === MENU PRINCIPAL ===

    public void openMainMenu(Player player) {
        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().getOrCreatePlayer(player.getUniqueId(), player.getName());

        Inventory inv = Bukkit.createInventory(null, 45, ChatColor.DARK_BLUE + "Menu Principal - Skyblock");

        // Informations de l'île
        if (skyblockPlayer.hasIsland()) {
            Island island = plugin.getDatabaseManager().loadIsland(skyblockPlayer.getIslandId());
            if (island != null) {
                inv.setItem(4, createItem(Material.GRASS_BLOCK, ChatColor.GREEN + "Votre Île",
                        ChatColor.GRAY + "Nom: " + ChatColor.WHITE + island.getName(),
                        ChatColor.GRAY + "Niveau: " + ChatColor.WHITE + island.getLevel(),
                        ChatColor.GRAY + "Taille: " + ChatColor.WHITE + island.getSize() + "x" + island.getSize(),
                        ChatColor.GRAY + "Banque: " + ChatColor.WHITE + String.format("%.2f $", island.getBank()),
                        "",
                        ChatColor.YELLOW + "Clic pour se téléporter"));
            }
        } else {
            inv.setItem(4, createItem(Material.BARRIER, ChatColor.RED + "Aucune île",
                    ChatColor.GRAY + "Vous n'avez pas encore d'île !",
                    "",
                    ChatColor.YELLOW + "Clic pour créer une île"));
        }

        // Navigation
        inv.setItem(10, createItem(Material.COMPASS, ChatColor.AQUA + "Téléportation",
                ChatColor.GRAY + "Se téléporter à votre île",
                ChatColor.GRAY + "ou à celle d'un ami",
                "",
                ChatColor.YELLOW + "Clic pour ouvrir"));

        inv.setItem(12, createItem(Material.PLAYER_HEAD, ChatColor.AQUA + "Gestion des Membres",
                ChatColor.GRAY + "Inviter, expulser ou gérer",
                ChatColor.GRAY + "les permissions des membres",
                "",
                ChatColor.YELLOW + "Clic pour ouvrir"));

        inv.setItem(14, createItem(Material.REDSTONE, ChatColor.AQUA + "Paramètres de l'île",
                ChatColor.GRAY + "Gérer les flags et",
                ChatColor.GRAY + "les permissions de votre île",
                "",
                ChatColor.YELLOW + "Clic pour ouvrir"));

        inv.setItem(16, createItem(Material.EMERALD, ChatColor.AQUA + "Banque de l'île",
                ChatColor.GRAY + "Gérer l'argent de votre île",
                ChatColor.GRAY + "Déposer ou retirer des fonds",
                "",
                ChatColor.YELLOW + "Clic pour ouvrir"));

        inv.setItem(19, createItem(Material.EXPERIENCE_BOTTLE, ChatColor.AQUA + "Niveau de l'île",
                ChatColor.GRAY + "Voir les détails du niveau",
                ChatColor.GRAY + "et les exigences pour progresser",
                "",
                ChatColor.YELLOW + "Clic pour ouvrir"));

        inv.setItem(21, createItem(Material.GOLDEN_APPLE, ChatColor.AQUA + "Améliorer l'île",
                ChatColor.GRAY + "Agrandir votre île",
                ChatColor.GRAY + "ou débloquer de nouvelles fonctionnalités",
                "",
                ChatColor.YELLOW + "Clic pour ouvrir"));

        inv.setItem(23, createItem(Material.PAPER, ChatColor.AQUA + "Classements",
                ChatColor.GRAY + "Voir le top des îles",
                ChatColor.GRAY + "par niveau et autres statistiques",
                "",
                ChatColor.YELLOW + "Clic pour ouvrir"));

        inv.setItem(25, createItem(Material.BOOK, ChatColor.AQUA + "Aide & Tutoriels",
                ChatColor.GRAY + "Apprendre à jouer",
                ChatColor.GRAY + "et découvrir les fonctionnalités",
                "",
                ChatColor.YELLOW + "Clic pour ouvrir"));

        // Boutons spéciaux
        if (skyblockPlayer.hasIsland()) {
            inv.setItem(40, createItem(Material.TNT, ChatColor.RED + "Supprimer l'île",
                    ChatColor.GRAY + "Attention ! Cette action",
                    ChatColor.GRAY + "est irréversible !",
                    "",
                    ChatColor.RED + "Clic pour supprimer"));
        } else {
            inv.setItem(40, createItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "Créer une île",
                    ChatColor.GRAY + "Commencer votre aventure",
                    ChatColor.GRAY + "sur votre propre île !",
                    "",
                    ChatColor.GREEN + "Clic pour créer"));
        }

        // Décoration
        fillEmptySlots(inv, Material.GRAY_STAINED_GLASS_PANE);

        player.openInventory(inv);
        playerMenus.put(player.getUniqueId(), "main");
    }

    // === MENU DES MEMBRES ===

    public void openMembersMenu(Player player) {
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

        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Gestion des Membres");

        // Boutons d'action (si propriétaire)
        if (island.getOwner().equals(player.getUniqueId())) {
            inv.setItem(4, createItem(Material.PLAYER_HEAD, ChatColor.GREEN + "Inviter un joueur",
                    ChatColor.GRAY + "Inviter un nouveau membre",
                    ChatColor.GRAY + "à rejoindre votre île",
                    "",
                    ChatColor.YELLOW + "Clic pour inviter"));
        }

        // Liste des membres
        int slot = 9;

        // Propriétaire
        Player owner = Bukkit.getPlayer(island.getOwner());
        if (owner != null) {
            ItemStack ownerHead = createPlayerHead(owner.getName(), ChatColor.GOLD + owner.getName() + " (Propriétaire)",
                    ChatColor.GRAY + "Statut: " + ChatColor.GREEN + "En ligne",
                    ChatColor.GRAY + "Rôle: " + ChatColor.GOLD + "Propriétaire",
                    "",
                    ChatColor.YELLOW + "Clic pour plus d'options");
            inv.setItem(slot++, ownerHead);
        }

        // Membres
        for (UUID memberUuid : island.getMembers()) {
            Player member = Bukkit.getPlayer(memberUuid);
            String memberName = member != null ? member.getName() : "Joueur inconnu";
            boolean isOnline = member != null && member.isOnline();

            ItemStack memberHead = createPlayerHead(memberName, ChatColor.AQUA + memberName,
                    ChatColor.GRAY + "Statut: " + (isOnline ? ChatColor.GREEN + "En ligne" : ChatColor.RED + "Hors ligne"),
                    ChatColor.GRAY + "Rôle: " + ChatColor.AQUA + "Membre",
                    "",
                    island.getOwner().equals(player.getUniqueId()) ? ChatColor.RED + "Clic pour expulser" : ChatColor.GRAY + "Membre de l'île");

            inv.setItem(slot++, memberHead);
            if (slot >= 44) break; // Limite d'affichage
        }

        // Bouton retour
        inv.setItem(49, createItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                ChatColor.GRAY + "Retourner au menu principal"));

        fillEmptySlots(inv, Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        player.openInventory(inv);
        playerMenus.put(player.getUniqueId(), "members");
        menuData.put(player.getUniqueId(), Map.of("island", island));
    }

    // === MENU DES FLAGS ===

    public void openFlagsMenu(Player player) {
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
            player.sendMessage(ChatColor.RED + "Seul le propriétaire peut modifier les paramètres !");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 45, ChatColor.DARK_RED + "Paramètres de l'île");

        int slot = 10;
        for (Island.IslandFlag flag : Island.IslandFlag.values()) {
            boolean enabled = island.getFlag(flag);
            Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
            String status = enabled ? ChatColor.GREEN + "Activé" : ChatColor.RED + "Désactivé";

            inv.setItem(slot, createItem(material, ChatColor.YELLOW + flag.getDescription(),
                    ChatColor.GRAY + "Statut: " + status,
                    "",
                    ChatColor.GRAY + "Description:",
                    ChatColor.WHITE + getDetailedFlagDescription(flag),
                    "",
                    ChatColor.YELLOW + "Clic pour " + (enabled ? "désactiver" : "activer")));

            slot++;
            if (slot % 9 == 8) slot += 2; // Saut de ligne avec espacement
        }

        // Bouton retour
        inv.setItem(40, createItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                ChatColor.GRAY + "Retourner au menu principal"));

        fillEmptySlots(inv, Material.RED_STAINED_GLASS_PANE);

        player.openInventory(inv);
        playerMenus.put(player.getUniqueId(), "flags");
        menuData.put(player.getUniqueId(), Map.of("island", island));
    }

    // === MENU DE LA BANQUE ===

    public void openBankMenu(Player player) {
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

        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "Banque de l'île");

        // Solde actuel
        inv.setItem(4, createItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "Solde de la banque",
                ChatColor.GRAY + "Solde actuel: " + ChatColor.WHITE + String.format("%.2f $", island.getBank()),
                "",
                ChatColor.GRAY + "La banque de l'île permet de",
                ChatColor.GRAY + "stocker l'argent commun de l'île"));

        // Boutons d'action
        inv.setItem(11, createItem(Material.GOLD_INGOT, ChatColor.YELLOW + "Déposer de l'argent",
                ChatColor.GRAY + "Déposer votre argent",
                ChatColor.GRAY + "dans la banque de l'île",
                "",
                ChatColor.YELLOW + "Clic pour déposer"));

        inv.setItem(15, createItem(Material.DIAMOND, ChatColor.AQUA + "Retirer de l'argent",
                ChatColor.GRAY + "Retirer de l'argent",
                ChatColor.GRAY + "de la banque de l'île",
                "",
                ChatColor.YELLOW + "Clic pour retirer"));

        // Historique des transactions (à implémenter)
        inv.setItem(13, createItem(Material.BOOK, ChatColor.BLUE + "Historique",
                ChatColor.GRAY + "Voir l'historique des",
                ChatColor.GRAY + "transactions de la banque",
                "",
                ChatColor.YELLOW + "Bientôt disponible"));

        // Bouton retour
        inv.setItem(22, createItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                ChatColor.GRAY + "Retourner au menu principal"));

        fillEmptySlots(inv, Material.GREEN_STAINED_GLASS_PANE);

        player.openInventory(inv);
        playerMenus.put(player.getUniqueId(), "bank");
        menuData.put(player.getUniqueId(), Map.of("island", island));
    }

    // === MENU D'AMÉLIORATION ===

    public void openUpgradeMenu(Player player) {
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

        Inventory inv = Bukkit.createInventory(null, 36, ChatColor.DARK_PURPLE + "Améliorer l'île");

        // Agrandissement avec beacons
        int nextSize = island.getSize() + 25;
        boolean canExpand = nextSize <= plugin.getMaxIslandSize();

        long expandCost = plugin.getPrisonTycoonHook().calculateExpandCost(island.getSize(), nextSize);
        boolean hasBeacons = plugin.getPrisonTycoonHook().hasBeacons(player.getUniqueId(), expandCost);
        canExpand = canExpand && hasBeacons;

        inv.setItem(11, createItem(canExpand ? Material.EMERALD : Material.BARRIER,
                ChatColor.GREEN + "Agrandir l'île",
                ChatColor.GRAY + "Taille actuelle: " + ChatColor.WHITE + island.getSize() + "x" + island.getSize(),
                ChatColor.GRAY + "Nouvelle taille: " + ChatColor.WHITE + nextSize + "x" + nextSize,
                ChatColor.GRAY + "Coût: " + ChatColor.AQUA + expandCost + " beacons",
                ChatColor.GRAY + "Vos beacons: " + ChatColor.WHITE + plugin.getPrisonTycoonHook().getBeacons(player.getUniqueId()),
                "",
                canExpand ? ChatColor.GREEN + "Clic pour agrandir" : ChatColor.RED + "Pas assez de beacons"));

        // Amélioration du niveau avec coins
        int nextLevel = island.getLevel() + 1;
        boolean canUpgradeLevel = true;

        long levelCost = plugin.getPrisonTycoonHook().calculateLevelUpgradeCost(island.getLevel(), nextLevel);
        boolean hasCoins = plugin.getPrisonTycoonHook().hasCoins(player.getUniqueId(), levelCost);
        canUpgradeLevel = hasCoins;

        inv.setItem(13, createItem(canUpgradeLevel ? Material.EXPERIENCE_BOTTLE : Material.BARRIER,
                ChatColor.BLUE + "Améliorer le niveau",
                ChatColor.GRAY + "Niveau actuel: " + ChatColor.WHITE + island.getLevel(),
                ChatColor.GRAY + "Nouveau niveau: " + ChatColor.WHITE + nextLevel,
                ChatColor.GRAY + "Coût: " + ChatColor.GOLD + levelCost + " coins",
                ChatColor.GRAY + "Vos coins: " + ChatColor.WHITE + plugin.getPrisonTycoonHook().getCoins(player.getUniqueId()),
                "",
                ChatColor.GRAY + "Augmente le prestige de votre île",
                "",
                canUpgradeLevel ? ChatColor.GREEN + "Clic pour améliorer" : ChatColor.RED + "Pas assez de coins"));

        // Économie PrisonTycoon
        inv.setItem(15, createItem(Material.GOLD_INGOT, ChatColor.GOLD + "Votre Économie",
                ChatColor.GRAY + "Coins: " + ChatColor.GOLD + plugin.getPrisonTycoonHook().getCoins(player.getUniqueId()),
                ChatColor.GRAY + "Tokens: " + ChatColor.LIGHT_PURPLE + plugin.getPrisonTycoonHook().getTokens(player.getUniqueId()),
                ChatColor.GRAY + "Beacons: " + ChatColor.AQUA + plugin.getPrisonTycoonHook().getBeacons(player.getUniqueId()),
                "",
                ChatColor.YELLOW + "Clic pour plus de détails"));

        // Bouton retour
        inv.setItem(31, createItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                ChatColor.GRAY + "Retourner au menu principal"));

        fillEmptySlots(inv, Material.PURPLE_STAINED_GLASS_PANE);

        player.openInventory(inv);
        playerMenus.put(player.getUniqueId(), "upgrade");
        menuData.put(player.getUniqueId(), Map.of("island", island));
    }

    // === MENU DE SÉLECTION DE SCHEMATIC ===

    public void openSchematicMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, ChatColor.DARK_BLUE + "Choisir un type d'île");

        List<String> schematics = plugin.getSchematicManager().getAvailableSchematics();

        int slot = 10;
        for (String schematic : schematics) {
            Map<String, Object> schematicData = plugin.getSchematicManager().getSchematicData(schematic);

            Material displayMaterial = Material.valueOf((String) schematicData.getOrDefault("material", "GRASS_BLOCK"));
            String displayName = (String) schematicData.getOrDefault("name", schematic);
            List<String> description = (List<String>) schematicData.getOrDefault("description", Arrays.asList("Île standard"));

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + displayName);
            lore.add("");
            for (String line : description) {
                lore.add(ChatColor.GRAY + line);
            }
            lore.add("");
            lore.add(ChatColor.YELLOW + "Clic pour sélectionner");

            inv.setItem(slot, createItem(displayMaterial, ChatColor.GREEN + displayName, lore.toArray(new String[0])));

            slot++;
            if (slot == 17) slot = 19; // Ligne suivante
            if (slot >= 26) break; // Limite
        }

        // Bouton annuler
        inv.setItem(31, createItem(Material.BARRIER, ChatColor.RED + "Annuler",
                ChatColor.GRAY + "Annuler la création d'île"));

        fillEmptySlots(inv, Material.BLUE_STAINED_GLASS_PANE);

        player.openInventory(inv);
        playerMenus.put(player.getUniqueId(), "schematic");
    }

    // === GESTIONNAIRE D'ÉVÉNEMENTS ===

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String menuType = playerMenus.get(player.getUniqueId());
        if (menuType == null) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (menuType) {
            case "main" -> handleMainMenuClick(player, event.getSlot(), clicked);
            case "members" -> handleMembersMenuClick(player, event.getSlot(), clicked);
            case "flags" -> handleFlagsMenuClick(player, event.getSlot(), clicked);
            case "bank" -> handleBankMenuClick(player, event.getSlot(), clicked);
            case "upgrade" -> handleUpgradeMenuClick(player, event.getSlot(), clicked);
            case "schematic" -> handleSchematicMenuClick(player, event.getSlot(), clicked);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            playerMenus.remove(player.getUniqueId());
            menuData.remove(player.getUniqueId());
        }
    }

    // === GESTIONNAIRES DE CLICS ===

    private void handleMainMenuClick(Player player, int slot, ItemStack clicked) {
        switch (slot) {
            case 4 -> { // Île principale
                SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
                if (skyblockPlayer != null && skyblockPlayer.hasIsland()) {
                    // Téléporter à l'île
                    player.closeInventory();
                    player.performCommand("island home");
                } else {
                    // Créer une île
                    player.closeInventory();
                    openSchematicMenu(player);
                }
            }
            case 10 -> { // Téléportation
                player.closeInventory();
                player.performCommand("island warp");
            }
            case 12 -> openMembersMenu(player); // Gestion des membres
            case 14 -> openFlagsMenu(player); // Paramètres
            case 16 -> openBankMenu(player); // Banque
            case 19 -> { // Niveau
                player.closeInventory();
                player.performCommand("island level");
            }
            case 21 -> openUpgradeMenu(player); // Améliorer
            case 23 -> { // Classements
                player.closeInventory();
                player.performCommand("island top");
            }
            case 25 -> { // Aide
                player.closeInventory();
                sendHelpMessage(player);
            }
            case 40 -> { // Créer/Supprimer île
                SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
                if (skyblockPlayer != null && skyblockPlayer.hasIsland()) {
                    player.closeInventory();
                    player.performCommand("island delete");
                } else {
                    player.closeInventory();
                    openSchematicMenu(player);
                }
            }
        }
    }

    private void handleMembersMenuClick(Player player, int slot, ItemStack clicked) {
        if (slot == 49) { // Retour
            openMainMenu(player);
            return;
        }

        if (slot == 4) { // Inviter
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Utilisez " + ChatColor.GOLD + "/island invite <joueur>" + ChatColor.YELLOW + " pour inviter un joueur !");
            return;
        }

        // Gestion des membres (expulsion, etc.)
        Island island = (Island) menuData.get(player.getUniqueId()).get("island");
        if (island.getOwner().equals(player.getUniqueId()) && slot >= 9 && slot < 44) {
            // Logic pour expulser un membre
            player.sendMessage(ChatColor.YELLOW + "Fonctionnalité d'expulsion en cours de développement...");
        }
    }

    private void handleFlagsMenuClick(Player player, int slot, ItemStack clicked) {
        if (slot == 40) { // Retour
            openMainMenu(player);
            return;
        }

        Island island = (Island) menuData.get(player.getUniqueId()).get("island");

        // Déterminer quel flag a été cliqué
        Island.IslandFlag[] flags = Island.IslandFlag.values();
        int flagIndex = -1;

        if (slot >= 10 && slot <= 16) {
            flagIndex = slot - 10;
        } else if (slot >= 19 && slot <= 25) {
            flagIndex = slot - 19 + 7;
        }

        if (flagIndex >= 0 && flagIndex < flags.length) {
            Island.IslandFlag flag = flags[flagIndex];
            boolean newValue = !island.getFlag(flag);
            island.setFlag(flag, newValue);
            plugin.getDatabaseManager().saveIsland(island);

            player.sendMessage(ChatColor.GREEN + flag.getDescription() + " " +
                    (newValue ? "activé" : "désactivé") + " !");

            // Rafraîchir le menu
            openFlagsMenu(player);
        }
    }

    private void handleBankMenuClick(Player player, int slot, ItemStack clicked) {
        if (slot == 22) { // Retour
            openMainMenu(player);
            return;
        }

        Island island = (Island) menuData.get(player.getUniqueId()).get("island");

        switch (slot) {
            case 11 -> { // Déposer
                player.closeInventory();
                plugin.getEconomyManager().startBankDeposit(player, island);
            }
            case 15 -> { // Retirer
                player.closeInventory();
                plugin.getEconomyManager().startBankWithdrawal(player, island);
            }
        }
    }

    private void handleUpgradeMenuClick(Player player, int slot, ItemStack clicked) {
        if (slot == 31) { // Retour
            openMainMenu(player);
            return;
        }

        Island island = (Island) menuData.get(player.getUniqueId()).get("island");

        switch (slot) {
            case 11 -> { // Agrandir
                int nextSize = island.getSize() + 25;

                if (plugin.getIslandManager().expandIsland(island, nextSize)) {
                    player.sendMessage(ChatColor.GREEN + "Île agrandie avec succès !");
                    openUpgradeMenu(player); // Rafraîchir
                } else {
                    long cost = plugin.getPrisonTycoonHook().calculateExpandCost(island.getSize(), nextSize);
                    player.sendMessage(ChatColor.RED + "Impossible d'agrandir l'île !");
                    player.sendMessage(ChatColor.GRAY + "Beacons requis: " + ChatColor.AQUA + cost);
                }
            }
            case 13 -> { // Améliorer niveau
                int nextLevel = island.getLevel() + 1;

                if (plugin.getIslandManager().upgradeLevelIsland(island, nextLevel)) {
                    player.sendMessage(ChatColor.GREEN + "Niveau de l'île amélioré !");
                    openUpgradeMenu(player); // Rafraîchir
                } else {
                    long cost = plugin.getPrisonTycoonHook().calculateLevelUpgradeCost(island.getLevel(), nextLevel);
                    player.sendMessage(ChatColor.RED + "Impossible d'améliorer le niveau !");
                    player.sendMessage(ChatColor.GRAY + "Coins requis: " + ChatColor.GOLD + cost);
                }
            }
            case 15 -> { // Économie
                player.closeInventory();
                plugin.getPrisonTycoonHook().showPlayerEconomy(player);
            }
        }
    }

    private void handleSchematicMenuClick(Player player, int slot, ItemStack clicked) {
        if (slot == 31) { // Annuler
            player.closeInventory();
            return;
        }

        List<String> schematics = plugin.getSchematicManager().getAvailableSchematics();

        // Déterminer quel schematic a été cliqué
        int schematicIndex = -1;
        if (slot >= 10 && slot <= 16) {
            schematicIndex = slot - 10;
        } else if (slot >= 19 && slot <= 25) {
            schematicIndex = slot - 19 + 7;
        }

        if (schematicIndex >= 0 && schematicIndex < schematics.size()) {
            String selectedSchematic = schematics.get(schematicIndex);
            player.closeInventory();

            player.sendMessage(ChatColor.YELLOW + "Création de votre île en cours...");
            plugin.getSchematicManager().createIslandWithSchematic(player, selectedSchematic);
        }
    }

    // === MÉTHODES UTILITAIRES ===

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPlayerHead(String playerName, String displayName, String... lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwner(playerName);
            meta.setDisplayName(displayName);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            head.setItemMeta(meta);
        }
        return head;
    }

    private void fillEmptySlots(Inventory inv, Material material) {
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
    }

    private String getDetailedFlagDescription(Island.IslandFlag flag) {
        return switch (flag) {
            case PVP -> "Permet aux joueurs de s'attaquer entre eux sur l'île";
            case MOB_SPAWNING -> "Autorise l'apparition des monstres hostiles";
            case ANIMAL_SPAWNING -> "Autorise l'apparition des animaux pacifiques";
            case FIRE_SPREAD -> "Permet au feu de se propager naturellement";
            case EXPLOSION_DAMAGE -> "Autorise les dégâts d'explosion (TNT, Creepers)";
            case VISITOR_INTERACT -> "Permet aux visiteurs d'interagir avec les objets";
            case VISITOR_PLACE -> "Permet aux visiteurs de placer des blocs";
            case VISITOR_BREAK -> "Permet aux visiteurs de casser des blocs";
            case VISITOR_CHEST -> "Permet aux visiteurs d'ouvrir les coffres";
        };
    }

    private double calculateExpandCost(int currentSize, int newSize) {
        // Coût basé sur la nouvelle taille
        return Math.pow(newSize / 50.0, 2) * 1000;
    }

    private double calculateLevelCost(int currentLevel) {
        // Coût exponentiel pour les niveaux
        return Math.pow(currentLevel + 1, 1.5) * 500;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "Guide Skyblock" + ChatColor.GOLD + " ===");
        player.sendMessage(ChatColor.AQUA + "1. " + ChatColor.WHITE + "Créez votre île avec /island create");
        player.sendMessage(ChatColor.AQUA + "2. " + ChatColor.WHITE + "Utilisez /island menu pour accéder aux options");
        player.sendMessage(ChatColor.AQUA + "3. " + ChatColor.WHITE + "Invitez des amis avec /island invite <joueur>");
        player.sendMessage(ChatColor.AQUA + "4. " + ChatColor.WHITE + "Gérez les permissions avec les flags");
        player.sendMessage(ChatColor.AQUA + "5. " + ChatColor.WHITE + "Améliorez votre île pour débloquer de nouvelles fonctionnalités");
        player.sendMessage(ChatColor.GOLD + "Amusez-vous bien sur votre île !");
    }
}