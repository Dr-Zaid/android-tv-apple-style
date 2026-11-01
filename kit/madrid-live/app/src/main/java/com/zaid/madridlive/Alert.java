package com.zaid.madridlive;

/** Something worth a banner: a goal, a red card, kick-off or full time. Carries everything the overlay prints. */
public final class Alert {
    public static final int GOAL = 1;
    public static final int RED = 2;
    public static final int KICKOFF = 3;
    public static final int FULLTIME = 4;

    /** A right-to-left mark after the Arabic word, so its "!" stays on the word's left in the left-to-right pill. */

    public int kind;
    public String home = "";
    public String away = "";
    public String homeId = "";
    public String awayId = "";
    public String homeLogo = "";
    public String awayLogo = "";
    public int homeScore;
    public int awayScore;
    public String clock = "";
    /** Who it was (scorer or sent-off player), may be empty. */
    public String player = "";
    /** The team the event belongs to, may be empty. */
    public String team = "";
    public boolean penalty;
    public boolean ownGoal;
    public String competition = "";
    public String venue = "";

    /** The Arabic word for the kind of event. */
    public static String kindWord(int kind) {
        switch (kind) {
            case GOAL: return "هدف!";                                         // هدف!
            case RED: return "بطاقة حمراء"; // بطاقة حمراء
            case KICKOFF: return "بدأت المباراة"; // بدأت المباراة
            case FULLTIME: return "نهاية المباراة"; // نهاية المباراة
            default: return "";
        }
    }

    /** First line, e.g. {@code هدف!  Real Madrid 1–0 Barcelona · 67'}. */
    /** Unicode first-strong isolates: a left-to-right run inside the Arabic line. */
    static final String LRI = "\u2066", PDI = "\u2069";

    public String headline() {
        // The Arabic word leads (right side of an RTL line); the teams, score and minute sit in a left-to-right
        // isolate so "Real Madrid 1–0 Barcelona · 67'" keeps its order and the minute's apostrophe its place.
        StringBuilder sb = new StringBuilder(kindWord(kind)).append("  ").append(LRI).append(home);
        if (kind == KICKOFF) {
            sb.append(" vs ");
        } else {
            sb.append(' ').append(homeScore).append('–').append(awayScore).append(' ');
        }
        sb.append(away);
        if (kind != KICKOFF && !clock.isEmpty()) sb.append(" · ").append(clock);
        return sb.append(PDI).toString();
    }

    /** Second, smaller line: the scorer, the player sent off, or the competition. */
    public String subline() {
        switch (kind) {
            case GOAL:
            case RED: {
                StringBuilder sb = new StringBuilder(player);
                if (kind == GOAL && penalty) sb.append(" (pen.)");
                if (kind == GOAL && ownGoal) sb.append(" (o.g.)");
                if (!team.isEmpty()) {
                    if (sb.length() > 0) sb.append(" · ");
                    sb.append(team);
                }
                return sb.toString();
            }
            case KICKOFF:
                return venue.isEmpty() ? competition : competition + " · " + venue;
            default:
                return competition;
        }
    }

    /** What the provider's {@code last_event} column says, e.g. {@code Goal · Vinícius Jr. 67'}. */
    public String lastEvent() {
        switch (kind) {
            case GOAL: return "Goal · " + who();
            case RED: return "Red card · " + who();
            case KICKOFF: return "Kick-off";
            case FULLTIME: return "Full time";
            default: return "";
        }
    }

    private String who() {
        String w = player.isEmpty() ? team : player;
        if (!clock.isEmpty()) w = w.isEmpty() ? clock : w + " " + clock;
        return w;
    }

    /** An alert from a match as it stands now. */
    public static Alert of(int kind, Match m) {
        Alert a = new Alert();
        a.kind = kind;
        a.home = m.home;
        a.away = m.away;
        a.homeId = m.homeId;
        a.awayId = m.awayId;
        a.homeLogo = m.homeLogo;
        a.awayLogo = m.awayLogo;
        a.homeScore = Math.max(0, m.homeScore);
        a.awayScore = Math.max(0, m.awayScore);
        a.clock = m.clock;
        a.competition = m.competition;
        a.venue = m.venue;
        return a;
    }
}
