package com.zaid.madridlive;

import android.content.Context;
import android.content.Intent;

/** Tells the home screen the row changed: the content observer ping and the MATCH_UPDATED broadcast. */
public final class Updates {
    private Updates() {}

    public static final String ACTION_MATCH_UPDATED = "com.zaid.madridlive.MATCH_UPDATED";

    public static void changed(Context c) {
        try {
            Context app = c.getApplicationContext();
            app.getContentResolver().notifyChange(MatchProvider.NEXT_URI, null);
            // Implicit on purpose: the launcher registers a runtime receiver for it.
            app.sendBroadcast(new Intent(ACTION_MATCH_UPDATED));
        } catch (Exception ignored) {
            // a failed ping must never break the caller
        }
    }
}
