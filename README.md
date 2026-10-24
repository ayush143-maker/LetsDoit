# Let's Do It

Focus timer + tasks + insights + a focus shield that sends distracting apps back
to your home screen while a session runs. No account, no ads, no INTERNET permission.

## Get the APK (GitHub Actions)

1. Create a **private** GitHub repo and push this folder to `main`.
2. Open the repo → **Actions** → *Build APK* (it also runs on every push; or use *Run workflow*).
3. When it turns green, open the run → **Artifacts** → `LetsDoIt-apk` → download, unzip, install.

> The signing key lives in `keystore/letsdoit.jks` so every build is signed with the
> same key and updates install over each other. Keep the repo private. If you ever publish
> the app, make a new keystore and move the passwords to GitHub Secrets.

Package name is `app.letsdoit.focus` (same as the old Qwen build) but the signing key
is different, so **uninstall the old APK first**.

## Enable the focus shield

1. Open the app → **Shield** tab → *Open accessibility settings* → Let's Do It → turn on.
2. Android 13+ and the switch is greyed out (APK installed outside the Play Store):
   App info → ⋮ → **Allow restricted settings**, then repeat step 1.
3. Tick the apps you want blocked (or *Auto-pick social apps*), switch **Use focus shield** on.
4. Start a focus session. Opening a blocked app now sends you home.

## Fonts

IBM Plex Sans + Mono (SIL OFL) are downloaded by the workflow into `app/src/main/assets/fonts/`.
If a download fails the app falls back to the system font. For local builds, drop the
`.ttf` files there yourself.

## Project layout

- `app/src/main/java/app/letsdoit/focus/` – Kotlin + Jetpack Compose source
  - `Data.kt` storage, timer engine, stats · `Services.kt` timer service + accessibility service
  - `FocusScreen.kt`, `TasksScreen.kt`, `InsightsScreen.kt`, `ShieldSetupScreen.kt`, `ShieldActivity.kt`
  - `Quotes.kt` quote list · `Theme.kt` colours + fonts
- `.github/workflows/build-apk.yml` – CI build
- `store-icon-512.png` – 512px icon for store listings
