package beastclash.resources;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * ResourceManager – load & cache gambar dari resources.
 *
 * SEMUA aset beast baru sudah menghadap KANAN (untuk tim player).
 *   • Player (kiri layar)  -> gambar ASLI  (sudah menghadap kanan) [OK]
 *   • Enemy  (kanan layar) -> gambar FLIP  (di-flip agar menghadap kiri) [OK]
 */
public class ResourceManager {

    private static ResourceManager instance;
    private final Map<String, BufferedImage> cache = new HashMap<>();

    private ResourceManager() {}

    public static ResourceManager getInstance() {
        if (instance == null) instance = new ResourceManager();
        return instance;
    }

    // ── Map backgrounds ───────────────────────────────────────────────────────
    public BufferedImage getMapBg(String mapName) {
        String key = "map:" + mapName;
        if (cache.containsKey(key)) return cache.get(key);
        BufferedImage img = load("/beastclash/resources/map/" + mapNameToFile(mapName));
        cache.put(key, img);
        return img;
    }

    private String mapNameToFile(String n) {
        switch (n) {
            case "Plains":      return "Plains.png";
            case "Dessert":     return "Dessert.png";
            case "Sea":         return "Sea.png";
            case "Blizzard":    return "Blizzard.png";
            case "Volcano":     return "Volcano.png";
            case "Dark Forest": return "DarkForest.png";
            default:            return "Plains.png";
        }
    }

    public Image getMapBgScaled(String mapName, int w, int h) {
        BufferedImage src = getMapBg(mapName);
        return src != null ? src.getScaledInstance(w, h, Image.SCALE_SMOOTH) : null;
    }

    // ── Beast images ──────────────────────────────────────────────────────────

    /** Gambar asli — semua beast menghadap KANAN. Dipakai untuk PLAYER. */
    public BufferedImage getBeastImg(String name) {
        String key = "beast:" + name;
        if (cache.containsKey(key)) return cache.get(key);
        BufferedImage img = load("/beastclash/resources/beast/" + name.replace(" ", "_") + ".png");
        cache.put(key, img);
        return img;
    }

    /** Gambar di-flip — menghadap KIRI. Dipakai untuk ENEMY. */
    public BufferedImage getBeastImgFlipped(String name) {
        String key = "beast_flip:" + name;
        if (cache.containsKey(key)) return cache.get(key);
        BufferedImage orig = getBeastImg(name);
        BufferedImage flipped = orig != null ? flipH(orig) : null;
        cache.put(key, flipped);
        return flipped;
    }

    /** Untuk PLAYER: gambar asli (menghadap kanan). */
    public BufferedImage getBeastForPlayer(String name) { return getBeastImg(name); }

    /** Untuk ENEMY: gambar di-flip (menghadap kiri). */
    public BufferedImage getBeastForEnemy(String name)  { return getBeastImgFlipped(name); }

    /** Helper scaled — forPlayer=true -> asli, false -> flip. */
    public Image getBeastImgScaled(String name, int size, boolean forPlayer) {
        BufferedImage src = forPlayer ? getBeastForPlayer(name) : getBeastForEnemy(name);
        return src != null ? src.getScaledInstance(size, size, Image.SCALE_SMOOTH) : null;
    }

    // ── Internal ──────────────────────────────────────────────────────────────
    private BufferedImage flipH(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-w, 0);
        new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(src, out);
        return out;
    }

    private BufferedImage load(String path) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) { System.err.println("[Resource] Tidak ditemukan: " + path); return null; }
            return ImageIO.read(is);
        } catch (Exception e) {
            System.err.println("[Resource] Gagal load " + path + ": " + e.getMessage());
            return null;
        }
    }

    public void clearCache() { cache.clear(); }
}
