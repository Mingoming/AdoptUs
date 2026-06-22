# AdoptUs Mobile App

AdoptUs adalah aplikasi Android native berbasis Kotlin untuk ekosistem adopsi hewan. Proyek ini dikembangkan sebagai Tugas Besar Pemrograman Mobile Universitas Mataram 2026.

Status kode terbaru sudah melewati starter app: aplikasi memiliki autentikasi Firebase, splash screen, navigasi utama dengan Jetpack Navigation, feed real-time dari Firestore, form add post berbasis Firestore, dan halaman setting untuk profil user.

## Status Proyek Saat Ini

### Sudah Selesai

* Autentikasi email/password dan Google Sign-In menggunakan Firebase Authentication.
* Session gate di `MainActivity`: user yang belum login diarahkan ke `LoginActivity`, user yang sudah login masuk ke Feed.
* Register dengan field nama lengkap, username, email, dan password.
* Pesan error login/register sudah dipetakan agar tidak menampilkan raw Firebase error.
* Splash screen menggunakan AndroidX Core SplashScreen.
* Jetpack Navigation Component untuk navigasi utama.
* `BottomNavigationView` terhubung ke `NavController` melalui `setupWithNavController()`.
* Feed vertikal full-screen dari koleksi `posts` Firestore.
* `FeedViewModel` memakai `StateFlow`; `PostRepository` memakai `callbackFlow` untuk update real-time.
* Feed dan Profile menampilkan gambar melalui Coil; post video memakai placeholder dengan ikon play tanpa autoplay.
* `AddPostFragment` menyimpan data teks hewan ke Firestore.
* Add Post dapat memilih satu gambar atau video MP4 dari galeri dan mengunggahnya ke Supabase Storage.
* `SettingFragment` memuat dan menyimpan data profil user ke Firestore.
* Register baru menyimpan profil user dengan field camelCase yang konsisten.
* Setting tetap dapat membaca field lama seperti `full_name`, `photo_url`, dan `created_at`.
* Firestore rules sederhana diterapkan manual melalui Firebase Console untuk membatasi profil dan post berdasarkan pemiliknya.
* Logout dari `SettingFragment` membersihkan session Firebase dan kembali ke `LoginActivity`.
* `ProfileFragment` menampilkan profil Firestore dan post milik user yang sedang login.
* `PetDetailFragment` memuat satu post dari Firestore berdasarkan `postId` dan memutar video dengan Media3.
* Pemuatan profil dinamis dan navigasi langsung ke profil pemilik hewan dari Feed (`FeedFragment`) dan halaman pencarian (`SearchFragment`).
* WhatsApp clickable di halaman profil untuk secara instan menghubungi pemilik hewan melalui chat WhatsApp.
* Fitur Swipe-to-Refresh untuk memuat ulang data dengan animasi menarik di halaman Feed dan Profile.
* Pilihan foto profil dengan fitur pemotongan (cropping) menggunakan pustaka UCrop.
* Safe layout pada `AddPostFragment` dengan bar atas transparan dan padding bawah agar nyaman digunakan dengan sistem navigasi tombol/gestur Android.
* Tombol like yang tersinkronisasi dan tersimpan statusnya secara persisten ke Firestore (`posts/{postId}/likes`).
* Alur adopsi lengkap (Apply dari Feed, halaman Inbox di Profile, Approve / Reject permohonan secara atomik menggunakan transaksi batch).
* Mengaktifkan Firestore Offline Persistence secara global untuk mendukung pemuatan data cache saat offline.
* Caching data profil lokal di SharedPreferences untuk mengurangi query reads Firestore.
* Pemuatan feed terpaginasi (pagination) pada feed utama dan explore grid pencarian dengan scroll listener dinamis.
* Sistem pemutaran video ala TikTok dengan scroll snapping (PagerSnapHelper) dan auto-pause/mute audio saat meninggalkan halaman feed.
* Navigasi Deep Link (`adoptus://pet/{postId}`) terintegrasi untuk pendaratan langsung di Pet Detail.
* Integrasi berkas aturan keamanan resmi (`firestore.rules` & `supabase_rls.sql`) di repositori git.

### Belum Selesai

* Notifikasi push (FCM) untuk memberi tahu pengadopsi saat permohonan disetujui/ditolak.

### Catatan Teknis

* Data user lama mungkin masih memakai `full_name`, `photo_url`, dan `created_at`. Aplikasi tetap membacanya sebagai fallback, tetapi user baru hanya ditulis dengan schema camelCase.

## Tech Stack

| Komponen | Teknologi |
|---|---|
| Bahasa | Kotlin |
| Build System | Gradle Kotlin DSL + Version Catalog |
| Min SDK | 24 |
| Target/Compile SDK | 36 |
| UI | XML Layout + Material Components |
| Navigasi | Jetpack Navigation Component 2.7.7 |
| Auth | Firebase Authentication |
| Database | Cloud Firestore |
| Media Upload | Supabase Storage REST API |
| Image Loading | Coil 2.6.0 |
| Video Player | Media3 ExoPlayer 1.3.1 |
| Async | Kotlin Coroutines, Flow, StateFlow |
| Architecture | MVVM sederhana: ViewModel + Repository |

