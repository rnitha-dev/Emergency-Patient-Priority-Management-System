import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * EmergencyWebServer is a standalone HTTP web server built using Java standard library
 * (com.sun.net.httpserver.HttpServer).
 * 
 * Provides:
 * 1. Static web page serving for the Hospital Triage Web Dashboard (web/index.html)
 * 2. REST API endpoints for all patient operations
 * 3. Thread-safe execution bridging HTTP requests to EmergencyManagementSystem
 * 4. Automatic Google Chrome browser launching at http://localhost:8080
 */
public class EmergencyWebServer {

    private static final int PORT = 8080;
    private final HttpServer server;
    private final EmergencyManagementSystem system;

    public EmergencyWebServer(EmergencyManagementSystem system) throws IOException {
        this.system = system;
        this.server = HttpServer.create(new InetSocketAddress(PORT), 0);
        this.server.setExecutor(Executors.newCachedThreadPool());
        setupRoutes();
    }

    private void setupRoutes() {
        // Static UI Route
        server.createContext("/", new StaticFileHandler());

        // REST API Routes
        server.createContext("/api/conditions", new ConditionsHandler());
        server.createContext("/api/patients/waiting", new WaitingPatientsHandler());
        server.createContext("/api/patients/register", new RegisterPatientHandler());
        server.createContext("/api/patients/search", new SearchPatientHandler());
        server.createContext("/api/patients/condition", new UpdateConditionHandler());
        server.createContext("/api/patients/priority", new UpdatePriorityHandler());
        server.createContext("/api/patients/treat", new TreatNextHandler());
        server.createContext("/api/patients/remove", new RemovePatientHandler());
        server.createContext("/api/history", new HistoryHandler());
        server.createContext("/api/demo/concurrent-registration", new ConcurrentRegistrationDemoHandler());
        server.createContext("/api/demo/concurrent-update", new ConcurrentUpdateDemoHandler());
    }

    public void start() {
        server.start();
        System.out.println("=========================================================");
        System.out.printf("  EMERGENCY WEB SERVER RUNNING ON: http://localhost:%d%n", PORT);
        System.out.println("  Connected to EmergencyManagementSystem backend.");
        System.out.println("=========================================================");
    }

    public void stop() {
        server.stop(0);
        System.out.println("Emergency Web Server stopped.");
    }

    public static void openChrome() {
        String url = "http://localhost:" + PORT;
        try {
            // Attempt to launch Chrome specifically first
            new ProcessBuilder("cmd", "/c", "start", "chrome", url).start();
            System.out.println("[Browser] Opened Google Chrome at " + url);
        } catch (Exception e1) {
            try {
                // Fallback to default browser
                new ProcessBuilder("cmd", "/c", "start", url).start();
                System.out.println("[Browser] Opened default browser at " + url);
            } catch (Exception e2) {
                System.err.println("[Browser] Could not auto-launch browser. Please visit " + url + " manually.");
            }
        }
    }

    // =========================================================================
    // HTTP HANDLERS
    // =========================================================================

