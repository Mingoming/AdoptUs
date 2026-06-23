# ADR 0015 - Fitur Edit Post (Ubah Postingan) dan Manajemen Pembersihan Media

* **Status:** Approved
* **Tanggal:** 23 Juni 2026
* **Penulis:** Tim Pengembang AdoptUs

## 1. Konteks

Brief tugas mensyaratkan adanya fitur **Edit Post** (Ubah Postingan) agar pengguna yang mengunggah postingan hewan peliharaan dapat menyunting kembali detail informasi hewan tersebut (seperti nama, ras, umur, tipe, deskripsi, biaya, status vaksin, dan paspor kesehatan).
Dalam implementasi sebelumnya:
1. Tidak ada mekanisme navigasi maupun tombol bagi pemilik postingan untuk mengubah data yang telah diunggah.
2. Belum ada manajemen pembersihan media lama di Supabase Storage, yang berpotensi menyisakan berkas-berkas sampah (*orphaned files*) jika pengguna mengubah gambar atau video hewan mereka dengan file baru.
3. Halaman detail postingan (`PetDetailFragment`) menerapkan penimbunan (*caching*) memori berdasarkan ID, sehingga data tidak akan dimuat ulang dari Firestore jika pengguna kembali ke halaman detail pasca melakukan pengeditan.

## 2. Keputusan

Kami memutuskan untuk menerapkan solusi berikut:

### A. Tombol Edit Berbasis Kepemilikan (Ownership Check)
* Menambahkan ImageButton `btnEdit` (ikon pensil) pada `detailTopBar` di halaman [fragment_petdetail.xml](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/res/layout/fragment_petdetail.xml).
* Di dalam [PetDetailFragment.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/fragment/Petdetailfragment.kt), kami memvalidasi apakah pengguna yang sedang login (`FirebaseAuth.getInstance().currentUser?.uid`) adalah pemilik postingan tersebut (`post.userId`). Tombol edit hanya ditampilkan (`View.VISIBLE`) jika validasi kepemilikan berhasil.

### B. Pemisahan Komponen Edit Post & Navigasi Manual
* Membuat fragment baru [EditPostFragment.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/fragment/EditPostFragment.kt) dan layout [fragment_edit_post.xml](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/res/layout/fragment_edit_post.xml) guna mematuhi *Single Responsibility Principle* (SRP) serta memisahkan alur logika pembuatan post vs penyuntingan post.
* Mengingat proyek ini tidak menggunakan auto-generation Safe Args (berkas parameter args dideklarasikan secara manual di proyek seperti `PetDetailFragmentArgs`), kami membuat berkas [EditPostFragmentArgs.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/fragment/EditPostFragmentArgs.kt) secara manual dan melakukan navigasi menggunakan objek `Bundle` standar (`R.id.action_detail_to_edit`).

### C. Pembersihan Media di Supabase Storage
* Di dalam [EditPostViewModel.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/ui/editpost/EditPostViewModel.kt), jika pengguna mengunggah media baru (`newMediaUri != null`), kami mendeteksi alamat media lama.
* Jika URL media lama merujuk pada Supabase Storage (ditandai dengan path `/adoptus-post-images/`), sistem akan mengekstrak lokasinya dan memicu fungsi `deleteMedia(oldPath)` dari `PostMediaRepository` untuk menghapus file lama secara permanen sebelum menyimpan perubahan baru ke database.

### D. Refresh Halaman Detail (Bypass Cache)
* Menambahkan parameter `forceRefresh: Boolean` pada fungsi `loadPost` di [PetDetailViewModel.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/ui/detail/PetDetailViewModel.kt).
* Mengatur pemanggilan `loadPost(args.postId, forceRefresh = true)` di `PetDetailFragment.kt` saat `onViewCreated` dipanggil. Hal ini memaksa fragment untuk mengambil ulang data terbaru dari Firestore ketika halaman dibuka kembali pasca penyuntingan.

## 3. Konsekuensi

### Positif
* Fitur Edit Post bekerja dengan baik dan mematuhi seluruh spesifikasi tugas user.
* Efisiensi ruang penyimpanan di Supabase Storage terjaga karena media lama otomatis dihapus ketika diganti.
* Halaman detail postingan selalu menampilkan data mutakhir setelah disunting tanpa perlu dimuat ulang secara manual oleh pengguna.

### Negatif
* Penambahan berkas baru ke proyek (1 layout XML, 1 Fragment, 1 ViewModel, dan 1 helper Args).
