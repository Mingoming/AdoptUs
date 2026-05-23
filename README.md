# AdoptUs Mobile App

AdoptUs adalah proyek Android native berbasis Kotlin untuk Tugas Besar Pemrograman Mobile. Kondisi proyek saat ini sudah memiliki fondasi autentikasi pengguna dengan Firebase: login email/password, register akun, login Google, penyimpanan data user ke Firestore, dan halaman utama sederhana setelah pengguna berhasil masuk.

Dokumen ini hanya menjelaskan implementasi yang sudah ada di repository saat ini. Fitur domain adopsi hewan seperti daftar hewan, feed, status adopsi, dan komunikasi WhatsApp belum diimplementasikan.

## Status Proyek Saat Ini

* Aplikasi Android native dengan satu module: `app`.
* Package dan application ID: `com.example.adoptus`.
* `LoginActivity` menjadi launcher utama.
* `RegisterActivity` menangani pembuatan akun baru.
* `MainActivity` berfungsi sebagai Single Activity Architecture Host yang mengelola navigasi halaman utama menggunakan FrameLayout (fragment_container) tanpa efek layar berkedip (screen blinking).
* Autentikasi menggunakan Firebase Authentication untuk email/password dan Google Sign-In.
* Data user baru disimpan ke koleksi `users` di Cloud Firestore.
* UI menggunakan XML layout dengan Material Components (Material 3).
* Belum ada Navigation Component, Jetpack Compose, Media3, video feed, atau fitur adopsi hewan.
* `Floating Bottom Navigation Bar:` Mengimplementasikan menu navigasi melayang bergaya light theme dengan 4 menu fungsional (Feed, Search, Add Post, Profile).
* Struktur Halaman Modular: Pemisahan halaman utama ke dalam package folder khusus `fragment` untuk memisahkan kode secara bersih.
* `UI Profile Halaman Terintegrasi (Frontend Mode)`:  Halaman profil menampilkan nama akun/username dinamis yang dipotong otomatis dari format email Firebase Auth.
  * Layout detail profil menggunakan `ConstraintLayout` dengan penataan baris statistik (Pets Listed & Successful Adoptions) yang presisi di sebelah kanan avatar, serta teks angka yang rata tengah (horizontal gravity). 
  * Implementasi komponen `TabLayout` dengan dua tab interaktif: `Pets` dan `Content`. 
  * Sistem peralihan data responsif menggunakan addOnTabSelectedListener untuk menukar isi list data di dalam ProfileGridAdapter secara instan saat tab diketuk. 
  * Tampilan galeri menggunakan `RecyclerView` berbentuk Grid 3 Kolom full-bleed photo. 
  * Implementasi deteksi tipe konten video `(isVideo = true)` khusus pada tab `Content` untuk menampilkan overlay ikon tombol Play (▶️) tepat di tengah gambar thumbnail.

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
  * Material Components (Diperbarui untuk mendukung penuh penyesuaian kontainer komponen Material 3)

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
|       |   |   |-- fragment/                     <-- Folder paket baru khusus fragment
|       |   |   |   |-- AddPostFragment.kt
|       |   |   |   |-- FeedFragment.kt
|       |   |   |   |-- ProfileFragment.kt        <-- Logika nama akun, TabLayout, & Adapter Grid
|       |   |   |   `-- SearchFragment.kt
|       |   |   `-- ui/auth/
|       |   |       |-- AuthViewModel.kt
|       |   |       |-- LoginActivity.kt
|       |   |       `-- RegisterActivity.kt
|       |   `-- res|       |   `-- res/
|       |       |-- color/
|       |       |   `-- nav_item_color.xml    <-- Selector warna state ikon/teks navbar
|       |       |-- drawable/
|       |       |   `-- bg_bottom_nav.xml     <-- Bentuk background melengkung & melayang navbar
|       |       |-- layout/
|       |       |   |-- activity_login.xml
|       |       |   |-- activity_register.xml
|       |       |   |-- activity_main.xml       <-- Struktur induk navbar & fragment container
|       |       |   |-- fragment_profile.xml    <-- Layout detail profil, tab, & RecyclerView
|       |       |   `-- profile_item_pet.xml    <-- Rangka kartu grid foto penuh + overlay play video
|       |       |-- menu/
|       |       |   `-- navbar.xml            <-- Pengaturan 4 item menu resmi bawaan Google
|       |       |-- values/
|       |       |   `-- themes.xml            <-- Kustomisasi warna dasar global aktif indicator pill|       |       `-- xml/
|       |-- test/java/com/example/adoptus/ExampleUnitTest.kt
|       `-- androidTest/java/com/example/adoptus/ExampleInstrumentedTest.kt
|-- gradle/libs.versions.toml
|-- build.gradle.kts
|-- settings.gradle.kts
|-- CHANGELOG.md
`-- doc/adr/0001-architecture-decision.md
```

## Alur Aplikasi

1. Aplikasi membuka LoginActivity sebagai launcher. 
2. User bisa login menggunakan email/password atau Google Sign-In. 
3. User yang belum punya akun bisa membuka RegisterActivity. 
4. Register membuat akun Firebase Auth dan menyimpan data awal user ke Firestore. 
5. Setelah login/register berhasil, aplikasi membuka MainActivity. 
6. MainActivity bertindak sebagai pengontrol navigasi utama, memuat BottomNavigationView, dan langsung menampilkan FeedFragment secara otomatis sebagai halaman pembuka. 
7. Ketika user mengetuk menu lain pada navbar, setOnItemSelectedListener akan memicu penukaran komponen fragment di dalam FrameLayout secara mulus. 
8. Pada halaman ProfileFragment, aplikasi memotong string email pengguna untuk dijadikan nama akun tiruan berformat bersih. 
9. Ketika user berpindah tab antara "Pets" dan "Content" di halaman Profil, aplikasi mendengarkan perubahan lewat addOnTabSelectedListener, lalu memperbarui list data di dalam adapter menggunakan fungsi updateList() secara instan. 
10. Jika item yang dimuat pada tab terdeteksi sebagai jenis video, kartu grid secara dinamis memunculkan overlay shadow beserta ikon tombol Play (▶️) di tengah gambar.

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

Untuk mempercepat pengerjaan visual di sisi Frontend, data pada halaman profil (seperti daftar peliharaan dan galeri konten edukasi video) sengaja menggunakan skema Mock Data (data tiruan lokal) yang terisolasi di dalam Fragment, sehingga pengembangan UI terhindar dari ketergantungan koneksi database langsung dan pengerjaan tata letak bisa berjalan jauh lebih rapi.

Dokumentasi ini perlu diperbarui setiap kali fitur nyata seperti data hewan, dashboard adopsi, atau navigasi utama ditambahkan.

## Lisensi

Proyek ini dikembangkan sebagai Tugas Besar mata kuliah Pemrograman Mobile. Hak cipta milik Tim Pengembang AdoptUs (C) 2026.
