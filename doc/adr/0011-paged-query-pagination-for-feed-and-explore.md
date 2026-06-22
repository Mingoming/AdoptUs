# ADR 0011 - Pemuatan Data Terpaginasi (Pagination) Feed Utama dan Jelajah

* **Status:** Approved
* **Tanggal:** 23 Juni 2026
* **Penulis:** Tim Pengembang AdoptUs

## 1. Konteks

Sebelumnya, daftar feed utama memuat seluruh postingan dari Firestore secara sekaligus. Seiring bertambahnya jumlah postingan di database:
1. **Beban Memori**: Memuat ratusan postingan sekaligus akan membebani RAM perangkat dan memperlambat rendering UI.
2. **Kuota Reads**: Melakukan query seluruh dokumen memicu pembacaan Firestore yang boros.

Aplikasi membutuhkan mekanisme pemuatan data per halaman (page-by-page loading) yang dipicu secara otomatis ketika pengguna menggulir mendekati akhir daftar.

## 2. Keputusan

Kami mengambil keputusan implementasi arsitektur berikut:

### A. Query Terpaginasi di Repository
* Menambahkan fungsi `getFeedPostsPaginated` di [PostRepository.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/data/repository/PostRepository.kt) yang membatasi hasil query menggunakan `.limit(pageSize)`.
* Menggunakan token kursor `.startAfter(lastDocument)` untuk menentukan titik awal halaman berikutnya.

### B. Scroll Listener & ViewModel State
* Merefaktor [FeedViewModel.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/ui/feed/FeedViewModel.kt) agar mendukung pemuatan per halaman serta melacak status `isLastPage` dan `isLoadingMore`.
* Menambahkan scroll listener pada `RecyclerView` di [FeedFragment.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/fragment/FeedFragment.kt) dan grid postingan jelajah di [SearchFragment.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/fragment/SearchFragment.kt) untuk memicu `loadNextPage()` saat item tersisa tinggal 2-3 baris.

### C. Firestore Composite Index
* Mendefinisikan Composite Index pada berkas [firestore.indexes.json](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/firestore.indexes.json) untuk mengurutkan data berdasarkan `status ASCENDING` dan `createdAt DESCENDING` secara atomik di tingkat server.

## 3. Konsekuensi

### Positif
* Penggunaan memori dan bandwidth jaringan menjadi sangat efisien dan konstan.
* Pemuatan daftar postingan terasa sangat responsif bagi pengguna.

### Negatif
* Diperlukan pembuatan Composite Index secara eksplisit di Firebase Console agar query pagination tidak menghasilkan error `FAILED_PRECONDITION`.
