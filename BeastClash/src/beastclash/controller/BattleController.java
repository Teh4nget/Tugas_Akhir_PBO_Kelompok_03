package beastclash.controller;

import beastclash.audio.SoundManager;
import beastclash.model.Beast;
import java.util.*;

/**
 * BattleController – Turn-based battle engine (HSR-style).
 *
 * SISTEM TURN:
 *  - Semua beast (player + enemy) masuk antrian berdasarkan Speed.
 *  - Setiap "tick" kita hitung siapa yang paling cepat mencapai threshold ACTION_POINT.
 *  - Beast dengan speed tinggi mendapat giliran lebih sering.
 *  - Satu giliran = satu aksi (attack/skill/ultimate), lalu giliran pindah.
 *
 * MULTI-ENEMY:
 *  - Enemy team bisa 1–5 beast.
 *  - Enemy memilih target player secara acak (yang masih hidup).
 *  - Player memilih target enemy mana yang ingin diserang.
 */
public class BattleController {

    public static final int ACTION_POINT = 10000; // threshold giliran

    private final GameState state;
    private final Random    rng = new Random();

    // Antrian turn: setiap entry = [beast_index, isEnemy(0/1), actionValue]
    // actionValue naik setiap tick sebesar Speed beast; giliran saat >= ACTION_POINT
    private final List<TurnEntry> turnQueue = new ArrayList<>();
    private int currentTurnIdx = 0; // indeks di turnQueue yang sedang giliran

    public BattleController() {
        this.state = GameState.getInstance();
    }

    // ── Inisialisasi antrian turn ─────────────────────────────────────────────
    public void initTurnQueue() {
        turnQueue.clear();
        List<Beast> players = state.getPlayerTeam();
        List<Beast> enemies = state.getEnemyTeam();

        // Tambahkan semua beast ke antrian
        for (int i = 0; i < players.size(); i++) {
            Beast b = players.get(i);
            // Nilai awal acak agar tidak semua mulai di 0
            int initVal = (int)(rng.nextDouble() * b.getSpeed() * 20);
            turnQueue.add(new TurnEntry(i, false, b, initVal));
        }
        for (int i = 0; i < enemies.size(); i++) {
            Beast b = enemies.get(i);
            int initVal = (int)(rng.nextDouble() * b.getSpeed() * 20);
            turnQueue.add(new TurnEntry(i, true, b, initVal));
        }

        advanceToNextTurn();
    }

    /**
     * Hitung siapa yang mendapat giliran berikutnya berdasarkan speed.
     * Naikkan actionValue semua beast sampai ada yang mencapai ACTION_POINT.
     */
    public void advanceToNextTurn() {
        // Hapus beast yang sudah mati dari antrian
        turnQueue.removeIf(e -> !e.beast.isAlive());

        if (turnQueue.isEmpty()) return;

        // Cari nilai minimum yang dibutuhkan untuk mencapai ACTION_POINT
        int maxVal = turnQueue.stream().mapToInt(e -> e.actionValue).max().orElse(0);
        if (maxVal >= ACTION_POINT) {
            // Sudah ada yang siap; tidak perlu advance
        } else {
            // Simulasikan tick sampai ada yang mencapai threshold
            while (true) {
                for (TurnEntry e : turnQueue) {
                    e.actionValue += e.beast.getSpeed();
                }
                if (turnQueue.stream().anyMatch(e -> e.actionValue >= ACTION_POINT)) break;
            }
        }

        // Urutkan: yang paling tinggi actionValue duluan; jika sama, speed lebih tinggi duluan
        turnQueue.sort((a, b) -> {
            if (b.actionValue != a.actionValue) return b.actionValue - a.actionValue;
            return b.beast.getSpeed() - a.beast.getSpeed();
        });

        currentTurnIdx = 0;
    }

    /** Entry beast aktif yang sedang giliran */
    public TurnEntry getCurrentTurn() {
        if (turnQueue.isEmpty()) return null;
        // Pastikan currentTurnIdx valid
        while (currentTurnIdx < turnQueue.size() && !turnQueue.get(currentTurnIdx).beast.isAlive()) {
            currentTurnIdx++;
        }
        return currentTurnIdx < turnQueue.size() ? turnQueue.get(currentTurnIdx) : null;
    }

    /** Snapshot antrian turn saat ini (untuk UI action order) */
    public List<TurnEntry> getTurnQueueSnapshot() {
        return Collections.unmodifiableList(turnQueue);
    }

