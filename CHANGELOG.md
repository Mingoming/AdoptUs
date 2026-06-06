# Changelog - AdoptUs Mobile Project

Semua perubahan penting pada proyek Android **AdoptUs** didokumentasikan di file ini.
Format mengikuti prinsip [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased] - 2026-06-06

### Changed

* Memperbarui README agar sesuai dengan kode terbaru: splash screen, Navigation Component, Firestore feed, AddPost, Setting, dan known gaps.
* Merapikan CHANGELOG agar memakai teks ASCII yang konsisten dan tidak menampilkan karakter encoding rusak.
* Menambahkan ADR `0005-current-implementation-baseline.md` sebagai baseline status implementasi terbaru.

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
