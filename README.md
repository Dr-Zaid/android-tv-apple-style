<div dir="rtl">

# حوّل Android TV مالتك لشكل Apple TV

**سوّي clone للريبو، افتحه بـ Claude Code، وجاوب على كم سؤال. Claude يسوي الباقي.**

[English README](README.en.md)

<img src="screenshots/home-apple-style.png" alt="واجهة بشكل Apple TV على Android TV" width="860">

جرّبت هذا الترند على Xiaomi Mi TV Stick مالتي: شلت البرامج الزايدة، وغيّرت الواجهة لشكل Apple TV، وزدت كارت يعرض
مباراة فريقي الجاية وتنبيه بالهدف فوق أي تطبيق. كل الشغل سواه Claude وأنا بس أوجّه. هذا الريبو بيه كلشي استخدمته:
البرومبت، والكود، والسكربتات. مبني على [CleanYourTV](https://github.com/Gathin23/cleanyourtv).

## شلون تبدي

```bash
git clone https://github.com/Dr-Zaid/android-tv-apple-style.git
cd android-tv-apple-style
claude        # افتح Claude Code هنا واكتب: start
```

1. Claude يسألك **مرة وحدة**: شنو تلفزيونك، وتطبيقات الـ dock، وشنو الشكل اللي تريده، وفريقك (أو لا أحد)، وشنو يصير ينشال.
2. يسوي نسخة احتياطية، ينظّف التلفزيون، ينصّب الواجهة الجديدة، ويتأكد إن زر Home بالريموت الحقيقي يشتغل.
3. يسكّر الباب اللي فتحه ويعطيك سكربت يرجّع كلشي.

هذا الشغل كله. هذي الطريقة السريعة: بدون اختبارات سرعة وبدون وقت طويل.

> تفضّل تلصق؟ افتح Claude Code بهذا الفولدر والصق محتوى [`PROMPT.md`](PROMPT.md).

## قوله ذوقك

مو لازم تمشي على تصميمي. لمن Claude يسألك عن الشكل، جاوبه بكلامك: "أغمق وأهدأ"، "خلفية غروب أزرق و٥ أعمدة وبدون ساعة"،
"dock مسطّح وأيقونات كبيرة". Claude يحوّل ذوقك للأشياء اللي الـ kit يكدر يغيّرها فعلاً (الخلفية، الأعمدة، الـ dock،
حركة التركيز، الهيدر، كارت المباراة)، وإذا شي مو ممكن يكلك بصراحة.

## شنو تحصل

- **واجهة بشكل Apple TV:** صورة كاملة ورا كلشي، dock زجاجي، التطبيق المختار يكبر ويرتفع وبيه ظل، وساعة صغيرة وأزرار دايرية.
- **تلفزيون أنظف:** برامج التتبع والإعلانات من الشركة تنطفي (disable)، مو تنمسح.
- **كارت مباراة مباشر:** المباراة الجاية لفريقك ويا عدّاد، والنتيجة مباشرة، وتنبيه الهدف فوق أي تطبيق. اختياري.
- **سكربت استرجاع:** أمر واحد يرجّع كلشي.

## النتائج على الستيك مالتي

| | قبل | بعد |
|---|---|---|
| الانتقال بين التطبيقات | 638 ms | **338 ms** |
| استجابة زر الريموت | 21 ms | 21 ms |
| التقطيع بالواجهة | 0% | 0.5% |
| Crash بـ 2000 ضغطة عشوائية | | **0** |
| فتح YouTube أول مرة | 5.4 s | **3.6 s** |
| الرام الفاضية بعد التنظيف | 288 MB | **451 MB** |

بصراحة: الواجهة الجديدة تفتح أبطأ شوية من الأولى (0.82 ثانية بدل 0.66) بسبب الصورة الخلفية، وسرعة فتح التطبيقات
تتغير من مرة لمرة على جهاز بيه 1 GB رام. جهازك ممكن يختلف. تريد نفس الأرقام لتلفزيونك؟ استخدم [`PROMPT-FULL.md`](PROMPT-FULL.md).

## شنو تحتاج

1. **Claude Code** ويا اشتراك **Pro** أو **Max**. أنا استخدمت Opus بـ effort على High.
2. **حاسبة Mac أو Windows** على **نفس شبكة الواي فاي** مال التلفزيون، بيها حوالي 3 GB فاضية. Claude ينصّب Python
   وJava وAndroid SDK، كلها مجانية.
3. **تلفزيون أو ستيك Android TV أو Google TV** (Android 8 أو أحدث): Xiaomi وTCL وHisense (Android) وSony وNvidia Shield
   وأغلب الأنواع. **ما يشتغل** على Samsung (Tizen) وLG (webOS) وRoku وVizio وHisense VIDAA وAmazon Fire TV.
4. **الريموت بيدك**، لأن Claude راح يطلب منك تضغط أزرار وتوافق على رسائل.

## شلون تشغّل ADB على التلفزيون

ADB هو الباب اللي يخلي الحاسبة تحچي ويا التلفزيون على الواي فاي.

**Android TV (مثل Mi TV Stick):**
1. Settings ← Device Preferences ← About.
2. اضغط OK على **Build** **7 مرات** لحد ما تطلع "You are now a developer".
3. ارجع لـ Device Preferences ← **Developer options** ← شغّل **USB debugging** (وNetwork debugging إذا موجود).

**Google TV:**
1. Settings ← System ← About ← اضغط على **Android TV OS build** سبع مرات.
2. ارجع لـ System ← **Developer options** ← شغّل **USB debugging**. إذا ما اشتغل، Claude راح يطلب منك
   **Wireless debugging** وكود الربط.

لمن Claude يتصل أول مرة تطلع رسالة **"Allow USB debugging?"** على التلفزيون: أشّر **Always allow** واضغط OK.
على Mac ممكن تطلع رسالة **Local Network**: وافق عليها.

## بعد ما تخلص: سد الباب

لازم تطفّي ADB لأن البورت 5555 مفتوح لأي جهاز على شبكتك:
1. Developer options ← **Revoke USB debugging authorisations**.
2. طفّي **مفتاح Developer options نفسه** اللي فوق (على Mi TV Stick، إطفاء USB debugging بس ما يكفي).
3. سوّي Restart، وخلّي Claude يتأكد إن البورت **refused**.

## شي لازم تعرفه

- Claude يسوي نسخة احتياطية وسكربت استرجاع قبل ما يغيّر أي شي. احتفظ بيهم.
- البرامج تنطفي مو تنمسح، وتحديثات التلفزيون ممكن ترجّع بعضها.
- ما يشيل إعلانات YouTube.
- كارت المباراة يستخدم feed غير رسمي من ESPN. إذا تغيّر، الكارت يبقى على آخر مباراة لحد ما ينصلح.
- هذا مشروع شخصي، يجي كما هو بدون ضمان. شوف [الرخصة](LICENSE).

## شنو بالريبو

| الملف | شنو هو |
|---|---|
| [`PROMPT.md`](PROMPT.md) | البرومبت السريع: يسألك عن ذوقك وبعدها يشتغل |
| [`PROMPT-FULL.md`](PROMPT-FULL.md) | البرومبت الكامل المجرّب، ويا اختبارات السرعة وأرقام قبل وبعد |
| [`kit/launcher-apple-tv.patch`](kit/launcher-apple-tv.patch) | تعديلات شكل Apple TV على launcher مال CleanYourTV |
| [`kit/madrid-live/`](kit/madrid-live) | تطبيق صغير: بيانات كارت الفريق وتنبيه الهدف |
| [`kit/tools/`](kit/tools) | أدوات قياس السرعة والتقطيع وسكربت التنصيب |
| [`kit/theme-apple/`](kit/theme-apple) | إعدادات الثيم |
| [`kit/examples/`](kit/examples) | قائمة التنظيف اللي استخدمتها على Mi TV Stick |
| [`screenshots/`](screenshots) | صور من الستيك |

## إذا وفّر عليك وقت

حط **نجمة** للريبو: هذا يساعد غيرك يلكيه. وحط نجمة لـ [CleanYourTV](https://github.com/Gathin23/cleanyourtv) هم، لأنه الأساس. وإذا شاركته، كلي شلون طلع على تلفزيونك.

## جرّبته على تلفزيونك؟

[افتح device report](../../issues/new?template=device-report.yml) واكتب نوع التلفزيون وشلون طلعت النتيجة. هذا يساعد اللي بعدك.

## شكر وملاحظات

- مبني على **[CleanYourTV](https://github.com/Gathin23/cleanyourtv)** مال Gathin (رخصة MIT). حط نجمة للأصلي، هو الأساس لكل هذا المشروع. إشعار رخصته بملف [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
- شكل Apple TV وكارت المباراة والأدوات والطريقة السريعة: **زيد فؤاد** ([@Dr-Zaid](https://github.com/Dr-Zaid)).
- Apple وXiaomi وGoogle وESPN وريال مدريد وغيرها أسماء تعود لأصحابها. هذا مشروع مستقل وما إله علاقة بأي منهم. شعارات
  الأندية والتطبيقات مو موجودة بالريبو.

## تعلّم تسوي شغلات مثل هذي

كل هذا انبنى بـ Claude Code، وأنا ما بديت مبرمج. إذا تريد تتعلم شلون تسويها بنفسك، سجّل مجاناً بمنصتي
**[Zaid AI Class](https://zaidaiclass.com)**: أكو هواية شغلات جاية.

## الرخصة

[MIT](LICENSE) لأجزائي. CleanYourTV والأشياء الثانية من أطراف ثانية تبقى برخصها: شوف [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

</div>
