package fr.skyblock.managers;

import fr.skyblock.CustomSkyblock;
import fr.skyblock.models.Island;
import fr.skyblock.models.MoneyPrinter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Gère les imprimantes à argent stockées dans Island.
 */
public class PrinterManager {

    private final CustomSkyblock plugin;
    private final NamespacedKey printerKey;
    private final NamespacedKey nametagKey;
    private final NamespacedKey nametagKindKey;

    // Cache: île -> armor stands (nametags) actifs par position
    private final Map<UUID, Map<String, UUID>> islandNametags = new HashMap<>();

    public PrinterManager(CustomSkyblock plugin) {
        this.plugin = plugin;
        this.printerKey = new NamespacedKey(plugin, "printer_tier");
        this.nametagKey = new NamespacedKey(plugin, "nametag_key");
        this.nametagKindKey = new NamespacedKey(plugin, "nametag_kind");
        startGenerationTask();
    }

    // === API publique pour le shop externe ===
    public ItemStack createPrinterItem(int tier) {
        ItemStack item = new ItemStack(Material.DROPPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Imprimante Tier " + tier);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Génère des billets automatiquement");
            lore.add(ChatColor.YELLOW + "Billet tier: " + tier + " ($" + plugin.getConfig().getLong("printers." + tier + ".value", tier * 10L) + ")");
            meta.setLore(lore);
            // Tag tier
            meta.getPersistentDataContainer().set(printerKey, PersistentDataType.INTEGER, tier);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void givePrinter(Player player, int tier) {
        player.getInventory().addItem(createPrinterItem(tier));
    }

    public boolean isPrinterItem(ItemStack item) {
        if (item == null || item.getType() != Material.DROPPER) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(printerKey, PersistentDataType.INTEGER);
    }

    public int getPrinterTier(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        Integer tier = meta.getPersistentDataContainer().get(printerKey, PersistentDataType.INTEGER);
        return tier != null ? tier : 0;
    }

    // === Gestion place/break ===
    public boolean placePrinter(Player player, Island island, Block block, int tier) {
        // Limite de pose pour le joueur
        int baseLimit = island.getMaxPrinters();
        int bonus = plugin.getPrisonTycoonHook().getAdditionalPrinterSlots(player.getUniqueId());
        int totalLimit = baseLimit + Math.max(0, bonus);
        int already = island.countPrintersOwnedBy(player.getUniqueId());
        if (already >= totalLimit) {
            player.sendMessage(ChatColor.RED + "Vous avez atteint votre limite d'imprimantes (" + totalLimit + ")");
            return false;
        }

        // Enregistrer imprimante
        MoneyPrinter printer = new MoneyPrinter(UUID.randomUUID(), player.getUniqueId(), tier,
                block.getX(), block.getY(), block.getZ());
        island.addPrinter(printer);
        plugin.getDatabaseManager().saveIsland(island);

        // Tag bloc en place
        setBlockAsPrinter(block, printer);
        player.sendMessage(ChatColor.GREEN + "Imprimante posée (Tier " + tier + ")");
        return true;
    }

    public boolean canBreakPrinter(Player breaker, Island island, MoneyPrinter printer) {
        if (breaker.getUniqueId().equals(printer.getOwnerUuid())) return true;
        return island.getOwner().equals(breaker.getUniqueId());
    }

    public boolean breakPrinter(Player breaker, Island island, Block block, MoneyPrinter printer) {
        if (!canBreakPrinter(breaker, island, printer)) {
            breaker.sendMessage(ChatColor.RED + "Vous ne pouvez pas retirer cette imprimante.");
            return false;
        }

        // Retirer de l'île et sauvegarder
        island.removePrinterById(printer.getId());
        plugin.getDatabaseManager().saveIsland(island);

        // Donner l'item au joueur qui l'a retirée
        breaker.getInventory().addItem(createPrinterItem(printer.getTier()));
        breaker.sendMessage(ChatColor.YELLOW + "Imprimante récupérée.");
        return true;
    }

    private void setBlockAsPrinter(Block block, MoneyPrinter printer) {
        // Le bloc lui-même est un DROPPER; nous n'avons pas d'API PDC sur Block, nous nous basons sur la position + island store
        // Le printerId est stocké sur les items; pour le bloc, on utilise la présence dans Island comme source de vérité.
    }

    // === Génération périodique ===
    private void startGenerationTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Island island : plugin.getDatabaseManager().getAllIslands()) {
                    if (island.getPrinters().isEmpty()) continue;

                    World world = plugin.getIslandManager().getIslandWorld(island);
                    if (world == null) continue; // île non chargée => stop

                    boolean changed = false;
                    // Parcourir les imprimantes
                    for (MoneyPrinter printer : island.getPrinters()) {
                        // Vérifier que le propriétaire est toujours membre
                        if (!island.isMember(printer.getOwnerUuid())) {
                            continue; // propriétaire n'est plus membre => ne pas générer
                        }

                        long intervalMs = Math.max(5000, (long) (60000.0 / Math.max(1, island.getBillGenerationSpeed())));
                        long now = System.currentTimeMillis();
                        if (printer.getLastGeneratedAt() == 0L || now - printer.getLastGeneratedAt() >= intervalMs) {
                            // Vérifier que le bloc est bien un dropper présent
                            Block block = world.getBlockAt(printer.getX(), printer.getY(), printer.getZ());
                            if (block == null || block.getType() != Material.DROPPER) {
                                continue;
                            }

                            // Générer le billet au-dessus
                            Location dropLoc = new Location(world, printer.getX() + 0.5, printer.getY() + 1.2, printer.getZ() + 0.5);
                            ItemStack bill = createBillForTier(printer.getTier());
                            world.dropItemNaturally(dropLoc, bill);

                            printer.setLastGeneratedAt(now);
                            changed = true;
                        }

                        // Gérer le nametag si un joueur est à <= 5 blocs
                        maybeShowNametag(island, world, printer);
                    }

                    // Sauvegarder l'île seulement si changement
                    if (changed) {
                        plugin.getDatabaseManager().saveIsland(island);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 5L, 20L * 5L); // toutes les 5 secondes
    }

