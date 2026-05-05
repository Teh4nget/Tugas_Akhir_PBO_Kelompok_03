package beastclash.view;

import beastclash.controller.BattleController;
import beastclash.controller.GameState;
import beastclash.model.Beast;
import beastclash.model.GameMap;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Random;

public class BattlePanel extends JPanel {

    private MainFrame frame;
    private GameState state;
    private BattleController battle;

    // UI components
    private BattleArena arenaPanel;
    private JTextArea battleLog;
    private JScrollPane logScroll;

    // Enemy info
    private JLabel enemyNameLabel;
    private HPBar enemyHPBar;
    private HPBar enemyManaBar;
    private JLabel enemyElemLabel;

    // Player info
    private JLabel playerNameLabel;
    private HPBar playerHPBar;
    private HPBar playerManaBar;
    private JLabel playerElemLabel;

    // Beast switch buttons
    private JButton[] switchButtons;

    // Action buttons
    private JButton btnAttack, btnSkill, btnUltimate, btnRun;

    private boolean battleEnded = false;

    // ── MAP EFFECT state ──────────────────────────────────────────────────────
    // Desert / Volcano: periodic damage timer (setiap 3 detik)
    private Timer mapDamageTimer;

    // Blizzard: freeze state
    private boolean playerFrozen   = false;   // beast player sedang beku
    private int     attackCounter  = 0;       // hitung jumlah serangan (player+enemy)
    private Timer   freezeTimer;              // timer durasi beku (3 detik)

    // Freeze check threshold (per 5 serangan)
    private static final int FREEZE_CHECK_EVERY = 5;

    // Label status efek map (ditampilkan di atas panel aksi)
    private JLabel mapEffectLabel;
    private final Random rng = new Random();

    // ─────────────────────────────────────────────────────────────────────────

