package beastclash.data;

import beastclash.model.Beast;
import java.util.ArrayList;
import java.util.List;

/**
 * BeastData – katalog statis semua beast yang ada dalam game.
 *
 * 24 beast total:
 *   - Api    x4  (id 1-4)
 *   - Air    x4  (id 5-8)
 *   - Tanah  x4  (id 9-12)
 *   - Daun   x4  (id 13-16)
 *   - Cahaya x4  (id 17-20)  ← langka
 *   - Gelap  x4  (id 21-24)  ← langka
 *
 * Starter beast (id 1, 5, 9, 13) dimiliki semua user baru.
 */
public class BeastData {

    private static final List<Beast> ALL = new ArrayList<>();

    static {
        // ── Api ───────────────────────────────────────────────────────────────
        ALL.add(new Beast( 1, "Ignaur",   "Api",    220, 100, 55, 30, 60));
        ALL.add(new Beast( 2, "Pyrodon",  "Api",    200, 110, 65, 25, 65));
        ALL.add(new Beast( 3, "Emberfang","Api",    240, 90,  60, 35, 55));
        ALL.add(new Beast( 4, "Cindrix",  "Api",    210, 120, 70, 20, 70));

        // ── Air ───────────────────────────────────────────────────────────────
        ALL.add(new Beast( 5, "Aquarex",  "Air",    230, 100, 50, 35, 58));
        ALL.add(new Beast( 6, "Tideclaw", "Air",    210, 115, 60, 28, 62));
        ALL.add(new Beast( 7, "Mistwave", "Air",    250, 90,  55, 40, 50));
        ALL.add(new Beast( 8, "Torrent",  "Air",    200, 125, 65, 22, 68));

        // ── Tanah ─────────────────────────────────────────────────────────────
        ALL.add(new Beast( 9, "Terrok",   "Tanah",  260, 80,  55, 50, 40));
        ALL.add(new Beast(10, "Bouldrex", "Tanah",  280, 75,  60, 55, 35));
        ALL.add(new Beast(11, "Gravelon", "Tanah",  245, 90,  50, 45, 45));
        ALL.add(new Beast(12, "Stonefang","Tanah",  270, 80,  65, 48, 38));

        // ── Daun ─────────────────────────────────────────────────────────────
        ALL.add(new Beast(13, "Verdix",   "Daun",   220, 110, 52, 30, 62));
        ALL.add(new Beast(14, "Thornback","Daun",   210, 100, 58, 28, 65));
        ALL.add(new Beast(15, "Mossclaw", "Daun",   235, 95,  55, 35, 58));
        ALL.add(new Beast(16, "Leafcrown","Daun",   200, 130, 60, 25, 70));

        // ── Cahaya (langka) ───────────────────────────────────────────────────
        ALL.add(new Beast(17, "Luminar",  "Cahaya", 230, 120, 65, 35, 65));
        ALL.add(new Beast(18, "Radiance", "Cahaya", 220, 130, 70, 30, 70));
        ALL.add(new Beast(19, "Solaris",  "Cahaya", 250, 110, 60, 40, 60));
        ALL.add(new Beast(20, "Gleamblade","Cahaya",215, 125, 75, 28, 72));

        // ── Gelap (langka) ────────────────────────────────────────────────────
        ALL.add(new Beast(21, "Umbrix",   "Gelap",  240, 115, 70, 32, 67));
        ALL.add(new Beast(22, "Shadowfang","Gelap", 230, 110, 75, 28, 70));
        ALL.add(new Beast(23, "Voidclaw", "Gelap",  255, 100, 65, 38, 62));
        ALL.add(new Beast(24, "Duskhorn", "Gelap",  225, 120, 72, 30, 68));
    }

    /** Kembalikan salinan baru semua beast (agar state tidak di-share). */
    public static List<Beast> getAllBeasts() {
        List<Beast> copy = new ArrayList<>();
        for (Beast b : ALL) {
            copy.add(new Beast(
                b.getId(), b.getName(), b.getElement(),
                b.getMaxHP(), b.getMaxMana(),
                b.getAttack(), b.getDefense(), b.getSpeed()
            ));
        }
        return copy;
    }

    /** Cari beast berdasarkan id, kembalikan null jika tidak ketemu. */
    public static Beast findById(int id) {
        for (Beast b : ALL) {
            if (b.getId() == id) {
                return new Beast(
                    b.getId(), b.getName(), b.getElement(),
                    b.getMaxHP(), b.getMaxMana(),
                    b.getAttack(), b.getDefense(), b.getSpeed()
                );
            }
        }
        return null;
    }

    /**
     * Bangkitkan tim musuh berdasarkan elemen map dan level saat ini.
     * Jumlah musuh = min(3, level). Stat musuh dinaikan per level.
     */
    public static List<Beast> getEnemyTeam(String element, int level) {
        // Semua beast dengan elemen yang cocok = pool utama
        List<Beast> pool = new ArrayList<>();
        for (Beast b : ALL) if (b.getElement().equals(element)) pool.add(b);
        if (pool.isEmpty()) pool.addAll(ALL);

        // Pool campuran (elemen lain) untuk variasi di level tinggi
        List<Beast> mixPool = new ArrayList<>(ALL);

        java.util.Random rng = new java.util.Random();

        // Jumlah enemy: level 1 → 2 enemy, level 2 → 3 enemy, level 3+ → 4-5 enemy
        int count;
        if      (level == 1) count = 2;
        else if (level == 2) count = 3;
        else if (level == 3) count = 4;
        else                 count = 5;

        List<Beast> team = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // 70% dari pool elemen utama, 30% elemen acak (variasi)
            Beast base = (rng.nextDouble() < 0.70 || mixPool.isEmpty())
                ? pool.get(rng.nextInt(pool.size()))
                : mixPool.get(rng.nextInt(mixPool.size()));

            // Skala stat: bertambah per level
            int bonus = (level - 1) * 12;
            // Enemy sedikit lebih lemah dari player agar adil (75% stat)
            team.add(new Beast(
                base.getId(),
                base.getName(),
                base.getElement(),
                (int)((base.getMaxHP()   + bonus) * 0.85),
                (int)((base.getMaxMana() + bonus / 2) * 0.85),
                (int)((base.getAttack()  + bonus / 3) * 0.80),
                (int)((base.getDefense() + bonus / 4) * 0.80),
                base.getSpeed() + rng.nextInt(5) // sedikit variasi speed
            ));
        }
        return team;
    }

    /** Id beast yang dimiliki semua user baru (starter). */
    public static List<Integer> getStarterIds() {
        List<Integer> ids = new ArrayList<>();
        ids.add(1);   // Ignaur   (Api)
        ids.add(5);   // Aquarex  (Air)
        ids.add(9);   // Terrok   (Tanah)
        ids.add(13);  // Verdix   (Daun)
        return ids;
    }
}
