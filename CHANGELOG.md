# Changelog — AdoptUs Mobile Project

Semua perubahan penting pada proyek Android **AdoptUs** didokumentasikan di file ini.
Format mengikuti [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [0.5.0-alpha] — 2026-05-31

### Added
- **SettingFragment**: Halaman pengaturan profil user yang diakses dari icon ⚙️ di pojok kanan atas ProfileFragment.
  - Field: nama lengkap, username, bio, kota, nomor WhatsApp, ganti password
  - Data di-load dari Firestore saat halaman dibuka
  - Tombol Save update Firestore + Firebase Auth (untuk ganti password)
  - Tombol Log Out sign out Firebase dan redirect ke LoginActivity
- **fragment_setting.xml**: Layout halaman Setting dengan NestedScrollView, TextInputLayout Material, dan loading overlay
- **bg_btn_logout.xml**: Drawable tombol logout bergaya outlined oranye

### Changed
- **ProfileFragment**: Tombol icon setting kanan atas sekarang navigasi ke SettingFragment via NavController
- **main_nav.xml**: Ditambah destination `settingFragment` dan action `action_profile_to_setting`

### Fixed
- Konten tertutup Dynamic Island/Notch — ditambah `fitsSystemWindows="true"` di layout Profile, Setting, dan AddPost
- Touch target tombol diperbesar ke minimal 48dp sesuai standar aksesibilitas Android
- Ditambah efek ripple pada tombol-tombol utama

---

## [0.4.0-alpha] — 2026-05-31

### Added
- **AddPostFragment (Firestore)**: Form upload hewan sekarang menyimpan data ke koleksi `posts` di Firestore
  - Field: petName, petType, breed, age, ageUnit, description, adoptionFee, isVaccinated, hasHealthPassport
  - Field `mediaUrl` dikosongkan sementara menunggu Firebase Storage aktif
  - Loading state saat upload (tombol disabled, teks "Uploading...")
  - Toast konfirmasi setelah berhasil upload
- **Post.kt**: Data model untuk koleksi `posts` Firestore dengan `fromMap()` dan `toMap()`
- **PostRepository.kt**: Repository dengan Flow real-time untuk getFeedPosts(), getMyPosts(), getPostsByType(), createPost(), updatePostStatus(), deletePost()
- **FeedViewModel.kt**: ViewModel dengan StateFlow dan sealed class FeedState (Loading, Success, Empty, Error)
- **FeedAdapter.kt**: RecyclerView Adapter dengan dukungan foto (Coil) dan video (ExoPlayer), play/pause otomatis saat item snap ke tengah
- **item_feed_post.xml**: Layout item feed full screen dengan gradient overlay, info hewan, tombol Apply & WhatsApp, panel like di kanan
- Drawable baru: gradient_feed_bottom, bg_status_badge, bg_btn_apply, bg_btn_whatsapp, bg_fee_badge, bg_avatar_circle, ic_location, ic_heart, ic_info, ic_profile_placeholder

### Changed
- **FeedFragment**: Dari placeholder menjadi RecyclerView full screen dengan PagerSnapHelper (TikTok behavior), observasi StateFlow, dan 3 state UI (loading, empty, error)
- **fragment_feed.xml**: Dari placeholder menjadi layout dengan RecyclerView, ProgressBar, empty state, dan error state
- **PostRepository.getFeedPosts()**: Dihapus `whereEqualTo("status", "available")` dari query Firestore untuk menghindari kebutuhan composite index — filter status dilakukan di sisi app

### Fixed
- Error `Operator '!=' cannot be applied to 'Int' and 'Long'` di FeedFragment — diganti `RecyclerView.NO_ID` (Long) ke `RecyclerView.NO_POSITION` (Int)
- Duplicate resource error `placeholder` — dihapus `placeholder.xml` yang konflik dengan `placeholder.jpeg` yang sudah ada

---

## [0.3.0-alpha] — 2026-05-31

### Added
- **Jetpack Navigation Component**: Migrasi dari fragment manual ke NavController
  - `main_nav.xml`: Graph navigasi dengan 5 destination (feed, search, addPost, profile, petDetail)
  - Animasi transisi slide: slide_in_right, slide_out_left, slide_in_left, slide_out_right
  - `PetDetailFragment` + `PetDetailFragmentArgs` sebagai placeholder halaman detail hewan
- **Dependency baru**: navigation-fragment-ktx 2.7.7, navigation-ui-ktx 2.7.7, Coil 2.6.0, Media3 ExoPlayer 1.3.1

### Changed
- **MainActivity**: Migrasi ke `NavController` + `setupWithNavController()`, tidak lagi pakai `supportFragmentManager.replace()` manual
- **activity_main.xml**: `FrameLayout` diganti `FragmentContainerView` dengan NavHostFragment, struktur diubah ke `LinearLayout` vertikal agar NavHost tidak overlap BottomNav
- **navbar.xml**: ID menu item disamakan dengan ID destination di nav graph (`menu_feed` → `feedFragment`, dst) agar `setupWithNavController` berfungsi
- **AddPostFragment**: Back button pakai `findNavController().navigateUp()` — tidak lagi hardcode ke FeedFragment
- **libs.versions.toml**: Ditambah versi navigationFragment, coil, exoplayer; dihapus section `[versions-nav-fragment]` yang invalid

### Fixed
- **Gradle Sync error** "Invalid TOML catalog definition" — section `[versions-nav-fragment]` yang tidak valid dihapus dari libs.versions.toml
- **BottomNav tidak bisa dipencet** — ID menu tidak cocok dengan destination nav graph, diperbaiki dengan menyamakan ID
- **Back HP di AddPost keluar app** — sekarang ditangani NavController secara otomatis

---

## [0.2.1-alpha] — 2026-05-31

### Fixed
- **Error message login** — pesan raw Firebase (bahasa Inggris teknis) diganti dengan pesan bersih via `mapFirebaseError()`:
  - "Invalid email or password."
  - "No account found with this email."
  - "Please enter a valid email address."
  - "Too many attempts. Please try again later."
  - "No internet connection."
- **Auto-redirect** — kalau user sudah login, buka app langsung ke Feed (skip LoginActivity)
- **Register validation** — error kini tampil di bawah field masing-masing (TextInputLayout.error) bukan Toast
- **viewBinding** — LoginActivity, RegisterActivity, MainActivity migrasi dari `findViewById` ke viewBinding

---

## [Unreleased] — 2026-05-23

### Added
- Floating Bottom Navigation Bar dengan 4 item: Feed, Search, Add Post, Profile
- Modular Fragment Package: FeedFragment, SearchFragment, AddPostFragment, ProfileFragment
- UI Profile Page (frontend mode): header, statistik, TabLayout, RecyclerView grid 3 kolom, deteksi isVideo

### Changed
- MainActivity menjadi host Single Activity dengan FrameLayout fragment_container
- Global theme: warna active indicator pill Material 3 jadi oranye pastel

### Fixed
- Error incompatible attribute TabLayout background
- Adapter syncing bug DummyItem unresolved reference

### Removed
- Tombol logout di MainActivity

---

## [0.2.0-alpha] — 2026-05-21

### Added
- Autentikasi Firebase: login email/password, register, Google Sign-In
- Penyimpanan data user ke koleksi `users` Firestore
- LoginActivity sebagai launcher, RegisterActivity, AuthViewModel, AuthRepository, model User
- Permission INTERNET di manifest

---

## [0.1.0-alpha] — 2026-05-21

### Added
- Inisialisasi proyek Android native Kotlin
- MainActivity, layout dasar, resource dasar
- Gradle Wrapper 9.3.1, version catalog, unit test dan instrumented test
- README.md, CHANGELOG.md, ADR arsitektur awal