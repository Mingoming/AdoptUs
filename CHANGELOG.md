# Changelog - AdoptUs Mobile Project

Semua perubahan penting pada proyek Android **AdoptUs** didokumentasikan di file ini.

Format changelog ini mengikuti prinsip [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

# Changelog

Semua perubahan penting pada proyek **AdoptUs** akan dicatat di file ini. Format ini mengacu pada standar [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased] - 2026-05-23

### Added
- **Floating Bottom Navigation Bar**: Membuat menu navigasi melayang di `MainActivity` dengan 4 item utama: Feed, Search, Add Post, dan Profile.
- **Modular Fragment Package**: Membuat package folder `fragment` dan memisahkan struktur halaman utama ke dalam `FeedFragment`, `SearchFragment`, `AddPostFragment`, dan `ProfileFragment`.
- **UI Profile Page (Frontend Mode)**:
    * Layout detail informasi pengguna menggunakan `ConstraintLayout`.
    * Komponen statistik (*Pets Listed* & *Successful Adoptions*) yang rata tengah secara horizontal di sebelah kanan avatar.
    * Komponen `TabLayout` interaktif untuk menu pemisah antara **Pets** dan **Content**.
    * `RecyclerView` dengan tipe Grid 3 Kolom untuk menampilkan galeri foto.
    * Kartu gambar *full-bleed* (foto penuh) tanpa sudut melengkung (`cardCornerRadius="0dp"`) agar visual terlihat lebih modern.
    * Fitur deteksi tipe konten (`isVideo`). Jika item bernilai `true`, kartu akan otomatis memunculkan overlay bayangan dan ikon tombol *Play* (▶️) di tengah gambar.
- **Interactive Tab Listener**: Menambahkan logika `addOnTabSelectedListener` di `ProfileFragment` untuk menukar isi list data di dalam adapter secara instan menggunakan fungsi `updateList()` saat tab diketuk.

### Changed
- **MainActivity Architecture**: Mengubah fungsi `MainActivity` menjadi *Host Single Activity* yang mengontrol navigasi fragment via `FrameLayout` (`fragment_container`) agar perpindahan halaman tidak berkedip (*screen blinking*).
- **Global Theme Customization**: Mengubah warna kapsul aktif bawaan (*Active Indicator Pill*) Material 3 secara global di `themes.xml` menjadi warna oranye pastel lembut (`#FFF0EC`) agar selaras dengan tema aplikasi.

### Fixed
- **Android Resource Linking Error**: Memperbaiki eror *incompatible attribute* pada `TabLayout` dengan mengganti properti latar belakang mentah `"transparent"` menjadi warna sistem resmi `@android:color/transparent`.
- **Adapter Syncing Bug**: Menyinkronkan variabel objek adapter di `rvPetGrid` agar data class `DummyItem` terbaca dengan benar dan tidak memicu eror *unresolved reference* saat proyek di-compile.

### Removed
- **Hapus button logout di `MainActivity`**

## Rencana Berikutnya
- **Waktu password salah, ganti toast messagenya jadi pass/email salah, soalnya dia masih pake "auth credential is incoreect, ......."**
- **Klo ud login dan punya akun, pas buka app langsung ke feed**

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
