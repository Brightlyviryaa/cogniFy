# CogniFy: Your Personal AI Companion

**CogniFy** adalah aplikasi web interaktif yang memungkinkan Anda berinteraksi dengan model bahasa AI canggih seperti ChatGPT. Dibangun menggunakan OpenAI API, CogniFy menawarkan pengalaman percakapan yang alami dan informatif.

## Fitur Utama

- **Percakapan Alami:** Berinteraksi dengan AI seolah-olah sedang berbicara dengan teman.
- **Multibahasa:** Dukung berbagai bahasa untuk komunikasi yang lebih luas.
- **Kustomisasi:** Sesuaikan pengalaman Anda dengan pengaturan yang fleksibel.
- **Riwayat Chat:** Simpan percakapan untuk referensi di masa mendatang.
- **AI Vision:** Analisa gambar yang kamu upload (terbatas).
- **Talk to AI:** Dengan teknologi Speech to Text dan Text to Speech memberikan pengalaman berbicara secara langsung dengan AI.

## Kelompok 6 - CogniFy: ChatGPT Clone with Official API

| Nama                  | NIM         |
| --------------------- | ----------- |
| Brightly Virya        | 00000068227 |
| Malvin Wijaya         | 00000080948 |
| Anastasia Br Sidebang | 00000083222 |
| Vanesa Dwi Lestari    | 00000071818 |

## Berkontribusi

Kami sangat senang menyambut kontribusi dari siapa saja! Berikut cara kamu bisa berkontribusi pada proyek ini:

1. **Fork** repositori ini.
2. **Buat branch** baru untuk fitur, perbaikan bug, atau peningkatan yang ingin kamu tambahkan. Misalnya, `feature/multi-user`, `bugfix/typo-in-readme`.
3. **Commit** perubahanmu dengan pesan commit yang jelas dan ringkas.
4. **Push** branch kamu ke repositori fork kamu.
5. **Buat pull request**. Jelaskan dengan detail perubahan yang kamu buat dan alasannya.

**Pedoman Kontribusi:**

- **Ikuti gaya coding** yang ada dalam proyek ini.
- **Tulis test cases** untuk memastikan perubahan yang kamu buat tidak merusak fitur yang sudah ada.
- **Dokumentasikan** kode kamu dengan jelas.

**Terima kasih** atas kontribusimu!

## ️ Teknologi yang Digunakan

- **Android Studio (Kotlin):** Platform utama pengembangan aplikasi, memastikan performa dan kompatibilitas Android.
- **Firebase Firestore:** Database NoSQL yang fleksibel untuk penyimpanan data real-time.
- **Firebase Storage:** Penyimpanan file media dengan integrasi langsung ke aplikasi.
- **Firebase Authentication:** Mengelola otentikasi pengguna dengan aman dan efisien.
- **OpenAI API:** Menyediakan akses ke model bahasa AI canggih yang mendukung berbagai fitur seperti generasi teks, terjemahan, dan banyak lagi.

## Cara Mengunduh `google-services.json` dari Firebase

Untuk dapat menjalankan aplikasi **CogniFy** dengan konfigurasi Firebase yang benar, Anda perlu mengunduh file `google-services.json` untuk proyek ini. Berikut adalah langkah-langkahnya:

1. **Masuk ke Firebase Console**: Buka [Firebase Console](https://console.firebase.google.com/) dan masuk menggunakan akun Google Anda.

2. **Pilih Proyek CogniFy**: Di Firebase Console, pilih proyek **CogniFy** yang telah dibuat oleh tim. Nama proyeknya adalah **cognify-22a99**.

3. **Buka Pengaturan Proyek**:

   - Setelah masuk ke proyek, arahkan ke sudut kiri atas dan klik ikon **Gear (Pengaturan)**.
   - Pilih **Project settings** atau **Pengaturan Proyek** dari menu dropdown.

4. **Scroll ke Bagian "Your apps"**:

   - Di halaman **Project settings**, scroll ke bawah hingga menemukan bagian **Your apps**.
   - Di sini, Anda akan melihat daftar aplikasi yang telah terdaftar di proyek ini.

5. **Unduh `google-services.json`**:

   - Temukan aplikasi **CogniFy** yang terkait dengan Android (biasanya teridentifikasi dengan logo Android dan nama paket `com.example.cognify`).
   - Klik tombol **Download `google-services.json`** untuk mengunduh file konfigurasi.

6. **Tempatkan File `google-services.json` di Proyek Android Studio**:

   - Salin file `google-services.json` yang sudah diunduh.
   - Di proyek Android Studio Anda, paste file ini ke dalam folder `app/`.

7. **Pastikan File Berada di `.gitignore`**:

   - Untuk menjaga keamanan, pastikan `google-services.json` sudah ada di file `.gitignore` agar tidak ter-_commit_ ke repositori.

8. **Selesai**: Setelah file `google-services.json` berada di folder `app/`, Anda sudah siap untuk menjalankan aplikasi dengan konfigurasi Firebase yang benar.

Jika mengalami kendala dalam mengikuti langkah-langkah ini, silakan hubungi anggota tim untuk mendapatkan bantuan.

---

Dokumentasi ini memberikan panduan jelas agar setiap anggota tim dapat mengunduh `google-services.json` mereka sendiri, sehingga konfigurasi Firebase dapat berjalan dengan baik di perangkat masing-masing tanpa berbagi file sensitif melalui repositori.
