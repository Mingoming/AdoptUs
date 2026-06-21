# Changelog - AdoptUs Mobile Project

Semua perubahan penting pada proyek Android **AdoptUs** didokumentasikan di file ini.
Format mengikuti prinsip [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added

* Add Post dapat memilih dan mengunggah video MP4 maksimal 20 MB ke Supabase Storage.
* Pet Detail dapat memutar video dengan kontrol Media3 ExoPlayer.
* Menambahkan `SplashActivity` dan `activity_splash.xml` yang mengimplementasikan animasi pembuka aplikasi (Overshoot bounce logo/text pada entry, dan swipe-up + fade-out pada exit) sesuai dengan prototipe HTML.
* Memindahkan logika routing/gate autentikasi (pemeriksaan `isLoggedIn()`) dari `MainActivity` ke `SplashActivity` agar berjalan mulus setelah animasi keluar selesai.
* Mengubah ikon splash screen bawaan (native AndroidX) menjadi transparan untuk menghindari efek logo ganda (*double intro*) saat transisi dari cold start ke animasi kustom.
* Mendesain ulang halaman login dan registrasi agar menggunakan latar belakang putih bersih (`@color/white`), mewarnai hitam teks tagline 'Adopt Love. Change a Life.', memperjelas opasitas/warna dari hint teks input email & password, serta mengubah warna teks tombol login/register dan batas stroke tombol Google menjadi hitam.
* Menambahkan dialog dan fungsi 'Forgot Password' di `LoginActivity` untuk mengirim email reset kata sandi menggunakan Firebase Auth.
* Mengatur agar status bar di `LoginActivity` dan `RegisterActivity` menggunakan latar belakang putih dengan ikon yang kontras (gelap) agar selaras dengan halaman lainnya.
* Melakukan refaktor penuh pada `SearchFragment` untuk memuat postingan aktif (`available`) dan mencari postingan (berdasarkan nama, jenis, atau ras hewan) serta pengguna (berdasarkan username atau nama lengkap) secara dinamis langsung dari Firestore.
* Membuat `SearchPostAdapter` dan `SearchUserAdapter` yang terikat dinamis dengan model data riil (`Post` dan `User`) menggunakan Coil untuk memuat gambar, menggantikan mock `SimpleGridAdapter` dan item dummy statis.
* Mengatur agar klik pada item hasil pencarian langsung mengarahkan pengguna ke `PetDetailFragment` dengan data postingan yang valid.
* Memperbaiki kesalahan kompilasi di `AddPostFragment` dengan menambahkan inisialisasi properti `db` (FirebaseFirestore instance) dan mengimpor pustaka `await`.

### Changed

* Repository upload media mendukung gambar JPEG/PNG/WebP maksimal 5 MB dan video MP4 maksimal 20 MB.
* Feed dan Profile menampilkan placeholder dengan ikon play untuk post video tanpa autoplay.

## [0.6.0-alpha] - 2026-06-21

### Added

* Menambahkan mapper `User.fromMap()` dan `User.toMap()` dengan fallback untuk data Firestore lama agar skema `camelCase` dan `snake_case` tetap kompatibel.
* Mendokumentasikan Firestore rules sederhana yang diterapkan manual untuk ownership profil dan post.
* Menampilkan profil Firestore dan post milik user secara dinamis pada `ProfileFragment` menggunakan `ProfileViewModel` dan `ProfilePostAdapter`.
* Menambahkan halaman Pet Detail berbasis data Firestore (`PetDetailFragment` & `PetDetailViewModel`) dan layout XML yang interaktif (`fragment_petdetail.xml`).
* Menambahkan batas panjang karakter input dan validasi format regex di `RegisterActivity` dan `SettingFragment` untuk menghindari spamming field Firestore (maksimal username 30 karakter, nama lengkap 80 karakter, bio 300 karakter, kota 80 karakter, WhatsApp 30 karakter).
* Menambahkan upload satu foto Add Post ke bucket Supabase Storage melalui REST API.

### Changed

* Memperbarui README agar sesuai dengan kode terbaru: splash screen, Navigation Component, Firestore feed, AddPost, Setting, skema camelCase, profil dinamis, detail dinamis, dan known gaps.
* Merapikan CHANGELOG agar memakai teks ASCII yang konsisten dan tidak menampilkan karakter encoding rusak.
* Menambahkan ADR `0005-current-implementation-baseline.md` sebagai baseline status implementasi terbaru.
* Register email dan Google sekarang menulis schema user camelCase tanpa menyimpan email ke Firestore.
* Setting membaca schema baru maupun field legacy, lalu menyimpan perubahan dalam camelCase.
* Validasi nama lengkap dan username disamakan antara register dan Setting.
* `ProfileFragment` sekarang memakai MVVM sederhana dan memuat ulang profil setelah kembali dari Setting.
* Dokumentasi rules disesuaikan karena rules diterapkan manual melalui Firebase Console.
* Post pada grid Profile sekarang membuka detail yang sama dengan post dari Feed.
* `AddPostFragment` sekarang membuat dokumen Firestore melalui `PostRepository.createPost()`.
* Konfigurasi Supabase dibaca dari `local.properties` dan diekspos melalui `BuildConfig`.

### Fixed

* Menghapus akun Firebase Auth yang baru dibuat jika penyimpanan profil Firestore saat register gagal (atomic rollback).

### Verified


* `:app:compileDebugKotlin --no-daemon` berhasil dengan JDK lokal lengkap.
* `:app:compileDebugJavaWithJavac --no-daemon` berhasil dengan JDK lokal lengkap.
* `:app:testDebugUnitTest --no-daemon` berhasil dengan JDK lokal lengkap.

## [0.5.0-alpha] - 2026-05-31

### Added

* Menambahkan `SettingFragment` untuk edit profil user.
* Menambahkan field setting: nama lengkap, username, bio, kota, nomor WhatsApp, dan password baru.
* Menambahkan load data user dari Firestore saat `SettingFragment` dibuka.
* Menambahkan save profile ke Firestore.
* Menambahkan logout dari `SettingFragment`.
* Menambahkan `fragment_setting.xml` dengan `NestedScrollView`, Material text fields, dan loading overlay.
* Menambahkan `bg_btn_logout.xml`.

### Changed

* `ProfileFragment` menavigasi ke `SettingFragment` melalui icon setting.
* `main_nav.xml` menambahkan destination `settingFragment` dan action `action_profile_to_setting`.

### Fixed

* Menambahkan `fitsSystemWindows` di beberapa layout agar konten tidak tertutup area sistem.
* Memperbesar touch target tombol utama.
* Menambahkan ripple pada tombol utama.

## [0.4.0-alpha] - 2026-05-31

### Added

* Menambahkan `AddPostFragment` yang menyimpan data post teks ke koleksi `posts` Firestore.
* Menambahkan model `Post` dengan `fromMap()` dan `toMap()`.
* Menambahkan `PostRepository` untuk feed real-time, my posts, filter type, create, update status, dan delete post.
* Menambahkan `FeedViewModel` berbasis `StateFlow`.
* Menambahkan `FeedAdapter` dengan dukungan image via Coil dan video via Media3 ExoPlayer.
* Menambahkan `item_feed_post.xml` untuk item feed full-screen.
* Menambahkan resource feed: gradient, status badge, fee badge, apply button, WhatsApp button, avatar circle, dan ikon pendukung.

### Changed

* `FeedFragment` berubah dari placeholder menjadi feed RecyclerView full-screen dengan `PagerSnapHelper`.
* `fragment_feed.xml` memiliki RecyclerView, loading state, empty state, dan error state.
* `PostRepository.getFeedPosts()` mengambil semua post lalu filter `status == "available"` di sisi app untuk menghindari composite index Firestore saat development.

### Fixed

* Mengganti penggunaan `RecyclerView.NO_ID` dengan `RecyclerView.NO_POSITION` untuk menghindari mismatch `Long` vs `Int`.
* Menghapus konflik duplicate resource `placeholder`.

## [0.3.0-alpha] - 2026-05-31

### Added

* Menambahkan Jetpack Navigation Component.
* Menambahkan `main_nav.xml` dengan destination feed, search, add post, profile, setting, dan pet detail.
* Menambahkan animasi transisi slide.
* Menambahkan `PetDetailFragment` dan argumen `postId` sebagai placeholder detail hewan.
* Menambahkan dependency `navigation-fragment-ktx`, `navigation-ui-ktx`, Coil, dan Media3 ExoPlayer.

### Changed

* `MainActivity` memakai `NavController` dan `setupWithNavController()`.
* `activity_main.xml` memakai `FragmentContainerView` sebagai `NavHostFragment`.
* ID menu di `navbar.xml` disamakan dengan ID destination navigation graph.
* Back button di `AddPostFragment` memakai `findNavController().navigateUp()`.

### Fixed

* Memperbaiki Gradle Sync error karena section TOML invalid.
* Memperbaiki bottom navigation yang tidak bisa dipencet karena ID menu tidak cocok dengan destination.
* Memperbaiki back navigation AddPost agar tidak langsung keluar app.

## [0.2.1-alpha] - 2026-05-31

### Added

* Menambahkan username pada flow register.

### Changed

* Login, register, dan main activity mulai memakai ViewBinding.
* User yang sudah login langsung diarahkan ke feed saat aplikasi dibuka.

### Fixed

* Pesan raw Firebase error di login diganti dengan pesan yang lebih bersih.
* Error register ditampilkan pada field terkait, bukan hanya Toast.

## [0.2.0-alpha] - 2026-05-21

### Added

* Menambahkan Firebase Authentication untuk email/password.
* Menambahkan Google Sign-In.
* Menambahkan register user.
* Menyimpan data user awal ke koleksi `users` Firestore.
* Menambahkan `LoginActivity`, `RegisterActivity`, `AuthViewModel`, `AuthRepository`, dan model `User`.
* Menambahkan permission `INTERNET`.

## [0.1.0-alpha] - 2026-05-21

### Added

* Inisialisasi proyek Android native Kotlin.
* Menambahkan module `app`.
* Menambahkan `MainActivity`, layout dasar, resource dasar, Gradle Wrapper 9.3.1, version catalog, unit test, dan instrumented test.
* Menambahkan README, CHANGELOG, dan ADR arsitektur awal.
