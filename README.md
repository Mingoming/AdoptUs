# AdoptUs Mobile App

AdoptUs adalah proyek Android native berbasis Kotlin untuk kebutuhan Tugas Besar Pemrograman Mobile. Kondisi proyek saat ini masih berupa starter app Android sederhana: satu `MainActivity`, satu layout XML dengan teks `Hello World!`, resource dasar, dan contoh test bawaan.

Dokumen ini hanya menjelaskan implementasi yang sudah ada di repository saat ini. Fitur domain seperti adopsi hewan, autentikasi, database, video feed, atau komunikasi WhatsApp belum diimplementasikan.

## Status Proyek Saat Ini

* Aplikasi Android native dengan satu module: `app`.
* Package aplikasi: `com.example.adoptus`.
* Entry point aplikasi: `MainActivity`.
* UI menggunakan XML layout dengan `ConstraintLayout`.
* Tema dasar menggunakan Material 3 DayNight NoActionBar.
* Belum ada integrasi backend, Firebase, Navigation Component, Media3, Jetpack Compose, atau arsitektur MVVM modular.

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
  * JUnit
  * AndroidX Test JUnit
  * Espresso

## Struktur Proyek

```text
.
|-- app/
|   |-- build.gradle.kts
|   |-- proguard-rules.pro
|   `-- src/
|       |-- main/
|       |   |-- AndroidManifest.xml
|       |   |-- java/com/example/adoptus/MainActivity.kt
|       |   `-- res/
|       |       |-- layout/activity_main.xml
|       |       |-- values/colors.xml
|       |       |-- values/strings.xml
|       |       |-- values/themes.xml
|       |       |-- values-night/themes.xml
|       |       |-- drawable/
|       |       |-- mipmap-*/
|       |       `-- xml/
|       |-- test/java/com/example/adoptus/ExampleUnitTest.kt
|       `-- androidTest/java/com/example/adoptus/ExampleInstrumentedTest.kt
|-- gradle/libs.versions.toml
|-- gradle/wrapper/gradle-wrapper.properties
|-- build.gradle.kts
|-- settings.gradle.kts
|-- gradle.properties
|-- CHANGELOG.md
`-- doc/adr/0001-architecture-decision.md
```

## Cara Menjalankan

### Prasyarat

* Android Studio yang mendukung Android Gradle Plugin 9.1.1.
* JDK yang sesuai dengan Gradle toolchain proyek.
* Android SDK dengan compile SDK 36.

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

Karena proyek masih berada pada tahap starter app, keputusan fitur dan arsitektur lanjutan sebaiknya dibuat setelah kebutuhan pertama AdoptUs ditentukan. Dokumentasi ini perlu diperbarui setiap kali fitur nyata ditambahkan ke aplikasi.

## Lisensi

Proyek ini dikembangkan sebagai Tugas Besar mata kuliah Pemrograman Mobile. Hak cipta milik Tim Pengembang AdoptUs (C) 2026.
