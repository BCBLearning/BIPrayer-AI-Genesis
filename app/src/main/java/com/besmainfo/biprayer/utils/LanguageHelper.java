package com.besmainfo.biprayer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

// AJOUTEZ CES IMPORTS
import android.content.Intent;
import android.app.Activity;
import android.os.Handler;

import com.besmainfo.biprayer.activities.MainActivity; // Important : import de MainActivity

import java.util.Locale;


public class LanguageHelper {
    private static final String TAG = "LanguageHelper";
    private static final String PREF_LANGUAGE = "pref_language";
    private static final String PREF_AUTO_DETECT = "pref_auto_detect";

    public static void applySystemLanguage(Context context) {
        try {
            String systemLanguage = getSystemLanguage();
            setAppLanguage(context, systemLanguage, true);
            Log.d(TAG, "System language applied: " + systemLanguage);
        } catch (Exception e) {
            Log.e(TAG, "Error applying system language", e);
        }
    }

    public static String getSystemLanguage() {
        try {
            Locale systemLocale;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                systemLocale = Resources.getSystem().getConfiguration().getLocales().get(0);
            } else {
                systemLocale = Resources.getSystem().getConfiguration().locale;
            }
            
            String language = systemLocale.getLanguage();
            
            Log.d(TAG, "System locale detected: " + systemLocale.toString() + ", language: " + language);
            
            // Support des langues de l'application
            if (language.equals("fr") || language.equals("ar") || language.equals("en")) {
                return language;
            } else {
                // Si la langue système n'est pas supportée, retourner anglais
                Log.d(TAG, "System language not supported, defaulting to English");
                return "en";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting system language", e);
            return "en";
        }
    }

