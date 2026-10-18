package com.bai.inventory.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class EnvConfig {

    private static final Map<String, String> dotEnvMap = new HashMap<>();

    static {
        loadDotEnv();
    }

    private static synchronized void loadDotEnv() {
        dotEnvMap.clear();
        File envFile = new File(System.getProperty("user.dir", "."), ".env");
        if (!envFile.exists() || !envFile.isFile()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx <= 0) {
                    continue;
                }
                String key = line.substring(0, eqIdx).trim();
                String val = line.substring(eqIdx + 1).trim();

                // Strip surrounding quotes (" or ') and trailing quotes/comments
                if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                    if (val.length() >= 2) {
                        val = val.substring(1, val.length() - 1);
                    }
                } else {
                    val = val.replaceAll("^[\"']+|[\"']+$", "").trim();
                }

                dotEnvMap.put(key, val);
            }
        } catch (Exception e) {
            System.err.println("[EnvConfig] Could not parse .env file: " + e.getMessage());
        }
    }

    public static String get(String key) {
        return get(key, null);
    }

    public static String get(String key, String defaultValue) {
        if (key == null) return defaultValue;
        String envVal = System.getenv(key);
        if (envVal != null && !envVal.isBlank()) {
            return envVal.trim();
        }
        String dotVal = dotEnvMap.get(key);
        if (dotVal != null && !dotVal.isBlank()) {
            return dotVal.trim();
        }
        return defaultValue;
    }

    public static String getApiKey() {
        String key = get("AI_API_KEY");
        if (key != null && !key.isBlank()) return key;
        key = get("GROQ_API");
        if (key != null && !key.isBlank()) return key;
        key = get("GROQ_API_KEY");
        if (key != null && !key.isBlank()) return key;
        key = get("OPENAI_API_KEY");
        if (key != null && !key.isBlank()) return key;
        return null;
    }

    public static String getModel() {
        String model = get("AI_MODEL");
        if (model != null && !model.isBlank()) return model;
        model = get("MODEL");
        if (model != null && !model.isBlank()) return model;
        return "gemini-1.5-flash";
    }

    public static String getBaseUrl() {
        String url = get("AI_BASE_URL");
        if (url != null && !url.isBlank()) return url;
        String key = getApiKey();
        if (key != null && key.startsWith("gsk_")) {
            return "https://api.groq.com/openai/v1";
        }
        return null;
    }

    public static boolean hasApiKey() {
        String key = getApiKey();
        return key != null && !key.isBlank() && !key.equalsIgnoreCase("demo_key");
    }

    public static String getMaskedApiKey() {
        return mask(getApiKey());
    }

    public static String mask(String secret) {
        if (secret == null || secret.isBlank()) {
            return "[NOT CONFIGURED]";
        }
        if (secret.length() <= 8) {
            return "********";
        }
        return secret.substring(0, 4) + "..." + secret.substring(secret.length() - 4);
    }

    public static void reload() {
        loadDotEnv();
    }
}
