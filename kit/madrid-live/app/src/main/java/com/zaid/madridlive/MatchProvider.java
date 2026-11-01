package com.zaid.madridlive;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * The home screen's window on the match. {@code content://com.zaid.madridlive.match/next} is zero or one row;
 * {@code /logo/home} and {@code /logo/away} are the crests as PNG files. Read-only, no permission.
 */
public final class MatchProvider extends ContentProvider {
    public static final String AUTHORITY = "com.zaid.madridlive.match";
    public static final Uri NEXT_URI = Uri.parse("content://" + AUTHORITY + "/next");

    private static final int CODE_NEXT = 1;
    private static final int CODE_LOGO_HOME = 2;
    private static final int CODE_LOGO_AWAY = 3;
    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        MATCHER.addURI(AUTHORITY, "next", CODE_NEXT);
        MATCHER.addURI(AUTHORITY, "logo/home", CODE_LOGO_HOME);
        MATCHER.addURI(AUTHORITY, "logo/away", CODE_LOGO_AWAY);
    }

    @Override
    public boolean onCreate() {
        return true;   // nothing to do until asked
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            if (MATCHER.match(uri) != CODE_NEXT) return null;
            Context c = getContext();
            if (c == null) return null;

            Refresher.kickIfStale(c);   // in the background; this query answers with what it has
            Match m = Store.current(c, System.currentTimeMillis());

            String[] cols = MatchRow.pick(projection);
            MatrixCursor cursor = new MatrixCursor(cols, 1);
            if (m != null) {
                Object[] full = MatchRow.values(m);
                Object[] out = new Object[cols.length];
                for (int i = 0; i < cols.length; i++) out[i] = full[MatchRow.indexOf(cols[i])];
                cursor.addRow(out);
            }
            cursor.setNotificationUri(c.getContentResolver(), NEXT_URI);
            return cursor;
        } catch (Throwable t) {
            return null;   // never crash the home screen's query
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        int code = MATCHER.match(uri);
        if ((code != CODE_LOGO_HOME && code != CODE_LOGO_AWAY) || !"r".equals(mode)) {
            throw new FileNotFoundException(String.valueOf(uri));
        }
        try {
            Context c = getContext();
            Match m = c == null ? null : Store.current(c, System.currentTimeMillis());
            if (m == null) throw new FileNotFoundException("no match");
            boolean home = code == CODE_LOGO_HOME;
            String id = home ? m.homeId : m.awayId;
            // Normally already cached when the row was stored; otherwise fetch it now (binder thread, short timeouts).
            if (!LogoCache.ensure(c, id, home ? m.homeLogo : m.awayLogo)) throw new FileNotFoundException("no crest");
            File f = LogoCache.file(c, id);
            return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Throwable t) {
            throw new FileNotFoundException("crest unavailable");
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (MATCHER.match(uri)) {
            case CODE_NEXT: return "vnd.android.cursor.item/vnd.com.zaid.madridlive.match";
            case CODE_LOGO_HOME:
            case CODE_LOGO_AWAY: return "image/png";
            default: return null;
        }
    }

    // Read-only: writes are ignored, not errors.
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
