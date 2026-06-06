# ADR 0005 - Baseline Implementasi Terbaru dan Gap Teknis

* **Status:** Approved
* **Tanggal:** 6 Juni 2026
* **Penulis:** Tim Pengembang AdoptUs

## 1. Konteks

Setelah rangkaian perubahan sampai commit `91e12c3`, proyek AdoptUs sudah memiliki struktur aplikasi yang lebih lengkap daripada fase auth awal. Aplikasi sekarang memakai splash screen, Firebase Authentication, Jetpack Navigation Component, feed real-time dari Firestore, form add post, halaman setting, dan bottom navigation.

Dokumentasi perlu menetapkan baseline terbaru agar tim tidak kembali mengacu ke status lama seperti fragment manual, feed placeholder, atau `LoginActivity` sebagai launcher utama.

## 2. Keputusan

Kami menetapkan baseline implementasi berikut sebagai kondisi proyek saat ini:

1. **MainActivity sebagai launcher dan auth gate**
   * `MainActivity` memakai splash screen.
   * Jika user belum login, `MainActivity` mengarahkan user ke `LoginActivity`.
   * Jika user sudah login, `MainActivity` memuat navigation host dan membuka feed.

2. **Navigation Component sebagai navigasi utama**
   * `activity_main.xml` memakai `FragmentContainerView` dengan `NavHostFragment`.
   * `main_nav.xml` menjadi sumber utama destination dan action.
   * `BottomNavigationView` terhubung ke `NavController` melalui `setupWithNavController()`.
   * Bottom navigation disembunyikan pada `AddPostFragment` dan `PetDetailFragment`.

3. **Firestore feed dan post model**
   * Koleksi `posts` menjadi sumber feed.
   * `PostRepository` membungkus query Firestore.
   * `FeedViewModel` memakai `StateFlow`.
   * `FeedAdapter` menampilkan image dengan Coil dan video dengan Media3 ExoPlayer.

4. **AddPost dan Setting**
   * `AddPostFragment` menyimpan data teks hewan ke Firestore.
   * Upload media belum aktif karena Firebase Storage belum dipakai.
   * `SettingFragment` membaca dan memperbarui data profil user di Firestore.
   * Logout dilakukan dari `SettingFragment`.

5. **Gap schema user harus diselesaikan**
   * Register menulis field seperti `full_name`, `photo_url`, dan `created_at`.
   * Setting memakai field seperti `fullName`, `bio`, `city`, dan `whatsapp`.
   * Schema user harus dinormalisasi sebelum fitur profile dan setting menjadi final.

## 3. Konsekuensi

### Positif

* Alur utama aplikasi sudah lebih jelas: splash, auth gate, feed, add post, profile, dan setting.
* Navigasi berbasis graph lebih mudah dipelihara daripada fragment replace manual.
* Firestore feed bisa update real-time.
* AddPost dapat dipakai untuk menguji data feed tanpa menunggu upload media.

### Negatif

* Search, profile data, dan pet detail belum final.
* Storage belum aktif sehingga feed media masih bergantung pada URL kosong atau data eksternal.
* User schema yang belum konsisten bisa menyebabkan data profile tidak terbaca sesuai ekspektasi.
* Dependency Compose/Navigation Compose ada di Gradle, tetapi UI aplikasi masih XML; perlu dirapikan jika tidak digunakan.

## 4. Tindak Lanjut

* Normalisasi field koleksi `users`.
* Tentukan apakah Compose dependency dipakai atau dihapus.
* Implementasikan Search berdasarkan `PostRepository.getPostsByType()`.
* Hubungkan `ProfileFragment` ke data user dan post milik user.
* Implementasikan `PetDetailFragment` dengan data dari `PostRepository.getPostById()`.
* Aktifkan Firebase Storage atau tentukan strategi media alternatif.
