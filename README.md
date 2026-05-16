# 🐉 Beast Clash — Dokumentasi Struktur Game Lengkap

> Dokumen ini menjelaskan arsitektur kode, struktur file, distribusi baris kode, fungsi tiap file, serta bagian-bagian penting yang menjalankan game Beast Clash secara menyeluruh.

---

## 📊 Ringkasan Statistik Proyek

| Kategori | Jumlah |
|---|---|
| **Total file Java** | 22 file |
| **Total baris kode** | 6.603 baris |
| **Gambar Beast** | 24 file PNG (1080×1080) |
| **Gambar Map** | 6 file PNG |
| **Package** | 7 package |
| **Library eksternal** | 1 (MySQL Connector/J 9.5.0) |

---

## 🗂️ Peta Struktur Lengkap Proyek

```
BeastClash/
├── src/beastclash/
│   ├── Main.java                           (18 baris)  — Entry point
│   │
│   ├── audio/
│   │   └── SoundManager.java              (503 baris)  — PCM synthesis audio engine
│   │
│   ├── controller/
│   │   ├── BattleController.java          (342 baris)  — Turn-based battle engine
│   │   └── GameState.java                 (214 baris)  — Singleton state global
│   │
│   ├── data/
│   │   ├── BeastData.java                 (113 baris)  — Katalog 24 Beast
│   │   └── MapData.java                   (32 baris)   — Daftar 6 Map
│   │
│   ├── database/
│   │   └── DatabaseManager.java           (336 baris)  — Koneksi MySQL & query
│   │
│   ├── gacha/
│   │   └── GachaSystem.java               (142 baris)  — Sistem gacha + pity
│   │
│   ├── model/
│   │   ├── Beast.java                     (152 baris)  — Entity Beast + combat logic
│   │   └── GameMap.java                   (57 baris)   — Entity Map + progress
│   │
│   ├── resources/
│   │   ├── ResourceManager.java           (112 baris)  — Load & cache aset gambar
│   │   ├── beast/                         (24 PNG)     — Sprite 24 Beast
│   │   └── map/                           (6 PNG)      — Background 6 Map
│   │
│   └── view/
│       ├── MainFrame.java                 (135 baris)  — Window utama & navigasi
│       ├── LoginPanel.java                (448 baris)  — Layar login / register
│       ├── StoryIntroPanel.java           (599 baris)  — Prolog animasi Zenith
│       ├── MainMenuPanel.java             (267 baris)  — Menu utama
│       ├── MapSelectPanel.java            (313 baris)  — Pilih map & level
│       ├── BeastSelectPanel.java          (586 baris)  — Pilih 5 Beast (tim)
│       ├── BattlePanel.java              (1381 baris)  — Arena battle lengkap
│       ├── GachaPanel.java                (476 baris)  — Sistem gacha visual
│       ├── EndingPanel.java               (259 baris)  — Layar ending
│       ├── HPBar.java                     (72 baris)   — Komponen HP/MP bar
│       └── ElementColor.java              (46 baris)   — Mapping warna elemen
│
├── lib/
│   └── mysql-connector-j-9.5.0.jar       — Driver MySQL
│
└── dist/
    └── BeastClash.jar                     — Distribusi executable
```

---

## 📈 Distribusi Baris Kode per File

```
BattlePanel.java        ████████████████████████████████████  1381 baris  (20.9%)
StoryIntroPanel.java    ██████████████████                      599 baris   (9.1%)
BeastSelectPanel.java   ██████████████████                      586 baris   (8.9%)
SoundManager.java       ███████████████                         503 baris   (7.6%)
GachaPanel.java         ██████████████                          476 baris   (7.2%)
LoginPanel.java         █████████████                           448 baris   (6.8%)
BattleController.java   ██████████                              342 baris   (5.2%)
DatabaseManager.java    ██████████                              336 baris   (5.1%)
MapSelectPanel.java     █████████                               313 baris   (4.7%)
MainMenuPanel.java       ████████                               267 baris   (4.0%)
EndingPanel.java         ████████                               259 baris   (3.9%)
GameState.java           ██████                                 214 baris   (3.2%)
Beast.java               █████                                  152 baris   (2.3%)
GachaSystem.java         ████                                   142 baris   (2.1%)
MainFrame.java           ████                                   135 baris   (2.0%)
BeastData.java           ███                                    113 baris   (1.7%)
ResourceManager.java     ███                                    112 baris   (1.7%)
HPBar.java               ██                                      72 baris   (1.1%)
GameMap.java             ██                                      57 baris   (0.9%)
ElementColor.java        █                                       46 baris   (0.7%)
MapData.java             █                                       32 baris   (0.5%)
Main.java                                                        18 baris   (0.3%)
```

---

## 🔍 Penjelasan Detail Tiap File

---

### `Main.java` — 18 baris

**Fungsi:** Entry point aplikasi. Menginisialisasi Look & Feel sistem operasi, lalu meluncurkan `MainFrame` di Swing Event Dispatch Thread (EDT) menggunakan `SwingUtilities.invokeLater()` untuk thread-safety.

