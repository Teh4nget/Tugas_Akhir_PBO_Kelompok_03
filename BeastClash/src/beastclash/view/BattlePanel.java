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
public class BattlePanel extends JPanel implements MainFrame.Cleanable {

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
    private boolean runLocked     = false; // tombol RUN dikunci setelah gagal kabur

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

        btnAttack   = makeBtn("ATTACK",          new Color(190, 55, 55));
        btnSkill    = makeBtn("* SKILL (30MP)",    new Color(55, 100, 200));
        btnUltimate = makeBtn("ULTIMATE (70MP)", new Color(130, 40, 180));
        btnRun      = makeBtn("RUN",             new Color(70, 70, 90));

        // Tooltip informasi tiap tombol aksi
        btnAttack.setToolTipText("<html><b>ATTACK</b><br>"
            + "Serang 1 musuh yang dipilih.<br>"
            + "Damage = ATK − DEF/2<br>"
            + "Tidak memerlukan MP.</html>");
        btnSkill.setToolTipText("<html><b>* SKILL</b> — Biaya: 30 MP<br>"
            + "Serang 1 musuh dengan kekuatan 1.5× ATK.<br>"
            + "Damage = (ATK × 1.5) − DEF/2<br>"
            + "Bonus elemen berlaku.</html>");
        btnUltimate.setToolTipText("<html><b>ULTIMATE</b> — Biaya: 70 MP<br>"
            + "Serang 1 musuh yang dipilih dengan kekuatan penuh.<br>"
            + "Damage = (ATK × 2) − DEF/3<br>"
            + "Damage tertinggi dari semua aksi.</html>");
        btnRun.setToolTipText("<html><b>RUN</b><br>"
            + "Kabur dari pertarungan.<br>"
            + "Akan muncul konfirmasi sebelum lari.</html>");

        btnAttack  .addActionListener(e -> onPlayerAction("attack"));
        btnSkill   .addActionListener(e -> onPlayerAction("skill"));
        btnUltimate.addActionListener(e -> onPlayerAction("ultimate"));
        btnRun     .addActionListener(e -> onPlayerAction("run"));

        actionPanel.add(btnAttack);
        actionPanel.add(btnSkill);
        actionPanel.add(btnUltimate);
        actionPanel.add(btnRun);

        // ── Toggle LOG button ────────────────────────────────────────────────
        JButton btnToggleLog = new JButton("LOG");
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

        JLabel lblTitle = new JLabel("Battle Log");
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

