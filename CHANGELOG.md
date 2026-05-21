# Changelog - AdoptUs Mobile Project

Semua perubahan penting pada proyek Android **AdoptUs** didokumentasikan di file ini.

Format changelog ini mengikuti prinsip [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [0.1.0-alpha] - 2026-05-21

### Added

* Inisialisasi proyek Android native dengan nama root project `AdoptUs`.
* Penambahan module aplikasi `app`.
* Konfigurasi package dan application ID `com.example.adoptus`.
* Penambahan `MainActivity` sebagai activity launcher utama.
* Penambahan layout XML dasar `activity_main.xml` berbasis `ConstraintLayout` dengan teks `Hello World!`.
* Penambahan resource dasar Android, termasuk `strings.xml`, `colors.xml`, tema aplikasi, launcher icon, backup rules, dan data extraction rules.
* Penambahan Gradle Wrapper dengan Gradle 9.3.1.
* Penambahan version catalog `gradle/libs.versions.toml` untuk dependency AndroidX, Material, JUnit, dan Espresso.
* Penambahan contoh local unit test dan instrumented test bawaan.
* Penambahan dokumen awal: `README.md`, `CHANGELOG.md`, dan ADR arsitektur awal.

### Verified

* `.\gradlew.bat test` berhasil dijalankan dan menghasilkan `BUILD SUCCESSFUL`.

## Rencana Berikutnya

* [ ] Menentukan fitur pertama AdoptUs yang benar-benar akan diimplementasikan di aplikasi Android.
* [ ] Mengganti UI awal `Hello World!` dengan layar awal yang sesuai kebutuhan fitur pertama.
* [ ] Menentukan struktur package dan arsitektur aplikasi setelah kebutuhan fitur pertama jelas.
* [ ] Menambahkan resource visual dan warna aplikasi hanya setelah desain yang akan dipakai sudah ditentukan.
* [ ] Memperbarui dokumentasi setiap kali dependency, arsitektur, atau fitur baru benar-benar masuk ke proyek.