**Bagian penting:**
```java
UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
```

---

## 📦 Package `audio`

---

### `SoundManager.java` — 503 baris

**Fungsi:** Engine audio lengkap yang menghasilkan semua suara secara **procedural PCM synthesis** — tanpa satu pun file audio eksternal (.wav/.mp3). Semua bunyi dibuat dari gelombang matematika sinusoidal dan noise.

**Pola Desain:** Singleton (`getInstance()`).

**Bagian penting yang dijelaskan:**

**1. Deteksi ketersediaan audio device**
Pada konstruktor, sistem mencoba membuat `Clip` kecil berisi silence. Jika berhasil, audio siap dipakai. Jika tidak (misalnya server headless), semua operasi audio menjadi *no-op* tanpa crash.

**2. Preloading SFX di background thread**
Saat startup, semua SFX di-generate dan di-cache di `ConcurrentHashMap<String, byte[]>` oleh thread daemon bernama `SFX-Preload` sehingga tidak ada jeda saat efek suara pertama kali diputar.

**3. BGM Tracks** — dikompilasi dari gelombang matematis:
| Track | Teknik Synthesis |
|---|---|
| `MENU` | Melodi 8 not, gelombang sinus + harmonik, envelope attack/release |
| `BATTLE` | Gelombang sawtooth bass, pulse wave, kick drum simulasi |
| `VICTORY` | Akord 4 nada (523/659/784/1047 Hz) dengan decay eksponensial |
| `STORY` | Pad 4 nada dengan modulasi tremolo lambat (3 Hz) |

**4. SFX Catalog** (12 efek):
| Nama SFX | Digunakan pada |
|---|---|
| `ATTACK` | Serangan normal |
| `SKILL` | Penggunaan Skill (30 MP) |
| `ULTIMATE` | Penggunaan Ultimate (70 MP) |
| `HURT` | Beast terkena serangan enemy |
| `VICTORY_SFX` | Semua musuh dikalahkan |
| `DEFEAT` | Tim player kalah |
| `GACHA` | Pull gacha (whoosh + chime) |
| `FREEZE` | Efek Blizzard kristal |
| `CLICK` | Navigasi UI |
| `RUN` | Kabur dari pertarungan |
| `EGG` | Animasi telur pecah |
| `UNLOCK` | Beast baru terbuka |

**5. Konversi volume (linear → dB)**
Menggunakan rumus matematis benar: `dB = 20 × log₁₀(volume)`, bukan skala linear, untuk mengontrol `FloatControl.Type.MASTER_GAIN` pada `Clip`.

**6. Strategi fallback 3 lapis untuk `openClip()`:**
- Strategi 1: `AudioSystem.getClip()` langsung (paling kompatibel Windows)
- Strategi 2: `DataLine.Info` eksplisit (fallback JVM lain)
- Strategi 3: Format stereo (duplikasi channel mono → stereo, untuk driver yang tidak support mono)

---

## 📦 Package `controller`

---

### `BattleController.java` — 342 baris

**Fungsi:** Engine turn-based pertarungan bergaya HSR (Honkai: Star Rail). Mengatur urutan giliran seluruh Beast berdasarkan stat **Speed** menggunakan **Action Value System**.

**Bagian penting yang dijelaskan:**

**1. Sistem Action Value**
Setiap Beast memiliki `actionValue` yang bertambah sebesar `speed`-nya setiap "tick". Beast pertama yang mencapai threshold `ACTION_POINT = 10.000` mendapat giliran. Beast dengan speed lebih tinggi mendapat giliran lebih sering secara proporsional.

```java
// Simulasikan tick sampai ada yang siap
while (true) {
    for (TurnEntry e : turnQueue) e.actionValue += e.beast.getSpeed();
    if (turnQueue.stream().anyMatch(e -> e.actionValue >= ACTION_POINT)) break;
}
```

**2. Antrian Turn (`List<TurnEntry>`)**
Setiap entry menyimpan: `teamIndex`, `isEnemy` (boolean), referensi `Beast`, dan `actionValue` saat ini. Setelah giliran berakhir, `actionValue` dikurangi `ACTION_POINT` (bukan di-reset ke 0) sehingga sistem proporsional terjaga.

**3. Aksi Player:**
| Method | Biaya MP | Formula Damage |
|---|---|---|
| `performAttack(targetIdx)` | 0 | `ATK − DEF/2` × elemen |
| `performSkill(targetIdx)` | 30 MP | `(ATK × 1.5) − DEF/2` × elemen |
| `performUltimate(targetIdx)` | 70 MP | `(ATK × 2) − DEF/3` × elemen |
| `performRun()` | 0 | 50% berhasil, giliran tidak berpindah jika gagal |

**4. Aksi Enemy (otomatis)**
Enemy memilih target player secara acak (yang masih hidup). Dengan probabilitas 20%, enemy menggunakan Skill jika mana cukup; sisanya serangan biasa.

