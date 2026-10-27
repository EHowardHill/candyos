# The bundled-app release updater

CandyOS Experience polls GitHub once a day for new releases of the
bundled-in apps and installs them in the background. The whole system
lives in `apps/CandyExperience/src/org/candyos/experience/updates/`
— no new packages, no services outside the app, no androidx
(the app has none, so the poll is plain `HttpURLConnection` +
`org.json`).

## The pieces

| Piece | File | What it does |
|-------|------|--------------|
| Registry | `updates/TrackedApps.kt` | The list of bundled apps ↔ GitHub repos. **This is the only file you edit when you bundle a new APK.** Also holds the per-app state (`SharedPreferences`: ETag, last-seen tag, last check) and the tag/version comparison. |
| GitHub client | `updates/GithubClient.kt` | `GET /repos/<owner>/<repo>/releases/latest` with a cached `If-None-Match` ETag, and the streaming download with a SHA-256 computed on the fly. |
| Engine | `updates/UpdateEngine.kt` | Per-app pass: poll → compare → download → install. Posts one notification summarizing what happened (reuses the Testing bench's `POST_NOTIFICATIONS` grant + guard). |
| Scheduler | `updates/UpdatesScheduler.kt` | The daily `JobScheduler` job (id 1001): `setPeriodic(24h)`, `NETWORK_TYPE_UNMETERED`, `setPersisted(true)`, `setRequiresBatteryNotLow(true)`. Persisted jobs re-register at boot through `RECEIVE_BOOT_COMPLETED` — no custom boot receiver. |
| Job entry | `updates/UpdateJobService.kt` | Runs one engine pass on its executor and `jobFinished`s when done. |
| UI | `UpdatesActivity.kt` + `layout/updates_activity.xml` | "Updates" row in the Experience menu: per-app status (installed / updated / failed / not installed), last check time, and a **Check now** button that runs on any network, not just unmetered. |

## How a check goes

1. For each tracked app: fetch `releases/latest`, pick the first
   `.apk` asset. A 304 (ETag hit) means "already know this release" and
   costs one tiny request per repo per day.
2. Compare the release tag to the installed `versionName`
   (`isTagNewer`: `v1.2.3` ≡ `1.2.3`, numeric per-dot segment, lexical
   for non-numeric tails like `1.2.3-beta1`). Not newer → done.
3. Download to `cacheDir/updates/<pkg>.apk` — the download aborts if
   the stream exceeds or undershoots the size the API declared, and the
   SHA-256 must match the API's `digest` field when the API provides
   one.
4. Commit a `PackageInstaller` session
   (`MODE_FULL_INSTALL`, `USER_ACTION_NOT_REQUIRED`). CandyOS Experience
   is a platform-signed privileged system_ext app with
   `INSTALL_PACKAGES` in its privapp whitelist
   (`privapp_whitelist_org.candyos.experience.xml`), so the session
   commits **silently** — that is the point: bundled apps keep
   themselves current with no user babysitting. The temp file is
   deleted whether the commit succeeds or not.
5. `last_check` is stamped; a notification reports installs/failures.

## Adding a bundled app

1. Bundle the APK (per the vendored-apps path in
   [apps.md](apps.md) / [architecture.md](architecture.md)).
2. Add one line to `TRACKED` in `updates/TrackedApps.kt`:
   `TrackedApp("<package>", "<owner>", "<repo>")`.
3. Have the repo publish releases whose tag sorts against
   `versionName` (e.g. tags `v1.2.3` for `versionName 1.2.3`) and whose
   release assets include the `.apk`.

## Design notes

* **Why JobScheduler, not WorkManager/AlarmManager:** the app carries no
  androidx, and JobScheduler's persisted periodic jobs are exactly the
  "once a day, forever, across reboots" primitive needed. The schedule
  is also re-asserted from `MenuActivity`/`UpdatesActivity` so a
  dropped job (e.g. after a reinstall of the Experience app) never
  stays dropped.
* **Wi-Fi only, automatically:** release APKs are whole-app downloads,
  so the daily job requires an unmetered network; the manual **Check
  now** deliberately does not.
* **Untracked = untouched:** apps not in `TRACKED` (or not installed)
  are never touched — the engine skips them; the Play Store and the
  platform-signed system apps manage themselves as before.
* **Downgrades are impossible:** the tag must compare *newer* than the
  installed `versionName`, so re-publishing an old tag is a no-op.
* **Integrity:** size enforced from the API metadata, SHA-256 checked
  against the API `digest` when present (the digest field is the
  GitHub API's own; a repo that omits it still gets the size check).
  A mismatch aborts the install and leaves the installed app alone.
