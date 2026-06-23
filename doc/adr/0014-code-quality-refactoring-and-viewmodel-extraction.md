# ADR 0014 - Refactoring Kualitas Kode, Ekstraksi ViewModel, dan Testability

* **Status:** Approved
* **Tanggal:** 23 Juni 2026
* **Penulis:** Tim Pengembang AdoptUs

## 1. Konteks

Berdasarkan hasil audit kualitas kode komprehensif pada proyek AdoptUs, diidentifikasi beberapa isu arsitektur dan keterbacaan kode (*code smells*) yang perlu dibenahi sebelum aplikasi didistribusikan ke fase produksi:
1. **Pelanggaran MVVM**: Beberapa Fragment (`AddPostFragment`, `SearchFragment`, dan `SettingFragment`) serta `FeedAdapter` mengakses instance Firestore secara langsung untuk mengambil/menyimpan data.
2. **Ketiadaan ViewModel**: `AddPostFragment` mengelola seluruh logika bisnis (upload media, lookup kota pemilik, pembuatan post) secara mandiri di dalam fragment tanpa ViewModel.
3. **ViewModel Untestable**: Semua ViewModel menggunakan konstruktor kosong tanpa parameter, sehingga mustahil melakukan *mocking* repositori/dependensi dalam pengujian unit (*unit testing*).
4. **Duplikasi Kode**: Logika formatting tautan WhatsApp diulang di 3 tempat berbeda (`FeedAdapter` pada 2 skema pemuatan dan `ProfileFragment`).
5. **God Adapter**: Metode `FeedViewHolder.bind()` memiliki panjang 120+ baris dengan tanggung jawab ganda (binding UI, inisialisasi media player, WhatsApp redirection, status like, dll).
6. **Risiko NullPointerException (NPE)**: Pemanggilan Toast menggunakan parameter `context` yang nullable dari fragment lifecycle.

## 2. Keputusan

Kami memutuskan untuk menerapkan pembenahan arsitektur dan kualitas kode sebagai berikut:

### A. Isolasi Lapisan Data & Ekstraksi ViewModel
* Membuat [AddPostViewModel.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/ui/addpost/AddPostViewModel.kt) untuk menampung seluruh logika bisnis pembuatan postingan baru.
* Membuat [SearchViewModel.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/ui/search/SearchViewModel.kt) untuk memisahkan logika pencarian dan pagination explore.
* Menghapus semua referensi langsung Firestore (`FirebaseFirestore.getInstance()`) pada fragment/adapter dan mengalihkannya melalui fungsi baru di `AuthRepository` (`getUserProfile`, `updateUserProfile`, `getAllUsers`).

### B. Constructor Injection Ramah Testing
* Menambahkan parameter konstruktor berupa instansi repositori dengan *default value* (nilai default) pada semua ViewModel. Kotlin secara otomatis menghasilkan konstruktor tanpa parameter di bytecode JVM agar tetap kompatibel dengan inisialisasi bawaan Android SDK (`by viewModels()`), namun pengembang tetap dapat menyuntikkan *mock* repositori saat unit testing.

### C. Pembersihan Kode & Modularisasi UI
* Membuat extension function kustom `String.formatToWaUrl()` di dalam [WhatsAppUtils.kt](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/java/com/example/adoptus/util/WhatsAppUtils.kt) untuk memusatkan formatting wa.me.
* Mendekomposisi metode `FeedViewHolder.bind()` menjadi fungsi privat: `bindMedia()`, `bindOwnerInfo()`, dan `bindLikeButton()`.
* Mengganti seluruh parameter Toast dari `context` ke `requireContext()`.
* Memindahkan array statis jenis hewan ke resource [arrays.xml](file:///c:/Amanta/Kuliah/Mobile/AdoptUs/AdoptUs/app/src/main/res/values/arrays.xml).
* Menambahkan listener `setOnItemReselectedListener` pada `BottomNavigationView` di `MainActivity` yang memanggil fungsi `refreshFeed()` milik `FeedFragment` (melakukan scroll ke posisi 0 dan memuat ulang data feed postingan terbaru seperti TikTok).

## 3. Konsekuensi

### Positif
* Lapisan antarmuka (UI) steril dari kueri langsung database (Firestore).
* Seluruh ViewModel kini 100% *testable* dan siap untuk penulisan berkas unit test.
* Meningkatkan readability dan keterawatan kode (*maintainability*) secara drastis melalui pengurangan kompleksitas fungsi dan penghapusan duplikasi.

### Negatif
* Meningkatkan jumlah kelas/berkas Kotlin di dalam proyek (penambahan 2 ViewModel baru dan 1 kelas utilitas).
