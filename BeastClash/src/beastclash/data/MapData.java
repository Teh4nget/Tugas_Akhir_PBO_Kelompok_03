package beastclash.data;

import beastclash.model.GameMap;
import java.util.ArrayList;
import java.util.List;

public class MapData {
    public static List<GameMap> getMaps() {
        List<GameMap> maps = new ArrayList<>();
        maps.add(new GameMap("Grass Land", true,  3, "Api",   "Daun"));
        maps.add(new GameMap("Blizzard",   false, 3, "Cahaya","Es"));
        maps.add(new GameMap("Volcano",    false, 3, "Tanah", "Api"));
        maps.add(new GameMap("Desert",     false, 3, "Gelap", "Pasir"));
        return maps;
    }
}
