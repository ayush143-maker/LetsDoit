# Let's Do It

Focus timer + tasks + insights + on-device Screen Habits usage reports + a focus shield that stops selected distractions while a session runs. No account, no ads, no INTERNET permission.

## Get the APK (GitHub Actions)

1. Open the repo → **Actions** → *Build APK* (it runs on pushes to `main`, `master`, and `ayush-screen-habits`, on pull requests, or via *Run workflow*).
2. When it turns green, open the run → **Artifacts** → `LetsDoIt-apk` → download, unzip, install.

> The signing key lives in `keystore/letsdoit.jks`. Keep the repo private. If you publish the app, use a protected keystore and move passwords to GitHub Secrets.

Package name is `app.letsdoit.focus`. This update increments the app to version `1.4.1` (version code `7`).

## Screen Habits

1. Open **Habits** in the bottom navigation.
2. Tap **Open Usage Access settings**.
3. Select **Let's Do It** and enable usage access, then return to the app.

The report shows today's app foreground time, a seven-day chart, a daily average, the most-used launchable apps, and a dynamically generated **Straight talk** insight based on the usage numbers. Historical usage uses Android's daily UsageStats; today's foreground events are also used to reduce lag in current-day totals. Usage remains on-device and is not uploaded. Values can vary with Android version and device behavior.

## Enable the focus shield

1. Open the app → **Shield** tab → *Open accessibility settings* → Let's Do It → turn on.
2. Android 13+ and the switch is greyed out (APK installed outside the Play Store): App info → ⋮ → **Allow restricted settings**, then repeat step 1.
3. Tick the apps you want blocked (or *Auto-pick social apps*), switch **Use focus shield** on.
4. Start a focus session.

## Fonts

IBM Plex Sans + Mono (SIL OFL) are downloaded by the workflow into `app/src/main/assets/fonts/`.

## Project layout

- `app/src/main/java/app/letsdoit/focus/` – Kotlin + Jetpack Compose source
  - `Data.kt` storage, timer engine, stats · `Services.kt` timer service + accessibility service
  - `FocusScreen.kt`, `TasksScreen.kt`, `InsightsScreen.kt`, `ScreenHabits.kt`, `ShieldSetupScreen.kt`, `ShieldActivity.kt`
- `.github/workflows/build-apk.yml` – CI build
- `store-icon-512.png` – 512px icon for store listings


## Screen Habits monthly analysis

- The Habits tab includes a month-by-month calendar. Tap a date to inspect total recorded screen time and the apps that contributed most to that day.
- Straight Talk generates a playful English roast from the selected day total and its recorded-day average. It does not invent numbers when data is unavailable.
- App labels are editable for display only. For fun, YouTube appears as Instagram by default; underlying package tracking remains unchanged.
- Month reports are cached in memory for five minutes to avoid recomputing when switching tabs. Daily package-duration history is saved locally for up to 366 days when the tracker is opened and data is available. Older history is not retroactively guaranteed.
- This update is version 1.4.2 (version code 8).