    public static void setAppLanguage(Context context, String languageCode, boolean autoDetect) {
    try {
        Log.d(TAG, "🌍 Setting app language to: " + languageCode);
        
        // Sauvegarder les préférences d'abord
        SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        prefs.edit()
            .putString(PREF_LANGUAGE, languageCode)
            .putBoolean(PREF_AUTO_DETECT, autoDetect)
            .apply();
        
        // Créer la locale
        Locale newLocale = new Locale(languageCode);
        Locale.setDefault(newLocale);
        
        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(newLocale);
            context.createConfigurationContext(configuration);
        } else {
            configuration.locale = newLocale;
        }
        
        // Mettre à jour la configuration
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        
        Log.d(TAG, "✅ Language set successfully: " + languageCode);
        
    } catch (Exception e) {
        Log.e(TAG, "❌ Error setting language", e);
    }
}

    /**
     * Version optimisée pour changement immédiat avec redémarrage
     */
    public static void setAppLanguageImmediate(Context context, String languageCode, boolean autoDetect) {
        try {
            Log.d(TAG, "⚡ Changement immédiat langue: " + languageCode + ", auto: " + autoDetect);
            
            // Créer la locale
            Locale newLocale = new Locale(languageCode);
            Locale.setDefault(newLocale);
            
            Resources resources = context.getResources();
            Configuration configuration = new Configuration(resources.getConfiguration());
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                configuration.setLocale(newLocale);
                configuration.setLayoutDirection(newLocale);
            } else {
                configuration.locale = newLocale;
            }
            
            // Mettre à jour la configuration
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            
            // Sauvegarder les préférences
            SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            prefs.edit()
                .putString(PREF_LANGUAGE, languageCode)
                .putBoolean(PREF_AUTO_DETECT, autoDetect)
                .apply();
            
            Log.d(TAG, "✅ Langue appliquée immédiatement: " + languageCode);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur changement immédiat langue", e);
        }
    }

    public static String getCurrentLanguage(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            boolean autoDetect = prefs.getBoolean(PREF_AUTO_DETECT, true);
            
            Log.d(TAG, "Getting current language - Auto detect: " + autoDetect);
            
            if (autoDetect) {
                String systemLang = getSystemLanguage();
                Log.d(TAG, "Auto-detected language: " + systemLang);
                return systemLang;
            } else {
                String savedLanguage = prefs.getString(PREF_LANGUAGE, getSystemLanguage());
                Log.d(TAG, "Saved language: " + savedLanguage);
                return savedLanguage != null ? savedLanguage : getSystemLanguage();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting current language", e);
            return "en";
        }
    }

    public static void applySavedLanguage(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            boolean autoDetect = prefs.getBoolean(PREF_AUTO_DETECT, true);
            String savedLanguage = prefs.getString(PREF_LANGUAGE, getSystemLanguage());
            
            Log.d(TAG, "Applying saved language - Auto: " + autoDetect + ", Language: " + savedLanguage);
            
            String languageToApply;
            
            if (autoDetect) {
                languageToApply = getSystemLanguage();
                Log.d(TAG, "Using auto-detected language: " + languageToApply);
            } else {
                languageToApply = savedLanguage != null ? savedLanguage : getSystemLanguage();
                Log.d(TAG, "Using saved language: " + languageToApply);
            }
            
            // Appliquer la langue
            setAppLanguage(context, languageToApply, autoDetect);
            
            Log.d(TAG, "Language applied successfully: " + languageToApply);
            
        } catch (Exception e) {
            Log.e(TAG, "Error applying saved language", e);
        }
    }

    /**
     * Applique la langue et redémarre immédiatement l'activité
     */
    public static void applyLanguageImmediately(Context context, String languageCode, boolean autoDetect) {
        try {
            Log.d(TAG, "🌍 Application immédiate de la langue: " + languageCode + ", auto: " + autoDetect);
            
            // Appliquer la langue
            setAppLanguageImmediate(context, languageCode, autoDetect);
            
            // Redémarrer l'activité
            restartActivity(context);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur application immédiate langue", e);
        }
    }

    /**
     * Redémarre l'activité courante pour appliquer les changements de langue
     */
    public static void restartActivity(Context context) {
        try {
            if (context instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) context;
                activity.recreate();
                Log.d(TAG, "🔄 Activité redémarrée pour changement de langue");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur redémarrage activité", e);
        }
    }

    /**
     * Vérifie et applique les changements de langue si nécessaire
     * À appeler dans onResume() de chaque activité
     */
    public static void checkAndApplyLanguageChange(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            boolean autoDetect = prefs.getBoolean(PREF_AUTO_DETECT, true);
            String savedLanguage = prefs.getString(PREF_LANGUAGE, getSystemLanguage());
            
            String currentLanguage = getCurrentLanguage(context);
            String targetLanguage = autoDetect ? getSystemLanguage() : savedLanguage;
            
            // Vérifier si la langue a changé
            if (!currentLanguage.equals(targetLanguage)) {
                Log.d(TAG, "🌍 Changement de langue détecté: " + currentLanguage + " → " + targetLanguage);
                applyLanguageImmediately(context, targetLanguage, autoDetect);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur vérification changement langue", e);
        }
    }

    /**
     * Vérifie si la langue a changé depuis la dernière fois
     */
    public static boolean hasLanguageChanged(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            boolean autoDetect = prefs.getBoolean(PREF_AUTO_DETECT, true);
            String savedLanguage = prefs.getString(PREF_LANGUAGE, getSystemLanguage());
            
            String currentLanguage = getCurrentLanguage(context);
            String targetLanguage = autoDetect ? getSystemLanguage() : savedLanguage;
            
            return !currentLanguage.equals(targetLanguage);
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur vérification changement langue", e);
            return false;
        }
    }

    public static String getLanguageName(String languageCode) {
        switch (languageCode) {
            case "fr": return "Français";
            case "en": return "English";
            case "ar": return "العربية";
            default: return getSystemLanguageName(languageCode);
        }
    }

    public static String getLanguageDisplayName(String languageCode, Context context) {
        try {
            Locale locale = new Locale(languageCode);
            Locale displayLocale = new Locale(getCurrentLanguage(context));
            return locale.getDisplayName(displayLocale);
        } catch (Exception e) {
            return getLanguageName(languageCode);
        }
    }

    public static String getOpusVoiceForLanguage(String languageCode) {
        switch (languageCode) {
            case "fr": return "fr-FR-Standard-A";
            case "en": return "en-US-Standard-B";
            case "ar": return "ar-XA-Standard-A";
            default: return "en-US-Standard-B";
        }
    }

    public static boolean isRTL(String languageCode) {
        return "ar".equals(languageCode);
    }

    public static boolean isRTLForCurrentLanguage(Context context) {
        return isRTL(getCurrentLanguage(context));
    }

    private static String getSystemLanguageName(String languageCode) {
        try {
            Locale locale = new Locale(languageCode);
            return locale.getDisplayName(locale);
        } catch (Exception e) {
            return "English";
        }
    }

    public static void forceApplyLanguage(Context context, String languageCode) {
        try {
            setAppLanguage(context, languageCode, false);
            Log.d(TAG, "Language forced to: " + languageCode);
        } catch (Exception e) {
            Log.e(TAG, "Error forcing language", e);
        }
    }

    public static String[] getSupportedLanguages() {
        return new String[]{"fr", "en", "ar"};
    }

    public static String getSupportedLanguageName(String languageCode) {
        switch (languageCode) {
            case "fr": return "Français";
            case "en": return "English";
            case "ar": return "العربية";
            default: return "Unknown";
        }
    }

    public static boolean isLanguageSupported(String languageCode) {
        for (String supported : getSupportedLanguages()) {
            if (supported.equals(languageCode)) {
                return true;
            }
        }
        return false;
    }

    public static void resetToSystemLanguage(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            boolean saved = prefs.edit()
                  .remove(PREF_LANGUAGE)
                  .putBoolean(PREF_AUTO_DETECT, true)
                  .commit();
            
            if (saved) {
                applySystemLanguage(context);
                Log.d(TAG, "Reset to system language - success");
            } else {
                Log.e(TAG, "Reset to system language - failed to save");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error resetting to system language", e);
        }
    }

    public static String getLanguageStatus(Context context) {
        try {
            boolean autoDetect = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                       .getBoolean(PREF_AUTO_DETECT, true);
            String currentLanguage = getCurrentLanguage(context);
            
            if (autoDetect) {
                return "Auto (" + getLanguageName(currentLanguage) + ")";
            } else {
                return getLanguageName(currentLanguage);
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    // Méthode utilitaire pour debugger les préférences
    public static void logLanguagePreferences(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            boolean autoDetect = prefs.getBoolean(PREF_AUTO_DETECT, true);
            String language = prefs.getString(PREF_LANGUAGE, "default");
            
            Log.d(TAG, "Language Preferences - Auto: " + autoDetect + ", Language: " + language);
        } catch (Exception e) {
            Log.e(TAG, "Error logging preferences", e);
        }
    }

/**
 * Redémarre l'application pour appliquer les changements de langue
 */
public static void restartApp(Context context) {
    try {
        Log.d(TAG, "🔄 Redémarrage de l'application...");
        
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
        
        Log.d(TAG, "✅ Application redémarrée avec succès");
        
    } catch (Exception e) {
        Log.e(TAG, "❌ Erreur redémarrage application", e);
        
        // Fallback : redémarrer l'activité courante
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.recreate();
        }
    }
}

}