**5. `BattleResult` DTO**
Setiap aksi mengembalikan objek `BattleResult` yang berisi:
- `log` — teks deskripsi aksi
- `playerFainted` — apakah ada player yang baru mati
- `allEnemyDefeated` — apakah semua musuh kalah
- `allPlayerDefeated` — apakah semua player kalah
- `damageDealt`, `targetIsEnemy`, `targetDied` — data untuk efek visual

**6. Regenerasi Mana per Giliran**
Setelah setiap aksi, Beast pulih 8 MP (player) atau 10 MP (enemy) secara otomatis.

---

### `GameState.java` — 214 baris

**Fungsi:** **Singleton** penyimpan seluruh state global game yang perlu diakses lintas layar: tim player, tim enemy, map yang dipilih, level saat ini, dan info user yang login.

**Bagian penting yang dijelaskan:**

**1. Dual-mode: Online vs Offline**
- **Online** (MySQL tersedia): data disimpan ke dan dibaca dari database
- **Offline** (tanpa MySQL): data disimpan di memori (`offlineEggs`, `offlineOwnedIds`) hanya untuk sesi berjalan

**2. Cache Beast yang Dimiliki**
`cachedOwnedBeastIds` menyimpan daftar ID Beast milik user agar tidak query DB berulang. Cache di-invalidate oleh `invalidateBeastCache()` setelah gacha berhasil unlock Beast baru.

**3. `loadProgressFromDB()`**
Mereset seluruh map ke kondisi awal lalu memuat ulang dari DB — mencegah data lama tersisa di memori saat berganti akun.

**4. Deteksi kondisi battle**
- `isPlayerDefeated()` — true jika semua Beast player HP = 0
- `isEnemyDefeated()` — true jika semua Beast enemy HP = 0
- `getActiveBeast()` — mencari Beast player pertama yang masih hidup

---

## 📦 Package `data`

---

### `BeastData.java` — 113 baris

**Fungsi:** Katalog statis 24 Beast beserta stat dasarnya. Menyediakan method untuk clone Beast (agar tidak berbagi referensi), mencari Beast by ID, dan menghasilkan tim enemy acak berdasarkan elemen map dan level.

**Bagian penting yang dijelaskan:**

**1. Mapping ID Beast**
```
Api    : 1=Blazefang,    2=Cinderion,   3=Ignarox,    4=Pyroth
Air    : 5=Aquarion,     6=Marivex,     7=Nerevion,   8=Tsunadra
Tanah  : 9=Bedrock Titan,10=Gravok,     11=Quakron,   12=Terragorn
Daun   : 13=Floravine,   14=Luminaire,  15=Mossdrake, 16=Rootzilla
Cahaya : 17=Aetherion,   18=Luxeron,    19=Radiantor,  20=Solareth
Gelap  : 21=Morvexis,    22=Noctyra,    23=Shadowfang, 24=Umbrax
```

**2. Stat Beast** (format: HP, Mana, ATK, DEF, SPD)
- Beast elemen biasa: HP 200–280, ATK 50–75, DEF 20–55, SPD 35–70
- Beast langka (Cahaya/Gelap): semua stat rata-rata lebih tinggi

**3. Generasi Tim Enemy (`getEnemyTeam`)**
- 70% musuh dari elemen map, 30% elemen acak (variasi)
- Jumlah musuh bertambah sesuai level: Level 1→2 musuh, Level 2→3, Level 3→4, Level 4+→5
- Stat enemy diskala: HP×0.85, ATK×0.80, DEF×0.80, SPD+random agar lebih lemah dari Beast player penuh

**4. Beast Starter**
ID 1, 3, 5, 9, 13 (Blazefang, Ignarox, Aquarion, Bedrock Titan, Floravine) — diberikan gratis saat registrasi di mode offline.

---

### `MapData.java` — 32 baris

**Fungsi:** Daftar 6 map dengan konfigurasi elemen dominan, jumlah level, dan status unlock awal. Map pertama (Plains) selalu terbuka; map berikutnya terbuka setelah map sebelumnya diselesaikan penuh.

| Index | Nama | Elemen Musuh | Max Level | Unlock Awal |
|---|---|---|---|---|
| 0 | Plains | Daun | 3 | ✅ Terbuka |
| 1 | Sea | Air | 3 | ❌ Terkunci |
| 2 | Dessert | Tanah | 3 | ❌ Terkunci |
| 3 | Blizzard | Cahaya | 3 | ❌ Terkunci |
| 4 | Volcano | Api | 3 | ❌ Terkunci |
| 5 | Dark Forest | Gelap | 4 | ❌ Terkunci |

---

## 📦 Package `database`

---

### `DatabaseManager.java` — 336 baris

**Fungsi:** Mengelola seluruh interaksi dengan database MySQL (XAMPP). Menggunakan koneksi JDBC dan membuat tabel secara otomatis saat pertama kali dijalankan.

**Pola Desain:** Singleton (`getInstance()`).

**Bagian penting yang dijelaskan:**

**1. Konfigurasi Koneksi**
```java
"jdbc:mysql://localhost:3306/beastclash?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
User: root | Password: (kosong, default XAMPP)
```

