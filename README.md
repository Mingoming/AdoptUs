# AdoptUs Mobile App

AdoptUs adalah proyek Android native berbasis Kotlin untuk Tugas Besar Pemrograman Mobile. Kondisi proyek saat ini sudah memiliki fondasi autentikasi pengguna dengan Firebase: login email/password, register akun, login Google, penyimpanan data user ke Firestore, dan halaman utama sederhana setelah pengguna berhasil masuk.

Dokumen ini hanya menjelaskan implementasi yang sudah ada di repository saat ini. Fitur domain adopsi hewan seperti daftar hewan, feed, status adopsi, dan komunikasi WhatsApp belum diimplementasikan.

## Status Proyek Saat Ini

* Aplikasi Android native dengan satu module: `app`.
* Package dan application ID: `com.example.adoptus`.
* `LoginActivity` menjadi launcher utama.
* `RegisterActivity` menangani pembuatan akun baru.
* `MainActivity` hanya bisa diakses saat user sudah login dan menampilkan email user serta tombol logout.
* Autentikasi menggunakan Firebase Authentication untuk email/password dan Google Sign-In.
* Data user baru disimpan ke koleksi `users` di Cloud Firestore.
* UI masih menggunakan XML layout dengan Material Components.
* Belum ada Navigation Component, Jetpack Compose, Media3, video feed, atau fitur adopsi hewan.

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
  * Material Components
  * AndroidX Activity
  * ConstraintLayout
  * Firebase Authentication
  * Cloud Firestore
  * Google Services Gradle Plugin
  * Google Sign-In
  * Kotlin Coroutines Android
  * Kotlin Coroutines Play Services
  * AndroidX Lifecycle ViewModel dan LiveData
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
|       |   |   `-- ui/auth/
|       |   |       |-- AuthViewModel.kt
|       |   |       |-- LoginActivity.kt
|       |   |       `-- RegisterActivity.kt
|       |   `-- res/
|       |       |-- drawable/
|       |       |-- layout/activity_login.xml
|       |       |-- layout/activity_register.xml
|       |       |-- layout/activity_main.xml
|       |       |-- values/
|       |       `-- xml/
|       |-- test/java/com/example/adoptus/ExampleUnitTest.kt
|       `-- androidTest/java/com/example/adoptus/ExampleInstrumentedTest.kt
|-- gradle/libs.versions.toml
|-- build.gradle.kts
|-- settings.gradle.kts
|-- CHANGELOG.md
`-- doc/adr/0001-architecture-decision.md
```

## Alur Aplikasi

1. Aplikasi membuka `LoginActivity` sebagai launcher.
2. User bisa login menggunakan email/password atau Google Sign-In.
3. User yang belum punya akun bisa membuka `RegisterActivity`.
4. Register membuat akun Firebase Auth dan menyimpan data awal user ke Firestore.
5. Setelah login/register berhasil, aplikasi membuka `MainActivity`.
6. `MainActivity` menampilkan email user yang sedang login dan menyediakan tombol logout.

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

Fitur yang sudah masuk baru berada di area autentikasi. Struktur saat ini mulai memakai pemisahan sederhana antara UI auth, `AuthViewModel`, dan `AuthRepository`, tetapi belum menjadi arsitektur aplikasi lengkap untuk semua fitur AdoptUs.

Dokumentasi ini perlu diperbarui setiap kali fitur nyata seperti data hewan, dashboard adopsi, atau navigasi utama ditambahkan.

## Lisensi

Proyek ini dikembangkan sebagai Tugas Besar mata kuliah Pemrograman Mobile. Hak cipta milik Tim Pengembang AdoptUs (C) 2026.
