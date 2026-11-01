package com.zaid.madridlive;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;

import java.util.List;

/**
 * Follows a match: from ten minutes before kick-off it reads the live score every 30 seconds, keeps the stored row
 * and the home screen up to date, raises the banners, and stops itself ten minutes after the final whistle. It also
 * hosts the banners of the DEMO hook so they are not lost if the system trims the process.
 */
public final class LiveService extends Service {
    static final String ACTION_DEMO_ALERT = "com.zaid.madridlive.DEMO_ALERT";
    static final String EXTRA_KIND = "kind";

    private HandlerThread thread;
    private Handler bg;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean polling;  // set on main in onStartCommand, cleared on the poll thread
    private int failures;
    private EventTracker tracker;
    private boolean destroyed;

    public static void start(Context c) {
        c.getApplicationContext().startForegroundService(new Intent(c, LiveService.class));
    }

    /** Show a demo banner through the service; falls back to the process-wide overlay if the service cannot start. */
    public static void startDemo(Context c, String kind) {
        try {
            c.getApplicationContext().startForegroundService(
                    new Intent(c, LiveService.class).setAction(ACTION_DEMO_ALERT).putExtra(EXTRA_KIND, kind));
        } catch (Throwable t) {
            Demo.showDirect(c.getApplicationContext(), kind);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        thread = new HandlerThread("madrid-live");
        thread.start();
        bg = new Handler(thread.getLooper());
        OverlayHost.get(this).setOnIdle(new Runnable() {
            @Override public void run() { maybeStop(); }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // A foreground start must call startForeground within seconds, whatever happens next.
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(Notifier.ID, Notifier.build(this, "Following the match"),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(Notifier.ID, Notifier.build(this, "Following the match"));
            }
        } catch (Throwable t) {
            stopSelf();
            return START_NOT_STICKY;
        }
        String action = intent == null ? null : intent.getAction();
        if (ACTION_DEMO_ALERT.equals(action)) {
            final String kind = intent.getStringExtra(EXTRA_KIND);
            bg.post(new Runnable() {
                @Override public void run() { Demo.showDirect(LiveService.this, kind); }
            });
        } else if (!polling) {
            polling = true;
            tracker = Store.tracker(this);
            bg.post(tick);
        }
        return START_NOT_STICKY;
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (destroyed) return;
            long delay = Timing.POLL_MS;
            try {
                long now = System.currentTimeMillis();
                Match target = Store.real(LiveService.this, now);
                if (!Timing.isClockSane(now) || !Timing.shouldPoll(target, now)) {
                    polling = false;
                    Scheduler.rescheduleAll(LiveService.this);
                    main.post(new Runnable() { @Override public void run() { maybeStop(); } });
                    return;
                }
                try {
                    Snapshot snap = Live.fetch(target, now);
                    if (snap == null) {
                        failures++;
                    } else {
                        failures = 0;
                        handle(target, snap, now);
                    }
                } catch (Throwable t) {
                    failures++;     // keep the last good row, try again with a longer wait
                }
                delay = Timing.pollDelayMs(failures);
            } catch (Throwable t) {
                delay = Timing.pollDelayMs(++failures);
            }
            if (!destroyed) bg.postDelayed(this, delay);
        }
    };

    /** Store the fresh row, ping the home screen, raise the banners. */
    private void handle(Match target, Snapshot snap, long now) {
        Match fresh = snap.match.copy();
        fresh.lastEvent = target.lastEvent;
        fresh.lastEventMs = target.lastEventMs;
        fresh.endedMs = target.endedMs;
        if (fresh.venue.isEmpty()) fresh.venue = target.venue;
        if (Match.POST.equals(fresh.state) && fresh.endedMs == 0) fresh.endedMs = now;
        if (fresh.stateRank() < target.stateRank()) return;    // a stale read must not move the match backwards

        List<Alert> alerts = tracker.update(snap);
        Store.saveTracker(this, tracker);
        for (Alert a : alerts) {
            String ev = a.lastEvent();
            if (!ev.isEmpty()) {
                fresh.lastEvent = ev;
                fresh.lastEventMs = now;
            }
        }
        Store.updateMatch(this, fresh);
        Updates.changed(this);
        Notifier.update(this, Notifier.line(fresh));

        if (!alerts.isEmpty()) {
            LogoCache.ensure(this, fresh.homeId, fresh.homeLogo);
            LogoCache.ensure(this, fresh.awayId, fresh.awayLogo);
            Bitmap h = LogoCache.bitmap(this, fresh.homeId);
            Bitmap a = LogoCache.bitmap(this, fresh.awayId);
            for (Alert alert : alerts) OverlayHost.get(this).show(alert, h, a);
        }
    }

    /** Stop when nothing is polling and no banner is up or waiting. Main thread. */
    private void maybeStop() {
        if (!polling && !destroyed) {
            destroyed = true;
            stopForeground(true);
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        destroyed = true;
        OverlayHost.get(this).setOnIdle(null);
        if (bg != null) bg.removeCallbacksAndMessages(null);
        if (thread != null) thread.quit();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
