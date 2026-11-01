package com.zaid.madridlive;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Where the data comes from: ESPN's public site API (no key) and the competitions Real Madrid plays in. */
public final class Espn {
    private Espn() {}

    private static final String BASE = "https://site.api.espn.com/apis/site/v2/sports/soccer/";

    /** {slug, display name}. */
    public static final String[][] LEAGUES = {
            {"esp.1", "LaLiga"},
            {"uefa.champions", "Champions League"},
            {"esp.copa_del_rey", "Copa del Rey"},
            {"esp.super_cup", "Supercopa"},
    };

    public static String competitionName(String slug) {
        for (String[] l : LEAGUES) if (l[0].equals(slug)) return l[1];
        return slug == null ? "" : slug;
    }

    /** The upcoming fixtures ({@code fixture=true}) or, without it, the results so far. */
    public static String scheduleUrl(String league, boolean fixtures) {
        return BASE + league + "/teams/" + Match.MADRID_ID + "/schedule" + (fixtures ? "?fixture=true" : "");
    }

    /** All of a league's games on one (UTC) day: small, and carries the goals and cards. */
    public static String scoreboardUrl(String league, long kickoffMs) {
        String day = DateTimeFormatter.BASIC_ISO_DATE.format(Instant.ofEpochMilli(kickoffMs).atOffset(ZoneOffset.UTC).toLocalDate());
        return BASE + league + "/scoreboard?dates=" + day;
    }

    /** One game in full; about 450 KB, so only the fallback when the scoreboard does not list it. */
    public static String summaryUrl(String league, String eventId) {
        return BASE + league + "/summary?event=" + eventId;
    }

    /** Fallback crest when the feed gave no logo URL. */
    public static String crestUrl(String teamId) {
        return "https://a.espncdn.com/i/teamlogos/soccer/500/" + teamId + ".png";
    }
}
