package fr.skyblock.menus;

import fr.skyblock.CustomSkyblock;
import fr.skyblock.managers.MenuManager;
import fr.skyblock.managers.SchematicManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class SchematicMenu extends BaseMenu {

    private static final int ITEMS_PER_PAGE = 27; // 3 rows of 9

    public SchematicMenu(CustomSkyblock plugin, MenuManager menuManager) {
        super(plugin, menuManager);
    }

    @Override
    public void open(Player player) {
        open(player, 1);
    }

    public void open(Player player, int page) {
        SchematicManager schematicManager = plugin.getSchematicManager();
        List<String> schematics = schematicManager.getAvailableSchematics();

        if (schematics.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Aucun schematic n'est disponible pour le moment.");
            return;
        }

        int totalPages = (int) Math.ceil((double) schematics.size() / ITEMS_PER_PAGE);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        Inventory inv = createInventory(45, ChatColor.DARK_BLUE + "Choisissez un Schematic (Page " + page + "/" + totalPages + ")");

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, schematics.size());

        for (int i = startIndex; i < endIndex; i++) {
            String schematicId = schematics.get(i);
            Map<String, Object> data = schematicManager.getSchematicData(schematicId);

            String name = (String) data.getOrDefault("name", "Schematic Inconnu");
            Material material = Material.valueOf((String) data.getOrDefault("material", "GRASS_BLOCK"));
            List<String> description = (List<String>) data.getOrDefault("description", List.of("Description non disponible."));

            ItemStack item = createItem(material, ChatColor.GREEN + name, description);
            inv.setItem(i - startIndex, item);
        }

        // Navigation
        if (page > 1) {
            inv.setItem(36, createPreviousPageButton());
        }
        inv.setItem(40, createCloseButton());
        if (page < totalPages) {
            inv.setItem(44, createNextPageButton());
        }

        fillEmptySlots(inv, Material.GRAY_STAINED_GLASS_PANE);

        player.openInventory(inv);
        setPlayerMenu(player, getMenuType());
        setMenuData(player, "page", page);
    }

    @Override
    public void handleClick(Player player, int slot) {
        int page = (int) getMenuData(player, "page");
        SchematicManager schematicManager = plugin.getSchematicManager();
        List<String> schematics = schematicManager.getAvailableSchematics();

        if (slot >= 0 && slot < ITEMS_PER_PAGE) {
            int index = (page - 1) * ITEMS_PER_PAGE + slot;
            if (index < schematics.size()) {
                String schematicId = schematics.get(index);
                player.closeInventory();
                schematicManager.createIslandWithSchematic(player, schematicId);
            }
        } else if (slot == 36) { // Page précédente
            open(player, page - 1);
        } else if (slot == 40) { // Fermer
            player.closeInventory();
        } else if (slot == 44) { // Page suivante
            open(player, page + 1);
        }
    }

    @Override
    public String getMenuType() {
        return "schematic";
    }
}
