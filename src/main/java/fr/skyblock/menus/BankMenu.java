package fr.skyblock.menus;

import fr.skyblock.CustomSkyblock;
import fr.skyblock.managers.MenuManager;
import fr.skyblock.models.Island;
import fr.skyblock.models.SkyblockPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public class BankMenu extends BaseMenu {

    public BankMenu(CustomSkyblock plugin, MenuManager menuManager) {
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

        Inventory inv = createInventory(27, ChatColor.DARK_GREEN + "Banque de l'île");

        // Solde actuel
        List<String> balanceLore = new ArrayList<>();
        balanceLore.add(ChatColor.GRAY + "Solde actuel: " + ChatColor.WHITE + String.format("%.2f $", island.getBank()));
        balanceLore.add("");
        balanceLore.add(ChatColor.GRAY + "La banque de l'île permet de");
        balanceLore.add(ChatColor.GRAY + "stocker l'argent commun de l'île");
        balanceLore.add("");
        balanceLore.add(ChatColor.AQUA + "Tous les membres peuvent:");
        balanceLore.add(ChatColor.GRAY + "• Voir le solde");
        balanceLore.add(ChatColor.GRAY + "• Déposer de l'argent");
        balanceLore.add(ChatColor.GRAY + "• Retirer de l'argent");

        inv.setItem(4, createItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "💰 Banque de l'île", balanceLore));

        // Boutons d'action
        List<String> depositLore = new ArrayList<>();
        depositLore.add(ChatColor.GRAY + "Déposer votre argent");
        depositLore.add(ChatColor.GRAY + "dans la banque de l'île");
        depositLore.add("");
        depositLore.add(ChatColor.WHITE + "Votre solde: " + ChatColor.GOLD +
                plugin.getEconomyManager().formatMoney(plugin.getEconomyManager().getBalance(player.getUniqueId())));
        depositLore.add("");
        depositLore.add(ChatColor.YELLOW + "Clic pour déposer");

        inv.setItem(11, createItem(Material.GOLD_INGOT, ChatColor.YELLOW + "💵 Déposer de l'argent", depositLore));

        List<String> withdrawLore = new ArrayList<>();
        withdrawLore.add(ChatColor.GRAY + "Retirer de l'argent");
        withdrawLore.add(ChatColor.GRAY + "de la banque de l'île");
        withdrawLore.add("");
        withdrawLore.add(ChatColor.WHITE + "Banque: " + ChatColor.GREEN + String.format("%.2f $", island.getBank()));
        withdrawLore.add("");
        withdrawLore.add(ChatColor.YELLOW + "Clic pour retirer");

        inv.setItem(15, createItem(Material.DIAMOND, ChatColor.AQUA + "💎 Retirer de l'argent", withdrawLore));

        // Statistiques de la banque
        List<String> statsLore = new ArrayList<>();
        statsLore.add(ChatColor.GRAY + "Statistiques de la banque:");
        statsLore.add("");
        statsLore.add(ChatColor.WHITE + "💰 Solde total: " + ChatColor.GREEN + String.format("%.2f $", island.getBank()));
        statsLore.add(ChatColor.WHITE + "👥 Membres: " + ChatColor.AQUA + (island.getMembers().size() + 1));
        statsLore.add(ChatColor.WHITE + "📊 Niveau île: " + ChatColor.YELLOW + island.getLevel());
        statsLore.add("");
        // TODO: Ajouter historique des transactions
        statsLore.add(ChatColor.GRAY + "📋 Historique: " + ChatColor.YELLOW + "Bientôt disponible");

        inv.setItem(13, createItem(Material.BOOK, ChatColor.BLUE + "📊 Statistiques", statsLore));

        // Actions rapides
        List<String> quickActionsLore = new ArrayList<>();
        quickActionsLore.add(ChatColor.GRAY + "Actions rapides:");
        quickActionsLore.add("");
        quickActionsLore.add(ChatColor.WHITE + "• " + ChatColor.GOLD + "Déposer tout");
        quickActionsLore.add(ChatColor.WHITE + "• " + ChatColor.AQUA + "Retirer 1000$");
        quickActionsLore.add(ChatColor.WHITE + "• " + ChatColor.GREEN + "Retirer 10000$");
        quickActionsLore.add("");
        quickActionsLore.add(ChatColor.YELLOW + "Clic pour voir les options");

        inv.setItem(20, createItem(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "⚡ Actions rapides", quickActionsLore));

        // Revenus passifs
        double passiveIncome = calculatePassiveIncome(island);
        List<String> passiveLore = new ArrayList<>();
        passiveLore.add(ChatColor.GRAY + "Revenus passifs de l'île:");
        passiveLore.add("");
        passiveLore.add(ChatColor.WHITE + "💫 Revenu/heure: " + ChatColor.GREEN + String.format("%.2f $", passiveIncome));
        passiveLore.add(ChatColor.WHITE + "📈 Basé sur: " + ChatColor.YELLOW + "Niveau " + island.getLevel());
        passiveLore.add("");
        passiveLore.add(ChatColor.GRAY + "Les revenus sont automatiquement");
        passiveLore.add(ChatColor.GRAY + "ajoutés à la banque chaque heure");

        inv.setItem(24, createItem(Material.CLOCK, ChatColor.GREEN + "🕐 Revenus passifs", passiveLore));

        // Bouton retour
        inv.setItem(22, createBackButton());

        fillEmptySlots(inv, Material.GREEN_STAINED_GLASS_PANE);

        player.openInventory(inv);
        setPlayerMenu(player, getMenuType());
        setMenuData(player, "island", island);
    }

    @Override
    public void handleClick(Player player, int slot) {
        Island island = (Island) getMenuData(player, "island");
        if (island == null) return;

        switch (slot) {
            case 11 -> { // Déposer
                player.closeInventory();
                plugin.getEconomyManager().startBankDeposit(player, island);
            }
            case 15 -> { // Retirer
                player.closeInventory();
                plugin.getEconomyManager().startBankWithdrawal(player, island);
            }
            case 13 -> { // Statistiques
                showBankStatistics(player, island);
            }
            case 20 -> { // Actions rapides
                openQuickActionsMenu(player, island);
            }
            case 24 -> { // Revenus passifs
                showPassiveIncomeInfo(player, island);
            }
            case 22 -> { // Retour
                openMainMenu(player);
            }
        }
    }

    @Override
    public String getMenuType() {
        return "bank";
    }

    private void openQuickActionsMenu(Player player, Island island) {
        Inventory inv = createInventory(27, ChatColor.DARK_PURPLE + "Actions rapides - Banque");

        double playerBalance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        double islandBank = island.getBank();

        // Déposer tout
        if (playerBalance > 0) {
            inv.setItem(10, createItem(Material.GOLD_BLOCK, ChatColor.GOLD + "💰 Déposer tout",
                    ChatColor.GRAY + "Déposer tout votre argent",
                    ChatColor.GRAY + "dans la banque de l'île",
                    "",
                    ChatColor.WHITE + "Montant: " + ChatColor.GOLD + String.format("%.2f $", playerBalance),
                    "",
                    ChatColor.YELLOW + "Clic pour déposer"));
        } else {
            inv.setItem(10, createItem(Material.BARRIER, ChatColor.RED + "💰 Déposer tout",
                    ChatColor.GRAY + "Vous n'avez pas d'argent",
                    ChatColor.GRAY + "à déposer"));
        }

        // Retirer montants fixes
        addQuickWithdrawButton(inv, 12, 1000, islandBank >= 1000);
        addQuickWithdrawButton(inv, 14, 10000, islandBank >= 10000);
        addQuickWithdrawButton(inv, 16, 50000, islandBank >= 50000);

        // Retirer tout
        if (islandBank > 0) {
            inv.setItem(22, createItem(Material.DIAMOND_BLOCK, ChatColor.AQUA + "💎 Retirer tout",
                    ChatColor.GRAY + "Retirer tout l'argent",
                    ChatColor.GRAY + "de la banque de l'île",
                    "",
                    ChatColor.WHITE + "Montant: " + ChatColor.GREEN + String.format("%.2f $", islandBank),
                    "",
                    ChatColor.YELLOW + "Clic pour retirer"));
        }

        inv.setItem(26, createItem(Material.ARROW, ChatColor.YELLOW + "Retour",
                ChatColor.GRAY + "Retourner au menu de la banque"));

        fillEmptySlots(inv, Material.PURPLE_STAINED_GLASS_PANE);

        player.openInventory(inv);
        setPlayerMenu(player, "bank_quick");
    }

    public void handleQuickActionsClick(Player player, int slot) {
        Island island = (Island) getMenuData(player, "island");
        if (island == null) return;

        switch (slot) {
            case 10 -> { // Déposer tout
                double playerBalance = plugin.getEconomyManager().getBalance(player.getUniqueId());
                if (playerBalance > 0) {
                    executeQuickDeposit(player, island, playerBalance);
                }
            }
            case 12 -> executeQuickWithdraw(player, island, 1000); // Retirer 1000
            case 14 -> executeQuickWithdraw(player, island, 10000); // Retirer 10000
            case 16 -> executeQuickWithdraw(player, island, 50000); // Retirer 50000
            case 22 -> executeQuickWithdraw(player, island, island.getBank()); // Retirer tout
            case 26 -> open(player); // Retour
        }
    }

    private void addQuickWithdrawButton(Inventory inv, int slot, double amount, boolean canAfford) {
        Material material = canAfford ? Material.EMERALD : Material.BARRIER;
        String color = canAfford ? ChatColor.GREEN.toString() : ChatColor.RED.toString();
        String formattedAmount = plugin.getEconomyManager().formatMoney(amount);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Retirer " + ChatColor.WHITE + formattedAmount);
        lore.add(ChatColor.GRAY + "de la banque de l'île");
        lore.add("");

        if (canAfford) {
            lore.add(ChatColor.YELLOW + "Clic pour retirer");
        } else {
            lore.add(ChatColor.RED + "Fonds insuffisants");
        }

        inv.setItem(slot, createItem(material, color + "💵 Retirer " + formattedAmount, lore));
    }

    private void executeQuickDeposit(Player player, Island island, double amount) {
        if (plugin.getEconomyManager().hasBalance(player.getUniqueId(), amount)) {
            plugin.getEconomyManager().removeBalance(player.getUniqueId(), amount);
            island.addToBank(amount);
            plugin.getDatabaseManager().saveIsland(island);

            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "✅ Vous avez déposé " +
                    plugin.getEconomyManager().formatMoney(amount) +
                    " dans la banque de l'île !");
        } else {
            player.sendMessage(ChatColor.RED + "❌ Fonds insuffisants !");
        }
    }

    private void executeQuickWithdraw(Player player, Island island, double amount) {
        if (island.removeFromBank(amount)) {
            plugin.getEconomyManager().addBalance(player.getUniqueId(), amount);
            plugin.getDatabaseManager().saveIsland(island);

            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "✅ Vous avez retiré " +
                    plugin.getEconomyManager().formatMoney(amount) +
                    " de la banque de l'île !");
        } else {
            player.sendMessage(ChatColor.RED + "❌ Fonds insuffisants dans la banque !");
        }
    }

    private void showBankStatistics(Player player, Island island) {
        player.closeInventory();

        player.sendMessage(ChatColor.GOLD + "=== 📊 Statistiques de la Banque ===");
        player.sendMessage(ChatColor.AQUA + "💰 Solde actuel: " + ChatColor.WHITE + String.format("%.2f $", island.getBank()));
        player.sendMessage(ChatColor.AQUA + "👥 Membres: " + ChatColor.WHITE + (island.getMembers().size() + 1));
        player.sendMessage(ChatColor.AQUA + "📊 Niveau île: " + ChatColor.WHITE + island.getLevel());
        player.sendMessage(ChatColor.AQUA + "💫 Revenus/heure: " + ChatColor.WHITE + String.format("%.2f $", calculatePassiveIncome(island)));

        // TODO: Ajouter plus de statistiques
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "📋 Historique des transactions bientôt disponible !");
    }

    private void showPassiveIncomeInfo(Player player, Island island) {
        double income = calculatePassiveIncome(island);
        double dailyIncome = income * 24;

        player.closeInventory();

        player.sendMessage(ChatColor.GOLD + "=== 🕐 Revenus Passifs ===");
        player.sendMessage(ChatColor.AQUA + "💫 Revenu par heure: " + ChatColor.WHITE + String.format("%.2f $", income));
        player.sendMessage(ChatColor.AQUA + "📅 Revenu par jour: " + ChatColor.WHITE + String.format("%.2f $", dailyIncome));
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "💡 Comment augmenter vos revenus:");
        player.sendMessage(ChatColor.YELLOW + "• Améliorer le niveau de votre île");
        player.sendMessage(ChatColor.YELLOW + "• Agrandir votre île");
        player.sendMessage(ChatColor.YELLOW + "• Rester actif sur votre île");
    }

    private double calculatePassiveIncome(Island island) {
        // Utiliser la méthode de l'EconomyManager
        double baseIncome = island.getLevel() * 0.5;
        double sizeMultiplier = 1.0 + (island.getSize() - 50) * 0.001;

        long daysSinceActivity = (System.currentTimeMillis() - island.getLastActivity()) / (24 * 60 * 60 * 1000);
        double activityMultiplier = daysSinceActivity <= 1 ? 1.5 : (daysSinceActivity <= 7 ? 1.0 : 0.5);

        return baseIncome * sizeMultiplier * activityMultiplier;
    }
}