## Struktur Proyek

```text
app/src/main/
|-- java/com/example/adoptus/
|   |-- MainActivity.kt
|   |-- data/
|   |   |-- model/
|   |   |   |-- User.kt
|   |   |   `-- Post.kt
|   |   `-- repository/
|   |       |-- AuthRepository.kt
|   |       |-- PostMediaRepository.kt
|   |       `-- PostRepository.kt
|   |-- fragment/
|   |   |-- FeedFragment.kt
|   |   |-- SearchFragment.kt
|   |   |-- AddPostFragment.kt
|   |   |-- ProfileFragment.kt
|   |   |-- SettingFragment.kt
|   |   |-- Petdetailfragment.kt
|   |   `-- Petdetailfragmentargs.kt
|   `-- ui/
|       |-- auth/
|       |   |-- AuthViewModel.kt
|       |   |-- LoginActivity.kt
|       |   `-- RegisterActivity.kt
|       |-- detail/
|       |   `-- PetDetailViewModel.kt
|       |-- feed/
|       |   |-- FeedViewModel.kt
|       |   `-- Feedadapter.kt
|       `-- profile/
|           |-- ProfileViewModel.kt
|           `-- ProfilePostAdapter.kt
`-- res/
    |-- anim/
    |-- color/
    |-- drawable/
    |-- layout/
    |-- menu/navbar.xml
    |-- navigation/main_nav.xml
    |-- values/
    `-- xml/
```

## Firestore Schema Saat Ini

### Koleksi `users`

| Field | Tipe | Keterangan |
|---|---|---|
| id | String | UID Firebase Auth dan sama dengan ID dokumen |
| username | String | Username user (3–30 karakter, regex: `^[A-Za-z0-9._]{3,30}$`) |
| fullName | String | Nama lengkap (maksimal 80 karakter) |
| photoUrl | String | URL foto profil, saat ini bisa kosong |
| bio | String | Bio user (maksimal 300 karakter) |
| city | String | Kota user (maksimal 80 karakter) |
| whatsapp | String | Nomor WhatsApp user (maksimal 30 karakter) |
| role | String | Default `user` |
| createdAt | Timestamp | Waktu register |

Email tetap disimpan oleh Firebase Authentication dan tidak ditulis ke dokumen user baru. Untuk kompatibilitas data development lama, aplikasi masih membaca `email`, `full_name`, `photo_url`, dan `created_at` jika field tersebut tersedia.

### Firestore Rules

Rules diterapkan manual melalui Firebase Console dengan aturan sederhana:

* User hanya dapat membaca dan mengubah dokumen profil miliknya.
* Role dan ID profil tidak dapat diubah lewat update biasa.
* Post dapat dibaca user yang sudah login.
* Post hanya dapat dibuat, diubah, atau dihapus oleh pemiliknya.

Repository ini tidak menyimpan atau mendeploy file rules secara otomatis.

### Koleksi `posts`

| Field | Tipe | Keterangan |
|---|---|---|
| postId | String | ID dokumen post |
| userId | String | UID owner post |
| petName | String | Nama hewan |
| petType | String | Jenis hewan |
| breed | String | Ras hewan |
| age | Number | Angka usia |
| ageUnit | String | `Months` atau `Years` |
| city | String | Kota lokasi hewan |
| description | String | Deskripsi |
| mediaUrl | String | Public URL Supabase atau kosong jika post tanpa media |
| mediaType | String | `image` atau `video` |
| isVaccinated | Boolean | Status vaksin |
| hasHealthPassport | Boolean | Status buku kesehatan |
| adoptionFee | Number | `0` berarti gratis |
| status | String | `available`, `pending`, atau `adopted` |
| likesCount | Number | Jumlah like |
| ownerUsername | String | Username pemilik hewan (embedded) |
| ownerPhotoUrl | String | URL foto profil pemilik (embedded) |
| ownerWhatsapp | String | Nomor WhatsApp pemilik (embedded) |
| createdAt | Timestamp | Waktu upload |

### Koleksi `adoptions`

| Field | Tipe | Keterangan |
|---|---|---|
| adoptionId | String | ID dokumen pengajuan adopsi |
| postId | String | ID postingan hewan terkait |
| petName | String | Nama hewan yang diajukan |
| adopterId | String | UID pelamar adopsi |
| adopterName | String | Nama lengkap pelamar adopsi |
| ownerId | String | UID pemilik postingan hewan |
| status | String | `pending`, `approved`, `rejected`, atau `cancelled` |
| createdAt | Timestamp | Waktu pengajuan request adopsi |
| updatedAt | Timestamp | Waktu pembaruan status |

## Alur Navigasi

