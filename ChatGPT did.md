# ChatGPT did

This branch was created as an exact copy of `deep-focus-onboarding` before changes were made on `ayush-screen-habits`.

## Screen Habits implementation

- Added a native **Screen Habits** tab using Android Usage Access.
- Added a seven-day chart, today's total, daily average, and top-app foreground-time report.
- Kept usage processing on-device; no backend, account, or network permission was added.
- Added an APK workflow trigger for pushes to the feature branch.
- Added explicit usage permission and set app version to 1.4.0 (version code 6).

## Fixes after device feedback

- Fixed app-duration labels to format milliseconds locally instead of accidentally passing minute values into another millisecond formatter.
- Switched historical reports to Android's daily UsageStats aggregates, and use today's events as a live fallback so totals are less likely to remain at zero while app statistics are delayed.
- Added a dynamic **Straight talk** card that reports what the current usage numbers actually show, in English, without pretending missing data is zero usage or inventing estimates.
- Reduced usage-history refresh frequency to avoid rescanning history every minute.
- Bumped app version to 1.4.1 (version code 7), so the corrected APK can be distinguished from the earlier build.

## Notes

Screen time depends on Android usage history and Usage Access being enabled. Estimates may vary slightly by device. Existing Focus, Tasks, Insights, and Shield features were retained.

Implementation credit: ChatGPT  
Co-author credit: OpenAI


## Monthly analysis and roast update (Claude)

- Added a month-by-month calendar with per-date foreground-time totals and a selected-day app contribution breakdown.
- Added an evidence-based, humorous English Straight Talk roast, with a daily-average comparison and no fabricated usage when records are missing.
- Added on-device daily usage history persistence and a five-minute in-memory report cache to avoid recomputation on every tab revisit.
- Added editable display labels for apps, with YouTube shown as Instagram by default for the requested joke; app tracking remains keyed to the real package.
- Saved rename changes now update the visible report immediately instead of clearing it and waiting for another refresh.
- Bumped version to 1.4.2 (version code 8).

Latest follow-up implementation credit: Claude  <br>Co-author: Claude <noreply@anthropic.com>
