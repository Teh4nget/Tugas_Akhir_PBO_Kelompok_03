# 🐉 Beast Clash

> **Cegah kebangkitan ZENITH — sebelum Arcana hancur selamanya.**

Beast Clash adalah game **turn-based RPG** berbasis Java Swing di mana pemain mengumpulkan Beast, membentuk tim, dan bertarung melintasi enam wilayah Arcana untuk mencegah bangkitnya senjata pemusnah massal bernama **Zenith**.

---

## 📖 Lore

Dunia **Arcana** hidup dalam harmoni selama ribuan tahun, dijaga oleh enam elemen kekuatan dan para Pelatih Beast. Hingga suatu malam, ratusan meteor menyobek langit — bukan sembarang meteor, melainkan **pecahan Zenith**: senjata pemusnah massal ciptaan peradaban kuno yang kini terbangun kembali dan orbitnya mengarah ke Arcana.

Setiap pecahan membawa **Kristal Zenith** yang mengkorupsi Beast dan menjadikannya liar. Kamu adalah anggota **The Wardens** — organisasi penjaga Arcana — yang ditugaskan untuk:

- Menghancurkan kristal-kristal Zenith yang tersebar
- Mengalahkan Beast yang telah terkontaminasi
- Mencegah Zenith bangkit sepenuhnya

---

## ✨ Fitur Utama

### Battle Turn-Based (HSR-Style)
- Urutan giliran ditentukan oleh stat **Speed** — Beast tercepat mendapat giliran lebih sering
- **Action Value System**: Beast mengakumulasi poin aksi hingga 10.000 untuk mendapat giliran
- UI **Action Order Bar** menampilkan urutan giliran semua Beast dengan gambar sprite-nya
- Pertarungan **1 vs 1** — satu Beast tim melawan satu Beast musuh bergantian sesuai urutan

### Aksi Battle
| Aksi | MP | Efek |
|------|----|------|
| **ATTACK** | 0 | Serangan normal ke 1 target. Damage = ATK − DEF/2 |
| **SKILL** | 30 | Serangan kuat ke 1 target. Damage = (ATK × 1.5) − DEF/2 |
| **ULTIMATE** | 70 | Serangan terkuat ke 1 target. Damage = (ATK × 2) − DEF/3 |
| **RUN** | 0 | 50% peluang kabur dari pertarungan |

### Efek Visual Battle
- **Hit Effect** — Angka damage melayang ke atas saat serangan mendarat (emas untuk Skill/Ultimate)
- **Flash Effect** — Sprite Beast berpendar merah/oranye mengikuti bentuk gambarnya saat terkena serangan
- **Death Effect** — Kristal ungu meledak saat Beast mati, disertai efek suara

### Enam Map dengan Debuff Elemen
| # | Map | Efek Utama |
|---|-----|------------|
| 1 | Plains | Beast Api: ATK -20% |
| 2 | Sea | Beast Api: ATK/DEF berkurang · Beast Daun: SPD -20% |
| 3 | Dessert | -3 HP/3 dtk (imun: Api & Tanah) · Beast Air: DEF -25% |
| 4 | Blizzard | Beast Api: ATK/DEF berkurang · Beast Cahaya: DEF +15% · Efek Freeze |
| 5 | Volcano | -5 HP/3 dtk (imun: Api) · Beast Air: ATK/DEF berkurang · Beast Api: ATK +15% |
| 6 | Dark Forest | Beast Cahaya: ATK/DEF berkurang · Beast Gelap: ATK/DEF meningkat |

### 24 Beast — 6 Elemen
| Elemen | Beast |
|--------|-------|
| Api | Blazefang, Cinderion, Ignarox, Pyroth |
| Air | Aquarion, Marivex, Nerevion, Tsunadra |
| Tanah | Bedrock Titan, Gravok, Quakron, Terragorn |
| Daun | Floravine, Luminaire, Mossdrake, Rootzilla |
| Cahaya ⭐ | Aetherion, Luxeron, Radiantor, Solareth |
| Gelap ⭐ | Morvexis, Noctyra, Shadowfang, Umbrax |

*Beast Cahaya dan Gelap hanya bisa didapat melalui sistem Gacha (langka).*

### Sistem Gacha
- Biaya **1 Telur** per pull (didapat dari memenangkan battle)
- Beast belum dimiliki: bobot normal (elemen biasa lebih sering, langka lebih jarang)
- Beast duplikat tetap bisa keluar dengan bobot lebih rendah
- **Sistem Pity**: pull ke-10 tanpa Cahaya/Gelap baru dijamin mendapat beast langka
- Menu Beast Select menampilkan semua 24 beast — yang belum dimiliki tampil abu-abu dengan keterangan "TERKUNCI"

### Sistem Kelemahan Elemen
```
Api    kuat vs Daun,  lemah vs Air
Air    kuat vs Api,   lemah vs Tanah
Tanah  kuat vs Air,   lemah vs Daun
Daun   kuat vs Tanah, lemah vs Api
Cahaya dan Gelap saling melemahkan (1.5x)
```

### Ending
Setelah semua **24 Beast berhasil dikumpulkan**, ending khusus akan muncul — Zenith tidak jadi bangkit, Arcana kembali damai, dan petualanganmu tercatat selamanya di Menara Bintang Arcana.

