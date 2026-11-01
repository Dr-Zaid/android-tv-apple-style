# The prompt: an Apple TV style home for your Android TV (fast mode)

**How to use it:** open Claude Code in the folder of this repo and paste everything below the line. Or just say "start": `CLAUDE.md` sends Claude here.
Built on [CleanYourTV](https://github.com/Gathin23/cleanyourtv) (MIT) and extended by Zaid Fouad: Apple TV focus and layout, a live football card with goal alerts, and a speed-first flow. This is the short version. The full version with speed tests and before/after numbers is `PROMPT-FULL.md`.

---

**Language:** speak to me in Arabic (simple and friendly; Iraqi-leaning is fine). Keep technical words in English (ADB, launcher, dock, Wi-Fi, prompt). If I write to you in another language, answer in that language.

You are setting up my Android TV or Google TV with the Apple TV style home screen from this kit (the `kit/` folder next to you).
The goal is a finished, working TV with as few steps as possible. Work on the real device, tell me plainly when something did not work, and never ask me the same thing twice.
Do NOT run benchmarks, stress tests or before/after reports unless I ask for "numbers" (then follow steps 3 and 8 of `PROMPT-FULL.md`). Take one screenshot after the launcher is installed and one at the end. Do not re-check what a tool already confirmed.

## 1. Ask me first, in ONE message (I answer once, "default" is fine)
1. **My TV:** brand and model, or "find it for me" once you are connected.
2. **My dock:** up to 6 apps in order, and the other apps I want to keep.
3. **My look.** Pick one:
   - **a) Apple TV** (the kit's default).
   - **b) Darker and calmer.**
   - **c) My own taste:** I describe the mood, colours, wallpaper (or give you a photo), how many columns, glass dock or flat, clock and buttons on or off. You map that onto what the kit really lets you change (the `config.xml` switches, the wallpaper, the columns, the Top Shelf strength) and tell me plainly if something I want is not possible, with the closest option.
4. **A football team** for the live card, or "none".
5. **What may go:** voice assistant, Chromecast, Miracast, system updates, streaming apps. Give me one checklist; disable only what I do not tick.

Ask the five questions in Arabic, with this wording (translate the choices if I answer in another language):
1. شنو تلفزيونك؟ (الماركة والموديل، أو كلي "دوّر عليه إنت" لمن تتصل.)
2. شنو تطبيقات الـ dock مالتك (لحد 6، بالترتيب)؟ وشنو التطبيقات الثانية اللي تريد تبقيها؟
3. شنو الشكل اللي تريده؟ أ) Apple TV (الافتراضي). ب) أغمق وأهدأ. ج) ذوقك أنت: كلي المزاج والألوان والخلفية (أو دز صورة)، وكم عمود، والـ dock زجاجي لو مسطّح، والساعة والأزرار.
4. شنو فريقك للكارت المباشر؟ أو كلي "none".
5. شنو يصير ينشال؟ المساعد الصوتي، Chromecast، Miracast، تحديثات النظام، تطبيقات البث. أشّر اللي تريد تبقيه.

Then repeat my choices back in five lines and start. No more questions unless something blocks you.

## 2. Set up the workspace
- Clone at the exact version the kit was built on: `git clone https://github.com/Gathin23/cleanyourtv.git && cd cleanyourtv && git checkout eb0131a32fd8567086f39002f1782f7c1b078ca6` (the patch applies to that commit; on a newer one, try it and fix conflicts by hand).
- Read that repo's `AGENTS.md` and `PROMPT.md` and follow their safety rules. They win over anything here.
- Apply the kit: `git apply ../kit/launcher-apple-tv.patch`, copy `../kit/madrid-live` to `addons/madrid-live` (only if I picked a team), and copy `../kit/tools/*` into a new `work/` folder.
- Install what is missing without asking me for each free tool: a Python 3 venv with `tools/requirements.txt`, JDK 17, Android SDK platform 34 + build-tools 34.0.0 (macOS: `brew install openjdk@17` and `brew install --cask android-commandlinetools`, then `sdkmanager`). Tell me before installing anything that costs money (nothing here should).

## 3. Connect
- Find the TV: `dns-sd -B _androidtvremote2._tcp` (macOS), the ARP table, or ask me for the IP. Connect over ADB on port 5555 with `tools/tv.py --probe`.
- macOS can fail the first time with "No route to host": that is the Local Network permission. Ask me to allow it and retry once. Do not debug it further.
- If port 5555 stays closed (Google TV), use Wireless debugging: find `_adb-tls-connect._tcp`, ask me for the pairing code and port, pair, connect, then `adb tcpip 5555`.
- Tell me just before the "Allow USB debugging?" prompt appears so I can tick "Always allow".