    // ── Setelah satu turn selesai ─────────────────────────────────────────────
    private void endTurn(TurnEntry actor) {
        // Kurangi actionValue aktor (bukan reset ke 0 agar sistem proporsional)
        actor.actionValue -= ACTION_POINT;

        // Pulihkan mana
        if (!actor.isEnemy) {
            if (actor.beast.isAlive()) actor.beast.restoreMana(8);
        } else {
            if (actor.beast.isAlive()) actor.beast.restoreMana(10);
        }

        advanceToNextTurn();
    }

    // ── AKSI PLAYER ──────────────────────────────────────────────────────────

    /**
     * Lakukan serangan normal. targetEnemyIndex = indeks di enemyTeam.
     */
    public BattleResult performAttack(int targetEnemyIdx) {
        TurnEntry actor  = getCurrentTurn();
        if (actor == null || actor.isEnemy)
            return new BattleResult("Bukan giliran player!\n", false, false, false);

        Beast player = actor.beast;
        List<Beast> enemies = state.getEnemyTeam();
        if (targetEnemyIdx < 0 || targetEnemyIdx >= enemies.size())
            return new BattleResult("Target tidak valid!\n", false, false, false);
        Beast target = enemies.get(targetEnemyIdx);
        if (!target.isAlive())
            return new BattleResult("Target sudah dikalahkan!\n", false, false, false);

        StringBuilder log = new StringBuilder();
        int dmg = player.calculateAttackDamage(target);
        target.takeDamage(dmg);
        SoundManager.getInstance().playSFX("ATTACK");
        log.append(String.format("⚔️ %s menyerang %s! (%d damage)\n",
            player.getName(), target.getName(), dmg));
        if (!target.isAlive())
            log.append(String.format("💥 %s dikalahkan!\n", target.getName()));

        endTurn(actor);
        return buildResult(log);
    }

    /**
     * Skill (30 MP): serangan 1.5x ke satu target.
     */
    public BattleResult performSkill(int targetEnemyIdx) {
        TurnEntry actor = getCurrentTurn();
        if (actor == null || actor.isEnemy)
            return new BattleResult("Bukan giliran player!\n", false, false, false);

        Beast player = actor.beast;
        if (player.getCurrentMana() < 30)
            return new BattleResult("❌ Mana tidak cukup untuk Skill! (butuh 30 MP)\n",
                false, false, false);

        List<Beast> enemies = state.getEnemyTeam();
        if (targetEnemyIdx < 0 || targetEnemyIdx >= enemies.size())
            return new BattleResult("Target tidak valid!\n", false, false, false);
        Beast target = enemies.get(targetEnemyIdx);
        if (!target.isAlive())
            return new BattleResult("Target sudah dikalahkan!\n", false, false, false);

        StringBuilder log = new StringBuilder();
        player.useMana(30);
        int dmg = player.calculateSkillDamage(target);
        target.takeDamage(dmg);
        SoundManager.getInstance().playSFX("SKILL");
        log.append(String.format("✨ %s Skill → %s! (%d damage)\n",
            player.getName(), target.getName(), dmg));
        if (!target.isAlive())
            log.append(String.format("💥 %s dikalahkan!\n", target.getName()));

        endTurn(actor);
        return buildResult(log);
    }

    /**
     * Ultimate (70 MP): serang SEMUA musuh.
     */
    public BattleResult performUltimate() {
        TurnEntry actor = getCurrentTurn();
        if (actor == null || actor.isEnemy)
            return new BattleResult("Bukan giliran player!\n", false, false, false);

        Beast player = actor.beast;
        if (player.getCurrentMana() < 70)
            return new BattleResult("❌ Mana tidak cukup untuk Ultimate! (butuh 70 MP)\n",
                false, false, false);

        StringBuilder log = new StringBuilder();
        player.useMana(70);
        SoundManager.getInstance().playSFX("ULTIMATE");
        log.append(String.format("💥 %s melepas ULTIMATE – semua musuh terkena!\n", player.getName()));

        for (Beast e : state.getEnemyTeam()) {
            if (e.isAlive()) {
                int dmg = player.calculateUltimateDamage(e);
                e.takeDamage(dmg);
                log.append(String.format("   💢 %s terkena %d damage!\n", e.getName(), dmg));
                if (!e.isAlive())
                    log.append(String.format("   💥 %s dikalahkan!\n", e.getName()));
            }
        }

        endTurn(actor);
        return buildResult(log);
    }