```text
App dibuka
`-- MainActivity + SplashScreen
    |-- Belum login -> LoginActivity
    |   |-- Login berhasil -> MainActivity
    |   `-- RegisterActivity -> MainActivity
    `-- Sudah login -> NavHost feedFragment
        |-- FeedFragment
        |   `-- PetDetailFragment
        |-- SearchFragment
        |-- AddPostFragment
        |-- ProfileFragment
        |   |-- SettingFragment
        |   |-- InboxFragment
        |   |   `-- PetDetailFragment
        |   `-- PetDetailFragment
        `-- SettingFragment
```

Bottom navigation disembunyikan saat masuk ke `AddPostFragment`, `PetDetailFragment`, dan `InboxFragment`.

## Cara Menjalankan

### Prasyarat

* Android Studio yang mendukung Android Gradle Plugin 9.1.1.
* Android SDK dengan compile SDK 36.
* JDK lengkap yang memiliki `jlink.exe`.
* File `app/google-services.json` untuk Firebase project.
* Firebase Authentication aktif untuk email/password dan Google.
* Cloud Firestore aktif.
* Bucket Supabase public bernama `adoptus-post-images`.
* `SUPABASE_URL` dan `SUPABASE_PUBLISHABLE_KEY` tersedia di `local.properties`.

Contoh konfigurasi lokal:

```properties
SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_your_key_here
```

Gunakan publishable key, bukan secret key atau `service_role`. Nilai aktual tidak boleh di-commit.

Kebijakan Row-Level Security (RLS) Supabase Storage yang lengkap dan aman untuk bucket `adoptus-post-images` telah didefinisikan secara resmi di dalam berkas [doc/supabase_rls.sql](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/doc/supabase_rls.sql). 

Kebijakan ini mengizinkan akses baca publik, namun membatasi unggah (`INSERT`) dan hapus (`DELETE`) media di bawah folder `posts/{uid}/*` hanya untuk user terautentikasi (`authenticated` role) yang mencocokkan ID pengguna mereka dengan nama folder (`auth.uid()`).

Aplikasi membatasi unggahan gambar maksimal 5 MB dan video MP4 maksimal 20 MB sebelum proses upload dilakukan.

Foto disimpan dengan path:

```text
posts/{firebaseUid}/{timestamp}.{jpg|png|webp}
```

### Jalankan dari Android Studio

1. Buka Android Studio.
2. Pilih **File > Open** dan arahkan ke folder repository.
3. Tunggu Gradle Sync selesai.
4. Jalankan konfigurasi `app` pada emulator atau perangkat Android.

### Jalankan Unit Test

Jika Gradle memakai JRE VS Code yang tidak punya `jlink.exe`, jalankan dengan JDK lokal lengkap:

```powershell
$env:JAVA_HOME='C:\Amanta\java\Install'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:testDebugUnitTest --no-daemon
```

## Dokumentasi Arsitektur

ADR tersedia di `doc/adr/`:

* [ADR 0001](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/doc/adr/0001-architecture-decision.md): Arsitektur awal dan autentikasi.
* [ADR 0002](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/doc/adr/0002-main-navigation-and-profile-ui.md): Bottom navigation dan profile frontend mode.
* [ADR 0003](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/doc/adr/0003-navigation-component-migration.md): Migrasi ke Jetpack Navigation Component.
* [ADR 0004](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/doc/adr/0004-feed-firestore-setting.md): Firestore feed, post model, AddPost, dan Setting.
* [ADR 0005](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/doc/adr/0005-current-implementation-baseline.md): Baseline implementasi terbaru dan gap teknis.
* [ADR 0006](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/doc/adr/0006-profile-and-pet-detail-firestore-integration.md): Integrasi Firestore untuk profil dinamis dan detail hewan.
* [ADR 0007](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/doc/adr/0007-profile-cropping-refresh-and-layout-safe-areas.md): Pemotongan foto profil (UCrop), Swipe-to-Refresh, dan Safe Areas.
* [ADR 0008](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/doc/adr/0008-direct-profile-redirection-from-feed.md): Navigasi profil pembuat postingan langsung dari feed.
* [ADR 0009](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/doc/adr/0009-adoption-flow-and-inbox-implementation.md): Alur pengajuan adopsi (Apply/Approve/Reject) dan Inbox masuk.
* [ADR 0010](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/doc/adr/0010-offline-persistence-and-local-caching.md): Persistensi offline Firestore dan Caching profil lokal (SharedPreferences).
* [ADR 0011](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/doc/adr/0011-paged-query-pagination-for-feed-and-explore.md): Pemuatan data terpaginasi (Pagination) feed utama dan explore.
* [ADR 0012](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/doc/adr/0012-deep-link-navigation-routing.md): Navigasi detail hewan berbasis Deep Link.
* [ADR 0013](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/doc/adr/0013-database-and-storage-security-rules.md): Aturan keamanan database Firestore dan Supabase Storage.

## Lisensi

Proyek ini dikembangkan sebagai Tugas Besar mata kuliah Pemrograman Mobile. Hak cipta milik Tim Pengembang AdoptUs (C) 2026.
