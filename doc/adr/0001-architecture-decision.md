# Architecture Decision Record: 0001 - Arsitektur Awal dan Autentikasi AdoptUs

* **Status:** Approved
* **Tanggal:** 21 Mei 2026
* **Penulis:** Tim Pengembang AdoptUs

## 1. Konteks

Proyek AdoptUs sudah bergerak dari starter app Android menjadi aplikasi dengan fondasi autentikasi. Fitur yang sudah tersedia meliputi login email/password, register akun, login Google, penyimpanan data user ke Firestore, halaman login/register, dan halaman utama sederhana setelah user berhasil login.

Implementasi saat ini masih fokus pada autentikasi. Fitur domain adopsi hewan seperti daftar hewan, feed, status adopsi, dan komunikasi WhatsApp belum tersedia.

## 2. Keputusan

Kami menggunakan arsitektur awal Android native dengan keputusan berikut:

1. **Android native Kotlin dengan XML layout**
   * Proyek dibuat sebagai aplikasi Android native berbasis Kotlin.
   * UI menggunakan XML layout dan Material Components.
   * Jetpack Compose belum digunakan.

2. **Flow autentikasi berbasis Activity**
   * `LoginActivity` menjadi launcher utama aplikasi.
   * `RegisterActivity` menangani pembuatan akun baru.
   * `MainActivity` hanya ditampilkan setelah user sudah login.
   * Belum ada Navigation Component atau fragment-based navigation.

3. **Firebase sebagai backend autentikasi awal**
   * Firebase Authentication digunakan untuk email/password dan Google Sign-In.
   * Cloud Firestore digunakan untuk menyimpan dokumen user pada koleksi `users`.
   * File `app/google-services.json` digunakan untuk konfigurasi Firebase project.

4. **Pemisahan sederhana UI, ViewModel, dan Repository**
   * `AuthViewModel` menyimpan state autentikasi dengan LiveData.
   * `AuthRepository` membungkus akses ke Firebase Auth dan Firestore.
   * `User` menjadi model data awal untuk user.
   * Coroutines digunakan untuk menjalankan operasi Firebase async melalui `await()`.

5. **Dependency dikelola dengan Gradle version catalog**
   * Dependency utama didefinisikan di `gradle/libs.versions.toml`.
   * Module `app` memakai Google Services Gradle Plugin, Firebase BOM, Firebase Auth, Firestore, Google Sign-In, Coroutines, dan Lifecycle.

## 3. Konsekuensi

### Positif

* Aplikasi sudah memiliki fondasi login, register, session check, dan logout.
* Firebase mempercepat implementasi autentikasi tanpa backend custom.
* Pemisahan `AuthViewModel` dan `AuthRepository` membuat logic auth lebih mudah dikembangkan.
* Google Sign-In bisa dipakai sebagai opsi login selain email/password.

### Negatif

* Flow masih berbasis beberapa Activity dan belum memakai navigasi terpusat.
* Arsitektur baru diterapkan pada area autentikasi, belum pada seluruh aplikasi.
* Aplikasi bergantung pada konfigurasi Firebase dan Google Sign-In yang benar.
* Fitur domain AdoptUs belum tersedia, sehingga `MainActivity` masih berupa halaman sederhana setelah login.

## 4. Catatan Lanjutan

ADR baru perlu dibuat ketika proyek mulai menambahkan arsitektur navigasi utama, data hewan, role adopter/foster, atau fitur backend lain di luar autentikasi. Jika fitur domain sudah bertambah, struktur package dan pola repository perlu distandardisasi agar tidak hanya berlaku untuk auth.