    /** Kabur: 50% berhasil */
    public BattleResult performRun() {
        StringBuilder log = new StringBuilder();
        if (rng.nextDouble() < 0.5) {
            SoundManager.getInstance().playSFX("RUN");
            log.append("[RUN_SUCCESS] Berhasil kabur dari pertarungan!\n");
            return new BattleResult(log.toString(), false, false, false);
        }
        log.append("❌ Gagal kabur! Musuh memblokir jalan!\n");
        // Tidak endTurn – giliran tetap milik player (bisa coba lagi / pilih aksi lain)
        return new BattleResult(log.toString(), false, false, false);
    }

    // ── AKSI ENEMY (otomatis) ─────────────────────────────────────────────────

    /**
     * Jalankan giliran enemy secara otomatis. Dipanggil BattlePanel
     * saat getCurrentTurn().isEnemy == true.
     */
    public BattleResult performEnemyTurn() {
        TurnEntry actor = getCurrentTurn();
        if (actor == null || !actor.isEnemy)
            return new BattleResult("Bukan giliran enemy!\n", false, false, false);

        Beast enemy = actor.beast;
        StringBuilder log = new StringBuilder();

        // Pilih target player secara acak (yang masih hidup)
        List<Beast> alive = new ArrayList<>();
        for (Beast p : state.getPlayerTeam()) if (p.isAlive()) alive.add(p);
        if (alive.isEmpty()) {
            endTurn(actor);
            return buildResult(log);
        }
        Beast target = alive.get(rng.nextInt(alive.size()));

        // Enemy: 20% skill jika cukup mana, sisanya attack biasa
        int dmg;
        if (enemy.getCurrentMana() >= 30 && rng.nextDouble() < 0.2) {
            enemy.useMana(30);
            dmg = enemy.calculateSkillDamage(target);
            SoundManager.getInstance().playSFX("SKILL");
            log.append(String.format("👹 %s menggunakan Skill ke %s! (%d damage)\n",
                enemy.getName(), target.getName(), dmg));
        } else {
            dmg = enemy.calculateAttackDamage(target);
            SoundManager.getInstance().playSFX("HURT");
            log.append(String.format("👹 %s menyerang %s! (%d damage)\n",
                enemy.getName(), target.getName(), dmg));
        }
        target.takeDamage(dmg);
        if (!target.isAlive())
            log.append(String.format("💀 %s pingsan!\n", target.getName()));

        endTurn(actor);
        return buildResult(log);
    }

    // ── Build Result ──────────────────────────────────────────────────────────
    private BattleResult buildResult(StringBuilder log) {
        boolean allEnemyDefeated  = state.isEnemyDefeated();
        boolean allPlayerDefeated = state.isPlayerDefeated();
        if (allEnemyDefeated)  SoundManager.getInstance().playSFX("VICTORY_SFX");
        if (allPlayerDefeated) SoundManager.getInstance().playSFX("DEFEAT");
        // playerFainted: cek apakah ada player yang baru saja mati
        boolean playerFainted = state.getPlayerTeam().stream()
            .anyMatch(b -> !b.isAlive());
        return new BattleResult(log.toString(), playerFainted, allEnemyDefeated, allPlayerDefeated);
    }

    // ── TurnEntry ─────────────────────────────────────────────────────────────
    public static class TurnEntry {
        public final int    teamIndex;  // indeks di playerTeam / enemyTeam
        public final boolean isEnemy;
        public final Beast  beast;
        public int          actionValue;

        public TurnEntry(int teamIndex, boolean isEnemy, Beast beast, int initVal) {
            this.teamIndex   = teamIndex;
            this.isEnemy     = isEnemy;
            this.beast       = beast;
            this.actionValue = initVal;
        }
    }

    // ── BattleResult DTO ──────────────────────────────────────────────────────
    public static class BattleResult {
        public String  log;
        public boolean playerFainted;
        public boolean allEnemyDefeated;
        public boolean allPlayerDefeated;

        public BattleResult(String log, boolean playerFainted,
                            boolean allEnemyDefeated, boolean allPlayerDefeated) {
            this.log              = log;
            this.playerFainted    = playerFainted;
            this.allEnemyDefeated = allEnemyDefeated;
            this.allPlayerDefeated = allPlayerDefeated;
        }
    }
}
