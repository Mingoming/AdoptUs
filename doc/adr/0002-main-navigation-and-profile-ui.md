# Architecture Decision Record: 0002 - Navigasi Utama dan Profile Frontend Mode

* **Status:** Approved
* **Tanggal:** 23 Mei 2026
* **Penulis:** Tim Pengembang AdoptUs

## 1. Konteks

Setelah autentikasi Firebase tersedia, aplikasi membutuhkan halaman utama yang bisa menampung beberapa area fitur. Commit Devita menambahkan navigasi bawah, fragment utama, resource visual, dan halaman profile frontend mode agar aplikasi tidak berhenti di halaman kosong setelah login.

Fitur domain seperti feed nyata, pencarian hewan, add post, dan data profile dari backend belum selesai. Karena itu, implementasi saat ini memprioritaskan struktur navigasi dan tampilan awal yang bisa dikembangkan bertahap.

## 2. Keputusan

Kami menggunakan pendekatan berikut:

1. **MainActivity sebagai host fragment utama**
   * `MainActivity` tetap memeriksa session login melalui `AuthViewModel`.
   * Setelah user login, `MainActivity` memuat `activity_main.xml`.
   * `FrameLayout` dengan id `fragment_container` menjadi tempat fragment utama ditukar.

2. **BottomNavigationView untuk navigasi utama**
   * Menu utama disimpan di `res/menu/navbar.xml`.
   * Item navigasi yang tersedia: Feed, Search, Add Post, dan Profile.
   * Perpindahan halaman dilakukan manual dengan `supportFragmentManager.replace(...)`.
   * Navigation Component belum digunakan untuk flow utama.

3. **Fragment modular per halaman**
   * Halaman utama dipisah ke package `com.example.adoptus.fragment`.
   * `FeedFragment`, `SearchFragment`, dan `AddPostFragment` masih placeholder.
   * `ProfileFragment` sudah berisi UI frontend mode yang lebih lengkap.

4. **Profile frontend mode dengan dummy data lokal**
   * `ProfileFragment` menampilkan header profile, statistik, bio, lokasi, tab, dan grid.
   * Tab `Pets` dan `Content` memakai `TabLayout`.
   * Grid memakai `RecyclerView` dengan `GridLayoutManager` 3 kolom.
   * Data masih dummy lokal melalui `DummyItem` dan `ProfileGridAdapter`.
   * Item content bertipe video menampilkan overlay ikon play.

5. **Resource visual dan warna mulai distandardisasi**
   * Warna aplikasi ditambahkan ke `colors.xml`.
   * State warna bottom navigation memakai `nav_item_color.xml`.
   * Background bottom navigation memakai `bg_bottom_nav.xml`.
   * Theme memakai `colorSecondaryContainer` untuk warna active indicator Material.

## 3. Konsekuensi

### Positif

* Aplikasi sudah memiliki struktur halaman utama setelah login.
* Navigasi bawah memberi jalur pengembangan yang jelas untuk fitur Feed, Search, Add Post, dan Profile.
* Fragment memisahkan tanggung jawab UI per halaman lebih baik daripada menumpuk semuanya di `MainActivity`.
* Profile frontend mode memungkinkan validasi tampilan sebelum integrasi backend.

### Negatif

* Perpindahan fragment masih manual dan belum memiliki back stack/navigasi terpusat.
* Beberapa fragment masih placeholder sehingga belum merepresentasikan fitur akhir.
* Data profile masih dummy lokal, belum sinkron dengan Firebase Auth atau Firestore.
* Dependency Compose/Navigation Compose sudah masuk, tetapi UI masih XML sehingga perlu dirapikan pada keputusan arsitektur berikutnya.

## 4. Catatan Lanjutan

Keputusan berikutnya perlu menentukan apakah aplikasi tetap memakai XML + Fragment manual, pindah ke Navigation Component, atau mulai memakai Compose secara nyata. Sebelum fitur domain ditambahkan lebih jauh, layout dan resource yang menghambat build harus dibersihkan agar baseline proyek stabil.