---

## 🛠️ Persyaratan

| Komponen | Versi |
|----------|-------|
| Java JDK | 26 |
| NetBeans IDE | 21 ke atas |
| XAMPP | 8.x (MySQL 8.x) |
| MySQL Connector/J | Sudah tersedia di folder `lib/` |

---

## 🚀 Cara Menjalankan

### 1. Clone Repository
```bash
git clone https://github.com/username/BeastClash.git
cd BeastClash
```

### 2. Setup Database XAMPP
1. Jalankan **XAMPP** → klik **Start** pada MySQL
2. Buka **phpMyAdmin** di `http://localhost/phpmyadmin`
3. Buat database baru bernama **`beastclash`** (kosong, tabel dibuat otomatis)

### 3. Buka di NetBeans
1. **File → Open Project** → pilih folder `BeastClash`
2. Klik kanan project → **Properties → Libraries**
3. Pastikan `lib/mysql-connector-j-*.jar` sudah terdaftar
4. Tekan **F6** untuk menjalankan

> **Catatan:** Jika MySQL tidak tersedia, game tetap bisa dimainkan dalam **mode offline** dengan menekan tombol "Main Offline" di halaman login.

---

## 🗂️ Struktur Project

```
BeastClash/
├── src/beastclash/
│   ├── audio/
│   │   └── SoundManager.java          # PCM synthesis audio (tanpa file eksternal)
│   ├── controller/
│   │   ├── BattleController.java      # Engine turn-based (Action Value System)
│   │   └── GameState.java             # State global game (singleton)
│   ├── data/
│   │   ├── BeastData.java             # Katalog 24 Beast
│   │   └── MapData.java               # Daftar 6 Map
│   ├── database/
│   │   └── DatabaseManager.java       # Koneksi MySQL, auth, progress
│   ├── gacha/
│   │   └── GachaSystem.java           # Logika gacha + pity
│   ├── model/
│   │   ├── Beast.java                 # Model Beast (stat, damage, debuff map)
│   │   └── GameMap.java               # Model Map (progress, unlock)
│   ├── resources/
│   │   ├── ResourceManager.java       # Load & cache gambar aset
│   │   ├── beast/                     # 24 gambar Beast (PNG, 1080×1080)
│   │   └── map/                       # 6 gambar background Map (PNG)
│   └── view/
│       ├── MainFrame.java             # Window utama + navigasi panel
│       ├── LoginPanel.java            # Login / Register
│       ├── StoryIntroPanel.java       # Prolog animasi (6 halaman)
│       ├── MainMenuPanel.java         # Menu utama
│       ├── MapSelectPanel.java        # Pilih map
│       ├── BeastSelectPanel.java      # Pilih 5 Beast untuk tim
│       ├── BattlePanel.java           # Arena battle HSR-style + efek visual
│       ├── GachaPanel.java            # Sistem gacha
│       ├── EndingPanel.java           # Ending setelah kumpulkan semua Beast
│       └── ElementColor.java          # Warna per elemen
├── lib/
│   └── mysql-connector-j-*.jar        # Driver MySQL (sudah tersedia)
└── nbproject/
    └── project.properties             # Konfigurasi NetBeans
```

---

## 🗄️ Skema Database

Tabel dibuat otomatis saat pertama kali dijalankan.

```sql
CREATE TABLE users (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    eggs     INT DEFAULT 0
);

CREATE TABLE beast_owned (
    user_id  INT,
    beast_id INT,
    PRIMARY KEY (user_id, beast_id)
);

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

1. **Daftar / Login** — buat akun atau masuk (mode offline tersedia jika tanpa database)
2. **Tonton Story** — prolog animasi Zenith (bisa di-skip kapan saja)
3. **Pilih Map** — mulai dari *Plains*, selesaikan semua level untuk membuka map berikutnya
4. **Pilih 5 Beast** — susun tim dari Beast yang dimiliki (beast terkunci tampil abu-abu)
5. **Battle** — ikuti Action Order Bar, pilih target musuh, gunakan aksi yang tepat
6. **Gacha** — tukarkan Telur untuk mendapat Beast baru
7. **Kumpulkan semua 24 Beast** untuk menyaksikan ending!

### Tips Strategis
- Perhatikan **kelemahan elemen** — serangan elemen yang unggul memberikan bonus damage
- Di **Blizzard**, bawa Beast Cahaya untuk mendapat bonus DEF +15%
- Di **Volcano**, hindari membawa Beast Air karena stat-nya turun drastis
- Di **Dark Forest**, Beast Gelap mendapat ATK/DEF meningkat — manfaatkan ini
- Gunakan **ULTIMATE** di saat kritis karena memberikan damage tertinggi

---

## 👥 Tim Pengembang

| Nama |
|------|
| Agung Wahyu Niti Wijaya |
| Raga Deva Bela Negara |
| Ahmad Dziqro Attayu Setio Damar |
| Nova Salwa Safitri |
| Septi Lailatul Fitria |

© 2026 Beast Clash Team

---

## 📄 Lisensi

Project ini dibuat untuk keperluan pembelajaran. Bebas digunakan dan dimodifikasi.
