# BBS Lezy — AGENT RULES

always use caveman and ponytail skill

Add-on BBS FS: cap jumlah model yg dirender per frame (yg terdekat ke kamera menang),
biar scene ribuan actor gak membebani GPU selama pengerjaan.

1. **Selalu `git commit` setelah selesai mengerjakan task.** Tak ada pengecualian: setiap
   perubahan yang sampai ke `src/`, `build.gradle`, `*.json`, atau dokumen ini langsung
   di-commit di repo ini setelah task selesai. Jangan menumpuk perubahan tidak ter-commit.
2. **Jangan sentuh `bbsrc/`.** Repo BBS punya git repo sendiri dan kontraknya sendiri. Bila
   perlu publish ulang BBS, build dari sana tanpa mengubah file-nya (lihat COMMANDS).
3. **Add-on hanya boleh menyentuh kontrak `mchorse.bbs_mod.api` + `mchorse.bbs_mod.api.client`.**
   Satu pengecualian: **dua client mixin UI** (`bbslezy.mixins.json`, `required: false`,
   `defaultRequire: 0`) karena BBS 2.7 gak punya event hook untuk context menu / toolbar panel
   replay. Pengecualian ini dicatat eksplisit di `bbssrc/ADDONS.md` baris 8-12: reach ke luar
   `api/` = pecah **silent di game**, bukan build error. Setiap update BBS, mixin ini adalah
   hal pertama yg harus di-test manual.
4. **Restore override visible hanya di `FilmEvents.RENDER_AFTER` / `SHUTDOWN`**, bukan di
   `FormRenderEvents.AFTER` — shadow dan name tag digambar setelahnya.
5. **Sebelum yield, build harus jalan**: `sh ./gradlew build` dari sini.

./
├── src/main/java/
│   ├── bbslod/                # engine package (history: id lama, jangan rename)
│   │   ├── BBSLod.java        # common entrypoint (bbs-addon), MOD_ID = "bbslezy"
│   │   ├── BBSLodClient.java  # client entrypoint: lang + settings + engine wiring
│   │   ├── LodSettings.java   # settings: enabled + render_limit
│   │   └── LodEngine.java     # ranking jarak + budget + override visible
│   └── bbslezy/
│       └── ui/LezyReplayActions.java  # aksi panel replay: select + duplicate math (stateless)
│           └── mixin/client/
│               ├── ReplayListMixin.java       # context menu: select-all / same-model / dupe-total
│               └── ReplaysListPanelMixin.java # toolbar: tombol scroll top/bottom instant
├── src/main/resources/
│   ├── fabric.mod.json              # id bbslezy, depends bbs >=2.7-1.20.1, mixins ref
│   ├── bbslezy.mixins.json          # required:false, defaultRequire:0 — fail-safe
│   └── assets/bbslezy/strings/en_us.json  # label settings + menu replay
├── build.gradle, settings.gradle, gradle.properties  # dikunci ke BBS 2.7
├── bbs-publish-init.gradle     # work-around Gradle 9 publish BBS (lihat COMMANDS)
└── .gitignore
```

## CONVENTIONS

- Add-on bus (`EventBus`) hanya menemukan `@Subscribe` di **class entrypoint**. Handler di
  class lain (mis. `LodEngine`) kagak ketemu — register Fabric events dari `BBSLodClient`.
  `LezyReplayActions` di-panggil langsung dari mixin, bukan lewat bus.
- `visible.setRuntimeValue(...)` adalah satu-satunya cara hide form tanpa clobber keyframe
  BBS sendiri (yang juga lewat runtimeValue).
- Build file `build.gradle` selain blok repositories/dependencies sodium adalah verbatim
  dari `docs/addon-template/build.gradle`. Jangan tambah konfigurasi tanpa alasan.
- Ranking dihitung di `FilmEvents.RENDER_AFTER` (sudah tau semua jarak form) lalu dipakai
  frame depan — jadi budget selalu telat 1 frame. Itu sengaja: full sort lebih murah daripada
  partial-select manual, dan 1 frame lag gak kelihatan.
- Budget disimpan sebagai **squared distance** (`double`) — gak ada sqrt, gak ada alloc
  `Vec3d` per form. `before()` bandingin squared, `onRenderAfter` hitung squared manual.
- `touched` (IdentityHashMap) melacak form yg di-override engine; restore via
  `clearOverrides` tiap RENDER_AFTER + SHUTDOWN.
- **Mixin UI hanya boleh appends, gak boleh replaces.** `UIElement.context(Consumer)` itu
  additive — inject di tail constructor nambah consumer kedua yg jalan setelah punya BBS.
  `@Inject` ke `<init>` TAIL, shadow `@Final public` field doang, target class BBS di
  `mchorse.bbs_mod.ui.film.replays.*` (internal, bukan kontrak).
- `LezyReplayActions` stateless: terima list + film, ubah selection, lalu panggil
  `refreshReplayList` milik UIReplayList supaya row rebuild. Gak simpan state per-panel
  apapun (UI panel di-recreate tiap dashboard dibuka).

## ANTI-PATTERNS

- Jangan simpan state per-form di static field tanpa reset per controller —
  `clearOverrides` wajib jalan tiap RENDER_AFTER, kagak boleh bocor ke film lain.
- Jangan restore di `FormRenderEvents.AFTER` (terlalu cepat, shadow/nametag belum gambar).
- Jangan cull form yang `form.anchor.get().hasTarget()` (camera-locked) — sama dengan
  precedent `BaseFilmController.isCulled`.
- Jangan skip pass picking: editor click harus tetap bisa select actor yg lagi di-cap.
  Guard `context.isPicking()` di `before()` buat itu.
- Jangan ubah `gradle.properties` versi selain dari `bbsrc/gradle.properties`.

## COMMANDS

```bash
# Publish BBS ke maven local (jalankan DARI bbsrc/, TANPA ubah file bbsrc):
sh ./gradlew publishToMavenLocal --init-script "C:/Users/Administrator/App/bbs-lod-1.20.1/bbs-publish-init.gradle" --no-daemon
# ^ work-around: Gradle 9 fail kalau duplicate jar entry; api/AGENTS.md ada di dua source
#   set BBS. Init script set duplicatesStrategy=EXCLUDE di semua task Jar.

