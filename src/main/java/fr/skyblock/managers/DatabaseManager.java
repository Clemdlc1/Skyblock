package fr.skyblock.managers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.skyblock.CustomSkyblock;
import fr.skyblock.models.Island;
import fr.skyblock.models.SkyblockPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {

    private final CustomSkyblock plugin;
    private HikariDataSource dataSource;
    private final Gson gson = new Gson();

    private final Map<UUID, Island> islandsCache = new ConcurrentHashMap<>();
    private final Map<UUID, SkyblockPlayer> playersCache = new ConcurrentHashMap<>();

    public DatabaseManager(CustomSkyblock plugin) {
        this.plugin = plugin;
        initPostgreSQL();
        loadAll();
    }

    private void initPostgreSQL() {
        try {
            Class.forName("org.postgresql.Driver");
            FileConfiguration config = plugin.getConfig();
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s",
                    config.getString("database.host", "localhost"),
                    config.getInt("database.port", 5432),
                    config.getString("database.database", "prisontycoon")));
            hikariConfig.setUsername(config.getString("database.username", "user"));
            hikariConfig.setPassword(config.getString("database.password", "password"));
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.setMaximumPoolSize(10);
            this.dataSource = new HikariDataSource(hikariConfig);
            createTables();
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("PostgreSQL JDBC Driver not found: " + e.getMessage());
        }
    }

    private void createTables() {
        String islandsTable = "CREATE TABLE IF NOT EXISTS islands (" +
                "id VARCHAR(36) PRIMARY KEY," +
                "owner_uuid VARCHAR(36) NOT NULL," +
                "name VARCHAR(255) NOT NULL," +
                "level INT DEFAULT 0," +
                "bank DOUBLE PRECISION DEFAULT 0," +
                "size INT DEFAULT 50," +
                "center_world VARCHAR(255)," +
                "center_x DOUBLE PRECISION," +
                "center_y DOUBLE PRECISION," +
                "center_z DOUBLE PRECISION," +
                "center_yaw REAL," +
                "center_pitch REAL," +
                "members TEXT," +
                "flags TEXT," +
                "creation_time BIGINT DEFAULT 0," +
                "last_activity BIGINT DEFAULT 0," +
                "hopper_limit INT DEFAULT 10," +
                "current_hoppers INT DEFAULT 0," +
                "max_deposit_chests INT DEFAULT 1," +
                "bill_generation_speed INT DEFAULT 1," +
                "max_printers INT DEFAULT 1" +
                ");";

        // CORRECTION : Supprimer l'ancienne contrainte si elle existe et recréer correctement
        String dropOldConstraint = "ALTER TABLE IF EXISTS skyblock_players DROP CONSTRAINT IF EXISTS skyblock_players_island_id_fkey;";

        String playersTable = "CREATE TABLE IF NOT EXISTS skyblock_players (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "name VARCHAR(16) NOT NULL," +
                "island_id VARCHAR(36)," +
                "first_join BIGINT DEFAULT 0," +
                "last_seen BIGINT DEFAULT 0," +
                "island_resets INT DEFAULT 0," +
                "member_of_islands TEXT," +
                "player_data TEXT" +
                ");";

        // CORRECTION : Contrainte de clé étrangère corrigée pointant vers 'islands'
        String addForeignKey = "ALTER TABLE skyblock_players ADD CONSTRAINT skyblock_players_island_id_fkey " +
                "FOREIGN KEY (island_id) REFERENCES islands(id) ON DELETE SET NULL;";

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(islandsTable);
            stmt.execute(dropOldConstraint); // Supprimer l'ancienne contrainte incorrecte
            stmt.execute(playersTable);

            // Vérifier si la contrainte existe avant de l'ajouter
            String checkConstraint = "SELECT constraint_name FROM information_schema.table_constraints " +
                    "WHERE table_name = 'skyblock_players' AND constraint_name = 'skyblock_players_island_id_fkey'";

            ResultSet rs = stmt.executeQuery(checkConstraint);
            if (!rs.next()) {
                stmt.execute(addForeignKey);
                plugin.getLogger().info("Contrainte de clé étrangère créée correctement");
            }
            rs.close();

        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create tables: " + e.getMessage());
        }
    }

    // --- Gestion des Îles ---

    public void saveIsland(Island island) {
        islandsCache.put(island.getId(), island);

        String query = "INSERT INTO islands (id, owner_uuid, name, level, bank, size, center_world, center_x, center_y, center_z, center_yaw, center_pitch, members, flags, creation_time, last_activity, hopper_limit, current_hoppers, max_deposit_chests, bill_generation_speed, max_printers) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (id) DO UPDATE SET " +
                "owner_uuid = EXCLUDED.owner_uuid, name = EXCLUDED.name, level = EXCLUDED.level, bank = EXCLUDED.bank, size = EXCLUDED.size, " +
                "center_world = EXCLUDED.center_world, center_x = EXCLUDED.center_x, center_y = EXCLUDED.center_y, center_z = EXCLUDED.center_z, " +
                "center_yaw = EXCLUDED.center_yaw, center_pitch = EXCLUDED.center_pitch, members = EXCLUDED.members, flags = EXCLUDED.flags, " +
                "creation_time = EXCLUDED.creation_time, last_activity = EXCLUDED.last_activity, " +
                "hopper_limit = EXCLUDED.hopper_limit, current_hoppers = EXCLUDED.current_hoppers, " +
                "max_deposit_chests = EXCLUDED.max_deposit_chests, bill_generation_speed = EXCLUDED.bill_generation_speed, max_printers = EXCLUDED.max_printers";

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, island.getId().toString());
            ps.setString(2, island.getOwner().toString());
            ps.setString(3, island.getName());
            ps.setInt(4, island.getLevel());
            ps.setDouble(5, island.getBank());
            ps.setInt(6, island.getSize());

            Location center = island.getCenter();
            if (center != null && center.getWorld() != null) {
                ps.setString(7, center.getWorld().getName());
                ps.setDouble(8, center.getX());
                ps.setDouble(9, center.getY());
                ps.setDouble(10, center.getZ());
                ps.setFloat(11, center.getYaw());
                ps.setFloat(12, center.getPitch());
            } else {
                ps.setNull(7, Types.VARCHAR);
                ps.setNull(8, Types.DOUBLE);
                ps.setNull(9, Types.DOUBLE);
                ps.setNull(10, Types.DOUBLE);
                ps.setNull(11, Types.FLOAT);
                ps.setNull(12, Types.FLOAT);
            }

            ps.setString(13, gson.toJson(island.getMembers()));
            ps.setString(14, gson.toJson(island.getFlags()));
            ps.setLong(15, island.getCreationTime());
            ps.setLong(16, island.getLastActivity());

            ps.setInt(17, island.getHopperLimit());
            ps.setInt(18, island.getCurrentHoppers());
            ps.setInt(19, island.getMaxDepositChests());
            ps.setInt(20, island.getBillGenerationSpeed());
            ps.setInt(21, island.getMaxPrinters());

            ps.executeUpdate();
            plugin.getLogger().info("Île sauvegardée: " + island.getId());

        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving island " + island.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Island loadIsland(UUID islandId) {
        if (islandsCache.containsKey(islandId)) {
            return islandsCache.get(islandId);
        }

        String query = "SELECT * FROM islands WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, islandId.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Island island = mapResultSetToIsland(rs);
                islandsCache.put(islandId, island);
                return island;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading island " + islandId + ": " + e.getMessage());
        }
        return null;
    }

    public void deleteIsland(UUID islandId) {
        islandsCache.remove(islandId);
        String query = "DELETE FROM islands WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, islandId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error deleting island " + islandId + ": " + e.getMessage());
        }
    }

    public Collection<Island> getAllIslands() {
        return islandsCache.values();
    }

    public Island getIslandByOwner(UUID ownerUuid) {
        return islandsCache.values().stream()
                .filter(island -> island.getOwner().equals(ownerUuid))
                .findFirst()
                .orElse(null);
    }

    // --- Gestion des Joueurs ---

    public void savePlayer(SkyblockPlayer player) {
        playersCache.put(player.getUuid(), player);

        String query = "INSERT INTO skyblock_players (uuid, name, island_id, first_join, last_seen, island_resets, member_of_islands, player_data) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (uuid) DO UPDATE SET " +
                "name = EXCLUDED.name, island_id = EXCLUDED.island_id, first_join = EXCLUDED.first_join, " +
                "last_seen = EXCLUDED.last_seen, island_resets = EXCLUDED.island_resets, " +
                "member_of_islands = EXCLUDED.member_of_islands, player_data = EXCLUDED.player_data";

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, player.getUuid().toString());
            ps.setString(2, player.getName());

            if (player.getIslandId() != null) {
                ps.setString(3, player.getIslandId().toString());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }

            ps.setLong(4, player.getFirstJoin());
            ps.setLong(5, player.getLastSeen());
            ps.setInt(6, player.getIslandResets());
            ps.setString(7, gson.toJson(player.getMemberOfIslands()));
            ps.setString(8, gson.toJson(player.getData()));

            ps.executeUpdate();
            plugin.getLogger().info("Joueur sauvegardé: " + player.getName());

        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving player " + player.getUuid() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public SkyblockPlayer loadPlayer(UUID playerUuid) {
        if (playersCache.containsKey(playerUuid)) {
            return playersCache.get(playerUuid);
        }

        String query = "SELECT * FROM skyblock_players WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                SkyblockPlayer player = mapResultSetToPlayer(rs);
                playersCache.put(playerUuid, player);
                return player;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading player " + playerUuid + ": " + e.getMessage());
        }
        return null;
    }

    public SkyblockPlayer getOrCreatePlayer(UUID playerUuid, String playerName) {
        SkyblockPlayer player = loadPlayer(playerUuid);
        if (player == null) {
            player = new SkyblockPlayer(playerUuid, playerName);
            savePlayer(player);
        } else if (!player.getName().equals(playerName)) {
            player.setName(playerName);
            savePlayer(player);
        }
        return player;
    }

    public Collection<SkyblockPlayer> getAllPlayers() {
        return playersCache.values();
    }

    // --- Gestion Globale et Chargement ---

    public void saveAll() {
        plugin.getLogger().info("Saving all data to database...");
        islandsCache.values().forEach(this::saveIsland);
        playersCache.values().forEach(this::savePlayer);
        plugin.getLogger().info("Save complete!");
    }

    private void loadAll() {
        plugin.getLogger().info("Loading all islands from database...");
        String islandQuery = "SELECT * FROM islands";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(islandQuery);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Island island = mapResultSetToIsland(rs);
                islandsCache.put(island.getId(), island);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading all islands: " + e.getMessage());
        }
        plugin.getLogger().info(islandsCache.size() + " islands loaded!");

        plugin.getLogger().info("Loading all players from database...");
        String playerQuery = "SELECT * FROM skyblock_players";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(playerQuery);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SkyblockPlayer player = mapResultSetToPlayer(rs);
                playersCache.put(player.getUuid(), player);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading all players: " + e.getMessage());
        }
        plugin.getLogger().info(playersCache.size() + " players loaded!");
    }

    public void clearCache() {
        islandsCache.clear();
        playersCache.clear();
    }

    public void reloadFromDisk() {
        plugin.getLogger().info("Reloading all data from database...");
        clearCache();
        loadAll();
        plugin.getLogger().info("Reload complete!");
    }

    // --- Méthodes de Statistiques et de Recherche ---

    public List<Island> getInactiveIslands(long inactiveDays) {
        List<Island> islands = new ArrayList<>();
        long threshold = System.currentTimeMillis() - (inactiveDays * 24 * 60 * 60 * 1000);
        String query = "SELECT * FROM islands WHERE last_activity < ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, threshold);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                islands.add(mapResultSetToIsland(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting inactive islands: " + e.getMessage());
        }
        return islands;
    }

    public List<Island> getIslandsByLevel(int minLevel) {
        List<Island> islands = new ArrayList<>();
        String query = "SELECT * FROM islands WHERE level >= ? ORDER BY level DESC";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, minLevel);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                islands.add(mapResultSetToIsland(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting islands by level: " + e.getMessage());
        }
        return islands;
    }

    public List<Island> getTopIslandsByLevel(int limit) {
        List<Island> islands = new ArrayList<>();
        String query = "SELECT * FROM islands ORDER BY level DESC LIMIT ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                islands.add(mapResultSetToIsland(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting top islands by level: " + e.getMessage());
        }
        return islands;
    }

    public int getTotalIslands() {
        String query = "SELECT COUNT(*) FROM islands";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting total islands count: " + e.getMessage());
        }
        return 0;
    }

    public int getTotalPlayers() {
        String query = "SELECT COUNT(*) FROM skyblock_players";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting total players count: " + e.getMessage());
        }
        return 0;
    }

    public int getActiveIslands(long activeDays) {
        long threshold = System.currentTimeMillis() - (activeDays * 24 * 60 * 60 * 1000);
        String query = "SELECT COUNT(*) FROM islands WHERE last_activity >= ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, threshold);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting active islands count: " + e.getMessage());
        }
        return 0;
    }

    // --- Fermeture et Méthodes d'Aide ---

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private Island mapResultSetToIsland(ResultSet rs) throws SQLException {
        UUID islandId = UUID.fromString(rs.getString("id"));
        UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
        String name = rs.getString("name");

        Location center = null;
        String worldName = rs.getString("center_world");
        if (worldName != null && !worldName.isEmpty()) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                center = new Location(
                        world,
                        rs.getDouble("center_x"),
                        rs.getDouble("center_y"),
                        rs.getDouble("center_z"),
                        rs.getFloat("center_yaw"),
                        rs.getFloat("center_pitch")
                );
            } else {
                plugin.getLogger().warning("Could not find world '" + worldName + "' for island " + islandId);
            }
        }

        Island island = new Island(islandId, ownerUuid, name, center);

        island.setLevel(rs.getInt("level"));
        island.setBank(rs.getDouble("bank"));
        island.setSize(rs.getInt("size"));
        island.setLastActivity(rs.getLong("last_activity"));

        // Charger améliorations avec valeurs par défaut simples
        int hopperLimit = rs.getInt("hopper_limit");
        island.setHopperLimit(hopperLimit <= 0 ? 10 : hopperLimit);
        island.setCurrentHoppers(Math.max(0, rs.getInt("current_hoppers")));
        int maxDeposit = rs.getInt("max_deposit_chests");
        island.setMaxDepositChests(maxDeposit <= 0 ? 1 : maxDeposit);
        int billSpeed = rs.getInt("bill_generation_speed");
        island.setBillGenerationSpeed(billSpeed <= 0 ? 1 : billSpeed);
        int maxPrinters = rs.getInt("max_printers");
        island.setMaxPrinters(maxPrinters <= 0 ? 1 : maxPrinters);

        // Charger les membres
        String membersJson = rs.getString("members");
        if (membersJson != null && !membersJson.isEmpty()) {
            Type memberListType = new TypeToken<Set<UUID>>() {}.getType();
            Set<UUID> members = gson.fromJson(membersJson, memberListType);
            if (members != null) {
                members.forEach(island::addMember);
            }
        }

        // Charger les flags
        String flagsJson = rs.getString("flags");
        if (flagsJson != null && !flagsJson.isEmpty()) {
            Type flagsType = new TypeToken<Map<Island.IslandFlag, Boolean>>() {}.getType();
            Map<Island.IslandFlag, Boolean> flags = gson.fromJson(flagsJson, flagsType);
            if (flags != null) {
                flags.forEach(island::setFlag);
            }
        }

        return island;
    }

    private SkyblockPlayer mapResultSetToPlayer(ResultSet rs) throws SQLException {
        UUID playerUuid = UUID.fromString(rs.getString("uuid"));
        String name = rs.getString("name");

        SkyblockPlayer player = new SkyblockPlayer(playerUuid, name);

        String islandIdStr = rs.getString("island_id");
        if (islandIdStr != null) {
            player.createIsland(UUID.fromString(islandIdStr));
        }

        player.setFirstJoin(rs.getLong("first_join"));
        player.setLastSeen(rs.getLong("last_seen"));
        player.setIslandResets(rs.getInt("island_resets"));

        // Charger member_of_islands
        String memberOfIslandsJson = rs.getString("member_of_islands");
        if (memberOfIslandsJson != null && !memberOfIslandsJson.isEmpty()) {
            Type memberOfIslandsType = new TypeToken<Set<UUID>>() {}.getType();
            Set<UUID> memberOfIslands = gson.fromJson(memberOfIslandsJson, memberOfIslandsType);
            if (memberOfIslands != null) {
                memberOfIslands.forEach(player::joinIsland);
            }
        }

        // Charger player_data
        String playerDataJson = rs.getString("player_data");
        if (playerDataJson != null && !playerDataJson.isEmpty()) {
            Type playerDataType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> playerData = gson.fromJson(playerDataJson, playerDataType);
            if (playerData != null) {
                playerData.forEach(player::setData);
            }
        }

        return player;
    }

    // Getters pour les données manquantes dans SkyblockPlayer
    public Map<String, Object> getData(SkyblockPlayer player) {
        return player.getData();
    }
}