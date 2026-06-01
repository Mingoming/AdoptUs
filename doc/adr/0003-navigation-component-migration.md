# ADR 0003 — Migrasi ke Jetpack Navigation Component

* **Status:** Approved
* **Tanggal:** 31 Mei 2026
* **Penulis:** Tim Pengembang AdoptUs

## 1. Konteks

Sebelumnya navigasi antar fragment dilakukan secara manual menggunakan `supportFragmentManager.replace()` di MainActivity. Pendekatan ini menimbulkan beberapa masalah:

- Back button HP di AddPostFragment langsung keluar app, bukan kembali ke fragment sebelumnya
- Tidak ada back stack — setiap pindah fragment membuat instance baru
- Navigasi ke halaman detail (PetDetail) dari beberapa fragment berbeda sulit dikelola
- Kode navigasi tersebar di MainActivity dan tiap Fragment

## 2. Keputusan

Migrasi ke **Jetpack Navigation Component 2.7.7** dengan pendekatan berikut:

1. **NavHostFragment** menggantikan `FrameLayout` di `activity_main.xml`
2. **`main_nav.xml`** sebagai single source of truth untuk semua alur navigasi
3. **`setupWithNavController()`** menghubungkan BottomNavigationView ke NavController secara otomatis
4. **ID menu item** di `navbar.xml` disamakan dengan ID destination di nav graph
5. **`findNavController().navigateUp()`** di tiap Fragment untuk back navigation
6. **`findNavController().navigate(R.id.action_xxx)`** untuk navigasi maju

## 3. Masalah yang Ditemui dan Solusi

### 3a. Gradle Sync Error — Invalid TOML
**Masalah:** Script sed menambahkan section `[versions-nav-fragment]` yang tidak valid di `libs.versions.toml`. TOML hanya mengizinkan section `[versions]`, `[libraries]`, `[plugins]`, `[bundles]`.
**Solusi:** Tulis ulang `libs.versions.toml` dari scratch tanpa section invalid.

### 3b. BottomNav tidak bisa dipencet
**Masalah:** `setupWithNavController()` mencocokkan ID menu item dengan ID destination — harus identik. ID lama (`menu_feed`, `menu_search`, dll) tidak cocok dengan ID fragment (`feedFragment`, `searchFragment`, dll).
**Solusi:** Ganti semua ID di `navbar.xml` agar cocok dengan ID di `main_nav.xml`.

### 3c. NavHost overlap BottomNav
**Masalah:** `FragmentContainerView` dengan `layout_above` di RelativeLayout tidak bekerja seperti yang diharapkan, menyebabkan NavHost menutupi area BottomNav.
**Solusi:** Ganti root layout ke `LinearLayout` vertikal dengan `layout_weight="1"` pada NavHost.

## 4. Konsekuensi

### Positif
- Back button HP otomatis ditangani NavController — tidak perlu `OnBackPressedCallback` manual
- Navigasi ke PetDetail dari Feed, Search, dan Profile tinggal `navigate(R.id.action_xxx)`
- Passing data ke detail (postId) via Bundle aman dan terdokumentasi di nav graph
- Animasi transisi slide konsisten di seluruh app

### Negatif
- ID menu di navbar.xml harus selalu sinkron dengan ID di nav graph — kalau tidak, BottomNav diam
- Safe Args tidak diaktifkan (butuh plugin tambahan) — args dipass via Bundle manual
