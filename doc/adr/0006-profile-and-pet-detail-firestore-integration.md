# ADR 0006 - Integrasi Firestore pada Profil, Detail Hewan, dan Normalisasi Data Pengguna

* **Status:** Approved
* **Tanggal:** 21 Juni 2026
* **Penulis:** Tim Pengembang AdoptUs

## 1. Konteks

Sebelumnya, pada ADR 0005 diidentifikasi beberapa celah teknis (gap), termasuk:
1. Inkonsistensi skema pengguna (`full_name` vs `fullName`) antara alur registrasi dan pembaruan pengaturan profil.
2. Halaman profil (`ProfileFragment`) dan pencarian (`SearchFragment`) yang masih menggunakan data mock lokal.
3. Halaman detail hewan (`PetDetailFragment`) yang hanya berupa placeholder kosong.
4. Kurangnya pembatasan/validasi panjang karakter input yang dapat menyebabkan eksploitasi data Firestore.

Perubahan pada rangkaian commit terbaru telah menyelesaikan permasalahan ini dengan mengintegrasikan Firestore secara dinamis pada Profil dan Detail Hewan, serta melakukan konsolidasi skema pengguna.

## 2. Keputusan

Kami mengambil keputusan implementasi berikut untuk meningkatkan keandalan dan konsistensi data:

### A. Normalisasi Skema Pengguna dan Fallback Data
* Struktur data pengguna dikonsolidasikan menggunakan gaya penulisan `camelCase` (`fullName`, `photoUrl`, `createdAt`).
* Untuk menjaga kompatibilitas dengan akun lama di database development, fungsi pembaca `User.fromMap()` dilengkapi dengan logika pencarian fallback (`full_name` -> `fullName`, `photo_url` -> `photoUrl`, `created_at` -> `createdAt`).
* Saat data diperbarui melalui `SettingFragment`, data selalu disimpan kembali dalam format skema baru (`camelCase`).

### B. Validasi Karakter Input (Input Constraints)
Untuk mengamankan database Firestore dari spamming atau payload terlalu besar, kami menerapkan batas karakter berikut di sisi aplikasi (`RegisterActivity` dan `SettingFragment`):
* **Username:** 3–30 karakter, hanya boleh berupa huruf, angka, titik (`.`), dan garis bawah (`_`) tanpa spasi (menggunakan Regex `^[A-Za-z0-9._]{3,30}$`).
* **Full Name:** Maksimal 80 karakter.
* **Bio:** Maksimal 300 karakter.
* **City:** Maksimal 80 karakter.
* **WhatsApp:** Maksimal 30 karakter.

### C. Alur Rollback Registrasi yang Aman (Atomic Registration)
* Saat pengguna melakukan registrasi baru, akun Authentication dibuat terlebih dahulu melalui Firebase Auth.
* Jika penyimpanan dokumen profil pengguna ke Cloud Firestore gagal (misalnya karena masalah koneksi atau pembatasan rules), akun Firebase Auth yang baru dibuat tersebut akan dihapus kembali secara otomatis (`user.delete().await()`). Ini mencegah timbulnya akun "yatim piatu" (terdaftar di Auth tetapi tidak memiliki profil di Firestore).

### D. Integrasi Dinamis Profil dan Detail Hewan
* **ProfileFragment:** Diubah menjadi arsitektur MVVM penuh dengan `ProfileViewModel` dan `ProfilePostAdapter`. Profil memuat data pengguna secara real-time dari Firestore melalui fungsi `getCurrentUserProfile()` di `AuthRepository`. Daftar postingan pengguna dimuat dinamis dari query Firestore `whereEqualTo("userId", uid)` secara real-time.
* **PetDetailFragment:** Diubah dari placeholder menjadi halaman interaktif lengkap dengan layout XML baru (`fragment_petdetail.xml`). Halaman memuat detail spesifik postingan hewan berdasarkan `postId` menggunakan `PetDetailViewModel`. Halaman ini juga menampilkan foto hewan, detail vaksinasi, kelengkapan paspor kesehatan, nama pemilik, dan tombol WhatsApp interaktif yang diarahkan ke nomor telepon pemilik.

## 3. Konsekuensi

### Positif
* Keandalan registrasi meningkat secara signifikan dengan adanya rollback otomatis jika penyimpanan data profil gagal.
* Skema data Firestore pengguna kini konsisten dan kompatibel dengan versi sebelumnya.
* Halaman profil dan detail hewan tidak lagi menggunakan data dummy dan siap digunakan untuk skenario pengujian riil.
* Batasan validasi input mencegah pengiriman data teks yang terlalu besar ke Firestore.

### Negatif
* Aplikasi melakukan satu kali request delete tambahan ke Firebase Auth jika penulisan ke Firestore gagal (memerlukan koneksi internet stabil).
* Pencarian (`SearchFragment`) masih menggunakan data dummy lokal dan akan dihubungkan pada fase pengembangan selanjutnya.
