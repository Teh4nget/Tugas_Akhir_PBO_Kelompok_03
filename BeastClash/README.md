# Beast Clash – Panduan Setup & Changelog Bug Fix

## ⚙ Persyaratan
| Komponen | Versi |
|---|---|
| NetBeans IDE | 21 ke atas (termasuk 29) |
| JDK | 11 – 26 (project dikompilasi dengan `--release 11`) |
| XAMPP | 8.x (MySQL 8.x) |
| MySQL Connector/J | 9.x (letakkan di folder `lib/`) |

---

## 🚀 Cara Setup (pertama kali)

### 1. Siapkan Driver MySQL
- Download `mysql-connector-j-9.x.x.jar` dari https://dev.mysql.com/downloads/connector/j/
- Salin file `.jar` ke folder `lib/` di dalam project ini.
- Baca file `lib/LETAKKAN_DRIVER_DISINI.txt` untuk detail.

### 2. Siapkan Database XAMPP
1. Jalankan XAMPP → klik **Start** pada Apache dan MySQL.
2. Buka **phpMyAdmin** (http://localhost/phpmyadmin).
3. Buat database baru bernama **`beastclash`** (kosong, tidak perlu import SQL).
4. Tabel dibuat otomatis oleh program saat pertama dijalankan.

### 3. Buka di NetBeans
1. File → Open Project → pilih folder `BeastClash`.
2. Klik kanan project → **Properties → Libraries**.
3. Pastikan `lib/mysql-connector-j-9.x.x.jar` sudah terdaftar.
   Jika belum, klik **Add JAR/Folder** dan pilih file tersebut.
4. Tekan **F6** untuk menjalankan.

---

## 🐛 Daftar Bug yang Diperbaiki

### 1. `GameState.java` – Progress & Beast Tidak Dimuat dari DB

**Masalah:**
- `loadProgressFromDB()` hanya memuat map progress, tidak memuat owned beast IDs.
- Setelah login, beast yang dimiliki tidak di-cache → `getAvailableBeasts()` selalu query DB ulang, kadang hasilnya tidak konsisten.

**Perbaikan:**
- `loadProgressFromDB()` sekarang juga load & cache `ownedBeastIds` dari DB.
- Ditambah method `invalidateBeastCache()` yang dipanggil setelah gacha unlock beast baru.
- Maps di-reset ke kondisi awal sebelum load, mencegah data duplikat `completeLevel()`.
- `setCurrentUserId()` otomatis reset cache saat user berganti.

---

### 2. `LoginPanel.java` – Koneksi DB Ganda & Progress Tidak Termuat Lengkap

**Masalah:**
- `checkDBConnection()` memanggil `db.connect()` tanpa cek apakah sudah terhubung → koneksi ganda.
- Setelah login berhasil, `GameState.loadProgressFromDB()` hanya dimuat map, bukan beast.
- Tidak ada validasi apakah DB terhubung sebelum tombol login ditekan.

**Perbaikan:**
- `checkDBConnection()` kini cek `db.isConnected()` sebelum memanggil `connect()`.
- Setelah login sukses: `setCurrentUserId(uid)` lalu `loadProgressFromDB()` dipanggil — keduanya sekarang mencakup map progress **dan** owned beasts.
- Ditambah guard: jika DB tidak terhubung saat tombol MASUK ditekan, tampilkan pesan error alih-alih mencoba login.

---

### 3. `GachaPanel.java` – Tidak Aman di Mode Offline & Data Tidak Diperbarui

**Masalah:**
- `userId` bisa bernilai `-1` (mode offline), namun kode langsung memanggil `DatabaseManager.getEggs(-1)` dan `getOwnedBeastIds(-1)` → hasil selalu 0 / fallback 24 beast.
- Setelah pull berhasil, `GameState` tidak tahu ada beast baru → `BeastSelectPanel` masih menampilkan daftar lama.
- `lblOwned` (jumlah beast dimiliki) tidak diperbarui setelah pull.

**Perbaikan:**
- Ditambah guard `userId <= 0`: tombol PULL dinonaktifkan, tampil keterangan mode offline.
- Setelah pull berhasil: `GameState.invalidateBeastCache()` dipanggil agar `BeastSelectPanel` mendapat daftar beast terbaru dari DB.
- `refreshOwnedCount()` dipanggil setelah setiap pull untuk memperbarui label jumlah beast.
- Ditambah validasi `db.isConnected()` sebelum setiap operasi gacha.

---

### 4. `project.properties` – Path JDBC Hardcoded ke Drive Lokal

**Masalah:**
```
file.reference.mysql-connector-j-9.5.0.jar=E:\\Game\\apache netbeans dll\\...
```
Path ini hanya valid di komputer asli developer → project tidak bisa dibuka di komputer lain.

**Perbaikan:**
- Path diubah ke **relatif**: `lib/mysql-connector-j-9.5.0.jar`
- Folder `lib/` disertakan dalam project.
- Baca `lib/LETAKKAN_DRIVER_DISINI.txt` untuk cara menempatkan file jar.

---

### 5. Kompatibilitas JDK 26 + NetBeans 29

- `javac.source` dan `javac.target` tetap `11` — ini **sudah benar** dan kompatibel dengan JDK 26.
- JDK 26 mendukung `--release 11` sehingga kode dikompilasi sebagai Java 11 dan berjalan di semua JDK 11+.
- Tidak ada penggunaan API internal (`sun.*`, `com.sun.*`) yang diblokir module system.
- Semua kode menggunakan API standar (`javax.swing`, `java.sql`, `java.awt`) yang tersedia di semua versi JDK.

