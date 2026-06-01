# AdoptUs Mobile App

AdoptUs adalah platform ekosistem adopsi hewan berbasis aplikasi mobile Android yang mengusung konsep *Trusted Adoption Ecosystem*. Aplikasi ini menggabungkan social feed berbasis video dan foto (menyerupai TikTok/Instagram) dengan alur adopsi terstruktur dan integrasi WhatsApp.

Dikembangkan sebagai Tugas Besar Pemrograman Mobile — Universitas Mataram 2026.

---

## Status Proyek Saat Ini

###  Sudah Selesai
- Autentikasi lengkap: email/password dan Google Sign-In via Firebase
- Session management: auto-redirect ke Feed jika sudah login, ke Login jika belum
- Pesan error login/register yang bersih (tidak lagi raw Firebase error)
- Navigation Component (Jetpack Navigation) — menggantikan fragment manual
- Social Feed vertikal full screen (TikTok style) — real-time dari Firestore
- AddPostFragment — upload data hewan ke Firestore (teks, tanpa foto sementara)
- SettingFragment — edit profil (nama, username, bio, kota, nomor WA), ganti password, logout
- SettingFragment diakses dari icon di pojok kanan atas ProfileFragment
- BottomNavigationView terhubung ke NavController
- Back button HP dan tombol `<` di AddPost berfungsi benar

###  Pending / Belum Selesai
- Upload foto/video ke Firebase Storage — menunggu upgrade ke Blaze plan
- Kota di AddPost masih hardcode `"Indonesia"` — belum ambil dari profil user
- ProfileFragment masih menampilkan dummy data — belum connect ke Firestore
- PetDetailFragment — halaman detail hewan (placeholder)
- SearchFragment — filter hewan berdasarkan jenis/kota (placeholder)
- Like functionality — tombol like ada di UI tapi belum tersimpan ke Firestore
- Koleksi `adoptions` di Firestore — alur Apply → Approve/Reject belum diimplementasi

###  Diketahui Ada Masalah
- `btnBack` di AddPostFragment masih bertipe `ImageView`, seharusnya `ImageButton`
- ProfileFragment masih menampilkan nama dan data dummy, belum dari Firestore

---

## Tech Stack

| Komponen | Teknologi |
|---|---|
| Bahasa | Kotlin |
| Build System | Gradle Kotlin DSL + Version Catalog |
| Min SDK | 24 |
| Target/Compile SDK | 36 |
| UI | XML Layout + Material Components 3 |
| Navigasi | Jetpack Navigation Component 2.7.7 |
| Backend Auth | Firebase Authentication |
| Database | Cloud Firestore |
| Media Storage | Firebase Storage *(pending Blaze plan)* |
| Image Loading | Coil 2.6.0 |
| Video Player | Media3 ExoPlayer 1.3.1 |
| Async | Kotlin Coroutines + Flow/StateFlow |
| Architecture | MVVM (ViewModel + Repository) |

---

## Struktur Proyek

```text
app/src/main/
├── java/com/example/adoptus/
│   ├── MainActivity.kt
│   ├── data/
│   │   ├── model/
│   │   │   ├── User.kt
│   │   │   └── Post.kt
│   │   └── repository/
│   │       ├── AuthRepository.kt
│   │       └── PostRepository.kt
│   ├── fragment/
│   │   ├── FeedFragment.kt
│   │   ├── SearchFragment.kt
│   │   ├── AddPostFragment.kt
│   │   ├── ProfileFragment.kt
│   │   ├── SettingFragment.kt
│   │   ├── PetDetailFragment.kt
│   │   └── PetDetailFragmentArgs.kt
│   └── ui/
│       ├── auth/
│       │   ├── AuthViewModel.kt
│       │   ├── LoginActivity.kt
│       │   └── RegisterActivity.kt
│       └── feed/
│           ├── FeedViewModel.kt
│           └── FeedAdapter.kt
└── res/
    ├── anim/           # Animasi transisi slide
    ├── drawable/
    ├── layout/
    ├── menu/navbar.xml
    ├── navigation/main_nav.xml
    └── values/
```

---

## Firestore Schema

### Koleksi `users`
| Field | Tipe | Keterangan |
|---|---|---|
| uid | String | Primary Key, sama dengan Firebase Auth UID |
| fullName | String | Nama lengkap |
| username | String | Unik, tanpa spasi |
| email | String | Dari Firebase Auth |
| photoUrl | String? | URL foto profil di Storage |
| bio | String? | Deskripsi singkat |
| city | String? | Kota domisili |
| whatsapp | String? | Nomor WA untuk dihubungi adopter |
| createdAt | Timestamp | Waktu register |

### Koleksi `posts`
| Field | Tipe | Keterangan |
|---|---|---|
| postId | String | Primary Key |
| userId | String | FK → users.uid |
| petName | String | Nama hewan |
| petType | String | Kucing / Anjing / dll |
| breed | String | Ras hewan |
| age | Number | Angka usia |
| ageUnit | String | "Months" atau "Years" |
| city | String | Kota lokasi hewan |
| description | String? | Deskripsi opsional |
| mediaUrl | String | URL foto/video di Storage |
| mediaType | String | "image" atau "video" |
| isVaccinated | Boolean | Sudah divaksin? |
| hasHealthPassport | Boolean | Punya buku kesehatan? |
| adoptionFee | Number | 0 = gratis |
| status | String | "available" / "pending" / "adopted" |
| likesCount | Number | Jumlah like |
| createdAt | Timestamp | Waktu upload |

---

## Alur Navigasi

```
App buka
└── MainActivity (SplashScreen)
    ├── Belum login → LoginActivity
    │   ├── Login berhasil → MainActivity (Feed)
    │   └── Belum punya akun → RegisterActivity → MainActivity (Feed)
    └── Sudah login → langsung Feed
        ├── FeedFragment (default)
        │   └── Tap item → PetDetailFragment
        ├── SearchFragment
        ├── AddPostFragment (BottomNav hilang sementara)
        └── ProfileFragment
            ├── Icon ⚙️ → SettingFragment
            └── Tap item → PetDetailFragment
```

---

## Cara Menjalankan

### Prasyarat
- Android Studio yang mendukung Android Gradle Plugin 9.1.1
- JDK sesuai Gradle toolchain proyek
- Android SDK dengan compile SDK 36
- File `app/google-services.json` dari Firebase project AdoptUs (`adoptus-e66f1`)
- Firebase Authentication aktif (email/password dan Google)
- Cloud Firestore aktif

### Langkah
1. Buka Android Studio → **File > Open** → pilih folder repository
2. Tunggu Gradle Sync selesai
3. Jalankan konfigurasi `app` pada emulator atau perangkat Android

---

## Lisensi

Proyek ini dikembangkan sebagai Tugas Besar mata kuliah Pemrograman Mobile.
Hak cipta milik Tim Pengembang AdoptUs © 2026.
