package com.zaid.madridlive;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

/**
 * The alarms: a 6-hourly inexact schedule refresh, one exact alarm at kick-off minus ten minutes for the next match,
 * a retry after a failed refresh, and the end of the demo row. All of them land in {@link SystemReceiver}.
 */
public final class Scheduler {
    private Scheduler() {}

    public static final String ACTION_REFRESH = "com.zaid.madridlive.ALARM_REFRESH";
    public static final String ACTION_RETRY = "com.zaid.madridlive.ALARM_RETRY";
    public static final String ACTION_KICKOFF = "com.zaid.madridlive.ALARM_KICKOFF";
    public static final String ACTION_DEMO_END = "com.zaid.madridlive.ALARM_DEMO_END";

    private static PendingIntent pending(Context c, String action, int flags) {
        Intent i = new Intent(c, SystemReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(c, action.hashCode(), i, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Re-arm everything from what is stored now. Cheap and idempotent: call it after every refresh and boot. */
    public static void rescheduleAll(Context ctx) {
        try {
            Context c = ctx.getApplicationContext();
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            long now = System.currentTimeMillis();

            // 1. Schedule refresh every six hours; the system may batch it (inexact), and it wakes the stick.
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + Timing.REFRESH_MS, Timing.REFRESH_MS,
                    pending(c, ACTION_REFRESH, PendingIntent.FLAG_UPDATE_CURRENT));

            // 2. Exact alarm for the next match's window (kick-off minus ten minutes).
            PendingIntent kick = pending(c, ACTION_KICKOFF, PendingIntent.FLAG_UPDATE_CURRENT);
            long at = Timing.isClockSane(now) ? Timing.nextKickoffAlarm(Store.all(c), now) : 0;
            if (at > 0) {
                setExact(am, at, kick);
            } else {
                am.cancel(kick);
            }

            // 3. Retry after a failed refresh, with backoff.
            PendingIntent retry = pending(c, ACTION_RETRY, PendingIntent.FLAG_UPDATE_CURRENT);
            long retryAt = Store.nextRetryMs(c);
            if (Store.failures(c) > 0 && retryAt > now) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, retryAt, retry);
            } else {
                am.cancel(retry);
            }
        } catch (Exception ignored) {
            // alarms are best effort; the next boot or query tries again
        }
    }

    public static void scheduleDemoEnd(Context ctx, long atMs) {
        try {
            Context c = ctx.getApplicationContext();
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                setExact(am, atMs, pending(c, ACTION_DEMO_END, PendingIntent.FLAG_UPDATE_CURRENT));
            }
        } catch (Exception ignored) {
            // the demo row simply expires on the next query instead of pushing the update
        }
    }

    private static void setExact(AlarmManager am, long at, PendingIntent pi) {
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    /** If a match is inside its window right now (and no alarm is going to start it), start the live service. */
    public static void ensureLive(Context ctx) {
        try {
            long now = System.currentTimeMillis();
            Match m = Store.real(ctx, now);
            if (Timing.isClockSane(now) && Timing.shouldPoll(m, now)) LiveService.start(ctx);
        } catch (Exception ignored) {
            // can't start from here (background limits): the next alarm or refresh retries
        }
    }
}
