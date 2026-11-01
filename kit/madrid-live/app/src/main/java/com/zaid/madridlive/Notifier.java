package com.zaid.madridlive;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

/** The one low-priority notification the foreground service must show while it follows a match. */
public final class Notifier {
    private Notifier() {}

    public static final int ID = 7001;
    private static final String CHANNEL = "live_match";

    public static Notification build(Context c, String text) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null && nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, c.getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            ch.setDescription(c.getString(R.string.channel_desc));
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        return new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(c.getString(R.string.notif_title))
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    public static void update(Context c, String text) {
        try {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(ID, build(c, text));
        } catch (Exception ignored) {
            // cosmetic
        }
    }

    /** "Real Madrid 1–0 Barcelona · 67'" */
    public static String line(Match m) {
        if (Match.PRE.equals(m.state)) return m.home + " vs " + m.away;
        String s = m.home + " " + Math.max(0, m.homeScore) + "–" + Math.max(0, m.awayScore) + " " + m.away;
        return m.clock.isEmpty() ? s : s + " · " + m.clock;
    }
}