## 4. Back up first (this is the only slow-ish part, keep it)
- Save the package list, the disabled-package list, getprop, all settings, `wm size`/`wm density`, the HOME activities and `dumpsys wifi | grep -o 'cname=[^ ]*'` into `backup/`.
- Write `work/restore.sh`: re-enable everything you disabled, give Home back to the stock launcher, set Private DNS back to automatic.

## 5. Clean up (two batches, one reboot)
- Sort the installed packages: manufacturer telemetry, manufacturer ads and content services, background services, preloaded apps. Check `../kit/examples/` for my exact model first (`mi-tv-stick-aesp0-debloat.txt` is the Mi TV Stick list).
- Disable with `tools/disable.sh <list>` (it refuses core packages). Never uninstall. Two batches, then one reboot, then compare `pm list packages -d` with your list.
- **Wi-Fi guard:** before disabling `setupwraith`, check `cname=`. If it owns the saved Wi-Fi network, keep it enabled (disabling it deletes the network and drops ADB; Chromecast needs it too).
- Offer ad-blocking Private DNS (`dns.adguard-dns.com`) and say it does not block YouTube ads.

## 6. Install the launcher
- Check `getprop ro.build.version.sdk` is 26 or higher.
- Edit `launcher/app/src/main/res/values/config.xml` from my answers: `dock_apps` in my order (find package names with `pm list packages`), `hidden_apps` (Live Channels on a set with no tuner), and the look switches: `apple_focus`, `apple_header`, `match_panel` (off if I said "none"), `cinematic_home`, `dock_glass`, `icon_only_buttons`, `focus_overshoot=false`, `show_inputs_pill` (false on a stick with no HDMI inputs). Option b or c: change them to match what I asked.
- Install with `work/install-launcher.sh <ip>`. It sets `HOME_KEY_SERVICE=1`, which Xiaomi remotes need (their Home button sends ALL_APPS). Confirm with `dumpsys activity activities | grep mResumedActivity`, then ask me to open an app and press Home on my real remote. An injected key is not proof.
- Push the theme: copy `themes/scenes/wallpapers/03-moon-lake.jpg` (or the 1920×1080 photo I gave you) to `../kit/theme-apple/wallpaper.jpg`, then `tools/theme.sh ../kit/theme-apple` (wallpaper, 6 columns, Top Shelf strength).
- Keep it light: no real blur and no video, because these TVs have weak GPUs. On a Mali-450 the tile's `withLayer()` must stay, and the moving light sweep (`Sheen.SWEEP`) stays off.
- Check that the Home-key helper is still bound: `dumpsys accessibility | grep -o "services:{[^}]*}"` must not be empty. If it is, reboot once.

## 7. Football card (skip if I said "none")
- The companion app `addons/madrid-live` reads ESPN's public feed and the launcher only reads the companion, so the launcher needs no internet permission and cannot freeze because of the network.
- For a team other than Real Madrid: find its ESPN id (`https://site.api.espn.com/apis/site/v2/sports/soccer/eng.1/teams` lists a league's teams), then edit `Match.MADRID_ID` and the league slugs in `Espn.java` (for example `eng.1`, `uefa.champions`), the app label and the demo names in `Demo.java`. Build with `./gradlew assembleRelease`.
- Install it, then: `appops set com.zaid.madridlive SYSTEM_ALERT_WINDOW allow` and `am broadcast --include-stopped-packages -n com.zaid.madridlive/.ControlReceiver -a com.zaid.madridlive.REFRESH`.
- Keep the kit's Unicode isolates around English names, scores and minutes so "1 – 0" does not flip on an Arabic TV.
- One quick check only: `... -a com.zaid.madridlive.DEMO --es kind goal` shows the goal banner. Then stop.

## 8. Finish
- Give me: `work/restore.sh`, `work/install-launcher.sh`, and five lines on how to re-enable an app, change the dock order, change the wallpaper and test the goal alert.
- **Close ADB** (port 5555 is open to anything on my network): ask me to revoke the USB debugging authorisations, switch off the **Developer options master switch** at the top (on the Mi TV Stick, turning off USB debugging alone leaves 5555 open), and restart. When the TV is back, run `tools/adb-closed.py <ip>`: only "refused the connection" proves it is closed.
- Stop every build daemon (`./gradlew --stop`) and any background job you started.
