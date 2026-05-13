package beastclash.data;

import beastclash.model.GameMap;
import java.util.ArrayList;
import java.util.List;

/**
 * MapData – daftar statis semua map dalam Beast Clash.
 *
 * Urutan map & unlock:
 *   1. Hutan Hijau   (Daun)   – terbuka dari awal
 *   2. Padang Pasir  (Api)    – terbuka setelah Hutan Hijau selesai
 *   3. Lautan Biru   (Air)    – terbuka setelah Padang Pasir selesai
 *   4. Gunung Beku   (Air)    – efek Blizzard; terbuka setelah Lautan Biru
 *   5. Gunung Api    (Api)    – efek Volcano; terbuka setelah Gunung Beku
 *   6. Hutan Gelap   (Gelap)  – terbuka setelah Gunung Api
 *
 * Nama map dipakai oleh BattlePanel sebagai kunci efek map, jadi jangan diubah:
 *   "Blizzard" → freeze random, "Desert" / "Volcano" → periodic damage.
 */
public class MapData {

    public static List<GameMap> getMaps() {
        List<GameMap> maps = new ArrayList<>();

        maps.add(new GameMap("Hutan Hijau",  "Daun",   3, true));   // map 0 – selalu terbuka
        maps.add(new GameMap("Desert",        "Api",    3, false));  // map 1 – "Desert" = efek panas
        maps.add(new GameMap("Lautan Biru",  "Air",    3, false));   // map 2
        maps.add(new GameMap("Blizzard",      "Air",    3, false));  // map 3 – efek beku
        maps.add(new GameMap("Volcano",       "Api",    3, false));  // map 4 – efek lava
        maps.add(new GameMap("Hutan Gelap",  "Gelap",  4, false));  // map 5 – boss map

        return maps;
    }
}
