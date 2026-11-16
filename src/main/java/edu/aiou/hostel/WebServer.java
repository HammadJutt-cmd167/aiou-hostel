package edu.aiou.hostel;

import com.sun.net.httpserver.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class WebServer {
    private static HttpServer server;
    private static int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    private static String serverUrl = "http://localhost:8080";
    private static volatile String publicUrl = null;

    public static synchronized void start() {
        if (server != null) return;
        try {
            port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
            // Find true local network IP
            String hostIp = getLocalIpAddress();
            serverUrl = "http://" + hostIp + ":" + port;

            server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

            // Contexts
            server.createContext("/", new RootHandler());
            server.createContext("/manifest.json", new ManifestHandler());
            server.createContext("/sw.js", new ServiceWorkerHandler());
            server.createContext("/icon-192.png", new IconHandler(192));
            server.createContext("/icon-512.png", new IconHandler(512));
            server.createContext("/download", new DownloadPageHandler());
            server.createContext("/download/windows", new FileDownloadHandler("target/HostelApp-Windows.zip", "HostelManagementSystem-Windows.zip", "application/zip"));
            server.createContext("/download/jar", new FileDownloadHandler("target/HostelApp.jar", "HostelApp.jar", "application/java-archive"));

            // APIs
            server.createContext("/api/info", new InfoHandler());
            server.createContext("/api/login", new LoginHandler());
            server.createContext("/api/register", new RegisterHandler());
            server.createContext("/api/overview", new OverviewHandler());
            server.createContext("/api/rooms", new RoomsHandler());
            server.createContext("/api/book-room", new BookRoomHandler());
            server.createContext("/api/vacate-room", new VacateRoomHandler());
            server.createContext("/api/complaints", new ComplaintsHandler());
            server.createContext("/api/payments", new PaymentsHandler());
            server.createContext("/api/change-password", new ChangePasswordHandler());

            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();

            System.out.println("=================================================");
            System.out.println("  HOSTEL Mobile & Web Portal Server Running!");
            System.out.println("  Local URL:   http://localhost:" + port);
            System.out.println("  Network URL: " + serverUrl);
            System.out.println("=================================================");

            // Start background tunnel only for local development (not when deployed on cloud with RENDER or RAILWAY)
            boolean isCloud = System.getenv("RENDER") != null || System.getenv("RAILWAY_ENVIRONMENT") != null || System.getenv("DYNO") != null;
            if (!isCloud) {
                Thread tunnel = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        // 1. Try serveo.net (ultra-reliable, high speed in Pakistan)
                        try {
                            ProcessBuilder pb = new ProcessBuilder(
                                    "ssh", "-o", "StrictHostKeyChecking=no",
                                    "-o", "ExitOnForwardFailure=yes",
                                    "-o", "ServerAliveInterval=10",
                                    "-o", "ServerAliveCountMax=2",
                                    "-R", "80:127.0.0.1:" + port,
                                    "serveo.net"
                            );
                            pb.redirectErrorStream(true);
                            Process p = pb.start();
                            Runtime.getRuntime().addShutdownHook(new Thread(p::destroyForcibly));

                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (line.contains(".serveousercontent.com")) {
                                        for (String token : line.split("\\s+")) {
                                            if (token.startsWith("https://") && token.contains(".serveousercontent.com")) {
                                                saveMobileUrl(token.trim());
                                                System.out.println("=================================================");
                                                System.out.println("  >>> PUBLIC MOBILE ACCESS URL (HTTPS): <<<");
                                                System.out.println("  " + publicUrl);
                                                System.out.println("=================================================");
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            p.waitFor();
                        } catch (Exception ignored) {}

                        // 2. Fallback to localhost.run
                        try {
                            ProcessBuilder pb = new ProcessBuilder(
                                    "ssh", "-o", "StrictHostKeyChecking=no",
                                    "-o", "ExitOnForwardFailure=yes",
                                    "-o", "ServerAliveInterval=10",
                                    "-o", "ServerAliveCountMax=2",
                                    "-R", "80:127.0.0.1:" + port,
                                    "nokey@localhost.run"
                            );
                            pb.redirectErrorStream(true);
                            Process p = pb.start();
                            Runtime.getRuntime().addShutdownHook(new Thread(p::destroyForcibly));
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (line.contains(".lhr.life") || line.contains(".localhost.run")) {
                                        for (String token : line.split("\\s+")) {
                                            if (token.startsWith("https://") && token.contains(".lhr.life")) {
                                                saveMobileUrl(token.trim());
                                                System.out.println("=================================================");
                                                System.out.println("  >>> PUBLIC MOBILE ACCESS URL (HTTPS): <<<");
                                                System.out.println("  " + publicUrl);
                                                System.out.println("=================================================");
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            p.waitFor();
                        } catch (Exception ignored) {}

                        try { Thread.sleep(2000); } catch (InterruptedException e) { break; }
                    }
                }, "HostelTunnelThread");
                tunnel.setDaemon(true);
                tunnel.start();
            }

        } catch (Exception e) {
            System.err.println("Could not start WebServer: " + e.getMessage());
        }
    }

    public static String getServerUrl() {
        return serverUrl;
    }

    public static String getPublicUrl() {
        return (publicUrl != null && !publicUrl.isEmpty()) ? publicUrl : serverUrl;
    }

    private static void saveMobileUrl(String url) {
        publicUrl = url;
        try {
            java.nio.file.Files.writeString(java.nio.file.Paths.get("PUBLIC_MOBILE_URL.txt"), url);
            String[] desktopPaths = {
                System.getProperty("user.home") + File.separator + "Desktop",
                System.getProperty("user.home") + File.separator + "OneDrive" + File.separator + "Desktop"
            };
            for (String dPath : desktopPaths) {
                File dFolder = new File(dPath);
                if (dFolder.exists()) {
                    File f = new File(dFolder, "AIOU_Hostel_Mobile_Link.txt");
                    java.nio.file.Files.writeString(f.toPath(),
                        "====================================================\n" +
                        "  AIOU HOSTEL MANAGEMENT - MOBILE & STUDENT LINK\n" +
                        "====================================================\n\n" +
                        "Public Web / Mobile Link (Open in Chrome or Edge):\n" +
                        url + "\n\n" +
                        "Local Wi-Fi Link (Same Wi-Fi Network):\n" +
                        serverUrl + "\n\n" +
                        "Sample Student Credentials:\n" +
                        "  Roll No:  0001072084\n" +
                        "  Password: 123456\n\n" +
                        "Note: On mobile, tap Chrome menu (3 dots) -> 'Install App'\n" +
                        "      to download it to your phone's home screen!\n"
                    );
                }
            }
        } catch (Exception ignored) {}
    }

    public static int getPort() {
        return port;
    }

    public static String getLocalIpAddress() {
        // 1. Try finding true outbound routing IP to internet (ignores all virtual interfaces)
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            if (ip != null && !ip.equals("0.0.0.0") && !ip.equals("127.0.0.1") && !ip.startsWith("192.168.56.")) {
                return ip;
            }
        } catch (Exception ignored) {}

        // 2. Fallback: filter network interfaces, excluding VirtualBox / VMware host-only adapters
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                String name = (iface.getName() + " " + iface.getDisplayName()).toLowerCase();
                if (name.contains("vbox") || name.contains("virtual") || name.contains("vmware") || name.contains("host-only")) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (!ip.startsWith("192.168.56.") && (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172."))) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "localhost";
    }

    // Helper: read request body
    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    // Helper: parse URL query or form body
    private static Map<String, String> parseForm(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.trim().isEmpty()) return map;

        // If it looks like JSON
        if (raw.trim().startsWith("{") && raw.trim().endsWith("}")) {
            String content = raw.trim().substring(1, raw.trim().length() - 1);
            String[] pairs = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String pair : pairs) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String k = kv[0].trim().replace("\"", "");
                    String v = kv[1].trim();
                    if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
                    map.put(k, v);
                }
            }
            return map;
        }

        // Standard urlencoded
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try {
                    String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8.name());
                    String v = URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name());
                    map.put(k, v);
                } catch (Exception ignored) {}
            }
        }
        return map;
    }

    private static Map<String, String> getQueryParams(HttpExchange ex) {
        URI uri = ex.getRequestURI();
        return parseForm(uri.getRawQuery());
    }

    private static void sendJsonResponse(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, HEAD");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        if ("HEAD".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(code, -1);
            ex.close();
            return;
        }
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendHtmlResponse(HttpExchange ex, int code, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        if ("HEAD".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(code, -1);
            ex.close();
            return;
        }
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ==========================================
    // 1. MANIFEST & SERVICE WORKER (PWA)
    // ==========================================
    static class ManifestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String manifest = "{\n" +
                    "  \"id\": \"/\",\n" +
                    "  \"name\": \"AIOU Hostel Management\",\n" +
                    "  \"short_name\": \"AIOU Hostel\",\n" +
                    "  \"description\": \"AIOU Hostel Management System & Student Portal\",\n" +
                    "  \"start_url\": \"/\",\n" +
                    "  \"scope\": \"/\",\n" +
                    "  \"display\": \"standalone\",\n" +
                    "  \"background_color\": \"#18181C\",\n" +
                    "  \"theme_color\": \"#9333EA\",\n" +
                    "  \"orientation\": \"portrait-primary\",\n" +
                    "  \"icons\": [\n" +
                    "    {\n" +
                    "      \"src\": \"/icon-192.png\",\n" +
                    "      \"sizes\": \"192x192\",\n" +
                    "      \"type\": \"image/png\",\n" +
                    "      \"purpose\": \"any maskable\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"src\": \"/icon-512.png\",\n" +
                    "      \"sizes\": \"512x512\",\n" +
                    "      \"type\": \"image/png\",\n" +
                    "      \"purpose\": \"any maskable\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            ex.getResponseHeaders().set("Content-Type", "application/manifest+json; charset=UTF-8");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            if ("HEAD".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(200, -1);
                ex.close();
                return;
            }
            byte[] b = manifest.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(b); }
        }
    }

    static class IconHandler implements HttpHandler {
        private final byte[] pngData;

        IconHandler(int size) {
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Purple modern gradient background
            GradientPaint gp = new GradientPaint(0, 0, new Color(147, 51, 234), size, size, new Color(79, 70, 229));
            g.setPaint(gp);
            g.fillRoundRect(0, 0, size, size, size / 5, size / 5);

            // White building body
            g.setColor(Color.WHITE);
            int pad = (int) (size * 0.22);
            int bWidth = size - (pad * 2);
            int bHeight = (int) (size * 0.45);
            int bx = pad;
            int by = (int) (size * 0.38);
            g.fillRoundRect(bx, by, bWidth, bHeight, size / 16, size / 16);

            // Roof triangle
            int[] xPoints = {bx - (size / 24), bx + (bWidth / 2), bx + bWidth + (size / 24)};
            int[] yPoints = {by, by - (int)(size * 0.16), by};
            g.fillPolygon(xPoints, yPoints, 3);

            // Windows
            g.setColor(new Color(147, 51, 234));
            int cols = 3;
            int rows = 2;
            int wSize = bWidth / 6;
            int wGapX = (bWidth - (cols * wSize)) / (cols + 1);
            int wGapY = (bHeight - (rows * wSize)) / (rows + 2);
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    g.fillRoundRect(bx + wGapX + c * (wSize + wGapX), by + wGapY + r * (wSize + wGapY), wSize, wSize, 4, 4);
                }
            }

            // Door
            int doorW = (int) (bWidth * 0.24);
            int doorH = (int) (bHeight * 0.38);
            int doorX = bx + (bWidth - doorW) / 2;
            int doorY = by + bHeight - doorH;
            g.fillRoundRect(doorX, doorY, doorW, doorH, 4, 4);

            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ImageIO.write(img, "png", baos);
            } catch (IOException ignored) {}
            this.pngData = baos.toByteArray();
        }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            ex.getResponseHeaders().set("Content-Type", "image/png");
            ex.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
            if ("HEAD".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(200, -1);
                ex.close();
                return;
            }
            ex.sendResponseHeaders(200, pngData.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(pngData);
            }
        }
    }

    static class ServiceWorkerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String sw = "const CACHE_NAME = 'hostel-portal-v1';\n" +
                    "self.addEventListener('install', e => { self.skipWaiting(); });\n" +
                    "self.addEventListener('activate', e => { e.waitUntil(clients.claim()); });\n" +
                    "self.addEventListener('fetch', e => {\n" +
                    "  e.respondWith(fetch(e.request).catch(() => caches.match(e.request)));\n" +
                    "});";
            ex.getResponseHeaders().set("Content-Type", "application/javascript; charset=UTF-8");
            if ("HEAD".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(200, -1);
                ex.close();
                return;
            }
            byte[] b = sw.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(b); }
        }
    }

    // ==========================================
    // 2. FILE DOWNLOAD HANDLER (.zip, .jar)
    // ==========================================
    static class FileDownloadHandler implements HttpHandler {
        private final String filePath;
        private final String downloadName;
        private final String contentType;

        FileDownloadHandler(String filePath, String downloadName, String contentType) {
            this.filePath = filePath;
            this.downloadName = downloadName;
            this.contentType = contentType;
        }

        @Override
        public void handle(HttpExchange ex) throws IOException {
            File f = new File(filePath);
            if (!f.exists()) {
                sendHtmlResponse(ex, 404, "<h3>File not found. Please compile or package the project first.</h3>");
                return;
            }
            ex.getResponseHeaders().set("Content-Type", contentType);
            ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + downloadName + "\"");
            ex.sendResponseHeaders(200, f.length());
            try (InputStream in = new FileInputStream(f);
                 OutputStream out = ex.getResponseBody()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
        }
    }

    // ==========================================
    // 3. DOWNLOAD PAGE (/download)
    // ==========================================
    static class DownloadPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String hostIp = getLocalIpAddress();
            String portalUrl = "http://" + hostIp + ":" + port;
            String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=" + URLEncoder.encode(portalUrl, "UTF-8");

            String html = "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "  <meta charset=\"UTF-8\">\n" +
                    "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "  <title>AIOU Hostel Management | Download Center</title>\n" +
                    "  <link rel=\"manifest\" href=\"/manifest.json\">\n" +
                    "  <link rel=\"icon\" type=\"image/png\" sizes=\"192x192\" href=\"/icon-192.png\">\n" +
                    "  <link rel=\"apple-touch-icon\" href=\"/icon-192.png\">\n" +
                    "  <meta name=\"theme-color\" content=\"#9333EA\">\n" +
                    "  <style>\n" +
                    "    :root { --bg: #F5F6FA; --dark: #18181C; --purple: #9333EA; --card: #FFFFFF; }\n" +
                    "    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }\n" +
                    "    body { background: var(--bg); color: var(--dark); padding: 20px; display: flex; justify-content: center; align-items: center; min-height: 100vh; }\n" +
                    "    .container { max-width: 680px; width: 100%; background: var(--card); border-radius: 20px; padding: 36px 30px; box-shadow: 0 10px 30px rgba(0,0,0,0.06); text-align: center; }\n" +
                    "    .badge { display: inline-block; background: #E9D5FF; color: #6B21A8; padding: 6px 14px; border-radius: 20px; font-weight: 700; font-size: 12px; margin-bottom: 12px; }\n" +
                    "    h1 { font-size: 26px; font-weight: 800; margin-bottom: 8px; }\n" +
                    "    p.sub { color: #6B7280; font-size: 14px; margin-bottom: 28px; }\n" +
                    "    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 28px; text-align: left; }\n" +
                    "    @media(max-width: 600px) { .grid { grid-template-columns: 1fr; } }\n" +
                    "    .card { background: #FAF5FF; border: 1.5px solid #E9D5FF; border-radius: 16px; padding: 22px; display: flex; flex-direction: column; justify-content: space-between; }\n" +
                    "    .card.blue { background: #F0F9FF; border-color: #BAE6FD; }\n" +
                    "    .card h3 { font-size: 17px; margin-bottom: 8px; display: flex; align-items: center; gap: 8px; }\n" +
                    "    .card p { font-size: 13px; color: #4B5563; margin-bottom: 16px; line-height: 1.4; }\n" +
                    "    .btn { display: inline-block; text-align: center; text-decoration: none; padding: 12px 18px; border-radius: 12px; font-weight: 700; font-size: 13px; cursor: pointer; transition: 0.2s; }\n" +
                    "    .btn-purple { background: var(--purple); color: #fff; }\n" +
                    "    .btn-purple:hover { background: #7E22CE; }\n" +
                    "    .btn-blue { background: #0284C7; color: #fff; }\n" +
                    "    .btn-blue:hover { background: #0369A1; }\n" +
                    "    .qr-box { background: #F9FAFB; border-radius: 16px; padding: 20px; border: 1px dashed #D1D5DB; margin-top: 10px; display: flex; align-items: center; justify-content: center; gap: 20px; text-align: left; }\n" +
                    "    .qr-box img { width: 110px; height: 110px; border-radius: 10px; background: white; padding: 6px; }\n" +
                    "    .qr-text h4 { font-size: 15px; margin-bottom: 4px; }\n" +
                    "    .qr-text p { font-size: 12px; color: #6B7280; line-height: 1.4; }\n" +
                    "    .qr-text a { color: var(--purple); font-weight: bold; word-break: break-all; }\n" +
                    "  </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "  <div class=\"container\">\n" +
                    "    <span class=\"badge\">ALLAMA IQBAL OPEN UNIVERSITY</span>\n" +
                    "    <h1>AIOU Hostel Management</h1>\n" +
                    "    <p class=\"sub\">Download or install AIOU Hostel Management on Mobile Phone and Laptop.</p>\n" +
                    "    \n" +
                    "    <div class=\"grid\">\n" +
                    "      <div class=\"card\">\n" +
                    "        <div>\n" +
                    "          <h3>📱 Mobile (Android / iPhone)</h3>\n" +
                    "          <p>Install the official AIOU Hostel Student App directly on your mobile phone without downloading bulky APKs.</p>\n" +
                    "        </div>\n" +
                    "        <a href=\"/\" class=\"btn btn-purple\">Open Mobile Portal &amp; Install</a>\n" +
                    "      </div>\n" +
                    "      \n" +
                    "      <div class=\"card blue\">\n" +
                    "        <div>\n" +
                    "          <h3>💻 Laptop / Windows PC</h3>\n" +
                    "          <p>Standalone Windows Application (.exe). Runs without needing Java installed.</p>\n" +
                    "        </div>\n" +
                    "        <a href=\"/download/windows\" class=\"btn btn-blue\">Download for Windows (.exe)</a>\n" +
                    "      </div>\n" +
                    "    </div>\n" +
                    "    \n" +
                    "    <div class=\"qr-box\">\n" +
                    "      <img src=\"" + qrUrl + "\" alt=\"QR Code\">\n" +
                    "      <div class=\"qr-text\">\n" +
                    "        <h4>📱 Scan with Mobile Camera</h4>\n" +
                    "        <p>Scan this QR code with your mobile camera or open: <br><a href=\"" + portalUrl + "\">AIOU Hostel Management Portal</a></p>\n" +
                    "        <p style=\"margin-top:4px;\">Tap <b>\"Install App\"</b> when prompted to add to your Home Screen.</p>\n" +
                    "      </div>\n" +
                    "    </div>\n" +
                    "  </div>\n" +
                    "</body>\n" +
                    "</html>";
            sendHtmlResponse(ex, 200, html);
        }
    }

    // ==========================================
    // 4. MAIN STUDENT PORTAL (HTML5 / PWA)
    // ==========================================
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"/".equals(ex.getRequestURI().getPath())) {
                sendHtmlResponse(ex, 404, "Page not found");
                return;
            }

            String html = "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "  <meta charset=\"UTF-8\">\n" +
                    "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n" +
                    "  <title>AIOU Hostel Management</title>\n" +
                    "  <link rel=\"manifest\" href=\"/manifest.json\">\n" +
                    "  <link rel=\"icon\" type=\"image/png\" sizes=\"192x192\" href=\"/icon-192.png\">\n" +
                    "  <link rel=\"apple-touch-icon\" href=\"/icon-192.png\">\n" +
                    "  <meta name=\"theme-color\" content=\"#9333EA\">\n" +
                    "  <meta name=\"apple-mobile-web-app-capable\" content=\"yes\">\n" +
                    "  <meta name=\"apple-mobile-web-app-status-bar-style\" content=\"black-translucent\">\n" +
                    "  <style>\n" +
                    "    :root {\n" +
                    "      --bg: #F5F6FA;\n" +
                    "      --sidebar: #18181C;\n" +
                    "      --sidebar-card: #22232A;\n" +
                    "      --sidebar-text: #A5A8B4;\n" +
                    "      --card: #FFFFFF;\n" +
                    "      --border: #ECEFF4;\n" +
                    "      --text-dark: #18191E;\n" +
                    "      --text-muted: #7D8291;\n" +
                    "      --primary: #9333EA;\n" +
                    "      --lime: #E2F992;\n" +
                    "      --lime-dark: #3F6212;\n" +
                    "      --blue: #BAE6FD;\n" +
                    "      --blue-dark: #0E7490;\n" +
                    "      --purple: #D8C4FF;\n" +
                    "      --purple-dark: #5B21B6;\n" +
                    "    }\n" +
                    "    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }\n" +
                    "    body { background: var(--bg); color: var(--text-dark); min-height: 100vh; display: flex; flex-direction: column; }\n" +
                    "    \n" +
                    "    /* Install Banner */\n" +
                    "    #installBanner { background: var(--sidebar); color: white; padding: 10px 16px; display: none; align-items: center; justify-content: space-between; font-size: 13px; border-bottom: 2px solid var(--primary); }\n" +
                    "    #installBanner button { background: var(--primary); color: white; border: none; padding: 6px 14px; border-radius: 20px; font-weight: 700; cursor: pointer; }\n" +
                    "    \n" +
                    "    /* App Container */\n" +
                    "    .app-layout { display: flex; flex: 1; height: 100vh; overflow: hidden; }\n" +
                    "    @media(max-width: 768px) { .app-layout { flex-direction: column; height: auto; min-height: 100vh; } }\n" +
                    "    \n" +
                    "    /* Sidebar */\n" +
                    "    .sidebar { width: 250px; background: var(--sidebar); padding: 24px 18px; display: flex; flex-direction: column; justify-content: space-between; }\n" +
                    "    @media(max-width: 768px) { .sidebar { width: 100%; padding: 14px 16px; flex-direction: row; align-items: center; } }\n" +
                    "    \n" +
                    "    .brand { display: flex; align-items: center; gap: 8px; color: white; font-weight: 800; font-size: 19px; }\n" +
                    "    .diamond { width: 20px; height: 20px; background: #DBF76B; border-radius: 6px; }\n" +
                    "    .badge-tag { font-size: 10px; color: #C4AFFA; font-weight: 700; margin-left: 4px; }\n" +
                    "    \n" +
                    "    .nav-links { list-style: none; margin-top: 24px; }\n" +
                    "    @media(max-width: 768px) { .nav-links { display: none; } }\n" +
                    "    .nav-item { display: flex; align-items: center; gap: 10px; padding: 10px 14px; border-radius: 10px; color: var(--sidebar-text); font-weight: 600; font-size: 13px; cursor: pointer; margin-bottom: 6px; transition: 0.2s; }\n" +
                    "    .nav-item:hover, .nav-item.active { background: white; color: var(--text-dark); }\n" +
                    "    \n" +
                    "    /* Mobile Nav Bar */\n" +
                    "    .mobile-nav { display: none; background: white; border-top: 1px solid var(--border); position: fixed; bottom: 0; left: 0; right: 0; z-index: 100; justify-content: space-around; padding: 8px 0; }\n" +
                    "    @media(max-width: 768px) { .mobile-nav { display: flex; } }\n" +
                    "    .mob-tab { text-align: center; font-size: 11px; font-weight: 600; color: var(--text-muted); cursor: pointer; text-decoration: none; flex: 1; }\n" +
                    "    .mob-tab.active { color: var(--primary); }\n" +
                    "    .mob-tab span { display: block; font-size: 18px; margin-bottom: 2px; }\n" +
                    "    \n" +
                    "    /* Main Content */\n" +
                    "    .main-view { flex: 1; overflow-y: auto; padding: 24px 28px; padding-bottom: 80px; }\n" +
                    "    @media(max-width: 768px) { .main-view { padding: 16px; padding-bottom: 90px; } }\n" +
                    "    \n" +
                    "    /* Cards & Stats */\n" +
                    "    .stat-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px; }\n" +
                    "    .stat-card { border-radius: 18px; padding: 16px 18px; display: flex; align-items: center; gap: 14px; cursor: pointer; }\n" +
                    "    .stat-card.blue { background: var(--blue); color: var(--blue-dark); }\n" +
                    "    .stat-card.lime { background: var(--lime); color: var(--lime-dark); }\n" +
                    "    .stat-card.purple { background: var(--purple); color: var(--purple-dark); }\n" +
                    "    .stat-icon { width: 44px; height: 44px; background: white; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 20px; }\n" +
                    "    .stat-info .title { font-size: 11px; font-weight: 700; text-transform: uppercase; margin-bottom: 2px; }\n" +
                    "    .stat-info .val { font-size: 20px; font-weight: 800; color: var(--text-dark); }\n" +
                    "    \n" +
                    "    .content-box { background: white; border-radius: 18px; border: 1px solid var(--border); padding: 20px; margin-bottom: 24px; }\n" +
                    "    .box-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }\n" +
                    "    .box-title { font-size: 16px; font-weight: 700; }\n" +
                    "    \n" +
                    "    /* Buttons */\n" +
                    "    .btn-action { background: var(--sidebar); color: white; border: none; padding: 8px 16px; border-radius: 10px; font-weight: 700; font-size: 12px; cursor: pointer; transition: 0.2s; }\n" +
                    "    .btn-action:hover { opacity: 0.9; }\n" +
                    "    .btn-green { background: #16A34A; color: white; }\n" +
                    "    .btn-purple { background: var(--primary); color: white; }\n" +
                    "    .btn-red { background: #DC2626; color: white; }\n" +
                    "    \n" +
                    "    /* Tables */\n" +
                    "    .table-resp { overflow-x: auto; }\n" +
                    "    table { width: 100%; border-collapse: collapse; font-size: 13px; text-align: left; }\n" +
                    "    th { padding: 10px 12px; color: var(--text-muted); font-size: 11px; font-weight: 700; border-bottom: 1px solid var(--border); }\n" +
                    "    td { padding: 12px; border-bottom: 1px solid #F3F4F6; color: var(--text-dark); }\n" +
                    "    \n" +
                    "    /* Badges */\n" +
                    "    .badge { display: inline-block; padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 700; }\n" +
                    "    .badge-active, .badge-resolved, .badge-available { background: #DCFCE7; color: #166534; }\n" +
                    "    .badge-open, .badge-unpaid, .badge-occupied { background: #FEE2E2; color: #991B1B; }\n" +
                    "    .badge-pending { background: #FEF3C7; color: #92400E; }\n" +
                    "    \n" +
                    "    /* Forms & Modal */\n" +
                    "    .form-group { margin-bottom: 14px; }\n" +
                    "    .form-group label { display: block; font-size: 12px; font-weight: 700; margin-bottom: 4px; color: var(--text-dark); }\n" +
                    "    .form-group input, .form-group select, .form-group textarea { width: 100%; padding: 10px 12px; border: 1px solid var(--border); border-radius: 8px; font-size: 13px; outline: none; }\n" +
                    "    .modal-overlay { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 200; justify-content: center; align-items: center; padding: 16px; }\n" +
                    "    .modal { background: white; border-radius: 20px; max-width: 440px; width: 100%; padding: 24px; box-shadow: 0 10px 30px rgba(0,0,0,0.2); max-height: 90vh; overflow-y: auto; }\n" +
                    "    \n" +
                    "    /* Login screen */\n" +
                    "    #loginScreen { position: fixed; inset: 0; background: var(--bg); display: flex; justify-content: center; align-items: center; z-index: 300; padding: 16px; }\n" +
                    "    .login-card { background: white; border-radius: 20px; max-width: 400px; width: 100%; padding: 32px 28px; box-shadow: 0 10px 30px rgba(0,0,0,0.06); text-align: center; }\n" +
                    "  </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "\n" +
                    "  <!-- Install Banner for Mobile -->\n" +
                    "  <div id=\"installBanner\">\n" +
                    "    <div>📱 <b>Install Hostel App</b> on your phone for quick access!</div>\n" +
                    "    <button id=\"btnInstallPwa\">Install App</button>\n" +
                    "  </div>\n" +
                    "\n" +
                    "  <!-- LOGIN SCREEN -->\n" +
                    "  <div id=\"loginScreen\">\n" +
                    "    <div class=\"login-card\">\n" +
                    "      <div class=\"diamond\" style=\"margin: 0 auto 12px; width: 28px; height: 28px;\"></div>\n" +
                    "      <h2 style=\"font-size: 20px; font-weight: 800; margin-bottom: 4px;\">AIOU Hostel Management</h2>\n" +
                    "      <p style=\"font-size: 13px; color: var(--text-muted); margin-bottom: 22px;\">Student Portal (Allama Iqbal Open University)</p>\n" +
                    "      \n" +
                    "      <div class=\"form-group\" style=\"text-align: left;\">\n" +
                    "        <label>Student Roll No / Email</label>\n" +
                    "        <input type=\"text\" id=\"loginUser\" placeholder=\"e.g. 0001072084\" autofocus>\n" +
                    "      </div>\n" +
                    "      <div class=\"form-group\" style=\"text-align: left;\">\n" +
                    "        <label>Password (Default: 123456)</label>\n" +
                    "        <input type=\"password\" id=\"loginPass\" placeholder=\"Password\">\n" +
                    "      </div>\n" +
                    "      \n" +
                    "      <button class=\"btn-action btn-purple\" style=\"width: 100%; padding: 12px; font-size: 14px; margin-top: 10px;\" onclick=\"doLogin()\">Sign In</button>\n" +
                    "      \n" +
                    "      <div style=\"margin-top: 18px; font-size: 12px;\">\n" +
                    "        <a href=\"#\" onclick=\"showRegisterModal()\" style=\"color: var(--primary); font-weight: 700; text-decoration: none;\">New student? Register here →</a>\n" +
                    "      </div>\n" +
                    "      <div style=\"margin-top: 14px; font-size: 11px; color: var(--text-muted);\">\n" +
                    "        Default password for existing students is: <b>123456</b>\n" +
                    "      </div>\n" +
                    "    </div>\n" +
                    "  </div>\n" +
                    "\n" +
                    "  <!-- MAIN APP LAYOUT -->\n" +
                    "  <div class=\"app-layout\" id=\"appLayout\" style=\"display: none;\">\n" +
                    "    <div class=\"sidebar\">\n" +
                    "      <div>\n" +
                    "        <div class=\"brand\">\n" +
                    "          <div class=\"diamond\"></div>\n" +
                    "          AIOU HOSTEL\n" +
                    "          <span class=\"badge-tag\">PORTAL</span>\n" +
                    "        </div>\n" +
                    "        <div style=\"background: var(--sidebar-card); border-radius: 12px; padding: 10px; margin-top: 18px; color: white;\">\n" +
                    "          <div id=\"sideName\" style=\"font-weight: 700; font-size: 13px;\">Student Name</div>\n" +
                    "          <div id=\"sideRoll\" style=\"font-size: 11px; color: var(--sidebar-text);\">Roll No • BSCS</div>\n" +
                    "        </div>\n" +
                    "        <ul class=\"nav-links\">\n" +
                    "          <li class=\"nav-item active\" onclick=\"showTab('overview')\">📊 Overview</li>\n" +
                    "          <li class=\"nav-item\" onclick=\"showTab('room')\">🛏️ Room &amp; Booking</li>\n" +
                    "          <li class=\"nav-item\" onclick=\"showTab('complaints')\">💬 Complaints</li>\n" +
                    "          <li class=\"nav-item\" onclick=\"showTab('fee')\">💳 Fee Status</li>\n" +
                    "          <li class=\"nav-item\" onclick=\"showTab('profile')\">👤 My Profile</li>\n" +
                    "          <li class=\"nav-item\" onclick=\"window.open('/download', '_blank')\">📥 Download App</li>\n" +
                    "        </ul>\n" +
                    "      </div>\n" +
                    "      <div style=\"text-align: left;\">\n" +
                    "        <button class=\"btn-action btn-red\" style=\"width: 100%;\" onclick=\"doLogout()\">🚪 Sign Out</button>\n" +
                    "      </div>\n" +
                    "    </div>\n" +
                    "\n" +
                    "    <!-- Main Views -->\n" +
                    "    <div class=\"main-view\">\n" +
                    "      \n" +
                    "      <!-- TAB 1: OVERVIEW -->\n" +
                    "      <div id=\"tab-overview\">\n" +
                    "        <div style=\"margin-bottom: 20px;\">\n" +
                    "          <h2 id=\"greetTitle\" style=\"font-size: 22px; font-weight: 800;\">Welcome back</h2>\n" +
                    "          <p id=\"greetSub\" style=\"color: var(--text-muted); font-size: 13px;\">Hostel Resident Portal</p>\n" +
                    "        </div>\n" +
                    "        <div class=\"stat-row\">\n" +
                    "          <div class=\"stat-card blue\" onclick=\"showTab('room')\">\n" +
                    "            <div class=\"stat-icon\">🛏️</div>\n" +
                    "            <div class=\"stat-info\">\n" +
                    "              <div class=\"title\">Allocated Room</div>\n" +
                    "              <div class=\"val\" id=\"statRoom\">Loading...</div>\n" +
                    "            </div>\n" +
                    "          </div>\n" +
                    "          <div class=\"stat-card lime\" onclick=\"showTab('complaints')\">\n" +
                    "            <div class=\"stat-icon\">💬</div>\n" +
                    "            <div class=\"stat-info\">\n" +
                    "              <div class=\"title\">My Complaints</div>\n" +
                    "              <div class=\"val\" id=\"statComp\">0 Open</div>\n" +
                    "            </div>\n" +
                    "          </div>\n" +
                    "          <div class=\"stat-card purple\" onclick=\"showTab('fee')\">\n" +
                    "            <div class=\"stat-icon\">💳</div>\n" +
                    "            <div class=\"stat-info\">\n" +
                    "              <div class=\"title\">Total Fee Paid</div>\n" +
                    "              <div class=\"val\" id=\"statFee\">Rs. 0</div>\n" +
                    "            </div>\n" +
                    "          </div>\n" +
                    "        </div>\n" +
                    "        \n" +
                    "        <div class=\"content-box\">\n" +
                    "          <div class=\"box-header\">\n" +
                    "            <div class=\"box-title\">Recent Complaints Status</div>\n" +
                    "            <button class=\"btn-action btn-green\" onclick=\"showModal('complaintModal')\">+ Lodge Complaint</button>\n" +
                    "          </div>\n" +
                    "          <div class=\"table-resp\">\n" +
                    "            <table id=\"recentCompTable\">\n" +
                    "              <thead><tr><th>Category</th><th>Subject</th><th>Status</th><th>Date</th></tr></thead>\n" +
                    "              <tbody><tr><td colspan=\"4\" style=\"text-align:center;\">Loading...</td></tr></tbody>\n" +
                    "            </table>\n" +
                    "          </div>\n" +
                    "        </div>\n" +
                    "      </div>\n" +
                    "\n" +
                    "      <!-- TAB 2: ROOM & BOOKING -->\n" +
                    "      <div id=\"tab-room\" style=\"display:none;\">\n" +
                    "        <h2 style=\"font-size: 22px; font-weight: 800; margin-bottom: 16px;\">My Room &amp; Hostel Booking</h2>\n" +
                    "        <div class=\"content-box\" id=\"curRoomCard\">\n" +
                    "          <div class=\"box-header\">\n" +
                    "            <div class=\"box-title\">Current Room Allocation</div>\n" +
                    "          </div>\n" +
                    "          <div id=\"curRoomDetails\">Loading...</div>\n" +
                    "        </div>\n" +
                    "        \n" +
                    "        <div class=\"content-box\">\n" +
                    "          <div class=\"box-header\">\n" +
                    "            <div class=\"box-title\">Available Hostel Rooms (Select &amp; Book)</div>\n" +
                    "          </div>\n" +
                    "          <div class=\"table-resp\">\n" +
                    "            <table id=\"availRoomsTable\">\n" +
                    "              <thead><tr><th>Room No</th><th>Type</th><th>Floor</th><th>Rent (Rs)</th><th>Action</th></tr></thead>\n" +
                    "              <tbody><tr><td colspan=\"5\" style=\"text-align:center;\">Loading available rooms...</td></tr></tbody>\n" +
                    "            </table>\n" +
                    "          </div>\n" +
                    "        </div>\n" +
                    "      </div>\n" +
                    "\n" +
                    "      <!-- TAB 3: COMPLAINTS -->\n" +
                    "      <div id=\"tab-complaints\" style=\"display:none;\">\n" +
                    "        <div class=\"box-header\" style=\"margin-bottom: 16px;\">\n" +
                    "          <h2 style=\"font-size: 22px; font-weight: 800;\">My Complaints</h2>\n" +
                    "          <button class=\"btn-action btn-green\" onclick=\"showModal('complaintModal')\">+ Lodge Complaint</button>\n" +
                    "        </div>\n" +
                    "        <div class=\"content-box\">\n" +
                    "          <div class=\"table-resp\">\n" +
                    "            <table id=\"allCompTable\">\n" +
                    "              <thead><tr><th>Category</th><th>Subject</th><th>Details</th><th>Status</th><th>Submitted</th></tr></thead>\n" +
                    "              <tbody><tr><td colspan=\"5\" style=\"text-align:center;\">Loading complaints...</td></tr></tbody>\n" +
                    "            </table>\n" +
                    "          </div>\n" +
                    "        </div>\n" +
                    "      </div>\n" +
                    "\n" +
                    "      <!-- TAB 4: FEE & PAYMENTS -->\n" +
                    "      <div id=\"tab-fee\" style=\"display:none;\">\n" +
                    "        <div class=\"box-header\" style=\"margin-bottom: 16px;\">\n" +
                    "          <h2 style=\"font-size: 22px; font-weight: 800;\">Fee &amp; Payment History</h2>\n" +
                    "          <button class=\"btn-action btn-purple\" onclick=\"showModal('paymentModal')\">+ Report Payment</button>\n" +
                    "        </div>\n" +
                    "        <div class=\"content-box\">\n" +
                    "          <div class=\"table-resp\">\n" +
                    "            <table id=\"paymentsTable\">\n" +
                    "              <thead><tr><th>Month</th><th>Amount</th><th>Method</th><th>Reference No</th><th>Date</th></tr></thead>\n" +
                    "              <tbody><tr><td colspan=\"5\" style=\"text-align:center;\">Loading payment history...</td></tr></tbody>\n" +
                    "            </table>\n" +
                    "          </div>\n" +
                    "        </div>\n" +
                    "      </div>\n" +
                    "\n" +
                    "      <!-- TAB 5: PROFILE -->\n" +
                    "      <div id=\"tab-profile\" style=\"display:none;\">\n" +
                    "        <h2 style=\"font-size: 22px; font-weight: 800; margin-bottom: 16px;\">My Profile &amp; Settings</h2>\n" +
                    "        <div class=\"content-box\">\n" +
                    "          <div class=\"box-title\" style=\"margin-bottom: 14px;\">Personal Information</div>\n" +
                    "          <div id=\"profInfoGrid\" style=\"display:grid; grid-template-columns: 1fr 1fr; gap:12px; font-size: 13px;\"></div>\n" +
                    "        </div>\n" +
                    "        <div class=\"content-box\">\n" +
                    "          <div class=\"box-title\" style=\"margin-bottom: 14px;\">Change Password</div>\n" +
                    "          <div class=\"form-group\"><label>Current Password</label><input type=\"password\" id=\"pCur\"></div>\n" +
                    "          <div class=\"form-group\"><label>New Password</label><input type=\"password\" id=\"pNew\"></div>\n" +
                    "          <div class=\"form-group\"><label>Confirm New Password</label><input type=\"password\" id=\"pConf\"></div>\n" +
                    "          <button class=\"btn-action btn-purple\" onclick=\"doChangePassword()\">Update Password</button>\n" +
                    "        </div>\n" +
                    "      </div>\n" +
                    "\n" +
                    "    </div>\n" +
                    "  </div>\n" +
                    "\n" +
                    "  <!-- Bottom Mobile Navigation -->\n" +
                    "  <div class=\"mobile-nav\">\n" +
                    "    <a class=\"mob-tab active\" id=\"mob-overview\" onclick=\"showTab('overview')\"><span>📊</span>Overview</a>\n" +
                    "    <a class=\"mob-tab\" id=\"mob-room\" onclick=\"showTab('room')\"><span>🛏️</span>Room</a>\n" +
                    "    <a class=\"mob-tab\" id=\"mob-complaints\" onclick=\"showTab('complaints')\"><span>💬</span>Complaints</a>\n" +
                    "    <a class=\"mob-tab\" id=\"mob-fee\" onclick=\"showTab('fee')\"><span>💳</span>Fees</a>\n" +
                    "    <a class=\"mob-tab\" id=\"mob-profile\" onclick=\"showTab('profile')\"><span>👤</span>Profile</a>\n" +
                    "  </div>\n" +
                    "\n" +
                    "  <!-- MODAL: LODGE COMPLAINT -->\n" +
                    "  <div class=\"modal-overlay\" id=\"complaintModal\">\n" +
                    "    <div class=\"modal\">\n" +
                    "      <h3 style=\"margin-bottom: 16px;\">💬 Lodge a Complaint</h3>\n" +
                    "      <div class=\"form-group\">\n" +
                    "        <label>Category</label>\n" +
                    "        <select id=\"cCat\">\n" +
                    "          <option>Maintenance</option>\n" +
                    "          <option>Cleanliness</option>\n" +
                    "          <option>Food &amp; Mess</option>\n" +
                    "          <option>Electricity &amp; Lights</option>\n" +
                    "          <option>Water Supply</option>\n" +
                    "          <option>Security</option>\n" +
                    "          <option>Internet &amp; Wi-Fi</option>\n" +
                    "          <option>Other</option>\n" +
                    "        </select>\n" +
                    "      </div>\n" +
                    "      <div class=\"form-group\">\n" +
                    "        <label>Subject</label>\n" +
                    "        <input type=\"text\" id=\"cSub\" placeholder=\"Brief summary of the issue\">\n" +
                    "      </div>\n" +
                    "      <div class=\"form-group\">\n" +
                    "        <label>Details</label>\n" +
                    "        <textarea id=\"cDet\" rows=\"3\" placeholder=\"Detailed explanation...\"></textarea>\n" +
                    "      </div>\n" +
                    "      <div style=\"display:flex; justify-content: flex-end; gap: 10px;\">\n" +
                    "        <button class=\"btn-action\" style=\"background:#E5E7EB; color:#374151;\" onclick=\"hideModal('complaintModal')\">Cancel</button>\n" +
                    "        <button class=\"btn-action btn-green\" onclick=\"doSubmitComplaint()\">Submit Complaint</button>\n" +
                    "      </div>\n" +
                    "    </div>\n" +
                    "  </div>\n" +
                    "\n" +
                    "  <!-- MODAL: REPORT PAYMENT -->\n" +
                    "  <div class=\"modal-overlay\" id=\"paymentModal\">\n" +
                    "    <div class=\"modal\">\n" +
                    "      <h3 style=\"margin-bottom: 16px;\">💳 Report Fee Payment</h3>\n" +
                    "      <div class=\"form-group\"><label>Amount (Rs)</label><input type=\"number\" id=\"payAmt\" value=\"12000\"></div>\n" +
                    "      <div class=\"form-group\"><label>Month For</label><input type=\"text\" id=\"payMonth\" value=\"November 2026\"></div>\n" +
                    "      <div class=\"form-group\"><label>Payment Date</label><input type=\"date\" id=\"payDate\"></div>\n" +
                    "      <div class=\"form-group\">\n" +
                    "        <label>Payment Method</label>\n" +
                    "        <select id=\"payMethod\">\n" +
                    "          <option>Bank Transfer</option>\n" +
                    "          <option>Online / EasyPaisa / JazzCash</option>\n" +
                    "          <option>Cash</option>\n" +
                    "        </select>\n" +
                    "      </div>\n" +
                    "      <div class=\"form-group\"><label>Receipt / Reference No</label><input type=\"text\" id=\"payRef\" placeholder=\"e.g. TRX198273\"></div>\n" +
                    "      <div style=\"display:flex; justify-content: flex-end; gap: 10px;\">\n" +
                    "        <button class=\"btn-action\" style=\"background:#E5E7EB; color:#374151;\" onclick=\"hideModal('paymentModal')\">Cancel</button>\n" +
                    "        <button class=\"btn-action btn-purple\" onclick=\"doSubmitPayment()\">Submit</button>\n" +
                    "      </div>\n" +
                    "    </div>\n" +
                    "  </div>\n" +
                    "\n" +
                    "  <!-- MODAL: REGISTER NEW STUDENT -->\n" +
                    "  <div class=\"modal-overlay\" id=\"registerModal\">\n" +
                    "    <div class=\"modal\">\n" +
                    "      <h3 style=\"margin-bottom: 16px;\">🎓 Student Registration</h3>\n" +
                    "      <div class=\"form-group\"><label>Roll / Reg No *</label><input type=\"text\" id=\"rNo\" placeholder=\"e.g. 0001072085\"></div>\n" +
                    "      <div class=\"form-group\"><label>Full Name *</label><input type=\"text\" id=\"rName\" placeholder=\"Full Name\"></div>\n" +
                    "      <div class=\"form-group\"><label>Department</label><input type=\"text\" id=\"rDept\" value=\"BSCS\"></div>\n" +
                    "      <div class=\"form-group\"><label>Phone</label><input type=\"text\" id=\"rPhone\"></div>\n" +
                    "      <div class=\"form-group\"><label>Email</label><input type=\"email\" id=\"rEmail\"></div>\n" +
                    "      <div class=\"form-group\"><label>Password *</label><input type=\"password\" id=\"rPass\"></div>\n" +
                    "      <div style=\"display:flex; justify-content: flex-end; gap: 10px;\">\n" +
                    "        <button class=\"btn-action\" style=\"background:#E5E7EB; color:#374151;\" onclick=\"hideModal('registerModal')\">Cancel</button>\n" +
                    "        <button class=\"btn-action btn-purple\" onclick=\"doRegister()\">Register Account</button>\n" +
                    "      </div>\n" +
                    "    </div>\n" +
                    "  </div>\n" +
                    "\n" +
                    "  <script>\n" +
                    "    let session = JSON.parse(localStorage.getItem('hostel_session') || 'null');\n" +
                    "    let deferredPrompt = null;\n" +
                    "\n" +
                    "    // Register Service Worker for PWA / WebAPK\n" +
                    "    if ('serviceWorker' in navigator) {\n" +
                    "      navigator.serviceWorker.register('/sw.js').catch(console.error);\n" +
                    "    }\n" +
                    "\n" +
                    "    window.addEventListener('beforeinstallprompt', e => {\n" +
                    "      e.preventDefault();\n" +
                    "      deferredPrompt = e;\n" +
                    "      document.getElementById('installBanner').style.display = 'flex';\n" +
                    "    });\n" +
                    "\n" +
                    "    document.getElementById('btnInstallPwa').addEventListener('click', () => {\n" +
                    "      if (deferredPrompt) {\n" +
                    "        deferredPrompt.prompt();\n" +
                    "        deferredPrompt.userChoice.then(() => {\n" +
                    "          document.getElementById('installBanner').style.display = 'none';\n" +
                    "          deferredPrompt = null;\n" +
                    "        });\n" +
                    "      }\n" +
                    "    });\n" +
                    "\n" +
                    "    document.getElementById('payDate').value = new Date().toISOString().split('T')[0];\n" +
                    "\n" +
                    "    if (session) {\n" +
                    "      showApp();\n" +
                    "    }\n" +
                    "\n" +
                    "    function showModal(id) { document.getElementById(id).style.display = 'flex'; }\n" +
                    "    function hideModal(id) { document.getElementById(id).style.display = 'none'; }\n" +
                    "    function showRegisterModal() { showModal('registerModal'); }\n" +
                    "\n" +
                    "    async function doLogin() {\n" +
                    "      const u = document.getElementById('loginUser').value.trim();\n" +
                    "      const p = document.getElementById('loginPass').value;\n" +
                    "      if (!u || !p) return alert('Please enter Roll No and Password.');\n" +
                    "      const res = await fetch('/api/login', {\n" +
                    "        method: 'POST',\n" +
                    "        headers: {'Content-Type': 'application/json'},\n" +
                    "        body: JSON.stringify({roll: u, pass: p})\n" +
                    "      });\n" +
                    "      const data = await res.json();\n" +
                    "      if (data.success) {\n" +
                    "        session = data.session;\n" +
                    "        localStorage.setItem('hostel_session', JSON.stringify(session));\n" +
                    "        showApp();\n" +
                    "      } else {\n" +
                    "        alert(data.message || 'Login failed.');\n" +
                    "      }\n" +
                    "    }\n" +
                    "\n" +
                    "    async function doRegister() {\n" +
                    "      const no = document.getElementById('rNo').value.trim();\n" +
                    "      const name = document.getElementById('rName').value.trim();\n" +
                    "      const dept = document.getElementById('rDept').value.trim();\n" +
                    "      const phone = document.getElementById('rPhone').value.trim();\n" +
                    "      const email = document.getElementById('rEmail').value.trim();\n" +
                    "      const pass = document.getElementById('rPass').value;\n" +
                    "      if (!no || !name || !pass) return alert('Roll No, Name and Password are required.');\n" +
                    "      const res = await fetch('/api/register', {\n" +
                    "        method: 'POST',\n" +
                    "        headers: {'Content-Type': 'application/json'},\n" +
                    "        body: JSON.stringify({studentNo: no, fullName: name, department: dept, phone, email, password: pass})\n" +
                    "      });\n" +
                    "      const data = await res.json();\n" +
                    "      if (data.success) {\n" +
                    "        alert('Account registered successfully! Please sign in.');\n" +
                    "        hideModal('registerModal');\n" +
                    "        document.getElementById('loginUser').value = no;\n" +
                    "      } else {\n" +
                    "        alert(data.message || 'Registration failed.');\n" +
                    "      }\n" +
                    "    }\n" +
                    "\n" +
                    "    function doLogout() {\n" +
                    "      localStorage.removeItem('hostel_session');\n" +
                    "      session = null;\n" +
                    "      document.getElementById('appLayout').style.display = 'none';\n" +
                    "      document.getElementById('loginScreen').style.display = 'flex';\n" +
                    "    }\n" +
                    "\n" +
                    "    function showApp() {\n" +
                    "      document.getElementById('loginScreen').style.display = 'none';\n" +
                    "      document.getElementById('appLayout').style.display = 'flex';\n" +
                    "      document.getElementById('sideName').innerText = session.fullName;\n" +
                    "      document.getElementById('sideRoll').innerText = session.studentNo + ' • ' + (session.department || 'AIOU');\n" +
                    "      document.getElementById('greetTitle').innerText = 'Welcome, ' + session.fullName;\n" +
                    "      document.getElementById('greetSub').innerText = 'Roll No: ' + session.studentNo + ' • ' + session.department;\n" +
                    "      loadOverview();\n" +
                    "    }\n" +
                    "\n" +
                    "    function showTab(t) {\n" +
                    "      ['overview', 'room', 'complaints', 'fee', 'profile'].forEach(id => {\n" +
                    "        document.getElementById('tab-' + id).style.display = (id === t ? 'block' : 'none');\n" +
                    "        const mob = document.getElementById('mob-' + id);\n" +
                    "        if (mob) mob.className = (id === t ? 'mob-tab active' : 'mob-tab');\n" +
                    "      });\n" +
                    "      document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));\n" +
                    "      if (t === 'overview') loadOverview();\n" +
                    "      if (t === 'room') loadRooms();\n" +
                    "      if (t === 'complaints') loadComplaints();\n" +
                    "      if (t === 'fee') loadPayments();\n" +
                    "      if (t === 'profile') loadProfile();\n" +
                    "    }\n" +
                    "\n" +
                    "    async function loadOverview() {\n" +
                    "      const res = await fetch('/api/overview?studentId=' + session.id);\n" +
                    "      const data = await res.json();\n" +
                    "      document.getElementById('statRoom').innerText = data.room || 'None';\n" +
                    "      document.getElementById('statComp').innerText = data.openComplaints + ' Open';\n" +
                    "      document.getElementById('statFee').innerText = 'Rs. ' + Number(data.totalPaid).toLocaleString();\n" +
                    "      \n" +
                    "      const tb = document.querySelector('#recentCompTable tbody');\n" +
                    "      if (!data.recentComplaints || data.recentComplaints.length === 0) {\n" +
                    "        tb.innerHTML = '<tr><td colspan=\"4\" style=\"text-align:center; color:#9CA3AF;\">No complaints registered yet.</td></tr>';\n" +
                    "      } else {\n" +
                    "        tb.innerHTML = data.recentComplaints.map(c => `\n" +
                    "          <tr>\n" +
                    "            <td><b>${c.category}</b></td>\n" +
                    "            <td>${c.subject}</td>\n" +
                    "            <td><span class=\"badge badge-${c.status.toLowerCase()}\">${c.status}</span></td>\n" +
                    "            <td>${c.date}</td>\n" +
                    "          </tr>\n" +
                    "        `).join('');\n" +
                    "      }\n" +
                    "    }\n" +
                    "\n" +
                    "    async function loadRooms() {\n" +
                    "      const res = await fetch('/api/rooms?studentId=' + session.id);\n" +
                    "      const data = await res.json();\n" +
                    "      \n" +
                    "      const curDiv = document.getElementById('curRoomDetails');\n" +
                    "      if (data.currentAllocation) {\n" +
                    "        const a = data.currentAllocation;\n" +
                    "        curDiv.innerHTML = `\n" +
                    "          <div style=\"display:grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 12px; margin-bottom: 14px;\">\n" +
                    "            <div><span style=\"font-size:11px; color:#6B7280;\">Room</span><br><b>Room ${a.room_no}</b></div>\n" +
                    "            <div><span style=\"font-size:11px; color:#6B7280;\">Type / Bed</span><br><b>${a.room_type} (${a.bed_no})</b></div>\n" +
                    "            <div><span style=\"font-size:11px; color:#6B7280;\">Monthly Fee</span><br><b>Rs. ${Number(a.monthly_fee).toLocaleString()}</b></div>\n" +
                    "            <div><span style=\"font-size:11px; color:#6B7280;\">Allocated Since</span><br><b>${a.start_date}</b></div>\n" +
                    "          </div>\n" +
                    "          <button class=\"btn-action btn-red\" onclick=\"doVacateRoom(${a.id})\">Vacate / Checkout Room</button>\n" +
                    "        `;\n" +
                    "      } else {\n" +
                    "        curDiv.innerHTML = '<p style=\"color:#D97706;\">⚠️ You currently have no allocated room. Book an available room below.</p>';\n" +
                    "      }\n" +
                    "      \n" +
                    "      const tb = document.querySelector('#availRoomsTable tbody');\n" +
                    "      if (!data.rooms || data.rooms.length === 0) {\n" +
                    "        tb.innerHTML = '<tr><td colspan=\"5\" style=\"text-align:center;\">No rooms currently available.</td></tr>';\n" +
                    "      } else {\n" +
                    "        tb.innerHTML = data.rooms.map(r => `\n" +
                    "          <tr>\n" +
                    "            <td><b>Room ${r.room_no}</b></td>\n" +
                    "            <td>${r.room_type}</td>\n" +
                    "            <td>${r.floor_no || '1st'}</td>\n" +
                    "            <td>Rs. ${Number(r.monthly_fee).toLocaleString()}</td>\n" +
                    "            <td><button class=\"btn-action btn-purple\" onclick=\"doBookRoom(${r.id}, '${r.room_no}')\">Book Room</button></td>\n" +
                    "          </tr>\n" +
                    "        `).join('');\n" +
                    "      }\n" +
                    "    }\n" +
                    "\n" +
                    "    async function doBookRoom(roomId, roomNo) {\n" +
                    "      if (!confirm('Confirm booking Room ' + roomNo + '?')) return;\n" +
                    "      const res = await fetch('/api/book-room', {\n" +
                    "        method: 'POST',\n" +
                    "        headers: {'Content-Type': 'application/json'},\n" +
                    "        body: JSON.stringify({studentId: session.id, roomId: roomId, bedNo: 'Bed 1'})\n" +
                    "      });\n" +
                    "      const data = await res.json();\n" +
                    "      if (data.success) {\n" +
                    "        alert('🎉 Room ' + roomNo + ' booked successfully!');\n" +
                    "        loadRooms();\n" +
                    "      } else {\n" +
                    "        alert(data.message || 'Booking failed.');\n" +
                    "      }\n" +
                    "    }\n" +
                    "\n" +
                    "    async function doVacateRoom(allocId) {\n" +
                    "      if (!confirm('Are you sure you want to vacate this room?')) return;\n" +
                    "      const res = await fetch('/api/vacate-room', {\n" +
                    "        method: 'POST',\n" +
                    "        headers: {'Content-Type': 'application/json'},\n" +
                    "        body: JSON.stringify({allocId: allocId})\n" +
                    "      });\n" +
                    "      const data = await res.json();\n" +
                    "      if (data.success) {\n" +
                    "        alert('Room vacated.');\n" +
                    "        loadRooms();\n" +
                    "      }\n" +
                    "    }\n" +
                    "\n" +
                    "    async function loadComplaints() {\n" +
                    "      const res = await fetch('/api/complaints?studentId=' + session.id);\n" +
                    "      const list = await res.json();\n" +
                    "      const tb = document.querySelector('#allCompTable tbody');\n" +
                    "      if (!list || list.length === 0) {\n" +
                    "        tb.innerHTML = '<tr><td colspan=\"5\" style=\"text-align:center;\">No complaints submitted yet.</td></tr>';\n" +
                    "      } else {\n" +
                    "        tb.innerHTML = list.map(c => `\n" +
                    "          <tr>\n" +
                    "            <td><b>${c.category}</b></td>\n" +
                    "            <td>${c.subject}</td>\n" +
                    "            <td>${c.details || '-'}</td>\n" +
                    "            <td><span class=\"badge badge-${c.status.toLowerCase()}\">${c.status}</span></td>\n" +
                    "            <td>${c.date}</td>\n" +
                    "          </tr>\n" +
                    "        `).join('');\n" +
                    "      }\n" +
                    "    }\n" +
                    "\n" +
                    "    async function doSubmitComplaint() {\n" +
                    "      const cat = document.getElementById('cCat').value;\n" +
                    "      const sub = document.getElementById('cSub').value.trim();\n" +
                    "      const det = document.getElementById('cDet').value.trim();\n" +
                    "      if (!sub) return alert('Please enter subject.');\n" +
                    "      const res = await fetch('/api/complaints', {\n" +
                    "        method: 'POST',\n" +
                    "        headers: {'Content-Type': 'application/json'},\n" +
                    "        body: JSON.stringify({studentId: session.id, category: cat, subject: sub, details: det})\n" +
                    "      });\n" +
                    "      const data = await res.json();\n" +
                    "      if (data.success) {\n" +
                    "        alert('Complaint submitted successfully!');\n" +
                    "        hideModal('complaintModal');\n" +
                    "        document.getElementById('cSub').value = '';\n" +
                    "        document.getElementById('cDet').value = '';\n" +
                    "        loadComplaints();\n" +
                    "      }\n" +
                    "    }\n" +
                    "\n" +
                    "    async function loadPayments() {\n" +
                    "      const res = await fetch('/api/payments?studentId=' + session.id);\n" +
                    "      const list = await res.json();\n" +
                    "      const tb = document.querySelector('#paymentsTable tbody');\n" +
                    "      if (!list || list.length === 0) {\n" +
                    "        tb.innerHTML = '<tr><td colspan=\"5\" style=\"text-align:center;\">No payment transactions recorded yet.</td></tr>';\n" +
                    "      } else {\n" +
                    "        tb.innerHTML = list.map(p => `\n" +
                    "          <tr>\n" +
                    "            <td>${p.month || '-'}</td>\n" +
                    "            <td><b>Rs. ${Number(p.amount).toLocaleString()}</b></td>\n" +
                    "            <td>${p.method}</td>\n" +
                    "            <td>${p.reference || '-'}</td>\n" +
                    "            <td>${p.date}</td>\n" +
                    "          </tr>\n" +
                    "        `).join('');\n" +
                    "      }\n" +
                    "    }\n" +
                    "\n" +
                    "    async function doSubmitPayment() {\n" +
                    "      const amt = document.getElementById('payAmt').value;\n" +
                    "      const month = document.getElementById('payMonth').value;\n" +
                    "      const date = document.getElementById('payDate').value;\n" +
                    "      const method = document.getElementById('payMethod').value;\n" +
                    "      const ref = document.getElementById('payRef').value.trim();\n" +
                    "      const res = await fetch('/api/payments', {\n" +
                    "        method: 'POST',\n" +
                    "        headers: {'Content-Type': 'application/json'},\n" +
                    "        body: JSON.stringify({studentId: session.id, amount: amt, month: month, date: date, method: method, ref: ref})\n" +
                    "      });\n" +
                    "      const data = await res.json();\n" +
                    "      if (data.success) {\n" +
                    "        alert('Payment reported successfully!');\n" +
                    "        hideModal('paymentModal');\n" +
                    "        loadPayments();\n" +
                    "      }\n" +
                    "    }\n" +
                    "\n" +
                    "    function loadProfile() {\n" +
                    "      const pGrid = document.getElementById('profInfoGrid');\n" +
                    "      pGrid.innerHTML = `\n" +
                    "        <div><span style=\"color:#6B7280; font-size:11px;\">Full Name</span><br><b>${session.fullName}</b></div>\n" +
                    "        <div><span style=\"color:#6B7280; font-size:11px;\">Roll / Reg No</span><br><b>${session.studentNo}</b></div>\n" +
                    "        <div><span style=\"color:#6B7280; font-size:11px;\">Department</span><br><b>${session.department || 'AIOU'}</b></div>\n" +
                    "        <div><span style=\"color:#6B7280; font-size:11px;\">Gender</span><br><b>${session.gender || 'N/A'}</b></div>\n" +
                    "        <div><span style=\"color:#6B7280; font-size:11px;\">Phone</span><br><b>${session.phone || 'N/A'}</b></div>\n" +
                    "        <div><span style=\"color:#6B7280; font-size:11px;\">Email</span><br><b>${session.email || 'N/A'}</b></div>\n" +
                    "      `;\n" +
                    "    }\n" +
                    "\n" +
                    "    async function doChangePassword() {\n" +
                    "      const cur = document.getElementById('pCur').value;\n" +
                    "      const nw = document.getElementById('pNew').value;\n" +
                    "      const conf = document.getElementById('pConf').value;\n" +
                    "      if (!nw) return alert('Enter new password.');\n" +
                    "      if (nw !== conf) return alert('Passwords do not match.');\n" +
                    "      const res = await fetch('/api/change-password', {\n" +
                    "        method: 'POST',\n" +
                    "        headers: {'Content-Type': 'application/json'},\n" +
                    "        body: JSON.stringify({studentId: session.id, oldPass: cur, newPass: nw})\n" +
                    "      });\n" +
                    "      const data = await res.json();\n" +
                    "      if (data.success) {\n" +
                    "        alert('Password changed successfully!');\n" +
                    "        document.getElementById('pCur').value = '';\n" +
                    "        document.getElementById('pNew').value = '';\n" +
                    "        document.getElementById('pConf').value = '';\n" +
                    "      } else {\n" +
                    "        alert(data.message || 'Error changing password.');\n" +
                    "      }\n" +
                    "    }\n" +
                    "  </script>\n" +
                    "</body>\n" +
                    "</html>";

            sendHtmlResponse(ex, 200, html);
        }
    }

    // ==========================================
    // 5. REST APIS FOR MOBILE APP
    // ==========================================
    static class InfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            sendJsonResponse(ex, 200, "{\"status\": \"ok\", \"server\": \"" + serverUrl + "\"}");
        }
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJsonResponse(ex, 200, "{}"); return; }
            Map<String, String> f = parseForm(readBody(ex));
            String user = f.getOrDefault("roll", f.get("user"));
            String pass = f.getOrDefault("pass", f.get("password"));

            try {
                Auth.StudentSession sess = Auth.loginStudent(user, pass);
                if (sess != null) {
                    String json = String.format(
                            "{\"success\": true, \"session\": {\"id\": %d, \"studentNo\": \"%s\", \"fullName\": \"%s\", \"email\": \"%s\", \"phone\": \"%s\", \"department\": \"%s\", \"gender\": \"%s\"}}",
                            sess.id, sess.studentNo, sess.fullName, sess.email, sess.phone, sess.department, sess.gender
                    );
                    sendJsonResponse(ex, 200, json);
                } else {
                    sendJsonResponse(ex, 200, "{\"success\": false, \"message\": \"Invalid Roll No or Password. Default password is 123456.\"}");
                }
            } catch (Exception e) {
                sendJsonResponse(ex, 500, "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJsonResponse(ex, 200, "{}"); return; }
            Map<String, String> f = parseForm(readBody(ex));
            try {
                Auth.registerStudent(
                        f.get("studentNo"),
                        f.get("fullName"),
                        f.get("email"),
                        f.get("phone"),
                        f.get("department"),
                        f.get("gender"),
                        f.get("password")
                );
                sendJsonResponse(ex, 200, "{\"success\": true}");
            } catch (Exception e) {
                sendJsonResponse(ex, 200, "{\"success\": false, \"message\": \"" + e.getMessage().replace("\"", "'") + "\"}");
            }
        }
    }

    static class OverviewHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            int sid = Integer.parseInt(getQueryParams(ex).getOrDefault("studentId", "0"));
            String room = "None";
            int openComp = 0;
            double paid = 0.0;
            List<String> compJson = new ArrayList<>();

            try (Connection c = Database.getConnection()) {
                // Room
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT r.room_no, a.bed_no FROM allocations a JOIN rooms r ON a.room_id = r.id WHERE a.student_id = ? AND a.status = 'Active' LIMIT 1")) {
                    p.setInt(1, sid);
                    try (ResultSet rs = p.executeQuery()) {
                        if (rs.next()) room = "Room " + rs.getString(1) + (rs.getString(2) != null ? " (" + rs.getString(2) + ")" : "");
                    }
                }
                // Complaints count
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT COUNT(*), COALESCE(SUM(CASE WHEN status='Open' THEN 1 ELSE 0 END), 0) FROM complaints WHERE student_id = ?")) {
                    p.setInt(1, sid);
                    try (ResultSet rs = p.executeQuery()) {
                        if (rs.next()) openComp = rs.getInt(2);
                    }
                }
                // Paid sum
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE student_id = ?")) {
                    p.setInt(1, sid);
                    try (ResultSet rs = p.executeQuery()) {
                        if (rs.next()) paid = rs.getDouble(1);
                    }
                }
                // Recent complaints
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT category, subject, status, DATE_FORMAT(created_at, '%Y-%m-%d') FROM complaints WHERE student_id = ? ORDER BY created_at DESC LIMIT 5")) {
                    p.setInt(1, sid);
                    try (ResultSet rs = p.executeQuery()) {
                        while (rs.next()) {
                            compJson.add(String.format("{\"category\": \"%s\", \"subject\": \"%s\", \"status\": \"%s\", \"date\": \"%s\"}",
                                    rs.getString(1), rs.getString(2).replace("\"", "'"), rs.getString(3), rs.getString(4)));
                        }
                    }
                }
            } catch (Exception ignored) {}

            String json = String.format("{\"room\": \"%s\", \"openComplaints\": %d, \"totalPaid\": %.2f, \"recentComplaints\": [%s]}",
                    room, openComp, paid, String.join(",", compJson));
            sendJsonResponse(ex, 200, json);
        }
    }

    static class RoomsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            int sid = Integer.parseInt(getQueryParams(ex).getOrDefault("studentId", "0"));
            String curAlloc = "null";
            List<String> rooms = new ArrayList<>();

            try (Connection c = Database.getConnection()) {
                // Active allocation
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT a.id, r.room_no, r.room_type, a.bed_no, r.monthly_fee, a.start_date FROM allocations a JOIN rooms r ON a.room_id = r.id WHERE a.student_id = ? AND a.status = 'Active' LIMIT 1")) {
                    p.setInt(1, sid);
                    try (ResultSet rs = p.executeQuery()) {
                        if (rs.next()) {
                            curAlloc = String.format("{\"id\": %d, \"room_no\": \"%s\", \"room_type\": \"%s\", \"bed_no\": \"%s\", \"monthly_fee\": %.2f, \"start_date\": \"%s\"}",
                                    rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getDouble(5), rs.getString(6));
                        }
                    }
                }
                // Available rooms
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT id, room_no, room_type, capacity, monthly_fee, floor_no FROM rooms WHERE status = 'Available' ORDER BY room_no ASC")) {
                    while (rs.next()) {
                        rooms.add(String.format("{\"id\": %d, \"room_no\": \"%s\", \"room_type\": \"%s\", \"capacity\": %d, \"monthly_fee\": %.2f, \"floor_no\": \"%s\"}",
                                rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getDouble(5), rs.getString(6) != null ? rs.getString(6) : "1st"));
                    }
                }
            } catch (Exception ignored) {}

            String json = String.format("{\"currentAllocation\": %s, \"rooms\": [%s]}", curAlloc, String.join(",", rooms));
            sendJsonResponse(ex, 200, json);
        }
    }

    static class BookRoomHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJsonResponse(ex, 200, "{}"); return; }
            Map<String, String> f = parseForm(readBody(ex));
            int sid = Integer.parseInt(f.getOrDefault("studentId", "0"));
            int rid = Integer.parseInt(f.getOrDefault("roomId", "0"));
            String bed = f.getOrDefault("bedNo", "Bed 1");

            try (Connection c = Database.getConnection()) {
                // Deactivate previous allocation if any
                try (PreparedStatement up = c.prepareStatement("UPDATE allocations SET status = 'Completed', end_date = CURRENT_DATE WHERE student_id = ? AND status = 'Active'")) {
                    up.setInt(1, sid);
                    up.executeUpdate();
                }
                // Insert new allocation
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO allocations (student_id, room_id, bed_no, start_date, status) VALUES (?, ?, ?, CURRENT_DATE, 'Active')")) {
                    ins.setInt(1, sid);
                    ins.setInt(2, rid);
                    ins.setString(3, bed);
                    ins.executeUpdate();
                }
                sendJsonResponse(ex, 200, "{\"success\": true}");
            } catch (Exception e) {
                sendJsonResponse(ex, 500, "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    static class VacateRoomHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJsonResponse(ex, 200, "{}"); return; }
            Map<String, String> f = parseForm(readBody(ex));
            int aid = Integer.parseInt(f.getOrDefault("allocId", "0"));
            try (Connection c = Database.getConnection();
                 PreparedStatement up = c.prepareStatement("UPDATE allocations SET status = 'Completed', end_date = CURRENT_DATE WHERE id = ?")) {
                up.setInt(1, aid);
                up.executeUpdate();
                sendJsonResponse(ex, 200, "{\"success\": true}");
            } catch (Exception e) {
                sendJsonResponse(ex, 500, "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    static class ComplaintsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                Map<String, String> f = parseForm(readBody(ex));
                int sid = Integer.parseInt(f.getOrDefault("studentId", "0"));
                String cat = f.getOrDefault("category", "General");
                String sub = f.getOrDefault("subject", "");
                String det = f.getOrDefault("details", "");

                try (Connection c = Database.getConnection();
                     PreparedStatement ins = c.prepareStatement(
                             "INSERT INTO complaints (student_id, category, subject, details, status, created_at) VALUES (?, ?, ?, ?, 'Open', NOW())")) {
                    ins.setInt(1, sid);
                    ins.setString(2, cat);
                    ins.setString(3, sub);
                    ins.setString(4, det);
                    ins.executeUpdate();
                    sendJsonResponse(ex, 200, "{\"success\": true}");
                } catch (Exception e) {
                    sendJsonResponse(ex, 500, "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
                }
            } else {
                int sid = Integer.parseInt(getQueryParams(ex).getOrDefault("studentId", "0"));
                List<String> list = new ArrayList<>();
                try (Connection c = Database.getConnection();
                     PreparedStatement p = c.prepareStatement(
                             "SELECT id, category, subject, details, status, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i') FROM complaints WHERE student_id = ? ORDER BY created_at DESC")) {
                    p.setInt(1, sid);
                    try (ResultSet rs = p.executeQuery()) {
                        while (rs.next()) {
                            list.add(String.format("{\"id\": %d, \"category\": \"%s\", \"subject\": \"%s\", \"details\": \"%s\", \"status\": \"%s\", \"date\": \"%s\"}",
                                    rs.getInt(1), rs.getString(2), rs.getString(3).replace("\"", "'"),
                                    (rs.getString(4) != null ? rs.getString(4).replace("\"", "'") : ""),
                                    rs.getString(5), rs.getString(6)));
                        }
                    }
                } catch (Exception ignored) {}
                sendJsonResponse(ex, 200, "[" + String.join(",", list) + "]");
            }
        }
    }

    static class PaymentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                Map<String, String> f = parseForm(readBody(ex));
                int sid = Integer.parseInt(f.getOrDefault("studentId", "0"));
                double amt = Double.parseDouble(f.getOrDefault("amount", "0"));
                String month = f.getOrDefault("month", "");
                String date = f.getOrDefault("date", LocalDate.now().toString());
                String method = f.getOrDefault("method", "Online");
                String ref = f.getOrDefault("ref", "");

                try (Connection c = Database.getConnection();
                     PreparedStatement ins = c.prepareStatement(
                             "INSERT INTO payments (student_id, amount, payment_date, month_for, method, reference_no, notes) VALUES (?, ?, ?, ?, ?, ?, 'Submitted via Mobile Portal')")) {
                    ins.setInt(1, sid);
                    ins.setDouble(2, amt);
                    ins.setString(3, date);
                    ins.setString(4, month);
                    ins.setString(5, method);
                    ins.setString(6, ref);
                    ins.executeUpdate();
                    sendJsonResponse(ex, 200, "{\"success\": true}");
                } catch (Exception e) {
                    sendJsonResponse(ex, 500, "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
                }
            } else {
                int sid = Integer.parseInt(getQueryParams(ex).getOrDefault("studentId", "0"));
                List<String> list = new ArrayList<>();
                try (Connection c = Database.getConnection();
                     PreparedStatement p = c.prepareStatement(
                             "SELECT id, month_for, amount, payment_date, method, reference_no FROM payments WHERE student_id = ? ORDER BY payment_date DESC")) {
                    p.setInt(1, sid);
                    try (ResultSet rs = p.executeQuery()) {
                        while (rs.next()) {
                            list.add(String.format("{\"id\": %d, \"month\": \"%s\", \"amount\": %.2f, \"date\": \"%s\", \"method\": \"%s\", \"reference\": \"%s\"}",
                                    rs.getInt(1), (rs.getString(2) != null ? rs.getString(2) : "-"), rs.getDouble(3),
                                    rs.getString(4), rs.getString(5), (rs.getString(6) != null ? rs.getString(6) : "-")));
                        }
                    }
                } catch (Exception ignored) {}
                sendJsonResponse(ex, 200, "[" + String.join(",", list) + "]");
            }
        }
    }

    static class ChangePasswordHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJsonResponse(ex, 200, "{}"); return; }
            Map<String, String> f = parseForm(readBody(ex));
            int sid = Integer.parseInt(f.getOrDefault("studentId", "0"));
            String oldPass = f.getOrDefault("oldPass", "");
            String newPass = f.getOrDefault("newPass", "");

            try {
                if (Auth.changeStudentPassword(sid, oldPass, newPass)) {
                    sendJsonResponse(ex, 200, "{\"success\": true}");
                } else {
                    sendJsonResponse(ex, 200, "{\"success\": false, \"message\": \"Current password is incorrect.\"}");
                }
            } catch (Exception e) {
                sendJsonResponse(ex, 500, "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }
}
