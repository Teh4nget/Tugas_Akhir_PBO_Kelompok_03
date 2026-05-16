package beastclash.gacha;

import beastclash.audio.SoundManager;
import beastclash.controller.GameState;
import beastclash.database.DatabaseManager;
import beastclash.data.BeastData;
import beastclash.model.Beast;
import java.util.*;

/**
 * GachaSystem – tanpa shard reward.
 * Duplikat tetap bisa keluar (bobot lebih rendah) tapi tidak ada telur kembali.
 *
 * Pool & bobot:
 *   Beast BARU  – Api/Air/Tanah/Daun : 4 | Cahaya/Gelap : 2
 *   Beast DUPLIKAT (semua elemen)     : 1  (bisa keluar, jarang)
 * Pity ke-10 : dijamin Cahaya/Gelap BARU jika masih ada.
 */
public class GachaSystem {

    private static final Map<String, Integer> WEIGHT_NEW = new HashMap<>();
    private static final int WEIGHT_DUP = 1;

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
        public final Beast   beast;
        public final boolean isDuplicate;

        public PullResult(Beast beast, boolean isDuplicate) {
            this.beast       = beast;
            this.isDuplicate = isDuplicate;
        }
    }

    // ── Single pull ───────────────────────────────────────────────────────────
    public PullResult pull(int userId, int[] pityCount) {
        DatabaseManager db = DatabaseManager.getInstance();
        GameState gs = GameState.getInstance();
        boolean isOffline = (userId <= 0 || !db.isConnected());

        // Cek & kurangi telur
        if (isOffline) {
            if (gs.getOfflineEggs() <= 0) return null;
            if (!gs.spendOfflineEgg()) return null;
        } else {
            if (db.getEggs(userId) <= 0) return null;
            if (!db.spendEgg(userId)) return null;
        }

        pityCount[0]++;

        // Pool beast: offline pakai offlineOwnedIds (starter + hasil gacha sesi ini)
        List<Beast> all = BeastData.getAllBeasts();
        List<Integer> ownedIds = isOffline
            ? gs.getOfflineOwnedIds()
            : db.getOwnedBeastIds(userId);

        List<Beast> newPool = new ArrayList<>();
        List<Beast> dupPool = new ArrayList<>();
        for (Beast b : all) {
            if (ownedIds.contains(b.getId())) dupPool.add(b);
            else                            newPool.add(b);
        }

        Beast   result;
        boolean isDuplicate;

        // Pity ke-10 -> paksa Cahaya/Gelap BARU
        if (pityCount[0] >= 10) {
            List<Beast> rareNew = new ArrayList<>();
            for (Beast b : newPool)
                if (b.getElement().equals("Cahaya") || b.getElement().equals("Gelap"))
                    rareNew.add(b);

            if (!rareNew.isEmpty()) {
                result      = rareNew.get(rng.nextInt(rareNew.size()));
                isDuplicate = false;
                pityCount[0] = 0;
            } else {
                result      = rollWeighted(newPool, dupPool);
                isDuplicate = dupPool.contains(result);
                if (!isDuplicate && isRare(result)) pityCount[0] = 0;
            }
        } else {
            result      = rollWeighted(newPool, dupPool);
            isDuplicate = dupPool.contains(result);
            if (!isDuplicate && isRare(result)) pityCount[0] = 0;
        }

        // Simpan beast baru: ke DB jika online, ke memori jika offline
        if (!isDuplicate) {
            if (!isOffline) {
                db.unlockBeast(userId, result.getId());
            } else {
                // Simpan ke offlineOwnedIds agar pull berikutnya tahu beast ini sudah dimiliki
                gs.addOfflineOwnedBeast(result.getId());
            }
        }

        SoundManager.getInstance().playSFX("GACHA");
        return new PullResult(result, isDuplicate);
    }

    private boolean isRare(Beast b) {
        return b.getElement().equals("Cahaya") || b.getElement().equals("Gelap");
    }

    private Beast rollWeighted(List<Beast> newPool, List<Beast> dupPool) {
        int total = 0;
        for (Beast b : newPool) total += WEIGHT_NEW.getOrDefault(b.getElement(), 1);
        for (Beast b : dupPool)  total += WEIGHT_DUP;

        if (total <= 0) {
            List<Beast> all = new ArrayList<>(newPool);
            all.addAll(dupPool);
            return all.get(rng.nextInt(all.size()));
        }

        int roll = rng.nextInt(total), cum = 0;
        for (Beast b : newPool) { cum += WEIGHT_NEW.getOrDefault(b.getElement(), 1); if (roll < cum) return b; }
        for (Beast b : dupPool)  { cum += WEIGHT_DUP; if (roll < cum) return b; }

        return dupPool.isEmpty()
            ? newPool.get(newPool.size()-1)
            : dupPool.get(dupPool.size()-1);
    }

    public boolean isOwned(int userId, int beastId) {
        return DatabaseManager.getInstance().getOwnedBeastIds(userId).contains(beastId);
    }
}
