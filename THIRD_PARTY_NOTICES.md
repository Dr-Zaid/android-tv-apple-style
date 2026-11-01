# Third-party notices

This project is built on and next to other people's work. Credit and licences are below.

## What the MIT license in `LICENSE` covers

The MIT license in `LICENSE` covers the original work in this repository: the prompts
(PROMPT.md, PROMPT-FULL.md, CLAUDE.md), the documentation, the screenshots, and
everything under kit/ that was written for this project (the Apple TV launcher
patch kit/launcher-apple-tv.patch, the companion app kit/madrid-live, the tools
in kit/tools, the theme in kit/theme-apple and the examples in kit/examples).

The launcher that the patch modifies is CleanYourTV by Gathin, which is not
included in this repository and has its own MIT license. Its notice and every other third-party item are below.

## CleanYourTV (MIT)

- Project: https://github.com/Gathin23/cleanyourtv (site: https://cleanyour.tv)
- Author: Gathin
- How it is used: `kit/launcher-apple-tv.patch` is a set of changes to CleanYourTV's launcher, and the prompts tell Claude to clone CleanYourTV at commit `eb0131a32fd8567086f39002f1782f7c1b078ca6` and apply the patch. CleanYourTV itself is not copied into this repository. The patch contains short lines of its code as context, so its notice is reproduced here.

```
MIT License

Copyright (c) 2026 Gathin

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Outfit typeface (SIL Open Font License 1.1)

CleanYourTV's launcher uses the Outfit typeface, copyright The Outfit Project Authors, licensed under the SIL Open Font License 1.1. It is not in this repository. You receive it, with its licence file, when you clone CleanYourTV.

## Gradle Wrapper (Apache License 2.0)

`kit/madrid-live/gradlew`, `gradlew.bat` and `gradle/wrapper/` are the standard Gradle Wrapper files, copyright Gradle Inc., licensed under the Apache License 2.0 (https://www.apache.org/licenses/LICENSE-2.0). The companion app's build downloads further open source libraries at build time under their own licences.

## Match data

The football card reads ESPN's public JSON feed (`site.api.espn.com`). It is unofficial and undocumented, ESPN can change or block it at any time, and its use is subject to ESPN's terms. Use it for personal use. You can point the companion app at another data source.

## Trademarks and images

Apple, Apple TV, Xiaomi, Google, Android TV, Google TV, YouTube, ESPN, Real Madrid, LaLiga and every other product, club or company name are trademarks of their owners. This project is independent and is not affiliated with, endorsed by or sponsored by any of them. "Apple TV style" describes a look only; no Apple artwork or code is included.

This repository contains no club crests or app logos. The companion app downloads team crests at run time from the data feed. In the screenshots, crests and unidentified app tiles are blurred.
