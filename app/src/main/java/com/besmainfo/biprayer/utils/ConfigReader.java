package com.besmainfo.biprayer.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    private static final String TAG = "ConfigReader";
    private static Properties properties;
    private static boolean initialized = false;

    public static void initialize(Context context) {
        if (initialized) {
            Log.d(TAG, "Déjà initialisé");
            return;
        }
        
        try {
            Log.d(TAG, "Initialisation de ConfigReader...");
            properties = new Properties();
            AssetManager assetManager = context.getAssets();
            
            String[] files = assetManager.list("");
            boolean keysFileExists = false;
            for (String file : files) {
                if ("keys.properties".equals(file)) {
                    keysFileExists = true;
                    break;
                }
            }
            
            if (!keysFileExists) {
                Log.w(TAG, "keys.properties non trouvé dans assets");
                properties = new Properties();
                initialized = true;
                return;
            }
            
            InputStream inputStream = assetManager.open("keys.properties");
            properties.load(inputStream);
            inputStream.close();
            initialized = true;
            
            Log.d(TAG, "Configuration chargée depuis assets/keys.properties");
            logKeyDiagnostics();
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur chargement configuration: " + e.getMessage(), e);
            properties = new Properties();
            initialized = true;
        }
    }

    private static void logKeyDiagnostics() {
        try {
            String geminiKey = getGeminiApiKey();
            String opusKey = getOpusApiKey();
            String qdrantKey = getQdrantApiKey();
            
            Log.d(TAG, "DIAGNOSTIC - GEMINI_API_KEY: " + 
                  (geminiKey == null ? "NULL" : 
                   geminiKey.isEmpty() ? "VIDE" : 
                   geminiKey.contains("your_actual_") ? "VALEUR_DEFAUT" : 
                   "PRESENT (" + geminiKey.length() + " chars)"));
                   
            Log.d(TAG, "DIAGNOSTIC - OPUS_API_KEY: " + 
                  (opusKey == null ? "NULL" : 
                   opusKey.isEmpty() ? "VIDE" : 
                   opusKey.contains("your_actual_") ? "VALEUR_DEFAUT" : 
                   "PRESENT (" + opusKey.length() + " chars)"));
                   
            Log.d(TAG, "DIAGNOSTIC - QDRANT_API_KEY: " + 
                  (qdrantKey == null ? "NULL" : 
                   qdrantKey.isEmpty() ? "VIDE" : 
                   qdrantKey.contains("your_actual_") ? "VALEUR_DEFAUT" : 
                   "PRESENT (" + qdrantKey.length() + " chars)"));
                   
            Log.d(TAG, "DIAGNOSTIC - QDRANT_BASE_URL: " + getQdrantBaseUrl());
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur diagnostic: " + e.getMessage());
        }
    }

    public static String getGeminiApiKey() {
        if (properties == null) {
            Log.e(TAG, "Properties est null!");
            return "";
        }
        try {
            String key = properties.getProperty("GEMINI_API_KEY", "").trim();
            return key;
        } catch (Exception e) {
            Log.e(TAG, "Erreur getGeminiApiKey: " + e.getMessage());
            return "";
        }
    }

    public static String getQdrantApiKey() {
        if (properties == null) return "";
        try {
            return properties.getProperty("QDRANT_API_KEY", "").trim();
        } catch (Exception e) {
            Log.e(TAG, "Erreur getQdrantApiKey: " + e.getMessage());
            return "";
        }
    }

    public static String getOpusApiKey() {
        if (properties == null) return "";
        try {
            return properties.getProperty("OPUS_API_KEY", "").trim();
        } catch (Exception e) {
            Log.e(TAG, "Erreur getOpusApiKey: " + e.getMessage());
            return "";
        }
    }

    public static String getQdrantBaseUrl() {
        if (properties == null) return "";
        try {
            return properties.getProperty("QDRANT_BASE_URL", "https://cloud.qdrant.io").trim();
        } catch (Exception e) {
            Log.e(TAG, "Erreur getQdrantBaseUrl: " + e.getMessage());
            return "https://cloud.qdrant.io";
        }
    }

    public static boolean hasValidGeminiKey() {
        try {
            String key = getGeminiApiKey();
            return key != null && 
                   !key.isEmpty() && 
                   !key.contains("your_actual_") && 
                   key.length() > 20 &&
                   key.startsWith("AIza");
        } catch (Exception e) {
            Log.e(TAG, "Erreur hasValidGeminiKey: " + e.getMessage());
            return false;
        }
    }

    public static boolean hasValidOpusKey() {
        try {
            String key = getOpusApiKey();
            return key != null && 
                   !key.isEmpty() && 
                   !key.contains("your_actual_") && 
                   key.length() > 10;
        } catch (Exception e) {
            Log.e(TAG, "Erreur hasValidOpusKey: " + e.getMessage());
            return false;
        }
    }

    public static boolean hasValidQdrantKey() {
        try {
            String key = getQdrantApiKey();
            return key != null && 
                   !key.isEmpty() && 
                   !key.contains("your_actual_") && 
                   key.length() > 10;
        } catch (Exception e) {
            Log.e(TAG, "Erreur hasValidQdrantKey: " + e.getMessage());
            return false;
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static String getDiagnosticInfo() {
        StringBuilder diagnostic = new StringBuilder();
        diagnostic.append("🔍 **Diagnostic Configuration**\n\n");
        
        boolean geminiValid = hasValidGeminiKey();
        boolean opusValid = hasValidOpusKey();
        boolean qdrantValid = hasValidQdrantKey();
        
        diagnostic.append("🤖 **Gemini AI:** ").append(geminiValid ? "✅ Configuré" : "❌ Mode Démo").append("\n");
        diagnostic.append("🎵 **Opus TTS:** ").append(opusValid ? "✅ Configuré" : "❌ Non configuré").append("\n");
        diagnostic.append("🔍 **Qdrant Vector:** ").append(qdrantValid ? "✅ Configuré" : "❌ Non configuré").append("\n");
        
        if (!geminiValid) {
            diagnostic.append("\n**Pour activer Gemini AI:**\n");
            diagnostic.append("1. Clé gratuite: https://aistudio.google.com/\n");
            diagnostic.append("2. Modifiez assets/keys.properties\n");
            diagnostic.append("3. Ajoutez: GEMINI_API_KEY=votre_clé_ici\n");
            diagnostic.append("4. Redémarrez l'application\n");
        }
        
        if (!opusValid || !qdrantValid) {
            diagnostic.append("\n**Services supplémentaires:**\n");
            if (!opusValid) {
                diagnostic.append("• Opus TTS: https://opus.ai/\n");
            }
            if (!qdrantValid) {
                diagnostic.append("• Qdrant: https://cloud.qdrant.io/\n");
            }
        }
        
        return diagnostic.toString();
    }

    public static String getApiStatusSummary() {
        StringBuilder status = new StringBuilder();
        
        boolean geminiValid = hasValidGeminiKey();
        boolean opusValid = hasValidOpusKey();
        boolean qdrantValid = hasValidQdrantKey();
        
        status.append("**Statut des Services AI**\n\n");
        status.append("🤖 Gemini: ").append(geminiValid ? "✅" : "❌").append("\n");
        status.append("🎵 Opus: ").append(opusValid ? "✅" : "❌").append("\n");
        status.append("🔍 Qdrant: ").append(qdrantValid ? "✅" : "❌").append("\n");
        
        if (geminiValid && opusValid && qdrantValid) {
            status.append("\n✅ **Tous les services sont configurés!**");
        } else {
            status.append("\n⚠️ **Certains services nécessitent une configuration**");
        }
        
        return status.toString();
    }
}