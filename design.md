# Dokumen Spesifikasi UI/UX: CatatUsaha

**Sistem Desain:** Modern Finance UI (Soft UI / Semi-Glassmorphism)
**Versi:** 2.0 (Redesign)

---

## 1. Prinsip Desain Utama

| Prinsip | Implementasi Praktis (Berdasarkan Referensi) |
| --- | --- |
| **Fokus pada Angka Utama** | Saldo/Keuntungan ditampilkan dalam ukuran *super-sized* di bagian atas layar dengan aksen gradien halus di bawahnya untuk penekanan visual. |
| **Grid Data Terstruktur** | Penggunaan tata letak kartu 2x2 (Pemasukan, Pengeluaran, Kas, Stok) yang memisahkan informasi menjadi blok-blok visual yang mudah dipindai (*scannable*). |
| **Navigasi Pill & Chip** | Filter rentang waktu ("Hari Ini", "Bulan Ini") dan kategori grafik menggunakan desain *pill* solid yang modern dan hemat ruang. |
| **Bercerita Lewat Visual** | Penggunaan *doughnut chart* bersisian dengan *legend* persentase, serta *line chart* halus untuk tren, membuat data analitik langsung dipahami tanpa perlu membaca tabel. |

---

## 2. Design Tokens & Visual Style

### 2.1 Warna (Semantik & Merek)

Gaya visual menggunakan pendekatan latar belakang netral dengan kartu putih bersih (Terang) atau abu-abu pekat (Gelap), dipadukan dengan aksen warna data yang cerah namun lembut (*pastel/neon-tinted*).

| Token | Mode Terang | Mode Gelap | Penggunaan |
| --- | --- | --- | --- |
| `bgBase` | `#F5F7F9` (Abu sangat muda) | `#121212` (Hitam solid) | Latar belakang utama aplikasi |
| `bgCard` | `#FFFFFF` (Putih) | `#1C1C1E` (Abu tua) | Latar kartu (Grid 2x2, Daftar Transaksi) |
| `primary` | `#000000` (Hitam) | `#FFFFFF` (Putih) | Teks utama, judul, angka saldo |
| `accent` | `#6366F1` (Indigo) | `#818CF8` (Indigo terang) | Elemen aktif, *Pill* tab yang dipilih |
| `income` | `#10B981` (Hijau zamrud) | `#34D399` (Hijau terang) | Angka pemasukan, ikon masuk, tren naik |
| `expense` | `#EF4444` (Merah bata) | `#F87171` (Merah pastel) | Angka pengeluaran, ikon keluar, tren turun |
| `gradientA` | `Linear: #E0C3FC to #8EC5FC` | `Linear: #432C7A to #264A7F` | Garis aksen di bawah "Total Saldo" |
| `glassShadow` | `0px 4px 20px rgba(0,0,0,0.05)` | `0px 4px 20px rgba(0,0,0,0.4)` | Bayangan kartu agar tampak melayang halus |

### 2.2 Tipografi

**Keluarga Font:** *SF Pro Display* (iOS) atau *Inter* (Android/Web). Desain ini sangat bergantung pada tipografi geometris yang bersih.

| Skala | Ukuran / Berat | Penggunaan |
| --- | --- | --- |
| `BalanceHero` | 40px / SemiBold | Angka Total Saldo/Keuntungan di Beranda |
| `Header1` | 24px / Bold | Sapaan ("Selamat Pagi!"), Judul Layar |
| `CardTitle` | 14px / Medium | Label dalam grid 2x2 ("Pemasukan", "Pengeluaran") |
| `CardValue` | 20px / SemiBold | Angka nominal di dalam kartu grid 2x2 |
| `BodyText` | 14px / Regular | Nama item transaksi, teks deskripsi |
| `SmallLabel` | 12px / Regular | Waktu transaksi ("08:30 AM"), Kategori |

### 2.3 Elemen UI Spesifik

* **Card Styling:** Sudut melengkung besar (*border-radius* 20px). Tanpa garis tepi (borderless) di mode terang, hanya menggunakan *soft shadow*.
* **Iconography:** Ikon ditempatkan di dalam lingkaran dengan latar transparan-berwarna (contoh: ikon pemasukan hijau di atas lingkaran hijau 15% opacity).
* **Avatar & Header:** Foto profil pengguna berbentuk lingkaran kecil di pojok kiri atas, disandingkan dengan tombol notifikasi (lonceng) di kanan atas.

---

## 3. Komponen Inti

| Komponen | Spesifikasi & Perilaku |
| --- | --- |
| **Hero Balance** | Judul label abu-abu (mis. "Total Saldo" atau "Untung Bersih"), angka nominal raksasa, indikator persentase naik/turun berwarna hijau/merah, dan sapuan gradien horizontal tipis sebagai alas. |
| **Overview 2x2 Grid** | 4 Kartu: Pemasukan, Pengeluaran, Kas, Nilai Stok. Tiap kartu memiliki Ikon (kiri atas), Label (kanan/samping ikon), Nominal (tengah), dan Tren % (bawah). |
| **Transaction Row** | Avatar/Logo Toko bulat (kiri) • Nama Transaksi & Jam (tengah) • Nominal tebal (kanan, hitam/putih untuk normal, hijau/merah khusus filter). |
| **Pill Navigation** | Baris filter tab (mis. "Ringkasan", "Pemasukan", "Pengeluaran"). Tab aktif berlatar hitam/gelap pekat dengan teks putih. Tab tidak aktif menyatu dengan *background*. |
| **Floating Action Button (FAB)** | Tombol bulat `+` mengambang, ditempatkan strategis di menu navigasi bawah untuk mencatat transaksi baru dengan cepat. |

