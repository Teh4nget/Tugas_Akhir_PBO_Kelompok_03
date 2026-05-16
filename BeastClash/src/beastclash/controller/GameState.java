package beastclash.controller;

import beastclash.data.BeastData;
import beastclash.data.MapData;
import beastclash.database.DatabaseManager;
import beastclash.model.Beast;
import beastclash.model.GameMap;
import java.util.ArrayList;
import java.util.List;

public class GameState {
    private static GameState instance;

    private List<GameMap> maps;
    private List<Beast> playerTeam;
    private List<Beast> enemyTeam;
    private GameMap selectedMap;
    private int currentLevel;
    private int activeBeastIndex;
    private int activeEnemyIndex;

    // Auth & progress
    private int currentUserId = -1;

    // Cache owned beast IDs agar tidak query DB berulang kali
    private List<Integer> cachedOwnedBeastIds = null;

    private GameState() {
        maps = MapData.getMaps();
        playerTeam = new ArrayList<>();
        enemyTeam  = new ArrayList<>();
        currentLevel     = 1;
        activeBeastIndex = 0;
        activeEnemyIndex = 0;
    }

    public static GameState getInstance() {
        if (instance == null) instance = new GameState();
        return instance;
    }

    // ── Auth ──────────────────────────────────────────────────────────────────
    // Telur in-memory untuk mode offline
    private int offlineEggs = 0;
    // Beast yang di-unlock lewat gacha offline (sesi ini saja)
    private List<Integer> offlineOwnedIds = null;

    public int getCurrentUserId() { return currentUserId; }

    public void setCurrentUserId(int uid) {
        this.currentUserId = uid;
        this.cachedOwnedBeastIds = null;
        this.offlineEggs = 0;
        this.offlineOwnedIds = null; // reset saat ganti user/session
    }

    /** Tambah beast ID ke daftar milik offline (dipanggil oleh GachaSystem). */
    public void addOfflineOwnedBeast(int beastId) {
        if (offlineOwnedIds == null) {
            offlineOwnedIds = new ArrayList<>(BeastData.getStarterIds());
        }
        if (!offlineOwnedIds.contains(beastId)) {
            offlineOwnedIds.add(beastId);
        }
    }

    /** Kembalikan list ID beast yang dimiliki di mode offline (starter + hasil gacha). */
    public List<Integer> getOfflineOwnedIds() {
        if (offlineOwnedIds == null) {
            offlineOwnedIds = new ArrayList<>(BeastData.getStarterIds());
        }
        return offlineOwnedIds;
    }

    // ── Egg management ────────────────────────────────────────────────────────
    public void addEggReward(int amount) {
        if (currentUserId > 0 && DatabaseManager.getInstance().isConnected()) {
            DatabaseManager.getInstance().addEggs(currentUserId, amount);
        } else {
            // Mode offline: simpan di memori
            offlineEggs += amount;
        }
    }

    public int getOfflineEggs() { return offlineEggs; }

    public boolean spendOfflineEgg() {
        if (offlineEggs <= 0) return false;
        offlineEggs--;
        return true;
    }

    // ── Invalidate cache (dipanggil setelah gacha unlock beast baru) ──────────
    public void invalidateBeastCache() {
        cachedOwnedBeastIds = null;
    }

    /**
     * Load map progress + owned beasts dari DB setelah login.
     * FIX: method ini kini juga me-reset maps agar progress benar-benar
     * dimuat ulang dari DB, mencegah data lama tersisa di memori.
     */
    public void loadProgressFromDB() {
        if (currentUserId <= 0) return;
        DatabaseManager db = DatabaseManager.getInstance();
        if (!db.isConnected()) return;

        // Reset maps ke kondisi awal sebelum load
        maps = MapData.getMaps();

        // Load map progress dari DB
        int[][] prog = db.getMapProgress(currentUserId);
        for (int i = 0; i < maps.size() && i < prog.length; i++) {
            GameMap m = maps.get(i);
            m.setUnlocked(prog[i][1] == 1);
            // FIX: gunakan setCompletedLevels langsung (lebih aman, tidak ada loop)
            m.setCompletedLevels(prog[i][0]);
        }

        // Load & cache owned beast IDs
        cachedOwnedBeastIds = db.getOwnedBeastIds(currentUserId);
    }