**2. Auto-create Tabel**
Tiga tabel dibuat otomatis dengan `CREATE TABLE IF NOT EXISTS`:

```sql
-- Menyimpan akun user
users: id, username, password (SHA-256), eggs, created_at

-- Progress per map per user
map_progress: user_id, map_index, completed_levels, unlocked
  UNIQUE KEY (user_id, map_index)

-- Beast yang dimiliki user
beast_owned: user_id, beast_id
  UNIQUE KEY (user_id, beast_id)
```

**3. Keamanan Password**
Password di-hash menggunakan SHA-256 sebelum disimpan — tidak pernah disimpan sebagai plain text.

**4. Beast Starter saat Registrasi**
Saat user baru register, 8 Beast langsung di-insert: `{1, 2, 3, 5, 6, 9, 13, 17}` (Blazefang, Cinderion, Ignarox, Aquarion, Marivex, Bedrock Titan, Floravine, Aetherion).

**5. `spendEgg()` — Atomic Transaction**
```sql
UPDATE users SET eggs = eggs - 1 WHERE id = ? AND eggs > 0
```
Kondisi `AND eggs > 0` memastikan telur tidak bisa negatif meski ada race condition.

**6. `saveMapProgress()` — Upsert**
Menggunakan `ON DUPLICATE KEY UPDATE` agar bisa insert sekaligus update dalam satu query.

**7. `ownsAllBeasts(userId)`**
Cek apakah user memiliki semua 24 Beast — kondisi untuk menampilkan ending game.

---

## 📦 Package `gacha`

---

### `GachaSystem.java` — 142 baris

**Fungsi:** Mengimplementasikan sistem gacha berbobot dengan pity system. Mendukung mode online (data ke DB) dan offline (data di memori).

**Bagian penting yang dijelaskan:**

**1. Tabel Bobot**
```
Beast BARU  – Elemen biasa (Api/Air/Tanah/Daun) : bobot 4 (sering)
Beast BARU  – Elemen langka (Cahaya/Gelap)        : bobot 2 (jarang)
Beast DUPLIKAT (semua elemen)                     : bobot 1 (sangat jarang)
```

**2. Sistem Pity (pull ke-10)**
Jika sudah 10 kali pull tanpa mendapat Beast Cahaya/Gelap baru, pull berikutnya **dijamin** Beast langka baru (jika masih ada yang belum dimiliki). Counter pity di-reset ke 0 setelah mendapat Beast langka.

**3. Alur `pull()`**
```
1. Cek & kurangi telur (DB atau memori)
2. Naikkan pityCount
3. Pisahkan pool: newPool (belum milik) vs dupPool (sudah milik)
4. Jika pityCount ≥ 10 → paksa Cahaya/Gelap baru
5. Jika tidak → rollWeighted() berdasarkan bobot
6. Simpan Beast baru ke DB (online) atau memori (offline)
7. Putar SFX "GACHA"
8. Return PullResult(beast, isDuplicate)
```

**4. `rollWeighted()`**
Mengakumulasi bobot kumulatif, lalu memilih Beast berdasarkan angka random dalam range total bobot — implementasi weighted random yang benar.

---

## 📦 Package `model`

---

### `Beast.java` — 152 baris

**Fungsi:** Entity utama game. Merepresentasikan satu Beast dengan seluruh stat dan logika combat-nya.

**Atribut:**
| Stat | Deskripsi |
|---|---|
| `id` | ID unik (1–24) untuk referensi ke DB dan aset |
| `name` | Nama Beast |
| `element` | Elemen (Api/Air/Tanah/Daun/Cahaya/Gelap) |
| `maxHP / currentHP` | HP maksimum dan saat ini |
| `maxMana / currentMana` | Mana untuk Skill/Ultimate |
| `attack` | Stat serangan (bisa dimodifikasi debuff map) |
| `defense` | Stat pertahanan |
| `speed` | Menentukan frekuensi giliran |
| `baseAttack/Defense/Speed` | Nilai asli sebelum modifikasi map |

**Bagian penting yang dijelaskan:**

**1. Sistem Kelemahan Elemen (`getElementMultiplier`)**
```
Api   → Daun : ×1.5 (kuat)   |  Api   → Air  : ×0.5 (lemah)
Air   → Api  : ×1.5           |  Air   → Tanah: ×0.5
Tanah → Air  : ×1.5           |  Tanah → Daun : ×0.5
Daun  → Tanah: ×1.5           |  Daun  → Api  : ×0.5
Cahaya → Gelap: ×1.5          |  Gelap → Cahaya: ×1.5
```

**2. Formula Damage (setelah multiplier elemen)**
```java
Attack  : Math.max(1, ATK - DEF/2) × elementMultiplier
Skill   : Math.max(1, (ATK×1.5) - DEF/2) × elementMultiplier
Ultimate: Math.max(1, ATK×2 - DEF/3) × elementMultiplier
```
`Math.max(1, ...)` memastikan damage selalu minimal 1.

**3. Modifikasi Stat Map**
`multiplyAttack()`, `multiplyDefense()`, `multiplySpeed()` mengubah stat saat ini (bukan base). Method `reset()` mengembalikan stat ke `baseAttack/Defense/Speed` yang tersimpan sejak konstruktor.

