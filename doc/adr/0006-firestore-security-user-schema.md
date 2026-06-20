# ADR 0006 - Firestore Security dan Schema User Canonical

* **Status:** Approved for implementation, pending production rollout
* **Tanggal:** 20 Juni 2026
* **Penulis:** Tim Pengembang AdoptUs

## 1. Konteks

ADR 0004 mencatat bahwa Firestore development rules terbuka sampai 20 Juni 2026. ADR 0005 juga mencatat ketidakkonsistenan schema: register menulis field snake_case, sedangkan Setting memakai camelCase. Kondisi ini berisiko membuka akses data dan membuat profil lama tidak terbaca konsisten.

## 2. Keputusan

### 2a. Schema canonical

Dokumen `users/{uid}` memakai field:

```text
uid, username, fullName, photoUrl, bio, city,
whatsapp, role, createdAt, updatedAt
```

`uid`, `role`, dan `createdAt` immutable. Email tetap menjadi data Firebase Authentication dan dihapus dari dokumen profil Firestore.

### 2b. Backward compatibility

Register baru hanya menulis schema canonical. `User.fromMap()` masih membaca `id`, `full_name`, `photo_url`, dan `created_at` selama masa transisi. Setting menulis field canonical tanpa menghapus field legacy secara langsung.

### 2c. Rollout bertahap

Urutan rollout:

1. Deploy transitional rules yang menutup akses anonymous dan menerapkan ownership.
2. Rilis client canonical-write dan dual-read.
3. Backup Firestore production.
4. Jalankan migration dry-run.
5. Migrasikan dokumen user secara idempotent.
6. Verifikasi seluruh dokumen canonical.
7. Deploy strict rules.

Strict rules tidak boleh dideploy sebelum verification menghasilkan nol dokumen invalid.

### 2d. Migrasi production

Migration menggunakan Firebase Admin SDK dan full document replacement untuk menghapus field legacy serta email. Script memerlukan credential eksplisit di luar emulator. Write production juga memerlukan `--confirm-production`, sehingga command biasa tidak dapat mengubah production tanpa sengaja.

### 2e. Aturan akses

* `users` dan `posts` hanya dapat dibaca user terautentikasi.
* User hanya dapat membuat atau memperbarui profilnya sendiri.
* Penghapusan profil langsung dari client ditolak.
* Post hanya dapat dibuat, diubah, atau dihapus pemiliknya.
* Unknown collections menggunakan deny-by-default.

## 3. Konsekuensi

### Positif

* Rules terbuka atau berbatas tanggal dapat diganti tanpa memutus data lama.
* Schema register dan Setting menjadi konsisten.
* Email tidak lagi terekspos melalui dokumen profil.
* Migration bisa di-dry-run, diulang, dan diverifikasi.
* Security contract dapat diuji melalui emulator.

### Negatif

* Client lama yang masih menulis schema legacy tidak kompatibel dengan strict rules.
* Production rollout membutuhkan backup, service-account credential, dan monitoring manual.
* Username global uniqueness belum dijamin.
* Profile masih dapat dibaca seluruh user terautentikasi karena feed/detail membutuhkan public owner data.

## 4. Rollback

Jika migration gagal, hentikan write dan restore export bila data rusak. Jika strict rules menolak operasi valid, deploy kembali transitional rules, reproduksi denial dalam emulator test, lalu perbaiki strict rules. Rules terbuka tidak boleh digunakan sebagai rollback.

## 5. Ditunda

UserRepository, username uniqueness, likes, adoption workflow, Firebase Storage, dan Cloud Functions berada di luar P0.
