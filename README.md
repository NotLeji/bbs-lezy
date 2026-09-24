# BBS Lezy

<p align="center"><img src="bbs-lezy.webp" alt="BBS Lezy" width="500"></p>

---

BBS Lezy adalah addon untuk membantu proses pembuatan konten Minecraft Leji dengan target video seperti Grox, Reff, Remanrhn, dll.

Addon ini dirancang untuk scene berskala besar dan kemudahan editing: batas render model (LOD) untuk ribuan actor, manajemen panel replay massal, **export video 2 track audio terpisah** (klip BBS + suara Minecraft), **pembacaan langsung berbagai format audio** (.mp3, .m4a, .opus, .flac, dll) tanpa konversi, serta perbaikan workflow lainnya. Semua fitur dapat dikonfigurasi langsung dari editor BBS tanpa perlu ngoprek file config secara manual.

## Fitur

**1. Batas render model (LOD)**

Nggak semua model replay dirender setiap frame — hanya yang terdekat dengan kamera yang masuk hitungan. Berguna banget pas scene punya ribuan actor sampai GPU menangis. Model yang terpilih untuk di-hide dimanipulasi lewat runtime value BBS, jadi keyframe animasi nggak rusak. Pas rendering/export video, tinggal dimatiin biar semua model tergambar.

**2. Panel replay**

- **Select all replays** — pilih semua replay di film, termasuk yang ada di dalam folder tertutup (select-all bawaan BBS cuma yang terlihat di layar).
- **Select same model** — pilih semua replay yang model-nya sama dengan seleksi sekarang. Cocok untuk ambil semua hasil duplicate farm dalam satu kali klik.
- **Duplicate to total** — angka yang dimasukkan adalah **total** copy di seluruh seleksi, bukan per-replay. Misal: pilih 3 replay, input 150, hasilnya 150 copy (50 per replay), bukan 450. Tiap replay dapat kategori sendiri (`Duplicates N`) biar gampang dikontrol atau dihapus.
- **Tombol scroll ▲▼** — lompat instant ke atas atau ke bawah daftar replay, tanpa animasi. Berguna pas daudarnya udah ribuan baris.

**3. Audio export & codec**

- **Separate audio tracks (2 track audio terpisah)** — saat export video dengan opsi bawaan BBS **audio** dan **minecraft sounds** dua-duanya aktif, hasil video membawa **dua track audio terpisah** (Track 1: audio klip film BBS, Track 2: efek suara/SFX Minecraft) bukan satu track campuran. Setiap track dipaksa stereo 2-channel standar (AAC 192k) sehingga langsung rapi saat diimpor ke software editing (Premiere Pro, DaVinci Resolve, CapCut, Vegas Pro, dll). File WAV sementara otomatis dibersihkan setelah mux selesai.
- **Multi-codec audio langsung & dikenali di Pick Audio** — BBS kini bisa langsung membaca `.mp3`, `.m4a`, `.aac`, `.opus`, `.wma`, `.alac`, `.ape`, `.flac`, `.aif/.aiff`, dan `.ac3`. File-file ini langsung terdeteksi di menu "Pick audio...", bisa dipreview waveform-nya, diedit (offset, durasi, volume), dicut/split di timeline, dan dirender layaknya WAV bawaan. Decode berjalan on-demand via ffmpeg, sehingga file asli di disk tidak pernah diubah atau dikonversi.
- **Import file audio tanpa konversi** — file audio yang di-drag & drop ke dalam BBS langsung disalin **apa adanya** (byte-identical), tidak lagi dire-encode paksa menjadi WAV mono. Khusus file video (`.mp4`), audionya tetap diekstrak otomatis seperti biasa.
- **Opsi auto-open folder saat import** — toggle di pengaturan untuk mematikan perilaku bawaan BBS yang selalu membuka File Explorer setiap kali file di-drop ke game (default: mati).

**4. Perbaikan dari fork BBS**

## Dokumentasi

### Requirement

| Item      | Versi                                        |
| --------- | -------------------------------------------- |
| Minecraft | 1.20.1                                       |
| Java      | 17+                                          |
| Fabric    | Loader 0.16.14, Fabric API 0.92.1+1.20.1     |
| BBS FS    | 2.7-1.20.1                                   |
| Sodium    | 0.5.8 (sudah include)                        |
| Iris      | Opsional (shader belum ditest di dev client) |

### Install

1. Download file `.jar` dari [Releases](../../releases) (nggak perlu git clone).
2. Drop ke folder `mods` seperti addon Fabric pada umumnya.

### Setting LOD

Ada tiga setting, bisa diubah dari dua tempat:

**Lewat settings screen BBS** (ikon gerigi → kategori BBS Lezy):

| Setting             | Range  | Keterangan                                                                                                                                              |
| ------------------- | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Enable render limit   | on/off | Nyalain/matemin seluruh fitur batas render.                                                                                                             |
| Max rendered models   | 0–2000 | Jumlah model yang tetap dirender per frame. Sisanya di-hide. **0 = mati** (semua dirender).                                                             |
| Focus distance        | 0–256m | Prioritas model di sekitar jarak ini dari kamera. **0 = model terdekat** yang menang. Bisa dipakai untuk ngeliat replay jauh tanpa naikin render limit. |
| Separate audio tracks | on/off | Hasil export bawa dua track audio terpisah (klip BBS + suara Minecraft). Cuma ngaruh pas opsi export BBS **audio** dan **minecraft sounds** dua-duanya nyala. Mati = satu track campuran kayak biasa. |
| Open folder on import | on/off | Otomatis buka File Explorer ke folder tujuan saat drag & drop file ke BBS. Default: **mati** (tidak otomatis buka folder). |

**Lewat toolbar preview film editor** — klik ikon mata (👁, sebelah tombol motion path) untuk buka popup: toggle On/Off, slider Render Limit, slider Focus Distance. Perubahan otomatis kesimpan ke `bbslezy.json`.

File setting-nya ada di `<folder config BBS>/bbs/settings/bbslezy.json`.

### Build dari source

```bash
sh ./gradlew build
```

Hasilnya ada di `build/libs/bbs-lezy-<versi>.jar`.

> Catatan: BBS-nya sendiri harus sudah ter-publish ke maven local dulu (`sh ./gradlew publishToMavenLocal` dari folder BBS). Addon ini dikunci ke BBS 2.7-1.20.1 — versi BBS beda bikin dev environment aneh.

### Catatan teknis

- **Mixin UI bersifat fail-safe**: `bbslezy.mixins.json` pake `required: false` + `defaultRequire: 0`. Kalau BBS internal berubah dan mixin gagal, addon tetap jalan — cuma fitur panel replay dan audio yang ilang, fitur LOD tetap aman karena lewat API resmi. Target mixin baru: `VideoExportSession` (two-track export), `AudioReader` (codec), `ToWAVImporter`/`WAVImporter` (no-conversion import), `UISoundOverlayPanel` (pick audio), `UIScreen` (drag-import auto open folder toggle).
- **Restore override hanya dilakukan di `RENDER_AFTER` dan `SHUTDOWN`**, bukan di render pass biasa, karena shadow dan name tag digambar setelahnya.
- **Budget dihitung sebagai squared distance** — nggak ada `sqrt` dan nggak ada alokasi `Vec3d` per form, biar murah pas ribuan actor.
- Form yang terkunci ke kamera (`anchor` punya target) nggak ikut di-cull, sama seperti behavior bawaan BBS.
- Editor click tetap bisa select actor yang lagi di-cap — pass picking di-skip dari culling.

## License

MIT — bebas dipake dan dimodifikasi. Bosan dengan bug? Pull request terbuka.
