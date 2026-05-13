# 🐉 Beast Clash

> **Cegah kebangkitan ZENITH — sebelum Arcana hancur selamanya.**

Beast Clash adalah game **turn-based RPG** berbasis Java Swing di mana pemain mengumpulkan Beast, membentuk tim, dan bertarung melintasi enam wilayah Arcana untuk mencegah bangkitnya senjata pemusnah massal bernama **Zenith**.

---

## 📖 Lore

Dunia **Arcana** hidup dalam harmoni selama ribuan tahun, dijaga oleh enam elemen kekuatan dan para Pelatih Beast. Hingga suatu malam, ratusan meteor menyobek langit — bukan sembarang meteor, melainkan **pecahan Zenith**: senjata pemusnah massal ciptaan peradaban kuno yang kini terbangun kembali dan orbitnya mengarah ke Arcana.

Setiap pecahan membawa **Kristal Zenith** yang mengkorupsi Beast, menjadikannya liar dan berbahaya. Kamu adalah anggota **The Wardens** — organisasi penjaga Arcana — yang ditugaskan untuk:

- 🔮 Menghancurkan kristal-kristal Zenith yang tersebar
- ⚔️ Mengalahkan Beast yang telah terkontaminasi
- 🌍 Mencegah Zenith bangkit sepenuhnya

---

## ✨ Fitur Utama

### ⚔️ Sistem Battle Turn-Based (HSR-Style)
- Urutan giliran ditentukan oleh stat **Speed** masing-masing Beast — Beast tercepat mendapat giliran lebih sering
- **Action Value System**: setiap Beast mengakumulasi poin aksi; yang pertama mencapai 10.000 mendapat giliran
- UI **Action Order Bar** di bagian atas arena menampilkan urutan giliran seluruh Beast (gaya Honkai: Star Rail)
- Pilih target musuh secara bebas sebelum menyerang

### 🎯 Aksi Battle
| Aksi | MP | Efek |
|------|----|------|
| ⚔️ **ATTACK** | 0 | Serangan normal ke 1 target. Damage = ATK − DEF/2 |
| ✨ **SKILL** | 30 | Serangan kuat ke 1 target. Damage = (ATK × 1.5) − DEF/2 |
| 💥 **ULTIMATE** | 70 | Serang **semua** musuh sekaligus. Damage = (ATK × 2) − DEF/3 |
| 🏃 **RUN** | 0 | 50% peluang kabur dari pertarungan |

### 🗺️ Enam Map dengan Debuff Elemen
Setiap map memberikan efek unik yang memengaruhi stat Beast sesuai elemennya:

| Map | Elemen | Efek |
|-----|--------|------|
| 🌿 **Hutan Hijau** | Daun | Beast Api: ATK −20% |
| 🌵 **Desert** | Api | −3 HP/3 dtk (imun: Api) · Beast Air: DEF −25% |
| 🌊 **Lautan Biru** | Air | Beast Api: ATK −30% DEF −20% · Beast Daun: SPD −20% |
| ❄️ **Blizzard** | Air | Freeze chance · Beast Api: ATK −35% DEF −25% · Beast Air: DEF +10% |
| 🌋 **Volcano** | Api | −5 HP/3 dtk (imun: Api) · Beast Air: ATK −40% DEF −30% · Beast Api: ATK +15% |
| 🌑 **Hutan Gelap** | Gelap | Beast Cahaya: ATK/DEF −35% · Beast Gelap: ATK +20% DEF +15% |

### 🐾 24 Beast — 6 Elemen
| Elemen | Beast | Keterangan |
|--------|-------|------------|
| 🔥 Api | Ignaur, Pyrodon, Emberfang, Cindrix | Serangan tinggi, pertahanan rendah |
| 💧 Air | Aquarex, Tideclaw, Mistwave, Torrent | HP tinggi, seimbang |
| 🪨 Tanah | Terrok, Bouldrex, Gravelon, Stonefang | HP & DEF tertinggi, SPD rendah |
| 🌿 Daun | Verdix, Thornback, Mossclaw, Leafcrown | Seimbang, MP tinggi |
| ☀️ Cahaya ⭐ | Luminar, Radiance, Solaris, Gleamblade | **Langka** — ATK & SPD tinggi |
| 🌑 Gelap ⭐ | Umbrix, Shadowfang, Voidclaw, Duskhorn | **Langka** — Serangan paling mematikan |

