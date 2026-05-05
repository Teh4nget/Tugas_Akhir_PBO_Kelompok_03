package beastclash.view;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainContainer;

    private static final String SCREEN_STORY  = "STORY";
    private static final String SCREEN_MENU   = "MENU";
    private static final String SCREEN_MAP    = "MAP";
    private static final String SCREEN_BEAST  = "BEAST";
    private static final String SCREEN_BATTLE = "BATTLE";

    public MainFrame() {
        setTitle("Beast Clash");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        // Mulai dari story intro
        mainContainer.add(new StoryIntroPanel(this), SCREEN_STORY);
        mainContainer.add(new MainMenuPanel(this),   SCREEN_MENU);

        setContentPane(mainContainer);
        pack();
        setLocationRelativeTo(null);
        cardLayout.show(mainContainer, SCREEN_STORY);
    }

    public void showStory() {
        replacePanel(new StoryIntroPanel(this), SCREEN_STORY);
        setSize(600, 480);
        cardLayout.show(mainContainer, SCREEN_STORY);
        setLocationRelativeTo(null);
    }

    public void showMainMenu() {
        replacePanel(new MainMenuPanel(this), SCREEN_MENU);
        cardLayout.show(mainContainer, SCREEN_MENU);
        pack();
        setLocationRelativeTo(null);
    }

    public void showMapSelect() {
        replacePanel(new MapSelectPanel(this), SCREEN_MAP);
        cardLayout.show(mainContainer, SCREEN_MAP);
        pack();
        setLocationRelativeTo(null);
    }

    public void showBeastSelect() {
        replacePanel(new BeastSelectPanel(this), SCREEN_BEAST);
        cardLayout.show(mainContainer, SCREEN_BEAST);
        pack();
        setLocationRelativeTo(null);
    }

    public void showBattle() {
        replacePanel(new BattlePanel(this), SCREEN_BATTLE);
        cardLayout.show(mainContainer, SCREEN_BATTLE);
        setSize(820, 560);
        setLocationRelativeTo(null);
    }

    private void replacePanel(JPanel panel, String name) {
        mainContainer.add(panel, name);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
