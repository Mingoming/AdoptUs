# ADR 0009 - Implementasi Alur Adopsi (Apply/Approve/Reject) dan Papan Inbox Masuk

* **Status:** Approved
* **Tanggal:** 22 Juni 2026
* **Penulis:** Tim Pengembang AdoptUs

## 1. Konteks

Sebelumnya, aplikasi AdoptUs belum memiliki fitur utama yang memungkinkan pengguna mengajukan permohonan adopsi secara langsung kepada pemilik hewan. Pengguna hanya dapat menghubungi via tautan WhatsApp. Hal ini membatasi ekosistem adopsi digital karena status hewan tidak diperbarui secara otomatis setelah diadopsi, dan pemilik hewan tidak memiliki papan terpusat untuk meninjau penawaran adopsi dari para peminat.

Kami memerlukan mekanisme pengajuan adopsi yang aman, pencegahan request ganda, serta antarmuka Inbox masuk bagi pemilik hewan untuk mengelola pengajuan tersebut secara real-time.

## 2. Keputusan

Kami mengambil keputusan implementasi berikut:

### A. Skema Koleksi `adoptions`
* Membuat koleksi baru `/adoptions/{adoptionId}` di Firestore dengan field:
  * `adoptionId` (String)
  * `postId` (String)
  * `petName` (String)
  * `adopterId` (String)
  * `adopterName` (String)
  * `ownerId` (String)
  * `status` (String: `"pending"`, `"approved"`, `"rejected"`, `"cancelled"`)
  * `createdAt` dan `updatedAt` (Timestamp)

### B. Validasi & Pengajuan Adopsi (Apply)
* Di dalam [FeedFragment](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/fragment/FeedFragment.kt), tombol Apply diintegrasikan dengan validasi:
  * Mencegah pengguna mengadopsi peliharaannya sendiri (`post.userId == currentUid`).
  * Mengecek ke database (`checkPendingAdoption`) untuk mendeteksi apakah pelamar memiliki aplikasi berstatus `"pending"` sebelumnya demi mencegah spam pengajuan.
  * Menampilkan `AlertDialog` konfirmasi sebelum mengirimkan data.

### C. Antarmuka Inbox Masuk (Incoming Requests)
* Menambahkan tombol amplop surat (`btnInbox`) pada top bar [ProfileFragment](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/fragment/ProfileFragment.kt) yang hanya muncul jika pengguna membuka halaman profilnya sendiri (`isOwnProfile`).
* Membuat [InboxFragment](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/fragment/InboxFragment.kt) dan [InboxAdapter](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/ui/inbox/InboxAdapter.kt) untuk menampilkan daftar masuk secara real-time berdasarkan query `ownerId == currentUid` dengan in-memory sorting berdasarkan tanggal pembuatan (`createdAt`).

### D. Alur Persetujuan (Approve / Reject)
* Menggunakan batch transaction (`db.runBatch`) pada [PostRepository](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/data/repository/PostRepository.kt) saat status pengajuan diubah menjadi `"approved"` untuk memastikan:
  * Status pengajuan pada dokumen `/adoptions/{adoptionId}` diubah menjadi `"approved"`.
  * Status postingan terkait di `/posts/{postId}` diubah menjadi `"adopted"` secara atomik, sehingga postingan tersebut tersembunyi dari feed utama dan tidak bisa diajukan adopsi lagi.

## 3. Konsekuensi

### Positif
* Siklus hidup adopsi hewan kini terkelola sepenuhnya di dalam aplikasi (End-to-End).
* Transaksi batch menjamin integritas data (tidak ada status hewan yang gantung jika update adopsi berhasil namun update post gagal).
* Proteksi ganda mencegah spam pengajuan dan memperkuat keamanan logika bisnis.

### Negatif
* Penambahan koleksi baru `adoptions` meningkatkan jumlah pembacaan/penulisan Firestore secara keseluruhan.
