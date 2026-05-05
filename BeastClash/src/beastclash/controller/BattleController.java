package beastclash.controller;

import beastclash.model.Beast;
import java.util.Random;

public class BattleController {
    private static final Random random = new Random();
    private GameState state;

    public BattleController() {
        this.state = GameState.getInstance();
    }

    public enum ActionResult {
        HIT, EFFECTIVE, NOT_EFFECTIVE, MISS, FAINTED
    }

    public static class BattleResult {
        public String log;
        public boolean playerFainted;
        public boolean enemyFainted;
        public boolean allEnemyDefeated;
        public boolean allPlayerDefeated;
        public int damageDealt;
        public int damageReceived;

        public BattleResult(String log) { this.log = log; }
    }

    public BattleResult performAttack() {
        Beast player = state.getActiveBeast();
        Beast enemy = state.getActiveEnemy();
        if (player == null || enemy == null) return new BattleResult("Tidak ada beast aktif!");

        BattleResult result = new BattleResult("");
        StringBuilder log = new StringBuilder();

        // Player attacks
        int dmg = player.calculateAttackDamage(enemy);
        dmg = applyVariance(dmg);
        enemy.takeDamage(dmg);
        result.damageDealt = dmg;
        double mult = player.getElementMultiplier(enemy.getElement());
        log.append(player.getName()).append(" menyerang ").append(enemy.getName())
           .append(" dengan ").append(dmg).append(" damage!");
        if (mult > 1.0) log.append(" [SANGAT EFEKTIF!]");
        else if (mult < 1.0) log.append(" [Kurang efektif...]");
        log.append("\n");

        // Restore some mana on attack
        player.restoreMana(10);

        if (!enemy.isAlive()) {
            log.append(enemy.getName()).append(" pingsan!\n");
            result.enemyFainted = true;
            state.nextAliveEnemy();
            if (state.isEnemyDefeated()) {
                result.allEnemyDefeated = true;
                log.append("🏆 Semua musuh dikalahkan!\n");
                result.log = log.toString();
                return result;
            }
            enemy = state.getActiveEnemy();
            if (enemy != null) log.append("Musuh mengeluarkan ").append(enemy.getName()).append("!\n");
        }

        // Enemy attacks back
        if (enemy != null && enemy.isAlive()) {
            int eDmg = enemy.calculateAttackDamage(player);
            eDmg = applyVariance(eDmg);
            player.takeDamage(eDmg);
            result.damageReceived = eDmg;
            log.append(enemy.getName()).append(" membalas dengan ").append(eDmg).append(" damage!\n");

            if (!player.isAlive()) {
                log.append(player.getName()).append(" pingsan!\n");
                result.playerFainted = true;
                if (state.isPlayerDefeated()) {
                    result.allPlayerDefeated = true;
                    log.append("💀 Semua beast mu dikalahkan!\n");
                }
            }
        }

        result.log = log.toString();
        return result;
    }

    public BattleResult performSkill() {
        Beast player = state.getActiveBeast();
        Beast enemy = state.getActiveEnemy();
        if (player == null || enemy == null) return new BattleResult("Tidak ada beast aktif!");

        if (player.getCurrentMana() < 25) {
            return new BattleResult("Mana tidak cukup untuk Skill! (Butuh 25 Mana)\n");
        }

        BattleResult result = new BattleResult("");
        StringBuilder log = new StringBuilder();

        player.useMana(25);
        int dmg = player.calculateSkillDamage(enemy);
        dmg = applyVariance(dmg);
        enemy.takeDamage(dmg);
        result.damageDealt = dmg;

        double mult = player.getElementMultiplier(enemy.getElement());
        log.append("✨ ").append(player.getName()).append(" menggunakan SKILL! ")
           .append(enemy.getName()).append(" terkena ").append(dmg).append(" damage!");
        if (mult > 1.0) log.append(" [SANGAT EFEKTIF!]");
        else if (mult < 1.0) log.append(" [Kurang efektif...]");
        log.append("\n");

        if (!enemy.isAlive()) {
            log.append(enemy.getName()).append(" pingsan!\n");
            result.enemyFainted = true;
            state.nextAliveEnemy();
            if (state.isEnemyDefeated()) {
                result.allEnemyDefeated = true;
                log.append("🏆 Semua musuh dikalahkan!\n");
                result.log = log.toString();
                return result;
            }
            enemy = state.getActiveEnemy();
            if (enemy != null) log.append("Musuh mengeluarkan ").append(enemy.getName()).append("!\n");
        }

        // Enemy counter
        if (enemy != null && enemy.isAlive()) {
            int eDmg = enemy.calculateAttackDamage(player);
            eDmg = applyVariance(eDmg);
            player.takeDamage(eDmg);
            result.damageReceived = eDmg;
            log.append(enemy.getName()).append(" membalas dengan ").append(eDmg).append(" damage!\n");
            if (!player.isAlive()) {
                log.append(player.getName()).append(" pingsan!\n");
                result.playerFainted = true;
                if (state.isPlayerDefeated()) {
                    result.allPlayerDefeated = true;
                    log.append("💀 Semua beast mu dikalahkan!\n");
                }
            }
        }

        result.log = log.toString();
        return result;
    }

