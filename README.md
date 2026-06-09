# AdoptUs Mobile App

AdoptUs adalah aplikasi Android native berbasis Kotlin untuk ekosistem adopsi hewan. Proyek ini dikembangkan sebagai Tugas Besar Pemrograman Mobile Universitas Mataram 2026.

Status kode terbaru sudah melewati starter app: aplikasi memiliki autentikasi Firebase, splash screen, navigasi utama dengan Jetpack Navigation, feed real-time dari Firestore, form add post berbasis Firestore, dan halaman setting untuk profil user.

## Status Proyek Saat Ini

### Sudah Selesai

* Autentikasi email/password dan Google Sign-In menggunakan Firebase Authentication.
* Session gate di `MainActivity`: user yang belum login diarahkan ke `LoginActivity`, user yang sudah login masuk ke Feed.
* Register dengan field nama lengkap, username, email, dan password.
* Pesan error login/register sudah dipetakan agar tidak menampilkan raw Firebase error.
* Splash screen menggunakan AndroidX Core SplashScreen.
* Jetpack Navigation Component untuk navigasi utama.
* `BottomNavigationView` terhubung ke `NavController` melalui `setupWithNavController()`.
* Feed vertikal full-screen dari koleksi `posts` Firestore.
* `FeedViewModel` memakai `StateFlow`; `PostRepository` memakai `callbackFlow` untuk update real-time.
* `FeedAdapter` mendukung media image via Coil dan video via Media3 ExoPlayer.
* `AddPostFragment` menyimpan data teks hewan ke Firestore.
* `SettingFragment` memuat dan menyimpan data profil user ke Firestore.
* Logout dari `SettingFragment` membersihkan session Firebase dan kembali ke `LoginActivity`.
* `ProfileFragment` punya UI profile dan tombol setting.
* `PetDetailFragment` tersedia sebagai destination Navigation, tetapi masih placeholder.

### Belum Selesai

* Upload foto/video ke Firebase Storage belum aktif.
* `mediaUrl` masih kosong saat Add Post.
* Kota di Add Post masih hardcode `"Indonesia"`.
* `SearchFragment` masih template/placeholder.
* `ProfileFragment` masih memakai dummy data lokal untuk grid.
* `PetDetailFragment` masih placeholder.
* Tombol like belum menyimpan state ke Firestore.
* Alur adopsi lengkap seperti apply, approve, reject, dan koleksi `adoptions` belum tersedia.

### Catatan Teknis

* `btnBack` di `AddPostFragment` masih bertipe `ImageView`; sebaiknya diganti `ImageButton` untuk aksesibilitas.
* Field user Firestore belum sepenuhnya konsisten:
  * Register menyimpan `full_name`, `photo_url`, dan `created_at`.
  * Setting membaca/menulis `fullName`, `bio`, `city`, dan `whatsapp`.
  * Perlu normalisasi schema sebelum fitur profil dipakai lebih jauh.
* Root `build.gradle.kts` lokal di working tree saat ini memiliki perubahan yang menambahkan plugin Kotlin JVM di root project. Itu bukan pola utama proyek Android ini dan sebaiknya tidak di-commit sebelum dipastikan perlu.

## Tech Stack

| Komponen | Teknologi |
|---|---|
| Bahasa | Kotlin |
| Build System | Gradle Kotlin DSL + Version Catalog |
| Min SDK | 24 |
| Target/Compile SDK | 36 |
| UI | XML Layout + Material Components |
| Navigasi | Jetpack Navigation Component 2.7.7 |
| Auth | Firebase Authentication |
| Database | Cloud Firestore |
| Media Upload | Pending Firebase Storage |
| Image Loading | Coil 2.6.0 |
| Video Player | Media3 ExoPlayer 1.3.1 |
| Async | Kotlin Coroutines, Flow, StateFlow |
| Architecture | MVVM sederhana: ViewModel + Repository |

## Struktur Proyek

