# Real Madrid Live

A companion with no screen of its own. It keeps Real Madrid's live, just-finished or next match available to the
launcher through a content provider, and pops a goal banner over whatever is playing. There is no launcher
activity, so it never shows up as an app tile.

- **Data** is ESPN's public JSON (no key, no account): the team schedule and the live scoreboard for LaLiga, the
  Champions League, the Copa del Rey and the Supercopa.
- **The match shown** is the live one if there is one; otherwise one that finished less than 3 hours ago (the
  result); otherwise the next one across all competitions.
- **Banner** (top right, rounded dark glass, both crests, Arabic event word): a goal for either team, a red card,
  kick-off and full time. Banners are queued one at a time, stay 8 seconds, and can never take focus or touches.

## When it runs

| What | When |
|---|---|
| Schedule refresh | on boot, after the app is updated, every 6 hours (inexact alarm), and when the provider is queried with data older than 6 hours (a background kick, the query is never blocked) |
| Kick-off alarm | one exact alarm at kick-off minus 10 minutes for the next match |
| Live service | foreground service (low-priority notification) from kick-off minus 10 min; reads the live score every 30 s; stops 10 min after the match is over |
| Failures | the last good data stays; schedule refresh retries after 1, 2, 4 ... up to 30 minutes, live polls slow down from 30 s to 5 minutes |

Nothing runs between matches except the 6-hourly alarm.

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | ESPN's JSON and the team crests |
| `RECEIVE_BOOT_COMPLETED` | refresh the schedule and re-arm the kick-off alarm after a reboot |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` | the live service that polls the score during a match |
| `SYSTEM_ALERT_WINDOW` | the banner over other apps; without it the banner is silently skipped |
| `SCHEDULE_EXACT_ALARM` | the exact kick-off alarm (only matters on Android 12+; otherwise an inexact alarm is used) |

No account, no telemetry, no location.

## Build and install

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
cd addons/madrid-live && ./gradlew --no-daemon testReleaseUnitTest assembleRelease && ./gradlew --stop
# app/build/outputs/apk/release/app-release.apk  (signed with the debug key, like the other add-ons)
adb install -r app/build/outputs/apk/release/app-release.apk
```

## Grant the overlay and wake it up

```bash
adb shell appops set com.zaid.madridlive SYSTEM_ALERT_WINDOW allow
# A freshly installed app is "stopped" and gets no broadcasts until something starts it. Kick the first refresh:
adb shell am broadcast --include-stopped-packages -n com.zaid.madridlive/.ControlReceiver -a com.zaid.madridlive.REFRESH
```

(The launcher's first query of the provider also starts it, and a reboot arms everything.)

## Try it without a live match

```bash
# banners: kind = goal | red | kickoff | fulltime
adb shell am broadcast -n com.zaid.madridlive/.ControlReceiver -a com.zaid.madridlive.DEMO --es kind goal
# fake live row in the provider for 2 minutes (Real Madrid 1-0 Barcelona, 67'), then it goes back to normal
adb shell am broadcast -n com.zaid.madridlive/.ControlReceiver -a com.zaid.madridlive.DEMO --es kind live
# refresh the schedule right now
adb shell am broadcast -n com.zaid.madridlive/.ControlReceiver -a com.zaid.madridlive.REFRESH
# read the row
adb shell content query --uri content://com.zaid.madridlive.match/next
```

## Provider contract

Authority `com.zaid.madridlive.match`, exported, read-only, no permission.

`content://com.zaid.madridlive.match/next` returns 0 or 1 row with exactly these columns, in this order:

| Column | Type | Meaning |
|---|---|---|
| `state` | TEXT | `pre`, `in` or `post` |
| `kickoff_ms` | INTEGER | kick-off, UTC epoch milliseconds |
| `competition` | TEXT | "LaLiga", "Champions League", "Copa del Rey", "Supercopa" |
| `home`, `away` | TEXT | team names |
| `home_abbr`, `away_abbr` | TEXT | e.g. `RMA`, `BAR` |
| `home_score`, `away_score` | INTEGER | `-1` before kick-off |
| `clock` | TEXT | `67'`, `HT`, `FT`; empty before kick-off |
| `madrid_home` | INTEGER | 1 if Real Madrid is the home side, else 0 |
| `venue` | TEXT | stadium |
| `fetched_ms` | INTEGER | when this data was fetched, UTC epoch ms |
| `last_event` | TEXT | e.g. `Goal · Vinícius Jr. 67'`, `Red card · ...`, `Kick-off`, `Full time`; may be empty |
| `last_event_ms` | INTEGER | when that event was seen (0 if none) |

- `content://com.zaid.madridlive.match/logo/home` and `/logo/away` open the current row's crests as PNG files
  (`openFile`, mode `r`), at most 160 px, downloaded once per team and cached in the app's files directory.
- After every change to the row the app calls `ContentResolver.notifyChange(.../next)` and sends the broadcast
  `com.zaid.madridlive.MATCH_UPDATED` (implicit; the launcher registers a runtime receiver for it).
- Querying with an old schedule (over 6 hours) starts a background refresh and answers at once with what is stored.

## Tests

`./gradlew testReleaseUnitTest` runs plain JVM tests against real saved ESPN responses
(`app/src/test/resources/fixtures`): match selection, parsing, kick-off and alarm arithmetic, backoff, the goal /
card / kick-off / full-time diffing (each event announced once, also across a service restart), and the provider
column contract. The live fixture `summary_live_synthetic.json` is the finished Real Madrid 4-1 Sevilla summary
edited to 3-0 at 67'; `scoreboard_live.json` is a real in-play LaLiga entry.