*Beast Cahaya dan Gelap hanya bisa didapat melalui sistem Gacha.*

### ✨ Sistem Gacha
- Biaya: **1 Telur** per pull (Telur didapat dari memenangkan battle)
- Beast baru: bobot normal (elemen biasa lebih sering, langka lebih jarang)
- **Duplikat**: Beast yang sudah dimiliki tetap bisa keluar, namun dikonversi menjadi **+3 Telur** sebagai kompensasi
- **Sistem Pity**: pull ke-10 tanpa mendapat Cahaya/Gelap baru → dijamin mendapat beast langka baru

### 🔗 Sistem Kelemahan Elemen
```
Api   → kuat vs Daun,  lemah vs Air
Air   → kuat vs Api,   lemah vs Tanah
Tanah → kuat vs Air,   lemah vs Daun
Daun  → kuat vs Tanah, lemah vs Api
Cahaya ↔ Gelap (saling melemahkan 1.5×)
```

---

## 🖥️ Tampilan Game

```
┌─────────────────────────────────────────────────────┐
│  [Action Order Bar] P P P P P  E E E E E            │
├─────────────────────────────────────────────────────┤
│                                                     │
│          [Arena Battle — Beast vs Beast]            │
│                                                     │
├────────────┬────────────────────┬───────────────────┤
│ Beast Info │  [Target Musuh]    │  [📋 LOG]         │
│ HP ██████  │  [E1][E2][E3][E4]  │  ⚔ ATTACK        │
│ MP ████    │  [Efek Map]        │  ✨ SKILL (30MP)  │
│            │                    │  💥 ULTIMATE(70MP)│
│            │                    │  🏃 RUN           │
└────────────┴────────────────────┴───────────────────┘
```

---

## 🛠️ Persyaratan

| Komponen | Versi |
|----------|-------|
| **Java JDK** | 11 – 26 (dikompilasi dengan `--release 11`) |
| **NetBeans IDE** | 21 ke atas |
| **XAMPP** | 8.x (MySQL 8.x) |
| **MySQL Connector/J** | 9.x |

---

## 🚀 Cara Menjalankan

### 1. Clone Repository
```bash
git clone https://github.com/username/BeastClash.git
cd BeastClash
```