    public BattleResult performUltimate() {
        Beast player = state.getActiveBeast();
        Beast enemy = state.getActiveEnemy();
        if (player == null || enemy == null) return new BattleResult("Tidak ada beast aktif!");

        if (player.getCurrentMana() < 60) {
            return new BattleResult("Mana tidak cukup untuk Ultimate! (Butuh 60 Mana)\n");
        }

        BattleResult result = new BattleResult("");
        StringBuilder log = new StringBuilder();

        player.useMana(60);
        int dmg = player.calculateUltimateDamage(enemy);
        dmg = applyVariance(dmg);
        enemy.takeDamage(dmg);
        result.damageDealt = dmg;

        double mult = player.getElementMultiplier(enemy.getElement());
        log.append("💥 ").append(player.getName()).append(" melepaskan ULTIMATE! ")
           .append(enemy.getName()).append(" terkena ").append(dmg).append(" damage!");
        if (mult > 1.0) log.append(" [SANGAT EFEKTIF!]");
        else if (mult < 1.0) log.append(" [Kurang efektif...]");
        log.append("\n");

        // Ultimate also hits all enemies for splash
        for (Beast e : GameState.getInstance().getEnemyTeam()) {
            if (e.isAlive() && e != enemy) {
                int splash = dmg / 3;
                e.takeDamage(splash);
                log.append("💢 Splash! ").append(e.getName()).append(" terkena ").append(splash).append(" damage!\n");
            }
        }

        if (!enemy.isAlive()) {
            log.append(enemy.getName()).append(" pingsan!\n");
            result.enemyFainted = true;
            state.nextAliveEnemy();
            if (state.isEnemyDefeated()) {
                result.allEnemyDefeated = true;
                log.append("🏆 Semua musuh dikalahkan!\n");
                result.log = log.toString();
                return result;
            }
            enemy = state.getActiveEnemy();
            if (enemy != null) log.append("Musuh mengeluarkan ").append(enemy.getName()).append("!\n");
        }

        // Enemy counter (weaker after ultimate)
        if (enemy != null && enemy.isAlive()) {
            int eDmg = (int)(enemy.calculateAttackDamage(player) * 0.7);
            eDmg = applyVariance(eDmg);
            player.takeDamage(eDmg);
            result.damageReceived = eDmg;
            log.append(enemy.getName()).append(" membalas dengan ").append(eDmg).append(" damage!\n");
            if (!player.isAlive()) {
                log.append(player.getName()).append(" pingsan!\n");
                result.playerFainted = true;
                if (state.isPlayerDefeated()) {
                    result.allPlayerDefeated = true;
                    log.append("💀 Semua beast mu dikalahkan!\n");
                }
            }
        }

        result.log = log.toString();
        return result;
    }

    public BattleResult performRun() {
        int chance = random.nextInt(100);
        if (chance < 60) {
            return new BattleResult("🏃 Kamu berhasil kabur dari pertarungan!\n[RUN_SUCCESS]");
        } else {
            BattleResult result = new BattleResult("");
            StringBuilder log = new StringBuilder();
            log.append("❌ Gagal kabur!\n");

            Beast player = state.getActiveBeast();
            Beast enemy = state.getActiveEnemy();
            if (player != null && enemy != null) {
                int eDmg = enemy.calculateAttackDamage(player);
                eDmg = applyVariance(eDmg);
                player.takeDamage(eDmg);
                result.damageReceived = eDmg;
                log.append(enemy.getName()).append(" menyerang! ").append(eDmg).append(" damage!\n");
                if (!player.isAlive()) {
                    log.append(player.getName()).append(" pingsan!\n");
                    result.playerFainted = true;
                    if (state.isPlayerDefeated()) {
                        result.allPlayerDefeated = true;
                    }
                }
            }
            result.log = log.toString();
            return result;
        }
    }

    private int applyVariance(int base) {
        int variance = (int)(base * 0.15);
        return base - variance + random.nextInt(variance * 2 + 1);
    }
}
