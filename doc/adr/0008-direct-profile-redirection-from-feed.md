# ADR 0008 - Redirection Profile Langsung dari Feed dan Halaman Profil Dinamis

* **Status:** Approved
* **Tanggal:** 21 Juni 2026
* **Penulis:** Tim Pengembang AdoptUs

## 1. Konteks

Sebelumnya, interaksi pada Feed halaman utama (`FeedFragment`) hanya mengarahkan pengguna ke halaman detail hewan (`PetDetailFragment`). Pengguna belum dapat melihat profil pembuat postingan secara dinamis dan langsung dari feed. Selain itu, halaman `ProfileFragment` hanya dikonfigurasi secara statis untuk menampilkan profil pengguna yang sedang login saat ini (My Profile).

Perbaikan ini bertujuan untuk mengaktifkan navigasi profil langsung saat menekan avatar pemilik hewan di Feed, memuat data Firestore pengguna tersebut secara dinamis, menyembunyikan tombol pengaturan jika profil milik orang lain, serta menyediakan pintasan WhatsApp langsung dari tampilan profil.

## 2. Keputusan

Kami mengambil keputusan implementasi berikut:

### A. Navigasi Avatar Feed ke Profil Pembuat Postingan
* Menambahkan callback `onOwnerClick: (Post) -> Unit` di `FeedAdapter`. Callback ini dipicu ketika pengguna menekan `ownerAvatar` pada feed item.
* Menerapkan action navigasi `action_feed_to_profile` dan `action_search_to_profile` di `main_nav.xml` yang mengarah ke `profileFragment` dengan parameter argumen `userId: String` (nullable).
* Menangani transisi navigasi di `FeedFragment` dengan mengirimkan data `post.userId` melalui Bundle arguments.

### B. Profil Dinamis dan Penanganan Kepemilikan (Ownership)
* Memodifikasi `ProfileViewModel` agar memuat data profil dan postingan berdasarkan parameter `userId`. Jika parameter null, ViewModel secara default memuat profil pengguna aktif saat ini (`getCurrentUser()?.uid`).
* Menambahkan metode `getUserPosts(uid)` pada `PostRepository` untuk memuat postingan milik pengguna tertentu secara real-time dari Firestore.
* Di dalam `ProfileFragment.kt`'s `onResume()`, visibilitas tombol pengaturan (`btnSetting`) diatur dinamis: disembunyikan (`View.GONE`) apabila `userId` yang dimuat bukan milik pengguna aktif saat ini.

### C. WhatsApp Clickable pada Tampilan Profil
* Menambahkan event listener `setOnClickListener` ke teks WhatsApp (`tvWhatsapp`) di halaman profil.
* Jika pengguna mengklik teks WhatsApp tersebut, aplikasi akan otomatis memformat nomor telepon (menghapus karakter non-numerik, memetakan awalan `0` ke kode negara `62`), membangun URI `https://wa.me/`, dan meluncurkan intent `Intent.ACTION_VIEW` untuk membuka obrolan WhatsApp secara instan.

## 3. Konsekuensi

### Positif
* Pengalaman menjelajahi aplikasi lebih lancar karena profil pembuat postingan kini dapat dikunjungi secara langsung dari Feed utama.
* Halaman `ProfileFragment` sekarang bersifat reusable dan dinamis untuk menampilkan data pengguna manapun.
* Hubungan kontak ke pemilik hewan dipermudah dengan tautan WhatsApp langsung di dalam detail profil.

### Negatif
* Backstack navigasi bertambah panjang saat berpindah bolak-balik antara Feed dan Profil.
