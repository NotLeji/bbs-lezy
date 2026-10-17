# Dashboard UI Performance Manual Verification Checklist

Verification checklist for user testing on Intel iGPU / desktop client:

- [ ] **1. Panel Film Dibuka (Async Key Scanning)**:
  - Buka dashboard BBS (`Ctrl + D` atau tombol BBS di title screen/pause menu).
  - Pindah ke panel Films (`UIFilmPanel`).
  - Verifikasi: Daftar film langsung tampil tanpa hitch/freeze >100 ms pada render thread.

- [ ] **2. Buka Film Besar (Async Film Loading)**:
  - Di panel film, pilih film berukuran besar (misalnya `raid_indah.dat` ~483 KB atau `OP-perang-1.dat` ~129 KB).
  - Verifikasi: UI tetap responsif saat data dimuat dari disk, timeline terisi normal, dan actor dapat bergerak saat diputar tanpa visual artifact.

- [ ] **3. Scroll List Besar**:
  - Buka panel dengan daftar item besar (Films, Models dengan ribuan file, atau Texture Browser).
  - Scroll ke atas dan ke bawah dengan cepat.
  - Verifikasi: Navigasi scroll mulus tanpa stuttering atau frame drop parah.

- [ ] **4. World-Skip A/B Test (Opaque vs Translucent Dashboard)**:
  - **Konfigurasi A (Default Transluen - `0x7b000000` atau default BBS)**:
    - Buka dashboard. Dunia 3D di belakang dashboard tetap ter-render normal sesuai tingkat transparansi (predikat world-skip tidak aktif).
  - **Konfigurasi B (Solid Opaque - Alpha = 1.0)**:
    - Atur warna background dashboard menjadi solid 100% opak di settings BBS.
    - Buka dashboard. Render dunia 3D di balik dashboard ter-skip otomatis; tampilan visual dashboard identik 100% dengan kondisi sebelum optimisasi, dengan FPS meningkat.

- [ ] **5. Mutasi Data BBS & Cache Invalidation (Review Focus #1)**:
  - Buat atau simpan film baru via editor BBS.
  - Ubah nama (rename) salah satu film.
  - Hapus (delete) salah satu film tes.
  - Verifikasi: Daftar nama langsung ter-refresh seketika di panel UI tanpa data basi.

- [ ] **6. Deteksi Perubahan File Eksternal (Review Focus #2)**:
  - Salin file `.dat` atau folder baru secara manual melalui OS file explorer ke folder `run/config/bbs/` atau folder world save `bbs/films/`.
  - Kembali ke editor BBS.
  - Verifikasi: File baru otomatis terdeteksi dan muncul dalam waktu ≤2 detik (sesuai TTL cache `LezyKeyCache`).
