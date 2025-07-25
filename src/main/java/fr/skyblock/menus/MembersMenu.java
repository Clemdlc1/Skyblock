package fr.skyblock.menus;

import fr.skyblock.CustomSkyblock;
import fr.skyblock.managers.MenuManager;
import fr.skyblock.models.Island;
import fr.skyblock.models.SkyblockPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MembersMenu extends BaseMenu {

    public MembersMenu(CustomSkyblock plugin, MenuManager menuManager) {
        super(plugin, menuManager);
    }

    @Override
    public void open(Player player) {
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

        Inventory inv = createInventory(54, ChatColor.DARK_GREEN + "Gestion des Membres");

        // Boutons d'action (si propriétaire)
        if (island.getOwner().equals(player.getUniqueId())) {
            inv.setItem(4, createItem(Material.PLAYER_HEAD, ChatColor.GREEN + "Inviter un joueur",
                    ChatColor.GRAY + "Inviter un nouveau membre",
                    ChatColor.GRAY + "à rejoindre votre île",
                    "",
                    ChatColor.YELLOW + "Utilisez: /island invite <joueur>"));

            inv.setItem(22, createItem(Material.WRITTEN_BOOK, ChatColor.AQUA + "Invitations en attente",
                    ChatColor.GRAY + "Voir les invitations que",
                    ChatColor.GRAY + "vous avez envoyées",
                    "",
                    ChatColor.YELLOW + "Clic pour voir"));
        }

        // Liste des membres
        int slot = 9;
        int maxMembersShown = 35;

        // Propriétaire
        Player owner = Bukkit.getPlayer(island.getOwner());
        String ownerName = owner != null ? owner.getName() : getPlayerNameFromUUID(island.getOwner());
        boolean ownerOnline = owner != null && owner.isOnline();

        List<String> ownerLore = new ArrayList<>();
        ownerLore.add(ChatColor.GRAY + "Statut: " + (ownerOnline ? ChatColor.GREEN + "En ligne" : ChatColor.RED + "Hors ligne"));
        ownerLore.add(ChatColor.GRAY + "Rôle: " + ChatColor.GOLD + "Propriétaire");
        ownerLore.add("");
        ownerLore.add(ChatColor.GRAY + "Le propriétaire de l'île");
        ownerLore.add(ChatColor.GRAY + "a tous les droits.");

        ItemStack ownerHead = createPlayerHead(ownerName,
                ChatColor.GOLD + "👑 " + ownerName + " (Propriétaire)", ownerLore);
        inv.setItem(slot++, ownerHead);

        // Membres
        for (UUID memberUuid : island.getMembers()) {
            if (slot >= 44) break; // Limite d'affichage

            Player member = Bukkit.getPlayer(memberUuid);
            String memberName = member != null ? member.getName() : getPlayerNameFromUUID(memberUuid);
            boolean isOnline = member != null && member.isOnline();

            List<String> memberLore = new ArrayList<>();
            memberLore.add(ChatColor.GRAY + "Statut: " + (isOnline ? ChatColor.GREEN + "En ligne" : ChatColor.RED + "Hors ligne"));
            memberLore.add(ChatColor.GRAY + "Rôle: " + ChatColor.AQUA + "Membre");
            memberLore.add("");

            if (island.getOwner().equals(player.getUniqueId())) {
                memberLore.add(ChatColor.RED + "Clic pour expulser");
                memberLore.add(ChatColor.GRAY + "Clic droit pour plus d'options");
            } else {
                memberLore.add(ChatColor.GRAY + "Membre de l'île");
            }

            ItemStack memberHead = createPlayerHead(memberName,
                    ChatColor.AQUA + memberName, memberLore);

            inv.setItem(slot++, memberHead);
        }

        // Informations générales
        inv.setItem(45, createItem(Material.BOOK, ChatColor.YELLOW + "Informations",
                ChatColor.GRAY + "Membres totaux: " + ChatColor.WHITE + (island.getMembers().size() + 1),
                ChatColor.GRAY + "Limite: " + ChatColor.WHITE + "10", // TODO: Configurable
                ChatColor.GRAY + "Propriétaire: " + ChatColor.WHITE + ownerName));

        // Statistiques des membres
        long membersOnline = island.getMembers().stream()
                .map(Bukkit::getPlayer)
                .mapToLong(p -> p != null && p.isOnline() ? 1 : 0)
                .sum();
        if (owner != null && owner.isOnline()) membersOnline++;

        inv.setItem(46, createItem(Material.EMERALD, ChatColor.GREEN + "Membres en ligne",
                ChatColor.GRAY + "Actuellement: " + ChatColor.WHITE + membersOnline,
                ChatColor.GRAY + "Total: " + ChatColor.WHITE + (island.getMembers().size() + 1)));

        // Bouton retour
        inv.setItem(49, createBackButton());

        fillEmptySlots(inv, Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        player.openInventory(inv);
        setPlayerMenu(player, getMenuType());
        setMenuData(player, "island", island);
    }

    @Override
    public void handleClick(Player player, int slot) {
        Island island = (Island) getMenuData(player, "island");
        if (island == null) return;

        switch (slot) {
            case 4 -> { // Inviter joueur
                if (island.getOwner().equals(player.getUniqueId())) {
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + "Utilisez " + ChatColor.GOLD + "/island invite <joueur>" +
                            ChatColor.YELLOW + " pour inviter un joueur !");
                }
            }
            case 22 -> { // Invitations en attente
                if (island.getOwner().equals(player.getUniqueId())) {
                    showPendingInvitations(player);
                }
            }
            case 49 -> openMainMenu(player); // Retour
            default -> {
                // Clic sur un membre
                if (slot >= 9 && slot < 44) {
                    handleMemberClick(player, slot, island);
                }
            }
        }
    }

    @Override
    public String getMenuType() {
        return "members";
    }

    private void handleMemberClick(Player player, int slot, Island island) {
        // Calculer quel membre a été cliqué
        int memberIndex = slot - 10; // -10 car le propriétaire est au slot 9

        if (memberIndex < 0) return; // C'est le propriétaire

        if (memberIndex >= island.getMembers().size()) return; // Hors limites

        // Trouver le membre cliqué
        UUID memberUuid = island.getMembers().toArray(new UUID[0])[memberIndex];

        if (!island.getOwner().equals(player.getUniqueId())) {
            // Pas le propriétaire, juste afficher les infos
            showMemberInfo(player, memberUuid);
            return;
        }

        // Propriétaire - ouvrir menu d'actions
        openMemberActionsMenu(player, memberUuid, island);
    }

    private void openMemberActionsMenu(Player player, UUID memberUuid, Island island) {
        Player member = Bukkit.getPlayer(memberUuid);
        String memberName = member != null ? member.getName() : getPlayerNameFromUUID(memberUuid);

        Inventory inv = createInventory(27, ChatColor.DARK_RED + "Actions - " + memberName);

        // Informations du membre
        inv.setItem(4, createPlayerHead(memberName, ChatColor.AQUA + memberName,
                ChatColor.GRAY + "Statut: " + (member != null && member.isOnline() ?
                        ChatColor.GREEN + "En ligne" : ChatColor.RED + "Hors ligne"),
                ChatColor.GRAY + "Membre depuis: " + ChatColor.WHITE + "TODO", // TODO: Date d'ajout
                "",
                ChatColor.YELLOW + "Choisissez une action"));

        // Actions disponibles
        inv.setItem(11, createItem(Material.RED_DYE, ChatColor.RED + "Expulser",
                ChatColor.GRAY + "Retirer ce joueur de l'île",
                ChatColor.GRAY + "Il perdra tous ses droits",
                "",
                ChatColor.RED + "Cette action est irréversible !",
                "",
                ChatColor.YELLOW + "Clic pour expulser"));

        inv.setItem(13, createItem(Material.ENDER_PEARL, ChatColor.BLUE + "Téléporter à lui",
                ChatColor.GRAY + "Se téléporter à la position",
                ChatColor.GRAY + "de ce membre",
                "",
                ChatColor.YELLOW + "Clic pour se téléporter"));

        inv.setItem(15, createItem(Material.PAPER, ChatColor.GREEN + "Envoyer un message",
                ChatColor.GRAY + "Envoyer un message privé",
                ChatColor.GRAY + "à ce membre",
                "",
                ChatColor.YELLOW + "Clic pour envoyer"));

        // Boutons de navigation
        inv.setItem(22, createItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                ChatColor.GRAY + "Retourner à la liste des membres"));

        fillEmptySlots(inv, Material.GRAY_STAINED_GLASS_PANE);

        player.openInventory(inv);
        setPlayerMenu(player, "member_actions");
        setMenuData(player, "member_uuid", memberUuid);
        setMenuData(player, "island", island);
    }

    public void handleMemberActionsClick(Player player, int slot) {
        UUID memberUuid = (UUID) getMenuData(player, "member_uuid");
        Island island = (Island) getMenuData(player, "island");

        if (memberUuid == null || island == null) return;

        switch (slot) {
            case 11 -> { // Expulser
                handleKickMember(player, memberUuid, island);
            }
            case 13 -> { // Téléporter
                handleTeleportToMember(player, memberUuid);
            }
            case 15 -> { // Message
                handleSendMessage(player, memberUuid);
            }
            case 22 -> { // Retour
                open(player);
            }
        }
    }

    private void handleKickMember(Player player, UUID memberUuid, Island island) {
        Player member = Bukkit.getPlayer(memberUuid);
        String memberName = member != null ? member.getName() : getPlayerNameFromUUID(memberUuid);

        if (plugin.getIslandManager().removeMember(island, memberUuid)) {
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + memberName + " a été expulsé de l'île !");

            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.RED + "Vous avez été expulsé de l'île de " + player.getName() + " !");
            }

            // Notifier les autres membres
            for (UUID otherMember : island.getMembers()) {
                Player otherPlayer = Bukkit.getPlayer(otherMember);
                if (otherPlayer != null && otherPlayer.isOnline() && !otherPlayer.equals(player)) {
                    otherPlayer.sendMessage(ChatColor.YELLOW + memberName + " a été expulsé de l'île.");
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "Impossible d'expulser ce joueur !");
        }
    }

    private void handleTeleportToMember(Player player, UUID memberUuid) {
        Player member = Bukkit.getPlayer(memberUuid);
        if (member == null || !member.isOnline()) {
            player.sendMessage(ChatColor.RED + "Ce joueur n'est pas en ligne !");
            return;
        }

        player.closeInventory();
        player.teleport(member.getLocation());
        player.sendMessage(ChatColor.GREEN + "Téléporté à " + member.getName() + " !");
    }

    private void handleSendMessage(Player player, UUID memberUuid) {
        Player member = Bukkit.getPlayer(memberUuid);
        if (member == null || !member.isOnline()) {
            player.sendMessage(ChatColor.RED + "Ce joueur n'est pas en ligne !");
            return;
        }

        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "Utilisez " + ChatColor.GOLD + "/msg " + member.getName() + " <message>" +
                ChatColor.YELLOW + " pour lui envoyer un message !");
    }

    private void showMemberInfo(Player player, UUID memberUuid) {
        Player member = Bukkit.getPlayer(memberUuid);
        String memberName = member != null ? member.getName() : getPlayerNameFromUUID(memberUuid);

        player.sendMessage(ChatColor.AQUA + "=== Informations de " + memberName + " ===");
        player.sendMessage(ChatColor.GRAY + "Statut: " + (member != null && member.isOnline() ?
                ChatColor.GREEN + "En ligne" : ChatColor.RED + "Hors ligne"));
        player.sendMessage(ChatColor.GRAY + "Rôle: " + ChatColor.AQUA + "Membre");
        // TODO: Ajouter plus d'informations (date d'ajout, activité, etc.)
    }

    private void showPendingInvitations(Player player) {
        // TODO: Implémenter l'affichage des invitations en attente
        player.sendMessage(ChatColor.YELLOW + "Fonctionnalité des invitations en attente en cours de développement...");
        player.sendMessage(ChatColor.GRAY + "Utilisez /island invite <joueur> pour inviter des joueurs.");
    }

    private String getPlayerNameFromUUID(UUID playerUuid) {
        SkyblockPlayer skyblockPlayer = plugin.getDatabaseManager().loadPlayer(playerUuid);
        return skyblockPlayer != null ? skyblockPlayer.getName() : "Joueur inconnu";
    }
}