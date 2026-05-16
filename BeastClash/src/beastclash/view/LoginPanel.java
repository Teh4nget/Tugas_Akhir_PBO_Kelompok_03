package beastclash.view;

import beastclash.audio.SoundManager;
import beastclash.database.DatabaseManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * LoginPanel – layar login / register Beast Clash.
 * Terhubung ke database XAMPP via DatabaseManager.
 *
 * FIX:
 *  - checkDBConnection() dipanggil di thread terpisah; koneksi hanya sekali.
 *  - Setelah login sukses, GameState.loadProgressFromDB() dipanggil
 *    (sudah muat map progress + owned beasts).
 *  - Mode offline menetapkan userId = -1 dan langsung ke story.
 *  - Tidak ada duplikasi koneksi DB.
 */
public class LoginPanel extends JPanel {

    private MainFrame frame;
    private DatabaseManager db;

    // Mode
    private boolean isRegister = false;

    // Komponen
    private JTextField     txtUsername;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirm;
    private JLabel lblConfirmLabel;
    private JButton btnAction;
    private JButton btnToggle;
    private JLabel lblTitle;
    private JLabel lblStatus;
    private JLabel lblDbStatus;

    // card sebagai field agar bisa diakses shake()
    private JPanel card;

    // Animasi
    private Timer animTimer;
    private float starPhase = 0;
    private float orbAngle  = 0;

    public LoginPanel(MainFrame frame) {
        this.frame = frame;
        this.db    = DatabaseManager.getInstance();
        setLayout(null);
        setPreferredSize(new Dimension(520, 520));
        setBackground(Color.BLACK);
        buildUI();
        startAnimation();
        checkDBConnection();   // FIX: cek koneksi satu kali, tidak double-connect
        SoundManager.getInstance().playBGM("MENU");
    }

