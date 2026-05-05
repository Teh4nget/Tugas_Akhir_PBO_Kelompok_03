package beastclash.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Cutscene / Visual Novel intro untuk Beast Clash.
 * Terdiri dari beberapa "scene" yang ditampilkan satu per satu.
 * Setiap scene punya background unik, teks narasi, dan animasi ringan.
 */
public class StoryIntroPanel extends JPanel {

    private MainFrame frame;

    // ── Scene data ────────────────────────────────────────────────────────────
    private static final String[][] SCENES = {
        // { judul, narasi baris 1, narasi baris 2 }
        {
            "Planet Elyra – Zaman Damai",
            "Di sebuah planet bernama ELYRA, kehidupan berjalan dengan tenang.",
            "Para penduduk hidup berdampingan dengan alam dalam kedamaian abadi."
        },
        {
            "Malam yang Mengubah Segalanya",
            "Namun suatu malam, langit yang biasanya biru berubah merah menyala.",
            "Sebuah METEOR raksasa meluncur deras menuju permukaan planet..."
        },
        {
            "Meteor Jatuh!",
            "BOOM! Meteor menghantam bumi dan pecah menjadi 24 kepingan bercahaya.",
            "Kepingan itu menyebar ke segala penjuru Elyra — setiap sudut planet."
        },
        {
            "Telur Misterius",
            "Para ilmuwan terkejut: kepingan meteor itu ternyata adalah TELUR!",
            "Di dalam setiap telur tersimpan makhluk ajaib yang belum pernah dilihat sebelumnya."
        },
        {
            "24 Beast Menetas!",
            "Satu per satu, 24 BEAST menetas dari telur-telur itu dan menyebar ke seluruh planet.",
            "Api, Air, Tanah, Daun, Cahaya, Kegelapan — enam kekuatan berbeda terlahir."
        },
        {
            "Ancaman Dari Langit",
            "Beberapa hari kemudian... sebuah bayangan gelap menutupi langit Elyra.",
            "Sebuah kapal perang raksasa milik sang VILLAIN mendarat dengan keras."
        },
        {
            "Sang Villain – Vortex",
            "\"Semua Beast di planet ini akan menjadi MILIKKU!\"",
            "VORTEX, penguasa galaksi yang tamak, mengincar kekuatan ke-24 Beast itu."
        },
        {
            "Tugasmu Dimulai",
            "Kamu — satu-satunya yang bisa menghentikan Vortex —",
            "harus MENGUMPULKAN semua Beast sebelum Vortex merampas segalanya!"
        },
        {
            "Beast Clash",
            "Perjalananmu dimulai dari Grass Land...",
            "Kumpulkan Beast, kuasai elemen, dan selamatkan Planet Elyra!"
        }
    };

    private int   currentScene   = 0;
    private float textAlpha      = 0f;   // fade-in teks
    private float bgAlpha        = 0f;   // fade-in background
    private int   animTick       = 0;    // tick animasi umum

    private Timer animTimer;             // timer animasi (60 fps)

    // Teks yang sedang "diketik" (typewriter effect)
    private String displayLine1  = "";
    private String displayLine2  = "";
    private int    typewriterPos = 0;    // posisi karakter yang sedang diketik
    private boolean typingDone   = false;

    // Partikel untuk scene meteor
    private List<float[]> particles = new ArrayList<>();

    // Tombol navigasi
    private JButton btnNext;
    private JButton btnSkip;