# Build add-on (dari root project ini):
sh ./gradlew build --no-daemon

# Versi (kunci dari `bbsrc/gradle.properties`): minecraft 1.20.1, yarn 1.20.1+build.10,
# loader 0.16.14, fabric-api 0.92.1+1.20.1, sodium mc1.20.1-0.5.8.
# BBS terpublish sebagai `mchorse:bbs:2.7-1.20.1` (versi = mod_version + "-" + mc_version).
# Dependensi mod `bbs >=2.7-1.20.1`. `BBSApi.requireVersion(MOD_ID, 2)` — API v2 punya
# `RegisterL10nEvent` + `FilmEditEvents` + hooks editor lain; `BBSApi.VERSION = 2` di 2.7.

# Dependencies report:
sh ./gradlew dependencies --configuration runtimeClasspath --no-daemon
```
## NOTES

- `bbsrc/` adalah symlink ke `App/bbs-fs-F6-Fix`; wrapper resolve ke path fisik tapi
  build project yang sama. Project ini ada di root `bbs-lod-1.20.1`, dan `bbsrc` adalah symlink di dalamnya — jangan di-commit (sudah di-ignore).
- Dev client jalan TANPA Iris (shader kagak ketest); Sodium sudah include.
- Settings client ada di `run/config/bbs/settings/bbslezy.json` — juga editable via settings
  screen BBS. Default: enabled true, render_limit 100. `render_limit` 0 = mati (semua render).
- Yang di-cull: root form milik controller (lewat `controller.getEntities()`). Body parts
  ikut parent karena kagak punya entri sendiri di map itu.
- Verifikasi behavioral hanya bisa manual di scene film asli: spawn ribuan actor, atur
  `render_limit`, liat yg jauh menghilang + yg dekat tetap.
- **Lang file HARUS di `assets/bbslezy/assets/strings/en_us.json`** — `registerAddon` bikin
  source pack baca dari `assets/<modid>/assets`, bukan `assets/<modid>/`. Kalau salah path,
  log bilang "Failed to load bbslezy:strings/en_us.json" terus label balik jadi raw key.
- Panel replay (fitur no.2): context menu punya 3 entry tambahan — **Select all replays**
  (semua replay di film, termasuk yg di folder tertutup; built-in select-all cuma row visible),
  **Select same model** (semua replay yg model-nya sama dengan seleksi: identity = model id
  untuk ModelForm, fallback `toData()` untuk form lain), **Duplicate to total** (input = TOTAL
  copy di seluruh seleksi, BUKAN per-replay: select 3 + input 150 = 150 copy, 50 per replay,
  bukan 450. Semua copy masuk kategori `Duplicates N` terpisah biar gampang kontrol/hapus).
- Tombol scroll **▲▼** di toolbar replay list: instant jump ke atas/bawah (set scroll langsung,
  gak animate). Bergunyi pas ribuan replay.
- Verifikasi panel replay hanya manual: buka film editor → replay panel → klik kanan, cek 3
  menu entry + tombol scroll. Mixin fail-safe: `required:false` + `defaultRequire:0` berarti
  BBS internal ganti → mixin skip, add-on tetap jalan (fitur no.2 aja yg ilang, no.1 aman).
- **`getSelectedReplays()` return fresh copy tiap call.** Jangan mutate list itu buat ganti
  selection — gak ada yg berubah di layar. Set lewat `selection.setAll(entries)` (entry =
  `ReplayListEntry` dari `getList()`), lalu `refreshReplayList()`. Ini bug yg peran kejadian:
  "select same model" kelihatan gak melakukan apa2 padahal selection copy doang yg berubah.