        selectedEnemyIdx = 0;
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i).isAlive()) { selectedEnemyIdx = i; break; }
        }

        beastclash.resources.ResourceManager rm =
            beastclash.resources.ResourceManager.getInstance();

        for (int i = 0; i < enemies.size(); i++) {
            final int idx = i;
            Beast e = enemies.get(i);
            Color ec = ElementColor.getColor(e.getElement());

            // Tombol dengan gambar beast
            JButton btn = new JButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                    boolean sel = (idx == selectedEnemyIdx);
                    boolean alive = e.isAlive();

                    // Background
                    g2.setColor(sel
                        ? new Color(ec.getRed()/2, ec.getGreen()/2, ec.getBlue()/2, 230)
                        : new Color(30, 30, 50, 200));
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);

                    // Gambar beast selalu tampil — grayscale jika mati
                    java.awt.image.BufferedImage img = rm.getBeastImg(e.getName());
                    if (img != null) {
                        int pad = 4;
                        if (!alive) {
                            // Konversi ke grayscale menggunakan ColorConvertOp
                            java.awt.image.ColorConvertOp grayOp = new java.awt.image.ColorConvertOp(
                                java.awt.color.ColorSpace.getInstance(java.awt.color.ColorSpace.CS_GRAY), null);
                            java.awt.image.BufferedImage grayImg = grayOp.filter(img, null);
                            g2.drawImage(grayImg, pad, pad, getWidth()-pad*2, getHeight()-18, null);
                            // Overlay gelap semi-transparan
                            g2.setColor(new Color(0, 0, 0, 100));
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                        } else {
                            g2.drawImage(img, pad, pad, getWidth()-pad*2, getHeight()-18, null);
                            // Overlay elemen tipis
                            g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), 30));
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                        }
                    }

                    // Nama di bawah
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                    String nm = e.getName().length()>8 ? e.getName().substring(0,7)+"…" : e.getName();
                    FontMetrics fm = g2.getFontMetrics();
                    g2.setColor(alive ? Color.WHITE : Color.GRAY);
                    g2.drawString(nm, (getWidth()-fm.stringWidth(nm))/2, getHeight()-4);

                    // Border seleksi
                    if (sel && alive) {
                        g2.setColor(Color.YELLOW);
                        g2.setStroke(new BasicStroke(2f));
                        g2.drawRoundRect(1,1,getWidth()-2,getHeight()-2,8,8);
                    } else {
                        g2.setColor(alive ? ec.darker() : new Color(60,60,60));
                        g2.setStroke(new BasicStroke(1f));
                        g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,8,8);
                    }
                    g2.setStroke(new BasicStroke(1));
                }
            };
            btn.setPreferredSize(new Dimension(68, 72));
            btn.setOpaque(false);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setEnabled(e.isAlive());
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (e.isAlive())
                btn.addActionListener(ev -> { selectedEnemyIdx = idx; refreshTargetButtons(); arenaPanel.refresh(); });

            enemyTargetBtns[i] = btn;
            enemyTargetPanel.add(btn);
        }
        refreshTargetButtons();
        enemyTargetPanel.revalidate();
        enemyTargetPanel.repaint();
    }

    private void refreshTargetButtons() {
        if (enemyTargetBtns == null) return;
        for (JButton btn : enemyTargetBtns) if (btn != null) btn.repaint();
        arenaPanel.refresh();
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
            addLog("" + cur.beast.getName() + " sedang BEKU, tidak bisa menyerang!\n");
            return;
        }

        BattleController.BattleResult result;
        if (action.equals("run")) {
            // Dialog konfirmasi sebelum lari
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "<html><b>Yakin ingin kabur dari pertarungan?</b><br>"
                    + "Progress battle akan hilang."
			+ "Peluang Berhasil Adalah 50%</html>",
		
                "Kabur dari Pertarungan",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (confirm != JOptionPane.YES_OPTION) return;
            result = battle.performRun();
        } else {
            // Attack, Skill, Ultimate — semuanya butuh target
            List<Beast> enemies = state.getEnemyTeam();
            if (selectedEnemyIdx >= enemies.size() || !enemies.get(selectedEnemyIdx).isAlive()) {
                for (int i = 0; i < enemies.size(); i++) {
                    if (enemies.get(i).isAlive()) { selectedEnemyIdx = i; break; }
                }
            }
            if (action.equals("ultimate")) {
                result = battle.performUltimate(selectedEnemyIdx);
            } else if (action.equals("skill")) {
                result = battle.performSkill(selectedEnemyIdx);
            } else {
                result = battle.performAttack(selectedEnemyIdx);
            }
        }

        addLog(result.log);

        // ── Efek visual ───────────────────────────────────────────────────────
        if (result.damageDealt > 0) {
            boolean isCrit = action.equals("skill") || action.equals("ultimate");
            String targetName = result.targetIsEnemy
                ? (state.getEnemyTeam().size() > selectedEnemyIdx ? state.getEnemyTeam().get(selectedEnemyIdx).getName() : "")
                : (state.getPlayerTeam().isEmpty() ? "" : state.getPlayerTeam().get(0).getName());
            arenaPanel.triggerHitEffect(result.damageDealt, isCrit, result.targetIsEnemy, targetName);
            arenaPanel.triggerShake(isCrit);
        }
        if (result.targetDied) {
            String targetName = result.targetIsEnemy
                ? (state.getEnemyTeam().size() > selectedEnemyIdx ? state.getEnemyTeam().get(selectedEnemyIdx).getName() : "")
                : (state.getPlayerTeam().isEmpty() ? "" : state.getPlayerTeam().get(0).getName());
            arenaPanel.triggerDeathEffect(result.targetIsEnemy, targetName);
        }

        if (result.log.contains("[RUN_SUCCESS]")) {
            battleEnded = true;
            stopMapTimers();
            setActionsEnabled(false);
            new Timer(1000, e -> frame.showMapSelect()) {{ setRepeats(false); start(); }};
            return;
        }

        if (result.log.contains("[RUN_FAIL]")) {
            runLocked = true;
            btnRun.setEnabled(false);
            btnRun.setToolTipText("<html><b>RUN</b><br>"
                + "<font color='red'>Gagal kabur! Tombol dikunci untuk giliran ini.</font></html>");
            checkBlizzardFreeze();
            refreshAll();
            // Giliran tetap milik player, tidak perlu scheduleNextTurn
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

            // ── Efek visual enemy ─────────────────────────────────────────────
            if (res.damageDealt > 0) {
                // Enemy menyerang player — cari player aktif yang hidup
                String targetName = "";
                for (Beast b : state.getPlayerTeam()) { if (b.isAlive()) { targetName = b.getName(); break; } }
                arenaPanel.triggerHitEffect(res.damageDealt, false, res.targetIsEnemy, targetName);
                arenaPanel.triggerShake(false);
            }
            if (res.targetDied) {
                String targetName = "";
                for (Beast b : state.getPlayerTeam()) { if (!b.isAlive()) { targetName = b.getName(); break; } }
                arenaPanel.triggerDeathEffect(res.targetIsEnemy, targetName);
            }

            checkBlizzardFreeze();
            refreshAll();
            enemyTurnPending = false;

            if (res.allEnemyDefeated) { handleVictory(); return; }
            if (res.allPlayerDefeated) { handleDefeat(); return; }

            // Setelah enemy selesai, cek apakah masih ada enemy berturut-turut
            scheduleNextTurn();
            if (!enemyTurnPending) {
                runLocked = false;
                btnRun.setToolTipText("<html><b>RUN</b><br>"
                    + "Kabur dari pertarungan.<br>"
                    + "Akan muncul konfirmasi sebelum lari.</html>");
                setActionsEnabled(true);
            }
        }) {{ setRepeats(false); start(); }};
    }

    private void checkIfEnemyTurn() {
        TurnEntry cur = battle.getCurrentTurn();
        if (cur != null && cur.isEnemy) scheduleNextTurn();
    }

    /**
     * Setelah freeze selesai: jika giliran berikutnya enemy, jalankan otomatis.
     * Jika giliran berikutnya player, aktifkan tombol aksi agar player bisa bermain.
     * Ini berbeda dari scheduleNextTurn() yang langsung return (tanpa enable tombol)
     * ketika giliran adalah milik player.
     */
    private void scheduleNextTurnOrEnablePlayer() {
        TurnEntry next = battle.getCurrentTurn();
        if (next == null) return;
        if (next.isEnemy) {
            scheduleNextTurn();
        } else {
            // Giliran player berikutnya — reset runLocked dan aktifkan tombol
            runLocked = false;
            btnRun.setToolTipText("<html><b>RUN</b><br>"
                + "Kabur dari pertarungan.<br>"
                + "Akan muncul konfirmasi sebelum lari.</html>");
            setActionsEnabled(true);
            refreshAll();
        }
    }

    // =========================================================================
    //  MAP EFFECTS
    // =========================================================================
    private void applyMapDebuffs() {
        GameMap map = state.getSelectedMap();
        if (map == null) return;
        switch (map.getName()) {
            case "Plains":
                applyDebuff("Api",    0.80f, 1f,    1f, false);
                break;
            case "Dessert":
                applyDebuff("Air",    1f,    0.75f, 1f, false);
                break;
            case "Sea":
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
            case "Dark Forest":
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
            case "Plains":
                addLog("Plains: Beast Api ATK -20%\n");
                applyElementDebuff("Api", 0.80f, 1f, 1f);
                break;
            case "Sea":
                addLog("Sea: Beast Api ATK/DEF berkurang | Beast Daun SPD -20%\n");
                applyElementDebuff("Api",  0.70f, 0.80f, 1f);
                applyElementDebuff("Daun", 1f,    1f,    0.80f);
                break;
            case "Dessert":
                addLog("Dessert: -3 HP/3 dtk (imun: Api & Tanah) | Air DEF-25% | Daun ATK-15%\n");
                applyElementDebuff("Air",  1f,    0.75f, 1f);
                applyElementDebuff("Daun", 0.85f, 1f,    1f);
                startPeriodicDamage(3, 3000, "Tanah",
                    "%s terkena 3 damage dari panas pasir!\n");
                break;
            case "Blizzard":
                addLog("Blizzard: Api ATK/DEF berkurang | Cahaya DEF+15% | Gelap DEF-20%\n");
                applyElementDebuff("Api",   0.65f, 0.75f, 1f);
                applyElementDebuff("Daun",  0.80f, 1f,    1f);
                applyElementBonus("Cahaya", 1f,    1.15f, 1f);
                applyElementDebuff("Gelap", 1f,    0.80f, 1f);
                break;
            case "Volcano":
                addLog("Volcano: -5 HP/3 dtk (imun: Api) | Air ATK/DEF berkurang | Api ATK+15%\n");
                applyElementDebuff("Air",   0.60f, 0.70f, 1f);
                applyElementDebuff("Tanah", 1f,    0.80f, 1f);
                applyElementBonus("Api",    1.15f, 1f,    1f);
                startPeriodicDamage(5, 3000, "Api",
                    "%s terkena 5 damage dari lahar!\n");
                break;
            case "Dark Forest":
                addLog("Dark Forest: Cahaya ATK/DEF berkurang | Gelap ATK/DEF meningkat\n");
                applyElementDebuff("Cahaya", 0.65f, 0.65f, 1f);
                applyElementBonus("Gelap",   1.20f, 1.15f, 1f);
                for (String elem : new String[]{"Api","Air","Tanah","Daun"})
                    applyElementDebuff(elem, 1f, 0.90f, 1f);
                break;
        }
    }

    private void applyElementDebuff(String element, float atkMult, float defMult, float spdMult) {
        applyStatMultiplier(element, atkMult, defMult, spdMult, false);
    }

    private void applyElementBonus(String element, float atkMult, float defMult, float spdMult) {
        applyStatMultiplier(element, atkMult, defMult, spdMult, true);
    }

    private void applyStatMultiplier(String element, float atkMult, float defMult,
                                     float spdMult, boolean isBonus) {
        List<Beast> team = state.getPlayerTeam();
        if (team == null) return;
        for (Beast b : team) {
            if (!b.getElement().equals(element)) continue;
            if (atkMult != 1f) b.multiplyAttack(atkMult);
            if (defMult != 1f) b.multiplyDefense(defMult);
            if (spdMult != 1f) b.multiplySpeed(spdMult);
            String tag = isBonus ? "BONUS" : "DEBUFF";
            addLog(tag + " [" + element + "] " + b.getName()
                + (atkMult != 1f ? String.format(" ATK x%.0f%%", atkMult * 100) : "")
                + (defMult != 1f ? String.format(" DEF x%.0f%%", defMult * 100) : "")
                + (spdMult != 1f ? String.format(" SPD x%.0f%%", spdMult * 100) : "")
                + "\n");
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

        TurnEntry cur = battle.getCurrentTurn();
        if (cur == null) return;

        Beast b = cur.beast;
        boolean isEnemyBeast = cur.isEnemy;

        // Jangan bekukan beast yang sudah ditandai beku (cegah double-freeze)
        if (!isEnemyBeast && playerFrozen) return;

        // Cahaya kebal beku (bonus Blizzard), Air mudah beku (40%), lain 25%
        int chance = b.getElement().equals("Cahaya") ? 0
                   : b.getElement().equals("Air")    ? 40 : 25;
        if (rng.nextInt(100) >= chance) return;

        // ── Beast terkena beku ──────────────────────────────────────────────
        addLog(b.getName() + " DIBEKUKAN selama 1 giliran!\n");
        mapEffectLabel.setText(b.getName() + " BEKU!");
        mapEffectLabel.setForeground(new Color(100, 200, 255));

        // Konsumsi giliran beast yang beku agar antrian turn benar-benar maju.
        // Tanpa ini, actionValue beast tidak pernah dikurangi sehingga getCurrentTurn()
        // selalu mengembalikan beast yang sama dan pertarungan stuck.
        battle.skipCurrentTurn();

        if (!isEnemyBeast) {
            // Beast player beku: nonaktifkan tombol, set flag, lalu lanjut setelah delay
            playerFrozen = true;
            setActionsEnabled(false);
            refreshAll();

            new Timer(1200, ev -> {
                ((Timer) ev.getSource()).stop();
                if (battleEnded) return;
                playerFrozen = false;
                mapEffectLabel.setText(getMapEffectText());
                mapEffectLabel.setForeground(new Color(200, 180, 120));
                addLog(b.getName() + " bebas dari beku.\n");
                // Lanjut ke giliran berikutnya — bisa enemy atau player lain
                scheduleNextTurnOrEnablePlayer();
            }) {{ setRepeats(false); start(); }};

        } else {
            // Beast enemy beku: turn sudah di-skip, lanjut langsung ke giliran berikutnya
            refreshAll();
            new Timer(800, ev -> {
                ((Timer) ev.getSource()).stop();
                if (battleEnded) return;
                mapEffectLabel.setText(getMapEffectText());
                mapEffectLabel.setForeground(new Color(200, 180, 120));
                addLog(b.getName() + " bebas dari beku.\n");
                // Cek apakah giliran berikutnya masih enemy atau sudah player
                scheduleNextTurnOrEnablePlayer();
            }) {{ setRepeats(false); start(); }};
        }
    }

    private String getMapEffectText() {
        GameMap map = state.getSelectedMap();
        if (map == null) return "";
        switch (map.getName()) {
            case "Plains":      return "Api ATK -20%";
            case "Sea":         return "Api ATK/DEF berkurang | Daun SPD -20%";
            case "Dessert":     return "-3HP/3dtk (imun:Api&Tanah) | Air DEF-25%";
            case "Blizzard":    return "Api ATK/DEF berkurang | Cahaya DEF+15% | Freeze";
            case "Volcano":     return "-5HP/3dtk (imun:Api) | Air berkurang | Api ATK+15%";
            case "Dark Forest": return "Cahaya ATK/DEF berkurang | Gelap ATK/DEF meningkat";
            default:            return "";
        }
    }

    private void stopMapTimers() {
        if (mapDamageTimer != null) mapDamageTimer.stop();
        if (freezeTimer    != null) freezeTimer.stop();
    }

    /** Dipanggil oleh MainFrame saat panel ini diganti — hentikan semua timer. */
    @Override
    public void cleanup() {
        battleEnded = true;
        stopMapTimers();
        if (arenaPanel != null && arenaPanel.shakeTimer != null) arenaPanel.shakeTimer.stop();
        if (arenaPanel != null && arenaPanel.effectTimer != null) arenaPanel.effectTimer.stop();
        if (logWindow  != null) { logWindow.dispose(); logWindow = null; }
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
        addLog("! MENANG! +1 Telur Telur\n");

        GameMap map = state.getSelectedMap();
        if (map != null) {
            map.completeLevel();
            // Cek apakah map ini sudah fully completed -> buka map berikutnya
            if (map.isFullyCompleted()) {
                List<GameMap> maps = state.getMaps();
                for (int i = 0; i < maps.size() - 1; i++) {
                    if (maps.get(i) == map && !maps.get(i + 1).isUnlocked()) {
                        maps.get(i + 1).setUnlocked(true);
                        addLog("Map baru terbuka: " + maps.get(i + 1).getName() + "!\n");
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
            msg += "\nMap " + map.getName() + " telah diselesaikan!";
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
            lblPlayerName.setText(showBeast.getName() + (playerFrozen ? " (Beku)" : ""));
            lblPlayerElem.setText(showBeast.getElement());
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
        btnRun     .setEnabled(en && !runLocked);
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
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int w = getWidth(), h = getHeight();

            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            g2.setColor(new Color(140, 140, 180));
            g2.drawString("URUTAN GILIRAN", 4, 12);

            if (queue.isEmpty()) return;

            int iconSize = Math.min(46, (w - 100) / Math.max(queue.size(), 1));
            int iconH    = iconSize;
            int startX   = 80;
            int centerY  = h / 2 + 4;

            g2.setColor(new Color(50, 50, 80));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(startX, centerY, w - 10, centerY);
            g2.setStroke(new BasicStroke(1));

            beastclash.resources.ResourceManager rm =
                beastclash.resources.ResourceManager.getInstance();

            for (int i = 0; i < queue.size(); i++) {
                TurnEntry e = queue.get(i);
                if (!e.beast.isAlive()) continue;

                int x = startX + i * (iconSize + 6);
                int y = centerY - iconH / 2;
                boolean isCurrent = (current != null && e == current);
                Color ec = ElementColor.getColor(e.beast.getElement());

                // Background bulat ikon
                if (!e.isEnemy) {
                    g2.setColor(isCurrent ? new Color(40,140,220,200) : new Color(20,70,130,160));
                } else {
                    g2.setColor(isCurrent ? new Color(220,60,60,200) : new Color(100,20,20,160));
                }
                g2.fillRoundRect(x, y, iconSize, iconH, 8, 8);

                // Gambar aset beast di ikon (player: flip, enemy: asli)
                java.awt.image.BufferedImage img = e.isEnemy
                    ? rm.getBeastForEnemy(e.beast.getName())
                    : rm.getBeastForPlayer(e.beast.getName());
                if (img != null) {
                    // Clip ke area ikon agar tidak keluar
                    Shape clip = g2.getClip();
                    g2.setClip(x+1, y+1, iconSize-2, iconH-2);
                    g2.drawImage(img, x, y, iconSize, iconH, null);
                    g2.setClip(clip);
                    // Overlay warna elemen tipis agar tetap ada identitas
                    g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), 40));
                    g2.fillRoundRect(x, y, iconSize, iconH, 8, 8);
                } else {
                    // Fallback: emoji elemen
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    g2.setColor(Color.WHITE);
                    String emoji = ElementColor.getEmoji(e.beast.getElement());
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(emoji, x + (iconSize - fm.stringWidth(emoji))/2, y + iconH/2 + 5);
                }

                // Border aktif (kuning) atau elemen
                if (isCurrent) {
                    g2.setColor(Color.YELLOW);
                    g2.setStroke(new BasicStroke(2.5f));
                    g2.drawRoundRect(x, y, iconSize, iconH, 8, 8);
                    g2.setStroke(new BasicStroke(1));
                    int ax = x + iconSize/2;
                    g2.fillPolygon(new int[]{ax-5,ax+5,ax}, new int[]{y-8,y-8,y-2}, 3);
                } else {
                    g2.setColor(ec.darker());
                    g2.drawRoundRect(x, y, iconSize, iconH, 8, 8);
                }

                // Nama singkat di bawah ikon
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                g2.setColor(isCurrent ? Color.YELLOW : new Color(200,200,200));
                String shortName = e.beast.getName().length() > 7
                    ? e.beast.getName().substring(0,6)+"…" : e.beast.getName();
                FontMetrics fm2 = g2.getFontMetrics();
                g2.drawString(shortName, x + (iconSize - fm2.stringWidth(shortName))/2, y + iconH + 10);

                // Tag P/E
                g2.setFont(new Font("Segoe UI", Font.BOLD, 7));
                g2.setColor(e.isEnemy ? new Color(255,150,150) : new Color(150,220,255));
                g2.drawString(e.isEnemy ? "E" : "P", x+2, y+9);
            }
        }
    }

    // =========================================================================
    //  BATTLE ARENA
    // =========================================================================
    // =========================================================================
    //  BATTLE ARENA
    // =========================================================================
    class BattleArena extends JPanel {
        int shakeX = 0, shakeY = 0;
        Timer shakeTimer;
        Timer effectTimer;

        // Cache background
        private Image cachedBg;
        private int   cachedBgW = -1, cachedBgH = -1;

        // ── Efek list (max 1 per jenis agar tidak menumpuk) ───────────────────
        // HitEffect: [x, y, vy, alpha, damage, isCrit, isEnemy]
        private final java.util.List<float[]> hitEffects   = new java.util.ArrayList<>();
        // FlashEffect: [x, y, w, h, alpha, isEnemy, spriteX,spriteY,spriteW,spriteH]
        private final java.util.List<float[]> flashEffects = new java.util.ArrayList<>();
        // DeathEffect: array per shard [shardX,shardY,vx,vy,rot,rotV,size] + metadata
        private final java.util.List<float[][]> deathEffects = new java.util.ArrayList<>();
        private final java.util.List<float[]>   deathMeta    = new java.util.ArrayList<>(); // [cx,cy,alpha,isEnemy]

        // ── Trigger dari luar ─────────────────────────────────────────────────
        void triggerHitEffect(int damage, boolean isCrit, boolean isEnemy, String beastName) {
            // Bersihkan efek LAMA sebelum tambah baru agar tidak menumpuk
            hitEffects.clear();
            flashEffects.clear();

            int W = getWidth(), H = getHeight();
            Random rnd = new Random();
            // Gunakan formula SAMA seperti paintComponent agar ukuran flash sesuai sprite
            int spriteSize = Math.min(W / 4, (int)((H - 80) * 0.72));
            int groundY    = getGroundY(H);
            int bOffset    = getBeastBottomOffset(beastName, spriteSize);

            // Posisi teks damage (di atas sprite, sedikit random)
            int tx = isEnemy
                ? (int)(W * 0.88) - spriteSize/2 + rnd.nextInt(30) - 15
                : (int)(W * 0.12) + spriteSize/2 + rnd.nextInt(30) - 15;
            int ty = groundY - spriteSize + bOffset - 10 + rnd.nextInt(20);

            // Posisi sprite yang kena
            int sx = isEnemy ? (int)(W * 0.88) - spriteSize : (int)(W * 0.12);
            int sy = groundY - spriteSize + bOffset;

            // [x, y, vy, alpha, damage, isCrit(1=yes), isEnemy(1=yes)]
            hitEffects.add(new float[]{tx, ty, -3.0f, 1.0f, damage, isCrit?1f:0f, isEnemy?1f:0f});
            // [x, y, w, h, alpha, isEnemy, spriteX, spriteY, spriteW, spriteH]
            flashEffects.add(new float[]{sx, sy, spriteSize, spriteSize, 1.0f, isEnemy?1f:0f,
                sx, sy, spriteSize, spriteSize});

            // Jalankan timer animasi efek secara mandiri
            startEffectTimer();
        }

        void triggerDeathEffect(boolean isEnemy, String beastName) {
            // Bersihkan death effect lama
            deathEffects.clear();
            deathMeta.clear();

            int W = getWidth(), H = getHeight();
            // Gunakan formula SAMA seperti paintComponent agar posisi death effect akurat
            int spriteSize = Math.min(W / 4, (int)((H - 80) * 0.72));
            int groundY    = getGroundY(H);
            int bOffset    = getBeastBottomOffset(beastName, spriteSize);
            float cx = isEnemy
                ? (float)(W * 0.88) - spriteSize/2f
                : (float)(W * 0.12) + spriteSize/2f;
            float cy = groundY - spriteSize/2f + bOffset;

            Random rnd = new Random();
            float[][] shards = new float[20][7];
            for (int i = 0; i < 20; i++) {
                double ang = Math.PI * 2 * i / 20 + rnd.nextGaussian() * 0.3;
                float spd  = 2f + rnd.nextFloat() * 5f;
                shards[i][0] = cx;
                shards[i][1] = cy;
                shards[i][2] = (float)(Math.cos(ang) * spd);
                shards[i][3] = (float)(Math.sin(ang) * spd) - 2f;
                shards[i][4] = rnd.nextFloat() * 360;
                shards[i][5] = (rnd.nextFloat() - 0.5f) * 16f;
                shards[i][6] = 7 + rnd.nextFloat() * 13f;
            }
            deathEffects.add(shards);
            deathMeta.add(new float[]{cx, cy, 1.0f, isEnemy?1f:0f});
            SoundManager.getInstance().playSFX("FREEZE");

            // Jalankan timer animasi efek secara mandiri
            startEffectTimer();
        }

        // Timer animasi efek yang berjalan mandiri (30fps)
        // effectTimer deklarasi di atas (package-visible untuk cleanup)
        private void startEffectTimer() {
            if (effectTimer != null && effectTimer.isRunning()) return; // sudah berjalan
            effectTimer = new Timer(33, e -> {
                // Tick semua efek
                hitEffects.removeIf(h -> h[3] <= 0);
                for (float[] h : hitEffects) { h[1] += h[2]; h[2] *= 0.92f; h[3] -= 0.028f; }
                flashEffects.removeIf(f -> f[4] <= 0);
                for (float[] f : flashEffects) f[4] -= 0.13f;
                for (int i = deathMeta.size()-1; i >= 0; i--) {
                    float[] meta = deathMeta.get(i);
                    meta[2] -= 0.020f;
                    if (meta[2] <= 0) { deathMeta.remove(i); deathEffects.remove(i); continue; }
                    for (float[] s : deathEffects.get(i)) {
                        s[0]+=s[2]; s[1]+=s[3]; s[3]+=0.18f; s[4]+=s[5]; s[2]*=0.95f;
                    }
                }
                repaint();
                // Stop timer jika semua efek selesai
                if (hitEffects.isEmpty() && flashEffects.isEmpty() && deathMeta.isEmpty()) {
                    effectTimer.stop();
                }
            });
            effectTimer.start();
        }

        // ── Tick efek ─────────────────────────────────────────────────────────
        void refresh() {
            // Animasi efek ditangani oleh effectTimer mandiri di triggerHitEffect/triggerDeathEffect
            repaint();
        }

        void triggerShake(boolean big) {
            if (shakeTimer != null) shakeTimer.stop();
            int amp = big ? 8 : 4;
            int[] cnt = {0};
            shakeTimer = new Timer(45, e -> {
                cnt[0]++;
                shakeX = (cnt[0] % 2 == 0) ? amp : -amp;
                shakeY = (cnt[0] % 3 == 0) ? amp/2 : 0;
                if (cnt[0] > 6) { shakeX=0; shakeY=0; shakeTimer.stop(); }
                repaint();
            });
            shakeTimer.start();
        }

        BattleArena() {
            setBackground(Color.BLACK);
            setMinimumSize(new Dimension(300, 200));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int W = getWidth(), H = getHeight();
            g2.translate(shakeX, shakeY);

            drawMapBackground(g2, W, H);

            List<Beast> enemies = state.getEnemyTeam();
            List<Beast> players = state.getPlayerTeam();
            TurnEntry   cur     = battle.getCurrentTurn();

            int spriteSize = Math.min(W / 4, (int)((H - 80) * 0.72)); // lebih kecil
            int groundY    = getGroundY(H);

            // ── Player: kiri bawah, menghadap kanan ───────────────────────────
            Beast activePlayer = null;
            if (cur != null && !cur.isEnemy) activePlayer = cur.beast;
            else for (Beast b : players) if (b.isAlive()) { activePlayer = b; break; }

            if (activePlayer != null) {
                int px = (int)(W * 0.12);
                int pOffset = getBeastBottomOffset(activePlayer.getName(), spriteSize);
                int py = groundY - spriteSize + pOffset;
                boolean pActive = (cur != null && !cur.isEnemy);
                drawBeastSprite(g2, px, py, spriteSize, spriteSize, activePlayer, true, pActive, false);
                drawMiniBar(g2, px, py+spriteSize+4, spriteSize, 8,
                    activePlayer.getCurrentHP(), activePlayer.getMaxHP(), new Color(60,210,80));
                drawMiniBar(g2, px, py+spriteSize+14, spriteSize, 5,
                    activePlayer.getCurrentMana(), activePlayer.getMaxMana(), new Color(60,120,220));
                drawBeastLabel(g2, px+spriteSize/2, py+spriteSize+28, activePlayer, false);
            }

            // ── Enemy: kanan bawah, menghadap kiri ───────────────────────────
            Beast activeEnemy = null;
            int activeEnemyIdx2 = selectedEnemyIdx;
            if (activeEnemyIdx2 < enemies.size()) activeEnemy = enemies.get(activeEnemyIdx2);
            if (activeEnemy == null || !activeEnemy.isAlive()) {
                for (int i = 0; i < enemies.size(); i++) {
                    if (enemies.get(i).isAlive()) { activeEnemy = enemies.get(i); activeEnemyIdx2=i; break; }
                }
            }
            if (activeEnemy != null) {
                int ex = (int)(W * 0.88) - spriteSize;
                int eOffset = getBeastBottomOffset(activeEnemy.getName(), spriteSize);
                int ey = groundY - spriteSize + eOffset;
                boolean eActive  = (cur != null && cur.isEnemy);
                boolean isTarget = (activeEnemyIdx2 == selectedEnemyIdx);
                drawBeastSprite(g2, ex, ey, spriteSize, spriteSize, activeEnemy, false, eActive, isTarget);
                drawMiniBar(g2, ex, ey+spriteSize+4, spriteSize, 8,
                    activeEnemy.getCurrentHP(), activeEnemy.getMaxHP(), new Color(220,60,60));
                drawBeastLabel(g2, ex+spriteSize/2, ey+spriteSize+18, activeEnemy, true);
            }

            g2.translate(-shakeX, -shakeY);

            // ── Render Flash (overlay mengikuti bentuk sprite) ─────────────────
            for (float[] f : flashEffects) {
                float a = Math.max(0, Math.min(1, f[4]));
                if (a <= 0) continue;
                int fx = (int)f[0], fy = (int)f[1], fw = (int)f[2], fh = (int)f[3];
                boolean fEnemy = (f[5] == 1f);
                // Ambil gambar beast yang terkena untuk AlphaComposite
                String bname = fEnemy
                    ? (activeEnemy  != null ? activeEnemy.getName()  : "")
                    : (activePlayer != null ? activePlayer.getName() : "");
                java.awt.image.BufferedImage bimg = fEnemy
                    ? beastclash.resources.ResourceManager.getInstance().getBeastForEnemy(bname)
                    : beastclash.resources.ResourceManager.getInstance().getBeastForPlayer(bname);

                if (bimg != null) {
                    java.awt.image.BufferedImage tinted = tintImage(bimg,
                        fEnemy ? new Color(255, 50, 50, (int)(a*200))
                               : new Color(255, 150, 30, (int)(a*200)));
                    g2.drawImage(tinted, fx, fy, fw, fh, null);
                } else {
                    // Fallback: overlay warna transparan (no box)
                    g2.setColor(fEnemy ? new Color(255,50,50,(int)(a*80))
                                       : new Color(255,150,30,(int)(a*80)));
                    g2.fillOval(fx + fw/4, fy + fh/4, fw/2, fh/2);
                }
                // Ring kilat dihapus — tidak ada kotak mengembang
            }

            // ── Render Hit Effects (angka damage melayang) ────────────────────
            for (float[] h : hitEffects) {
                float a = Math.max(0, Math.min(1, h[3]));
                if (a <= 0) continue;
                boolean crit   = (h[5] == 1f);
                boolean hEnemy = (h[6] == 1f);
                String dmgStr  = (crit ? "* " : "") + (int)h[4];
                Font dmgFont   = new Font("Segoe UI", Font.BOLD, crit ? 30 : 24);
                g2.setFont(dmgFont);
                FontMetrics fm = g2.getFontMetrics();
                int tx2 = (int)h[0] - fm.stringWidth(dmgStr)/2;
                int ty2 = (int)h[1];

                Color textCol = crit ? new Color(255,220,0) :
                                hEnemy ? new Color(255,70,70) :
                                         new Color(255,160,40);

                // Outline hitam
                g2.setColor(new Color(0,0,0,(int)(a*220)));
                for (int dx=-2; dx<=2; dx++)
                    for (int dy=-2; dy<=2; dy++)
                        if (dx!=0||dy!=0) g2.drawString(dmgStr, tx2+dx, ty2+dy);
                // Teks
                g2.setColor(new Color(textCol.getRed(), textCol.getGreen(), textCol.getBlue(), (int)(a*255)));
                g2.drawString(dmgStr, tx2, ty2);
            }

            // ── Render Death Effects (kristal ungu pecah) ─────────────────────
            for (int di = 0; di < deathMeta.size(); di++) {
                float[] meta   = deathMeta.get(di);
                float   da     = Math.max(0, meta[2]);
                float[][] shards = deathEffects.get(di);

                // Glow pusat
                int gR = (int)(60 * da);
                if (gR > 0) {
                    RadialGradientPaint glow = new RadialGradientPaint(
                        meta[0], meta[1], gR,
                        new float[]{0f, 1f},
                        new Color[]{new Color(180,60,255,(int)(da*140)), new Color(100,0,180,0)});
                    g2.setPaint(glow);
                    g2.fillOval((int)meta[0]-gR, (int)meta[1]-gR, gR*2, gR*2);
                }

                // Shard
                for (float[] s : shards) {
                    AffineTransform old = g2.getTransform();
                    g2.translate((int)s[0], (int)s[1]);
                    g2.rotate(Math.toRadians(s[4]));
                    int sz = (int)s[6];
                    int[] px2 = {0, sz/2, sz/2, 0, -sz/2, -sz/2};
                    int[] py2 = {-sz, -sz/2, sz/2, sz, sz/2, -sz/2};
                    g2.setColor(new Color(180,60,255,(int)(da*220)));
                    g2.fillPolygon(px2, py2, 6);
                    g2.setColor(new Color(80,0,140,(int)(da*180)));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawPolygon(px2, py2, 6);
                    g2.setColor(new Color(255,255,255,(int)(da*140)));
                    g2.fillOval(-sz/6, -sz/6, sz/3, sz/3);
                    g2.setTransform(old);
                }
            }
        }

        // ── Tint image (overlay warna pada sprite) ─────────────────────────────
        private java.awt.image.BufferedImage tintImage(java.awt.image.BufferedImage src, Color tint) {
            int w = src.getWidth(), h = src.getHeight();
            java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.drawImage(src, 0, 0, null);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,
                tint.getAlpha() / 255f));
            g.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue()));
            g.fillRect(0, 0, w, h);
            g.dispose();
            return out;
        }

        // ── Ground Y per-map: posisi kaki beast sesuai garis tanah gambar ─────
        // ── Offset kaki visual per-beast: koreksi padding transparan di bawah sprite ─
        // Nilai = fraksi dari spriteSize yang harus digeser ke bawah
        // agar kaki visual beast tepat menyentuh groundY.
        // Dihitung dari: bottomPad / imageHeight (semua gambar 1080x1080)
        private int getBeastBottomOffset(String beastName, int spriteSize) {
            float frac;
            switch (beastName) {
                case "Blazefang":    frac = 0.000f; break;
                case "Rootzilla":    frac = 0.000f; break;
                case "Terragorn":    frac = 0.000f; break;
                case "Noctyra":      frac = 0.001f; break;
                case "Quakron":      frac = 0.001f; break;
                case "Umbrax":       frac = 0.001f; break;
                case "Bedrock Titan": frac = 0.007f; break;
                case "Solareth":     frac = 0.017f; break;
                case "Luminaire":    frac = 0.009f; break;
                case "Floravine":    frac = 0.037f; break;
                case "Tsunadra":     frac = 0.044f; break;
                case "Aquarion":     frac = 0.045f; break;
                case "Morvexis":     frac = 0.051f; break;
                case "Shadowfang":   frac = 0.056f; break;
                case "Aetherion":    frac = 0.064f; break;
                case "Luxeron":      frac = 0.067f; break;
                case "Radiantor":    frac = 0.069f; break;
                case "Cinderion":    frac = 0.072f; break;
                case "Nerevion":     frac = 0.073f; break;
                case "Ignarox":      frac = 0.100f; break;
                case "Pyroth":       frac = 0.101f; break;
                case "Mossdrake":    frac = 0.108f; break;
                case "Gravok":       frac = 0.119f; break;
                case "Marivex":      frac = 0.181f; break;
                default:             frac = 0.050f; break;
            }
            return (int)(spriteSize * frac);
        }

        private int getGroundY(int H) {
            GameMap map = state.getSelectedMap();
            String name = (map != null) ? map.getName() : "Plains";
            float pct;
            switch (name) {
                case "Plains":      pct = 0.72f; break; // rumput hijau ~70-72%
                case "Sea":         pct = 0.77f; break; // garis pasir/laut ~77%
                case "Dessert":     pct = 0.88f; break; // pasir gelap ~88%
                case "Blizzard":    pct = 0.73f; break; // salju ~73%
                case "Volcano":     pct = 0.80f; break; // dalam lingkaran arena ~80%
                case "Dark Forest": pct = 0.73f; break; // tanah hutan ~73%
                default:            pct = 0.75f; break;
            }
            return (int)(H * pct);
        }

        // ── Draw background map ────────────────────────────────────────────────
        private void drawMapBackground(Graphics2D g2, int W, int H) {
            GameMap map  = state.getSelectedMap();
            String  name = (map != null) ? map.getName() : "Plains";
            if (cachedBg == null || cachedBgW != W || cachedBgH != H) {
                cachedBg  = beastclash.resources.ResourceManager.getInstance().getMapBgScaled(name, W, H);
                cachedBgW = W; cachedBgH = H;
            }
            if (cachedBg != null) {
                g2.drawImage(cachedBg, 0, 0, null);
                g2.setColor(new Color(0,0,0,55));
                g2.fillRect(0, 0, W, H);
            } else {
                g2.setColor(new Color(14,14,28));
                g2.fillRect(0, 0, W, H);
            }
        }

        // ── Draw sprite beast ──────────────────────────────────────────────────
        private void drawBeastSprite(Graphics2D g2, int x, int y, int w, int h,
                Beast b, boolean isPlayer, boolean isActive, boolean isTarget) {
            Color ec = ElementColor.getColor(b.getElement());
            if (!b.isAlive()) {
                // Beast mati: tampilkan sprite grayscale tanpa lingkaran/silang
                beastclash.resources.ResourceManager rm2 = beastclash.resources.ResourceManager.getInstance();
                java.awt.image.BufferedImage deadImg = isPlayer
                    ? rm2.getBeastForPlayer(b.getName())
                    : rm2.getBeastForEnemy(b.getName());
                if (deadImg != null) {
                    java.awt.image.ColorConvertOp grayOp = new java.awt.image.ColorConvertOp(
                        java.awt.color.ColorSpace.getInstance(java.awt.color.ColorSpace.CS_GRAY), null);
                    java.awt.image.BufferedImage grayImg = grayOp.filter(deadImg, null);
                    // Gambar grayscale dengan opacity 50%
                    java.awt.AlphaComposite ac = java.awt.AlphaComposite.getInstance(
                        java.awt.AlphaComposite.SRC_OVER, 0.4f);
                    java.awt.Composite oldComp = g2.getComposite();
                    g2.setComposite(ac);
                    g2.drawImage(grayImg, x, y, w, h, null);
                    g2.setComposite(oldComp);
                }
                return;
            }
            if (isActive) {
                // Tidak ada efek visual apapun di belakang beast saat aktif
            }
            if (isTarget && !isPlayer) {
                // Tidak ada efek visual apapun di belakang beast target
            }
            beastclash.resources.ResourceManager rm = beastclash.resources.ResourceManager.getInstance();
            java.awt.image.BufferedImage rawImg = isPlayer
                ? rm.getBeastForPlayer(b.getName())
                : rm.getBeastForEnemy(b.getName());
            if (rawImg != null) {
                g2.drawImage(rawImg, x, y, w, h, null);
            } else {
                drawProceduralBeast(g2, x, y, w, h, b, isPlayer, ec);
            }
        }

        private void drawBeastLabel(Graphics2D g2, int cx, int y, Beast b, boolean isEnemy) {
            String nm = b.getName() + " [" + b.getElement() + "]";
            g2.setFont(new Font("Segoe UI",Font.BOLD,11));
            FontMetrics fm = g2.getFontMetrics();
            int tx = cx - fm.stringWidth(nm)/2;
            g2.setColor(new Color(0,0,0,160)); g2.drawString(nm,tx+1,y+1);
            g2.setColor(isEnemy ? new Color(255,160,160) : new Color(160,255,180));
            g2.drawString(nm,tx,y);
        }

        private void drawProceduralBeast(Graphics2D g2,int x,int y,int w,int h,Beast b,boolean isPlayer,Color ec) {
            g2.setColor(ec);
            switch (b.getElement()) {
                case "Api":
                    g2.fillOval(x+w/4,y+h/3,w/2,h*2/3);
                    g2.setColor(new Color(255,200,50));
                    g2.fillPolygon(new int[]{x+w/2,x+w/4,x+w*3/8,x+w/3,x+w*2/3,x+w*5/8,x+w*3/4},
                        new int[]{y,y+h*2/3,y+h/2,y+h,y+h,y+h/2,y+h*2/3},7);
                    break;
                case "Air":
                    g2.fillOval(x+w/5,y+h/5,w*3/5,h*3/5);
                    g2.setColor(new Color(150,220,255));
                    g2.fillArc(x+w/8,y+h/2,w*3/4,h/2,0,180);
                    break;
                case "Tanah":
                    g2.fillRoundRect(x+w/8,y+h/4,w*3/4,h*3/4,10,10);
                    g2.setColor(ec.brighter());
                    g2.fillRoundRect(x+w/4,y,w/2,h/2,8,8);
                    break;
                case "Daun":
                    g2.fillOval(x+w/4,y,w/2,h*2/3);
                    g2.setColor(new Color(100,220,100));
                    for (int i=0;i<3;i++) g2.fillOval(x+i*w/3,y+h/3,w/3,h/3);
                    break;
                case "Cahaya":
                    drawStar(g2,x+w/2,y+h/2,w/2,h/2,6);
                    g2.setColor(new Color(255,255,200,120));
                    g2.fillOval(x+w/4,y+h/4,w/2,h/2);
                    break;
                case "Gelap":
                    g2.fillOval(x+w/6,y+h/6,w*2/3,h*2/3);
                    g2.setColor(new Color(40,0,80));
                    g2.fillOval(x+w/3,y+h/3,w/3,h/3);
                    g2.setColor(new Color(180,0,255,100));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawOval(x+w/6,y+h/6,w*2/3,h*2/3);
                    g2.setStroke(new BasicStroke(1));
                    break;
                default:
                    g2.fillOval(x+w/4,y+h/4,w/2,h/2);
            }
        }

        private void drawMiniBar(Graphics2D g2,int x,int y,int w,int h,int cur,int max,Color c) {
            g2.setColor(new Color(0,0,0,120));
            g2.fillRoundRect(x,y,w,h,3,3);
            if (max>0) {
                float ratio = Math.max(0,Math.min(1,(float)cur/max));
                g2.setColor(c);
                g2.fillRoundRect(x,y,(int)(w*ratio),h,3,3);
            }
        }

        private void drawStar(Graphics2D g2,int cx,int cy,int rx,int ry,int pts) {
            int[] xs=new int[pts*2],ys=new int[pts*2];
            for (int i=0;i<pts*2;i++) {
                double angle=Math.PI/pts*i-Math.PI/2;
                int r=(i%2==0)?Math.max(rx,ry):Math.min(rx,ry)/2;
                xs[i]=cx+(int)(r*Math.cos(angle));
                ys[i]=cy+(int)(r*Math.sin(angle));
            }
            g2.fillPolygon(xs,ys,pts*2);
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