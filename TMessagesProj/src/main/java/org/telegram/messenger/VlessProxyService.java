package org.telegram.messenger;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.UUID;

/**
 * Manages a local sing-box instance that tunnels traffic through a VLESS proxy.
 * Creates a local SOCKS5 listener with random port and credentials,
 * so Telegram can connect through it as a regular SOCKS5 proxy.
 */
public class VlessProxyService {

    private static volatile VlessProxyService instance;

    private volatile boolean running;
    private int localPort;
    private String localUsername;
    private String localPassword;
    private File baseDir;
    private Object boxService; // can be Libbox BoxService instance

    public static VlessProxyService getInstance() {
        if (instance == null) {
            synchronized (VlessProxyService.class) {
                if (instance == null) {
                    instance = new VlessProxyService();
                }
            }
        }
        return instance;
    }

    private VlessProxyService() {}

    private void log(String msg) {
        FileLog.d("VlessProxy: " + msg);
        DebugLogServer.log("VLESS: " + msg);
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public int getLocalPort() {
        return localPort;
    }

    public String getLocalUsername() {
        return localUsername;
    }

    public String getLocalPassword() {
        return localPassword;
    }

    /**
     * Start sing-box with a VLESS outbound and local SOCKS5 inbound.
     * Returns true if started successfully.
     */
    public synchronized boolean start(SharedConfig.ProxyInfo proxyInfo) {
        if (running) {
            stop();
        }

        try {
            Context context = ApplicationLoader.applicationContext;
            baseDir = new File(context.getFilesDir(), "singbox");
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }
            File workingDir = new File(baseDir, "work");
            if (!workingDir.exists()) {
                workingDir.mkdirs();
            }
            File tempDir = new File(baseDir, "tmp");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            // Generate random local SOCKS5 credentials
            localPort = findFreePort();
            localUsername = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            localPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

            // Generate sing-box config
            String configJson = generateConfig(proxyInfo);

            // Setup and start sing-box via libbox
            com.hiddify.core.mobile.SetupOptions options = new com.hiddify.core.mobile.SetupOptions();
            options.setBasePath(baseDir.getAbsolutePath());
            options.setWorkingDir(workingDir.getAbsolutePath());
            options.setTempDir(tempDir.getAbsolutePath());
            VlessPlatformInterface platform = new VlessPlatformInterface();

            // Step 1: Libbox.setup with paths
            log("=== Step 1: Libbox.setup ===");
            try {
                com.hiddify.core.libbox.SetupOptions lbOpts = new com.hiddify.core.libbox.SetupOptions();
                lbOpts.setBasePath(baseDir.getAbsolutePath());
                lbOpts.setWorkingPath(workingDir.getAbsolutePath());
                lbOpts.setTempPath(tempDir.getAbsolutePath());
                com.hiddify.core.libbox.Libbox.setup(lbOpts);
                log("Libbox.setup OK");
            } catch (Throwable t) {
                log("Libbox.setup FAILED: " + t.getMessage());
                return false;
            }

            // Step 2: Validate config
            log("=== Step 2: Libbox.checkConfig ===");
            try {
                com.hiddify.core.libbox.Libbox.checkConfig(configJson);
                log("config valid");
            } catch (Throwable t) {
                log("config INVALID: " + t.getMessage());
                return false;
            }

            // Step 3: Create CommandServer and start service directly
            log("=== Step 3: CommandServer + startOrReloadService ===");
            try {
                com.hiddify.core.libbox.CommandServerHandler handler = new com.hiddify.core.libbox.CommandServerHandler() {
                    @Override
                    public com.hiddify.core.libbox.SystemProxyStatus getSystemProxyStatus() { return null; }
                    @Override
                    public void serviceReload() { log("CSH.serviceReload"); }
                    @Override
                    public void serviceStop() { log("CSH.serviceStop"); }
                    @Override
                    public void setSystemProxyEnabled(boolean enabled) {}
                    @Override
                    public void writeDebugMessage(String message) { log("CSH.debug: " + message); }
                };

                com.hiddify.core.libbox.CommandServer server = com.hiddify.core.libbox.Libbox.newCommandServer(handler, platform);
                log("CommandServer created");

                server.start();
                log("CommandServer.start() OK");

                com.hiddify.core.libbox.OverrideOptions overrideOptions = new com.hiddify.core.libbox.OverrideOptions();
                server.startOrReloadService(configJson, overrideOptions);
                log("startOrReloadService() OK — sing-box should be running");

                boxService = server;
            } catch (Throwable t) {
                log("CommandServer FAILED: " + t.getClass().getName() + ": " + t.getMessage());
                if (t.getCause() != null) log("  cause: " + t.getCause().getMessage());
                return false;
            }

            // Wait for sing-box to bind the port and verify
            boolean portOpen = false;
            for (int attempt = 0; attempt < 10; attempt++) {
                Thread.sleep(500);
                try {
                    java.net.Socket testSocket = new java.net.Socket();
                    testSocket.connect(new java.net.InetSocketAddress("127.0.0.1", localPort), 500);
                    testSocket.close();
                    portOpen = true;
                    break;
                } catch (Exception ignored) {}
            }

            if (!portOpen) {
                log("port " + localPort + " not listening after 5s");
                // Read sing-box log
                try {
                    File sbLog = new File(baseDir, "singbox.log");
                    if (sbLog.exists()) {
                        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(sbLog));
                        String line;
                        while ((line = br.readLine()) != null) {
                            log("sb: " + line);
                        }
                        br.close();
                    } else {
                        log("sing-box log file does not exist - core never started");
                    }
                } catch (Exception ignored) {}
                return false;
            }

            running = true;
            log("started on 127.0.0.1:" + localPort);
            return true;
        } catch (Exception e) {
            log("start failed: " + e.getMessage());
            running = false;
            return false;
        }
    }

    /**
     * Stop the sing-box instance.
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        if (boxService instanceof com.hiddify.core.libbox.CommandServer) {
            com.hiddify.core.libbox.CommandServer server = (com.hiddify.core.libbox.CommandServer) boxService;
            try {
                server.closeService();
                log("CommandServer.closeService OK");
            } catch (Exception e) {
                log("closeService error: " + e.getMessage());
            }
            try {
                server.close();
                log("CommandServer.close OK");
            } catch (Exception e) {
                log("close error: " + e.getMessage());
            }
            boxService = null;
        }
        running = false;
        localPort = 0;
        localUsername = "";
        localPassword = "";
    }

    private int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            // Fallback to random port in ephemeral range
            return 49152 + (int) (Math.random() * 16383);
        }
    }

    private String generateConfig(SharedConfig.ProxyInfo proxy) throws Exception {
        JSONObject config = new JSONObject();

        // Log to file (viewable via /singbox endpoint)
        JSONObject logObj = new JSONObject();
        logObj.put("level", "info");
        File sbLog = new File(baseDir, "singbox.log");
        // Truncate previous log on each start
        if (sbLog.exists()) sbLog.delete();
        logObj.put("output", sbLog.getAbsolutePath());
        logObj.put("timestamp", true);
        config.put("log", logObj);

        // DNS: use DoH to bypass mobile carrier DNS filtering
        // dns-direct must come FIRST since dns-remote references it
        JSONObject dns = new JSONObject();
        JSONArray dnsServers = new JSONArray();

        JSONObject dnsDirect = new JSONObject();
        dnsDirect.put("tag", "dns-direct");
        dnsDirect.put("address", "1.1.1.1");
        dnsDirect.put("detour", "direct");
        dnsDirect.put("strategy", "ipv4_only");
        dnsServers.put(dnsDirect);

        JSONObject dnsRemote = new JSONObject();
        dnsRemote.put("tag", "dns-remote");
        dnsRemote.put("address", "https://1.1.1.1/dns-query");
        dnsRemote.put("address_resolver", "dns-direct");
        dnsRemote.put("strategy", "ipv4_only");
        dnsServers.put(dnsRemote);

        dns.put("servers", dnsServers);
        dns.put("final", "dns-direct");
        dns.put("strategy", "ipv4_only");
        dns.put("disable_cache", false);
        config.put("dns", dns);

        // Inbound: local SOCKS5 with auth
        JSONObject inbound = new JSONObject();
        inbound.put("type", "socks");
        inbound.put("tag", "socks-in");
        inbound.put("listen", "127.0.0.1");
        inbound.put("listen_port", localPort);
        JSONArray users = new JSONArray();
        JSONObject user = new JSONObject();
        user.put("username", localUsername);
        user.put("password", localPassword);
        users.put(user);
        inbound.put("users", users);

        JSONArray inbounds = new JSONArray();
        inbounds.put(inbound);
        config.put("inbounds", inbounds);

        // Outbound: VLESS
        JSONObject outbound = new JSONObject();
        outbound.put("type", "vless");
        outbound.put("tag", "vless-out");
        outbound.put("server", proxy.address);
        outbound.put("server_port", proxy.port);
        outbound.put("uuid", proxy.vlessUuid);
        outbound.put("packet_encoding", "xudp");
        outbound.put("domain_strategy", "ipv4_only");
        if (!TextUtils.isEmpty(proxy.vlessFlow)) {
            outbound.put("flow", proxy.vlessFlow);
        }

        // TLS settings
        String security = proxy.vlessSecurity;
        if ("tls".equals(security) || "reality".equals(security)) {
            JSONObject tls = new JSONObject();
            tls.put("enabled", true);
            if (!TextUtils.isEmpty(proxy.vlessSni)) {
                tls.put("server_name", proxy.vlessSni);
            }
            if ("reality".equals(security)) {
                JSONObject reality = new JSONObject();
                reality.put("enabled", true);
                if (!TextUtils.isEmpty(proxy.vlessPublicKey)) {
                    reality.put("public_key", proxy.vlessPublicKey);
                }
                if (!TextUtils.isEmpty(proxy.vlessShortId)) {
                    reality.put("short_id", proxy.vlessShortId);
                }
                tls.put("reality", reality);
            }
            if (!TextUtils.isEmpty(proxy.vlessAlpn)) {
                JSONArray alpnArray = new JSONArray();
                for (String a : proxy.vlessAlpn.split(",")) {
                    alpnArray.put(a.trim());
                }
                tls.put("alpn", alpnArray);
            }
            if (!TextUtils.isEmpty(proxy.vlessFingerprint)) {
                JSONObject utls = new JSONObject();
                utls.put("enabled", true);
                utls.put("fingerprint", proxy.vlessFingerprint);
                tls.put("utls", utls);
            }
            outbound.put("tls", tls);
        }

        // Transport settings
        String transport = proxy.vlessTransport;
        if (!TextUtils.isEmpty(transport) && !"tcp".equals(transport)) {
            JSONObject transportObj = new JSONObject();
            transportObj.put("type", transport);
            if ("ws".equals(transport)) {
                if (!TextUtils.isEmpty(proxy.vlessTransportPath)) {
                    transportObj.put("path", proxy.vlessTransportPath);
                }
            } else if ("grpc".equals(transport)) {
                if (!TextUtils.isEmpty(proxy.vlessTransportPath)) {
                    transportObj.put("service_name", proxy.vlessTransportPath);
                }
            } else if ("http".equals(transport) || "h2".equals(transport)) {
                transportObj.put("type", "http");
                if (!TextUtils.isEmpty(proxy.vlessTransportPath)) {
                    transportObj.put("path", proxy.vlessTransportPath);
                }
            }
            outbound.put("transport", transportObj);
        }

        // Direct outbound (for DNS)
        JSONObject directOutbound = new JSONObject();
        directOutbound.put("type", "direct");
        directOutbound.put("tag", "direct");

        JSONArray outbounds = new JSONArray();
        outbounds.put(outbound);
        outbounds.put(directOutbound);
        config.put("outbounds", outbounds);

        // Route: all traffic through VLESS
        JSONObject route = new JSONObject();
        route.put("final", "vless-out");
        config.put("route", route);

        return config.toString(2);
    }

}
