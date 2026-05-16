package beastclash.data;

import beastclash.model.Beast;
import java.util.ArrayList;
import java.util.List;

/**
 * BeastData – katalog 24 Beast dengan nama & aset gambar baru.
 *
 * ID mapping:
 *   Api    : 1=Blazefang, 2=Cinderion, 3=Ignarox,     4=Pyroth
 *   Air    : 5=Aquarion,  6=Marivex,   7=Nerevion,    8=Tsunadra
 *   Tanah  : 9=Bedrock Titan, 10=Gravok, 11=Quakron,  12=Terragorn
 *   Daun   : 13=Floravine, 14=Luminaire,15=Mossdrake, 16=Rootzilla
 *   Cahaya : 17=Aetherion, 18=Luxeron,  19=Radiantor,  20=Solareth
 *   Gelap  : 21=Morvexis,  22=Noctyra,  23=Shadowfang, 24=Umbrax
 *
 * Starter beast (id 1=Blazefang, 5=Aquarion, 9=Bedrock Titan, 13=Floravine)
 */
public class BeastData {

    private static final List<Beast> ALL = new ArrayList<>();

    static {
        // ── Api ───────────────────────────────────────────────────────────────
        ALL.add(new Beast( 1, "Blazefang",    "Api",    220, 100, 55, 30, 60));
        ALL.add(new Beast( 2, "Cinderion",    "Api",    200, 110, 65, 25, 65));
        ALL.add(new Beast( 3, "Ignarox",      "Api",    240,  90, 60, 35, 55));
        ALL.add(new Beast( 4, "Pyroth",       "Api",    210, 120, 70, 20, 70));

        // ── Air ───────────────────────────────────────────────────────────────
        ALL.add(new Beast( 5, "Aquarion",     "Air",    230, 100, 50, 35, 58));
        ALL.add(new Beast( 6, "Marivex",      "Air",    210, 115, 60, 28, 62));
        ALL.add(new Beast( 7, "Nerevion",     "Air",    250,  90, 55, 40, 50));
        ALL.add(new Beast( 8, "Tsunadra",     "Air",    200, 125, 65, 22, 68));

        // ── Tanah ─────────────────────────────────────────────────────────────
        ALL.add(new Beast( 9, "Bedrock Titan","Tanah",  260,  80, 55, 50, 40));
        ALL.add(new Beast(10, "Gravok",       "Tanah",  280,  75, 60, 55, 35));
        ALL.add(new Beast(11, "Quakron",      "Tanah",  245,  90, 50, 45, 45));
        ALL.add(new Beast(12, "Terragorn",    "Tanah",  270,  80, 65, 48, 38));

        // ── Daun ─────────────────────────────────────────────────────────────
        ALL.add(new Beast(13, "Floravine",    "Daun",   220, 110, 52, 30, 62));
        ALL.add(new Beast(14, "Luminaire",    "Daun",   210, 100, 58, 28, 65));
        ALL.add(new Beast(15, "Mossdrake",    "Daun",   235,  95, 55, 35, 58));
        ALL.add(new Beast(16, "Rootzilla",    "Daun",   200, 130, 60, 25, 70));

        // ── Cahaya (langka) ───────────────────────────────────────────────────
        ALL.add(new Beast(17, "Aetherion",    "Cahaya", 230, 120, 65, 35, 65));
        ALL.add(new Beast(18, "Luxeron",      "Cahaya", 220, 130, 70, 30, 70));
        ALL.add(new Beast(19, "Radiantor",    "Cahaya", 250, 110, 60, 40, 60));
        ALL.add(new Beast(20, "Solareth",     "Cahaya", 215, 125, 75, 28, 72));

        // ── Gelap (langka) ────────────────────────────────────────────────────
        ALL.add(new Beast(21, "Morvexis",     "Gelap",  240, 115, 70, 32, 67));
        ALL.add(new Beast(22, "Noctyra",      "Gelap",  230, 110, 75, 28, 70));
        ALL.add(new Beast(23, "Shadowfang",   "Gelap",  255, 100, 65, 38, 62));
        ALL.add(new Beast(24, "Umbrax",       "Gelap",  225, 120, 72, 30, 68));
    }

    public static List<Beast> getAllBeasts() {
        List<Beast> copy = new ArrayList<>();
        for (Beast b : ALL) copy.add(cloneBean(b));
        return copy;
    }

    public static Beast findById(int id) {
        for (Beast b : ALL) if (b.getId() == id) return cloneBean(b);
        return null;
    }

    private static Beast cloneBean(Beast b) {
        return new Beast(b.getId(), b.getName(), b.getElement(),
            b.getMaxHP(), b.getMaxMana(), b.getAttack(), b.getDefense(), b.getSpeed());
    }

    public static List<Beast> getEnemyTeam(String element, int level) {
        List<Beast> pool = new ArrayList<>();
        for (Beast b : ALL) if (b.getElement().equals(element)) pool.add(b);
        if (pool.isEmpty()) pool.addAll(ALL);

        List<Beast> mixPool = new ArrayList<>(ALL);
        java.util.Random rng = new java.util.Random();

        int count;
        if      (level == 1) count = 2;
        else if (level == 2) count = 3;
        else if (level == 3) count = 4;
        else                 count = 5;

        List<Beast> team = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Beast base = (rng.nextDouble() < 0.70 || mixPool.isEmpty())
                ? pool.get(rng.nextInt(pool.size()))
                : mixPool.get(rng.nextInt(mixPool.size()));
            int bonus = (level - 1) * 12;
            team.add(new Beast(base.getId(), base.getName(), base.getElement(),
                (int)((base.getMaxHP()   + bonus) * 0.85),
                (int)((base.getMaxMana() + bonus / 2) * 0.85),
                (int)((base.getAttack()  + bonus / 3) * 0.80),
                (int)((base.getDefense() + bonus / 4) * 0.80),
                base.getSpeed() + rng.nextInt(5)));
        }
        return team;
    }

    public static List<Integer> getStarterIds() {
        // 5 beast starter: 1 per elemen (Api, Air, Tanah, Daun) + 1 bonus Api
        // Blazefang(1), Aquarion(5), Bedrock Titan(9), Floravine(13), Ignarox(3)
        return java.util.Arrays.asList(1, 3, 5, 9, 13);
    }
}
