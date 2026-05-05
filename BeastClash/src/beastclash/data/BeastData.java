package beastclash.data;

import beastclash.model.Beast;
import java.util.ArrayList;
import java.util.List;

public class BeastData {

    public static List<Beast> getAllBeasts() {
        List<Beast> beasts = new ArrayList<>();

        // === API (Fire) - ID 1-4 ===
        beasts.add(new Beast(1,  "Ignarex",   "Api",    120, 80,  35, 20, 30));
        beasts.add(new Beast(2,  "Emberon",   "Api",    100, 100, 40, 15, 35));
        beasts.add(new Beast(3,  "Pyroclaw",  "Api",    140, 60,  30, 25, 25));
        beasts.add(new Beast(4,  "Blazewing","Api",     110, 90,  38, 18, 32));

        // === AIR (Water) - ID 5-8 ===
        beasts.add(new Beast(5,  "Aquafang",  "Air",    115, 85,  33, 22, 28));
        beasts.add(new Beast(6,  "Tidecrest", "Air",    130, 70,  28, 28, 22));
        beasts.add(new Beast(7,  "Wavemane",  "Air",    105, 95,  36, 18, 36));
        beasts.add(new Beast(8,  "Hydropus",  "Air",    125, 75,  30, 26, 26));

        // === TANAH (Earth) - ID 9-12 ===
        beasts.add(new Beast(9,  "Bouldrak",  "Tanah",  150, 60,  32, 30, 18));
        beasts.add(new Beast(10, "Rockhorn",  "Tanah",  145, 65,  30, 32, 20));
        beasts.add(new Beast(11, "Mudcrawl",  "Tanah",  135, 70,  28, 28, 22));
        beasts.add(new Beast(12, "Terrafang", "Tanah",  140, 60,  35, 26, 24));

        // === DAUN (Leaf/Nature) - ID 13-16 ===
        beasts.add(new Beast(13, "Thornvine",  "Daun",  110, 90,  34, 20, 30));
        beasts.add(new Beast(14, "Mossclaw",   "Daun",  120, 80,  32, 24, 28));
        beasts.add(new Beast(15, "Leafwing",   "Daun",  100, 100, 38, 16, 38));
        beasts.add(new Beast(16, "Bramblefox", "Daun",  115, 85,  36, 20, 32));

        // === CAHAYA (Light) - ID 17-20 ===
        beasts.add(new Beast(17, "Luminos",    "Cahaya", 115, 90,  36, 20, 34));
        beasts.add(new Beast(18, "Solaris",    "Cahaya", 105, 100, 40, 16, 38));
        beasts.add(new Beast(19, "Radiantis",  "Cahaya", 125, 80,  32, 24, 28));
        beasts.add(new Beast(20, "Glowhawk",   "Cahaya", 110, 95,  38, 18, 36));

        // === GELAP (Dark) - ID 21-24 ===
        beasts.add(new Beast(21, "Shadowfang", "Gelap",  120, 85,  38, 20, 36));
        beasts.add(new Beast(22, "Voidclaw",   "Gelap",  115, 90,  40, 16, 38));
        beasts.add(new Beast(23, "Nightmaw",   "Gelap",  130, 75,  35, 24, 28));
        beasts.add(new Beast(24, "Darkspire",  "Gelap",  125, 80,  36, 22, 32));

        return beasts;
    }

    public static List<Beast> getEnemyTeam(String mapElement, int level) {
        List<Beast> allBeasts = getAllBeasts();
        List<Beast> enemies = new ArrayList<>();

        // Filter beasts by element
        for (Beast b : allBeasts) {
            if (b.getElement().equals(mapElement)) {
                // Clone with scaled stats for level
                int scale = 80 + level * 10;
                enemies.add(new Beast(
                    b.getId(), b.getName(), b.getElement(),
                    b.getMaxHP() * scale / 100,
                    b.getMaxMana(),
                    b.getAttack() * scale / 100,
                    b.getDefense() * scale / 100,
                    b.getSpeed()
                ));
            }
        }
        // return 3 enemies
        while (enemies.size() > 3) enemies.remove(enemies.size() - 1);
        return enemies;
    }
}