    public StoryIntroPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(null);
        setPreferredSize(new Dimension(600, 480));
        setBackground(Color.BLACK);
        initParticles();
        buildButtons();
        startAnimation();
    }

    // ── Partikel meteor / bintang ─────────────────────────────────────────────
    private void initParticles() {
        particles.clear();
        for (int i = 0; i < 60; i++) {
            // { x, y, vx, vy, size, alpha }
            particles.add(new float[]{
                (float)(Math.random() * 600),
                (float)(Math.random() * 480),
                (float)(Math.random() * 1.5 - 0.75f),
                (float)(Math.random() * 0.5 + 0.1f),
                (float)(Math.random() * 2.5 + 0.5f),
                (float)(Math.random())
            });
        }
    }

    // ── Tombol UI ─────────────────────────────────────────────────────────────
    private void buildButtons() {
        btnNext = new JButton("LANJUT ▶");
        btnNext.setBounds(430, 430, 150, 36);
        btnNext.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnNext.setBackground(new Color(60, 120, 200));
        btnNext.setForeground(Color.WHITE);
        btnNext.setBorderPainted(false);
        btnNext.setFocusPainted(false);
        btnNext.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnNext.addActionListener(e -> onNextClicked());
        add(btnNext);

        btnSkip = new JButton("Lewati ✕");
        btnSkip.setBounds(20, 430, 110, 36);
        btnSkip.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnSkip.setBackground(new Color(80, 80, 80));
        btnSkip.setForeground(new Color(200, 200, 200));
        btnSkip.setBorderPainted(false);
        btnSkip.setFocusPainted(false);
        btnSkip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSkip.addActionListener(e -> finishStory());
        add(btnSkip);
    }

    private void onNextClicked() {
        if (!typingDone) {
            // Jika teks belum selesai diketik, langsung tampilkan semua
            displayLine1 = SCENES[currentScene][1];
            displayLine2 = SCENES[currentScene][2];
            typingDone   = true;
            btnNext.setText("LANJUT ▶");
            repaint();
        } else {
            // Pindah ke scene berikutnya
            currentScene++;
            if (currentScene >= SCENES.length) {
                finishStory();
            } else {
                resetSceneAnimation();
            }
        }
    }

    private void finishStory() {
        if (animTimer != null) animTimer.stop();
        frame.showMainMenu();
    }

    // ── Reset untuk scene baru ────────────────────────────────────────────────
    private void resetSceneAnimation() {
        textAlpha     = 0f;
        bgAlpha       = 0f;
        animTick      = 0;
        displayLine1  = "";
        displayLine2  = "";
        typewriterPos = 0;
        typingDone    = false;
        btnNext.setText("...");
        initParticles();
        repaint();
    }

    // ── Animation timer ───────────────────────────────────────────────────────
    private void startAnimation() {
        resetSceneAnimation();
        animTimer = new Timer(16, e -> {
            animTick++;

            // Fade in background
            if (bgAlpha < 1f) bgAlpha = Math.min(1f, bgAlpha + 0.04f);

            // Fade in teks (mulai setelah bgAlpha > 0.5)
            if (bgAlpha > 0.5f && textAlpha < 1f)
                textAlpha = Math.min(1f, textAlpha + 0.03f);

            // Update partikel
            for (float[] p : particles) {
                p[0] += p[2]; p[1] += p[3];
                if (p[0] < 0 || p[0] > 600) p[2] = -p[2];
                if (p[1] > 480) { p[1] = -5; p[0] = (float)(Math.random() * 600); }
            }

            // Typewriter effect (mulai setelah fade selesai)
            if (textAlpha >= 0.9f && !typingDone) {
                String fullText = SCENES[currentScene][1] + "  " + SCENES[currentScene][2];
                if (typewriterPos < fullText.length()) {
                    typewriterPos++;
                    String typed = fullText.substring(0, typewriterPos);
                    int split    = SCENES[currentScene][1].length();
                    if (typewriterPos <= split) {
                        displayLine1 = typed;
                        displayLine2 = "";
                    } else {
                        displayLine1 = SCENES[currentScene][1];
                        displayLine2 = typed.substring(split + 2);
                    }
                } else {
                    typingDone = true;
                    btnNext.setText("LANJUT ▶");
                }
            }

            repaint();
        });
        animTimer.start();
    }

    // =========================================================================
    //  PAINT
    // =========================================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        // Latar hitam selalu ada
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, w, h);

        // Gambar background scene dengan alpha fade-in
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, bgAlpha);
        g2.setComposite(ac);
        drawSceneBackground(g2, w, h, currentScene);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // Kotak teks di bagian bawah
        drawTextBox(g2, w, h);

        // Progress dots (scene indicator)
        drawProgressDots(g2, w, h);
    }

    // ── Background per scene ──────────────────────────────────────────────────
    private void drawSceneBackground(Graphics2D g2, int w, int h, int scene) {
        switch (scene) {
            case 0: drawPeacefulPlanet(g2, w, h);   break;
            case 1: drawNightSkyRed(g2, w, h);       break;
            case 2: drawMeteorImpact(g2, w, h);      break;
            case 3: drawEggScene(g2, w, h);           break;
            case 4: drawBeastsHatching(g2, w, h);    break;
            case 5: drawVillainShip(g2, w, h);        break;
            case 6: drawVillainClose(g2, w, h);       break;
            case 7: drawHeroScene(g2, w, h);          break;
            case 8: drawTitleScene(g2, w, h);         break;
            default: drawPeacefulPlanet(g2, w, h);   break;
        }
    }

    // Scene 0 – Planet damai
    private void drawPeacefulPlanet(Graphics2D g2, int w, int h) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(100, 180, 240), 0, h * 0.65f, new Color(160, 220, 255));
        g2.setPaint(sky);
        g2.fillRect(0, 0, w, (int)(h * 0.65));

        // Awan
        g2.setColor(new Color(255, 255, 255, 200));
        g2.fillRoundRect(60,  60,  140, 50, 30, 30);
        g2.fillRoundRect(300, 40,  180, 55, 30, 30);
        g2.fillRoundRect(470, 80,  120, 40, 20, 20);

        // Matahari
        g2.setColor(new Color(255, 230, 80));
        g2.fillOval(w - 120, 20, 80, 80);
        g2.setColor(new Color(255, 230, 80, 100));
        g2.fillOval(w - 135, 5, 110, 110);

        // Tanah
        GradientPaint ground = new GradientPaint(0, (int)(h*0.65), new Color(80, 160, 70), 0, h, new Color(50, 120, 40));
        g2.setPaint(ground);
        g2.fillRect(0, (int)(h * 0.65), w, h);

        // Pohon
        drawTree(g2, 80,  (int)(h*0.65));
        drawTree(g2, 160, (int)(h*0.65));
        drawTree(g2, 420, (int)(h*0.65));
        drawTree(g2, 520, (int)(h*0.65));

        // Rumah kecil
        g2.setColor(new Color(210, 170, 120));
        g2.fillRect(250, (int)(h*0.57), 80, 55);
        g2.setColor(new Color(180, 80, 60));
        int[] rx = {235, 295, 345}; int[] ry = {(int)(h*0.57), (int)(h*0.45), (int)(h*0.57)};
        g2.fillPolygon(rx, ry, 3);
        g2.setColor(new Color(120, 80, 40));
        g2.fillRect(275, (int)(h*0.6), 30, 25);

        // Bintang kecil di langit (sedikit)
        drawStars(g2, 10, w, (int)(h*0.4));
    }

    // Scene 1 – Langit malam merah pertanda
    private void drawNightSkyRed(Graphics2D g2, int w, int h) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(20, 0, 40), 0, h * 0.7f, new Color(100, 20, 20));
        g2.setPaint(sky);
        g2.fillRect(0, 0, w, (int)(h * 0.7));
        g2.setColor(new Color(50, 20, 20));
        g2.fillRect(0, (int)(h * 0.7), w, h);

        // Bintang
        drawStars(g2, 80, w, (int)(h * 0.65));

        // Bulan merah
        g2.setColor(new Color(220, 60, 40, 180));
        g2.fillOval(50, 40, 90, 90);
        g2.setColor(new Color(255, 100, 60, 80));
        g2.fillOval(35, 25, 120, 120);

        // Silhouette kota
        g2.setColor(new Color(15, 15, 25));
        for (int i = 0; i < w; i += 40) {
            int bh = 40 + (i * 7 + 13) % 60;
            g2.fillRect(i, (int)(h * 0.7) - bh, 35, bh + 10);
        }

        // Meteor di langit (garis cahaya)
        g2.setColor(new Color(255, 200, 100, 200));
        g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(w - 80, 30, w/2 + 60, (int)(h * 0.35));
        g2.setStroke(new BasicStroke(2));
        g2.setColor(new Color(255, 200, 100, 100));
        g2.drawLine(w - 60, 20, w/2 + 80, (int)(h * 0.33));
        g2.setStroke(new BasicStroke(1));
    }

    // Scene 2 – Meteor impact
    private void drawMeteorImpact(Graphics2D g2, int w, int h) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(10, 0, 20), 0, h, new Color(80, 30, 0));
        g2.setPaint(sky);
        g2.fillRect(0, 0, w, h);

        // Shockwave lingkaran
        int cx = w/2, cy = (int)(h * 0.6);
        for (int r = 20; r < 250; r += 30) {
            int alpha = Math.max(0, 180 - r);
            g2.setColor(new Color(255, 180, 50, alpha));
            g2.setStroke(new BasicStroke(3));
            g2.drawOval(cx - r, cy - r/2, r*2, r);
            g2.setStroke(new BasicStroke(1));
        }

        // Kepingan meteor bersinar (24 bongkahan)
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            int dist     = 80 + (i % 3) * 40;
            int px = cx + (int)(dist * Math.cos(angle));
            int py = cy + (int)(dist * 0.5 * Math.sin(angle));
            int sz = 8 + i % 6;
            Color shardColor = shardColor(i);
            g2.setColor(shardColor);
            g2.fillOval(px - sz/2, py - sz/2, sz, sz);
            // Ekor cahaya
            g2.setColor(new Color(shardColor.getRed(), shardColor.getGreen(), shardColor.getBlue(), 80));
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(cx, cy, px, py);
            g2.setStroke(new BasicStroke(1));
        }

        // Kawah
        g2.setColor(new Color(60, 30, 0));
        g2.fillOval(cx - 80, cy - 20, 160, 50);
        g2.setColor(new Color(200, 100, 0));
        g2.setStroke(new BasicStroke(3));
        g2.drawOval(cx - 80, cy - 20, 160, 50);
        g2.setStroke(new BasicStroke(1));

        // Partikel
        for (float[] p : particles) {
            g2.setColor(new Color(255, (int)(p[4]*60), 0, (int)(p[5]*180)));
            g2.fillOval((int)p[0], (int)p[1], (int)p[4], (int)p[4]);
        }

        // Teks "BOOM!"
        g2.setFont(new Font("Segoe UI", Font.BOLD, 60));
        g2.setColor(new Color(255, 220, 0, (int)(Math.abs(Math.sin(animTick * 0.05)) * 200 + 55)));
        g2.drawString("BOOM!", cx - 105, cy - 60);
    }

    // Scene 3 – Telur bercahaya di alam
    private void drawEggScene(Graphics2D g2, int w, int h) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(20, 40, 80), 0, h * 0.6f, new Color(40, 80, 120));
        g2.setPaint(sky);
        g2.fillRect(0, 0, w, (int)(h * 0.6));
        g2.setColor(new Color(30, 70, 40));
        g2.fillRect(0, (int)(h * 0.6), w, h);
        drawStars(g2, 50, w, (int)(h * 0.55));

        // 6 telur berkilau dengan warna elemen
        String[] elems  = {"Api","Air","Tanah","Daun","Cahaya","Gelap"};
        int[] ex = {60, 160, 260, 360, 460, 530};
        int   ey = (int)(h * 0.55);

        for (int i = 0; i < 6; i++) {
            Color ec = ElementColor.getColor(elems[i]);
            // Glow berdenyut
            int pulse = (int)(Math.sin(animTick * 0.06 + i) * 15);
            g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), 60 + pulse));
            g2.fillOval(ex[i] - 20, ey - 55 - pulse, 70, 90);
            // Badan telur
            g2.setColor(new Color(240, 230, 210));
            g2.fillOval(ex[i], ey - 45, 45, 58);
            // Garis elemen
            g2.setColor(ec);
            g2.setStroke(new BasicStroke(2));
            g2.drawArc(ex[i] + 5, ey - 30, 35, 40, 60, 60);
            g2.setStroke(new BasicStroke(1));
            // Kilap
            g2.setColor(new Color(255, 255, 255, 150));
            g2.fillOval(ex[i] + 10, ey - 40, 10, 14);

            // Label elemen
            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            g2.setColor(ec);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(elems[i], ex[i] + (45 - fm.stringWidth(elems[i]))/2, ey + 22);
        }

        // Ilmuwan kecil
        drawStickFigure(g2, w/2, (int)(h * 0.62), new Color(200, 200, 200));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(255, 255, 180));
        g2.drawString("!", w/2 - 3, (int)(h * 0.53));
    }

    // Scene 4 – Beast menetas
    private void drawBeastsHatching(Graphics2D g2, int w, int h) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(255, 140, 60), 0, h * 0.6f, new Color(255, 200, 100));
        g2.setPaint(sky);
        g2.fillRect(0, 0, w, (int)(h * 0.65));
        g2.setColor(new Color(60, 140, 60));
        g2.fillRect(0, (int)(h * 0.65), w, h);

        // 6 beast keluar dari cangkang berbeda
        String[] elems = {"Api","Air","Tanah","Daun","Cahaya","Gelap"};
        int[] bx = {50, 150, 250, 350, 450, 530};

        for (int i = 0; i < 6; i++) {
            Color ec  = ElementColor.getColor(elems[i]);
            int   bxi = bx[i];
            int   byi = (int)(h * 0.48) + (int)(Math.sin(animTick * 0.07 + i * 1.2) * 8);

            // Cangkang pecah (setengah bawah)
            g2.setColor(new Color(200, 190, 175));
            g2.fillArc(bxi - 5, byi + 20, 40, 30, 180, 180);
            g2.setColor(new Color(160, 150, 135));
            g2.drawArc(bxi - 5, byi + 20, 40, 30, 180, 180);

            // Beast kecil muncul
            g2.setColor(ec);
            g2.fillOval(bxi, byi, 30, 30);
            // Mata
            g2.setColor(Color.WHITE);
            g2.fillOval(bxi + 6,  byi + 8, 6, 6);
            g2.fillOval(bxi + 18, byi + 8, 6, 6);
            g2.setColor(Color.BLACK);
            g2.fillOval(bxi + 8,  byi + 10, 3, 3);
            g2.fillOval(bxi + 20, byi + 10, 3, 3);

            // Aura
            g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), 70));
            g2.fillOval(bxi - 10, byi - 10, 50, 50);

            // Partikel kecil
            g2.setColor(ec);
            for (int j = 0; j < 4; j++) {
                double a = Math.PI * 2 * j / 4 + animTick * 0.04;
                int px = bxi + 15 + (int)(20 * Math.cos(a));
                int py = byi + 15 + (int)(10 * Math.sin(a));
                g2.fillOval(px, py, 4, 4);
            }
        }

        // Label
        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        g2.setColor(new Color(60, 30, 0));
        g2.drawString("24 Beast Telah Menetas!", w/2 - 120, 50);
    }

    // Scene 5 – Kapal villain datang
    private void drawVillainShip(Graphics2D g2, int w, int h) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(15, 15, 35), 0, h * 0.7f, new Color(40, 20, 50));
        g2.setPaint(sky);
        g2.fillRect(0, 0, w, (int)(h * 0.7));
        g2.setColor(new Color(25, 50, 25));
        g2.fillRect(0, (int)(h * 0.7), w, h);
        drawStars(g2, 60, w, (int)(h * 0.65));

        // Kapal villain (besar, gelap, mengancam)
        int shipX = w/2 - 180;
        int shipY = (int)(h * 0.08) + (int)(Math.sin(animTick * 0.02) * 5);

        // Bayangan kapal di tanah
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(w/2 - 160, (int)(h * 0.68), 320, 30);

        // Badan kapal utama
        g2.setColor(new Color(30, 30, 50));
        g2.fillRoundRect(shipX, shipY + 30, 360, 80, 20, 20);

        // Menara kapal
        g2.setColor(new Color(20, 20, 40));
        g2.fillRoundRect(shipX + 120, shipY, 120, 50, 10, 10);

        // Lampu merah berkedip
        int blinkAlpha = (animTick % 30 < 15) ? 255 : 100;
        g2.setColor(new Color(220, 40, 40, blinkAlpha));
        g2.fillOval(shipX + 20,  shipY + 50, 14, 14);
        g2.fillOval(shipX + 326, shipY + 50, 14, 14);
        g2.fillOval(shipX + 170, shipY + 8,  12, 12);

        // Sinar tractor beam ke bawah
        GradientPaint beam = new GradientPaint(
            w/2, shipY + 110, new Color(150, 50, 200, 180),
            w/2, (int)(h * 0.7), new Color(150, 50, 200, 0));
        g2.setPaint(beam);
        int[] beamX = {w/2 - 20, w/2 + 20, w/2 + 80, w/2 - 80};
        int[] beamY = {shipY+110, shipY+110, (int)(h*0.7), (int)(h*0.7)};
        g2.fillPolygon(beamX, beamY, 4);

        // Silhouette orang berlari
        g2.setColor(new Color(20, 60, 20));
        drawStickFigure(g2, w/2 - 80, (int)(h * 0.72), new Color(100, 200, 100));
        drawStickFigure(g2, w/2 + 40, (int)(h * 0.72), new Color(100, 200, 100));
    }

    // Scene 6 – Close-up villain
    private void drawVillainClose(Graphics2D g2, int w, int h) {
        // Background gelap dramatis
        GradientPaint bg = new GradientPaint(0, 0, new Color(5, 0, 15), 0, h, new Color(30, 5, 40));
        g2.setPaint(bg);
        g2.fillRect(0, 0, w, h);

        // Efek cahaya ungu di belakang villain
        g2.setColor(new Color(100, 20, 150, 60));
        g2.fillOval(w/2 - 150, 20, 300, 300);

        // Gambar villain (siluet manusia dengan jubah)
        drawVillainFigure(g2, w/2 - 70, 50, 140, 280);

        // Nama villain
        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g2.setColor(new Color(200, 50, 220));
        g2.drawString("V O R T E X", w/2 - 75, h - 140);

        // Quote bubble
        g2.setColor(new Color(40, 10, 60, 200));
        g2.fillRoundRect(30, h - 125, w - 60, 50, 15, 15);
        g2.setColor(new Color(180, 100, 220));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(30, h - 125, w - 60, 50, 15, 15);
        g2.setStroke(new BasicStroke(1));

        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(new Color(255, 200, 255));
        g2.drawString("\"Semua Beast di planet ini akan menjadi MILIKKU!\"", 45, h - 95);
    }

    // Scene 7 – Karakter utama
    private void drawHeroScene(Graphics2D g2, int w, int h) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(255, 180, 80), 0, h * 0.6f, new Color(255, 120, 40));
        g2.setPaint(sky);
        g2.fillRect(0, 0, w, (int)(h * 0.65));
        g2.setColor(new Color(50, 130, 50));
        g2.fillRect(0, (int)(h * 0.65), w, h);
        drawTree(g2, 30,  (int)(h * 0.65));
        drawTree(g2, 520, (int)(h * 0.65));

        // Hero berdiri gagah di tengah
        int hx = w/2 - 25, hy = (int)(h * 0.35);
        // Cape/mantel
        g2.setColor(new Color(200, 50, 50));
        int[] capeX = {hx + 10, hx + 40, hx + 55, hx - 10};
        int[] capeY = {hy + 30, hy + 30, hy + 90, hy + 90};
        g2.fillPolygon(capeX, capeY, 4);
        // Badan
        g2.setColor(new Color(50, 100, 200));
        g2.fillRoundRect(hx + 5, hy + 30, 40, 55, 8, 8);
        // Kepala
        g2.setColor(new Color(240, 200, 160));
        g2.fillOval(hx + 5, hy, 40, 40);
        // Rambut
        g2.setColor(new Color(60, 40, 20));
        g2.fillArc(hx + 5, hy - 5, 40, 25, 0, 180);
        // Mata determinasi
        g2.setColor(new Color(50, 80, 200));
        g2.fillOval(hx + 12, hy + 12, 7, 7);
        g2.fillOval(hx + 27, hy + 12, 7, 7);
        // Kaki
        g2.setColor(new Color(40, 40, 80));
        g2.fillRoundRect(hx + 8,  hy + 82, 16, 28, 4, 4);
        g2.fillRoundRect(hx + 28, hy + 82, 16, 28, 4, 4);
        // Tangan
        g2.setColor(new Color(240, 200, 160));
        g2.fillOval(hx - 5,  hy + 35, 14, 14);
        g2.fillOval(hx + 45, hy + 35, 14, 14);

        // Aura pahlawan
        g2.setColor(new Color(255, 220, 50, 60 + (int)(Math.sin(animTick * 0.08) * 30)));
        g2.fillOval(hx - 30, hy - 20, 110, 160);

        // Teks motivasi
        g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
        g2.setColor(new Color(60, 30, 0));
        String heroText = "Tugasmu: Kumpulkan semua Beast!";
        FontMetrics fm  = g2.getFontMetrics();
        g2.drawString(heroText, (w - fm.stringWidth(heroText)) / 2, 40);
    }

    // Scene 8 – Title card
    private void drawTitleScene(Graphics2D g2, int w, int h) {
        GradientPaint bg = new GradientPaint(0, 0, new Color(10, 10, 30), 0, h, new Color(20, 40, 80));
        g2.setPaint(bg);
        g2.fillRect(0, 0, w, h);
        drawStars(g2, 100, w, h);

        // 6 orb elemen mengelilingi judul
        String[] elems = {"Api","Air","Tanah","Daun","Cahaya","Gelap"};
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2 * i / 6 + animTick * 0.015;
            int ox = w/2 + (int)(160 * Math.cos(angle));
            int oy = h/2 + (int)(80  * Math.sin(angle));
            Color ec = ElementColor.getColor(elems[i]);
            g2.setColor(new Color(ec.getRed(), ec.getGreen(), ec.getBlue(), 120));
            g2.fillOval(ox - 20, oy - 20, 40, 40);
            g2.setColor(ec);
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(ox - 20, oy - 20, 40, 40);
            g2.setStroke(new BasicStroke(1));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(elems[i], ox - fm.stringWidth(elems[i])/2, oy + 4);
        }

        // Judul utama
        g2.setFont(new Font("Segoe UI", Font.BOLD, 52));
        String title = "BEAST CLASH";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(title)) / 2;
        // Shadow
        g2.setColor(new Color(50, 100, 200, 120));
        g2.drawString(title, tx + 3, h/2 + 3);
        // Teks
        GradientPaint titleGrad = new GradientPaint(tx, h/2 - 50, new Color(100, 180, 255), tx + fm.stringWidth(title), h/2, new Color(200, 100, 255));
        g2.setPaint(titleGrad);
        g2.drawString(title, tx, h/2);

        // Subtitle
        g2.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        g2.setColor(new Color(180, 200, 240));
        String sub = "Selamatkan Planet Elyra!";
        FontMetrics fm2 = g2.getFontMetrics();
        g2.drawString(sub, (w - fm2.stringWidth(sub)) / 2, h/2 + 35);
    }

    // ── Kotak teks naratif ────────────────────────────────────────────────────
    private void drawTextBox(Graphics2D g2, int w, int h) {
        int boxH = 120, boxY = h - boxH - 50, boxX = 20, boxW = w - 40;

        // Latar kotak
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.88f));
        g2.setColor(new Color(10, 10, 30));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 16, 16);
        g2.setColor(new Color(80, 120, 200));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 16, 16);
        g2.setStroke(new BasicStroke(1));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // Judul scene
        AlphaComposite ta = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, textAlpha);
        g2.setComposite(ta);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(new Color(120, 180, 255));
        g2.drawString("◆ " + SCENES[currentScene][0], boxX + 14, boxY + 22);

        // Garis pemisah
        g2.setColor(new Color(60, 100, 160));
        g2.drawLine(boxX + 10, boxY + 30, boxX + boxW - 10, boxY + 30);

        // Teks narasi dengan typewriter
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        g2.setColor(new Color(220, 230, 255));
        g2.drawString(displayLine1, boxX + 14, boxY + 52);
        g2.setColor(new Color(190, 205, 240));
        g2.drawString(displayLine2, boxX + 14, boxY + 76);

        // Scene counter
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.setColor(new Color(100, 120, 160));
        g2.drawString((currentScene + 1) + " / " + SCENES.length, boxX + boxW - 45, boxY + boxH - 8);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // ── Progress dots ─────────────────────────────────────────────────────────
    private void drawProgressDots(Graphics2D g2, int w, int h) {
        int dotY = h - 18;
        int totalW = SCENES.length * 16;
        int startX = (w - totalW) / 2;
        for (int i = 0; i < SCENES.length; i++) {
            g2.setColor(i == currentScene ? new Color(100, 180, 255) :
                        i < currentScene  ? new Color(60, 100, 160) :
                        new Color(40, 50, 70));
            g2.fillOval(startX + i * 16, dotY, 8, 8);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Color shardColor(int i) {
        String[] e = {"Api","Air","Tanah","Daun","Cahaya","Gelap"};
        return ElementColor.getColor(e[i % 6]);
    }

    private void drawStars(Graphics2D g2, int count, int maxX, int maxY) {
        g2.setColor(new Color(255, 255, 255, 180));
        for (int i = 0; i < count; i++) {
            int sx = (i * 97 + 13) % maxX;
            int sy = (i * 53 + 7)  % maxY;
            int ss = (i % 3 == 0) ? 2 : 1;
            g2.fillOval(sx, sy, ss, ss);
        }
    }

    private void drawTree(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(100, 60, 30));
        g2.fillRect(x + 12, groundY - 50, 10, 50);
        g2.setColor(new Color(40, 140, 40));
        g2.fillOval(x, groundY - 100, 35, 55);
        g2.setColor(new Color(30, 120, 30));
        g2.fillOval(x + 5, groundY - 115, 25, 40);
    }

    private void drawStickFigure(Graphics2D g2, int x, int y, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2));
        g2.fillOval(x - 8, y - 60, 16, 16);          // kepala
        g2.drawLine(x, y - 44, x, y - 10);            // badan
        g2.drawLine(x, y - 35, x - 15, y - 20);       // tangan kiri
        g2.drawLine(x, y - 35, x + 15, y - 20);       // tangan kanan
        g2.drawLine(x, y - 10, x - 10, y + 15);       // kaki kiri
        g2.drawLine(x, y - 10, x + 10, y + 15);       // kaki kanan
        g2.setStroke(new BasicStroke(1));
    }

    private void drawVillainFigure(Graphics2D g2, int x, int y, int w, int h) {
        // Jubah
        g2.setColor(new Color(30, 0, 50));
        int[] jx = {x + w/2 - 20, x + w/2 + 20, x + w, x};
        int[] jy = {y + 80,        y + 80,        y + h,  y + h};
        g2.fillPolygon(jx, jy, 4);

        // Badan
        g2.setColor(new Color(20, 0, 40));
        g2.fillRoundRect(x + w/2 - 25, y + 50, 50, 70, 6, 6);

        // Kepala
        g2.setColor(new Color(15, 0, 25));
        g2.fillOval(x + w/2 - 25, y + 10, 50, 50);

        // Mata merah berpendar
        int eyeGlow = 150 + (int)(Math.sin(animTick * 0.1) * 80);
        g2.setColor(new Color(220, 0, 0, eyeGlow));
        g2.fillOval(x + w/2 - 14, y + 28, 10, 10);
        g2.fillOval(x + w/2 + 4,  y + 28, 10, 10);
        g2.setColor(new Color(255, 50, 50));
        g2.fillOval(x + w/2 - 12, y + 30, 6, 6);
        g2.fillOval(x + w/2 + 6,  y + 30, 6, 6);

        // Mahkota
        g2.setColor(new Color(150, 0, 180));
        int[] crownX = {x+w/2-25, x+w/2-20, x+w/2-10, x+w/2, x+w/2+10, x+w/2+20, x+w/2+25};
        int[] crownY = {y+18,     y+5,       y+18,     y+3,   y+18,     y+5,       y+18};
        g2.fillPolygon(crownX, crownY, 7);

        // Tangan
        g2.setColor(new Color(25, 0, 40));
        g2.fillOval(x + w/2 - 50, y + 65, 25, 20);
        g2.fillOval(x + w/2 + 25, y + 65, 25, 20);
    }
}