    private void buildUI() {
        // ── Card tengah ─────────────────────────────────────────────────────
        card = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(18, 18, 40, 230));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.setColor(new Color(80, 100, 200, 180));
                g2.setStroke(new BasicStroke(2));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth()-2, getHeight()-2, 20, 20));
            }
        };
        card.setOpaque(false);
        card.setBounds(80, 100, 360, 380);
        add(card);

        // Judul
        lblTitle = new JLabel("MASUK", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(120, 180, 255));
        lblTitle.setBounds(0, 20, 360, 35);
        card.add(lblTitle);

        JLabel lblSub = new JLabel("Beast Clash", SwingConstants.CENTER);
        lblSub.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblSub.setForeground(new Color(150, 100, 220));
        lblSub.setBounds(0, 52, 360, 18);
        card.add(lblSub);

        // Username
        JLabel lblUser = styledLabel("Username");
        lblUser.setBounds(30, 82, 300, 18);
        card.add(lblUser);

        txtUsername = styledTextField();
        txtUsername.setBounds(30, 100, 300, 36);
        card.add(txtUsername);

        // Password
        JLabel lblPass = styledLabel("Password");
        lblPass.setBounds(30, 145, 300, 18);
        card.add(lblPass);

        txtPassword = new JPasswordField();
        styleTextField(txtPassword);
        txtPassword.setBounds(30, 163, 300, 36);
        card.add(txtPassword);

        // Konfirmasi password (register only)
        lblConfirmLabel = styledLabel("Konfirmasi Password");
        lblConfirmLabel.setBounds(30, 208, 300, 18);
        lblConfirmLabel.setVisible(false);
        card.add(lblConfirmLabel);

        txtConfirm = new JPasswordField();
        styleTextField(txtConfirm);
        txtConfirm.setBounds(30, 226, 300, 36);
        txtConfirm.setVisible(false);
        card.add(txtConfirm);

        // Tombol aksi (login/register)
        btnAction = new JButton("MASUK");
        btnAction.setBounds(30, 213, 300, 42);
        btnAction.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAction.setBackground(new Color(50, 100, 220));
        btnAction.setForeground(Color.WHITE);
        btnAction.setBorderPainted(false);
        btnAction.setFocusPainted(false);
        btnAction.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAction.addActionListener(e -> onAction());
        card.add(btnAction);
        card.setComponentZOrder(btnAction, 0);

        // Toggle login/register
        btnToggle = new JButton("Belum punya akun? Daftar");
        btnToggle.setBounds(30, 263, 300, 28);
        btnToggle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnToggle.setBackground(new Color(18, 18, 40, 0));
        btnToggle.setForeground(new Color(120, 160, 255));
        btnToggle.setBorderPainted(false);
        btnToggle.setContentAreaFilled(false);
        btnToggle.setFocusPainted(false);
        btnToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnToggle.addActionListener(e -> toggleMode());
        card.add(btnToggle);

        // Status pesan
        lblStatus = new JLabel("", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblStatus.setForeground(new Color(255, 100, 100));
        lblStatus.setBounds(30, 298, 300, 18);
        card.add(lblStatus);

        // Status koneksi DB (di luar card, di bawah layar)
        lblDbStatus = new JLabel("⏳ Menghubungkan ke database...", SwingConstants.CENTER);
        lblDbStatus.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lblDbStatus.setForeground(new Color(150, 150, 180));
        lblDbStatus.setBounds(0, 492, 520, 18);
        add(lblDbStatus);

        // Judul game di atas card
        JLabel gameTitle = new JLabel(" BEAST CLASH", SwingConstants.CENTER);
        gameTitle.setFont(new Font("Segoe UI", Font.BOLD, 30));
        gameTitle.setForeground(new Color(100, 180, 255));
        gameTitle.setBounds(0, 30, 520, 50);
        add(gameTitle);

        JLabel gameSub = new JLabel("Selamatkan Planet Elyra!", SwingConstants.CENTER);
        gameSub.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        gameSub.setForeground(new Color(180, 140, 220));
        gameSub.setBounds(0, 70, 520, 22);
        add(gameSub);

        // Enter key listeners
        txtPassword.addActionListener(e -> onAction());
        txtUsername.addActionListener(e -> txtPassword.requestFocus());
        txtConfirm.addActionListener(e -> onAction());
    }

    // ── Mode toggle ──────────────────────────────────────────────────────────
    private void toggleMode() {
        SoundManager.getInstance().playSFX("CLICK");
        isRegister = !isRegister;
        lblStatus.setText("");

        if (isRegister) {
            lblTitle.setText("DAFTAR");
            btnAction.setText("DAFTAR SEKARANG");
            btnAction.setBounds(30, 272, 300, 42);
            btnToggle.setText("Sudah punya akun? Masuk");
            btnToggle.setBounds(30, 322, 300, 28);
            lblStatus.setBounds(30, 355, 300, 18);
            lblConfirmLabel.setVisible(true);
            txtConfirm.setVisible(true);
        } else {
            lblTitle.setText("MASUK");
            btnAction.setText("MASUK");
            btnAction.setBounds(30, 213, 300, 42);
            btnToggle.setText("Belum punya akun? Daftar");
            btnToggle.setBounds(30, 263, 300, 28);
            lblStatus.setBounds(30, 298, 300, 18);
            lblConfirmLabel.setVisible(false);
            txtConfirm.setVisible(false);
        }
        repaint();
    }

    // ── Aksi login / register ─────────────────────────────────────────────────
    private void onAction() {
        SoundManager.getInstance().playSFX("CLICK");
        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            setStatus("Username dan password tidak boleh kosong!", false);
            return;
        }

        if (!db.isConnected()) {
            setStatus("Database tidak terhubung! Gunakan mode offline.", false);
            return;
        }

        if (isRegister) {
            String confirm = new String(txtConfirm.getPassword());
            if (!pass.equals(confirm)) {
                setStatus("Password tidak cocok!", false);
                return;
            }
            if (user.length() < 3) {
                setStatus("Username minimal 3 karakter!", false);
                return;
            }
            if (pass.length() < 4) {
                setStatus("Password minimal 4 karakter!", false);
                return;
            }

            // Register – jalankan di thread agar UI tidak freeze
            btnAction.setEnabled(false);
            btnAction.setText("Mendaftarkan...");
            new Thread(() -> {
                int uid = db.register(user, pass);
                SwingUtilities.invokeLater(() -> {
                    btnAction.setEnabled(true);
                    btnAction.setText("DAFTAR SEKARANG");
                    if (uid > 0) {
                        setStatus("Akun berhasil dibuat! Silakan login.", true);
                        toggleMode();
                    } else if (uid == -2) {
                        setStatus("Username sudah digunakan!", false);
                    } else {
                        setStatus("Gagal mendaftar. Cek koneksi database!", false);
                    }
                });
            }).start();

        } else {
            // Login – jalankan di thread agar UI tidak freeze
            btnAction.setEnabled(false);
            btnAction.setText("Memproses...");
            new Thread(() -> {
                int uid = db.login(user, pass);
                SwingUtilities.invokeLater(() -> {
                    btnAction.setEnabled(true);
                    btnAction.setText("MASUK");
                    if (uid > 0) {
                        setStatus("Login berhasil! Memuat game...", true);
                        SoundManager.getInstance().playSFX("UNLOCK");

                        // FIX: simpan userId dulu, lalu load semua progress dari DB
                        beastclash.controller.GameState.getInstance().setCurrentUserId(uid);
                        beastclash.controller.GameState.getInstance().loadProgressFromDB();

                        Timer t = new Timer(800, ev -> frame.showStory());
                        t.setRepeats(false);
                        t.start();
                    } else {
                        setStatus("Username atau password salah!", false);
                        shake();
                    }
                });
            }).start();
        }
    }

    private void setStatus(String msg, boolean success) {
        lblStatus.setText(msg);
        lblStatus.setForeground(success ? new Color(80, 220, 100) : new Color(255, 100, 100));
    }

    // ── DB connection check ───────────────────────────────────────────────────
    /**
     * FIX: koneksi dilakukan sekali di sini. DatabaseManager.connect() idempoten
     * (hanya connect jika belum terhubung), jadi aman dipanggil berulang.
     */
    private void checkDBConnection() {
        new Thread(() -> {
            // Jika sudah terhubung (misal dari sesi sebelumnya), tidak perlu connect lagi
            boolean ok = db.isConnected() || db.connect();
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    lblDbStatus.setText("Database terhubung (localhost MySQL)");
                    lblDbStatus.setForeground(new Color(80, 200, 100));
                } else {
                    lblDbStatus.setText("Database offline – mode offline aktif");
                    lblDbStatus.setForeground(new Color(255, 180, 60));
                    addOfflineButton();
                }
            });
        }).start();
    }

    private void addOfflineButton() {
        // Tambahkan tombol offline di dalam card agar tidak tertimpa
        JButton btnOffline = new JButton("> Main Offline (tanpa simpan)") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(80, 80, 115) : getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(140, 140, 190));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.setFont(getFont());
                g2.setColor(getForeground());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        btnOffline.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnOffline.setBackground(new Color(45, 45, 75));
        btnOffline.setForeground(new Color(200, 210, 240));
        btnOffline.setBorderPainted(false);
        btnOffline.setFocusPainted(false);
        btnOffline.setContentAreaFilled(false);
        btnOffline.setOpaque(false);
        btnOffline.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Letakkan di dalam card, di bawah status label
        // card.getBounds() = {80, 100, 360, 380} → area dalam card
        // Posisi relatif terhadap card: x=30 (margin), lebar=300, sesuai field lain
        btnOffline.setBounds(30, 325, 300, 30);
        btnOffline.addActionListener(e -> {
            beastclash.controller.GameState.getInstance().setCurrentUserId(-1);
            frame.showStory();
        });
        card.add(btnOffline);
        card.setComponentZOrder(btnOffline, 0); // letakkan di atas
        card.revalidate();
        card.repaint();
    }

    // ── Shake animation saat login gagal ─────────────────────────────────────
    private int shakeCount = 0;
    private void shake() {
        shakeCount = 0;
        Timer t = new Timer(40, null);
        t.addActionListener(e -> {
            shakeCount++;
            int dx = (shakeCount % 2 == 0) ? 6 : -6;
            card.setLocation(card.getX() + dx, card.getY());
            if (shakeCount >= 6) {
                card.setLocation(80, 100);
                ((Timer) e.getSource()).stop();
            }
        });
        t.start();
    }

    // ── Background animation ──────────────────────────────────────────────────
    private void startAnimation() {
        animTimer = new Timer(33, e -> {
            starPhase += 0.015f;
            orbAngle  += 0.012f;
            repaint();
        });
        animTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        GradientPaint bg = new GradientPaint(0, 0, new Color(5, 5, 20), 0, h, new Color(10, 10, 40));
        g2.setPaint(bg);
        g2.fillRect(0, 0, w, h);

        for (int i = 0; i < 80; i++) {
            int sx = (i * 97 + 23) % w;
            int sy = (i * 53 + 11) % h;
            // FIX: clamp alpha ke [0.0, 1.0] — sin() bisa negatif sehingga 0.4+0.6*sin < 0
            float alpha = Math.max(0f, Math.min(1f, (float)(0.4 + 0.6 * Math.sin(starPhase + i * 0.3))));
            int size = (i % 5 == 0) ? 3 : 1;
            g2.setColor(new Color(1f, 1f, 1f, alpha));
            g2.fillOval(sx, sy, size, size);
        }

        String[] elems = {"Api","Air","Tanah","Daun","Cahaya","Gelap"};
        int cx = w/2, cy = h/2 + 10;
        for (int i = 0; i < 6; i++) {
            double angle = orbAngle + Math.PI * 2 * i / 6;
            int ox = cx + (int)(220 * Math.cos(angle));
            int oy = cy + (int)(160 * Math.sin(angle));
            Color ec = beastclash.view.ElementColor.getColor(elems[i]);
            g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), 60));
            g2.fillOval(ox - 18, oy - 18, 36, 36);
            g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), 140));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(ox - 18, oy - 18, 36, 36);
            g2.setStroke(new BasicStroke(1));
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────
    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(new Color(160, 180, 220));
        return l;
    }

    private JTextField styledTextField() {
        JTextField tf = new JTextField();
        styleTextField(tf);
        return tf;
    }

    private void styleTextField(JTextField tf) {
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBackground(new Color(30, 35, 60));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 90, 160), 1),
            new EmptyBorder(4, 8, 4, 8)
        ));
    }
}
