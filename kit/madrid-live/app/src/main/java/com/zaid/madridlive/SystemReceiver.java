package com.zaid.madridlive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Boot, app update and our own alarms (not exported: only the system and our PendingIntents reach it). */
public final class SystemReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        final Context app = context.getApplicationContext();
        try {
            switch (intent.getAction()) {
                case Intent.ACTION_BOOT_COMPLETED:
                case Intent.ACTION_MY_PACKAGE_REPLACED:
                case Scheduler.ACTION_REFRESH:
                case Scheduler.ACTION_RETRY:
                    Scheduler.rescheduleAll(app);      // arm what we know first; the refresh may fail
                    refreshInBackground(app);
                    break;
                case Scheduler.ACTION_KICKOFF:
                    Scheduler.ensureLive(app);
                    Scheduler.rescheduleAll(app);
                    break;
                case Scheduler.ACTION_DEMO_END:
                    Store.clearDemo(app);
                    Updates.changed(app);
                    break;
                default:
                    break;
            }
        } catch (Throwable ignored) {
            // a receiver must never crash the process
        }
    }

    private void refreshInBackground(final Context app) {
        final PendingResult pr = goAsync();
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    Refresher.refreshNow(app);
                } finally {
                    pr.finish();
                }
            }
        }, "madrid-refresh");
        t.setDaemon(true);
        t.start();
    }
}