### 2. Setup Driver MySQL
1. Download **MySQL Connector/J** dari [dev.mysql.com/downloads/connector/j](https://dev.mysql.com/downloads/connector/j/)
2. Pilih *Platform Independent* → download `.zip`
3. Salin file `mysql-connector-j-X.X.X.jar` ke folder **`lib/`** di dalam project
4. Sesuaikan nama di `nbproject/project.properties` jika versi berbeda:
   ```properties
   file.reference.mysql-connector-j.jar=lib/mysql-connector-j-9.5.0.jar
   ```

### 3. Setup Database XAMPP
1. Jalankan **XAMPP** → klik **Start** pada MySQL
2. Buka **phpMyAdmin** → `http://localhost/phpmyadmin`
3. Buat database baru bernama **`beastclash`** (kosong, tabel dibuat otomatis)

### 4. Buka di NetBeans
1. **File → Open Project** → pilih folder `BeastClash`
2. Klik kanan project → **Properties → Libraries**
3. Pastikan `lib/mysql-connector-j-X.X.X.jar` sudah terdaftar
4. Tekan **F6** untuk menjalankan

---

## 🗂️ Struktur Project

```
BeastClash/
├── src/beastclash/
│   ├── Main.java                    # Entry point
│   ├── audio/
│   │   └── SoundManager.java        # PCM synthesis audio (tanpa file eksternal)
│   ├── controller/
│   │   ├── BattleController.java    # Engine turn-based (Action Value System)
│   │   └── GameState.java           # State global game (singleton)
│   ├── data/
│   │   ├── BeastData.java           # Katalog 24 Beast
│   │   └── MapData.java             # Daftar 6 Map
│   ├── database/
│   │   └── DatabaseManager.java     # Koneksi MySQL, auth, progress
│   ├── gacha/
│   │   └── GachaSystem.java         # Logika gacha + pity + duplikat
│   ├── model/
│   │   ├── Beast.java               # Model Beast (stat, damage, debuff)
│   │   └── GameMap.java             # Model Map (progress, unlock)
│   └── view/
│       ├── MainFrame.java           # Window utama + navigasi panel
│       ├── LoginPanel.java          # Login / Register
│       ├── StoryIntroPanel.java     # Prolog animasi (6 halaman)
│       ├── MainMenuPanel.java       # Menu utama
│       ├── MapSelectPanel.java      # Pilih map
│       ├── BeastSelectPanel.java    # Pilih 5 Beast untuk tim
│       ├── BattlePanel.java         # Arena battle HSR-style
│       ├── GachaPanel.java          # Sistem gacha
│       ├── ElementColor.java        # Warna & emoji per elemen
│       └── HPBar.java               # Komponen HP/MP bar
├── lib/
│   └── LETAKKAN_DRIVER_DISINI.txt   # Panduan instalasi MySQL Connector/J
└── nbproject/
    └── project.properties           # Konfigurasi NetBeans + classpath
```

---

## 🗄️ Skema Database

Tabel dibuat otomatis saat aplikasi pertama kali dijalankan.

```sql
-- Akun pengguna
CREATE TABLE users (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    eggs     INT DEFAULT 0
);

-- Beast yang dimiliki user
CREATE TABLE beast_owned (
    user_id  INT,
    beast_id INT,
    PRIMARY KEY (user_id, beast_id)
);

-- Progress per map
CREATE TABLE map_progress (
    user_id          INT,
    map_index        INT,
    completed_levels INT DEFAULT 0,
    unlocked         TINYINT DEFAULT 0,
    PRIMARY KEY (user_id, map_index)
);
```

---

## 🎮 Cara Bermain

1. **Daftar / Login** — buat akun baru atau masuk dengan akun yang ada
2. **Tonton Story** — prolog animasi tentang Zenith (bisa di-skip)
3. **Pilih Map** — mulai dari *Hutan Hijau*, selesaikan semua level untuk membuka map berikutnya
4. **Pilih 5 Beast** — susun tim dari Beast yang kamu miliki
5. **Battle** — ikuti urutan giliran di Action Order Bar, pilih target, dan gunakan aksi yang tepat
6. **Gacha** — tukarkan Telur yang didapat dari kemenangan untuk mendapat Beast baru
7. **Cegah Zenith** — tamatkan semua 6 map untuk menyelamatkan Arcana!

### Tips Strategis
- 🔄 Perhatikan **kelemahan elemen** — serangan elemen yang unggul memberikan bonus damage
- 📋 Buka **LOG** untuk melihat detail damage dan efek status
- ❄️ Di **Blizzard**, Beast Air mendapat bonus DEF — jadikan "tank" tim
- 🌋 Di **Volcano**, hindari membawa Beast Air — stat-nya turun drastis
- ⭐ Prioritaskan mendapat Beast **Cahaya** atau **Gelap** untuk melawan Hutan Gelap

---

## 🔧 Mode Offline

Jika database tidak tersedia, game tetap bisa dimainkan dalam **mode offline**:
- Semua 24 Beast tersedia langsung (tanpa gacha)
- Progress tidak disimpan
- Klik tombol **"▶ Main Offline"** di halaman login

---

## 🐛 Diketahui / Catatan Teknis

- Audio di-*synthesize* secara **prosedural** menggunakan PCM — tidak membutuhkan file `.wav` atau `.mp3` apapun
- Sistem debuff map bersifat **non-permanen**: stat Beast dikembalikan ke nilai semula setelah setiap battle melalui `Beast.reset()`
- Semua operasi database berjalan di **thread terpisah** agar UI tidak freeze

---

## 👤 Author

Dibuat sebagai proyek game Java Swing berbasis OOP.

---

## 📄 Lisensi

Project ini dibuat untuk keperluan pembelajaran. Bebas digunakan dan dimodifikasi.
