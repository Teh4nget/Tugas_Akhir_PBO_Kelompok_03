package beastclash.gacha;

import beastclash.audio.SoundManager;
import beastclash.database.DatabaseManager;
import beastclash.data.BeastData;
import beastclash.model.Beast;
import java.util.*;

/**
 * GachaSystem – sistem gacha Beast Clash.
 *
 * Biaya    : 1 Telur per pull.
 *
 * Pool & Probabilitas:
 *   - Beast BARU (belum dimiliki) : bobot normal
 *       · Elemen biasa (Api/Air/Tanah/Daun) : bobot 4
 *       · Elemen langka (Cahaya/Gelap)       : bobot 2
 *   - Beast DUPLIKAT (sudah dimiliki) : bobot 1 (semua elemen sama)
 *       → Duplikat tetap BISA keluar, tapi jauh lebih jarang dari beast baru.
 *       → Jika semua beast sudah dimiliki, seluruh pool jadi duplikat (bobot 1).
 *
 * Hasil Duplikat:
 *   Beast yang sudah dimiliki → ditukar otomatis menjadi +3 Telur (shard reward).
 *   Beast baru → langsung unlock dan masuk koleksi.
 *
 * Pity:
 *   Setiap 10 pull berturut-turut tanpa dapat Cahaya/Gelap BARU,
 *   pull ke-10 dijamin mendapat Cahaya atau Gelap (duplikat/baru mengikuti pool).
 */
public class GachaSystem {

    // Bobot untuk beast BARU berdasarkan elemen
    private static final Map<String, Integer> WEIGHT_NEW = new HashMap<>();
    // Bobot untuk beast DUPLIKAT (lebih rendah, sama untuk semua elemen)
    private static final int WEIGHT_DUP = 1;

    // Telur yang dikembalikan jika dapat duplikat
    public static final int SHARD_REWARD = 3;

    static {
        WEIGHT_NEW.put("Api",    4);
        WEIGHT_NEW.put("Air",    4);
        WEIGHT_NEW.put("Tanah",  4);
        WEIGHT_NEW.put("Daun",   4);
        WEIGHT_NEW.put("Cahaya", 2);
        WEIGHT_NEW.put("Gelap",  2);
    }

    private final Random rng = new Random();

    // ── Hasil pull ────────────────────────────────────────────────────────────
    public static class PullResult {
        public final Beast beast;
        public final boolean isDuplicate;
        public final int shardReward;   // telur bonus jika duplikat

        public PullResult(Beast beast, boolean isDuplicate, int shardReward) {
            this.beast       = beast;
            this.isDuplicate = isDuplicate;
            this.shardReward = shardReward;
        }
    }

    // ── Single pull ───────────────────────────────────────────────────────────
    /**
     * Lakukan satu pull gacha.
     * @param userId     user yang melakukan pull
     * @param pityCount  array[0] = jumlah pull sejak terakhir dapat Cahaya/Gelap BARU
     * @return PullResult berisi beast + info duplikat, atau null jika gagal
     */
    public PullResult pull(int userId, int[] pityCount) {
        DatabaseManager db = DatabaseManager.getInstance();

        // Cek telur
        if (db.getEggs(userId) <= 0) return null;

        // Kurangi telur
        if (!db.spendEgg(userId)) return null;

        pityCount[0]++;

        List<Beast> allBeasts = BeastData.getAllBeasts();
        List<Integer> ownedIds = db.getOwnedBeastIds(userId);

        // Pisahkan pool baru vs duplikat
        List<Beast> newPool = new ArrayList<>();
        List<Beast> dupPool = new ArrayList<>();
        for (Beast b : allBeasts) {
            if (ownedIds.contains(b.getId())) dupPool.add(b);
            else                               newPool.add(b);
        }

        Beast result;
        boolean isDuplicate;

        // ── Pity: pull ke-10 → paksa Cahaya/Gelap BARU jika ada ──────────────
        if (pityCount[0] >= 10) {
            List<Beast> rareNew = new ArrayList<>();
            for (Beast b : newPool) {
                if (b.getElement().equals("Cahaya") || b.getElement().equals("Gelap"))
                    rareNew.add(b);
            }
            if (!rareNew.isEmpty()) {
                // Ada beast langka baru → dijamin dapat
                result      = rareNew.get(rng.nextInt(rareNew.size()));
                isDuplicate = false;
                pityCount[0] = 0;
            } else {
                // Semua beast langka sudah dimiliki → roll normal
                result = rollWeighted(newPool, dupPool);
                isDuplicate = dupPool.contains(result);
                if (!isDuplicate &&
                    (result.getElement().equals("Cahaya") || result.getElement().equals("Gelap"))) {
                    pityCount[0] = 0;
                }
            }
        } else {
            // ── Roll normal ───────────────────────────────────────────────────
            result = rollWeighted(newPool, dupPool);
            isDuplicate = dupPool.contains(result);

            // Reset pity jika dapat Cahaya/Gelap BARU
            if (!isDuplicate &&
                (result.getElement().equals("Cahaya") || result.getElement().equals("Gelap"))) {
                pityCount[0] = 0;
            }
        }

        // ── Simpan ke DB / beri shard ─────────────────────────────────────────
        int shard = 0;
        if (!isDuplicate) {
            db.unlockBeast(userId, result.getId());
        } else {
            // Duplikat → kembalikan SHARD_REWARD telur sebagai kompensasi
            db.addEggs(userId, SHARD_REWARD);
            shard = SHARD_REWARD;
        }

        SoundManager.getInstance().playSFX("GACHA");
        return new PullResult(result, isDuplicate, shard);
    }

    /**
     * Weighted random dari gabungan pool baru (bobot penuh) + duplikat (bobot rendah).
     * Jika newPool kosong, roll dari dupPool saja.
     */
    private Beast rollWeighted(List<Beast> newPool, List<Beast> dupPool) {
        // Hitung total bobot
        int total = 0;
        for (Beast b : newPool) total += WEIGHT_NEW.getOrDefault(b.getElement(), 1);
        for (Beast b : dupPool)  total += WEIGHT_DUP;

        if (total <= 0) {
            // Fallback: ambil acak dari seluruh beast
            List<Beast> all = new ArrayList<>(newPool);
            all.addAll(dupPool);
            return all.get(rng.nextInt(all.size()));
        }

        int roll = rng.nextInt(total);
        int cum  = 0;

        for (Beast b : newPool) {
            cum += WEIGHT_NEW.getOrDefault(b.getElement(), 1);
            if (roll < cum) return b;
        }
        for (Beast b : dupPool) {
            cum += WEIGHT_DUP;
            if (roll < cum) return b;
        }

        // Fallback
        return dupPool.isEmpty()
            ? newPool.get(newPool.size() - 1)
            : dupPool.get(dupPool.size() - 1);
    }

    // ── Helper legacy (untuk backward compat jika dipanggil dari tempat lain) ─
    public int getLockedCount(int userId) {
        return 24 - DatabaseManager.getInstance().getOwnedBeastIds(userId).size();
    }

    public boolean isOwned(int userId, int beastId) {
        return DatabaseManager.getInstance().getOwnedBeastIds(userId).contains(beastId);
    }
}
