package com.zaid.madridlive;

import java.util.ArrayList;
import java.util.List;

/** A live read: the match as it stands plus its goals and red cards so far, oldest first. */
public final class Snapshot {
    public final Match match;
    public final List<Detail> details = new ArrayList<>();

    public Snapshot(Match match) {
        this.match = match;
    }
}