```text
app/src/main/
|-- java/com/example/adoptus/
|   |-- MainActivity.kt
|   |-- data/
|   |   |-- model/
|   |   |   |-- User.kt
|   |   |   `-- Post.kt
|   |   `-- repository/
|   |       |-- AuthRepository.kt
|   |       `-- PostRepository.kt
|   |-- fragment/
|   |   |-- FeedFragment.kt
|   |   |-- SearchFragment.kt
|   |   |-- AddPostFragment.kt
|   |   |-- ProfileFragment.kt
|   |   |-- SettingFragment.kt
|   |   |-- Petdetailfragment.kt
|   |   `-- Petdetailfragmentargs.kt
|   `-- ui/
|       |-- auth/
|       |   |-- AuthViewModel.kt
|       |   |-- LoginActivity.kt
|       |   `-- RegisterActivity.kt
|       `-- feed/
|           |-- FeedViewModel.kt
|           `-- Feedadapter.kt
`-- res/
    |-- anim/
    |-- color/
    |-- drawable/
    |-- layout/
    |-- menu/navbar.xml
    |-- navigation/main_nav.xml
    |-- values/
    `-- xml/
```

## Firestore Schema Saat Ini

### Koleksi `users`

| Field | Tipe | Keterangan |
|---|---|---|
| id | String | UID Firebase Auth |
| username | String | Username user |
| email | String | Email Firebase Auth |
| full_name | String | Nama lengkap dari register |
| photo_url | String | URL foto profil, saat ini bisa kosong |
| role | String | Default `user` |
| created_at | Timestamp | Waktu register |
| fullName | String | Ditulis oleh SettingFragment saat update profil |
| bio | String | Ditulis oleh SettingFragment |
| city | String | Ditulis oleh SettingFragment |
| whatsapp | String | Ditulis oleh SettingFragment |

### Koleksi `posts`

| Field | Tipe | Keterangan |
|---|---|---|
| postId | String | ID dokumen post |
| userId | String | UID owner post |
| petName | String | Nama hewan |
| petType | String | Jenis hewan |
| breed | String | Ras hewan |
| age | Number | Angka usia |
| ageUnit | String | `Months` atau `Years` |
| city | String | Kota lokasi hewan |
| description | String | Deskripsi |
| mediaUrl | String | Kosong sampai Storage aktif |
| mediaType | String | `image` atau `video` |
| isVaccinated | Boolean | Status vaksin |
| hasHealthPassport | Boolean | Status buku kesehatan |
| adoptionFee | Number | `0` berarti gratis |
| status | String | `available`, `pending`, atau `adopted` |
| likesCount | Number | Jumlah like |
| createdAt | Timestamp | Waktu upload |

## Alur Navigasi

```text
App dibuka
`-- MainActivity + SplashScreen
    |-- Belum login -> LoginActivity
    |   |-- Login berhasil -> MainActivity
    |   `-- RegisterActivity -> MainActivity
    `-- Sudah login -> NavHost feedFragment
        |-- FeedFragment
        |   `-- PetDetailFragment
        |-- SearchFragment
        |-- AddPostFragment
        |-- ProfileFragment
        |   |-- SettingFragment
        |   `-- PetDetailFragment
        `-- SettingFragment
```

Bottom navigation disembunyikan saat masuk ke `AddPostFragment` dan `PetDetailFragment`.

## Cara Menjalankan

### Prasyarat

* Android Studio yang mendukung Android Gradle Plugin 9.1.1.
* Android SDK dengan compile SDK 36.
* JDK lengkap yang memiliki `jlink.exe`.
* File `app/google-services.json` untuk Firebase project.
* Firebase Authentication aktif untuk email/password dan Google.
* Cloud Firestore aktif.

### Jalankan dari Android Studio

1. Buka Android Studio.
2. Pilih **File > Open** dan arahkan ke folder repository.
3. Tunggu Gradle Sync selesai.
4. Jalankan konfigurasi `app` pada emulator atau perangkat Android.

### Jalankan Unit Test

Jika Gradle memakai JRE VS Code yang tidak punya `jlink.exe`, jalankan dengan JDK lokal lengkap:

```powershell
$env:JAVA_HOME='C:\Amanta\java\Install'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:testDebugUnitTest --no-daemon
```

## Dokumentasi Arsitektur

ADR tersedia di `doc/adr/`:

* `0001-architecture-decision.md`: arsitektur awal dan autentikasi.
* `0002-main-navigation-and-profile-ui.md`: bottom navigation dan profile frontend mode.
* `0003-navigation-component-migration.md`: migrasi ke Jetpack Navigation Component.
* `0004-feed-firestore-setting.md`: Firestore feed, post model, AddPost, dan Setting.
* `0005-current-implementation-baseline.md`: baseline implementasi terbaru dan gap teknis.

## Lisensi

Proyek ini dikembangkan sebagai Tugas Besar mata kuliah Pemrograman Mobile. Hak cipta milik Tim Pengembang AdoptUs (C) 2026.
