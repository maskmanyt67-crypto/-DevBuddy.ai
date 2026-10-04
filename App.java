import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * DevBuddy AI - Hacktoberfest 2026: Build for a Friend
 * Zero-dependency Java HTTP Server connecting modern web UI with Open-Source AI.
 * Tailored for helping a developer friend debug, explain stack traces, and review code.
 */
public class App {

    private static final int PORT = 8080;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/", new StaticFileHandler("index.html", "text/html; charset=UTF-8"));
        server.createContext("/style.css", new StaticFileHandler("style.css", "text/css; charset=UTF-8"));
        server.createContext("/api/analyze", new AnalyzeHandler());

        System.out.println("===============================================================");
        System.out.println(" 💻 DevBuddy AI - Built for your Developer Friend!");
        System.out.println(" 🚀 Open your browser at: http://localhost:" + PORT);
        System.out.println(" 🤖 Open-Source AI Backend: Ollama (http://localhost:11434)");
        System.out.println("===============================================================");

        server.setExecutor(null);
        server.start();
    }

    static class StaticFileHandler implements HttpHandler {
        private final String filename;
        private final String contentType;

        public StaticFileHandler(String filename, String contentType) {
            this.filename = filename;
            this.contentType = contentType;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Path filePath = Paths.get(filename);
            if (!Files.exists(filePath)) {
                filePath = Paths.get(".", filename);
            }

            if (Files.exists(filePath)) {
                byte[] bytes = Files.readAllBytes(filePath);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                String notFound = "404 Not Found: " + filename;
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    static class AnalyzeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\": \"Method not allowed. Use POST.\"}");
                return;
            }

            String requestBody;
            try (InputStream is = exchange.getRequestBody()) {
                requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            String friendName = extractJsonField(requestBody, "friendName");
            String language = extractJsonField(requestBody, "language");
            String mode = extractJsonField(requestBody, "mode");
            String codeSnippet = extractJsonField(requestBody, "codeSnippet");
            String errorMessage = extractJsonField(requestBody, "errorMessage");
            String model = extractJsonField(requestBody, "model");
            String provider = extractJsonField(requestBody, "provider");
            String apiKey = extractJsonField(requestBody, "apiKey");

            if (friendName.isEmpty()) friendName = "Friend";
            if (language.isEmpty()) language = "General";
            if (mode.isEmpty()) mode = "Supportive Senior Dev";
            if (model.isEmpty()) model = "qwen2.5-coder";

            String systemPrompt = "You are DevBuddy, a compassionate, brilliant open-source AI pair programmer built specifically to help a developer friend named " + friendName + ".\n"
                    + "Your Persona Mode: " + mode + ".\n"
                    + "Language / Stack: " + language + ".\n\n"
                    + "Provide a friendly, highly practical markdown response with:\n"
                    + "1. A warm opening addressing " + friendName + " directly in your chosen persona tone.\n"
                    + "2. **Root Cause Analysis**: Explain why this bug or error happened in plain English without corporate jargon.\n"
                    + "3. **The Fix**: The corrected, clean code snippet with inline explanations.\n"
                    + "4. **Senior Dev Pro-Tip**: One preventive habit or debugging shortcut for the future.\n"
                    + "5. **Friendship Debug Coupon**: A humorous redeemable voucher (e.g., '1 Free Emergency Discord Screen Share', 'Pizza & Semicolon Hunt').";

            String userContent = "Developer Friend: " + friendName + "\n"
                    + "Language: " + language + "\n"
                    + "Error / Stack Trace:\n" + errorMessage + "\n\n"
                    + "Code Snippet:\n" + codeSnippet + "\n\n"
                    + "Please analyze, debug, and unblock " + friendName + ":";

            String resultText = "";
            try {
                if ("huggingface".equalsIgnoreCase(provider) && !apiKey.isEmpty()) {
                    resultText = callHuggingFace(model, systemPrompt, userContent, apiKey);
                } else if ("ollama".equalsIgnoreCase(provider) || provider.isEmpty()) {
                    resultText = callOllama(model, systemPrompt, userContent);
                } else {
                    resultText = getFallbackAnalysis(friendName, language, mode, errorMessage, codeSnippet);
                }
            } catch (Exception ex) {
                System.err.println("AI request failed (" + ex.getMessage() + "). Serving smart fallback analyzer.");
                resultText = getFallbackAnalysis(friendName, language, mode, errorMessage, codeSnippet)
                        + "\n\n*(Note: Generated via DevBuddy smart offline analyzer. Start Ollama with `" + model + "` for real-time local open model inference!)*";
            }

            String jsonOutput = "{\"status\": \"success\", \"reply\": " + escapeJson(resultText) + "}";
            sendJsonResponse(exchange, 200, jsonOutput);
        }

        private String callOllama(String model, String systemPrompt, String userContent) throws Exception {
            String combined = systemPrompt + "\n\n" + userContent;
            String escaped = escapeJson(combined);
            String payload = "{\"model\": \"" + model + "\", \"prompt\": " + escaped + ", \"stream\": false}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return extractJsonField(response.body(), "response");
            } else {
                throw new RuntimeException("Ollama HTTP " + response.statusCode() + ": " + response.body());
            }
        }

        private String callHuggingFace(String model, String systemPrompt, String userContent, String apiKey) throws Exception {
            String hfModel = model.contains("/") ? model : "Qwen/Qwen2.5-Coder-7B-Instruct";
            String promptText = "<|im_start|>system\n" + systemPrompt + "<|im_end|>\n<|im_start|>user\n" + userContent + "<|im_end|>\n<|im_start|>assistant\n";
            String payload = "{\"inputs\": " + escapeJson(promptText) + ", \"parameters\": {\"max_new_tokens\": 750, \"return_full_text\": false}}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api-inference.huggingface.co/models/" + hfModel))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(45))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                int genTextIndex = body.indexOf("\"generated_text\":");
                if (genTextIndex != -1) {
                    return extractJsonField(body, "generated_text");
                }
                return body;
            } else {
                throw new RuntimeException("HuggingFace HTTP " + response.statusCode() + ": " + response.body());
            }
        }

        private String getFallbackAnalysis(String name, String lang, String mode, String error, String code) {
            boolean hasNull = (error + code).toLowerCase().contains("null") || (error + code).toLowerCase().contains("undefined");
            boolean hasCors = (error + code).toLowerCase().contains("cors") || (error + code).toLowerCase().contains("access-control");

            StringBuilder sb = new StringBuilder();
            sb.append("### ☕ Hey ").append(name).append(", Take a Breath!\n\n");
            sb.append("**Mode:** *").append(mode).append("* | **Stack:** *").append(lang).append("*\n\n");

            if (hasCors) {
                sb.append("### 🔍 Root Cause Analysis\n");
                sb.append("Classic CORS blocker! Browsers enforce the Same-Origin Policy when the client origin doesn't match the server response headers. The server is rejecting or missing the `Access-Control-Allow-Origin` header.\n\n");
                sb.append("### 🛠️ The Fix\n");
                sb.append("Ensure your backend includes proper CORS pre-flight handlers and response headers:\n\n");
                sb.append("```").append(lang.toLowerCase()).append("\n");
                sb.append("// Add CORS headers to response\n");
                sb.append("response.setHeader(\"Access-Control-Allow-Origin\", \"*\");\n");
                sb.append("response.setHeader(\"Access-Control-Allow-Methods\", \"GET, POST, PUT, DELETE, OPTIONS\");\n");
                sb.append("response.setHeader(\"Access-Control-Allow-Headers\", \"Content-Type, Authorization\");\n");
                sb.append("```\n\n");
            } else if (hasNull) {
                sb.append("### 🔍 Root Cause Analysis\n");
                sb.append("You hit the infamous 'Billion Dollar Mistake'—dereferencing a null/undefined reference. An object, variable, or API response was expected to exist, but came back empty before you accessed its properties.\n\n");
                sb.append("### 🛠️ The Fix\n");
                sb.append("Introduce optional chaining or defensive null checks before dereferencing:\n\n");
                sb.append("```").append(lang.toLowerCase()).append("\n");
                sb.append("// Defensive guard clause\n");
                sb.append("if (targetObject != null && targetObject.isValid()) {\n");
                sb.append("    // Safe to execute logic\n");
                sb.append("    targetObject.process();\n");
                sb.append("} else {\n");
                sb.append("    System.out.println(\"Safely handled null fallback!\");\n");
                sb.append("}\n");
                sb.append("```\n\n");
            } else {
                sb.append("### 🔍 Root Cause Analysis\n");
                sb.append("Looking at this snippet, the execution flow is stumbling either on unhandled state or a boundary condition in `").append(lang).append("`.\n\n");
                sb.append("### 🛠️ The Fix\n");
                sb.append("Double check variable scoping, make sure async calls are properly awaited, and isolate the exact line throwing the exception using a breakpoint or focused print statement.\n\n");
            }

            sb.append("### 💡 Senior Dev Pro-Tip for ").append(name).append("\n");
            sb.append("When a bug takes longer than 20 minutes: stand up, refill your water, and explain the code out loud line-by-line to your rubber duck (or ping me!). 80% of bugs reveal themselves the moment you speak them out loud.\n\n");
            sb.append("### 🎟️ Friendship Debug Voucher\n");
            sb.append("> **COUPON CODE:** DEV-PAIR-ASSIST-2026\n");
            sb.append("> **Entitles ").append(name).append(" to:** 1x Free 30-minute Screen Share Pair-Programming Session & Boba/Coffee!");

            return sb.toString();
        }

        private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String extractJsonField(String json, String key) {
            String target = "\"" + key + "\":";
            int idx = json.indexOf(target);
            if (idx == -1) return "";
            idx += target.length();

            while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;
            if (idx >= json.length()) return "";

            if (json.charAt(idx) == '"') {
                idx++;
                StringBuilder sb = new StringBuilder();
                boolean escaped = false;
                while (idx < json.length()) {
                    char c = json.charAt(idx);
                    if (escaped) {
                        if (c == 'n') sb.append('\n');
                        else if (c == 'r') sb.append('\r');
                        else if (c == 't') sb.append('\t');
                        else sb.append(c);
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == '"') {
                        break;
                    } else {
                        sb.append(c);
                    }
                    idx++;
                }
                return sb.toString();
            }
            return "";
        }

        private String escapeJson(String raw) {
            if (raw == null) return "\"\"";
            StringBuilder sb = new StringBuilder("\"");
            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                switch (c) {
                    case '"': sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '\b': sb.append("\\b"); break;
                    case '\f': sb.append("\\f"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    default:
                        if (c < ' ') {
                            String t = "000" + Integer.toHexString(c);
                            sb.append("\\u").append(t.substring(t.length() - 4));
                        } else {
                            sb.append(c);
                        }
                }
            }
            sb.append("\"");
            return sb.toString();
        }
    }
}
