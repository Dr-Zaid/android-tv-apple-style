package com.zaid.madridlive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** A GET with HttpURLConnection: short timeouts, a size cap, and the HTTP status kept on the exception. */
public final class Http {
    private Http() {}

    private static final int CONNECT_MS = 6_000;
    private static final int READ_MS = 8_000;
    /** The biggest feed seen is ~450 KB; anything over this is not what we asked for. */
    private static final int MAX_JSON = 3 * 1024 * 1024;
    private static final int MAX_IMAGE = 1024 * 1024;

    /** A non-200 answer. */
    public static final class StatusException extends IOException {
        public final int code;
        public StatusException(int code) {
            super("HTTP " + code);
            this.code = code;
        }
    }

    public static String getString(String url) throws IOException {
        return new String(getBytes(url, MAX_JSON), StandardCharsets.UTF_8);
    }

    public static byte[] getImage(String url) throws IOException {
        return getBytes(url, MAX_IMAGE);
    }

    private static byte[] getBytes(String url, int max) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(CONNECT_MS);
            c.setReadTimeout(READ_MS);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", "RealMadridLive/1.0 (Android TV)");
            c.setRequestProperty("Accept", "application/json, image/png, */*");
            int code = c.getResponseCode();
            if (code != 200) throw new StatusException(code);
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(32 * 1024);
                byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    if (out.size() > max) throw new IOException("response too large");
                }
                return out.toByteArray();
            }
        } finally {
            c.disconnect();
        }
    }
}
