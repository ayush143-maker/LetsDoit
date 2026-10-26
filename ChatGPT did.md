# ChatGPT did

This branch was created as a copy of `deep-focus-onboarding` at its existing head, then updated on the separate `ayush-screen-habits` branch.

## Implemented

- Added a native **Screen Habits** tab using Android's Usage Access and `UsageStatsManager`.
- Added a seven-day screen-time chart, today's usage, seven-day daily average, and a ranked list of apps by recent foreground time.
- Added a clear Usage Access settings entry point and an on-screen explanation that report data stays on-device.
- Kept usage queries local; no network permission, backend, account, or remote analytics was added.
- Added the tab to the existing bottom navigation and added a matching custom navigation icon.
- Declared the protected Android usage-stats permission for system settings, incremented app version to 1.4.0 (versionCode 6), and enabled the existing APK workflow on pushes to this feature branch.
- Updated the README with setup instructions and behavior notes.

## Notes

Screen time is estimated from Android foreground usage events and depends on OS-provided event history and Usage Access being enabled. The existing focus timer, tasks, Insights, and Shield code was retained.

Implementation credit: ChatGPT  
Co-author credit: OpenAI