---

### `GameMap.java` — 57 baris

**Fungsi:** Entity yang merepresentasikan satu map/dunia. Menyimpan nama, elemen musuh dominan, progress level yang sudah diselesaikan, dan status terkunci/terbuka.

**Method kunci:**
- `completeLevel()` — tandai satu level selesai (tidak melebihi `maxLevels`)
- `isFullyCompleted()` — `completedLevels >= maxLevels`, digunakan untuk membuka map berikutnya
- `setCompletedLevels(v)` — setter aman dengan clamp 0–maxLevels

---

## 📦 Package `resources`

---

### `ResourceManager.java` — 112 baris

**Fungsi:** Memuat dan menyimpan cache semua aset gambar (Beast dan Map) dari classpath. Menghindari load ulang gambar yang sama berulang kali menggunakan `HashMap<String, BufferedImage>`.

**Pola Desain:** Singleton dengan lazy initialization.

**Bagian penting yang dijelaskan:**

**1. Gambar Beast: Asli vs Flipped**
Semua sprite Beast menghadap ke kanan. Untuk membedakan posisi:
- **Player (kiri layar)** → gambar asli `getBeastForPlayer(name)`
- **Enemy (kanan layar)** → gambar di-flip horizontal `getBeastForEnemy(name)`

Flip dilakukan dengan `AffineTransform` untuk membalik sumbu X:
```java
AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
tx.translate(-width, 0);
```

**2. Mapping Nama Map → File**
```
"Plains"      → Plains.png
"Dessert"     → Dessert.png
"Sea"         → Sea.png
"Blizzard"    → Blizzard.png
"Volcano"     → Volcano.png
"Dark Forest" → DarkForest.png
```

**3. Cache Key Convention**
- `"map:Plains"` → background map
- `"beast:Blazefang"` → sprite asli
- `"beast_flip:Blazefang"` → sprite flipped

---

## 📦 Package `view`

---

### `MainFrame.java` — 135 baris

**Fungsi:** Window utama aplikasi. Mengatur navigasi antar-layar menggunakan `CardLayout`, menentukan ukuran window yang berbeda tiap layar, dan membersihkan resource layar lama sebelum berpindah.

**Konstanta layar:**
| Konstanta | Ukuran Window | Panel |
|---|---|---|
| `SCREEN_LOGIN` | 560×560 | LoginPanel |
| `SCREEN_STORY` | 680×520 | StoryIntroPanel |
| `SCREEN_MENU` | 560×600 | MainMenuPanel |
| `SCREEN_MAP` | 560×640 | MapSelectPanel |
| `SCREEN_BEAST` | 760×640 | BeastSelectPanel |
| `SCREEN_BATTLE` | 920×640 | BattlePanel |
| `SCREEN_GACHA` | 580×580 | GachaPanel |
| `SCREEN_ENDING` | 720×580 | EndingPanel |

**Bagian penting — `switchTo()` (7 langkah):**
1. Cleanup resource panel lama (stop timer, dll) via interface `Cleanable`
2. Hapus semua komponen lama dari container
3. Set preferred size panel baru
4. Tambahkan panel baru ke `CardLayout`
5. Paksa `CardLayout` menampilkan panel baru
6. Resize window dengan memperhitungkan insets (title bar + border)
7. Re-center window di tengah layar

**Interface `Cleanable`:** Panel yang menggunakan `javax.swing.Timer` wajib mengimplementasikan `cleanup()` agar timer tidak berjalan di background setelah layar diganti (mencegah *timer zombie*).

---

### `LoginPanel.java` — 448 baris

**Fungsi:** Layar pertama yang muncul. Menangani registrasi, login, pengecekan koneksi database, dan mode offline.

**Bagian penting yang dijelaskan:**

**1. Animasi Background**
Timer 33ms menganimasikan bintang berkedip (`starPhase`) dan orb berputar (`orbAngle`) yang digambar di `paintComponent` — memberikan tampilan dinamis tanpa file eksternal.

**2. Deteksi Koneksi Database**
`checkDBConnection()` mencoba `DatabaseManager.connect()`. Jika berhasil, status "Database: Terhubung" ditampilkan hijau. Jika gagal, muncul tombol **"Main Offline"** yang langsung masuk ke game tanpa akun.

**3. Animasi Shake saat Login Gagal**
`shake()` menggunakan `Timer` 40ms yang memindahkan posisi card (panel login) ke kiri-kanan sebanyak 8 kali, mensimulasikan getaran.

**4. Alur Login Sukses**
```
Validasi input → login(username, password) → setCurrentUserId(uid)
→ loadProgressFromDB() → Timer 800ms → showStory()
```

---

### `StoryIntroPanel.java` — 599 baris

**Fungsi:** Menampilkan prolog 6 halaman dengan animasi partikel dan teks typewriter sebelum game dimulai.

