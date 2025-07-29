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
                "center_world VARCHAR(255)," +
                "center_x DOUBLE PRECISION," +
                "center_y DOUBLE PRECISION," +
                "center_z DOUBLE PRECISION," +
                "center_yaw REAL," +
                "center_pitch REAL," +
                "members TEXT," +
                "last_activity BIGINT DEFAULT 0" +
                ");";

        String playersTable = "CREATE TABLE IF NOT EXISTS skyblock_players (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "name VARCHAR(16) NOT NULL," +
                "island_id VARCHAR(36)," +
                "FOREIGN KEY (island_id) REFERENCES islands(id) ON DELETE SET NULL" +
                ");";

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(islandsTable);
            stmt.execute(playersTable);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create tables: " + e.getMessage());
        }
    }

    // --- Gestion des Îles ---

    public void saveIsland(Island island) {
        islandsCache.put(island.getId(), island);
        String query = "INSERT INTO islands (id, owner_uuid, name, level, bank, center_world, center_x, center_y, center_z, center_yaw, center_pitch, members, last_activity) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (id) DO UPDATE SET " +
                "owner_uuid = EXCLUDED.owner_uuid, name = EXCLUDED.name, level = EXCLUDED.level, bank = EXCLUDED.bank, center_world = EXCLUDED.center_world, " +
                "center_x = EXCLUDED.center_x, center_y = EXCLUDED.center_y, center_z = EXCLUDED.center_z, center_yaw = EXCLUDED.center_yaw, " +
                "center_pitch = EXCLUDED.center_pitch, members = EXCLUDED.members, last_activity = EXCLUDED.last_activity";

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, island.getId().toString());
            ps.setString(2, island.getOwner().toString());
            ps.setString(3, island.getName());
            ps.setInt(4, island.getLevel());
            ps.setDouble(5, island.getBank());

            Location center = island.getCenter();
            if (center != null && center.getWorld() != null) {
                ps.setString(6, center.getWorld().getName());
                ps.setDouble(7, center.getX());
                ps.setDouble(8, center.getY());
                ps.setDouble(9, center.getZ());
                ps.setFloat(10, center.getYaw());
                ps.setFloat(11, center.getPitch());
            } else {
                ps.setNull(6, Types.VARCHAR);
                ps.setNull(7, Types.DOUBLE);
                ps.setNull(8, Types.DOUBLE);
                ps.setNull(9, Types.DOUBLE);
                ps.setNull(10, Types.FLOAT);
                ps.setNull(11, Types.FLOAT);
            }

            ps.setString(12, gson.toJson(island.getMembers()));
            ps.setLong(13, island.getLastActivity());

            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving island " + island.getId() + ": " + e.getMessage());
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

    // RÉINTÉGRÉ
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
        String query = "INSERT INTO skyblock_players (uuid, name, island_id) VALUES (?, ?, ?) " +
                "ON CONFLICT (uuid) DO UPDATE SET name = EXCLUDED.name, island_id = EXCLUDED.island_id";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, player.getUuid().toString());
            ps.setString(2, player.getName());

            if (player.getIslandId() != null) {
                ps.setString(3, player.getIslandId().toString());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }

            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving player " + player.getUuid() + ": " + e.getMessage());
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
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(islandQuery); ResultSet rs = ps.executeQuery()) {
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
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(playerQuery); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SkyblockPlayer player = mapResultSetToPlayer(rs);
                playersCache.put(player.getUuid(), player);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading all players: " + e.getMessage());
        }
        plugin.getLogger().info(playersCache.size() + " players loaded!");
    }

    // RÉINTÉGRÉ
    public void clearCache() {
        islandsCache.clear();
        playersCache.clear();
    }

    // RÉINTÉGRÉ
    public void reloadFromDisk() {
        plugin.getLogger().info("Reloading all data from database...");
        clearCache();
        loadAll();
        plugin.getLogger().info("Reload complete!");
    }

    // --- Méthodes de Statistiques et de Recherche ---

    // RÉINTÉGRÉ
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

    // RÉINTÉGRÉ
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

    // RÉINTÉGRÉ
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

    // RÉINTÉGRÉ
    public int getTotalIslands() {
        String query = "SELECT COUNT(*) FROM islands";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting total islands count: " + e.getMessage());
        }
        return 0;
    }

    // RÉINTÉGRÉ
    public int getTotalPlayers() {
        String query = "SELECT COUNT(*) FROM skyblock_players";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting total players count: " + e.getMessage());
        }
        return 0;
    }

    // RÉINTÉGRÉ
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
        island.setLastActivity(rs.getLong("last_activity"));

        String membersJson = rs.getString("members");
        if(membersJson != null && !membersJson.isEmpty()){
            Type memberListType = new TypeToken<Set<UUID>>() {}.getType();
            Set<UUID> members = gson.fromJson(membersJson, memberListType);
            if(members != null) {
                members.forEach(island::addMember);
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

        return player;
    }
}