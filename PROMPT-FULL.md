# The full prompt: Apple TV style, with speed tests and before/after numbers

This is the version that was tested end to end on a Xiaomi Mi TV Stick (MiTV-AESP0, Android TV 9, 1 GB RAM) on 24 Oct 2026. It measures everything before and after, runs a crash stress test and takes much longer than `PROMPT.md`. Use it when you want the numbers. Built on [CleanYourTV](https://github.com/Gathin23/cleanyourtv) (MIT) and extended by Zaid Fouad.

How to use it: open Claude Code in the folder of this repo, fill in the four lines below the first line "---" and paste everything from there down.

---

My TV: <brand and model, e.g. Xiaomi Mi TV Stick MiTV-AESP0, Android TV 9>
My dock apps, in order: <e.g. YouTube, Netflix, Shahid, Google Play Store>
Apps I use and want kept: <e.g. Crunchyroll, Kick>
My football team for the live card (or "none"): <e.g. Real Madrid>

**Language:** speak to me in Arabic (simple and friendly; Iraqi-leaning is fine). Keep technical words in English (ADB, launcher, dock, Wi-Fi, prompt). If I write to you in another language, answer in that language.

I want to debloat my Android TV, make it as fast as it can be, and replace its launcher with an Apple TV style home
screen, using the kit in the `kit/` folder of this repo. Work on the real device, step by step, verify
everything with screenshots pulled from the TV, and prove speed and stability with numbers before and after. Ask me
before anything that depends on my preferences. Once the scope is agreed, work through it without asking at every
step, and tell me plainly when something didn't work.

## 0. Set up the workspace
- Clone the repo at the exact version the kit was built on:
  `git clone https://github.com/Gathin23/cleanyourtv.git && cd cleanyourtv && git checkout eb0131a32fd8567086f39002f1782f7c1b078ca6`
  (the kit's patch applies to that commit; on a newer one, try it and fix any conflicts by hand).
- Read the repo's `AGENTS.md` and `PROMPT.md` and follow their safety rules: they win over anything below.
- Apply the kit: `git apply ../kit/launcher-apple-tv.patch`, copy `../kit/madrid-live` to
  `addons/madrid-live`, and copy `../kit/tools/*` into a new `work/` folder in the repo.
- Install what is missing, without asking me for each free tool: Python 3 venv with `tools/requirements.txt`
  (the ADB client, no adb binary needed), JDK 17, Android SDK platform 34 + build-tools 34.0.0
  (macOS: `brew install openjdk@17` and `brew install --cask android-commandlinetools`, then `sdkmanager`).
  Tell me before installing anything that costs money (nothing here should).

## 1. Connect
- Find the TV on my network: `dns-sd -B _androidtvremote2._tcp` (macOS) then resolve it, or the ARP table, or ask me
  for the IP. Connect over ADB on port 5555 with `tools/tv.py --probe`.
- On macOS the first connection can fail with "No route to host": that is the Local Network permission prompt. Ask me
  to allow it, then retry once. Don't debug it further.
- If port 5555 stays closed, the TV may only offer Wireless debugging (Google TV): look for `_adb-tls-connect._tcp`,
  ask me for the pairing code and port under Developer options > Wireless debugging, pair, connect, then `adb tcpip 5555`.
- Tell me before the "Allow USB debugging?" prompt appears on the TV, so I can tick "Always allow" and press OK.

## 2. Back up first
- Save the full package list, disabled-package list, getprop, all settings, `wm size`/`wm density`, the HOME
  activities and `dumpsys wifi | grep -o 'cname=[^ ]*'` into `backup/` before changing anything.
- Write `work/restore.sh` that re-enables everything you disable, gives Home back to the stock launcher and sets
  Private DNS back to automatic.

## 3. Measure the "before" (do this before changing anything)
- Right now, the whole-device test: `TV_HOST=<ip> tools/venv/bin/python work/measure.py --out work/measure-before.json`.
  It finds every launchable app by itself (so apps I add later are covered) and measures cold open, warm open,
  close-to-Home and stutter inside each app, the Settings / Wi-Fi / Network / About / Display / Sound / Apps / Date
  screens, internet latency and free RAM. Run long measurements in the background and keep going with other work.
- Note the system animation scales it prints. If they are 1.0, offer me 0.5 (`settings put global
  window_animation_scale 0.5`, same for `transition_animation_scale` and `animator_duration_scale`); if they are
  already 0.5, say so and don't claim a gain from it.

## 4. Debloat
- List installed packages and sort them: manufacturer telemetry/analytics, manufacturer content and ad services,
  background services I probably don't need, preloaded apps, streaming apps. Check `examples/` for my exact model
  first (the kit has `../kit/examples/mi-tv-stick-aesp0-debloat.txt` for the Mi TV Stick).
- Disable with `tools/disable.sh <list>` (it refuses core packages and keeps the input/Settings handlers), never
  uninstall. Small batches, screenshot between them.
- Before touching streaming apps, the voice assistant, Chromecast, Miracast or the update service, ask me with one
  checklist question and disable only what I don't tick.
- Before disabling setupwraith, check `cname=`: if it owns the saved Wi-Fi network, keep it enabled (disabling it
  deletes the network and drops ADB). It is also needed for Chromecast.
- After the last batch, reboot once and compare `pm list packages -d` with the list.
- Offer ad-blocking Private DNS (`dns.adguard-dns.com`), say it doesn't block YouTube ads, check an app still loads.

## 5. Launcher: install and take Home
- Check the SDK (`getprop ro.build.version.sdk` ≥ 26) and that the TV accepts APKs.
- Set `launcher/app/src/main/res/values/config.xml`: `dock_apps` = my dock apps in order (find package names with
  `pm list packages`; ask me only if one is ambiguous), `hidden_apps` (Live Channels on a set with no tuner), and
  keep the kit's switches on: `apple_focus`, `apple_header`, `match_panel` (off if I said "none" for the team),
  `cinematic_home`, `dock_glass`, `icon_only_buttons`, `focus_overshoot=false`, `show_inputs_pill` (false on a stick
  or box with no HDMI inputs).
- First install it with `apple_focus` and `apple_header` set to false (the repo's own look) and record the launcher
  "before": `tools/venv/bin/python tools/bench.py --out-dir work/bench-before` (about 6 minutes, in the background).
  Then switch them back on for every later install.
- Install with `work/install-launcher.sh <ip>`. It sets `HOME_KEY_SERVICE=1`, which Xiaomi remotes need (their Home
  button sends ALL_APPS). Confirm with `dumpsys activity activities | grep mResumedActivity` and with my real remote:
  open an app, ask me to press Home. An injected key is not proof.
- Push the Apple theme: copy `themes/scenes/wallpapers/03-moon-lake.jpg` (or a 1920×1080 photo I give you) to
  `../kit/theme-apple/wallpaper.jpg`, then `tools/theme.sh ../kit/theme-apple`
  (it sets the wallpaper, 6 columns and the Top Shelf strength).

## 6. Apple TV look: compare and fix
- Look at the real thing before judging: Apple's tvOS 26 home screen (Apple Newsroom, "Apple TV brings a beautiful
  redesign", June 2025) and the Apple-style launchers canvasOS (github.com/getisolated/canvasOS) and LiquiTvOs
  (github.com/purnavikas/cleanyourtv, branch liquitvos). Put them next to my TV's screenshots and list the gaps.
- Target: a full-screen picture behind everything; a glass dock at the bottom with about 6 tiles; no box or ring
  around the focused app; the focused tile grows ~13%, lifts with a soft shadow, catches a glare and settles from a
  slight tilt; a small clock pill and round icon buttons top right; the app name only under the focused tile.
- Keep it light: no real blur, no video. Measure smoothness after every visual change with `work/jankprobe.sh`
  (20 presses on Home; janky frames should stay under ~2%; run it 3 times, the first run after a restart is noisy).
  What we measured on a Mali-450 GPU: the tile's `withLayer()` must stay (without it every frame took ~30 ms);
  `cinematic_art` 0 made it worse (keep 30); the moving light sweep (`Sheen.SWEEP`) made Home stutter, so it ships off.
  Turn it on only if the jank probe stays low on a stronger TV.

## 7. Live football card and goal alerts (skip if I said "none")
- The kit's companion app `addons/madrid-live` fetches ESPN's free public feed (no key) and the launcher only reads
  it, so the launcher keeps no internet permission and can't freeze or crash because of the network.
- For a team other than Real Madrid: find its ESPN id (for example
  `https://site.api.espn.com/apis/site/v2/sports/soccer/eng.1/teams` lists Premier League teams with ids), then edit
  `Match.MADRID_ID` and the league slugs in `Espn.java` (e.g. `eng.1`, `uefa.champions`, `eng.fa`), the app label,
  and the demo names in `Demo.java`. Run its unit tests (`./gradlew testReleaseUnitTest assembleRelease`).
- Install it, grant the overlay: `appops set com.zaid.madridlive SYSTEM_ALERT_WINDOW allow`, then wake it once:
  `am broadcast --include-stopped-packages -n com.zaid.madridlive/.ControlReceiver -a com.zaid.madridlive.REFRESH`.
  Check `content query --uri content://com.zaid.madridlive.match/next` returns the next match.
- Test without a live match: `... -a com.zaid.madridlive.DEMO --es kind live` (fake live score for 2 minutes) and
  `--es kind goal` (the goal banner over any app). Screenshot both.
- Mixed Arabic + English text: the kit wraps English names, scores and minutes in Unicode isolates (U+2066/U+2069)
  and shows scores in Western digits; keep that, or "1 – 0" shows as "0 – 1" on an Arabic TV.

## 8. Speed and stability evidence
- Pre-compile the apps I use most: `cmd package compile -m speed -f <package>`. A large app can take minutes: run it
  with a long timeout (the ADB client's default 120 s can cut it), and confirm with
  `dumpsys package <pkg> | grep status=` that it reads `speed`. The launcher's own compile can time out too: check it.
- Stress test: `monkey -p com.gathin.tvhome -c android.intent.category.HOME --pct-touch 0 --pct-motion 0
  --pct-trackball 0 --pct-syskeys 0 --pct-appswitch 0 --pct-anyevent 0 --pct-nav 70 --pct-majornav 30 --throttle 60
  -s 42 2000`, after `logcat -b crash -c`; then the crash log and `am_anr` events for the launcher and the team app
  must be empty. (Without `-c android.intent.category.HOME` monkey finds no activity and quits at once.)
- Run `tools/bench.py` and `work/measure.py` again (`--out work/measure-after.json`), then
  `work/measure.py --compare work/measure-before.json work/measure-after.json`, and give me one before/after table:
  key response, focus settle time, Home stutter, Home cold start, return to Home, RAM, and per app open/close.
  Be honest: app open times swing 20–40% between runs on a 1 GB device; say what got worse and why.
- Benchmarks and stress tests force-stop the launcher, which unbinds the Home-key helper. Afterwards check
  `dumpsys accessibility | grep -o "services:{[^}]*}"` is not empty; if it is, reboot once and check again.
- Android 9 logs a harmless `AlarmManager` "removeLocked: Shouldn't for UID=1000" warning each time a test
  force-stops a system app (Settings). It is the test, not your app.

## 9. Finish
- Give me `work/restore.sh`, `work/install-launcher.sh`, the measurement files and a README that explains how to
  re-enable any app, change the dock order, change the wallpaper, test the goal alert and re-run the speed test.
- Close ADB: ask me to revoke the USB debugging authorisations, switch off the Developer options master switch at the
  top (on the Mi TV Stick, switching off USB debugging alone leaves 5555 open) and restart. Then wait until the TV is
  back on the network and run `tools/adb-closed.py <ip>`: only "refused the connection" proves it is closed;
  "did not answer" just means the TV is still restarting.
- Stop every build daemon (`./gradlew --stop`) and background job you started.
