package beastclash.controller;

import beastclash.data.MapData;
import beastclash.model.Beast;
import beastclash.model.GameMap;
import java.util.ArrayList;
import java.util.List;

public class GameState {
    private static GameState instance;

    private List<GameMap> maps;
    private List<Beast> playerTeam;
    private List<Beast> enemyTeam;
    private GameMap selectedMap;
    private int currentLevel;
    private int activeBeastIndex;
    private int activeEnemyIndex;

    private GameState() {
        maps = MapData.getMaps();
        playerTeam = new ArrayList<>();
        enemyTeam = new ArrayList<>();
        currentLevel = 1;
        activeBeastIndex = 0;
        activeEnemyIndex = 0;
    }

    public static GameState getInstance() {
        if (instance == null) instance = new GameState();
        return instance;
    }

    public void resetBattle() {
        activeBeastIndex = 0;
        activeEnemyIndex = 0;
        for (Beast b : playerTeam) b.reset();
        for (Beast b : enemyTeam) b.reset();
    }

    public Beast getActiveBeast() {
        for (int i = activeBeastIndex; i < playerTeam.size(); i++) {
            if (playerTeam.get(i).isAlive()) {
                activeBeastIndex = i;
                return playerTeam.get(i);
            }
        }
        return null;
    }

    public Beast getActiveEnemy() {
        for (int i = activeEnemyIndex; i < enemyTeam.size(); i++) {
            if (enemyTeam.get(i).isAlive()) {
                activeEnemyIndex = i;
                return enemyTeam.get(i);
            }
        }
        return null;
    }

    public void nextAliveEnemy() {
        activeEnemyIndex++;
    }

    public boolean isPlayerDefeated() {
        for (Beast b : playerTeam) {
            if (b.isAlive()) return false;
        }
        return true;
    }

    public boolean isEnemyDefeated() {
        for (Beast b : enemyTeam) {
            if (b.isAlive()) return false;
        }
        return true;
    }

    public void switchActiveBeast(int index) {
        if (index >= 0 && index < playerTeam.size() && playerTeam.get(index).isAlive()) {
            activeBeastIndex = index;
        }
    }

    // Getters & Setters
    public List<GameMap> getMaps() { return maps; }
    public List<Beast> getPlayerTeam() { return playerTeam; }
    public void setPlayerTeam(List<Beast> team) { this.playerTeam = team; }
    public List<Beast> getEnemyTeam() { return enemyTeam; }
    public void setEnemyTeam(List<Beast> team) { this.enemyTeam = team; }
    public GameMap getSelectedMap() { return selectedMap; }
    public void setSelectedMap(GameMap map) { this.selectedMap = map; }
    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int level) { this.currentLevel = level; }
    public int getActiveBeastIndex() { return activeBeastIndex; }
    public int getActiveEnemyIndex() { return activeEnemyIndex; }
}
