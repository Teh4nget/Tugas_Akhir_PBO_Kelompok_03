package beastclash.view;

import beastclash.audio.SoundManager;
import beastclash.controller.BattleController;
import beastclash.controller.BattleController.TurnEntry;
import beastclash.controller.GameState;
import beastclash.model.Beast;
import beastclash.model.GameMap;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * BattlePanel – HSR-style turn-based battle UI.
 *
 * Fitur:
 *  • Turn order bar (seperti HSR) di bagian atas arena — menampilkan urutan giliran semua beast.
 *  • Multi-enemy: 5 player beast vs N enemy beast (1–5).
 *  • Player pilih target enemy sebelum menyerang.
 *  • Enemy turn otomatis dengan delay animasi.
 *  • Sistem map debuff tetap ada.
 *  • Blizzard freeze, periodic damage tetap berjalan.
 */
public class BattlePanel extends JPanel {

    private final MainFrame    frame;
    private final GameState    state;
    private final BattleController battle;
    private final Random       rng = new Random();

    // ── UI ────────────────────────────────────────────────────────────────────
    private ActionOrderBar  actionOrderBar;   // turn order strip di atas
    private BattleArena     arenaPanel;
    private JTextArea       battleLog;

    // Info panel atas: map + level
    private JLabel lblMapInfo;

    // Panel stat player aktif (bawah kiri)
    private JLabel lblPlayerName, lblPlayerElem;
    private HPBar  hpBarPlayer, mpBarPlayer;

    // Target selector (enemy yang dipilih player)
    private int    selectedEnemyIdx = 0;
    private JButton[] enemyTargetBtns;
    private JPanel    enemyTargetPanel;

    // Tombol aksi
    private JButton btnAttack, btnSkill, btnUltimate, btnRun;

    // Efek map
    private JLabel  mapEffectLabel;
    private Timer   mapDamageTimer;
    private boolean playerFrozen  = false;
    private Timer   freezeTimer;
    private int     attackCounter = 0;

    private boolean battleEnded = false;
    private boolean enemyTurnPending = false; // mencegah klik ganda saat enemy turn

    // ─────────────────────────────────────────────────────────────────────────

    public BattlePanel(MainFrame frame) {
        this.frame  = frame;
        this.state  = GameState.getInstance();
        this.battle = new BattleController();
        setLayout(new BorderLayout(4, 4));
        setBackground(new Color(18, 18, 35));
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        buildUI();

        // Inisialisasi turn queue SETELAH UI siap
        battle.initTurnQueue();
        applyMapDebuffs();
        startMapEffects();

        refreshAll();
        checkIfEnemyTurn(); // mungkin enemy duluan jika speed lebih tinggi
    }

