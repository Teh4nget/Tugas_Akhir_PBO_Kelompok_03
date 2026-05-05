package beastclash.model;

public class GameMap {
    private String name;
    private boolean unlocked;
    private int totalLevels;
    private int completedLevels;
    private String enemyElement;
    private String theme;

    public GameMap(String name, boolean unlocked, int totalLevels, String enemyElement, String theme) {
        this.name = name;
        this.unlocked = unlocked;
        this.totalLevels = totalLevels;
        this.completedLevels = 0;
        this.enemyElement = enemyElement;
        this.theme = theme;
    }

    public boolean isCompleted() {
        return completedLevels >= totalLevels;
    }

    public void completeLevel() {
        if (completedLevels < totalLevels) {
            completedLevels++;
        }
    }

    public String getProgressText() {
        return completedLevels + "/" + totalLevels + " Level";
    }

    // Getters & Setters
    public String getName() { return name; }
    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
    public int getTotalLevels() { return totalLevels; }
    public int getCompletedLevels() { return completedLevels; }
    public String getEnemyElement() { return enemyElement; }
    public String getTheme() { return theme; }
}
