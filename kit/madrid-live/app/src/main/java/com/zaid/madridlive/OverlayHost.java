package com.zaid.madridlive;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayDeque;

/**
 * Draws alert banners over whatever is playing, one at a time, with WindowManager. The banner can never take focus or
 * touches, so the remote keeps working underneath. Without the "draw over other apps" permission it quietly skips.
 */
public final class OverlayHost {
    private static final long FADE_MS = 250;
    private static final long STAY_MS = 8_000;

    private static OverlayHost instance;

    public static synchronized OverlayHost get(Context c) {
        if (instance == null) instance = new OverlayHost(c.getApplicationContext());
        return instance;
    }

    private static final class Item {
        final Alert alert;
        final Bitmap homeCrest;
        final Bitmap awayCrest;
        Item(Alert alert, Bitmap h, Bitmap a) {
            this.alert = alert;
            this.homeCrest = h;
            this.awayCrest = a;
        }
    }

    private final Context ctx;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ArrayDeque<Item> queue = new ArrayDeque<>();   // touched on the main thread only
    private boolean showing;
    private Runnable onIdle;

    private OverlayHost(Context c) {
        this.ctx = c;
    }

    /** Called (on the main thread) each time the queue runs dry and nothing is on screen. */
    public void setOnIdle(Runnable r) {
        this.onIdle = r;
    }

    /** Queue a banner. Safe from any thread; crests may be null. */
    public void show(final Alert alert, final Bitmap homeCrest, final Bitmap awayCrest) {
        main.post(new Runnable() {
            @Override public void run() {
                queue.add(new Item(alert, homeCrest, awayCrest));
                next();
            }
        });
    }

    private void next() {
        if (showing) return;
        Item item = queue.poll();
        if (item == null) {
            Runnable r = onIdle;
            if (r != null) r.run();
            return;
        }
        showing = true;
        final WindowManager wm;
        final View view;
        try {
            if (!Settings.canDrawOverlays(ctx)) throw new IllegalStateException("overlay permission not granted");
            wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            view = build(item);
            wm.addView(view, params());
        } catch (Throwable t) {
            showing = false;     // skip this banner, go on with the next
            next();
            return;
        }
        float d = ctx.getResources().getDisplayMetrics().density;
        view.animate().alpha(1f).translationX(0f).setDuration(FADE_MS).setInterpolator(new DecelerateInterpolator()).start();
        main.postDelayed(new Runnable() {
            @Override public void run() {
                view.animate().alpha(0f).translationX(16 * d).setDuration(FADE_MS).start();
                main.postDelayed(new Runnable() {
                    @Override public void run() {
                        try {
                            wm.removeViewImmediate(view);
                        } catch (Throwable ignored) {
                            // already gone
                        }
                        showing = false;
                        next();
                    }
                }, FADE_MS + 30);
            }
        }, FADE_MS + STAY_MS);
    }

    private WindowManager.LayoutParams params() {
        float d = ctx.getResources().getDisplayMetrics().density;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.RIGHT;
        lp.x = Math.round(48 * d);    // clear of the TV's overscan
        lp.y = Math.round(32 * d);
        lp.windowAnimations = 0;
        lp.setTitle("madrid-live-alert");
        return lp;
    }

    /** The pill: dark glass, rounded 28dp, a crest at each end and two lines of text between them. */
    private View build(Item item) {
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        float d = dm.density;

        LinearLayout pill = new LinearLayout(ctx);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        pill.setPadding(Math.round(14 * d), Math.round(8 * d), Math.round(14 * d), Math.round(8 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6161618);
        bg.setCornerRadius(28 * d);
        bg.setStroke(Math.max(1, Math.round(d)), 0x33FFFFFF);
        pill.setBackground(bg);
        pill.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        pill.setAlpha(0f);
        pill.setTranslationX(32 * d);

        addCrest(pill, item.homeCrest, d);

        LinearLayout text = new LinearLayout(ctx);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setGravity(Gravity.CENTER_VERTICAL);
        int maxText = Math.round(dm.widthPixels * 0.55f);

        TextView line1 = new TextView(ctx);
        line1.setText(item.alert.headline());
        line1.setTextColor(0xFFFFFFFF);
        line1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        line1.setTypeface(Typeface.DEFAULT_BOLD);
        line1.setSingleLine(true);
        line1.setEllipsize(TextUtils.TruncateAt.END);
        line1.setMaxWidth(maxText);
        line1.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        line1.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        text.addView(line1);

        String sub = item.alert.subline();
        if (!sub.isEmpty()) {
            TextView line2 = new TextView(ctx);
            line2.setText(sub);
            line2.setTextColor(0xB3FFFFFF);
            line2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            line2.setSingleLine(true);
            line2.setEllipsize(TextUtils.TruncateAt.END);
            line2.setMaxWidth(maxText);
            line2.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
            line2.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            text.addView(line2);
        }
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.leftMargin = Math.round(12 * d);
        tlp.rightMargin = Math.round(12 * d);
        pill.addView(text, tlp);

        addCrest(pill, item.awayCrest, d);
        return pill;
    }

    private void addCrest(LinearLayout parent, Bitmap crest, float d) {
        if (crest == null) return;
        ImageView iv = new ImageView(ctx);
        iv.setImageBitmap(crest);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        parent.addView(iv, new LinearLayout.LayoutParams(Math.round(40 * d), Math.round(40 * d)));
    }
}
