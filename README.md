# 🐉 BEAST CLASH - Setup Guide

## Cara Membuka di NetBeans

1. Buka **NetBeans IDE**
2. Pilih **File → Open Project**
3. Arahkan ke folder `BeastClash` (folder yang berisi nbproject/)
4. Klik **Open Project**
5. Tekan **F6** atau klik tombol ▶ **Run Project**

## Struktur Folder

```
BeastClash/
├── src/
│   └── beastclash/
│       ├── Main.java                    ← Entry point utama
│       ├── controller/
│       │   ├── BattleController.java    ← Logika pertempuran
│       │   └── GameState.java           ← State game (singleton)
│       ├── data/
│       │   ├── BeastData.java           ← Data 24 beast
│       │   └── MapData.java             ← Data 4 map
│       ├── model/
│       │   ├── Beast.java               ← Model beast
│       │   └── GameMap.java             ← Model map
│       └── view/
│           ├── MainFrame.java           ← JFrame utama (navigasi)
│           ├── MainMenuPanel.java       ← Layar menu utama
│           ├── MapSelectPanel.java      ← Layar pilih map
│           ├── BeastSelectPanel.java    ← Layar pilih 5 beast
│           ├── BattlePanel.java         ← Layar pertempuran
│           ├── HPBar.java               ← Custom HP/Mana bar
│           └── ElementColor.java        ← Utility warna elemen
├── nbproject/
│   ├── project.xml
│   └── project.properties
└── manifest.mf
```

## Fitur Game

### 🗺 Menu Utama
- Tombol **MULAI** → munculkan PLAY / CREDIT / EXIT
- Animasi langit bergerak dengan awan
- Tombol **CREDIT** menampilkan nama tim
- Tombol **EXIT** konfirmasi keluar

### 🗺 Pilih Map
- **4 map**: Grass Land, Blizzard, Volcano, Desert
- Hanya Grass Land yang terbuka di awal
- Map berikutnya terbuka setelah semua level map sebelumnya selesai
- Setiap map punya **3 level**

### 🐉 Pilih Beast
- **24 beast** tersedia (4 per elemen)
- **6 elemen**: Api 🔥, Air 💧, Tanah 🪨, Daun 🌿, Cahaya ✨, Gelap 🌑
- Filter berdasarkan elemen
- Pilih **tepat 5 beast** untuk tim
- Preview tim di bagian bawah
- Info elemen musuh ditampilkan

### ⚔ Battle
- **Attack** - Serangan normal, restore 10 MP
- **Skill** - Serangan kuat (25 MP)
- **Ultimate** - Serangan dahsyat + splash (60 MP)
- **Run** - 60% berhasil kabur
- HP & Mana bar real-time
- Log pertempuran (kanan)
- Ganti beast aktif (klik tombol beast di bawah)
- Efektivitas elemen (1.5x / 0.5x)

### 🔥 Sistem Elemen
| Kuat vs    | Lemah vs   |
|-----------|------------|
| Api → Daun | Api → Air  |
| Air → Api  | Air → Daun |
| Daun → Air | Daun → Api |
| Cahaya → Gelap | Cahaya → Tanah |
| Gelap → Tanah  | Gelap → Cahaya |
| Tanah → Cahaya | Tanah → Gelap  |

## Persyaratan
- Java JDK 11 atau lebih baru
- NetBeans IDE 12+
