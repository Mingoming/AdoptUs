# Changelog - AdoptUs Mobile Project

Semua perubahan penting pada proyek Android **AdoptUs** didokumentasikan di file ini.

Format changelog ini mengikuti prinsip [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased] - 2026-05-23

### Added

* Menambahkan floating bottom navigation di `MainActivity` dengan 4 menu: Feed, Search, Add Post, dan Profile.
* Menambahkan package `fragment` berisi `FeedFragment`, `SearchFragment`, `AddPostFragment`, dan `ProfileFragment`.
* Menambahkan layout fragment untuk feed, search, add post, dan profile.
* Menambahkan `FrameLayout` `fragment_container` sebagai host fragment di `activity_main.xml`.
* Menambahkan menu `navbar.xml` untuk konfigurasi item bottom navigation.
* Menambahkan resource navigasi: `nav_item_color.xml`, `bg_bottom_nav.xml`, dan ikon feed/search/add/profile/settings.
* Menambahkan halaman profile frontend mode dengan header profile, avatar, statistik, bio, lokasi, `TabLayout`, dan `RecyclerView` grid 3 kolom.
* Menambahkan adapter lokal di `ProfileFragment` untuk menampilkan dummy pets dan dummy content.
* Menambahkan overlay ikon play untuk item dummy content bertipe video.
* Menambahkan resource `placeholder.jpeg` untuk isi grid profile.
* Menambahkan warna aplikasi di `colors.xml`, termasuk `primary_orange`, `highlight_orange`, `app_background`, dan warna pendukung UI.
* Menambahkan dependency Compose/Navigation Compose/Material3 ke konfigurasi Gradle, walaupun UI yang dipakai saat ini masih XML.
* Menambahkan ADR `0002-main-navigation-and-profile-ui.md` untuk keputusan navigasi utama dan profile frontend mode.

### Changed

* `MainActivity` berubah dari halaman sederhana setelah login menjadi host fragment utama dengan `BottomNavigationView`.
* Halaman awal setelah login sekarang memuat `FeedFragment`.
* Layout login, register, dan main dirapikan agar memakai resource warna yang lebih konsisten.
* Theme aplikasi menambahkan `colorSecondaryContainer` untuk penyesuaian warna active indicator Material.
* README dan ADR diperbarui agar mencatat perubahan dari commit Devita: perapihan warna/UI, bottom navigation, fragment utama, dan profile frontend mode.

### Fixed

* Memperbaiki penggunaan warna transparan pada `TabLayout` menjadi `@android:color/transparent`.
* Menyinkronkan adapter profile agar pergantian tab `Pets` dan `Content` memperbarui list melalui `updateList()`.

### Removed

* Menghapus tombol logout dari layout utama setelah `MainActivity` difokuskan sebagai host fragment.

### Known Issues

* `FeedFragment`, `SearchFragment`, dan `AddPostFragment` masih placeholder.
* `ProfileFragment` masih memakai dummy data lokal, belum mengambil data dari Firestore.
* `activity_main.xml` saat ini masih perlu dicek karena ada karakter sisa di luar elemen XML utama.

## [0.2.0-alpha] - 2026-05-21

### Added

* Menambahkan autentikasi Firebase untuk login email/password.
* Menambahkan register akun baru menggunakan Firebase Authentication.
* Menyimpan data user baru ke koleksi `users` di Cloud Firestore.
* Menambahkan login Google menggunakan Google Sign-In dan Firebase credential.
* Menambahkan `LoginActivity` sebagai launcher utama aplikasi.
* Menambahkan `RegisterActivity` untuk pembuatan akun.
* Menambahkan `AuthViewModel` untuk state autentikasi berbasis LiveData.
* Menambahkan `AuthRepository` sebagai pembungkus akses Firebase Auth dan Firestore.
* Menambahkan model `User`.
* Menambahkan layout XML untuk login dan register.
* Menambahkan logo AdoptUs, logo teks, dan ikon Google pada resource drawable.
* Menambahkan permission `INTERNET` di manifest.
* Menambahkan Google Services Gradle Plugin, Firebase BOM, Firebase Auth, Firestore, Google Sign-In, Coroutines, dan Lifecycle dependency.

### Changed

* `LoginActivity` sekarang menjadi entry point aplikasi melalui intent launcher.
* `MainActivity` sekarang memeriksa session login sebelum menampilkan halaman utama.
* Layout `activity_main.xml` berubah dari tampilan `Hello World!` menjadi halaman sederhana setelah login.
* Dokumentasi proyek diperbarui agar mengikuti implementasi autentikasi saat ini.

## [0.1.0-alpha] - 2026-05-21

### Added

* Inisialisasi proyek Android native dengan nama root project `AdoptUs`.
* Penambahan module aplikasi `app`.
* Konfigurasi package dan application ID `com.example.adoptus`.
* Penambahan `MainActivity` sebagai activity awal.
* Penambahan layout XML dasar `activity_main.xml` berbasis `ConstraintLayout`.
* Penambahan resource dasar Android, termasuk `strings.xml`, `colors.xml`, tema aplikasi, launcher icon, backup rules, dan data extraction rules.
* Penambahan Gradle Wrapper dengan Gradle 9.3.1.
* Penambahan version catalog `gradle/libs.versions.toml` untuk dependency dasar.
* Penambahan contoh local unit test dan instrumented test bawaan.
* Penambahan dokumen awal: `README.md`, `CHANGELOG.md`, dan ADR arsitektur awal.

## Rencana Berikutnya

* [ ] Rapikan pesan error login saat email/password salah agar tidak menampilkan pesan teknis Firebase mentah.
* [ ] Pastikan user yang sudah login langsung masuk ke feed saat aplikasi dibuka ulang.
* [ ] Ganti placeholder feed, search, dan add post dengan UI/logic nyata.
* [ ] Hubungkan data profile, pets, dan content ke backend ketika skema data sudah ditentukan.
* [ ] Bersihkan resource/layout yang menghambat build sebelum rilis berikutnya.