    public BattlePanel(MainFrame frame) {
        this.frame  = frame;
        this.state  = GameState.getInstance();
        this.battle = new BattleController();
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 50));
        buildUI();
        refreshBattleState();
        startMapEffects();   // aktifkan efek map setelah UI siap
    }

    // =========================================================================
    //  MAP EFFECT – inisialisasi
    // =========================================================================
    private void startMapEffects() {
        GameMap map = state.getSelectedMap();
        if (map == null) return;

        String mapName = map.getName();

        // Tulis info efek ke log saat pertama kali
        switch (mapName) {
            case "Desert":
                addLog("🌵 [EFEK MAP – Desert] Panas pasir memberikan 3 damage setiap 3 detik"
                     + " kepada semua beast (kecuali elemen Tanah)!\n");
                startPeriodicDamage(3, 3_000, "Tanah",
                    "🌵 [Desert] Panas terik! %s terkena 3 damage dari pasir!\n",
                    "🌵 [Desert] %s tahan panas pasir (elemen Tanah).\n");
                break;

            case "Volcano":
                addLog("🌋 [EFEK MAP – Volcano] Lahar panas memberikan 5 damage setiap 3 detik"
                     + " kepada semua beast (kecuali elemen Api)!\n");
                startPeriodicDamage(5, 3_000, "Api",
                    "🌋 [Volcano] Lahar menyembur! %s terkena 5 damage dari lahar!\n",
                    "🌋 [Volcano] %s kebal terhadap lahar (elemen Api).\n");
                break;

            case "Blizzard":
                addLog("❄️ [EFEK MAP – Blizzard] Badai beku! Setiap 5 serangan ada chance beast dibekukan.\n"
                     + "   • Elemen Air: 25% chance beku | Beast lain: 10% chance beku\n"
                     + "   • Beast yang beku tidak bisa menyerang selama 3 detik.\n");
                break;

            default:
                // Grass Land – tidak ada efek khusus
                break;
        }
    }

    /**
     * Membuat timer yang setiap [intervalMs] ms memberikan [damage] kepada
     * beast aktif pemain, KECUALI jika elemen beast == [immuneElement].
     */
    private void startPeriodicDamage(int damage, int intervalMs,
                                     String immuneElement,
                                     String hitMsg, String immuneMsg) {
        mapDamageTimer = new Timer(intervalMs, e -> {
            if (battleEnded) { mapDamageTimer.stop(); return; }

            Beast player = state.getActiveBeast();
            if (player == null || !player.isAlive()) return;

            if (player.getElement().equals(immuneElement)) {
                // Hanya log sesekali (tiap 9 detik agar tidak spam)
                // Kita pakai counter sederhana
            } else {
                player.takeDamage(damage);
                addLog(String.format(hitMsg, player.getName()));
                refreshBattleState();
                checkPlayerDefeatedFromEffect();
            }
        });
        mapDamageTimer.setInitialDelay(intervalMs);
        mapDamageTimer.start();
    }

    /**
     * Dipanggil setelah setiap serangan (player ATAU enemy) untuk
     * mengecek efek beku Blizzard.
     */
    private void checkBlizzardFreeze() {
        GameMap map = state.getSelectedMap();
        if (map == null || !map.getName().equals("Blizzard")) return;

        attackCounter++;
        if (attackCounter % FREEZE_CHECK_EVERY != 0) return;

        Beast player = state.getActiveBeast();
        if (player == null || !player.isAlive() || playerFrozen) return;

        // Hitung chance berdasarkan elemen
        int chance = player.getElement().equals("Air") ? 25 : 10;
        int roll   = rng.nextInt(100);

        if (roll < chance) {
            playerFrozen = true;
            setActionsEnabled(false);
            addLog("❄️ [Blizzard] " + player.getName() + " DIBEKUKAN oleh badai! "
                 + "Tidak bisa menyerang selama 3 detik! (roll " + roll + " < " + chance + "%)\n");
            mapEffectLabel.setText("❄️ " + player.getName() + " BEKU! (3 detik)");
            mapEffectLabel.setForeground(new Color(100, 200, 255));

            // Beku selama 3 detik
            freezeTimer = new Timer(3_000, ev -> {
                playerFrozen = false;
                if (!battleEnded) {
                    setActionsEnabled(true);
                    addLog("❄️ [Blizzard] " + player.getName() + " sudah tidak beku!\n");
                    mapEffectLabel.setText(getMapEffectLabelText());
                    mapEffectLabel.setForeground(Color.WHITE);
                }
            });
            freezeTimer.setRepeats(false);
            freezeTimer.start();
        }
    }

    /** Teks default label efek map di UI */
    private String getMapEffectLabelText() {
        GameMap map = state.getSelectedMap();
        if (map == null) return "";
        switch (map.getName()) {
            case "Desert":  return "🌵 Desert: -3 HP/3 dtk (imun: Tanah)";
            case "Volcano": return "🌋 Volcano: -5 HP/3 dtk (imun: Api)";
            case "Blizzard":return "❄️ Blizzard: beku 10% (Air 25%) per 5 serangan";
            default:        return "🌿 Grass Land";
        }
    }

    /** Cek apakah semua beast player mati akibat efek map */
    private void checkPlayerDefeatedFromEffect() {
        if (state.isPlayerDefeated()) {
            battleEnded = true;
            setActionsEnabled(false);
            if (mapDamageTimer != null) mapDamageTimer.stop();
            addLog("💀 KEKALAHAN! Beast mu tumbang oleh efek map...\n");
            Timer t = new Timer(2000, e -> showDefeatDialog());
            t.setRepeats(false);
            t.start();
        } else {
            // Pindah ke beast berikutnya jika aktif mati
            Beast current = state.getActiveBeast();
            if (current == null || !current.isAlive()) {
                for (int i = 0; i < state.getPlayerTeam().size(); i++) {
                    if (state.getPlayerTeam().get(i).isAlive()) {
                        state.switchActiveBeast(i);
                        addLog("🔄 " + state.getPlayerTeam().get(i).getName()
                             + " maju menggantikan beast yang pingsan!\n");
                        break;
                    }
                }
                refreshBattleState();
            }
        }
    }

    // =========================================================================
    //  BUILD UI
    // =========================================================================
    private void buildUI() {
        // === TOP: Enemy info ===
        add(buildEnemyPanel(), BorderLayout.NORTH);

        // === CENTER: Arena + Log ===
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setDividerSize(4);
        centerSplit.setResizeWeight(0.55);

        arenaPanel = new BattleArena();
        centerSplit.setLeftComponent(arenaPanel);

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBackground(new Color(20, 20, 40));
        logPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 160), 1),
            "LOG PERTEMPURAN", 0, 0,
            new Font("Segoe UI", Font.BOLD, 11), new Color(180, 180, 220)));

        battleLog = new JTextArea();
        battleLog.setEditable(false);
        battleLog.setFont(new Font("Consolas", Font.PLAIN, 11));
        battleLog.setBackground(new Color(15, 15, 30));
        battleLog.setForeground(new Color(200, 220, 200));
        battleLog.setLineWrap(true);
        battleLog.setWrapStyleWord(true);
        battleLog.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        logScroll = new JScrollPane(battleLog);
        logScroll.setBorder(null);
        logPanel.add(logScroll, BorderLayout.CENTER);
        centerSplit.setRightComponent(logPanel);

        add(centerSplit, BorderLayout.CENTER);

        // === BOTTOM: Player info + map effect label + Actions ===
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildEnemyPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(40, 20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        GameMap map = state.getSelectedMap();
        String mapName = map != null ? map.getName() : "?";
        int level = state.getCurrentLevel();
        JLabel mapLabel = new JLabel("Map: " + mapName + " | Level " + level, SwingConstants.LEFT);
        mapLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        mapLabel.setForeground(new Color(180, 160, 100));

        JPanel enemyInfo = new JPanel(new GridLayout(4, 1, 2, 2));
        enemyInfo.setOpaque(false);

        enemyNameLabel = new JLabel("Enemy", SwingConstants.CENTER);
        enemyNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        enemyNameLabel.setForeground(Color.WHITE);

        enemyElemLabel = new JLabel("", SwingConstants.CENTER);
        enemyElemLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        enemyHPBar  = new HPBar("HP", 100, 100, new Color(80, 200, 80));
        enemyManaBar = new HPBar("MP", 100, 100, new Color(80, 120, 220));

        enemyInfo.add(enemyNameLabel);
        enemyInfo.add(enemyElemLabel);
        enemyInfo.add(enemyHPBar);
        enemyInfo.add(enemyManaBar);

        // Dot indikator tim musuh
        JPanel enemyTeamPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        enemyTeamPanel.setOpaque(false);
        for (Beast e : state.getEnemyTeam()) {
            JPanel dot = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.setColor(e.isAlive() ? ElementColor.getColor(e.getElement()) : Color.GRAY);
                    g.fillOval(1, 1, getWidth()-2, getHeight()-2);
                }
            };
            dot.setPreferredSize(new Dimension(14, 14));
            dot.setOpaque(false);
            dot.setToolTipText(e.getName());
            enemyTeamPanel.add(dot);
        }

        panel.add(mapLabel, BorderLayout.WEST);
        panel.add(enemyInfo, BorderLayout.CENTER);
        panel.add(enemyTeamPanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(20, 20, 40));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        // Player info
        JPanel playerInfo = new JPanel(new GridLayout(4, 1, 2, 2));
        playerInfo.setOpaque(false);
        playerInfo.setPreferredSize(new Dimension(200, 90));

        playerNameLabel = new JLabel("Beast", SwingConstants.LEFT);
        playerNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        playerNameLabel.setForeground(Color.WHITE);

        playerElemLabel = new JLabel("", SwingConstants.LEFT);
        playerElemLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        playerHPBar  = new HPBar("HP", 100, 100, new Color(80, 200, 80));
        playerManaBar = new HPBar("MP", 100, 100, new Color(80, 120, 220));

        playerInfo.add(playerNameLabel);
        playerInfo.add(playerElemLabel);
        playerInfo.add(playerHPBar);
        playerInfo.add(playerManaBar);

        // Beast switch buttons
        JPanel switchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        switchPanel.setBackground(new Color(20, 20, 40));
        switchPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 120)),
            "Tim Beast", 0, 0,
            new Font("Segoe UI", Font.PLAIN, 10), new Color(150, 150, 180)));

        List<Beast> team = state.getPlayerTeam();
        switchButtons = new JButton[team.size()];
        for (int i = 0; i < team.size(); i++) {
            final int idx = i;
            Beast b = team.get(i);
            JButton sb = new JButton("<html><center><b>" + ElementColor.getEmoji(b.getElement())
                + "</b><br/><small>" + b.getName().substring(0, Math.min(6, b.getName().length()))
                + "</small></center></html>");
            sb.setPreferredSize(new Dimension(68, 50));
            sb.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            sb.setBackground(ElementColor.getColor(b.getElement()).darker());
            sb.setForeground(Color.WHITE);
            sb.setBorderPainted(false);
            sb.setFocusPainted(false);
            sb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            sb.addActionListener(e -> {
                if (playerFrozen) {
                    addLog("❄️ Tidak bisa mengganti beast – sedang beku!\n");
                    return;
                }
                if (team.get(idx).isAlive()) {
                    state.switchActiveBeast(idx);
                    addLog("🔄 Berganti ke " + team.get(idx).getName() + "!\n");
                    refreshBattleState();
                } else {
                    addLog("❌ " + team.get(idx).getName() + " sudah pingsan!\n");
                }
            });
            switchButtons[i] = sb;
            switchPanel.add(sb);
        }

        // Action buttons
        JPanel actionPanel = new JPanel(new BorderLayout(0, 4));
        actionPanel.setBackground(new Color(20, 20, 40));
        actionPanel.setPreferredSize(new Dimension(210, 90));
        actionPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        // Label efek map
        mapEffectLabel = new JLabel(getMapEffectLabelText(), SwingConstants.CENTER);
        mapEffectLabel.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        mapEffectLabel.setForeground(Color.WHITE);
        mapEffectLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));

        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 6, 6));
        btnGrid.setBackground(new Color(20, 20, 40));

        btnAttack  = createActionButton("⚔ ATTACK",           new Color(180, 60,  60));
        btnSkill   = createActionButton("✨ SKILL (25MP)",     new Color(60,  100, 180));
        btnUltimate= createActionButton("💥 ULTIMATE (60MP)",  new Color(120, 40,  160));
        btnRun     = createActionButton("🏃 RUN",              new Color(80,  80,  80));

        btnAttack  .addActionListener(e -> performAction("attack"));
        btnSkill   .addActionListener(e -> performAction("skill"));
        btnUltimate.addActionListener(e -> performAction("ultimate"));
        btnRun     .addActionListener(e -> performAction("run"));

        btnGrid.add(btnAttack);
        btnGrid.add(btnSkill);
        btnGrid.add(btnUltimate);
        btnGrid.add(btnRun);

        actionPanel.add(mapEffectLabel, BorderLayout.NORTH);
        actionPanel.add(btnGrid, BorderLayout.CENTER);

        panel.add(playerInfo,  BorderLayout.WEST);
        panel.add(switchPanel, BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.EAST);
        return panel;
    }

    private JButton createActionButton(String text, Color bg) {
        JButton btn = new JButton("<html><center>" + text + "</center></html>");
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            public void mouseExited(MouseEvent  e) { btn.setBackground(bg); }
        });
        return btn;
    }

    // =========================================================================
    //  PERFORM ACTION
    // =========================================================================
    private void performAction(String action) {
        if (battleEnded) return;

        // Blok aksi jika beku
        if (playerFrozen && !action.equals("run")) {
            addLog("❄️ " + (state.getActiveBeast() != null ? state.getActiveBeast().getName() : "Beast kamu")
                 + " sedang BEKU dan tidak bisa menyerang!\n");
            return;
        }

        BattleController.BattleResult result;
        switch (action) {
            case "attack":   result = battle.performAttack();   break;
            case "skill":    result = battle.performSkill();    break;
            case "ultimate": result = battle.performUltimate(); break;
            case "run":      result = battle.performRun();      break;
            default:         return;
        }

        addLog(result.log);

        if (result.log.contains("[RUN_SUCCESS]")) {
            battleEnded = true;
            stopMapTimers();
            setActionsEnabled(false);
            addLog("--- Pertarungan berakhir (Kabur) ---\n");
            Timer t = new Timer(1200, e -> frame.showMapSelect());
            t.setRepeats(false); t.start();
            return;
        }

        // Hitung serangan untuk cek freeze Blizzard
        // (hanya jika aksi menyerang, bukan run)
        if (!action.equals("run")) {
            checkBlizzardFreeze();
        }

        refreshBattleState();
        arenaPanel.triggerShake(action.equals("ultimate"));

        if (result.playerFainted) {
            Beast next = state.getActiveBeast();
            if (next != null) addLog("🔄 " + next.getName() + " maju ke pertempuran!\n");
        }

        if (result.allEnemyDefeated) {
            battleEnded = true;
            stopMapTimers();
            setActionsEnabled(false);
            addLog("🎉 KEMENANGAN! Level " + state.getCurrentLevel() + " selesai!\n");
            handleVictory();
            return;
        }

        if (result.allPlayerDefeated) {
            battleEnded = true;
            stopMapTimers();
            setActionsEnabled(false);
            addLog("💀 KEKALAHAN! Semua beast mu dikalahkan...\n");
            Timer t = new Timer(2000, e -> showDefeatDialog());
            t.setRepeats(false); t.start();
        }

        refreshBattleState();
    }

    /** Hentikan semua timer efek map */
    private void stopMapTimers() {
        if (mapDamageTimer != null) mapDamageTimer.stop();
        if (freezeTimer    != null) freezeTimer.stop();
    }

    // =========================================================================
    //  VICTORY / DEFEAT
    // =========================================================================
    private void handleVictory() {
        GameMap map = state.getSelectedMap();
        if (map != null) {
            map.completeLevel();
            if (map.isCompleted()) {
                List<GameMap> maps = state.getMaps();
                for (int i = 0; i < maps.size() - 1; i++) {
                    if (maps.get(i) == map && !maps.get(i + 1).isUnlocked()) {
                        maps.get(i + 1).setUnlocked(true);
                        addLog("🗺 Map baru terbuka: " + maps.get(i + 1).getName() + "!\n");
                        break;
                    }
                }
            }
        }
        Timer t = new Timer(2000, e -> showVictoryDialog(map));
        t.setRepeats(false); t.start();
    }

    private void showVictoryDialog(GameMap map) {
        String msg = "🏆 Selamat! Level " + state.getCurrentLevel() + " selesai!";
        if (map != null && map.isCompleted())
            msg += "\n🗺 Map " + map.getName() + " telah diselesaikan!";
        String[] opts = {"Next Level", "Kembali ke Map"};
        int choice = JOptionPane.showOptionDialog(this, msg, "MENANG!",
            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
            null, opts, opts[0]);
        if (choice == 0 && map != null && !map.isCompleted()) {
            state.setCurrentLevel(state.getCurrentLevel() + 1);
            frame.showBeastSelect();
        } else {
            frame.showMapSelect();
        }
    }

    private void showDefeatDialog() {
        String[] opts = {"Coba Lagi", "Kembali ke Map"};
        int choice = JOptionPane.showOptionDialog(this,
            "💀 Semua beast mu dikalahkan!\nCoba lagi?", "KALAH",
            JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
            null, opts, opts[0]);
        if (choice == 0) frame.showBeastSelect();
        else             frame.showMapSelect();
    }

    // =========================================================================
    //  REFRESH UI
    // =========================================================================
    private void refreshBattleState() {
        Beast player = state.getActiveBeast();
        Beast enemy  = state.getActiveEnemy();

        if (player != null) {
            playerNameLabel.setText(player.getName() + (playerFrozen ? " ❄️" : ""));
            playerElemLabel.setText(ElementColor.getEmoji(player.getElement()) + " " + player.getElement());
            playerElemLabel.setForeground(ElementColor.getColor(player.getElement()));
            playerHPBar .update(player.getCurrentHP(),   player.getMaxHP());
            playerManaBar.update(player.getCurrentMana(), player.getMaxMana());
        }

        if (enemy != null) {
            enemyNameLabel.setText(enemy.getName());
            enemyElemLabel.setText(ElementColor.getEmoji(enemy.getElement()) + " " + enemy.getElement());
            enemyElemLabel.setForeground(ElementColor.getColor(enemy.getElement()));
            enemyHPBar .update(enemy.getCurrentHP(),   enemy.getMaxHP());
            enemyManaBar.update(enemy.getCurrentMana(), enemy.getMaxMana());
        }

        if (switchButtons != null) {
            List<Beast> team = state.getPlayerTeam();
            for (int i = 0; i < switchButtons.length && i < team.size(); i++) {
                Beast b = team.get(i);
                switchButtons[i].setEnabled(b.isAlive());
                switchButtons[i].setBackground(b.isAlive()
                    ? ElementColor.getColor(b.getElement()).darker() : Color.DARK_GRAY);
                switchButtons[i].setBorder(i == state.getActiveBeastIndex()
                    ? BorderFactory.createLineBorder(Color.YELLOW, 2) : null);
            }
        }

        if (arenaPanel != null) arenaPanel.updateBeasts(player, enemy);
        repaint();
    }

    private void addLog(String text) {
        battleLog.append(text);
        battleLog.setCaretPosition(battleLog.getDocument().getLength());
    }

    private void setActionsEnabled(boolean enabled) {
        btnAttack  .setEnabled(enabled);
        btnSkill   .setEnabled(enabled);
        btnUltimate.setEnabled(enabled);
        btnRun     .setEnabled(enabled);
    }

    // =========================================================================
    //  INNER CLASS – BattleArena (canvas pertempuran)
    // =========================================================================
    class BattleArena extends JPanel {
        private Beast player, enemy;
        private int   shakeOffset = 0;
        private Timer shakeTimer;

        BattleArena() {
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(320, 220));
        }

        void updateBeasts(Beast p, Beast e) { this.player = p; this.enemy = e; repaint(); }

        void triggerShake(boolean big) {
            if (shakeTimer != null) shakeTimer.stop();
            shakeOffset = big ? 8 : 4;
            shakeTimer  = new Timer(60, null);
            shakeTimer.addActionListener(e -> {
                shakeOffset = shakeOffset > 0 ? -shakeOffset + (shakeOffset > 1 ? 1 : 0) : 0;
                if (shakeOffset == 0) shakeTimer.stop();
                repaint();
            });
            shakeTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            // ── Latar map ────────────────────────────────────────────────────
            drawMapBackground(g2, w, h);

            // ── Freeze overlay jika beku ──────────────────────────────────────
            if (playerFrozen) {
                g2.setColor(new Color(100, 200, 255, 40));
                g2.fillRect(0, h / 2 - 30, w / 2 + 40, h / 2 + 30);
            }

            // ── Beast musuh (kanan atas) + shake ─────────────────────────────
            if (enemy != null) {
                drawBeast(g2, w - 110 + shakeOffset, 20, 80, 80, enemy, false);
                drawMiniBar(g2, w - 120 + shakeOffset, 110, 100, 10,
                    enemy.getCurrentHP(), enemy.getMaxHP(), new Color(220, 80, 80));
            }

            // ── Beast player (kiri bawah) ─────────────────────────────────────
            if (player != null) {
                drawBeast(g2, 30, h / 2 - 20, 100, 100, player, true);
                drawMiniBar(g2, 20, h / 2 + 90, 120, 12,
                    player.getCurrentHP(),   player.getMaxHP(),   new Color(80, 220, 80));
                drawMiniBar(g2, 20, h / 2 + 105, 120, 10,
                    player.getCurrentMana(), player.getMaxMana(), new Color(80, 120, 220));

                // Ikon beku di atas beast player
                if (playerFrozen) {
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
                    g2.drawString("❄️", 70, h / 2 - 22);
                }
            }
        }

        /** Gambar latar belakang sesuai map */
        private void drawMapBackground(Graphics2D g2, int w, int h) {
            GameMap map = state.getSelectedMap();
            String mapName = (map != null) ? map.getName() : "";

            switch (mapName) {
                case "Desert":
                    // Langit oranye – pasir
                    GradientPaint desertSky = new GradientPaint(0, 0, new Color(255, 160, 60),
                                                                 0, h * 0.6f, new Color(240, 200, 80));
                    g2.setPaint(desertSky);
                    g2.fillRect(0, 0, w, (int)(h * 0.65));
                    g2.setColor(new Color(220, 180, 100));
                    g2.fillRect(0, (int)(h * 0.65), w, h);
                    // Dune
                    g2.setColor(new Color(200, 155, 70));
                    g2.fillArc(-20, (int)(h * 0.55), 180, 80, 0, 180);
                    g2.fillArc(w - 120, (int)(h * 0.6), 160, 60, 0, 180);
                    // Kaktus kecil
                    g2.setColor(new Color(60, 130, 60));
                    g2.fillRect(w - 40, (int)(h * 0.45), 8, 30);
                    g2.fillRect(w - 52, (int)(h * 0.52), 20, 6);
                    break;

                case "Volcano":
                    // Langit gelap merah – lahar
                    GradientPaint volSky = new GradientPaint(0, 0, new Color(60, 10, 10),
                                                              0, h * 0.6f, new Color(120, 30, 10));
                    g2.setPaint(volSky);
                    g2.fillRect(0, 0, w, (int)(h * 0.7));
                    // Lahar di bawah
                    g2.setColor(new Color(200, 60, 0));
                    g2.fillRect(0, (int)(h * 0.7), w, h);
                    // Gelombang lahar
                    g2.setColor(new Color(255, 120, 0));
                    for (int x = 0; x < w; x += 30) {
                        g2.fillArc(x - 10, (int)(h * 0.68), 40, 20, 0, 180);
                    }
                    // Abu beterbangan
                    g2.setColor(new Color(80, 80, 80, 150));
                    for (int i = 0; i < 8; i++) {
                        g2.fillOval((i * 43) % w, (i * 27) % (int)(h * 0.5), 5, 5);
                    }
                    break;

                case "Blizzard":
                    // Langit biru-putih – salju
                    GradientPaint blizSky = new GradientPaint(0, 0, new Color(180, 210, 240),
                                                               0, h * 0.6f, new Color(230, 240, 255));
                    g2.setPaint(blizSky);
                    g2.fillRect(0, 0, w, (int)(h * 0.7));
                    g2.setColor(new Color(220, 235, 255));
                    g2.fillRect(0, (int)(h * 0.7), w, h);
                    // Salju jatuh (posisi statis berdasarkan waktu)
                    g2.setColor(new Color(255, 255, 255, 200));
                    long t = System.currentTimeMillis() / 80;
                    for (int i = 0; i < 20; i++) {
                        int sx = (int)((i * 53 + t * 2) % w);
                        int sy = (int)((i * 31 + t * 3) % h);
                        g2.fillOval(sx, sy, 4, 4);
                    }
                    // Gumpalan salju di tanah
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(-10, (int)(h * 0.78), 80, 30, 20, 20);
                    g2.fillRoundRect(w - 100, (int)(h * 0.75), 120, 35, 20, 20);
                    break;

                default:
                    // Grass Land – latar gelap default
                    g2.setColor(new Color(15, 15, 30));
                    g2.fillRect(0, 0, w, h);
                    g2.setColor(new Color(40, 40, 60));
                    g2.fillRect(0, h * 3 / 4, w, h / 4);
                    g2.setColor(new Color(60, 60, 90));
                    g2.drawLine(0, h * 3 / 4, w, h * 3 / 4);
                    g2.setColor(new Color(200, 200, 200, 100));
                    for (int i = 0; i < 20; i++) {
                        int sx = (i * 37 + 5) % w;
                        int sy = (i * 23 + 10) % (h * 3 / 4);
                        g2.fillOval(sx, sy, 2, 2);
                    }
                    break;
            }
        }

        private void drawBeast(Graphics2D g2, int x, int y, int w, int h, Beast beast, boolean isPlayer) {
            Color ec = ElementColor.getColor(beast.getElement());

            // Glow
            g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), 30));
            g2.fillOval(x - 10, y - 10, w + 20, h + 20);

            // Body
            g2.setColor(ec);
            switch (beast.getElement()) {
                case "Api":
                    int[] fx = {x+w/2, x+w/5, x+w*2/5, x+w/4, x+w*3/4, x+w*3/5, x+w*4/5};
                    int[] fy = {y, y+h*2/3, y+h/2, y+h, y+h, y+h/2, y+h*2/3};
                    g2.fillPolygon(fx, fy, fx.length);
                    break;
                case "Air":
                    g2.fillOval(x+w/6, y+h/6, w*2/3, h*2/3);
                    g2.setColor(ec.darker());
                    g2.fillArc(x, y+h/3, w, h*2/3, 0, 180);
                    break;
                case "Tanah":
                    g2.fillRoundRect(x+w/8, y+h/4, w*3/4, h*3/4, 10, 10);
                    g2.setColor(ec.darker());
                    g2.fillRoundRect(x+w/4, y, w/2, h/2, 8, 8);
                    break;
                case "Daun":
                    g2.fillOval(x+w/4, y, w/2, h*2/3);
                    g2.setColor(ec.darker());
                    for (int i = 0; i < 3; i++) g2.fillOval(x+i*w/3, y+h/3, w/3, h/3);
                    break;
                case "Cahaya":
                    drawStar(g2, x+w/2, y+h/2, w/2, h/2, 6);
                    break;
                case "Gelap":
                    g2.fillOval(x+w/6, y+h/6, w*2/3, h*2/3);
                    g2.setColor(new Color(30, 0, 60));
                    g2.fillOval(x+w/3, y+h/4, w/3, h/3);
                    break;
                default:
                    g2.fillOval(x+w/4, y+h/4, w/2, h/2);
            }

            // Mata
            g2.setColor(Color.WHITE);
            if (isPlayer) {
                g2.fillOval(x+w/3, y+h*2/5, w/8, w/8);
                g2.fillOval(x+w/2, y+h*2/5, w/8, w/8);
                g2.setColor(Color.BLACK);
                g2.fillOval(x+w/3+2, y+h*2/5+2, w/12, w/12);
                g2.fillOval(x+w/2+2, y+h*2/5+2, w/12, w/12);
            } else {
                g2.fillOval(x+w/3, y+h/3, w/8, w/8);
                g2.fillOval(x+w/2, y+h/3, w/8, w/8);
                g2.setColor(Color.RED);
                g2.fillOval(x+w/3+2, y+h/3+2, w/12, w/12);
                g2.fillOval(x+w/2+2, y+h/3+2, w/12, w/12);
            }

            // Nama
            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(beast.getName(), x + (w - fm.stringWidth(beast.getName())) / 2, y + h + 12);

            // Overlay pingsan
            if (!beast.isAlive()) {
                g2.setColor(new Color(0, 0, 0, 150));
                g2.fillRoundRect(x, y, w, h, 8, 8);
                g2.setColor(Color.RED);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.drawString("✗", x + w/2 - 5, y + h/2 + 5);
            }

            // Overlay beku (hanya beast player)
            if (isPlayer && playerFrozen) {
                g2.setColor(new Color(100, 200, 255, 80));
                g2.fillRoundRect(x, y, w, h, 8, 8);
                g2.setColor(new Color(100, 200, 255));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(x, y, w, h, 8, 8);
                g2.setStroke(new BasicStroke(1));
            }
        }

        private void drawStar(Graphics2D g2, int cx, int cy, int rx, int ry, int points) {
            int[] xs = new int[points * 2], ys = new int[points * 2];
            for (int i = 0; i < points * 2; i++) {
                double angle = Math.PI / points * i - Math.PI / 2;
                double r = (i % 2 == 0) ? rx : rx / 2;
                xs[i] = cx + (int)(r * Math.cos(angle));
                ys[i] = cy + (int)(ry * Math.sin(angle));
            }
            g2.fillPolygon(xs, ys, points * 2);
        }

        private void drawMiniBar(Graphics2D g2, int x, int y, int w, int h,
                                  int current, int max, Color color) {
            g2.setColor(new Color(40, 40, 40));
            g2.fillRoundRect(x, y, w, h, 4, 4);
            if (max > 0 && current > 0) {
                int fw = (int)((double) current / max * w);
                g2.setColor(color);
                g2.fillRoundRect(x, y, fw, h, 4, 4);
            }
        }
    }
}
