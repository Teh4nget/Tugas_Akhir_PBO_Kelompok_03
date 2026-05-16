# 🐉 Beast Clash — Dokumentasi Lengkap Proyek

> *"Di dunia Arcana, kekuatan sejati bukan dari seberapa keras kamu menyerang — melainkan dari seberapa jauh kamu bersedia berjalan demi melindungi yang kamu sayangi."*

---

## 📋 Daftar Isi

1. [Tentang Game](#tentang-game)
2. [Tim Pengembang](#tim-pengembang)
3. [Persyaratan Sistem](#persyaratan-sistem)
4. [Cara Setup](#cara-setup)
5. [Struktur Proyek](#struktur-proyek)
6. [Ringkasan File & Statistik Kode](#ringkasan-file--statistik-kode)
7. [Penjelasan Detail Setiap File](#penjelasan-detail-setiap-file)
8. [Arsitektur & Alur Sistem](#arsitektur--alur-sistem)
9. [Lore & Cerita Beast Clash](#lore--cerita-beast-clash)
10. [Sistem Ending](#sistem-ending)
11. [Slogan](#slogan)

---

## Tentang Game

**Beast Clash** adalah game RPG turn-based berbasis Java Swing yang terinspirasi dari sistem pertarungan bergaya *Honkai: Star Rail (HSR)*. Pemain berperan sebagai seorang **Pelatih Beast** yang bertugas melindungi dunia **Arcana** dari ancaman kehancuran oleh entitas bernama **Zenith**.

Game ini dibangun sepenuhnya dengan **Java (Swing)** dan menggunakan **MySQL (XAMPP)** sebagai database untuk sistem autentikasi, progres permainan, dan kepemilikan Beast. Seluruh audio di-*generate* secara prosedural tanpa file eksternal.

**Fitur Utama:**
- Sistem battle turn-based berbasis kecepatan (HSR-style action queue)
- 24 Beast unik dengan 6 elemen yang saling berinteraksi
- 6 map dunia dengan efek lingkungan yang memengaruhi battle
- Sistem Gacha dengan pity ke-10 dan rarity Cahaya/Gelap
- Autentikasi pengguna (register/login) dengan hashing SHA-256
- Mode offline (tanpa database) dengan progress in-memory
- Musik & SFX prosedural (PCM synthesis, tanpa file .wav/.mp3)
- Animasi cerita pembuka (prolog 6 halaman) dan layar ending

---

## Tim Pengembang

| Peran | Nama |
|---|---|
| 🎮 Game Designer & Lead Developer | **Agung Wahyu Niti Wijaya** |
| 💻 Backend & Database Engineer | **Nova Salwa Safitri & Septi Lailatul Fitria** |
| 🎨 UI/UX & View Developer | **Ahmad Dziqro Attayu Setio Damar** |
| 🔊 Audio & Visual Effects | **Raga Deva Bela Negara** |

> Proyek ini dikembangkan sebagai karya akademis di lingkungan pengembangan NetBeans IDE dengan target JDK 11+ dan MySQL 8.x melalui XAMPP.

---

## Persyaratan Sistem

| Komponen | Versi |
|---|---|
| **NetBeans IDE** | 21 ke atas (termasuk 29) |
| **JDK** | 11 – 26 (dikompilasi dengan `--release 11`) |
| **XAMPP** | 8.x (MySQL 8.x) |
| **MySQL Connector/J** | 9.x (letakkan di folder `lib/`) |

---

## Cara Setup

### 1. Clone Repository
```
git clone https://github.com/username/BeastClash.git](https://github.com/Teh4nget/Tugas_Akhir_PBO_Kelompok_03.git
cd BeastClash
```

### 1. Siapkan Driver MySQL

```
lib/mysql-connector-j-9.x.x.jar
```

Sudah ada di folder lib, Jika belum ada bisa download dari https://dev.mysql.com/downloads/connector/j/ lalu letakkan di folder `lib/`. 

### 2. Siapkan Database

1. Jalankan **XAMPP** → Start **Apache** & **MySQL**
2. Buka **phpMyAdmin** → Buat database baru bernama **`beastclash`** (kosong)
3. Tabel dibuat otomatis oleh program saat pertama dijalankan:
   - `users` — data akun pengguna
   - `map_progress` — progres tiap map per user
   - `beast_owned` — daftar Beast yang dimiliki per user

### 3. Buka di NetBeans

```
File → Open Project → pilih folder BeastClash
Klik kanan project → Properties → Libraries
Pastikan lib/mysql-connector-j-9.x.x.jar terdaftar
Tekan F6 untuk menjalankan
```

### 4. Mode Offline (Tanpa Database)

Jika MySQL tidak aktif, game otomatis masuk **mode offline**: Beast starter tersedia, gacha menggunakan telur in-memory, dan progres tidak disimpan antar sesi.

---

## Struktur Proyek

```
BeastClash/
├── src/
│   └── beastclash/
│       ├── Main.java                        ← Entry point aplikasi
│       ├── audio/
│       │   └── SoundManager.java            ← Manajer BGM & SFX prosedural
│       ├── controller/
│       │   ├── BattleController.java        ← Engine pertarungan turn-based
│       │   └── GameState.java               ← Singleton state game global
│       ├── data/
│       │   ├── BeastData.java               ← Katalog 24 Beast + enemy generator
│       │   └── MapData.java                 ← Daftar 6 map dunia
│       ├── database/
│       │   └── DatabaseManager.java         ← Koneksi MySQL, CRUD user/beast/map
│       ├── gacha/
│       │   └── GachaSystem.java             ← Sistem gacha berbobot + pity
│       ├── model/
│       │   ├── Beast.java                   ← Entitas Beast (stats, damage, elemen)
│       │   └── GameMap.java                 ← Entitas map (nama, elemen, progres)
│       ├── resources/
│       │   └── ResourceManager.java         ← Load & cache gambar Beast/map
│       └── view/
│           ├── MainFrame.java               ← Window utama + navigasi layar
│           ├── LoginPanel.java              ← Layar login & register
│           ├── StoryIntroPanel.java         ← Prolog cerita animasi (6 halaman)
│           ├── MainMenuPanel.java           ← Menu utama game
│           ├── MapSelectPanel.java          ← Pilih map & level
│           ├── BeastSelectPanel.java        ← Pilih tim Beast (maks. 3)
│           ├── BattlePanel.java             ← Arena pertarungan utama
│           ├── GachaPanel.java              ← Sistem gacha Beast baru
│           ├── EndingPanel.java             ← Layar ending + animasi
│           ├── HPBar.java                   ← Komponen visual HP/MP bar
│           └── ElementColor.java            ← Utilitas warna per elemen
│
├── build/
│   └── classes/beastclash/
│       ├── resources/
│       │   ├── beast/      ← 24 gambar Beast (.png)
│       │   └── map/        ← 6 gambar map (.png)
│       └── [*.class]       ← Bytecode hasil kompilasi
│
├── dist/
│   ├── BeastClash.jar      ← JAR siap distribusi
│   └── lib/
│       └── mysql-connector-j-9.5.0.jar
│
├── lib/
│   └── mysql-connector-j-9.5.0.jar
│
├── nbproject/              ← Konfigurasi NetBeans
├── build.xml               ← Ant build script
├── manifest.mf             ← Manifest JAR
└── README.md               ← Panduan setup
```

---

## Ringkasan File & Statistik Kode

### Total File Sumber Java

| # | File | Package | Baris Kode |
|---|---|---|---|
| 1 | `Main.java` | `beastclash` | 18 |
| 2 | `SoundManager.java` | `audio` | 503 |
| 3 | `BattleController.java` | `controller` | 354 |
| 4 | `GameState.java` | `controller` | 214 |
| 5 | `BeastData.java` | `data` | 113 |
| 6 | `MapData.java` | `data` | 32 |
| 7 | `DatabaseManager.java` | `database` | 336 |
| 8 | `GachaSystem.java` | `gacha` | 142 |
| 9 | `Beast.java` | `model` | 152 |
| 10 | `GameMap.java` | `model` | 57 |
| 11 | `ResourceManager.java` | `resources` | 112 |
| 12 | `MainFrame.java` | `view` | 135 |
| 13 | `LoginPanel.java` | `view` | 448 |
| 14 | `StoryIntroPanel.java` | `view` | 599 |
| 15 | `MainMenuPanel.java` | `view` | 267 |
| 16 | `MapSelectPanel.java` | `view` | 313 |
| 17 | `BeastSelectPanel.java` | `view` | 586 |
| 18 | `BattlePanel.java` | `view` | **1.459** |
| 19 | `GachaPanel.java` | `view` | 476 |
| 20 | `EndingPanel.java` | `view` | 259 |
| 21 | `HPBar.java` | `view` | 72 |
| 22 | `ElementColor.java` | `view` | 46 |
| | **TOTAL** | | **~6.693 baris** |

### Aset Gambar

| Kategori | Jumlah File | Format |
|---|---|---|
| Gambar Beast | 24 file | `.png` |
| Gambar Map | 6 file | `.png` |
| **Total** | **30 file** | |

### Distribusi Kode per Package

```
view/         ~4.660 baris  (69.6%) — UI & rendering
controller/   ~  568 baris  ( 8.5%) — logika game
audio/        ~  503 baris  ( 7.5%) — audio prosedural
database/     ~  336 baris  ( 5.0%) — database layer
data/         ~  145 baris  ( 2.2%) — katalog data
model/        ~  209 baris  ( 3.1%) — entitas game
gacha/        ~  142 baris  ( 2.1%) — sistem gacha
resources/    ~  112 baris  ( 1.7%) — manajemen aset
beastclash/   ~   18 baris  ( 0.3%) — entry point
```

---

## Penjelasan Detail Setiap File

---

### `Main.java` — Entry Point
**Baris:** 18 | **Package:** `beastclash`

File terkecil namun paling krusial. Titik awal eksekusi program.

```java
public static void main(String[] args) {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    SwingUtilities.invokeLater(() -> {
        MainFrame frame = new MainFrame();
        frame.setVisible(true);
    });
}
```

**Fungsi penting:**
- Menerapkan *System Look and Feel* agar tampilan mengikuti OS.
- Membungkus inisialisasi `MainFrame` di dalam `SwingUtilities.invokeLater()` untuk memastikan semua operasi UI berjalan di **Event Dispatch Thread (EDT)** — standar aman Swing.

---

### `SoundManager.java` — Audio Prosedural
**Baris:** 503 | **Package:** `audio`

Seluruh musik dan efek suara di-*generate* langsung dari kode sebagai gelombang PCM (Pulse-Code Modulation). Tidak ada file `.wav` atau `.mp3` yang dibutuhkan.

**BGM Track yang tersedia:**
| Track | Digunakan di |
|---|---|
| `MENU` | Main Menu, layar login |
| `BATTLE` | Arena pertarungan |
| `VICTORY` | Layar ending |
| `STORY` | Prolog cerita |

**SFX yang tersedia:**
`ATTACK`, `SKILL`, `ULTIMATE`, `HURT`, `VICTORY_SFX`, `DEFEAT`, `GACHA`, `FREEZE`, `CLICK`, `RUN`, `EGG`, `UNLOCK`

**Bagian penting:**

```java
// Perbaikan volume menggunakan formula dB yang benar
private void setVol(Clip clip, float level) {
    // Gunakan 20*log10 bukan linear
    FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
    float dB = (float)(20.0 * Math.log10(Math.max(0.0001, level)));
    gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
}
```

- SFX di-*preload* di background thread `SFX-Preload` (daemon) saat startup.
- Deteksi otomatis apakah audio device tersedia — semua operasi `no-op` tanpa crash jika tidak ada.
- BGM berjalan di thread terpisah agar tidak memblokir UI.

---

### `BattleController.java` — Engine Pertarungan
**Baris:** 354 | **Package:** `controller`

Inti dari seluruh sistem battle. Mengimplementasikan giliran berbasis kecepatan (*speed-based turn order*) mirip Honkai: Star Rail.

**Sistem Turn (HSR-style):**

```
ACTION_POINT = 10.000

Setiap "tick", actionValue semua Beast naik sebesar Speed-nya.
Beast yang pertama mencapai ACTION_POINT mendapat giliran.
Setelah bertindak, actionValue-nya dikurangi ACTION_POINT (bukan di-reset ke 0),
sehingga Beast cepat mendapat giliran lebih sering secara proporsional.
```

**Kelas Inner:**

| Kelas | Fungsi |
|---|---|
| `TurnEntry` | DTO berisi referensi Beast, tim (player/enemy), dan `actionValue` saat ini |
| `BattleResult` | DTO hasil aksi: log teks, flag kemenangan/kekalahan, damage, dan target |

**Aksi yang tersedia:**

| Aksi | Mana | Efek |
|---|---|---|
| `performAttack()` | 0 MP | Serangan normal ke satu target |
| `performSkill()` | 30 MP | Serangan 1.5× ke satu target |
| `performUltimate()` | 70 MP | Serangan 2× ke satu target |
| `performRun()` | 0 MP | 50% berhasil kabur |
| `performEnemyTurn()` | — | AI enemy: 20% skill, 80% attack normal |

**Pemulihan Mana otomatis:**
- Player: +8 MP setiap akhir giliran
- Enemy: +10 MP setiap akhir giliran

**`skipCurrentTurn()`** — digunakan untuk efek Freeze agar giliran Beast tertentu dilewati tanpa melakukan aksi.

---

### `GameState.java` — State Global (Singleton)
**Baris:** 214 | **Package:** `controller`

Satu-satunya sumber kebenaran (*single source of truth*) untuk seluruh kondisi game yang sedang berjalan. Diimplementasikan sebagai Singleton.

**Data yang dikelola:**

| Field | Keterangan |
|---|---|
| `maps` | Daftar 6 GameMap dengan status unlock dan progres level |
| `playerTeam` | Tim Beast pilihan pemain (maks. 3) |
| `enemyTeam` | Tim Beast musuh yang di-generate per level |
| `selectedMap` | Map yang sedang aktif |
| `currentLevel` | Level dalam map (1–4) |
| `currentUserId` | ID user login (-1 = offline) |
| `cachedOwnedBeastIds` | Cache ID Beast yang dimiliki (hindari query DB berulang) |
| `offlineEggs` | Telur gacha untuk mode offline |

**Metode penting:**

```java
// Muat progres dari DB setelah login
public void loadProgressFromDB()

// Simpan progres ke DB setelah level selesai
public void saveMapProgressToDB()

// Invalidasi cache setelah gacha unlock beast baru
public void invalidateBeastCache()

// Hanya kembalikan Beast yang dimiliki user
public List<Beast> getAvailableBeasts()
```

**Mode offline:** Jika `currentUserId <= 0` atau DB tidak terhubung, semua data disimpan di memori (`offlineEggs`, `offlineOwnedIds`) dan hilang saat session berakhir.

---

### `Beast.java` — Entitas Beast
**Baris:** 152 | **Package:** `model`

Model data inti yang merepresentasikan satu Beast dalam game.

**Atribut:**

| Atribut | Keterangan |
|---|---|
| `id` | ID unik (1–24) |
| `name` | Nama Beast |
| `element` | Elemen: Api / Air / Tanah / Daun / Cahaya / Gelap |
| `maxHP / currentHP` | Titik darah |
| `maxMana / currentMana` | Poin mana untuk skill/ultimate |
| `attack` | Serangan dasar |
| `defense` | Pertahanan (mengurangi damage masuk) |
| `speed` | Menentukan seberapa sering mendapat giliran |
| `baseAttack/Defense/Speed` | Nilai asli (untuk reset setelah battle) |

**Sistem Elemen — Rantai Kelemahan:**

```
Api     → kuat vs Daun  (1.5×), lemah vs Air   (0.5×)
Air     → kuat vs Api   (1.5×), lemah vs Tanah (0.5×)
Tanah   → kuat vs Air   (1.5×), lemah vs Daun  (0.5×)
Daun    → kuat vs Tanah (1.5×), lemah vs Api   (0.5×)
Cahaya  ↔ Gelap : saling melemahkan (keduanya 1.5× satu sama lain)
```

**Formula Damage:**

```java
// Attack normal
int raw = Math.max(1, attack - target.defense / 2);
damage = (int)(raw * elementMultiplier);

// Skill (1.5× ATK)
int raw = Math.max(1, (int)(attack * 1.5) - target.defense / 2);

// Ultimate (2× ATK)
int raw = Math.max(1, attack * 2 - target.defense / 3);
```

**Modifier stat** (`multiplyAttack/Defense/Speed`) digunakan oleh efek map (misal: lava di Volcano mengurangi DEF) dan direset saat `reset()` dipanggil setelah battle.

---

### `GameMap.java` — Entitas Map
**Baris:** 57 | **Package:** `model`

Representasi satu wilayah/dunia yang bisa dijelajahi pemain.

**Atribut:**

| Atribut | Keterangan |
|---|---|
| `name` | Nama map (harus cocok dengan nama file PNG di resources) |
| `enemyElement` | Elemen dominan musuh di map ini |
| `maxLevels` | Jumlah level yang harus diselesaikan (3–4) |
| `completedLevels` | Jumlah level yang sudah selesai |
| `unlocked` | Apakah map sudah bisa dimainkan |

Map baru terbuka (*unlock*) setelah map sebelumnya diselesaikan seluruh levelnya.

---

### `BeastData.java` — Katalog 24 Beast
**Baris:** 113 | **Package:** `data`

Database statis berisi seluruh data 24 Beast yang ada di game, dikelompokkan per elemen.

**Distribusi Beast per Elemen:**

| Elemen | Beast | ID |
|---|---|---|
| **Api** | Blazefang, Cinderion, Ignarox, Pyroth | 1–4 |
| **Air** | Aquarion, Marivex, Nerevion, Tsunadra | 5–8 |
| **Tanah** | Bedrock Titan, Gravok, Quakron, Terragorn | 9–12 |
| **Daun** | Floravine, Luminaire, Mossdrake, Rootzilla | 13–16 |
| **Cahaya** *(langka)* | Aetherion, Luxeron, Radiantor, Solareth | 17–20 |
| **Gelap** *(langka)* | Morvexis, Noctyra, Shadowfang, Umbrax | 21–24 |

**Beast Starter (offline):** Blazefang (1), Ignarox (3), Aquarion (5), Bedrock Titan (9), Floravine (13)

**Beast Starter (online/register):** Blazefang, Cinderion, Ignarox, Aquarion, Marivex, Bedrock Titan, Floravine, Aetherion (8 Beast)

**Generator Tim Enemy:**

```java
// Jumlah musuh per level
Level 1 → 2 musuh
Level 2 → 3 musuh
Level 3 → 4 musuh
Level 4 → 5 musuh (hanya di Dark Forest)

// 70% dari elemen dominan map, 30% elemen acak
// Stat musuh: 80-85% dari stat Beast normal + bonus (level-1)*12
```

---

### `MapData.java` — Daftar 6 Map
**Baris:** 32 | **Package:** `data`

```
Map 0 – Plains       | Elemen: Daun   | 3 level | Terbuka dari awal
Map 1 – Sea          | Elemen: Air    | 3 level | Unlock setelah Plains
Map 2 – Dessert      | Elemen: Tanah  | 3 level | Unlock setelah Sea
Map 3 – Blizzard     | Elemen: Cahaya | 3 level | Unlock setelah Dessert
Map 4 – Volcano      | Elemen: Api    | 3 level | Unlock setelah Blizzard
Map 5 – Dark Forest  | Elemen: Gelap  | 4 level | Map boss terakhir
```

---

### `DatabaseManager.java` — Lapisan Database
**Baris:** 336 | **Package:** `database`

Mengelola seluruh komunikasi dengan MySQL menggunakan **Singleton pattern** dan **JDBC**.

**Skema Database:**

```sql
-- Tabel users
CREATE TABLE users (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,    -- SHA-256 hash
    eggs       INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabel map_progress
CREATE TABLE map_progress (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    user_id          INT NOT NULL,
    map_index        INT NOT NULL,
    completed_levels INT DEFAULT 0,
    unlocked         TINYINT(1) DEFAULT 0,
    UNIQUE KEY uq_user_map (user_id, map_index),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Tabel beast_owned
CREATE TABLE beast_owned (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    user_id  INT NOT NULL,
    beast_id INT NOT NULL,
    UNIQUE KEY uq_user_beast (user_id, beast_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**Keamanan Password:** SHA-256 hash via `java.security.MessageDigest` — password tidak pernah disimpan dalam bentuk plaintext.

**Operasi utama:**

| Metode | Fungsi |
|---|---|
| `connect()` | Sambung ke MySQL, buat tabel jika belum ada |
| `register()` | Daftarkan user baru, inisialisasi starter beasts |
| `login()` | Verifikasi username + password hash |
| `getEggs() / addEggs() / spendEgg()` | Manajemen telur gacha |
| `getMapProgress() / saveMapProgress()` | Baca/tulis progres map |
| `getOwnedBeastIds() / unlockBeast()` | Manajemen koleksi Beast |
| `ownsAllBeasts()` | Cek apakah pemain sudah mengumpulkan semua 24 Beast |

---

### `GachaSystem.java` — Sistem Gacha
**Baris:** 142 | **Package:** `gacha`

Sistem *pull* Beast baru menggunakan **weighted random** dengan mekanisme **pity**.

**Bobot Pull:**

| Status Beast | Elemen | Bobot |
|---|---|---|
| Beast **BARU** | Api / Air / Tanah / Daun | 4 |
| Beast **BARU** | Cahaya / Gelap (langka) | 2 |
| Beast **DUPLIKAT** | Semua elemen | 1 |

**Mekanisme Pity ke-10:**
Jika dalam 10 pull berturut-turut tidak mendapat Beast Cahaya/Gelap yang **baru**, pull ke-10 **dijamin** memberikan Beast Cahaya/Gelap baru (jika masih ada yang belum dimiliki).

**Biaya:** 1 Telur (*egg*) per pull. Telur didapat sebagai reward setelah menyelesaikan level battle.

**Dukungan Offline:** Jika `userId <= 0` atau DB tidak aktif, gacha menggunakan `offlineEggs` dari `GameState` dan menyimpan hasil ke `offlineOwnedIds` (in-memory).

---

### `ResourceManager.java` — Manajemen Aset
**Baris:** 112 | **Package:** `resources`

Load dan cache gambar Beast serta latar map menggunakan `HashMap` sebagai *image cache*.

**Fitur penting:**

```java
// Gambar Beast menghadap KANAN (untuk player di sisi kiri layar)
public BufferedImage getBeastForPlayer(String name)

// Gambar Beast di-flip horizontal → menghadap KIRI (untuk enemy di sisi kanan)
public BufferedImage getBeastForEnemy(String name)

// Flip dilakukan dengan AffineTransform
AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
tx.translate(-width, 0);
```

Semua gambar di-cache setelah pertama kali di-load agar tidak ada I/O berulang.

---

### `MainFrame.java` — Window Utama & Navigasi
**Baris:** 135 | **Package:** `view`

*Controller* navigasi layar menggunakan **CardLayout**. Setiap navigasi ke layar baru membersihkan (*cleanup*) panel lama untuk mencegah *timer zombie* dan kebocoran memori.

**Layar yang tersedia:**

| Konstanta | Panel | Ukuran Window |
|---|---|---|
| `SCREEN_LOGIN` | `LoginPanel` | 560 × 560 |
| `SCREEN_STORY` | `StoryIntroPanel` | 680 × 520 |
| `SCREEN_MENU` | `MainMenuPanel` | 560 × 600 |
| `SCREEN_MAP` | `MapSelectPanel` | 560 × 640 |
| `SCREEN_BEAST` | `BeastSelectPanel` | 760 × 640 |
| `SCREEN_BATTLE` | `BattlePanel` | 920 × 640 |
| `SCREEN_GACHA` | `GachaPanel` | 580 × 580 |
| `SCREEN_ENDING` | `EndingPanel` | 720 × 580 |

Window otomatis di-*resize* dan di-*centered* ke layar monitor setiap kali berpindah panel.

---

### `LoginPanel.java` — Autentikasi
**Baris:** 448 | **Package:** `view`

Layar pertama yang tampil saat game dibuka. Menangani register dan login pengguna.

**Alur Login:**
```
1. Cek koneksi DB (sekali saja, tidak ganda)
2. Validasi input (username & password tidak kosong)
3. Kirim ke DatabaseManager.login()
4. Jika sukses: setCurrentUserId() → loadProgressFromDB() → showStory() / showMainMenu()
5. Jika gagal: tampilkan pesan error
```

**Mode Offline:** Tombol *"Masuk Offline"* tersedia jika DB tidak terhubung — memungkinkan bermain tanpa akun dengan Beast starter.

---

### `StoryIntroPanel.java` — Prolog Animasi
**Baris:** 599 | **Package:** `view`

Panel prolog berisi **6 halaman cerita** dengan animasi latar berbeda tiap halaman, menggunakan `javax.swing.Timer` untuk rendering animasi.

**Halaman Cerita:**

| Halaman | Judul | Animasi Latar |
|---|---|---|
| 0 | Dunia Arcana | Langit malam berbintang tenang |
| 1 | Malam yang Robek oleh Langit | Hujan meteor jatuh |
| 2 | Zenith | Siluet raksasa muncul dari kegelapan |
| 3 | Pecahan Kehancuran | Dunia retak, kristal Zenith tumbuh |
| 4 | Misi Terakhir | Layar *call-to-action* |
| 5 | Beast Clash | Title card game |

Teks muncul dengan efek *typewriter* — karakter demi karakter. Klik layar untuk skip ke teks lengkap.

---

### `MapSelectPanel.java` — Pilih Map
**Baris:** 313 | **Package:** `view`

Menampilkan 6 map dalam bentuk kartu bergambar. Map yang terkunci (*locked*) ditampilkan dengan overlay abu-abu dan ikon gembok. Klik map yang tersedia membawa pemain ke pemilihan level (1–3/4).

---

### `BeastSelectPanel.java` — Pilih Tim Beast
**Baris:** 586 | **Package:** `view`

Layar pemilihan Beast untuk tim player. Hanya menampilkan Beast yang dimiliki oleh user.

**Fitur:**
- Grid Beast dengan gambar, nama, elemen, dan stat (HP/ATK/DEF/SPD)
- Warna elemen berbeda (via `ElementColor`)
- Pemain memilih maksimal **3 Beast** untuk dibawa ke battle
- Validasi: minimal 1 Beast harus dipilih

---

### `BattlePanel.java` — Arena Pertarungan
**Baris:** 1.459 (file terbesar) | **Package:** `view`

Panel terkompleks dalam seluruh proyek. Menggabungkan rendering visual, logika UI, animasi, dan koordinasi dengan `BattleController`.

**Komponen Internal:**

| Kelas Inner | Fungsi |
|---|---|
| `BattleArena` | Custom `JPanel` yang menggambar latar map, sprite Beast, HP bar, damage number, dan efek partikel |
| `ActionOrderBar` | Visual antrian giliran (siapa giliran berikutnya, bergaya HSR) |

**Efek Map (per lingkungan):**

| Map | Efek Battle |
|---|---|
| Plains | Netral (tidak ada efek khusus) |
| Sea | DEF Beast Air +15%, DEF Beast Api -15% |
| Dessert | SPD semua Beast -10% (panas memengaruhi kecepatan) |
| Blizzard | Freezze acak: ada peluang giliran di-*skip* |
| Volcano | ATK Beast Api +20%, DEF Beast Tanah -20%, lava particles |
| Dark Forest | ATK Beast Gelap +20%, semua Beast non-Gelap -10% ATK |

**Animasi yang dirender:**
- Latar animasi per-frame (timer 40ms = 25 FPS)
- Sprite Beast player (kiri) dan enemy (kanan)
- HP/MP bar real-time
- Angka damage melayang (*floating damage text*)
- Partikel lava di Volcano
- Efek kilat di Blizzard
- Partikel kegelapan di Dark Forest

**Reward setelah menang:** +3 telur (*eggs*) per level. Map berikutnya otomatis di-*unlock* jika semua level map selesai.

---

### `GachaPanel.java` — Panel Gacha
**Baris:** 476 | **Package:** `view`

Antarmuka untuk melakukan pull Beast baru menggunakan telur.

**Informasi yang ditampilkan:**
- Jumlah telur yang dimiliki
- Pity counter (pull ke berapa)
- Hasil pull (gambar Beast, nama, elemen, status baru/duplikat)
- Jumlah Beast yang sudah dimiliki dari total 24

---

### `EndingPanel.java` — Layar Ending
**Baris:** 259 | **Package:** `view`

Ditampilkan ketika pemain berhasil mengumpulkan semua **24 Beast** (kondisi kemenangan sejati game).

**Visual:**
- Latar langit fajar biru-emas dengan animasi bintang dan partikel kunang-kunang
- Teks ending muncul satu baris per satu (*typewriter effect*)
- Judul beranimasi `* ARCANA DAMAI *` dengan gradien emas-biru
- Aurora berwarna-warni di bagian atas layar

---

### `HPBar.java` — Komponen HP/MP Bar
**Baris:** 72 | **Package:** `view`

Custom `JPanel` yang menampilkan HP dan MP bar dengan gradien warna:
- HP: merah-oranye (penuh) → merah gelap (kritis)
- MP: biru muda (penuh) → biru gelap

---

### `ElementColor.java` — Utilitas Warna Elemen
**Baris:** 46 | **Package:** `view`

Peta warna statis untuk tiap elemen:

| Elemen | Warna |
|---|---|
| Api | Oranye-merah |
| Air | Biru |
| Tanah | Cokelat |
| Daun | Hijau |
| Cahaya | Kuning emas |
| Gelap | Ungu gelap |

---

## Arsitektur & Alur Sistem

```
┌─────────────────────────────────────────────────────────────┐
│                        MainFrame                            │
│              (CardLayout Navigator + Window)                │
└──────────────────────────┬──────────────────────────────────┘
                           │  navigasi layar
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
     LoginPanel      StoryIntroPanel  MainMenuPanel
           │                               │
           ▼                    ┌──────────┼──────────┐
      GameState ◄──────────     ▼          ▼          ▼
      (Singleton)        MapSelectPanel  GachaPanel  [dll]
           │                    │
           ▼                    ▼
    DatabaseManager      BeastSelectPanel
    (MySQL CRUD)                │
                                ▼
                          BattlePanel
                                │
                                ▼
                        BattleController
                        (Turn Engine)
                                │
                    ┌───────────┼───────────┐
                    ▼           ▼           ▼
                 Beast.java  GameMap   BeastData
                 (Model)     (Model)   (Katalog)
```

**Alur Permainan Lengkap:**

```
Login/Register
    │
    ▼
Story Intro (6 halaman prolog)
    │
    ▼
Main Menu
    │
    ├──► Gacha (pull Beast baru dengan telur)
    │
    └──► Map Select → Level Select
              │
              ▼
         Beast Select (pilih tim 1-3 Beast)
              │
              ▼
         Battle Panel (turn-based combat)
              │
         ┌────┴────┐
         ▼         ▼
       Menang    Kalah → kembali ke menu
         │
         ▼
    +3 Telur reward
    Simpan progres ke DB
    Cek apakah sudah punya 24 Beast
         │
    ┌────┴────┐
    ▼         ▼
 Ending      Lanjut
 Panel       main
```

---

## Lore & Cerita Beast Clash

### Dunia Arcana

Di jagad raya yang disebut **Arcana**, makhluk-makhluk legendaris yang dikenal sebagai **Beast** telah hidup berdampingan dengan manusia selama ribuan tahun. Enam elemen primordial — **Api, Air, Tanah, Daun, Cahaya, dan Gelap** — menjaga keseimbangan alam semesta Arcana dalam harmoni yang rapuh namun abadi.

Para **Pelatih Beast** adalah penjaga keseimbangan ini. Mereka membangun ikatan dengan Beast, melatih mereka, dan bersama-sama melindungi kedamaian Arcana dari ancaman yang datang silih berganti.

Selama berabad-abad, Arcana hidup tenang.

---

### Datangnya Zenith

Malam itu tidak seperti malam-malam lainnya.

Ratusan **meteor** menyobek langit Arcana. Bumi berguncang. Lautan mendidih. Hutan-hutan kuno terbakar dalam sekejap. Namun ini bukan sembarang hujan meteor — ini adalah **Pecahan Zenith**.

**ZENITH** adalah senjata pemusnah massal berbentuk bintang raksasa yang diciptakan oleh peradaban kuno yang telah lama punah. Dirancang untuk menghancurkan dunia-dunia yang dianggap *"tidak layak"* oleh para penciptanya, Zenith telah melayang dalam kegelapan antariksa selama ribuan tahun.

Dan kini — Zenith telah terbangun. Orbitnya mengarah langsung ke Arcana.

---

### Kristal Zenith & Beast yang Terkontaminasi

Setiap pecahan meteor yang jatuh membawa **Kristal Zenith** — batu gelap bercahaya yang memancarkan energi korup. Energi ini mengubah Beast-Beast yang menyentuhnya menjadi liar dan ganas, menyerang manusia tanpa henti.

Lebih buruk lagi: jika semua kristal terkumpul di satu titik, Zenith akan bangkit sepenuhnya dan menghancurkan Arcana dalam sekejap mata.

---

### Misi Sang Pelatih

Organisasi penjaga Arcana — **"The Wardens"** — telah memilih satu Pelatih untuk mengemban tugas yang mustahil:

> *Hancurkan kristal-kristal Zenith yang tersebar di enam penjuru Arcana.*  
> *Kalahkan Beast yang telah terkontaminasi.*  
> *Cegah Zenith dari kebangkitannya.*  
> *Dunia Arcana bergantung padamu.*

Perjalanan dimulai dari **Plains** yang tenang, melewati **Sea** yang bergelombang, **Dessert** yang terbakar, **Blizzard** yang membekukan, **Volcano** yang mengamuk, hingga akhirnya tiba di **Dark Forest** — hutan tergelap di Arcana, tempat kristal Zenith terbesar bersembunyi.

---

## Sistem Ending

### Kondisi Ending

Ending game terpicu ketika pemain berhasil **mengumpulkan semua 24 Beast** dalam koleksinya — baik melalui starter awal maupun sistem gacha.

### Ending: "Arcana Damai"

Ketika Beast ke-24 berhasil dikumpulkan, layar `EndingPanel` ditampilkan dengan latar langit fajar biru-emas. Teks ending muncul perlahan, satu baris demi satu:

```
"Semua kristal Zenith telah dihancurkan."
"Beast-beast Arcana kembali hidup dalam damai."
"Zenith tidak jadi bangkit."
"Dunia Arcana terselamatkan."

"Di bawah langit biru yang tenang,
manusia dan Beast kembali berdampingan —
seperti ribuan tahun sebelumnya."

"Kamu, sang Pelatih, kini menjadi legenda.
The Wardens menorehkan namamu
di Menara Bintang Arcana untuk selamanya."

"— T H E   E N D —"
```

### Makna Ending

Zenith tidak dikalahkan dalam satu pertarungan epik — melainkan dilemahkan perlahan oleh ikatan antara manusia dan Beast. Setiap kristal yang dihancurkan, setiap Beast yang diselamatkan dari kontaminasi, merupakan satu langkah lebih jauh menjauhkan Arcana dari kehancuran.

Ketika semua 24 Beast telah bersatu — enam elemen dalam harmoni sempurna — energi Zenith kehilangan sumbernya dan bintang pemusnah itu kembali tertidur untuk selamanya.

**Pesan ending:** Kekuatan sejati tidak berasal dari satu pahlawan, melainkan dari persatuan semua makhluk yang berjuang bersama.

---

## Slogan

---

> **"Enam Elemen. Satu Dunia. Tak Terbatas Kemungkinan."**

---

> *"Beast bukan sekadar senjata — mereka adalah sahabat yang akan berdiri bersamamu di tengah badai tergelap Arcana."*

---

> **"Kumpulkan. Latih. Bangkitkan. — Selamatkan Arcana."**

---

*Beast Clash © 2026 — Dikembangkan dengan ❤️ oleh Tim Beast Clash*  
*Agung Wahyu Niti Wijaya · Ahmad Dziqro Attayu Setio Damar · Septi Lailatul Fitria · Raga Deva Bela Negara · Nova Salwa Safitri*
