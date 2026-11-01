package com.zaid.madridlive;

/** One goal or red card from the live feed's {@code details} (or {@code keyEvents}). */
public final class Detail {
    public static final int GOAL = 1;
    public static final int RED = 2;

    public int kind;
    public String teamId = "";
    public String player = "";
    /** "63'" */
    public String clock = "";
    public boolean ownGoal;
    public boolean penalty;

    /**
     * Identity across polls (and across the scoreboard and summary feeds). A red card is one per player, so it leaves
     * out the clock, which the two feeds do not always spell the same way.
     */
    public String key() {
        if (kind == RED) return "red|" + teamId + "|" + player;
        return "goal|" + clock + "|" + teamId + "|" + player;
    }
}
