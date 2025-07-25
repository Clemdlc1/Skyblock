package fr.skyblock.commands;

import fr.skyblock.CustomSkyblock;
import fr.skyblock.models.Island;
import fr.skyblock.models.SkyblockPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class IslandCommand implements CommandExecutor, TabCompleter {

    private final CustomSkyblock plugin;

    public IslandCommand(CustomSkyblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande ne peut être utilisée que par un joueur !");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create", "c" -> handleCreate(player);
            case "home", "h" -> handleHome(player);
            case "delete", "reset" -> handleDelete(player);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player, args);
            case "deny" -> handleDeny(player, args);
            case "kick" -> handleKick(player, args);
            case "leave" -> handleLeave(player);
            case "info", "i" -> handleInfo(player, args);
            case "menu", "m" -> handleMenu(player);
            case "warp", "w" -> handleWarp(player, args);
            case "sethome" -> handleSetHome(player);
            case "bank", "b" -> handleBank(player, args);
            case "level", "l" -> handleLevel(player);
            case "top" -> handleTop(player);
            case "expand" -> handleExpand(player, args);
            case "flags", "settings" -> handleFlags(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "Commandes Île" + ChatColor.GOLD + " ===");
        player.sendMessage(ChatColor.AQUA + "/is create" + ChatColor.WHITE + " - Créer une île");
        player.sendMessage(ChatColor.AQUA + "/is home" + ChatColor.WHITE + " - Retourner à votre île");
        player.sendMessage(ChatColor.AQUA + "/is info [joueur]" + ChatColor.WHITE + " - Informations sur une île");
        player.sendMessage(ChatColor.AQUA + "/is menu" + ChatColor.WHITE + " - Ouvrir le menu principal");
        player.sendMessage(ChatColor.AQUA + "/is invite <joueur>" + ChatColor.WHITE + " - Inviter un joueur");
        player.sendMessage(ChatColor.AQUA + "/is accept <joueur>" + ChatColor.WHITE + " - Accepter une invitation");
        player.sendMessage(ChatColor.AQUA + "/is kick <joueur>" + ChatColor.WHITE + " - Expulser un membre");
        player.sendMessage(ChatColor.AQUA + "/is leave" + ChatColor.WHITE + " - Quitter une île");
        player.sendMessage(ChatColor.AQUA + "/is warp <joueur>" + ChatColor.WHITE + " - Se téléporter à une île");
        player.sendMessage(ChatColor.AQUA + "/is bank [deposit|withdraw] [montant]" + ChatColor.WHITE + " - Gérer la banque");
        player.sendMessage(ChatColor.AQUA + "/is level" + ChatColor.WHITE + " - Voir le niveau de l'île");
        player.sendMessage(ChatColor.AQUA + "/is top" + ChatColor.WHITE + " - Classement des îles");
        player.sendMessage(ChatColor.AQUA + "/is expand <taille>" + ChatColor.WHITE + " - Agrandir l'île");
        player.sendMessage(ChatColor.AQUA + "/is flags" + ChatColor.WHITE + " - Gérer les paramètres");
        player.sendMessage(ChatColor.AQUA + "/is delete" + ChatColor.WHITE + " - Supprimer votre île");
    }

    private void handleCreate(Player player) {
        if (!plugin.getPrisonTycoonHook().canCreateIsland(player)) {
            player.sendMessage(ChatColor.RED + "Vous devez débloquer Free pour créer une île");
            return;
        }
        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().getOrCreatePlayer(player.getUniqueId(), player.getName());

        if (skyblockPlayer.hasIsland()) {
            player.sendMessage(ChatColor.RED + "Vous avez déjà une île ! Utilisez /is delete pour la supprimer.");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Création de votre île en cours...");

        Island island = plugin.getIslandManager().createIsland(player);
        if (island != null) {
            player.sendMessage(ChatColor.GREEN + "Votre île a été créée avec succès !");
            player.sendMessage(ChatColor.GOLD + "Utilisez /is menu pour accéder aux options de votre île.");
        } else {
            player.sendMessage(ChatColor.RED + "Erreur lors de la création de l'île. Contactez un administrateur.");
        }
    }

    private void handleHome(Player player) {
        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
        if (skyblockPlayer == null || !skyblockPlayer.hasIsland()) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas d'île ! Utilisez /is create pour en créer une.");
            return;
        }

        Island island = plugin.getDatabaseManager().loadIsland(skyblockPlayer.getIslandId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "Impossible de trouver votre île !");
            return;
        }

        if (plugin.getIslandManager().teleportToIsland(player, island)) {
            player.sendMessage(ChatColor.GREEN + "Bienvenue sur votre île !");
        } else {
            player.sendMessage(ChatColor.RED + "Erreur lors de la téléportation !");
        }
    }

    private void handleDelete(Player player) {
        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
        if (skyblockPlayer == null || !skyblockPlayer.hasIsland()) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas d'île à supprimer !");
            return;
        }

        Island island = plugin.getDatabaseManager().loadIsland(skyblockPlayer.getIslandId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "Impossible de trouver votre île !");
            return;
        }

        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Vous n'êtes pas le propriétaire de cette île !");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Suppression de votre île en cours...");

        if (plugin.getIslandManager().deleteIsland(island)) {
            player.sendMessage(ChatColor.GREEN + "Votre île a été supprimée avec succès !");
            player.sendMessage(ChatColor.GOLD + "Vous pouvez créer une nouvelle île avec /is create.");
        } else {
            player.sendMessage(ChatColor.RED + "Erreur lors de la suppression de l'île !");
        }
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /is invite <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Joueur introuvable !");
            return;
        }

        plugin.getInvitationManager().sendInvitation(player, target);
    }

    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            // Afficher la liste des invitations
            List<String> invitations = plugin.getInvitationManager().getPendingInvitationNames(player.getUniqueId());
            if (invitations.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Vous n'avez aucune invitation en attente !");
                return;
            }

            player.sendMessage(ChatColor.YELLOW + "Invitations en attente:");
            for (String inviterName : invitations) {
                player.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + inviterName);
            }
            player.sendMessage(ChatColor.GREEN + "Utilisez /is accept <joueur> pour accepter");
            return;
        }

        plugin.getInvitationManager().acceptInvitation(player, args[1]);
    }

    private void handleDeny(Player player, String[] args) {
        if (args.length < 2) {
            // Afficher la liste des invitations
            List<String> invitations = plugin.getInvitationManager().getPendingInvitationNames(player.getUniqueId());
            if (invitations.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Vous n'avez aucune invitation en attente !");
                return;
            }

            player.sendMessage(ChatColor.YELLOW + "Invitations en attente:");
            for (String inviterName : invitations) {
                player.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + inviterName);
            }
            player.sendMessage(ChatColor.RED + "Utilisez /is deny <joueur> pour refuser");
            return;
        }

        plugin.getInvitationManager().denyInvitation(player, args[1]);
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /is kick <joueur>");
            return;
        }

        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
        if (skyblockPlayer == null || !skyblockPlayer.hasIsland()) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas d'île !");
            return;
        }

        Island island = plugin.getDatabaseManager().loadIsland(skyblockPlayer.getIslandId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "Impossible de trouver votre île !");
            return;
        }

        if (!island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Seul le propriétaire peut expulser des membres !");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUuid;
        String targetName;

        if (target != null) {
            targetUuid = target.getUniqueId();
            targetName = target.getName();
        } else {
            // Chercher par nom dans les membres
            targetUuid = findMemberByName(island, args[1]);
            if (targetUuid == null) {
                player.sendMessage(ChatColor.RED + "Membre introuvable: " + args[1]);
                return;
            }
            targetName = args[1];
        }

        if (plugin.getIslandManager().removeMember(island, targetUuid)) {
            player.sendMessage(ChatColor.GREEN + targetName + " a été expulsé de l'île !");

            if (target != null && target.isOnline()) {
                target.sendMessage(ChatColor.RED + "Vous avez été expulsé de l'île de " + player.getName() + " !");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Impossible d'expulser ce joueur !");
        }
    }

    private void handleLeave(Player player) {
        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
        if (skyblockPlayer == null) {
            player.sendMessage(ChatColor.RED + "Aucune donnée trouvée !");
            return;
        }

        // Trouver une île dont le joueur est membre (mais pas propriétaire)
        UUID islandToLeave = null;
        for (UUID islandId : skyblockPlayer.getMemberOfIslands()) {
            Island island = plugin.getDatabaseManager().loadIsland(islandId);
            if (island != null && !island.getOwner().equals(player.getUniqueId())) {
                islandToLeave = islandId;
                break;
            }
        }

        if (islandToLeave == null) {
            player.sendMessage(ChatColor.RED + "Vous n'êtes membre d'aucune île que vous pouvez quitter !");
            return;
        }

        Island island = plugin.getDatabaseManager().loadIsland(islandToLeave);
        if (plugin.getIslandManager().removeMember(island, player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Vous avez quitté l'île de " +
                    getPlayerName(island.getOwner()) + " !");

            // Notifier le propriétaire
            Player owner = Bukkit.getPlayer(island.getOwner());
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(ChatColor.YELLOW + player.getName() + " a quitté votre île.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Erreur lors de la sortie de l'île !");
        }
    }

    private void handleInfo(Player player, String[] args) {
        Island island;

        if (args.length > 1) {
            // Info sur l'île d'un autre joueur
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Joueur introuvable !");
                return;
            }

            SkyblockPlayer targetPlayer = plugin.getDatabaseManager().loadPlayer(target.getUniqueId());
            if (targetPlayer == null || !targetPlayer.hasIsland()) {
                player.sendMessage(ChatColor.RED + "Ce joueur n'a pas d'île !");
                return;
            }

            island = plugin.getDatabaseManager().loadIsland(targetPlayer.getIslandId());
        } else {
            // Info sur sa propre île
            SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
            if (skyblockPlayer == null || !skyblockPlayer.hasIsland()) {
                player.sendMessage(ChatColor.RED + "Vous n'avez pas d'île !");
                return;
            }

            island = plugin.getDatabaseManager().loadIsland(skyblockPlayer.getIslandId());
        }

        if (island == null) {
            player.sendMessage(ChatColor.RED + "Impossible de trouver l'île !");
            return;
        }

        showIslandInfo(player, island);
    }

    private void showIslandInfo(Player player, Island island) {
        Player owner = Bukkit.getPlayer(island.getOwner());
        String ownerName = owner != null ? owner.getName() : "Inconnu";

        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "Informations de l'île" + ChatColor.GOLD + " ===");
        player.sendMessage(ChatColor.AQUA + "Nom: " + ChatColor.WHITE + island.getName());
        player.sendMessage(ChatColor.AQUA + "Propriétaire: " + ChatColor.WHITE + ownerName);
        player.sendMessage(ChatColor.AQUA + "Niveau: " + ChatColor.WHITE + island.getLevel());
        player.sendMessage(ChatColor.AQUA + "Taille: " + ChatColor.WHITE + island.getSize() + "x" + island.getSize());
        player.sendMessage(ChatColor.AQUA + "Banque: " + ChatColor.WHITE + String.format("%.2f", island.getBank()) + " $");
        player.sendMessage(ChatColor.AQUA + "Membres: " + ChatColor.WHITE + (island.getMembers().size() + 1)); // +1 pour le propriétaire

        long daysSinceCreation = (System.currentTimeMillis() - island.getCreationTime()) / (24 * 60 * 60 * 1000);
        player.sendMessage(ChatColor.AQUA + "Créée il y a: " + ChatColor.WHITE + daysSinceCreation + " jour(s)");
    }

    private void handleMenu(Player player) {
        plugin.getMenuManager().openMainMenu(player);
    }

    private void handleWarp(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /is warp <joueur>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
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
            player.sendMessage(ChatColor.RED + "Impossible de trouver l'île de ce joueur !");
            return;
        }

        if (plugin.getIslandManager().teleportToIsland(player, targetIsland)) {
            player.sendMessage(ChatColor.GREEN + "Vous avez été téléporté à l'île de " + target.getName() + " !");
            targetIsland.addVisitor(player.getUniqueId());
            plugin.getDatabaseManager().saveIsland(targetIsland);
        } else {
            player.sendMessage(ChatColor.RED + "Erreur lors de la téléportation !");
        }
    }

    private void handleSetHome(Player player) {
        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
        if (skyblockPlayer == null || !skyblockPlayer.hasIsland()) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas d'île !");
            return;
        }

        Island island = plugin.getDatabaseManager().loadIsland(skyblockPlayer.getIslandId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "Impossible de trouver votre île !");
            return;
        }

        // Vérifier que le joueur est sur son île
        if (!plugin.getIslandManager().isPlayerOnIsland(player, island)) {
            player.sendMessage(ChatColor.RED + "Vous devez être sur votre île pour définir le home !");
            return;
        }

        // Stocker la position comme données personnalisées
        skyblockPlayer.setData("home_x", player.getLocation().getX());
        skyblockPlayer.setData("home_y", player.getLocation().getY());
        skyblockPlayer.setData("home_z", player.getLocation().getZ());
        skyblockPlayer.setData("home_yaw", (double) player.getLocation().getYaw());
        skyblockPlayer.setData("home_pitch", (double) player.getLocation().getPitch());
        plugin.getDatabaseManager().savePlayer(skyblockPlayer);

        player.sendMessage(ChatColor.GREEN + "Point de home défini à votre position actuelle !");
    }

    private void handleBank(Player player, String[] args) {
        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
        if (skyblockPlayer == null || !skyblockPlayer.hasIsland()) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas d'île !");
            return;
        }

        Island island = plugin.getDatabaseManager().loadIsland(skyblockPlayer.getIslandId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "Impossible de trouver votre île !");
            return;
        }

        if (args.length == 1) {
            // Afficher le solde
            player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "Banque de l'île" + ChatColor.GOLD + " ===");
            player.sendMessage(ChatColor.AQUA + "Solde: " + ChatColor.WHITE +
                    plugin.getEconomyManager().formatMoney(island.getBank()));
            player.sendMessage(ChatColor.GRAY + "Utilisez /is bank deposit/withdraw pour gérer les fonds");
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "deposit", "d" -> {
                if (args.length >= 3) {
                    try {
                        double amount = Double.parseDouble(args[2]);
                        if (amount <= 0) {
                            player.sendMessage(ChatColor.RED + "Le montant doit être positif !");
                            return;
                        }

                        if (plugin.getEconomyManager().hasBalance(player.getUniqueId(), amount)) {
                            plugin.getEconomyManager().removeBalance(player.getUniqueId(), amount);
                            island.addToBank(amount);
                            plugin.getDatabaseManager().saveIsland(island);

                            player.sendMessage(ChatColor.GREEN + "Vous avez déposé " +
                                    plugin.getEconomyManager().formatMoney(amount) +
                                    " dans la banque de l'île !");
                        } else {
                            player.sendMessage(ChatColor.RED + "Fonds insuffisants !");
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Montant invalide !");
                    }
                } else {
                    plugin.getEconomyManager().startBankDeposit(player, island);
                }
            }
            case "withdraw", "w" -> {
                if (args.length >= 3) {
                    try {
                        double amount = Double.parseDouble(args[2]);
                        if (amount <= 0) {
                            player.sendMessage(ChatColor.RED + "Le montant doit être positif !");
                            return;
                        }

                        if (island.removeFromBank(amount)) {
                            plugin.getEconomyManager().addBalance(player.getUniqueId(), amount);
                            plugin.getDatabaseManager().saveIsland(island);

                            player.sendMessage(ChatColor.GREEN + "Vous avez retiré " +
                                    plugin.getEconomyManager().formatMoney(amount) +
                                    " de la banque de l'île !");
                        } else {
                            player.sendMessage(ChatColor.RED + "Fonds insuffisants dans la banque !");
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Montant invalide !");
                    }
                } else {
                    plugin.getEconomyManager().startBankWithdrawal(player, island);
                }
            }
            default -> {
                player.sendMessage(ChatColor.RED + "Usage: /is bank [deposit|withdraw] [montant]");
            }
        }
    }

    private void handleLevel(Player player) {
        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
        if (skyblockPlayer == null || !skyblockPlayer.hasIsland()) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas d'île !");
            return;
        }

        Island island = plugin.getDatabaseManager().loadIsland(skyblockPlayer.getIslandId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "Impossible de trouver votre île !");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "Niveau de votre île: " + ChatColor.WHITE + island.getLevel());
        // TODO: Ajouter plus de détails sur le calcul du niveau
    }

    private void handleTop(Player player) {
        List<Island> topIslands = plugin.getDatabaseManager().getTopIslandsByLevel(10);

        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "Top 10 des îles" + ChatColor.GOLD + " ===");

        for (int i = 0; i < topIslands.size(); i++) {
            Island island = topIslands.get(i);
            Player owner = Bukkit.getPlayer(island.getOwner());
            String ownerName = owner != null ? owner.getName() : "Inconnu";

            player.sendMessage("" + ChatColor.AQUA + (i + 1) + ". " + ChatColor.WHITE + ownerName +
                    ChatColor.GRAY + " - Niveau " + ChatColor.YELLOW + island.getLevel());
        }
    }

    private void handleExpand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /is expand <nouvelle_taille>");
            return;
        }

        int newSize;
        try {
            newSize = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Taille invalide !");
            return;
        }

        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
        if (skyblockPlayer == null || !skyblockPlayer.hasIsland()) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas d'île !");
            return;
        }

        Island island = plugin.getDatabaseManager().loadIsland(skyblockPlayer.getIslandId());
        if (island == null) {
            player.sendMessage(ChatColor.RED + "Impossible de trouver votre île !");
            return;
        }

        if (plugin.getIslandManager().expandIsland(island, newSize)) {
            player.sendMessage(ChatColor.GREEN + "Votre île a été agrandie à " + newSize + "x" + newSize + " !");
        } else {
            player.sendMessage(ChatColor.RED + "Impossible d'agrandir l'île à cette taille !");
        }
    }

    private void handleFlags(Player player) {
        plugin.getMenuManager().openFlagsMenu(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                    "create", "home", "delete", "invite", "accept", "deny", "kick", "leave",
                    "info", "menu", "warp", "sethome", "bank", "level", "top", "expand", "flags"
            );
            return subCommands.stream()
                    .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "invite", "warp", "info", "kick" -> {
                    // Complétion des noms de joueurs
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .toList();
                }
                case "bank" -> {
                    return Arrays.asList("deposit", "withdraw");
                }
            }
        }

        return new ArrayList<>();
    }

    // === MÉTHODES UTILITAIRES ===

    private UUID findMemberByName(Island island, String playerName) {
        // Chercher le propriétaire
        Player owner = Bukkit.getPlayer(island.getOwner());
        if (owner != null && owner.getName().equalsIgnoreCase(playerName)) {
            return island.getOwner();
        }

        // Chercher dans les membres
        for (UUID memberUuid : island.getMembers()) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null && member.getName().equalsIgnoreCase(playerName)) {
                return memberUuid;
            }

            // Chercher dans la base de données si hors ligne
            SkyblockPlayer memberData = plugin.getDatabaseManager().loadPlayer(memberUuid);
            if (memberData != null && memberData.getName().equalsIgnoreCase(playerName)) {
                return memberUuid;
            }
        }

        return null;
    }

    private String getPlayerName(UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            return player.getName();
        }

        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(playerUuid);
        return skyblockPlayer != null ? skyblockPlayer.getName() : "Joueur inconnu";
    }
}