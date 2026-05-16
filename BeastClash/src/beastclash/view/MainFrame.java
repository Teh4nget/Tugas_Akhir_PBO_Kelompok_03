package beastclash.view;

import beastclash.audio.SoundManager;
import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainContainer;

    public static final String SCREEN_LOGIN  = "LOGIN";
    public static final String SCREEN_STORY  = "STORY";
    public static final String SCREEN_MENU   = "MENU";
    public static final String SCREEN_MAP    = "MAP";
    public static final String SCREEN_BEAST  = "BEAST";
    public static final String SCREEN_BATTLE = "BATTLE";
    public static final String SCREEN_GACHA  = "GACHA";
    public static final String SCREEN_ENDING = "ENDING";

    private static final Dimension SIZE_LOGIN  = new Dimension(560, 560);
    private static final Dimension SIZE_STORY  = new Dimension(680, 520);
    private static final Dimension SIZE_MENU   = new Dimension(560, 600);
    private static final Dimension SIZE_MAP    = new Dimension(560, 640);
    private static final Dimension SIZE_BEAST  = new Dimension(760, 640);
    private static final Dimension SIZE_BATTLE = new Dimension(920, 640);
    private static final Dimension SIZE_GACHA  = new Dimension(580, 580);
    private static final Dimension SIZE_ENDING = new Dimension(720, 580);

    public MainFrame() {
        setTitle("Beast Clash");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        cardLayout    = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        setContentPane(mainContainer);

        // Tampilkan login sebagai layar pertama
        switchTo(SCREEN_LOGIN, new LoginPanel(this), SIZE_LOGIN);
    }

    // =========================================================================
    //  Public navigation methods
    // =========================================================================
    public void showLogin() {
        switchTo(SCREEN_LOGIN, new LoginPanel(this), SIZE_LOGIN);
    }

    public void showStory() {
        switchTo(SCREEN_STORY, new StoryIntroPanel(this), SIZE_STORY);
    }

    public void showMainMenu() {
        SoundManager.getInstance().playBGM("MENU");
        switchTo(SCREEN_MENU, new MainMenuPanel(this), SIZE_MENU);
    }

    public void showMapSelect() {
        switchTo(SCREEN_MAP, new MapSelectPanel(this), SIZE_MAP);
    }

    public void showBeastSelect() {
        switchTo(SCREEN_BEAST, new BeastSelectPanel(this), SIZE_BEAST);
    }

    public void showBattle() {
        SoundManager.getInstance().playBGM("BATTLE");
        switchTo(SCREEN_BATTLE, new BattlePanel(this), SIZE_BATTLE);
    }

    public void showGacha() {
        switchTo(SCREEN_GACHA, new GachaPanel(this), SIZE_GACHA);
    }

    public void showEnding() {
        SoundManager.getInstance().playBGM("VICTORY");
        switchTo(SCREEN_ENDING, new EndingPanel(this), SIZE_ENDING);
    }

    // =========================================================================
    //  Core switch logic  –  HAPUS semua panel lama, tambah baru, resize, show
    // Interface untuk panel yang punya resource yang perlu dibersihkan
    public interface Cleanable {
        void cleanup();
    }

    // =========================================================================
    private void switchTo(String screenName, JPanel panel, Dimension size) {
        // 0. Cleanup resource panel lama (timer, dll) agar tidak ada timer zombie
        for (Component c : mainContainer.getComponents()) {
            if (c instanceof Cleanable) {
                ((Cleanable) c).cleanup();
            }
        }

        // 1. Hapus semua komponen lama agar tidak menumpuk
        mainContainer.removeAll();

        // 2. Set preferred size panel agar pack() bekerja benar
        panel.setPreferredSize(size);

        // 3. Tambahkan HANYA satu panel ke container
        mainContainer.add(panel, screenName);

        // 4. Paksa CardLayout menampilkan panel ini
        cardLayout.show(mainContainer, screenName);

        // 5. Resize window ke ukuran yang ditentukan
        //    Hitung insets (title bar + border) agar konten pas
        Insets insets = getInsets();
        int w = size.width  + insets.left + insets.right;
        int h = size.height + insets.top  + insets.bottom;
        setSize(w, h);

        // 6. Refresh layout
        mainContainer.revalidate();
        mainContainer.repaint();

        // 7. Tengahkan window di layar
        setLocationRelativeTo(null);
    }

    // =========================================================================
    //  Entry point
    // =========================================================================
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            MainFrame f = new MainFrame();
            f.setVisible(true);
        });
    }
}
