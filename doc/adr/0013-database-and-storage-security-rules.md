# ADR 0013 - Aturan Keamanan Database Firestore dan Supabase Storage

* **Status:** Approved
* **Tanggal:** 23 Juni 2026
* **Penulis:** Tim Pengembang AdoptUs

## 1. Konteks

AdoptUs merupakan aplikasi berbasis serverless menggunakan Firebase Firestore untuk database dokumen dan Supabase Storage untuk media. Karena API Client dipanggil langsung dari kode aplikasi seluler, keamanan integritas data harus ditegakkan di sisi server (*server-side rules*) agar:
1. Pengguna lain tidak bisa memodifikasi atau menghapus profil milik orang lain.
2. Bidang penting (seperti status adopsi postingan) tidak dapat diubah sembarangan oleh bukan pemilik postingan.
3. Media gambar/video yang diunggah ke storage tidak ditimpa (*upsert*) atau dihapus oleh pengguna tidak berwenang.

## 2. Keputusan

Kami mengambil keputusan implementasi arsitektur berikut:

### A. Firestore Security Rules
* Membuat berkas [firestore.rules](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/firestore.rules) pada direktori root proyek untuk version control.
* Menerapkan aturan akses:
  * **Koleksi `users`**: Publik dapat membaca data profil (`read: if signedIn()`), namun hanya pemilik yang bisa membuat atau memperbaruinya (`write: if isOwner(userId)`).
  * **Koleksi `posts`**: Hanya pemilik yang dapat membuat, memperbarui, atau menghapus postingan miliknya. Pengguna lain hanya diizinkan memperbarui bidang `likesCount` demi kelancaran fitur *toggle like*.
  * **Koleksi `adoptions`**: Pengajuan adopsi baru harus disetujui secara atomik. Perubahan status hanya diizinkan untuk pemilik postingan hewan (`ownerId`) dan hanya terbatas ke status `"approved"` atau `"rejected"`.

### B. Supabase Storage Row-Level Security (RLS)
* Membuat berkas dokumentasi SQL RLS [supabase_rls.sql](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/doc/supabase_rls.sql) di bawah folder `doc`.
* Mengamankan bucket `adoptus-post-images`:
  * Mengizinkan publik untuk mengunduh/membaca media (`SELECT`).
  * Membatasi proses unggah (`INSERT`) dan hapus (`DELETE`) media di bawah folder `posts/{uid}/*` hanya untuk user yang terautentikasi dan memiliki `uid` yang sesuai dengan nama folder (`auth.uid()`).

## 3. Konsekuensi

### Positif
* Menjamin keamanan data database dan storage dari serangan manipulasi API client secara ilegal.
* Seluruh aturan keamanan terdokumentasi dan terkontrol dalam pelacakan versi Git (*Infrastructure as Code*).

### Negatif
* Perubahan skema data di masa depan membutuhkan pembaruan rules secara bersamaan agar tidak memicu error `Permission Denied` pada aplikasi.
