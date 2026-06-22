# ADR 0012 - Navigasi Detail Hewan Berbasis Deep Link

* **Status:** Approved
* **Tanggal:** 23 Juni 2026
* **Penulis:** Tim Pengembang AdoptUs

## 1. Konteks

Sebagai platform adopsi sosial, pengguna seringkali membagikan postingan hewan kesayangan ke media luar (seperti media sosial, WhatsApp, atau email). Agar pendaratan pengguna dari luar aplikasi terasa mulus, aplikasi AdoptUs harus mendukung navigasi langsung ke halaman detail postingan hewan (`PetDetailFragment`) ketika tautan khusus diklik, tanpa mengharuskan pengguna mencari postingan tersebut secara manual dari awal.

## 2. Keputusan

Kami mengambil keputusan implementasi arsitektur berikut:

### A. Intent Filter di AndroidManifest
* Mendeklarasikan `<intent-filter>` pada `SplashActivity` di dalam [AndroidManifest.xml](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/AndroidManifest.xml) untuk menangkap skema:
  * Kustom: `adoptus://pet/{postId}`
  * Web / HTTPs: `https://adoptus.com/pet/{postId}` dan `https://www.adoptus.com/pet/{postId}`

### B. Penerusan Intent Data
* Di dalam [SplashActivity.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/SplashActivity.kt), mendeteksi apakah peluncuran dipicu oleh tautan Deep Link, lalu meneruskan objek `Uri` data tersebut ke `Intent` target saat membuka aktivitas utama (`MainActivity`).

### C. Ekstraksi & Routing Navigasi
* Di dalam [MainActivity.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/MainActivity.kt), mengekstrak data tautan pada `onCreate()` dan `onNewIntent()`.
* Mendeteksi ketersediaan parameter `postId` (sebagai segmen jalur terakhir dari URI), lalu menginisiasi navigasi langsung ke `PetDetailFragment` dengan argumen `postId` yang sesuai:
  ```kotlin
  val bundle = Bundle().apply { putString("postId", postId) }
  navController.navigate(R.id.petDetailFragment, bundle)
  ```

## 3. Konsekuensi

### Positif
* Meningkatkan pengalaman pengguna (*user experience*) secara signifikan saat membagikan dan membuka tautan hewan.
* Membantu strategi promosi adopsi hewan dari luar aplikasi.

### Negatif
* Pengguna harus sudah login agar gate autentikasi tidak memotong alur rute dan mengarahkannya kembali ke halaman login.
