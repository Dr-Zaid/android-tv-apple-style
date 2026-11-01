package com.zaid.madridlive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * The hook for the owner and for tests: {@code com.zaid.madridlive.DEMO --es kind goal|red|kickoff|fulltime|live} and
 * {@code com.zaid.madridlive.REFRESH}. Exported, no permission: it can only show a fake banner or refresh public data.
 */
public final class ControlReceiver extends BroadcastReceiver {
    public static final String ACTION_DEMO = "com.zaid.madridlive.DEMO";
    public static final String ACTION_REFRESH = "com.zaid.madridlive.REFRESH";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        try {
            switch (intent.getAction()) {
                case ACTION_DEMO:
                    Demo.run(context, intent.getStringExtra("kind"));
                    break;
                case ACTION_REFRESH:
                    final PendingResult pr = goAsync();
                    final Context app = context.getApplicationContext();
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
                    break;
                default:
                    break;
            }
        } catch (Throwable ignored) {
            // a receiver must never crash the process
        }
    }
}
