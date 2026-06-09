# ADR 0004 — Feed Real-time Firestore, Post Model, dan Setting

* **Status:** Approved
* **Tanggal:** 31 Mei 2026
* **Penulis:** Tim Pengembang AdoptUs

## 1. Konteks

Setelah navigasi stabil, tiga fitur utama perlu diimplementasi: Feed real-time dari Firestore, kemampuan upload post, dan halaman Setting untuk edit profil. Keputusan arsitektur dibuat berdasarkan kondisi aktual proyek (Firebase Storage belum aktif, Firestore rules masih open sampai 20 Juni 2026).

## 2. Keputusan

### 2a. State Management — Flow/StateFlow bukan LiveData
Feed menggunakan `StateFlow` + `callbackFlow` di PostRepository untuk mendukung real-time updates dari Firestore. Berbeda dengan LiveData, Flow memungkinkan Firestore `addSnapshotListener` langsung di-wrap sebagai stream yang otomatis update UI saat ada perubahan data.

### 2b. Filter status di sisi app, bukan Firestore query
Query awal menggunakan `whereEqualTo("status", "available")` + `orderBy("createdAt")` yang membutuhkan composite index Firestore. Karena index belum dibuat, query gagal dengan error `FAILED_PRECONDITION`.

**Solusi:** Hapus `whereEqualTo` dari query, ambil semua post, filter `status == "available"` di sisi app setelah data diterima.

**Trade-off:** Tidak efisien untuk data skala besar, tapi cukup untuk skala development saat ini.

### 2c. Upload foto/video — ditunda
Firebase Storage membutuhkan upgrade ke Blaze (pay-as-you-go) plan. Karena keputusan upgrade belum disepakati tim, field `mediaUrl` dikosongkan sementara. AddPost tetap berfungsi untuk data teks.

### 2d. Setting diakses dari ProfileFragment, bukan tab baru
Berdasarkan dokumen desain, tidak ada tab Setting di BottomNav. Setting diakses dari icon ⚙️ di pojok kanan atas ProfileFragment — navigasi via `action_profile_to_setting` di nav graph.

### 2e. Ganti password membutuhkan re-authentication
Firebase mensyaratkan user melakukan login ulang (re-auth) sebelum bisa update password jika sudah lama login. Error `REQUIRES_RECENT_LOGIN` ditangkap dan ditampilkan sebagai pesan yang jelas ke user.

## 3. Masalah yang Ditemui dan Solusi

### 3a. Feed error FAILED_PRECONDITION (Composite Index)
**Masalah:** Query dengan `whereEqualTo` + `orderBy` butuh composite index yang belum dibuat.
**Solusi:** Filter status di sisi app. Index bisa dibuat nanti untuk optimasi production.

### 3b. Duplicate resource `placeholder`
**Masalah:** `placeholder.xml` baru konflik dengan `placeholder.jpeg` yang sudah ada.
**Solusi:** Hapus `placeholder.xml`, pakai `placeholder.jpeg` yang sudah ada.

### 3c. Type mismatch `NO_ID` vs `NO_POSITION`
**Masalah:** `RecyclerView.NO_ID` bertipe `Long`, tidak bisa dibandingkan langsung dengan posisi yang bertipe `Int`.
**Solusi:** Ganti ke `RecyclerView.NO_POSITION` yang bertipe `Int`.

## 4. Status Saat Ini

| Fitur | Status | Catatan |
|---|---|---|
| Feed real-time | ✅ Selesai | Filter status di sisi app |
| AddPost → Firestore | ✅ Selesai | Tanpa foto sementara |
| Setting edit profil | ✅ Selesai | Load & update Firestore |
| Setting ganti password | ✅ Selesai | Handle re-auth error |
| Setting logout | ✅ Selesai | Clear session + redirect Login |
| Upload foto/video | ⏳ Pending | Tunggu Firebase Storage Blaze plan |
| Kota AddPost | ⏳ Pending | Masih hardcode "Indonesia" |
| Like button | ⏳ Pending | UI ada, logic belum |

## 5. Hal yang Perlu Diperhatikan Tim

- **Firebase Storage**: Tim perlu sepakat soal upgrade Blaze plan untuk mengaktifkan upload foto/video
- **Firestore Rules**: Rules saat ini open sampai 20 Juni 2026 — perlu diperbarui ke rules berbasis auth sebelum release
- **`btnBack` AddPost**: Masih bertipe `ImageView`, seharusnya `ImageButton` untuk aksesibilitas
