# Architecture Decision Record: 0001 - Arsitektur Awal Proyek Android AdoptUs

* **Status:** Approved
* **Tanggal:** 21 Mei 2026
* **Penulis:** Tim Pengembang AdoptUs

## 1. Konteks

Proyek AdoptUs saat ini berada pada tahap awal sebagai aplikasi Android native untuk Tugas Besar Pemrograman Mobile. Repository sudah berisi struktur proyek Android standar, tetapi belum memiliki fitur domain adopsi hewan.

Implementasi yang sudah ada saat ini:

* Satu module Android bernama `app`.
* Package dan application ID `com.example.adoptus`.
* Satu activity utama, yaitu `MainActivity`.
* Satu layout XML, yaitu `activity_main.xml`, dengan tampilan `Hello World!`.
* Resource dasar Android seperti string, warna, tema, launcher icon, backup rules, dan data extraction rules.
* Dependency dasar AndroidX, Material Components, ConstraintLayout, JUnit, dan Espresso.

Karena fitur utama belum dibuat, keputusan arsitektur perlu dibatasi pada pondasi proyek yang benar-benar sudah ada.

## 2. Keputusan

Kami menggunakan arsitektur awal Android native sederhana dengan keputusan berikut:

1. **Android native Kotlin**
   * Proyek dibuat sebagai aplikasi Android native dengan Kotlin.
   * Build menggunakan Gradle Kotlin DSL.

2. **Single activity starter**
   * `MainActivity` menjadi activity launcher utama.
   * Belum ada fragment, Navigation Component, atau multi-screen flow.

3. **XML layout**
   * UI awal menggunakan XML layout.
   * `activity_main.xml` menggunakan `ConstraintLayout`.
   * Jetpack Compose belum digunakan.

4. **Gradle version catalog**
   * Dependency dikelola melalui `gradle/libs.versions.toml`.
   * Konfigurasi module aplikasi berada di `app/build.gradle.kts`.

5. **Dependency dasar**
   * Dependency saat ini dibatasi pada AndroidX Core KTX, AppCompat, Material Components, AndroidX Activity, ConstraintLayout, JUnit, AndroidX Test JUnit, dan Espresso.
   * Firebase, Media3, Navigation Component, dan dependency fitur lain belum ditambahkan.

## 3. Konsekuensi

### Positif

* Struktur proyek masih sederhana dan mudah dipahami.
* Pondasi Android native sudah siap untuk dikembangkan bertahap.
* Dependency masih minimal sehingga risiko konfigurasi awal lebih rendah.
* Dokumentasi lebih mudah dijaga karena hanya mencatat kondisi yang benar-benar ada.

### Negatif

* Aplikasi belum memiliki fitur domain AdoptUs.
* UI masih berupa tampilan default `Hello World!`.
* Arsitektur MVVM, repository, backend, navigasi, dan penyimpanan data belum tersedia.
* Keputusan fitur seperti Firebase, video feed, atau komunikasi WhatsApp belum bisa dianggap sebagai bagian dari implementasi saat ini.

## 4. Catatan Lanjutan

ADR baru perlu dibuat atau ADR ini perlu diperbarui ketika proyek mulai menambahkan fitur nyata, dependency besar, atau struktur arsitektur baru seperti MVVM, Navigation Component, database lokal, atau integrasi backend.