**6 Halaman Cerita:**
| Halaman | Judul | Konten |
|---|---|---|
| 0 | Arcana yang Tenang | Latar dunia harmonis, animasi langit berbintang |
| 1 | Malam Naas | Meteor jatuh, Beast terkontaminasi |
| 2 | Zenith Bangkit | Siluet senjata purba, pulsasi cahaya |
| 3 | Kristal Zenith | Kristal merusak dunia, animasi retakan |
| 4 | Misi The Wardens | Tugas player, animasi misi |
| 5 | Judul | Title card "Beast Clash", transisi ke menu |

**Bagian penting yang dijelaskan:**

**1. Efek Typewriter**
`typeTimer` (interval 22ms) menambahkan satu karakter per tick ke `JTextArea`, menciptakan efek teks muncul perlahan. Klik pada area teks langsung menampilkan teks penuh (skip typewriter).

**2. Sistem Animasi per Scene**
`animTimer` (30ms) memanggil metode draw yang berbeda sesuai `pageIndex`:
- `drawStarField()` — bintang berkedip dengan brightness acak
- `drawMeteorScene()` — meteor jatuh miring dengan ekor cahaya dan partikel
- `drawZenithScene()` — siluet gelap berpulsasi dengan aura ungu
- `drawCrystalScene()` — kristal ungu tumbuh dengan animasi crack
- `drawMissionScene()` — teks misi dengan latar bergradasi

---

### `MainMenuPanel.java` — 267 baris

**Fungsi:** Menu utama dengan tombol navigasi utama dan animasi awan bergerak.

**Tombol navigasi:**
- **Mulai Bertarung** → `showMapSelect()`
- **Lihat Beast** → `showBeastSelect()`
- **Gacha** → `showGacha()`
- **Credit** → dialog popup nama tim
- **Pengaturan Audio** → dialog toggle BGM/SFX + slider volume
- **Keluar** → `System.exit(0)`

**Animasi:** `animTimer` (40ms) menggerakkan `cloudOffset` secara horizontal, menggambar awan berbentuk oval berlapis yang berputar dari kanan ke kiri.

**Implements `Cleanable`:** `cleanup()` menghentikan `animTimer` agar tidak berjalan di latar belakang.

---

### `MapSelectPanel.java` — 313 baris

**Fungsi:** Menampilkan 6 kartu map yang bisa dipilih. Map terkunci ditampilkan dengan overlay abu-abu; map terbuka bisa diklik untuk memilih level.

**Bagian penting yang dijelaskan:**

**1. Kartu Map (`buildMapCard`)**
Setiap kartu berisi:
- Thumbnail background map (scaled)
- Badge elemen musuh dengan warna dari `ElementColor`
- Progress bar level (X/max)
- Status label: "Terbuka", "Selesai", atau "Terkunci"
- Tombol "Pilih Level 1/2/3" (jika terbuka)

**2. Logika Unlock Map**
Saat player menyelesaikan semua level di suatu map, map berikutnya di-unlock dan progress disimpan ke DB via `GameState.saveMapProgressToDB()`.

**3. Alur Memilih Map + Level**
```
Klik tombol level → state.setSelectedMap(map)
→ state.setCurrentLevel(level) → generateEnemyTeam()
→ state.setEnemyTeam(enemies) → frame.showBeastSelect()
```

---

### `BeastSelectPanel.java` — 586 baris

**Fungsi:** Layar pemilihan tim — menampilkan semua 24 Beast dalam grid, memungkinkan player memilih maksimal 5 Beast untuk dibawa ke battle.

**Bagian penting yang dijelaskan:**

**1. Grid Kartu Beast**
- Beast yang dimiliki: kartu berwarna dengan gambar, nama, elemen, dan badge stat
- Beast yang belum dimiliki: kartu abu-abu dengan overlay "TERKUNCI" dan gambar grayscale
- Filter elemen di bagian atas untuk menyaring tampilan

**2. `toGrayscale()`**
Menggunakan `ColorConvertOp` dengan `ColorSpace.CS_GRAY` untuk mengubah sprite Beast berwarna menjadi abu-abu bagi Beast yang terkunci.

**3. Popup Detail (Hover)**
`showDetailPopup()` membuat `JWindow` transparan yang muncul di dekat kartu saat mouse hover, menampilkan stat lengkap Beast (HP, ATK, DEF, SPD, Elemen) dengan latar hitam semi-transparan.

**4. Preview Tim**
Panel bawah menampilkan 5 slot tim. Saat Beast dipilih, slotnya terisi dengan gambar kecil Beast. Klik Beast yang sudah dipilih untuk menghapusnya dari tim.

**5. `onBegin()`**
Validasi minimal 1 Beast dipilih, lalu memanggil `state.setPlayerTeam(selectedBeasts)` diikuti `frame.showBattle()`.

**6. Label Koleksi**
`collectionLabel` menampilkan "X / 24 Beast" — jumlah Beast yang sudah dimiliki dari total 24.

---

### `BattlePanel.java` — 1.381 baris *(file terbesar)*

**Fungsi:** Arena battle lengkap dengan efek visual, animasi, debuff map, dan semua mekanisme pertarungan.

**Inner Class penting:**