    // =========================================================================
    //  BUILD UI
    // =========================================================================
    private void buildUI() {
        // ── NORTH: Action Order Bar ───────────────────────────────────────────
        actionOrderBar = new ActionOrderBar();
        actionOrderBar.setPreferredSize(new Dimension(0, 64));
        add(actionOrderBar, BorderLayout.NORTH);

        // ── CENTER: Arena full width ──────────────────────────────────────────
        arenaPanel = new BattleArena();
        add(arenaPanel, BorderLayout.CENTER);

        // ── SOUTH: Control panel ──────────────────────────────────────────────
        add(buildSouthPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildSouthPanel() {
        JPanel south = new JPanel(new BorderLayout(6, 0));
        south.setBackground(new Color(18, 18, 35));
        south.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        // ── WEST: Stat player aktif ───────────────────────────────────────────
        JPanel statPanel = new JPanel(new GridLayout(4, 1, 1, 2));
        statPanel.setBackground(new Color(26, 26, 48));
        statPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 120), 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        statPanel.setPreferredSize(new Dimension(180, 0));

        lblPlayerName = new JLabel("–", SwingConstants.LEFT);
        lblPlayerName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPlayerName.setForeground(Color.WHITE);

        lblPlayerElem = new JLabel("", SwingConstants.LEFT);
        lblPlayerElem.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        hpBarPlayer = new HPBar("HP", 100, 100, new Color(80, 220, 80));
        mpBarPlayer = new HPBar("MP", 100, 100, new Color(80, 140, 240));

        statPanel.add(lblPlayerName);
        statPanel.add(lblPlayerElem);
        statPanel.add(hpBarPlayer);
        statPanel.add(mpBarPlayer);
        south.add(statPanel, BorderLayout.WEST);

        // ── CENTER: Target selector + map effect ──────────────────────────────
        JPanel midPanel = new JPanel(new BorderLayout(0, 3));
        midPanel.setBackground(new Color(18, 18, 35));

        mapEffectLabel = new JLabel(getMapEffectText(), SwingConstants.CENTER);
        mapEffectLabel.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        mapEffectLabel.setForeground(new Color(200, 180, 120));
        midPanel.add(mapEffectLabel, BorderLayout.NORTH);

        // Target enemy buttons
        enemyTargetPanel = new JPanel();
        enemyTargetPanel.setBackground(new Color(18, 18, 35));
        enemyTargetPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(180, 60, 60), 1),
            "TARGET MUSUH", 0, 0,
            new Font("Segoe UI", Font.BOLD, 9), new Color(200, 100, 100)));
        buildEnemyTargetButtons();
        midPanel.add(enemyTargetPanel, BorderLayout.CENTER);

        south.add(midPanel, BorderLayout.CENTER);

        // ── EAST: Action buttons ──────────────────────────────────────────────
        JPanel actionPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        actionPanel.setBackground(new Color(18, 18, 35));
        actionPanel.setPreferredSize(new Dimension(220, 0));
        actionPanel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

        btnAttack   = makeBtn("⚔ ATTACK",          new Color(190, 55, 55));
        btnSkill    = makeBtn("✨ SKILL (30MP)",    new Color(55, 100, 200));
        btnUltimate = makeBtn("💥 ULTIMATE (70MP)", new Color(130, 40, 180));
        btnRun      = makeBtn("🏃 RUN",             new Color(70, 70, 90));

        // Tooltip informasi tiap tombol aksi
        btnAttack.setToolTipText("<html><b>⚔ ATTACK</b><br>"
            + "Serang 1 musuh yang dipilih.<br>"
            + "Damage = ATK − DEF/2<br>"
            + "Tidak memerlukan MP.</html>");
        btnSkill.setToolTipText("<html><b>✨ SKILL</b> — Biaya: 30 MP<br>"
            + "Serang 1 musuh dengan kekuatan 1.5× ATK.<br>"
            + "Damage = (ATK × 1.5) − DEF/2<br>"
            + "Bonus elemen berlaku.</html>");
        btnUltimate.setToolTipText("<html><b>💥 ULTIMATE</b> — Biaya: 70 MP<br>"
            + "Serang SEMUA musuh sekaligus!<br>"
            + "Damage = (ATK × 2) − DEF/3 per musuh<br>"
            + "Sangat efektif melawan kelompok.</html>");
        btnRun.setToolTipText("<html><b>🏃 RUN</b><br>"
            + "Coba kabur dari pertarungan.<br>"
            + "Peluang berhasil: <b>50%</b><br>"
            + "Jika gagal, giliran tetap berlanjut.</html>");

        btnAttack  .addActionListener(e -> onPlayerAction("attack"));
        btnSkill   .addActionListener(e -> onPlayerAction("skill"));
        btnUltimate.addActionListener(e -> onPlayerAction("ultimate"));
        btnRun     .addActionListener(e -> onPlayerAction("run"));

        actionPanel.add(btnAttack);
        actionPanel.add(btnSkill);
        actionPanel.add(btnUltimate);
        actionPanel.add(btnRun);

        // ── Toggle LOG button ────────────────────────────────────────────────
        JButton btnToggleLog = new JButton("📋 LOG");
        btnToggleLog.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnToggleLog.setBackground(new Color(40, 50, 80));
        btnToggleLog.setForeground(new Color(180, 200, 240));
        btnToggleLog.setFocusPainted(false);
        btnToggleLog.setBorderPainted(false);
        btnToggleLog.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnToggleLog.setToolTipText("Tampilkan / sembunyikan log battle");
        btnToggleLog.setPreferredSize(new Dimension(70, 24));
        btnToggleLog.addActionListener(e -> toggleLogPanel());

        JPanel eastWrap = new JPanel(new BorderLayout(0, 3));
        eastWrap.setBackground(new Color(18, 18, 35));
        eastWrap.setPreferredSize(new Dimension(220, 0));
        eastWrap.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        eastWrap.add(btnToggleLog, BorderLayout.NORTH);
        eastWrap.add(actionPanel, BorderLayout.CENTER);
        south.add(eastWrap, BorderLayout.EAST);

        // Init battleLog (dipakai oleh floating log window)
        battleLog = new JTextArea();
        battleLog.setEditable(false);
        battleLog.setFont(new Font("Consolas", Font.PLAIN, 10));
        battleLog.setBackground(new Color(10, 10, 22));
        battleLog.setForeground(new Color(190, 210, 190));
        battleLog.setLineWrap(true);
        battleLog.setWrapStyleWord(true);
        battleLog.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        return south;
    }

    // ── Floating log panel ────────────────────────────────────────────────────
    private JWindow  logWindow;
    private boolean  logVisible = false;

    private void toggleLogPanel() {
        if (logWindow == null) buildLogWindow();
        logVisible = !logVisible;
        logWindow.setVisible(logVisible);
        if (logVisible) positionLogWindow();
    }

    private void buildLogWindow() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        logWindow = new JWindow(owner);
        logWindow.setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(20, 20, 45));
        header.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 6));

        JLabel lblTitle = new JLabel("📋 Battle Log");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(new Color(160, 200, 255));
        header.add(lblTitle, BorderLayout.WEST);

        JButton btnClose = new JButton("✕");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnClose.setBackground(new Color(20, 20, 45));
        btnClose.setForeground(new Color(180, 120, 120));
        btnClose.setBorderPainted(false);
        btnClose.setFocusPainted(false);
        btnClose.setContentAreaFilled(false);
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> { logVisible = false; logWindow.setVisible(false); });
        header.add(btnClose, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(battleLog);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(60, 80, 140), 1));
        scroll.setPreferredSize(new Dimension(320, 220));

        logWindow.add(header, BorderLayout.NORTH);
        logWindow.add(scroll, BorderLayout.CENTER);
        logWindow.setSize(320, 250);
        logWindow.getRootPane().setBorder(
            BorderFactory.createLineBorder(new Color(80, 100, 180), 1));
    }

    private void positionLogWindow() {
        if (logWindow == null) return;
        try {
            Point p = this.getLocationOnScreen();
            Dimension d = this.getSize();
            logWindow.setLocation(p.x + d.width - 330, p.y + 60);
        } catch (Exception ignored) {}
    }

    private void buildEnemyTargetButtons() {
        enemyTargetPanel.removeAll();
        List<Beast> enemies = state.getEnemyTeam();
        enemyTargetBtns = new JButton[enemies.size()];
        enemyTargetPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 4, 2));

        // Reset target ke enemy pertama yang masih hidup
        selectedEnemyIdx = 0;
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i).isAlive()) { selectedEnemyIdx = i; break; }
        }

        for (int i = 0; i < enemies.size(); i++) {
            final int idx = i;
            Beast e = enemies.get(i);
            String label = "<html><center>" + ElementColor.getEmoji(e.getElement())
                + "<br/><small>" + truncate(e.getName(), 7) + "</small></center></html>";
            JButton btn = new JButton(label);
            btn.setPreferredSize(new Dimension(58, 44));
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            btn.setFocusPainted(false);
            btn.setBorderPainted(true);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (e.isAlive()) {
                Color ec = ElementColor.getColor(e.getElement());
                btn.setBackground(ec.darker().darker());
                btn.setForeground(Color.WHITE);
                btn.addActionListener(ev -> { selectedEnemyIdx = idx; refreshTargetButtons(); });
            } else {
                btn.setBackground(new Color(50, 50, 50));
                btn.setForeground(Color.GRAY);
                btn.setEnabled(false);
            }
            enemyTargetBtns[i] = btn;
            enemyTargetPanel.add(btn);
        }
        refreshTargetButtons();
        enemyTargetPanel.revalidate();
        enemyTargetPanel.repaint();
    }

    private void refreshTargetButtons() {
        if (enemyTargetBtns == null) return;
        List<Beast> enemies = state.getEnemyTeam();
        for (int i = 0; i < enemyTargetBtns.length; i++) {
            if (i >= enemies.size()) break;
            Beast e = enemies.get(i);
            if (!e.isAlive()) {
                enemyTargetBtns[i].setEnabled(false);
                enemyTargetBtns[i].setBackground(new Color(50, 50, 50));
            } else {
                boolean sel = (i == selectedEnemyIdx);
                enemyTargetBtns[i].setBorder(sel
                    ? BorderFactory.createLineBorder(Color.YELLOW, 2)
                    : BorderFactory.createLineBorder(new Color(100, 100, 100), 1));
                Color ec = ElementColor.getColor(e.getElement());
                enemyTargetBtns[i].setBackground(sel ? ec.darker() : ec.darker().darker());
            }
        }
    }

    // =========================================================================
    //  PLAYER ACTION
    // =========================================================================
    private void onPlayerAction(String action) {
        if (battleEnded || enemyTurnPending) return;

        TurnEntry cur = battle.getCurrentTurn();
        if (cur == null || cur.isEnemy) {
            addLog("⏳ Bukan giliranmu!\n"); return;
        }
        if (playerFrozen && !action.equals("run")) {
            addLog("❄️ " + cur.beast.getName() + " sedang BEKU, tidak bisa menyerang!\n");
            return;
        }

        BattleController.BattleResult result;
        if (action.equals("run")) {
            result = battle.performRun();
        } else if (action.equals("ultimate")) {
            result = battle.performUltimate();
        } else {
            // Pastikan target valid
            List<Beast> enemies = state.getEnemyTeam();
            if (selectedEnemyIdx >= enemies.size() || !enemies.get(selectedEnemyIdx).isAlive()) {
                // Auto-pilih target hidup pertama
                for (int i = 0; i < enemies.size(); i++) {
                    if (enemies.get(i).isAlive()) { selectedEnemyIdx = i; break; }
                }
            }
            result = action.equals("skill")
                ? battle.performSkill(selectedEnemyIdx)
                : battle.performAttack(selectedEnemyIdx);
        }

        addLog(result.log);

        if (result.log.contains("[RUN_SUCCESS]")) {
            battleEnded = true;
            stopMapTimers();
            setActionsEnabled(false);
            new Timer(1000, e -> frame.showMapSelect()) {{ setRepeats(false); start(); }};
            return;
        }

        checkBlizzardFreeze();
        refreshAll();

        if (result.allEnemyDefeated) { handleVictory(); return; }
        if (result.allPlayerDefeated) { handleDefeat(); return; }

        // Lanjut ke giliran berikutnya (mungkin enemy)
        scheduleNextTurn();
    }

    // ── Giliran enemy (otomatis dengan delay) ─────────────────────────────────
    private void scheduleNextTurn() {
        TurnEntry next = battle.getCurrentTurn();
        if (next == null || !next.isEnemy) return; // giliran player, tunggu input

        // Enemy turn: jalankan otomatis setelah delay animasi
        enemyTurnPending = true;
        setActionsEnabled(false);
        addLog("━━ Giliran " + next.beast.getName() + " (musuh) ━━\n");

        new Timer(900, e -> {
            if (battleEnded) return;
            BattleController.BattleResult res = battle.performEnemyTurn();
            addLog(res.log);
            checkBlizzardFreeze();
            refreshAll();
            enemyTurnPending = false;

            if (res.allEnemyDefeated) { handleVictory(); return; }
            if (res.allPlayerDefeated) { handleDefeat(); return; }

            // Setelah enemy selesai, cek apakah masih ada enemy berturut-turut
            scheduleNextTurn();
            if (!enemyTurnPending) setActionsEnabled(true);
        }) {{ setRepeats(false); start(); }};
    }

    private void checkIfEnemyTurn() {
        TurnEntry cur = battle.getCurrentTurn();
        if (cur != null && cur.isEnemy) scheduleNextTurn();
    }

    // =========================================================================
    //  MAP EFFECTS
    // =========================================================================
    private void applyMapDebuffs() {
        GameMap map = state.getSelectedMap();
        if (map == null) return;
        switch (map.getName()) {
            case "Hutan Hijau":
                applyDebuff("Api",    0.80f, 1f,    1f, false);
                break;
            case "Desert":
                applyDebuff("Air",    1f,    0.75f, 1f, false);
                break;
            case "Lautan Biru":
                applyDebuff("Api",    0.70f, 0.80f, 1f, false);
                applyDebuff("Daun",   1f,    1f,    0.80f, false);
                break;
            case "Blizzard":
                applyDebuff("Api",    0.65f, 0.75f, 1f, false);
                applyDebuff("Daun",   0.80f, 1f,    1f, false);
                applyDebuff("Air",    1f,    1.10f, 1f, true);
                break;
            case "Volcano":
                applyDebuff("Air",    0.60f, 0.70f, 1f, false);
                applyDebuff("Tanah",  1f,    0.80f, 1f, false);
                applyDebuff("Api",    1.15f, 1f,    1f, true);
                break;
            case "Hutan Gelap":
                applyDebuff("Cahaya", 0.65f, 0.65f, 1f, false);
                applyDebuff("Gelap",  1.20f, 1.15f, 1f, true);
                for (String e : new String[]{"Api","Air","Tanah","Daun"})
                    applyDebuff(e, 1f, 0.90f, 1f, false);
                break;
        }
    }

    private void applyDebuff(String elem, float atk, float def, float spd, boolean isBonus) {
        List<Beast> team = state.getPlayerTeam();
        if (team == null) return;
        for (Beast b : team) {
            if (!b.getElement().equals(elem)) continue;
            if (atk != 1f) b.multiplyAttack(atk);
            if (def != 1f) b.multiplyDefense(def);
            if (spd != 1f) b.multiplySpeed(spd);
        }
    }

    private void startMapEffects() {
        GameMap map = state.getSelectedMap();
        if (map == null) return;
        String n = map.getName();

        switch (n) {
            case "Hutan Hijau":
                addLog("🌿 [MAP] Hutan Hijau: beast Api ATK-20%\n"); break;
            case "Desert":
                addLog("🌵 [MAP] Desert: -3HP/3dtk (imun:Api) | Air DEF-25%\n");
                startPeriodicDamage(3, 3000, "Api",
                    "🌵 [Desert] %s terkena 3 damage dari pasir!\n"); break;
            case "Lautan Biru":
                addLog("🌊 [MAP] Lautan: Api ATK/DEF↓ | Daun SPD-20%\n"); break;
            case "Blizzard":
                addLog("❄️ [MAP] Blizzard: Api ATK/DEF↓ | Air DEF+10% | Freeze chance\n"); break;
            case "Volcano":
                addLog("🌋 [MAP] Volcano: -5HP/3dtk (imun:Api) | Air ATK/DEF↓ | Api ATK+15%\n");
                startPeriodicDamage(5, 3000, "Api",
                    "🌋 [Volcano] %s terkena 5 damage dari lahar!\n"); break;
            case "Hutan Gelap":
                addLog("🌑 [MAP] Hutan Gelap: Cahaya↓↓ | Gelap↑↑ | Lain DEF-10%\n"); break;
        }
    }

    private void startPeriodicDamage(int dmg, int ms, String immune, String msg) {
        mapDamageTimer = new Timer(ms, e -> {
            if (battleEnded) { mapDamageTimer.stop(); return; }
            for (Beast b : state.getPlayerTeam()) {
                if (b.isAlive() && !b.getElement().equals(immune)) {
                    b.takeDamage(dmg);
                    addLog(String.format(msg, b.getName()));
                }
            }
            refreshAll();
            if (state.isPlayerDefeated()) handleDefeat();
        });
        mapDamageTimer.setInitialDelay(ms);
        mapDamageTimer.start();
    }

    private void checkBlizzardFreeze() {
        GameMap map = state.getSelectedMap();
        if (map == null || !map.getName().equals("Blizzard")) return;
        attackCounter++;
        if (attackCounter % 5 != 0) return;

        // Cek semua beast player (yang aktif giliran)
        TurnEntry cur = battle.getCurrentTurn();
        if (cur == null || cur.isEnemy || playerFrozen) return;
        Beast b = cur.beast;
        int chance = b.getElement().equals("Air") ? 5 : 20; // Air kebal (5%), lain 20%
        if (rng.nextInt(100) < chance) {
            playerFrozen = true;
            setActionsEnabled(false);
            addLog("❄️ [Blizzard] " + b.getName() + " DIBEKUKAN 3 detik!\n");
            mapEffectLabel.setText("❄️ " + b.getName() + " BEKU!");
            mapEffectLabel.setForeground(new Color(100, 200, 255));
            freezeTimer = new Timer(3000, ev -> {
                playerFrozen = false;
                if (!battleEnded) {
                    setActionsEnabled(true);
                    addLog("❄️ " + b.getName() + " sudah bebas dari beku!\n");
                    mapEffectLabel.setText(getMapEffectText());
                    mapEffectLabel.setForeground(new Color(200, 180, 120));
                }
            });
            freezeTimer.setRepeats(false);
            freezeTimer.start();
        }
    }

    private String getMapEffectText() {
        GameMap map = state.getSelectedMap();
        if (map == null) return "";
        switch (map.getName()) {
            case "Hutan Hijau": return "🌿 Api ATK-20%";
            case "Desert":      return "🌵 -3HP/3dtk | Air DEF-25%";
            case "Lautan Biru": return "🌊 Api↓ Daun SPD-20%";
            case "Blizzard":    return "❄️ Freeze | Api↓ Air DEF+10%";
            case "Volcano":     return "🌋 -5HP/3dtk | Air↓ Api ATK+15%";
            case "Hutan Gelap": return "🌑 Cahaya↓↓ Gelap↑↑";
            default:            return "";
        }
    }

    private void stopMapTimers() {
        if (mapDamageTimer != null) mapDamageTimer.stop();
        if (freezeTimer    != null) freezeTimer.stop();
    }

    // =========================================================================
    //  VICTORY / DEFEAT
    // =========================================================================
    private void handleVictory() {
        battleEnded = true;
        stopMapTimers();
        setActionsEnabled(false);
        SoundManager.getInstance().playSFX("VICTORY_SFX");
        state.addEggReward(1);
        addLog("🎉 MENANG! +1 🥚 Telur\n");

        GameMap map = state.getSelectedMap();
        if (map != null) {
            map.completeLevel();
            // Cek apakah map ini sudah fully completed → buka map berikutnya
            if (map.isFullyCompleted()) {
                List<GameMap> maps = state.getMaps();
                for (int i = 0; i < maps.size() - 1; i++) {
                    if (maps.get(i) == map && !maps.get(i + 1).isUnlocked()) {
                        maps.get(i + 1).setUnlocked(true);
                        addLog("🗺 Map baru terbuka: " + maps.get(i + 1).getName() + "!\n");
                    }
                }
            }
            // FIX: simpan ke DB SETELAH unlock diset, bukan sebelumnya
            // Sebelumnya saveMapProgressToDB() dipanggil sebelum setUnlocked(true)
            // sehingga status unlock map berikutnya tidak tersimpan ke DB
            state.saveMapProgressToDB();
        }
        new Timer(1800, e -> showVictoryDialog(map)) {{ setRepeats(false); start(); }};
    }

    private void handleDefeat() {
        battleEnded = true;
        stopMapTimers();
        setActionsEnabled(false);
        addLog("💀 KALAH! Semua beast mu pingsan.\n");
        new Timer(1800, e -> showDefeatDialog()) {{ setRepeats(false); start(); }};
    }

    private void showVictoryDialog(GameMap map) {
        String msg = "🏆 Level " + state.getCurrentLevel() + " selesai!";
        if (map != null && map.isFullyCompleted())
            msg += "\n🗺 Map " + map.getName() + " telah diselesaikan!";
        String[] opts = {"Level Berikutnya", "Kembali ke Map"};
        int choice = JOptionPane.showOptionDialog(this, msg, "MENANG!",
            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
            null, opts, opts[0]);
        if (choice == 0 && map != null && !map.isFullyCompleted()) {
            state.setCurrentLevel(state.getCurrentLevel() + 1);
            state.resetBattle();
            frame.showBeastSelect();
        } else {
            state.resetBattle();
            frame.showMapSelect();
        }
    }

    private void showDefeatDialog() {
        String[] opts = {"Coba Lagi", "Kembali ke Map"};
        int c = JOptionPane.showOptionDialog(this,
            "💀 Semua beast mu dikalahkan!", "KALAH",
            JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
            null, opts, opts[0]);
        state.resetBattle();
        if (c == 0) frame.showBeastSelect();
        else        frame.showMapSelect();
    }

    // =========================================================================
    //  REFRESH UI
    // =========================================================================
    private void refreshAll() {
        // Stat player aktif giliran (atau beast player pertama yang hidup)
        TurnEntry cur = battle.getCurrentTurn();
        Beast showBeast = null;
        if (cur != null && !cur.isEnemy) {
            showBeast = cur.beast;
        } else {
            for (Beast b : state.getPlayerTeam()) if (b.isAlive()) { showBeast = b; break; }
        }
        if (showBeast != null) {
            lblPlayerName.setText(showBeast.getName() + (playerFrozen ? " ❄️" : ""));
            lblPlayerElem.setText(ElementColor.getEmoji(showBeast.getElement())
                + " " + showBeast.getElement());
            lblPlayerElem.setForeground(ElementColor.getColor(showBeast.getElement()));
            hpBarPlayer.update(showBeast.getCurrentHP(), showBeast.getMaxHP());
            mpBarPlayer.update(showBeast.getCurrentMana(), showBeast.getMaxMana());
        }

        // Tombol target enemy
        refreshTargetButtons();

        // Turn order bar
        actionOrderBar.refresh(battle.getTurnQueueSnapshot(), battle.getCurrentTurn());

        // Arena
        arenaPanel.refresh();

        // Highlight giliran
        if (cur != null) {
            boolean isPlayerTurn = !cur.isEnemy && !playerFrozen;
            setActionsEnabled(isPlayerTurn && !battleEnded && !enemyTurnPending);
        }
    }

    private void addLog(String s) {
        battleLog.append(s);
        battleLog.setCaretPosition(battleLog.getDocument().getLength());
    }

    private void setActionsEnabled(boolean en) {
        btnAttack  .setEnabled(en);
        btnSkill   .setEnabled(en);
        btnUltimate.setEnabled(en);
        btnRun     .setEnabled(en);
    }

    // =========================================================================
    //  ACTION ORDER BAR (HSR-style)
    // =========================================================================
    class ActionOrderBar extends JPanel {
        private List<TurnEntry> queue = new ArrayList<>();
        private TurnEntry       current;

        ActionOrderBar() {
            setBackground(new Color(12, 12, 24));
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(60, 60, 120)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        }

        void refresh(List<TurnEntry> q, TurnEntry cur) {
            this.queue   = new ArrayList<>(q);
            this.current = cur;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            // Label "URUTAN GILIRAN"
            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            g2.setColor(new Color(140, 140, 180));
            g2.drawString("URUTAN GILIRAN", 4, 12);

            if (queue.isEmpty()) return;

            int totalBeasts = queue.size();
            int iconSize    = Math.min(44, (w - 100) / Math.max(totalBeasts, 1));
            int iconH       = iconSize;
            int startX      = 80;
            int centerY     = h / 2 + 4;

            // Garis timeline
            g2.setColor(new Color(50, 50, 80));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(startX, centerY, w - 10, centerY);
            g2.setStroke(new BasicStroke(1));

            // Gambar ikon setiap beast sesuai urutan
            for (int i = 0; i < queue.size(); i++) {
                TurnEntry e = queue.get(i);
                if (!e.beast.isAlive()) continue;

                int x = startX + i * (iconSize + 6);
                int y = centerY - iconH / 2;

                boolean isCurrent = (current != null && e == current);

                // Background ikon
                Color ec = ElementColor.getColor(e.beast.getElement());
                if (!e.isEnemy) {
                    g2.setColor(isCurrent
                        ? new Color(60, 180, 255, 220)
                        : new Color(30, 100, 160, 180));
                } else {
                    g2.setColor(isCurrent
                        ? new Color(255, 80, 80, 220)
                        : new Color(120, 30, 30, 180));
                }
                g2.fillRoundRect(x, y, iconSize, iconH, 8, 8);

                // Border – highlight giliran aktif
                if (isCurrent) {
                    g2.setColor(Color.YELLOW);
                    g2.setStroke(new BasicStroke(2.5f));
                    g2.drawRoundRect(x, y, iconSize, iconH, 8, 8);
                    g2.setStroke(new BasicStroke(1));
                    // Panah di atas
                    g2.setColor(Color.YELLOW);
                    int ax = x + iconSize / 2;
                    g2.fillPolygon(new int[]{ax-5, ax+5, ax}, new int[]{y-8, y-8, y-2}, 3);
                } else {
                    g2.setColor(ec.darker());
                    g2.drawRoundRect(x, y, iconSize, iconH, 8, 8);
                }

                // Elemen emoji + nama pendek
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                g2.setColor(Color.WHITE);
                String emoji = ElementColor.getEmoji(e.beast.getElement());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(emoji, x + (iconSize - fm.stringWidth(emoji)) / 2, y + iconH / 2 + 2);

                // Nama (di bawah ikon)
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                g2.setColor(isCurrent ? Color.YELLOW : new Color(200, 200, 200));
                String shortName = truncate(e.beast.getName(), 6);
                fm = g2.getFontMetrics();
                g2.drawString(shortName, x + (iconSize - fm.stringWidth(shortName)) / 2, y + iconH + 10);

                // Tag P/E kecil
                g2.setFont(new Font("Segoe UI", Font.BOLD, 7));
                g2.setColor(e.isEnemy ? new Color(255, 150, 150) : new Color(150, 220, 255));
                g2.drawString(e.isEnemy ? "E" : "P", x + 2, y + 9);
            }
        }
    }

    // =========================================================================
    //  BATTLE ARENA
    // =========================================================================
    class BattleArena extends JPanel {
        private int shakeX = 0, shakeY = 0;
        private Timer shakeTimer;

        BattleArena() {
            setBackground(Color.BLACK);
            setMinimumSize(new Dimension(300, 200));
        }

        void triggerShake(boolean big) {
            if (shakeTimer != null) shakeTimer.stop();
            int amp = big ? 8 : 4;
            final int[] cnt = {0};
            shakeTimer = new Timer(45, e -> {
                cnt[0]++;
                shakeX = (cnt[0] % 2 == 0) ? amp : -amp;
                shakeY = (cnt[0] % 3 == 0) ? amp / 2 : 0;
                if (cnt[0] > 6) { shakeX = 0; shakeY = 0; shakeTimer.stop(); }
                repaint();
            });
            shakeTimer.start();
        }

        void refresh() { repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.translate(shakeX, shakeY);

            drawBackground(g2, w, h);

            List<Beast> enemies = state.getEnemyTeam();
            List<Beast> players = state.getPlayerTeam();
            TurnEntry   cur     = battle.getCurrentTurn();

            // ── Gambar enemy di bagian atas arena ──────────────────────────────
            int n = enemies.size();
            int eW = Math.min(70, (w - 20) / Math.max(n, 1));
            int eH = eW;
            int eStartX = (w - n * (eW + 8)) / 2;
            for (int i = 0; i < n; i++) {
                Beast e = enemies.get(i);
                int ex = eStartX + i * (eW + 8);
                int ey = 14;
                boolean active = (cur != null && cur.isEnemy && cur.teamIndex == i);
                boolean isTarget = (i == selectedEnemyIdx && e.isAlive());
                drawBeastSprite(g2, ex, ey, eW, eH, e, false, active, isTarget);
                // HP mini bar
                drawMiniBar(g2, ex, ey + eH + 2, eW, 6, e.getCurrentHP(), e.getMaxHP(),
                    new Color(220, 70, 70));
            }

            // ── Gambar player di bagian bawah arena ───────────────────────────
            int p = players.size();
            int pW = Math.min(72, (w - 20) / Math.max(p, 1));
            int pH = pW;
            int pStartX = (w - p * (pW + 6)) / 2;
            int pY = h - pH - 28;
            for (int i = 0; i < p; i++) {
                Beast b = players.get(i);
                int px = pStartX + i * (pW + 6);
                boolean active = (cur != null && !cur.isEnemy && cur.teamIndex == i);
                drawBeastSprite(g2, px, pY, pW, pH, b, true, active, false);
                drawMiniBar(g2, px, pY + pH + 2, pW, 6, b.getCurrentHP(), b.getMaxHP(),
                    new Color(70, 220, 70));
                drawMiniBar(g2, px, pY + pH + 10, pW, 4, b.getCurrentMana(), b.getMaxMana(),
                    new Color(70, 120, 220));
            }

            g2.translate(-shakeX, -shakeY);
        }

        private void drawBeastSprite(Graphics2D g2, int x, int y, int w, int h,
                                     Beast b, boolean isPlayer,
                                     boolean isActive, boolean isTarget) {
            Color ec = ElementColor.getColor(b.getElement());

            if (!b.isAlive()) {
                // Mati: gambar abu-abu transparan
                g2.setColor(new Color(80, 80, 80, 100));
                g2.fillOval(x + w/4, y + h/4, w/2, h/2);
                g2.setColor(new Color(200, 50, 50));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                g2.drawString("✗", x + w/2 - 5, y + h/2 + 5);
                // Nama
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                g2.setColor(Color.GRAY);
                String n = truncate(b.getName(), 7);
                g2.drawString(n, x + (w - g2.getFontMetrics().stringWidth(n)) / 2, y + h + 14);
                return;
            }

            // Glow efek untuk yang sedang giliran
            if (isActive) {
                g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), 60));
                g2.fillOval(x - 6, y - 6, w + 12, h + 12);
            }

            // Target highlight (kuning)
            if (isTarget && !isPlayer) {
                g2.setColor(new Color(255, 255, 0, 50));
                g2.fillOval(x - 4, y - 4, w + 8, h + 8);
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x - 4, y - 4, w + 8, h + 8);
                g2.setStroke(new BasicStroke(1));
            }

            // Sprite beast (warna elemen)
            g2.setColor(ec);
            switch (b.getElement()) {
                case "Api":
                    g2.fillOval(x + w/4, y + h/3, w/2, h*2/3);
                    g2.setColor(new Color(255, 200, 50));
                    g2.fillPolygon(
                        new int[]{x+w/2, x+w/4, x+w*3/8, x+w/3, x+w*2/3, x+w*5/8, x+w*3/4},
                        new int[]{y, y+h*2/3, y+h/2, y+h, y+h, y+h/2, y+h*2/3}, 7);
                    break;
                case "Air":
                    g2.fillOval(x+w/5, y+h/5, w*3/5, h*3/5);
                    g2.setColor(new Color(150, 220, 255));
                    g2.fillArc(x+w/8, y+h/2, w*3/4, h/2, 0, 180);
                    break;
                case "Tanah":
                    g2.fillRoundRect(x+w/8, y+h/4, w*3/4, h*3/4, 10, 10);
                    g2.setColor(ec.brighter());
                    g2.fillRoundRect(x+w/4, y, w/2, h/2, 8, 8);
                    break;
                case "Daun":
                    g2.fillOval(x+w/4, y, w/2, h*2/3);
                    g2.setColor(new Color(100, 220, 100));
                    for (int i = 0; i < 3; i++) g2.fillOval(x+i*w/3, y+h/3, w/3, h/3);
                    break;
                case "Cahaya":
                    drawStar(g2, x+w/2, y+h/2, w/2, h/2, 6);
                    g2.setColor(new Color(255, 255, 200, 120));
                    g2.fillOval(x+w/4, y+h/4, w/2, h/2);
                    break;
                case "Gelap":
                    g2.fillOval(x+w/6, y+h/6, w*2/3, h*2/3);
                    g2.setColor(new Color(40, 0, 80));
                    g2.fillOval(x+w/3, y+h/3, w/3, h/3);
                    g2.setColor(new Color(180, 0, 255, 100));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawOval(x+w/6, y+h/6, w*2/3, h*2/3);
                    g2.setStroke(new BasicStroke(1));
                    break;
                default:
                    g2.fillOval(x+w/4, y+h/4, w/2, h/2);
            }

            // Mata
            int eyeY = y + h * 2 / 5;
            if (isPlayer) {
                g2.setColor(Color.WHITE);
                g2.fillOval(x+w/3, eyeY, w/7, w/7);
                g2.fillOval(x+w/2, eyeY, w/7, w/7);
                g2.setColor(Color.BLACK);
                g2.fillOval(x+w/3+2, eyeY+2, w/10, w/10);
                g2.fillOval(x+w/2+2, eyeY+2, w/10, w/10);
            } else {
                g2.setColor(Color.WHITE);
                g2.fillOval(x+w/3, eyeY, w/7, w/7);
                g2.fillOval(x+w/2, eyeY, w/7, w/7);
                g2.setColor(Color.RED);
                g2.fillOval(x+w/3+2, eyeY+2, w/10, w/10);
                g2.fillOval(x+w/2+2, eyeY+2, w/10, w/10);
            }

            // Beku overlay
            if (isPlayer && playerFrozen) {
                g2.setColor(new Color(100, 200, 255, 90));
                g2.fillRoundRect(x, y, w, h, 8, 8);
                g2.setColor(new Color(150, 220, 255));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(x, y, w, h, 8, 8);
                g2.setStroke(new BasicStroke(1));
            }

            // Nama
            g2.setFont(new Font("Segoe UI", isActive ? Font.BOLD : Font.PLAIN, 8));
            g2.setColor(isActive ? Color.YELLOW : Color.WHITE);
            String nm = truncate(b.getName(), 7);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(nm, x + (w - fm.stringWidth(nm)) / 2, y + h + 14);

            // Indikator giliran
            if (isActive) {
                g2.setColor(Color.YELLOW);
                g2.fillPolygon(
                    new int[]{x+w/2-4, x+w/2+4, x+w/2},
                    new int[]{y-10, y-10, y-4}, 3);
            }
        }

        private void drawStar(Graphics2D g2, int cx, int cy, int rx, int ry, int pts) {
            int[] xs = new int[pts*2], ys = new int[pts*2];
            for (int i = 0; i < pts*2; i++) {
                double a = Math.PI / pts * i - Math.PI / 2;
                double r = (i%2==0) ? rx : rx/2.2;
                xs[i] = cx + (int)(r * Math.cos(a));
                ys[i] = cy + (int)(ry * Math.sin(a));
            }
            g2.fillPolygon(xs, ys, pts*2);
        }

        private void drawMiniBar(Graphics2D g2, int x, int y, int w, int h,
                                  int cur, int max, Color color) {
            g2.setColor(new Color(30, 30, 30));
            g2.fillRoundRect(x, y, w, h, 3, 3);
            if (max > 0 && cur > 0) {
                int fw = (int)((double)cur / max * w);
                g2.setColor(color);
                g2.fillRoundRect(x, y, fw, h, 3, 3);
            }
        }

        private void drawBackground(Graphics2D g2, int w, int h) {
            GameMap map = state.getSelectedMap();
            String nm = map != null ? map.getName() : "";
            switch (nm) {
                case "Desert":
                    g2.setPaint(new GradientPaint(0,0,new Color(255,155,50),0,h*0.6f,new Color(235,195,70)));
                    g2.fillRect(0,0,w,(int)(h*0.65));
                    g2.setColor(new Color(215,175,90));
                    g2.fillRect(0,(int)(h*0.65),w,h);
                    g2.setColor(new Color(195,150,65));
                    g2.fillArc(-20,(int)(h*0.52),180,80,0,180);
                    g2.fillArc(w-130,(int)(h*0.58),170,60,0,180);
                    break;
                case "Volcano":
                    g2.setPaint(new GradientPaint(0,0,new Color(50,8,8),0,h*0.6f,new Color(110,25,8)));
                    g2.fillRect(0,0,w,(int)(h*0.72));
                    g2.setColor(new Color(190,55,0));
                    g2.fillRect(0,(int)(h*0.72),w,h);
                    g2.setColor(new Color(250,115,0));
                    for (int x=0;x<w;x+=28) g2.fillArc(x-8,(int)(h*0.70),36,18,0,180);
                    break;
                case "Blizzard":
                    g2.setPaint(new GradientPaint(0,0,new Color(170,205,238),0,h*0.6f,new Color(225,238,255)));
                    g2.fillRect(0,0,w,(int)(h*0.72));
                    g2.setColor(new Color(215,232,252));
                    g2.fillRect(0,(int)(h*0.72),w,h);
                    g2.setColor(new Color(255,255,255,180));
                    long t = System.currentTimeMillis()/70;
                    for (int i=0;i<24;i++) {
                        int sx=(int)((i*61+t*2)%w), sy=(int)((i*37+t*4)%h);
                        g2.fillOval(sx,sy,4,4);
                    }
                    break;
                case "Lautan Biru":
                    g2.setPaint(new GradientPaint(0,0,new Color(20,80,160),0,h*0.6f,new Color(30,120,200)));
                    g2.fillRect(0,0,w,(int)(h*0.65));
                    g2.setColor(new Color(10,60,140));
                    g2.fillRect(0,(int)(h*0.65),w,h);
                    g2.setColor(new Color(80,180,255,100));
                    long tw = System.currentTimeMillis()/120;
                    for (int i=0;i<5;i++) g2.fillArc((int)((i*80+tw*3)%w),(int)(h*0.60),60,20,0,180);
                    break;
                case "Hutan Hijau":
                    g2.setPaint(new GradientPaint(0,0,new Color(30,80,30),0,h*0.5f,new Color(50,130,50)));
                    g2.fillRect(0,0,w,(int)(h*0.65));
                    g2.setColor(new Color(30,100,30));
                    g2.fillRect(0,(int)(h*0.65),w,h);
                    g2.setColor(new Color(20,70,20,180));
                    for (int i=0;i<5;i++) g2.fillOval(i*w/5-10,(int)(h*0.4),50,80);
                    break;
                case "Hutan Gelap":
                    g2.setPaint(new GradientPaint(0,0,new Color(5,0,15),0,h*0.6f,new Color(15,5,30)));
                    g2.fillRect(0,0,w,(int)(h*0.7));
                    g2.setColor(new Color(10,5,25));
                    g2.fillRect(0,(int)(h*0.7),w,h);
                    g2.setColor(new Color(80,0,160,80));
                    for (int i=0;i<8;i++) g2.fillOval((i*43)%w,(i*29)%(int)(h*0.5),8,8);
                    break;
                default:
                    g2.setColor(new Color(14,14,28));
                    g2.fillRect(0,0,w,h);
                    g2.setColor(new Color(35,35,60));
                    g2.fillRect(0,h*3/4,w,h/4);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JButton makeBtn(String text, Color bg) {
        JButton b = new JButton("<html><center>" + text + "</center></html>");
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(bg.brighter()); }
            public void mouseExited (MouseEvent e) { b.setBackground(bg); }
        });
        return b;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
