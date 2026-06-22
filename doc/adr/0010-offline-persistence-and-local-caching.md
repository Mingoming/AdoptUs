# ADR 0010 - Persistensi Offline Firestore dan Caching Profil Lokal

* **Status:** Approved
* **Tanggal:** 23 Juni 2026
* **Penulis:** Tim Pengembang AdoptUs

## 1. Konteks

Dalam pengembangan aplikasi AdoptUs, performa data dan kestabilan saat offline sangatlah penting:
1. **Biaya & Kecepatan Pembacaan Firestore**: Selama ini, saat mengunggah postingan baru di `AddPostFragment`, aplikasi melakukan query Firestore berlebih untuk memuat profil pengguna (khususnya informasi kota, foto, dan WhatsApp). Ini menimbulkan masalah performa (N+1 query) dan memicu biaya database tambahan.
2. **Ketersediaan Offline**: Saat pengguna kehilangan koneksi internet, aplikasi tidak dapat menampilkan data postingan lama yang sudah dimuat sebelumnya.

## 2. Keputusan

Kami mengambil keputusan implementasi arsitektur berikut:

### A. Firestore Offline Persistence
* Membuat subclass `Application` kustom bernama [AdoptUsApplication.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/AdoptUsApplication.kt) untuk mengaktifkan **Firestore Offline Persistence** secara global sebelum komponen lain diinisialisasi.
* Mendaftarkan kelas tersebut pada berkas [AndroidManifest.xml](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/AndroidManifest.xml).

### B. Caching Profil Lokal dengan SharedPreferences
* Menambahkan helper caching profil (`cacheUserProfile`, `getCachedUserProfile`, `clearUserCache`) di [AuthRepository.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/data/repository/AuthRepository.kt) menggunakan `SharedPreferences` lokal.
* Melakukan pre-fetch data profil saat startup aplikasi pada [SplashActivity.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/SplashActivity.kt) untuk disimpan di cache lokal.
* Memperbarui cache lokal setelah profil berhasil disimpan dan membersihkannya saat logout pada [SettingFragment.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/fragment/SettingFragment.kt).
* Membaca informasi profil & kota instan dari cache lokal di [AddPostFragment.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/fragment/AddPostFragment.kt).
* Mengoptimalkan fungsi `createPost` pada [PostRepository.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/data/repository/PostRepository.kt) agar menggunakan data `cachedUser` sehingga menghemat 1x query Firestore read.

## 3. Konsekuensi

### Positif
* Penghematan kuota reads Firestore yang signifikan (menghindari N+1 query profil user saat menampilkan data / membuat postingan).
* Aplikasi tetap dapat memuat feed postingan sebelumnya yang sudah dicache saat offline.

### Negatif
* Perlu ketelitian dalam sinkronisasi data cache lokal agar profil lokal tidak kedaluwarsa (*stale*) saat terjadi pembaruan di server cloud.
