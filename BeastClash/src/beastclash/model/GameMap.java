package beastclash.model;

/**
 * GameMap – merepresentasikan satu map/dunia dalam Beast Clash.
 *
 * Setiap map punya:
 *   - name           : nama map (dipakai juga sebagai kunci efek map di BattlePanel)
 *   - enemyElement   : elemen musuh dominan di map ini
 *   - maxLevels      : jumlah level yang harus diselesaikan untuk unlock map berikutnya
 *   - completedLevels: berapa level sudah selesai
 *   - unlocked       : apakah map sudah bisa dimainkan
 */
public class GameMap {

    private final String name;
    private final String enemyElement;
    private final int    maxLevels;
    private int     completedLevels;
    private boolean unlocked;

    public GameMap(String name, String enemyElement, int maxLevels, boolean unlocked) {
        this.name           = name;
        this.enemyElement   = enemyElement;
        this.maxLevels      = maxLevels;
        this.completedLevels = 0;
        this.unlocked       = unlocked;
    }

    // ── Progress ──────────────────────────────────────────────────────────────

    /** Tandai satu level selesai (tidak melebihi maxLevels). */
    public void completeLevel() {
        if (completedLevels < maxLevels) completedLevels++;
    }

    public boolean isFullyCompleted() {
        return completedLevels >= maxLevels;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getName()            { return name; }
    public String getEnemyElement()    { return enemyElement; }
    public int    getMaxLevels()       { return maxLevels; }
    public int    getCompletedLevels() { return completedLevels; }
    public void   setCompletedLevels(int v) { this.completedLevels = Math.max(0, Math.min(maxLevels, v)); }

    public boolean isUnlocked()        { return unlocked; }
    public void    setUnlocked(boolean v) { this.unlocked = v; }

    @Override
    public String toString() {
        return name + " [" + enemyElement + "] "
                + completedLevels + "/" + maxLevels
                + (unlocked ? " (TERBUKA)" : " (TERKUNCI)");
    }
}