---

## 4. Arsitektur Layar Utama & Wireframe (Text-Based)

### 4.1 Beranda (Dashboard)

```text
┌───────────────────────────────┐
│ (Avatar) Selamat Pagi!    [🔔]│
│          Bu Sari              │
│                               │
│ Total Saldo/Keuntungan        │
│ Rp12.750.000                  │
│ [━━━━ Gradien Halus ━━━━]     │
│                               │
│ Ringkasan        [Bulan Ini ▾]│
│ ┌──────────────┬─────────────┐│
│ │ 🟢 Pemasukan │ 🔴 Keluar   ││
│ │ Rp5.620.000  │ Rp3.480.000 ││
│ │ ↗ 8.2%       │ ↘ 5.1%      ││
│ ├──────────────┼─────────────┤│
│ │ 📦 Nilai Stok│ 💳 Kas Tunai││
│ │ Rp2.140.000  │ Rp1.950.000 ││
│ │ ↗ 15.3%      │ ↗ 7.8%      ││
│ └──────────────┴─────────────┘│
│                               │
│ Riwayat Transaksi   [Lihat >] │
│ ☕ Beli Kopi Susu   -Rp15.000 │
│    Hari ini, 08:30            │
│ 🛒 Beras 10kg      +Rp135.000 │
│    Hari ini, 15:15            │
│                               │
│   🏠     🧾    [+]    📦     📈   │
└───────────────────────────────┘
```

### 4.2 Analitik & Grafik (Analytics Tab)

```text
┌───────────────────────────────┐
│ <        Analitik Usaha    [⋮]│
│                               │
│ (Ringkasan)  Pemasukan  Keluar│  <-- Pill Navigation
│                               │
│ Total Pengeluaran [Bulan Ini ▾]
│ Rp3.480.000                   │
│ ↗ 5.1% dari bulan lalu        │
│                               │
│ ┌─────┐ Makanan       32%     │
│ │ 🍩  │ Rp1.113.600           │
│ │     │ Stok Barang   48%     │
│ └─────┘ Rp1.670.400           │
│                               │
│ Tren Pengeluaran              │
│ Rp420.500 (Rata-rata/hari)    │
│                               │
│    📈 [ Line Chart Halus ]    │
│ 1 Jan  8 Jan  15 Jan   22 Jan │
└───────────────────────────────┘
```

### 4.3 AI Assistant / Saran Bisnis (Opsional/Ekspansi)

```text
┌───────────────────────────────┐
│ <        Asisten Usaha     [⋮]│
│                               │
│ ✨                            │
│ Halo Bu Sari, mari kelola     │
│ usaha lebih cerdas.           │
│                               │
│ Yang bisa Ibu lakukan:        │
│ ┌───────────────────────────┐ │
│ │ 💰 Bagaimana cara hemat?  │ │
│ │    Ubah pengeluaran kecil │ │
│ └───────────────────────────┘ │
│ ┌───────────────────────────┐ │
│ │ 📊 Analisis belanja saya  │ │
│ │    Cari pengeluaran bocor │ │
│ └───────────────────────────┘ │
│                               │
│ [ + Tanya apa saja ke AI... ] │
└───────────────────────────────┘
```

---

## 5. Pola Interaksi & Mikro-UX

1. **Kartu Interaktif:** Menekan salah satu kartu di *Overview Grid 2x2* (misal: Pemasukan) akan langsung membawa pengguna ke layar Daftar Transaksi yang sudah difilter khusus untuk Pemasukan.
2. **Animasi Angka (Number Ticker):** Saat layar Beranda dimuat atau *pull-to-refresh* dilakukan, angka "Total Saldo" (`Rp12.750.000`) akan menghitung naik (*count up animation*) dari nol hingga nominal sebenarnya dalam durasi 600ms.
3. **Transisi Grafik:** Saat berpindah *pill tab* dari "Pemasukan" ke "Pengeluaran" di layar Analitik, *Doughnut Chart* tidak *refresh* secara kaku, melainkan *slices* (potongan kue) beranimasi menyesuaikan persentase data baru secara *fluid*.
4. **Sticky Bottom Action:** Tombol `+` (FAB) atau navigasi bawah menggunakan efek *background blur* (transparan *blur*) agar daftar transaksi yang di-*scroll* tetap terlihat samar di bawahnya.

## 6. Kriteria Aksesibilitas Visual

* Karena menggunakan bayangan halus (*soft shadows*) sebagai batas elemen di Mode Terang, pastikan ada **kontras minimal 1.1:1** antara warna latar belakang utama (`#F5F7F9`) dan latar belakang kartu (`#FFFFFF`).
* Jika di Mode Gelap bayangan tidak terlihat, gunakan batas (*border*) setebal 1px dengan warna `rgba(255, 255, 255, 0.1)` pada kartu grid untuk mempertegas pemisahan elemen.
* Data persentase di dalam *Doughnut Chart* harus ditarik keluar dengan garis penunjuk (*callout lines*) jika potongan terlalu kecil (< 10%) agar teks tetap terbaca dengan jelas.