#### `ActionOrderBar` (inner class, ~120 baris)
Panel strip horizontal di bagian atas yang menampilkan urutan giliran semua Beast. Setiap slot menampilkan thumbnail sprite Beast yang di-scale kecil, dengan outline berwarna elemen dan indikator giliran saat ini (lebih besar/terang).

#### `BattleArena` (inner class, ~450 baris)
Canvas utama pertarungan yang menggambar sprite Beast berskala besar, HP bar overlay, dan semua efek visual:

| Efek | Method | Mekanisme |
|---|---|---|
| **Hit Effect** | `triggerHitEffect()` | Angka damage melayang ke atas selama 800ms, warna emas untuk Skill/Ultimate |
| **Flash Effect** | *(di dalam `triggerHitEffect`)* | Sprite Beast berpendar merah/oranye sesuai bentuk gambar (shape-aware) menggunakan alpha compositing |
| **Death Effect** | `triggerDeathEffect()` | Kristal ungu meledak dengan 12 partikel yang menyebar, disertai SFX "HURT" |

**Bagian penting lainnya:**

**1. `applyMapDebuffs()` & `startMapEffects()`**
Dipanggil saat konstruktor. Menerapkan modifier stat ke Beast player berdasarkan map yang dipilih:

| Map | Efek Elemen |
|---|---|
| Plains | Api ATK −20% |
| Sea | Api ATK/DEF berkurang, Daun SPD −20% |
| Dessert | −3 HP/3 dtk (imun Api & Tanah), Air DEF −25%, Daun ATK −15% |
| Blizzard | Api ATK/DEF berkurang, Cahaya DEF +15%, Gelap DEF −20% |
| Volcano | −5 HP/3 dtk (imun Api), Air ATK/DEF berkurang, Api ATK +15% |
| Dark Forest | Cahaya ATK/DEF berkurang, Gelap ATK/DEF meningkat, semua elemen DEF −10% |

**2. Periodic Damage Timer**
Di Dessert dan Volcano, `mapDamageTimer` (`javax.swing.Timer`) berjalan setiap 3.000ms dan mengurangi HP semua Beast yang tidak imun sebesar nilai yang ditentukan.

**3. `onPlayerAction()`**
Router aksi player: memanggil method yang sesuai di `BattleController`, lalu mengproses `BattleResult`:
- Trigger efek visual
- Cek kondisi menang/kalah
- Jadwalkan `scheduleNextTurn()` untuk giliran berikutnya

**4. `scheduleNextTurn()`**
Menggunakan `Timer` 900ms delay sebelum giliran enemy berjalan, agar transisi terasa alami dan tidak langsung.

**5. Floating Battle Log**
`JWindow` terpisah yang dapat di-toggle, ditampilkan di luar batas panel utama, berisi `JTextArea` dengan riwayat seluruh aksi battle.

**6. `cleanup()` (Cleanable)**
Menghentikan `mapDamageTimer` dan `freezeTimer` agar tidak ada efek damage yang terus berjalan setelah battle selesai.

---

### `GachaPanel.java` — 476 baris

**Fungsi:** Layar gacha dengan animasi telur berputar, efek partikel, dan kartu reveal Beast yang didapat.

**Bagian penting yang dijelaskan:**

**1. Animasi Telur (`drawEggAnimation`)**
`animTimer` (30ms) menggambar telur yang berdenyut (scale ±5%) dengan `eggPhase`, dikelilingi partikel melingkar berputar (`particleAng`). Telur berubah warna dari kuning tua → gradasi akord merah-kuning.

**2. Alur `doPull()`**
```
Disable tombol → GachaSystem.pull() → Timer 1.500ms delay
→ showReveal = true → drawReveal() → refresh egg count
→ Cek ownsAllBeasts() → jika ya: Timer 2.000ms → showEnding()
```

**3. `drawReveal()` — Kartu Beast**
Menampilkan kartu dengan:
- Background gradasi sesuai elemen Beast (warna dari `ElementColor`)
- Sprite Beast berukuran besar dari `ResourceManager`
- Nama Beast, elemen, dan label "BARU!" atau "DUPLIKAT"
- Efek bintang berputar (Beast langka) atau latar statis (Beast biasa)

**4. Implements `Cleanable`:** `cleanup()` menghentikan `animTimer`.

---

### `EndingPanel.java` — 259 baris

**Fungsi:** Layar kemenangan akhir setelah semua 24 Beast dikumpulkan. Menampilkan animasi langit fajar dengan percikan bintang dan teks epilog yang muncul bertahap.

**Bagian penting yang dijelaskan:**

**1. Teks Epilog Bertahap**
`lineTimer` (800ms per baris) mengungkap teks satu baris per giliran, dengan efek typewriter per karakter di dalam baris tersebut. Klik untuk langsung menampilkan semua teks.

**2. Animasi Langit Fajar**
- Gradasi warna dari biru malam → emas pagi menggunakan `GradientPaint`
- Orb matahari terbit berpulsasi dengan aura berlapis
- 30 partikel percikan bintang yang terbang ke atas lalu memudar

