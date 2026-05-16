package beastclash.data;

import beastclash.model.GameMap;
import java.util.ArrayList;
import java.util.List;

/**
 * MapData – daftar map dengan nama sesuai aset gambar.
 *
 * Nama map HARUS cocok dengan ResourceManager.mapNameToFile():
 *   "Plains"      -> Plains.png
 *   "Dessert"     -> Dessert.png
 *   "Sea"         -> Sea.png
 *   "Blizzard"    -> Blizzard.png
 *   "Volcano"     -> Volcano.png
 *   "Dark Forest" -> DarkForest.png
 *
 * Nama juga dipakai sebagai kunci efek map di BattlePanel.startMapEffects().
 */
public class MapData {

    public static List<GameMap> getMaps() {
        List<GameMap> maps = new ArrayList<>();
        maps.add(new GameMap("Plains",      "Daun",   3, true));   // map 0 – selalu terbuka
        maps.add(new GameMap("Sea",         "Air",    3, false));  // map 1
        maps.add(new GameMap("Dessert",     "Tanah",  3, false));  // map 2 – elemen Tanah
        maps.add(new GameMap("Blizzard",    "Cahaya", 3, false));  // map 3 – elemen Cahaya
        maps.add(new GameMap("Volcano",     "Api",    3, false));  // map 4 – efek lava
        maps.add(new GameMap("Dark Forest", "Gelap",  4, false));  // map 5 – boss map
        return maps;
    }
}
