package com.zaid.madridlive;

/** The provider row: its column names and how a {@link Match} fills them. Pure, so the contract is unit-tested. */
public final class MatchRow {
    private MatchRow() {}

    /** The row's columns, in this order. */
    public static final String[] COLUMNS = {
            "state", "kickoff_ms", "competition", "home", "away", "home_abbr", "away_abbr",
            "home_score", "away_score", "clock", "madrid_home", "venue", "fetched_ms", "last_event", "last_event_ms",
    };

    /** One value per column: Strings for TEXT, Long/Integer for INTEGER. */
    public static Object[] values(Match m) {
        return new Object[]{
                m.state, m.kickoffMs, m.competition, m.home, m.away, m.homeAbbr, m.awayAbbr,
                m.homeScore, m.awayScore, m.clock, m.madridHome ? 1 : 0, m.venue, m.fetchedMs, m.lastEvent, m.lastEventMs,
        };
    }

    public static int indexOf(String col) {
        for (int i = 0; i < COLUMNS.length; i++) if (COLUMNS[i].equals(col)) return i;
        return -1;
    }

    /** The requested columns that exist, or all of them when the caller asked for none (or none we know). */
    public static String[] pick(String[] projection) {
        if (projection == null) return COLUMNS;
        java.util.List<String> ok = new java.util.ArrayList<>();
        for (String p : projection) if (indexOf(p) >= 0) ok.add(p);
        return ok.isEmpty() ? COLUMNS : ok.toArray(new String[0]);
    }
}
