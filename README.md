# Let's Do It

Focus timer + tasks + insights + on-device Screen Habits usage reports + a focus shield that stops selected distractions while a session runs. No account, no ads, no INTERNET permission.

## Get the APK (GitHub Actions)

1. Open the repo → **Actions** → *Build APK* (it also runs on pushes to `main`, `master`, and `ayush-screen-habits`, on pull requests, or via *Run workflow*).
2. When it turns green, open the run → **Artifacts** → `LetsDoIt-apk` → download, unzip, install.

> The signing key lives in `keystore/letsdoit.jks` so every build is signed with the same key and updates install over each other. Keep the repo private. If you ever publish the app, make a new keystore and move the passwords to GitHub Secrets.

Package name is `app.letsdoit.focus` (same as the old Qwen build). If Android reports a signing conflict with an older build, uninstall that older APK first.

## Enable Screen Habits

1. Open **Habits** in the bottom navigation.
2. Tap **Open Usage Access settings**.
3. Select **Let's Do It** and enable usage access, then return to the app.

The report shows estimated app foreground time for today, a seven-day chart, and the most-used apps for the past seven days. Usage events are processed on-device; the app does not add internet access or upload usage data. Android controls the permission and how much recent event history it makes available.

## Enable the focus shield

1. Open the app → **Shield** tab → *Open accessibility settings* → Let's Do It → turn on.
2. Android 13+ and the switch is greyed out (APK installed outside the Play Store): App info → ⋮ → **Allow restricted settings**, then repeat step 1.
3. Tick the apps you want blocked (or *Auto-pick social apps*), switch **Use focus shield** on.
4. Start a focus session. Opening a blocked app now shows the shield screen.

## Fonts

IBM Plex Sans + Mono (SIL OFL) are downloaded by the workflow into `app/src/main/assets/fonts/`. If a download fails the build fails rather than silently shipping with the system font. For local builds, drop the `.ttf` files there yourself.

## Project layout

- `app/src/main/java/app/letsdoit/focus/` – Kotlin + Jetpack Compose source
  - `Data.kt` storage, timer engine, stats · `Services.kt` timer service + accessibility service
  - `FocusScreen.kt`, `TasksScreen.kt`, `InsightsScreen.kt`, `ScreenHabits.kt`, `ShieldSetupScreen.kt`, `ShieldActivity.kt`
  - `Quotes.kt` quote list · `Theme.kt` colors + fonts
- `.github/workflows/build-apk.yml` – CI build
- `store-icon-512.png` – 512px icon for store listings
