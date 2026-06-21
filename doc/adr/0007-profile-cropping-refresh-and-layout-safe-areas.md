# ADR 0007 - Pemotongan Foto Profil, Fitur Refresh, dan Pengaturan Safe Area Layout

* **Status:** Approved
* **Tanggal:** 21 Juni 2026
* **Penulis:** Tim Pengembang AdoptUs

## 1. Konteks

Setelah integrasi Firestore pada Profil dan Detail Hewan selesai pada fase sebelumnya (ADR 0006), diidentifikasi beberapa kebutuhan perbaikan dari sisi UI/UX dan kegunaan aplikasi:
1. Tombol ganti foto profil (`btnChangePhoto`) di halaman Pengaturan belum berfungsi penuh dan hanya menampilkan pesan placeholder.
2. Pengguna membutuhkan fitur pemotongan (crop) gambar profil dengan ukuran 1:1 agar avatar seragam dan tidak terdistorsi.
3. Form input data pada `SettingFragment` memiliki tingkat keterbacaan (contrast/opacity) yang rendah terhadap latar belakang krem lembut.
4. Halaman Feed Utama (`FeedFragment`) dan Profil Pengguna (`ProfileFragment`) belum mendukung gesture tarik untuk memuat ulang data (swipe-to-refresh).
5. Beberapa elemen layout pada halaman "Add Post" dan aktivitas pemotong gambar (`UCropActivity`) bertabrakan (overlap) dengan area sistem (status bar dan tombol navigasi sistem) pada perangkat seluler.

## 2. Keputusan

Kami mengambil keputusan implementasi berikut untuk mengatasi masalah UI/UX di atas:

### A. Integrasi Pustaka UCrop dan Fitur Ganti Foto Profil
* Menambahkan pustaka pemotongan gambar `com.github.yalantis:ucrop:2.2.8` melalui repositori JitPack.
* Menerapkan launcher `ActivityResultContracts.GetContent()` untuk memicu galeri gambar, dilanjutkan dengan peluncuran `UCrop` dengan pengaturan rasio 1:1, dimensi hasil maksimal 500x500 piksel, filter area lingkaran, serta penyembunyian kontrol rotasi/skala untuk menyederhanakan alur kerja pengguna.
* Hasil pangkasan gambar disimpan di cache direktori lokal, ditampilkan sebagai preview lokal secara instan dengan Coil, lalu diunggah ke Supabase Storage menggunakan `PostMediaRepository` ketika pengguna menekan tombol "Save".

### B. Optimalisasi Deteksi Tipe Media (MIME Type)
* Memodifikasi parser tipe file di `PostMediaRepository` agar tidak hanya bergantung pada `contentResolver.getType()`.
* Logika dimodifikasi untuk mendeteksi MIME type dari file URI (`file://`) lokal hasil pangkasan UCrop menggunakan `MimeTypeMap` Android, dengan fallback ke ekstensi nama berkas (seperti `.png`, `.webp`, `.mp4`). Ini berhasil memperbaiki eror "Unable to detect media type" yang sebelumnya menggagalkan penyimpanan foto profil.

### C. Pembuatan Tema Khusus Safe Area untuk UCropActivity
* Mendeklarasikan tema khusus `Theme.AdoptUs.UCrop` berbasis `Theme.AppCompat.Light.NoActionBar` dengan atribut `<item name="android:fitsSystemWindows">true</item>`.
* Mengaplikasikan tema ini ke `UCropActivity` di `AndroidManifest.xml` untuk mencegah tumpang tindih antara tombol aksi pangkas (X dan centang) dengan status bar dan navigation bar sistem.

### D. Fitur Swipe-to-Refresh di Feed dan Profil
* Menambahkan ketergantungan pustaka `androidx.swiperefreshlayout:swiperefreshlayout:1.1.0`.
* Membungkus `RecyclerView` pada Feed dan `NestedScrollView` pada Profil dengan komponen `SwipeRefreshLayout`.
* Menyelaraskan skema warna spinner menggunakan warna oranye utama `@color/primary_orange` untuk konsistensi visual.
* Mengatur penanganan pemuatan ulang data agar tidak memicu spinner ganda (double loading spinner) dengan menyembunyikan progress bar bawaan saat gesture refresh sedang berjalan.

### E. Penyelarasan Layout dan Safe Area Add Post
* Menghilangkan warna latar belakang putih statis dan bayangan keras (`elevation`) pada bilah atas `AddPostFragment` agar membaur secara alami dengan latar belakang krem aplikasi.
* Menambahkan parameter `clipToPadding="false"` dan padding bawah `80dp` pada scroll container utama di layout "Add Post" agar semua kolom input dan tombol checklist tidak tertutup oleh tombol navigasi sistem di bagian bawah layar.

## 3. Konsekuensi

### Positif
* Pengalaman pengguna lebih dinamis dan premium dengan dukungan pangkas foto profil lingkaran (1:1) dan gesture swipe-to-refresh.
* Keandalan unggah file meningkat secara signifikan karena sistem dapat mendeteksi URI lokal maupun URI konten media.
* Konsistensi gaya desain (status bar seamless) terjaga di seluruh halaman utama dan form postingan.
* Masalah tumpang tindih elemen navigasi sistem (overlapping) telah teratasi sepenuhnya.

### Negatif
* Ukuran APK bertambah sedikit dengan masuknya dependensi pustaka `UCrop` dan `SwipeRefreshLayout`.
