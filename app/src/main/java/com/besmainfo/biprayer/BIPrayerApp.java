package com.besmainfo.biprayer;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import com.besmainfo.biprayer.utils.ConfigReader;
import com.besmainfo.biprayer.utils.LanguageHelper;

public class BIPrayerApp extends Application {
    private static final String TAG = "BIPrayerApp";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🚀 Application BIPrayer démarrée");
        
        try {
            // Initialiser la configuration
            ConfigReader.initialize(this);
            
            // Log des préférences avant application
            logPreferencesBeforeApply();
            
            // Appliquer la langue sauvegardée
            LanguageHelper.applySavedLanguage(this);
            
            Log.d(TAG, "✅ Configuration et langue initialisées avec succès");
            
            // Log de diagnostic
            logAppDiagnostics();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur initialisation application: " + e.getMessage(), e);
        }
    }

    private void logPreferencesBeforeApply() {
        try {
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            boolean autoDetect = prefs.getBoolean("pref_auto_detect", true);
            String language = prefs.getString("pref_language", "default");
            
            Log.d(TAG, "📋 Préférences avant application:");
            Log.d(TAG, "• Auto-detect: " + autoDetect);
            Log.d(TAG, "• Langue: " + language);
            Log.d(TAG, "• Toutes les préférences: " + prefs.getAll().toString());
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur log préférences", e);
        }
    }

    private void logAppDiagnostics() {
        try {
            String geminiKey = ConfigReader.getGeminiApiKey();
            String currentLanguage = LanguageHelper.getCurrentLanguage(this);
            boolean hasValidKey = ConfigReader.hasValidGeminiKey();
            
            Log.d(TAG, "🔍 DIAGNOSTIC APPLICATION:");
            Log.d(TAG, "• Gemini API Key: " + (geminiKey != null ? "PRÉSENTE" : "ABSENTE"));
            Log.d(TAG, "• Clé valide: " + hasValidKey);
            Log.d(TAG, "• Langue actuelle: " + currentLanguage);
            Log.d(TAG, "• Mode: " + (hasValidKey ? "IA ACTIVÉE" : "MODE DÉMO"));
            
            // Log des préférences après application
            LanguageHelper.logLanguagePreferences(this);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur diagnostic: " + e.getMessage());
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG, "🛑 Application BIPrayer terminée");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "⚠️ Mémoire faible - Optimisations nécessaires");
    }
}