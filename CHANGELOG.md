# Changelog - AdoptUs Mobile Project

Semua perubahan penting pada proyek Android **AdoptUs** didokumentasikan di file ini.

Format changelog ini mengikuti prinsip [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

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
* `MainActivity` sekarang memeriksa session login, menampilkan email user, dan menyediakan logout.
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

* [ ] Menyelesaikan validasi dan pesan error pada flow login/register.
* [ ] Memastikan konfigurasi Google Sign-In sesuai Firebase project yang digunakan.
* [ ] Menentukan fitur domain AdoptUs berikutnya setelah autentikasi, misalnya data profil atau daftar hewan.
* [ ] Menambahkan navigasi utama setelah login jika aplikasi mulai memiliki lebih dari satu halaman fitur.
* [ ] Memperbarui dokumentasi setiap kali dependency, arsitektur, atau fitur baru masuk ke proyek.
