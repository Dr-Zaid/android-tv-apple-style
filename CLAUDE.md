# Instructions for Claude Code

This repository is a kit that gives an Android TV or Google TV an Apple TV style home screen, built on CleanYourTV.

Talk to the user in Arabic (simple and friendly; technical words stay in English) unless they write in another language. The main README is Arabic (`README.md`), the English one is `README.en.md`.

When the user opens this folder in Claude Code and says "start", "go", or pastes nothing at all:

1. Read `PROMPT.md` (the fast flow) and follow it from step 1. Ask the user's questions in ONE message, then work without asking again.
2. Only if the user asks for numbers, speed tests or a before/after report, use `PROMPT-FULL.md` instead.
3. Safety rules that always apply:
   - Back up first and write a restore script before changing anything on the TV.
   - Disable packages, never uninstall. Never disable core Android packages. Small batches.
   - Follow CleanYourTV's own `AGENTS.md` and `PROMPT.md` once cloned: they win over this file.
   - Tell the user before anything that costs money or installs something they did not expect.
   - At the end, close ADB and prove port 5555 refuses connections.
4. Do not commit, push or publish anything on the user's behalf.
5. Do not add club crests, app logos or Apple artwork to the repository.