    private ItemStack createBillForTier(int tier) {
        // Papier avec valeur depuis printer.yml (chargé dans getConfig sous printers.*)
        long value = plugin.getConfig().getLong("printers." + tier + ".value", tier * 10L);
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Billet Tier " + tier);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Tier " + tier + " (" + value + "$)");
            lore.add(ChatColor.GRAY + "Utiliser /sellall ou un tank pour vendre vos billets");
            meta.setLore(lore);
            paper.setItemMeta(meta);
        }
        return paper;
    }

    public void maybeShowNametag(Island island, World world, MoneyPrinter printer) {
        // Montrer un ArmorStand invisible si un joueur est à <=5 blocs
        Location loc = new Location(world, printer.getX() + 0.5, printer.getY() + 1.8, printer.getZ() + 0.5);
        boolean someoneNear = false;
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= 25) { someoneNear = true; break; }
        }

        String key = printer.getX() + ":" + printer.getY() + ":" + printer.getZ();
        islandNametags.computeIfAbsent(island.getId(), k -> new HashMap<>());
        Map<String, UUID> tags = islandNametags.get(island.getId());

        if (someoneNear) {
            UUID existing = tags.get(key);
            ArmorStand asFound = findExistingNametag(world, key, loc);
            if (existing == null || getEntity(world, existing) == null) {
                if (asFound == null) {
                    ArmorStand as = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
                    as.setInvisible(true);
                    as.setMarker(true);
                    as.setCustomNameVisible(true);
                    as.setCustomName(ChatColor.GOLD + "Imprimante T" + printer.getTier());
                    // Marqueurs PDC
                    PersistentDataContainer pdc = as.getPersistentDataContainer();
                    pdc.set(nametagKey, PersistentDataType.STRING, key);
                    pdc.set(nametagKindKey, PersistentDataType.STRING, "printer");
                    tags.put(key, as.getUniqueId());
                } else {
                    asFound.setCustomName(ChatColor.GOLD + "Imprimante T" + printer.getTier());
                    tags.put(key, asFound.getUniqueId());
                }
            } else {
                Entity ent = getEntity(world, existing);
                if (ent instanceof ArmorStand as) {
                    as.setCustomName(ChatColor.GOLD + "Imprimante T" + printer.getTier());
                }
            }
        } else {
            removeNametag(island.getId(), world, printer.getX(), printer.getY(), printer.getZ());
        }
    }

    private ArmorStand findExistingNametag(World world, String key, Location around) {
        for (Entity e : world.getNearbyEntities(around, 1.0, 1.5, 1.0)) {
            if (e instanceof ArmorStand as) {
                PersistentDataContainer pdc = as.getPersistentDataContainer();
                if (pdc.has(nametagKey, PersistentDataType.STRING) &&
                        key.equals(pdc.get(nametagKey, PersistentDataType.STRING)) &&
                        "printer".equals(pdc.get(nametagKindKey, PersistentDataType.STRING))) {
                    return as;
                }
            }
        }
        return null;
    }

    private Entity getEntity(World world, UUID uuid) {
        for (Entity e : world.getEntities()) {
            if (e.getUniqueId().equals(uuid)) return e;
        }
        return null;
    }

    public void removeNametag(UUID islandId, World world, int x, int y, int z) {
        Map<String, UUID> tags = islandNametags.get(islandId);
        String key = x + ":" + y + ":" + z;
        if (tags != null) {
            UUID tagId = tags.remove(key);
            if (tagId != null) {
                Entity ent = getEntity(world, tagId);
                if (ent != null) ent.remove();
            }
        }
        // Supprimer tout ArmorStand marqué à cette position, même non tracké
        for (Entity e : world.getNearbyEntities(new Location(world, x + 0.5, y + 1.8, z + 0.5), 1.0, 1.5, 1.0)) {
            if (e instanceof ArmorStand as) {
                PersistentDataContainer pdc = as.getPersistentDataContainer();
                if ("printer".equals(pdc.get(nametagKindKey, PersistentDataType.STRING)) &&
                        key.equals(pdc.get(nametagKey, PersistentDataType.STRING))) {
                    as.remove();
                }
            }
        }
    }

    public void clearNametagsForIsland(UUID islandId, World world) {
        Map<String, UUID> tags = islandNametags.get(islandId);
        if (tags != null) {
            for (UUID tagId : new ArrayList<>(tags.values())) {
                Entity ent = getEntity(world, tagId);
                if (ent != null) ent.remove();
            }
            tags.clear();
            islandNametags.remove(islandId);
        }
        // Supprimer tous les nametags d'imprimantes présents dans le monde (sécurité contre doublons)
        for (Entity e : world.getEntities()) {
            if (e instanceof ArmorStand as) {
                PersistentDataContainer pdc = as.getPersistentDataContainer();
                if ("printer".equals(pdc.get(nametagKindKey, PersistentDataType.STRING))) {
                    as.remove();
                }
            }
        }
    }
}