**3. Tombol Kembali**
Setelah beberapa detik atau klik, pemain dapat kembali ke Main Menu dengan BGM beralih ke "MENU".

---

### `HPBar.java` — 72 baris

**Fungsi:** Komponen UI custom (`JPanel`) yang menampilkan bar HP atau Mana dengan label teks dan warna dinamis.

**Bagian penting:** Saat HP berada di bawah 30% kapasitas, bar yang semula berwarna hijau otomatis berubah menjadi merah (`new Color(200, 60, 60)`) sebagai peringatan visual kritis. Menggunakan `fillRoundRect` untuk tampilan yang lebih modern.

---

### `ElementColor.java` — 46 baris

**Fungsi:** Utility class statis yang memetakan nama elemen ke warna `java.awt.Color` dan label teks. Digunakan secara konsisten di seluruh UI agar warna elemen seragam.

| Elemen | Warna RGB |
|---|---|
| Api | `(220, 80, 30)` — merah-oranye |
| Air | `(30, 120, 220)` — biru |
| Tanah | `(139, 90, 43)` — coklat |
| Daun | `(34, 139, 34)` — hijau |
| Cahaya | `(255, 215, 0)` — emas |
| Gelap | `(75, 0, 130)` — ungu tua |

---

## 🔄 Alur Navigasi Antar Layar

```
Main.java
    └── MainFrame
            │
            ├── LoginPanel ──────────────────────────────────────► StoryIntroPanel
            │   (login / register / offline)                             │
            │                                                            ▼
            │                                                       MainMenuPanel
            │                                                    ┌──────┴──────┐
            │                                              [Mulai]           [Gacha]
            │                                                │                  │
            │                                          MapSelectPanel      GachaPanel
            │                                                │                  │
            │                                          BeastSelectPanel    (kembali ke menu)
            │                                                │
            │                                          BattlePanel
            │                                      ┌────────┴────────┐
            │                                   [Menang]          [Kalah]
            │                                      │                  │
            │                               MapSelectPanel      MapSelectPanel
            │                            (jika semua map selesai)
            │                                      │
            │                                 EndingPanel
            │                            (setelah 24 Beast terkumpul)
            └──────────────────────────────────────────────────────
```

---

## 🧵 Arsitektur Thread

| Thread | Nama | Tujuan |
|---|---|---|
| Swing EDT | `main` | Semua operasi UI (Swing tidak thread-safe) |
| `BGM-<track>` | daemon | Membangun dan memutar BGM di background |
| `SFX-<name>` | daemon | Memutar SFX tanpa memblokir UI |
| `SFX-Preload` | daemon | Pre-generate semua SFX saat startup |
| `javax.swing.Timer` | EDT | Animasi, map damage, enemy turn delay — aman untuk UI |

---

## 🎮 Dependency Antar Komponen

```
Main
  └── MainFrame (navigasi)
        ├── LoginPanel
        │     └── DatabaseManager (auth)
        ├── BeastSelectPanel
        │     ├── GameState (available beasts)
        │     ├── BeastData (katalog)
        │     └── ResourceManager (sprite)
        ├── BattlePanel
        │     ├── BattleController (engine)
        │     │     ├── GameState (teams, status)
        │     │     └── SoundManager (sfx)
        │     ├── ResourceManager (sprite, map bg)
        │     └── ElementColor (warna)
        ├── GachaPanel
        │     ├── GachaSystem (logika pull)
        │     │     ├── DatabaseManager (eggs, beast_owned)
        │     │     ├── GameState (offline mode)
        │     │     └── BeastData (pool)
        │     └── ResourceManager (sprite reveal)
        └── MapSelectPanel
              └── GameState (map progress)
```

---

## 📝 Catatan Teknis Penting

**Mengapa tidak ada file audio eksternal?**
Semua suara di-generate secara procedural menggunakan PCM synthesis (gelombang sinus, noise, envelope). Pendekatan ini menghilangkan ketergantungan pada file `.wav`/`.mp3` dan mengurangi ukuran distribusi secara signifikan.

**Mengapa semua gambar menghadap kanan?**
Konvensi sprite: semua Beast menghadap kanan. Saat ditampilkan sebagai enemy, `ResourceManager` membalik gambar secara horizontal menggunakan `AffineTransform` — tidak perlu aset terpisah per sisi.

**Mengapa `GameState` menggunakan Singleton?**
State game perlu diakses dari berbagai layar yang berbeda (`BattlePanel`, `MapSelectPanel`, `BeastSelectPanel`, dll.) tanpa passing parameter yang panjang. Singleton memberikan titik akses terpusat yang konsisten.

**Mode Offline**
Jika MySQL tidak tersedia, game tetap bisa dimainkan penuh. Progress tidak tersimpan antar sesi, tetapi Beast yang didapat lewat gacha tersedia selama sesi berjalan (disimpan di `offlineOwnedIds` dalam memori).

---

*Dokumen ini dibuat berdasarkan analisis seluruh kode sumber Beast Clash — 22 file Java, 6.603 baris kode, oleh Tim Beast Clash 2026.*
