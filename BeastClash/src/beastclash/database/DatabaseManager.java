package beastclash.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DatabaseManager – koneksi ke XAMPP MySQL (localhost:3306).
 *
 * Tabel yang dibuat otomatis:
 *   users       : id, username, password, eggs, created_at
 *   map_progress: id, user_id, map_index, completed_levels, unlocked
 *   beast_owned : id, user_id, beast_id
 *
 * Cara pakai XAMPP:
 *   1. Jalankan XAMPP → Start Apache & MySQL
 *   2. Buka phpMyAdmin → buat database bernama "beastclash"
 *   3. Driver MySQL Connector/J sudah di-bundle atau tambahkan
 *      mysql-connector-j-8.x.x.jar ke Libraries project NetBeans.
 */
public class DatabaseManager {

    private static DatabaseManager instance;

    // ── Konfigurasi koneksi ──────────────────────────────────────────────────
    private static final String DB_URL  = "jdbc:mysql://localhost:3306/beastclash?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";   // kosong = default XAMPP

    private Connection conn;
    private boolean connected = false;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    // =========================================================================
    //  KONEKSI
    // =========================================================================
    public boolean connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn      = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            connected = true;
            createTablesIfNotExist();
            return true;
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] MySQL driver tidak ditemukan. Tambahkan mysql-connector-j ke Libraries.");
            return false;
        } catch (SQLException e) {
            System.err.println("[DB] Gagal konek ke database: " + e.getMessage());
            return false;
        }
    }

    public boolean isConnected() {
        try { return connected && conn != null && !conn.isClosed(); }
        catch (SQLException e) { return false; }
    }

    public void disconnect() {
        try { if (conn != null) conn.close(); }
        catch (SQLException ignored) {}
        connected = false;
    }

    // =========================================================================
    //  BUAT TABEL
    // =========================================================================
    private void createTablesIfNotExist() throws SQLException {
        Statement st = conn.createStatement();

        // Tabel users
        st.execute(
            "CREATE TABLE IF NOT EXISTS users (" +
                "id         INT AUTO_INCREMENT PRIMARY KEY," +
                "username   VARCHAR(50)  NOT NULL UNIQUE," +
                "password   VARCHAR(255) NOT NULL," +
                "eggs       INT DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")");

        // Tabel map_progress
        st.execute(
            "CREATE TABLE IF NOT EXISTS map_progress (" +
                "id               INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id          INT NOT NULL," +
                "map_index        INT NOT NULL," +
                "completed_levels INT DEFAULT 0," +
                "unlocked         TINYINT(1) DEFAULT 0," +
                "UNIQUE KEY uq_user_map (user_id, map_index)," +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
            ")");

        // Tabel beast yang dimiliki
        st.execute(
            "CREATE TABLE IF NOT EXISTS beast_owned (" +
                "id       INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id  INT NOT NULL," +
                "beast_id INT NOT NULL," +
                "UNIQUE KEY uq_user_beast (user_id, beast_id)," +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
            ")");

        st.close();
    }

    // =========================================================================
    //  USER – Register & Login
    // =========================================================================
    /** Register user baru. Return: user id jika sukses, -1 jika gagal. */
    public int register(String username, String password) {
        if (!isConnected()) return -1;
        try {
            // Cek username sudah ada
            PreparedStatement check = conn.prepareStatement(
                "SELECT id FROM users WHERE username = ?");
            check.setString(1, username);
            ResultSet rs = check.executeQuery();
            if (rs.next()) { rs.close(); check.close(); return -2; } // duplikat
            rs.close(); check.close();

            // Insert user baru
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (username, password, eggs) VALUES (?, ?, 0)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, hashPassword(password));
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            int uid = keys.next() ? keys.getInt(1) : -1;
            keys.close(); ps.close();

            if (uid > 0) {
                // Inisialisasi map progress (map 0 = Grass Land unlocked)
                initMapProgress(uid);
                // Beri 7 beast awal (id 1-7)
                initStarterBeasts(uid);
            }
            return uid;
        } catch (SQLException e) {
            System.err.println("[DB] Register error: " + e.getMessage());
            return -1;
        }
    }

    /** Login. Return user_id jika sukses, -1 jika gagal. */
    public int login(String username, String password) {
        if (!isConnected()) return -1;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, password FROM users WHERE username = ?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) { rs.close(); ps.close(); return -1; }
            int uid  = rs.getInt("id");
            String hashed = rs.getString("password");
            rs.close(); ps.close();
            return hashPassword(password).equals(hashed) ? uid : -1;
        } catch (SQLException e) {
            System.err.println("[DB] Login error: " + e.getMessage());
            return -1;
        }
    }

    // =========================================================================
    //  EGGS
    // =========================================================================
    public int getEggs(int userId) {
        if (!isConnected()) return 0;
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT eggs FROM users WHERE id = ?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            int eggs = rs.next() ? rs.getInt("eggs") : 0;
            rs.close(); ps.close();
            return eggs;
        } catch (SQLException e) { return 0; }
    }

    public void addEggs(int userId, int amount) {
        if (!isConnected()) return;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET eggs = eggs + ? WHERE id = ?");
            ps.setInt(1, amount);
            ps.setInt(2, userId);
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { System.err.println("[DB] addEggs: " + e.getMessage()); }
    }

    public boolean spendEgg(int userId) {
        if (!isConnected()) return false;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET eggs = eggs - 1 WHERE id = ? AND eggs > 0");
            ps.setInt(1, userId);
            int rows = ps.executeUpdate(); ps.close();
            return rows > 0;
        } catch (SQLException e) { return false; }
    }

    // =========================================================================
    //  MAP PROGRESS
    // =========================================================================
    private void initMapProgress(int userId) throws SQLException {
        // 4 map; map 0 = unlocked
        for (int i = 0; i < 4; i++) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT IGNORE INTO map_progress (user_id, map_index, completed_levels, unlocked) VALUES (?,?,0,?)");
            ps.setInt(1, userId);
            ps.setInt(2, i);
            ps.setInt(3, i == 0 ? 1 : 0);
            ps.executeUpdate(); ps.close();
        }
    }

    /** Return array [completedLevels, unlocked(0/1)] per map index */
    public int[][] getMapProgress(int userId) {
        int[][] prog = new int[4][2];
        if (!isConnected()) return prog;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT map_index, completed_levels, unlocked FROM map_progress WHERE user_id = ? ORDER BY map_index");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int idx = rs.getInt("map_index");
                if (idx < 4) {
                    prog[idx][0] = rs.getInt("completed_levels");
                    prog[idx][1] = rs.getInt("unlocked");
                }
            }
            rs.close(); ps.close();
        } catch (SQLException e) { System.err.println("[DB] getMapProgress: " + e.getMessage()); }
        return prog;
    }

    public void saveMapProgress(int userId, int mapIndex, int completedLevels, boolean unlocked) {
        if (!isConnected()) return;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO map_progress (user_id, map_index, completed_levels, unlocked) VALUES (?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE completed_levels=VALUES(completed_levels), unlocked=VALUES(unlocked)");
            ps.setInt(1, userId);
            ps.setInt(2, mapIndex);
            ps.setInt(3, completedLevels);
            ps.setInt(4, unlocked ? 1 : 0);
            ps.executeUpdate(); ps.close();
        } catch (SQLException e) { System.err.println("[DB] saveMapProgress: " + e.getMessage()); }
    }

    // =========================================================================
    //  BEAST OWNED
    // =========================================================================
    private void initStarterBeasts(int userId) throws SQLException {
        // Beast starter: id 1,2,5,6,9,13,17 (satu dari setiap elemen + 1 bonus)
        int[] starters = {1, 2, 5, 6, 9, 13, 17};
        for (int bid : starters) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT IGNORE INTO beast_owned (user_id, beast_id) VALUES (?,?)");
            ps.setInt(1, userId);
            ps.setInt(2, bid);
            ps.executeUpdate(); ps.close();
        }
    }

    public List<Integer> getOwnedBeastIds(int userId) {
        List<Integer> ids = new ArrayList<>();
        if (!isConnected()) {
            // Fallback: berikan semua 24 jika tidak ada DB
            for (int i = 1; i <= 24; i++) ids.add(i);
            return ids;
        }
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT beast_id FROM beast_owned WHERE user_id = ? ORDER BY beast_id");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("beast_id"));
            rs.close(); ps.close();
        } catch (SQLException e) { System.err.println("[DB] getOwnedBeasts: " + e.getMessage()); }
        return ids;
    }

    public boolean unlockBeast(int userId, int beastId) {
        if (!isConnected()) return false;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT IGNORE INTO beast_owned (user_id, beast_id) VALUES (?,?)");
            ps.setInt(1, userId);
            ps.setInt(2, beastId);
            int rows = ps.executeUpdate(); ps.close();
            return rows > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean ownsAllBeasts(int userId) {
        return getOwnedBeastIds(userId).size() >= 24;
    }

    // =========================================================================
    //  USERNAME
    // =========================================================================
    public String getUsername(int userId) {
        if (!isConnected()) return "Player";
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT username FROM users WHERE id = ?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            String name = rs.next() ? rs.getString("username") : "Player";
            rs.close(); ps.close();
            return name;
        } catch (SQLException e) { return "Player"; }
    }

    // =========================================================================
    //  HELPER – Password hash sederhana (SHA-256)
    // =========================================================================
    private String hashPassword(String plain) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return plain; }
    }
}
