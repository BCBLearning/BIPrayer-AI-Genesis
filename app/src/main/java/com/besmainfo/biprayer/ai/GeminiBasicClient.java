package com.besmainfo.biprayer.ai;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiBasicClient {
    private static final String TAG = "GeminiBasicClient";
    private final String apiKey;
    private final Context context;
    private String workingModel = null;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ✅ NOUVEAUX MODÈLES 2025
    private static final String[] GEMINI_MODELS = {
        "gemini-2.0-flash-exp",      // Modèle le plus récent
        "gemini-2.0-flash",          // Version stable
        "gemini-2.0-flash-lite",     // Version légère
        "gemini-1.5-flash",          // Ancien mais fiable
        "gemini-1.5-pro"             // Pour réponses complexes
    };

    public GeminiBasicClient(Context context, String apiKey) {
        this.context = context;
        this.apiKey = apiKey;
        Log.d(TAG, "Client initialisé avec nouveau format d'authentification");
        testAllModels();
    }

    // ============================================================
    //  VALIDATION CLE API
    // ============================================================
    private boolean isApiKeyValid() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            Log.w(TAG, "Clé API: null ou vide");
            return false;
        }
        
        if (apiKey.contains("your_actual_") || apiKey.contains("example") || apiKey.contains("placeholder")) {
            Log.w(TAG, "Clé API: valeur par défaut détectée");
            return false;
        }
        
        if (apiKey.length() < 30) {
            Log.w(TAG, "Clé API: trop courte (" + apiKey.length() + " caractères)");
            return false;
        }
        
        if (!apiKey.startsWith("AIza")) {
            Log.w(TAG, "Clé API: format Google invalide");
            return false;
        }
        
        Log.d(TAG, "Clé API: valide");
        return true;
    }

    // ============================================================
    //  TEST DES MODELES - NOUVEAU FORMAT
    // ============================================================
    private void testAllModels() {
        if (!isApiKeyValid()) {
            Log.w(TAG, "Clé API invalide, skip test des modèles");
            return;
        }
        
        executor.submit(() -> {
            for (String model : GEMINI_MODELS) {
                if (testModel(model)) {
                    workingModel = model;
                    Log.i(TAG, "✅ Modèle sélectionné: " + model);
                    break;
                }
            }
            
            if (workingModel == null) {
                Log.w(TAG, "⚠️ Aucun modèle Gemini disponible");
            }
        });
    }

    private boolean testModel(String model) {
        HttpURLConnection conn = null;
        try {
            // ✅ NOUVELLE URL sans paramètre key
            String urlString = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            // ✅ NOUVEAU HEADER d'authentification
            conn.setRequestProperty("X-goog-api-key", apiKey);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setDoOutput(true);

            String jsonRequest = "{\"contents\":[{\"parts\":[{\"text\":\"Test\"}]}]}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "Test modèle " + model + ": HTTP " + responseCode);
            
            return responseCode == 200;

        } catch (Exception e) {
            Log.d(TAG, "Test modèle " + model + ": ❌ Exception - " + e.getMessage());
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ============================================================
    //  APPEL PRINCIPAL - NOUVEAU FORMAT
    // ============================================================
    public String callGemini(String prompt) {
        Log.d(TAG, "Appel Gemini: " + (prompt.length() > 50 ? prompt.substring(0, 50) + "..." : prompt));
        
        if (!isApiKeyValid()) {
            Log.w(TAG, "Clé API invalide, utilisation mode démo");
            return getEnhancedDemoResponse(prompt);
        }

        // Essai avec modèle déjà détecté
        if (workingModel != null) {
            String result = callWithModel(prompt, workingModel);
            if (!result.contains("❌")) {
                return result;
            } else {
                Log.w(TAG, "Échec avec modèle " + workingModel + ", recherche alternative...");
                workingModel = null;
            }
        }

        // Fallback multi-modèles
        for (String model : GEMINI_MODELS) {
            try {
                Log.d(TAG, "Essai avec modèle: " + model);
                String result = callWithModel(prompt, model);
                
                if (!result.contains("❌")) {
                    workingModel = model;
                    Log.i(TAG, "✅ Réponse réussie avec modèle: " + model);
                    return result;
                } else {
                    Log.w(TAG, "❌ Échec avec " + model + ": " + result);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception avec " + model + ": " + e.getMessage());
            }
        }
        
        Log.e(TAG, "❌ Tous les modèles ont échoué, fallback vers mode démo");
        return getEnhancedDemoResponse(prompt);
    }

    private String callWithModel(String prompt, String model) {
        HttpURLConnection conn = null;
        try {
            // ✅ NOUVELLE URL sans paramètre key
            String urlString = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            // ✅ NOUVEAU HEADER d'authentification
            conn.setRequestProperty("X-goog-api-key", apiKey);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);

            String jsonRequest = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]}]}";

            Log.d(TAG, "Envoi JSON à " + model);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "Code HTTP " + responseCode + " pour " + model);

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                responseCode == 200 ? conn.getInputStream() : conn.getErrorStream()
            ));
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            if (responseCode == 200) {
                return extractTextFromResponse(response.toString());
            } else {
                // Essayer d'extraire le message d'erreur détaillé
                try {
                    JSONObject errorJson = new JSONObject(response.toString());
                    String errorMsg = errorJson.getJSONObject("error").getString("message");
                    return "❌ Erreur " + responseCode + " avec " + model + ": " + errorMsg;
                } catch (Exception e) {
                    return "❌ Erreur " + responseCode + " avec " + model + ": " + response.toString();
                }
            }

        } catch (Exception e) {
            return "❌ Exception avec " + model + ": " + e.getMessage();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ============================================================
    //  MÉTHODES EXISTANTES (conservées)
    // ============================================================
    
    private String extractTextFromResponse(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            JSONArray candidates = json.getJSONArray("candidates");
            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject content = candidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            JSONObject part = parts.getJSONObject(0);
            return part.getString("text").trim();
        } catch (Exception e) {
            Log.e(TAG, "Erreur extraction JSON: " + e.getMessage());
            return "Erreur d'extraction: " + e.getMessage();
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    // ============================================================
    //  MÉTHODES INTERNATIONALISÉES (conservées)
    // ============================================================
    
    private String getStringResource(String resourceName, String defaultValue) {
        try {
            int resId = context.getResources().getIdentifier(resourceName, "string", context.getPackageName());
            if (resId != 0) {
                return context.getString(resId);
            }
        } catch (Exception e) {
            Log.w(TAG, "Ressource non trouvée: " + resourceName);
        }
        return defaultValue;
    }

    private String getEnhancedDemoResponse(String prompt) {
        // [Votre code existant conservé]
        String lowerPrompt = prompt.toLowerCase();
        
        Map<String, String[]> knowledgeBase = new HashMap<>();
        knowledgeBase.put("prière", new String[]{
            getStringResource("demo_prayer_1", "🕌 **Conseil sur la Prière**\n\nLa prière (Salah) est le deuxième pilier de l'Islam..."),
            getStringResource("demo_prayer_2", "📖 **Importance de la Prière**\n\n«La prière préserve de la turpitude et du blâmable.»...")
        });
        // [Le reste de votre code démo...]
        
        for (Map.Entry<String, String[]> entry : knowledgeBase.entrySet()) {
            if (lowerPrompt.contains(entry.getKey())) {
                String[] responses = entry.getValue();
                int randomIndex = (int) (Math.random() * responses.length);
                return responses[randomIndex] + getConfigurationHelp();
            }
        }
        
        String[] defaultResponses = {
            getStringResource("demo_default_1", "🕌 **BIPrayer AI - Assistant Spirituel**\n\nJe suis là pour vous accompagner..."),
            getStringResource("demo_default_2", "🌙 **Guidance Islamique**\n\nQue souhaitez-vous savoir sur...")
        };
        
        int randomIndex = (int) (Math.random() * defaultResponses.length);
        return defaultResponses[randomIndex] + getConfigurationHelp();
    }

    private String getConfigurationHelp() {
        return "\n\n" + getStringResource("config_help", 
            "🔧 **Pour activer Gemini AI:**\n" +
            "1. Obtenez une clé GRATUITE sur https://aistudio.google.com/\n" +
            "2. Ajoutez-la dans assets/keys.properties\n" +
            "3. Format: GEMINI_API_KEY=votre_clé_ici\n" +
            "4. Redémarrez l'application");
    }

    public String testConnection() {
        if (!isApiKeyValid()) {
            return getStringResource("error_api_key_invalid", "❌ Clé API manquante ou invalide") + "\n\n" + getConfigurationHelp();
        }

        if (workingModel != null) {
            return getStringResource("connection_success", "✅ Connecté avec modèle: ") + workingModel;
        }

        try {
            String result = callGemini("Test de connexion - réponse courte");
            if (result.contains("❌")) {
                return getStringResource("connection_failed", "❌ Échec de connexion: ") + result + "\n\n" + getConfigurationHelp();
            } else {
                return getStringResource("api_operational", "✅ Gemini API: Opérationnel") + 
                       (workingModel != null ? " (" + getStringResource("model", "Modèle") + ": " + workingModel + ")" : "");
            }
        } catch (Exception e) {
            return getStringResource("error_general", "❌ Erreur: ") + e.getMessage() + "\n\n" + getConfigurationHelp();
        }
    }

    public String getWorkingModel() {
        return workingModel;
    }

    public void resetModelSelection() {
        workingModel = null;
        testAllModels();
    }
}