    /**
     * Serves web/index.html dashboard
     */
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.equals("/index.html")) {
                File file = new File("web/index.html");
                if (file.exists()) {
                    byte[] bytes = readAllBytes(file);
                    sendResponse(exchange, 200, "text/html; charset=UTF-8", bytes);
                    return;
                }
            }
            sendJsonResponse(exchange, 404, "{\"error\":\"File not found\"}");
        }
    }

    /**
     * GET /api/conditions
     */
    private class ConditionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            List<String> conditions = system.getPriorityCalculator().getSupportedConditions();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < conditions.size(); i++) {
                String cond = conditions.get(i);
                try {
                    int p = system.getPriorityCalculator().calculatePriority(cond);
                    String level = system.getPriorityCalculator().getPriorityLevelName(p);
                    if (i > 0) sb.append(",");
                    sb.append(String.format("{\"condition\":\"%s\",\"priority\":%d,\"level\":\"%s\"}",
                            escapeJson(cond), p, escapeJson(level)));
                } catch (Exception ignored) {}
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    /**
     * GET /api/patients/waiting
     */
    private class WaitingPatientsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            List<EmergencyPatient> queue = system.getWaitingPatients();
            sendJsonResponse(exchange, 200, patientsToJsonArray(queue));
        }
    }

    /**
     * POST /api/patients/register
     */
    private class RegisterPatientHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readBody(exchange);
            try {
                int id = extractInt(body, "patientId");
                String name = extractString(body, "name");
                int age = extractInt(body, "age");
                String gender = extractString(body, "gender");
                String phone = extractString(body, "phoneNumber");
                String condition = extractString(body, "medicalCondition");
                String notes = extractString(body, "triageNotes");
                boolean isTrauma = extractBoolean(body, "isTraumaCase");

                int priority = system.getPriorityCalculator().calculatePriority(condition);
                EmergencyPatient p = new EmergencyPatient(id, name, age, gender, phone, condition, priority, notes, isTrauma);

                system.registerPatient(p);
                sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Patient registered successfully\",\"patient\":" + patientToJson(p) + "}");

            } catch (InvalidPatientException e) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid payload: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * GET /api/patients/search?id=101
     */
    private class SearchPatientHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            int id = -1;
            if (query != null && query.contains("id=")) {
                try {
                    String[] parts = query.split("id=");
                    if (parts.length > 1) {
                        id = Integer.parseInt(parts[1].split("&")[0].trim());
                    }
                } catch (Exception ignored) {}
            }

            if (id <= 0) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Valid positive patient ID required as query parameter (e.g. ?id=101)\"}");
                return;
            }

            try {
                EmergencyPatient p = system.searchPatient(id);
                sendJsonResponse(exchange, 200, "{\"success\":true,\"patient\":" + patientToJson(p) + "}");
            } catch (InvalidPatientException e) {
                sendJsonResponse(exchange, 404, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * POST /api/patients/condition
     */
    private class UpdateConditionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readBody(exchange);
            try {
                int id = extractInt(body, "patientId");
                String newCondition = extractString(body, "newCondition");

                system.updateMedicalCondition(id, newCondition);
                EmergencyPatient updated = system.searchPatient(id);
                sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Condition and priority updated successfully\",\"patient\":" + patientToJson(updated) + "}");

            } catch (InvalidPatientException e) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid payload: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * POST /api/patients/priority
     */
    private class UpdatePriorityHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readBody(exchange);
            try {
                int id = extractInt(body, "patientId");
                int newPriority = extractInt(body, "newPriority");

                system.updatePriority(id, newPriority);
                EmergencyPatient updated = system.searchPatient(id);
                sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Priority manually updated\",\"patient\":" + patientToJson(updated) + "}");

            } catch (InvalidPatientException e) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid payload: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * POST /api/patients/treat
     */
    private class TreatNextHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                EmergencyPatient treated = system.treatNextPatient();
                sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Patient treated successfully\",\"patient\":" + patientToJson(treated) + "}");
            } catch (InvalidPatientException e) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * POST /api/patients/remove
     */
    private class RemovePatientHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readBody(exchange);
            try {
                int id = extractInt(body, "patientId");
                EmergencyPatient removed = system.removePatient(id);
                sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Patient removed from waiting queue\",\"patient\":" + patientToJson(removed) + "}");
            } catch (InvalidPatientException e) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Invalid payload: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * GET /api/history
     */
    private class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            List<EmergencyPatient> history = system.getTreatmentHistory();
            sendJsonResponse(exchange, 200, patientsToJsonArray(history));
        }
    }

    /**
     * POST /api/demo/concurrent-registration
     */
    private class ConcurrentRegistrationDemoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            int idBase = 800 + (int)(Math.random() * 100);
            List<String> logs = new ArrayList<>();

            try {
                EmergencyPatient p1 = new EmergencyPatient(idBase + 1, "Rohan Verma", 48, "Male", "9870001", "Cardiac Arrest", 1, "Desk A - Critical Intake", true);
                EmergencyPatient p2 = new EmergencyPatient(idBase + 2, "Anita Roy", 34, "Female", "9870002", "Severe Bleeding", 2, "Desk B - Trauma Intake", true);
                EmergencyPatient p3 = new EmergencyPatient(idBase + 3, "Suresh Pillai", 62, "Male", "9870003", "Fracture", 3, "Desk C - Urgent Intake", false);

                PatientRegistrationThread t1 = new PatientRegistrationThread("RegistrationThread-1", system, p1);
                PatientRegistrationThread t2 = new PatientRegistrationThread("RegistrationThread-2", system, p2);
                PatientRegistrationThread t3 = new PatientRegistrationThread("RegistrationThread-3", system, p3);

                t1.start();
                t2.start();
                t3.start();

                t1.join();
                t2.join();
                t3.join();

                logs.add(String.format("[RegistrationThread-1] Patient %d (%s) registered - Priority: %d (%s)", p1.getPatientId(), p1.getName(), p1.getPriority(), p1.getPriorityLevelName()));
                logs.add(String.format("[RegistrationThread-2] Patient %d (%s) registered - Priority: %d (%s)", p2.getPatientId(), p2.getName(), p2.getPriority(), p2.getPriorityLevelName()));
                logs.add(String.format("[RegistrationThread-3] Patient %d (%s) registered - Priority: %d (%s)", p3.getPatientId(), p3.getName(), p3.getPriority(), p3.getPriorityLevelName()));

                StringBuilder jsonLogs = new StringBuilder("[");
                for (int i = 0; i < logs.size(); i++) {
                    if (i > 0) jsonLogs.append(",");
                    jsonLogs.append("\"").append(escapeJson(logs.get(i))).append("\"");
                }
                jsonLogs.append("]");

                sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"3 Registration threads completed safely concurrently\",\"logs\":" + jsonLogs.toString() + ",\"waitingCount\":" + system.getWaitingCount() + "}");

            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * POST /api/demo/concurrent-update
     */
    private class ConcurrentUpdateDemoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            try {
                // Ensure at least 3 waiting patients exist
                List<EmergencyPatient> queue = system.getWaitingPatients();
                if (queue.size() < 2) {
                    int b = 900 + (int)(Math.random() * 50);
                    system.registerPatient(new EmergencyPatient(b + 1, "Demo Patient X", 30, "Male", "999111", "Minor Injury", 5, "Demo", false));
                    system.registerPatient(new EmergencyPatient(b + 2, "Demo Patient Y", 45, "Female", "999222", "High Fever", 4, "Demo", false));
                    queue = system.getWaitingPatients();
                }

                int id1 = queue.get(0).getPatientId();
                int id2 = queue.get(1).getPatientId();

                PriorityUpdateThread u1 = new PriorityUpdateThread("UpdateThread-1", system, id1, "Cardiac Arrest");
                PriorityUpdateThread u2 = new PriorityUpdateThread("UpdateThread-2", system, id2, 1);

                u1.start();
                u2.start();

                u1.join();
                u2.join();

                List<String> logs = new ArrayList<>();
                logs.add(String.format("[UpdateThread-1] Patient %d condition escalated to 'Cardiac Arrest' (Priority 1 - CRITICAL)", id1));
                logs.add(String.format("[UpdateThread-2] Patient %d priority manually escalated to Priority 1 (CRITICAL)", id2));

                StringBuilder jsonLogs = new StringBuilder("[");
                for (int i = 0; i < logs.size(); i++) {
                    if (i > 0) jsonLogs.append(",");
                    jsonLogs.append("\"").append(escapeJson(logs.get(i))).append("\"");
                }
                jsonLogs.append("]");

                sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"2 Priority Update threads executed concurrently\",\"logs\":" + jsonLogs.toString() + "}");

            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =========================================================================
    // JSON UTILITIES (Standard Java - Zero External Dependencies)
    // =========================================================================

    public static String patientToJson(EmergencyPatient p) {
        if (p == null) return "null";
        return String.format(
                "{\"patientId\":%d,\"name\":\"%s\",\"age\":%d,\"gender\":\"%s\",\"phoneNumber\":\"%s\",\"medicalCondition\":\"%s\",\"priority\":%d,\"priorityLevelName\":\"%s\",\"registrationTime\":%d,\"formattedRegistrationTime\":\"%s\",\"triageNotes\":\"%s\",\"isTraumaCase\":%b,\"treatmentTime\":\"%s\"}",
                p.getPatientId(),
                escapeJson(p.getName()),
                p.getAge(),
                escapeJson(p.getGender()),
                escapeJson(p.getPhoneNumber()),
                escapeJson(p.getMedicalCondition()),
                p.getPriority(),
                escapeJson(p.getPriorityLevelName()),
                p.getRegistrationTime(),
                escapeJson(p.getFormattedRegistrationTime()),
                escapeJson(p.getTriageNotes()),
                p.isTraumaCase(),
                escapeJson(p.getTreatmentTime())
        );
    }

    public static String patientsToJsonArray(List<EmergencyPatient> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(patientToJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String extractString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyPos = json.indexOf(searchKey);
        if (keyPos == -1) return "";
        int colonPos = json.indexOf(":", keyPos + searchKey.length());
        if (colonPos == -1) return "";
        int quoteStart = json.indexOf("\"", colonPos + 1);
        if (quoteStart == -1) return "";
        int quoteEnd = json.indexOf("\"", quoteStart + 1);
        if (quoteEnd == -1) return "";
        return json.substring(quoteStart + 1, quoteEnd).trim();
    }

    private static int extractInt(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyPos = json.indexOf(searchKey);
        if (keyPos == -1) return 0;
        int colonPos = json.indexOf(":", keyPos + searchKey.length());
        if (colonPos == -1) return 0;
        int start = colonPos + 1;
        while (start < json.length() && (Character.isWhitespace(json.charAt(start)) || json.charAt(start) == '"')) {
            start++;
        }
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static boolean extractBoolean(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyPos = json.indexOf(searchKey);
        if (keyPos == -1) return false;
        int colonPos = json.indexOf(":", keyPos + searchKey.length());
        if (colonPos == -1) return false;
        String sub = json.substring(colonPos + 1, Math.min(json.length(), colonPos + 20)).toLowerCase();
        return sub.contains("true");
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static byte[] readAllBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] b = new byte[(int) file.length()];
            fis.read(b);
            return b;
        }
    }

    public static void main(String[] args) throws IOException {
        EmergencyManagementSystem ems = new EmergencyManagementSystem();
        ems.loadTreatmentHistoryFromFile();

        // Seed with sample initial emergency triage patients if queue is empty
        if (ems.getWaitingCount() == 0) {
            try {
                ems.registerPatient(new EmergencyPatient(101, "Arun Sharma", 45, "Male", "9876543210", "Cardiac Arrest", 1, "Arrived via ICU Ambulance", true));
                ems.registerPatient(new EmergencyPatient(102, "Sunita Rao", 32, "Female", "9876543211", "Severe Bleeding", 2, "Laceration in emergency trauma bay", true));
                ems.registerPatient(new EmergencyPatient(103, "David Wilson", 28, "Male", "9876543212", "Fracture", 3, "Compound fracture right arm", false));
                ems.registerPatient(new EmergencyPatient(104, "Priya Patel", 55, "Female", "9876543213", "High Fever", 4, "Temp 103.5 F with chills", false));
                ems.registerPatient(new EmergencyPatient(105, "Rahul Verma", 21, "Male", "9876543214", "Minor Injury", 5, "Superficial finger abrasion", false));
            } catch (InvalidPatientException ignored) {}
        }

        EmergencyWebServer webServer = new EmergencyWebServer(ems);
        webServer.start();
        openChrome();
    }
}