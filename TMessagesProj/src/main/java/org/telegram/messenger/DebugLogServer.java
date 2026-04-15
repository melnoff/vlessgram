package org.telegram.messenger;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import fi.iki.elonen.NanoHTTPD;

/**
 * Simple HTTP log viewer for debugging.
 * Listens on 0.0.0.0:8765, accessible from local network.
 * Buffers recent log lines in memory and also serves sing-box log file if present.
 */
public class DebugLogServer extends NanoHTTPD {

    public static final int PORT = 8765;
    private static volatile DebugLogServer instance;

    private static final int BUFFER_SIZE = 2000;
    private static final LinkedList<String> buffer = new LinkedList<>();
    private static final SimpleDateFormat TS = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    public static synchronized void startServer() {
        if (instance != null) return;
        try {
            instance = new DebugLogServer();
            instance.start(SOCKET_READ_TIMEOUT, true);
            log("DebugLogServer started on http://0.0.0.0:" + PORT);
        } catch (IOException e) {
            instance = null;
        }
    }

    public static void log(String msg) {
        String line = TS.format(new Date()) + " " + msg;
        synchronized (buffer) {
            buffer.add(line);
            while (buffer.size() > BUFFER_SIZE) {
                buffer.removeFirst();
            }
        }
    }

    private DebugLogServer() {
        super("0.0.0.0", PORT);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if ("/clear".equals(uri)) {
            synchronized (buffer) {
                buffer.clear();
            }
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "cleared");
        }
        if ("/singbox".equals(uri)) {
            return serveSingboxLog();
        }
        return serveMain();
    }

    private Response serveMain() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='utf-8'>");
        html.append("<title>TG Debug</title>");
        html.append("<meta http-equiv='refresh' content='3'>");
        html.append("<style>body{font-family:monospace;background:#1e1e1e;color:#d4d4d4;margin:0;padding:10px;font-size:12px}");
        html.append("a{color:#569cd6;margin-right:10px}.info{color:#608b4e}.warn{color:#dcdcaa}.err{color:#f44747}");
        html.append("pre{margin:0;white-space:pre-wrap;word-break:break-all}</style></head><body>");
        html.append("<div><a href='/'>refresh</a><a href='/singbox'>sing-box log</a><a href='/clear'>clear</a> ");
        html.append("buffer: ").append(buffer.size()).append(" / ").append(BUFFER_SIZE).append("</div><hr>");
        html.append("<pre>");
        ArrayList<String> snapshot;
        synchronized (buffer) {
            snapshot = new ArrayList<>(buffer);
        }
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            String line = snapshot.get(i);
            String cls = "";
            if (line.contains(" failed") || line.contains(" error") || line.contains("FAIL")) cls = " class='err'";
            else if (line.contains("warn")) cls = " class='warn'";
            else if (line.contains("started") || line.contains("OK")) cls = " class='info'";
            html.append("<div").append(cls).append(">").append(escape(line)).append("</div>");
        }
        html.append("</pre></body></html>");
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html.toString());
    }

    private Response serveSingboxLog() {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            File logFile = new File(ctx.getFilesDir(), "singbox/singbox.log");
            if (!logFile.exists()) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "no sing-box log file");
            }
            byte[] data = readFile(logFile);
            String content = new String(data);
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><meta charset='utf-8'>");
            html.append("<title>sing-box log</title>");
            html.append("<style>body{font-family:monospace;background:#1e1e1e;color:#d4d4d4;margin:0;padding:10px;font-size:11px}");
            html.append("a{color:#569cd6;margin-right:10px}pre{margin:0;white-space:pre-wrap;word-break:break-all}</style></head><body>");
            html.append("<div><a href='/'>back</a> size: ").append(data.length).append(" bytes</div><hr><pre>");
            html.append(escape(content));
            html.append("</pre></body></html>");
            return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html.toString());
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "error: " + e.getMessage());
        }
    }

    private byte[] readFile(File f) throws IOException {
        byte[] data = new byte[(int) f.length()];
        try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
            int read = 0;
            while (read < data.length) {
                int r = fis.read(data, read, data.length - read);
                if (r < 0) break;
                read += r;
            }
        }
        return data;
    }

    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
