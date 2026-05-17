# ⚙️ Cara Setup — Beast Clash

## Persyaratan

| Komponen | Versi |
|---|---|
| NetBeans IDE | 21 ke atas |
| JDK | 11 – 26 |
| XAMPP | 8.x (MySQL 8.x) |
| MySQL Connector/J | 9.x |

---

## Langkah 1 — Clone Repository

```
git clone https://github.com/username/BeastClash.git](https://github.com/Teh4nget/Tugas_Akhir_PBO_Kelompok_03.git
cd BeastClash
```

## Langkah 2 — Siapkan Driver MySQL (Sudah ada dalam folder `lib/`)

1. Download `mysql-connector-j-9.x.x.jar` dari:  
   👉 https://dev.mysql.com/downloads/connector/j/

2. Salin file `.jar` ke folder `lib/` di dalam project:
   ```
   BeastClash/
   └── lib/
       └── mysql-connector-j-9.x.x.jar   ← letakkan di sini
   ```

---

## Langkah 3 — Siapkan Database (XAMPP)

1. Jalankan **XAMPP** → klik **Start** pada **Apache** dan **MySQL**
2. Buka **phpMyAdmin** → http://localhost/phpmyadmin
3. Buat database baru bernama **`beastclash`** (kosong, tidak perlu import SQL)

> Tabel `users`, `map_progress`, dan `beast_owned` dibuat **otomatis** oleh program saat pertama dijalankan.

---

## Langkah 4 — Buka di NetBeans

1. **File → Open Project** → pilih folder `BeastClash`
2. Klik kanan project → **Properties → Libraries**
3. Pastikan `lib/mysql-connector-j-9.x.x.jar` sudah terdaftar  
   Jika belum → klik **Add JAR/Folder** → pilih file tersebut
4. Tekan **F6** untuk menjalankan

---

## Mode Offline (Tanpa Database)

Jika MySQL tidak aktif, game tetap bisa dimainkan dalam **mode offline**:

- Beast starter tersedia secara otomatis
- Gacha menggunakan telur in-memory
- Progress **tidak tersimpan** saat aplikasi ditutup

Gunakan tombol **"Masuk Offline"** di layar login untuk memulai tanpa akun.

---

## Troubleshooting

| Masalah | Solusi |
|---|---|
| `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | JAR driver belum ditambahkan ke Libraries project |
| `Access denied for user 'root'` | Pastikan MySQL di XAMPP sudah **Start** |
| `Unknown database 'beastclash'` | Buat database `beastclash` di phpMyAdmin terlebih dahulu |
| Tidak ada suara | Audio device tidak tersedia di sistem — game tetap berjalan normal |
| Project tidak bisa dibuild | Pastikan JDK 11–26 sudah terpasang dan terdaftar di NetBeans |
