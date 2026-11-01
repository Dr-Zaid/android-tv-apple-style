package com.zaid.madridlive;

/** One live read of a match from ESPN: the day's scoreboard first (small), the full summary if it is not listed. */
public final class Live {
    private Live() {}

    /** Null when neither feed has the match. Throws on network errors so the caller can back off. */
    public static Snapshot fetch(Match target, long now) throws Exception {
        String board = Http.getString(Espn.scoreboardUrl(target.league, target.kickoffMs));
        Snapshot s = EspnParser.parseScoreboard(board, target.id, target.league, now);
        board = null;
        if (s != null) return s;
        String summary = Http.getString(Espn.summaryUrl(target.league, target.id));
        return EspnParser.parseSummary(summary, target.league, now);
    }
}
