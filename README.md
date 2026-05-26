# AdoptUs Mobile App

AdoptUs adalah proyek Android native berbasis Kotlin untuk Tugas Besar Pemrograman Mobile. Kondisi proyek saat ini sudah memiliki fondasi autentikasi Firebase, halaman utama berbasis fragment, bottom navigation, dan halaman profile frontend dengan data dummy lokal.

Dokumen ini hanya menjelaskan implementasi yang sudah ada di repository saat ini. Fitur domain adopsi hewan yang terhubung ke backend, upload post, pencarian hewan, feed video, dan komunikasi WhatsApp belum diimplementasikan penuh.

## Status Proyek Saat Ini

* Aplikasi Android native dengan satu module: `app`.
* Package dan application ID: `com.example.adoptus`.
* `LoginActivity` menjadi launcher utama.
* `RegisterActivity` menangani pembuatan akun baru.
* `MainActivity` memeriksa session login, lalu menjadi host fragment utama.
* Autentikasi menggunakan Firebase Authentication untuk email/password dan Google Sign-In.
* Data user baru disimpan ke koleksi `users` di Cloud Firestore.
* UI menggunakan XML layout dengan Material Components.
* Halaman utama menggunakan `FrameLayout` sebagai `fragment_container` dan `BottomNavigationView` sebagai navigasi bawah.
* Fragment utama yang tersedia: `FeedFragment`, `SearchFragment`, `AddPostFragment`, dan `ProfileFragment`.
* `FeedFragment`, `SearchFragment`, dan `AddPostFragment` masih berupa halaman placeholder.
* `ProfileFragment` sudah memiliki layout profile, tab `Pets` dan `Content`, serta grid lokal 3 kolom dengan dummy data.
* Belum ada Navigation Component berbasis graph, Jetpack Compose UI, Media3, feed video nyata, upload post, atau data hewan dari backend.

## Tech Stack

* **Bahasa:** Kotlin
* **Build system:** Gradle Kotlin DSL
* **Gradle Wrapper:** Gradle 9.3.1
* **Android Gradle Plugin:** 9.1.1
* **Compile SDK:** 36
* **Target SDK:** 36
* **Min SDK:** 24
* **Java compatibility:** Java 11
* **Dependency utama:**
  * AndroidX Core KTX
  * AndroidX AppCompat
  * AndroidX Activity
  * ConstraintLayout
  * Material Components
  * Firebase Authentication
  * Cloud Firestore
  * Google Services Gradle Plugin
  * Google Sign-In
  * Kotlin Coroutines Android
  * Kotlin Coroutines Play Services
  * AndroidX Lifecycle ViewModel dan LiveData
  * AndroidX Navigation Compose, Compose Material Icons Extended, dan Compose Material3 sudah tercatat sebagai dependency, tetapi UI aplikasi saat ini masih XML.
  * JUnit, AndroidX Test JUnit, dan Espresso

## Struktur Proyek

```text
.
|-- app/
|   |-- build.gradle.kts
|   |-- google-services.json
|   |-- proguard-rules.pro
|   `-- src/
|       |-- main/
|       |   |-- AndroidManifest.xml
|       |   |-- java/com/example/adoptus/
|       |   |   |-- MainActivity.kt
|       |   |   |-- data/
|       |   |   |   |-- model/User.kt
|       |   |   |   `-- repository/AuthRepository.kt
|       |   |   |-- fragment/
|       |   |   |   |-- AddPostFragment.kt
|       |   |   |   |-- FeedFragment.kt
|       |   |   |   |-- ProfileFragment.kt
|       |   |   |   `-- SearchFragment.kt
|       |   |   `-- ui/auth/
|       |   |       |-- AuthViewModel.kt
|       |   |       |-- LoginActivity.kt
|       |   |       `-- RegisterActivity.kt
|       |   `-- res/
|       |       |-- color/nav_item_color.xml
|       |       |-- drawable/
|       |       |-- layout/
|       |       |   |-- activity_login.xml
|       |       |   |-- activity_register.xml
|       |       |   |-- activity_main.xml
|       |       |   |-- fragment_add_post.xml
|       |       |   |-- fragment_feed.xml
|       |       |   |-- fragment_profile.xml
|       |       |   |-- fragment_search.xml
|       |       |   `-- profile_item_pet.xml
|       |       |-- menu/navbar.xml
|       |       |-- values/
|       |       `-- xml/
|       |-- test/java/com/example/adoptus/ExampleUnitTest.kt
|       `-- androidTest/java/com/example/adoptus/ExampleInstrumentedTest.kt
|-- doc/adr/
|   |-- 0001-architecture-decision.md
|   `-- 0002-main-navigation-and-profile-ui.md
|-- gradle/libs.versions.toml
|-- build.gradle.kts
|-- settings.gradle.kts
`-- CHANGELOG.md
```

## Alur Aplikasi

1. Aplikasi membuka `LoginActivity` sebagai launcher.
2. User bisa login menggunakan email/password atau Google Sign-In.
3. User yang belum punya akun bisa membuka `RegisterActivity`.
4. Register membuat akun Firebase Auth dan menyimpan data awal user ke Firestore.
5. Setelah login/register berhasil, aplikasi membuka `MainActivity`.
6. `MainActivity` memuat `FeedFragment` sebagai halaman awal jika user sudah login.
7. `BottomNavigationView` mengganti isi `fragment_container` ke `FeedFragment`, `SearchFragment`, `AddPostFragment`, atau `ProfileFragment`.
8. `ProfileFragment` menampilkan data profile frontend mode dengan dummy pets dan dummy content lokal.
9. Tab `Pets` dan `Content` mengganti data grid melalui adapter lokal.
10. Item content dengan `isVideo = true` menampilkan overlay ikon play pada kartu grid.

## Cara Menjalankan

### Prasyarat

* Android Studio yang mendukung Android Gradle Plugin 9.1.1.
* JDK yang sesuai dengan Gradle toolchain proyek.
* Android SDK dengan compile SDK 36.
* Project Firebase yang sesuai dengan file `app/google-services.json`.
* Firebase Authentication aktif untuk metode email/password dan Google.
* Cloud Firestore aktif untuk penyimpanan dokumen user.

### Buka di Android Studio

1. Buka Android Studio.
2. Pilih **File > Open**.
3. Arahkan ke folder repository ini.
4. Tunggu proses Gradle Sync selesai.
5. Jalankan konfigurasi `app` pada emulator atau perangkat Android.

### Jalankan Test

Windows:

```powershell
.\gradlew.bat test
```

macOS/Linux:

```bash
./gradlew test
```

## Catatan Pengembangan

Area autentikasi sudah memakai pemisahan sederhana antara UI auth, `AuthViewModel`, dan `AuthRepository`. Area halaman utama memakai fragment manual via `supportFragmentManager`, belum memakai Navigation Component.

Halaman profile masih frontend mode: data pet dan content masih dummy lokal di `ProfileFragment`, belum berasal dari Firestore atau API. `FeedFragment`, `SearchFragment`, dan `AddPostFragment` juga masih placeholder.

Dokumentasi ini perlu diperbarui setiap kali fitur nyata seperti data hewan, upload post, pencarian, dashboard adopsi, atau navigasi utama berbasis graph ditambahkan.

## Lisensi

Proyek ini dikembangkan sebagai Tugas Besar mata kuliah Pemrograman Mobile. Hak cipta milik Tim Pengembang AdoptUs (C) 2026.