    /** Simpan progress map ke DB setelah level selesai. */
    public void saveMapProgressToDB() {
        if (currentUserId <= 0) return;
        DatabaseManager db = DatabaseManager.getInstance();
        if (!db.isConnected()) return;
        for (int i = 0; i < maps.size(); i++) {
            GameMap m = maps.get(i);
            db.saveMapProgress(currentUserId, i, m.getCompletedLevels(), m.isUnlocked());
        }
    }

    // ── Beast ─────────────────────────────────────────────────────────────────
    /**
     * Return list beast yang boleh dipilih oleh user (owned).
     * FIX: gunakan cachedOwnedBeastIds jika tersedia agar konsisten,
     *      dan tidak query DB berulang kali.
     */
    public List<Beast> getAvailableBeasts() {
        List<Beast> all = BeastData.getAllBeasts();
        List<Integer> owned;

        if (currentUserId > 0 && DatabaseManager.getInstance().isConnected()) {
            // Online: query DB
            if (cachedOwnedBeastIds == null) {
                cachedOwnedBeastIds = DatabaseManager.getInstance().getOwnedBeastIds(currentUserId);
            }
            owned = cachedOwnedBeastIds;
        } else {
            // Offline: starter + beast yang sudah di-unlock lewat gacha sesi ini
            owned = getOfflineOwnedIds();
        }

        List<Beast> available = new ArrayList<>();
        for (Beast b : all) {
            if (owned.contains(b.getId())) available.add(b);
        }
        return available;
    }

    // ── Battle reset ──────────────────────────────────────────────────────────
    public void resetBattle() {
        activeBeastIndex = 0;
        activeEnemyIndex = 0;
        for (Beast b : playerTeam) b.reset();
        for (Beast b : enemyTeam) b.reset();
    }

    public Beast getActiveBeast() {
        for (int i = activeBeastIndex; i < playerTeam.size(); i++) {
            if (playerTeam.get(i).isAlive()) { activeBeastIndex = i; return playerTeam.get(i); }
        }
        return null;
    }

    public Beast getActiveEnemy() {
        for (int i = activeEnemyIndex; i < enemyTeam.size(); i++) {
            if (enemyTeam.get(i).isAlive()) { activeEnemyIndex = i; return enemyTeam.get(i); }
        }
        return null;
    }

    public void nextAliveEnemy() { activeEnemyIndex++; }

    public boolean isPlayerDefeated() {
        for (Beast b : playerTeam) if (b.isAlive()) return false;
        return true;
    }

    public boolean isEnemyDefeated() {
        for (Beast b : enemyTeam) if (b.isAlive()) return false;
        return true;
    }

    public void switchActiveBeast(int index) {
        if (index >= 0 && index < playerTeam.size() && playerTeam.get(index).isAlive())
            activeBeastIndex = index;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public List<GameMap> getMaps()            { return maps; }
    public List<Beast>   getPlayerTeam()      { return playerTeam; }
    public void setPlayerTeam(List<Beast> t)  { this.playerTeam = t; }
    public List<Beast>   getEnemyTeam()       { return enemyTeam; }
    public void setEnemyTeam(List<Beast> t)   { this.enemyTeam = t; }
    public GameMap getSelectedMap()           { return selectedMap; }
    public void setSelectedMap(GameMap m)     { this.selectedMap = m; }
    public int getCurrentLevel()              { return currentLevel; }
    public void setCurrentLevel(int l)        { this.currentLevel = l; }
    public int getActiveBeastIndex()          { return activeBeastIndex; }
    public int getActiveEnemyIndex()          { return activeEnemyIndex; }
}
