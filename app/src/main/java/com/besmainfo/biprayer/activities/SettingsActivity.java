package com.besmainfo.biprayer.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import org.json.JSONObject;
import android.widget.*;
import com.besmainfo.biprayer.R;

import androidx.appcompat.app.AppCompatActivity;
import com.besmainfo.biprayer.ai.GeminiBasicClient;
import com.besmainfo.biprayer.ai.OpusClient;
import com.besmainfo.biprayer.ai.QdrantClient;
import com.besmainfo.biprayer.utils.ConfigReader;
import com.besmainfo.biprayer.utils.LanguageHelper;

public class SettingsActivity extends AppCompatActivity {
    
    private static final String TAG = "SettingsActivity";
    
    private CheckBox checkAutoDetect, checkNotifications, checkDarkMode;
    private Spinner spinnerLanguage;
    private RadioGroup radioCalculationMethod;
    private EditText editLocationLat, editLocationLng;
    private TextView textApiStatus, textAppVersion;
    private Button btnTestAPI, btnReset, btnSave, btnHelp;
    
    private GeminiBasicClient geminiClient;
    private OpusClient opusClient;
    private QdrantClient qdrantClient;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LanguageHelper.applySavedLanguage(this);
        setContentView(R.layout.activity_settings);
        
        initializeClients();
        initializeUI();
        loadCurrentSettings();
        setupClickListeners();
        updateApiStatus();
    }
    
    private void initializeClients() {
    ConfigReader.initialize(this);
    String geminiApiKey = ConfigReader.getGeminiApiKey();
    String opusApiKey = ConfigReader.getOpusApiKey();
    String qdrantApiKey = ConfigReader.getQdrantApiKey();
    String qdrantBaseUrl = ConfigReader.getQdrantBaseUrl();
    
    // Passer le contexte aux clients
    geminiClient = new GeminiBasicClient(this, geminiApiKey);
    opusClient = new OpusClient(this, opusApiKey);
    qdrantClient = new QdrantClient(this, qdrantBaseUrl, qdrantApiKey);
    
    Log.d(TAG, "Qdrant URL: " + qdrantBaseUrl);
    Log.d(TAG, "Opus configured: " + opusClient.isConfigured());
    Log.d(TAG, "Qdrant configured: " + qdrantClient.isConfigured());
    Log.d(TAG, "Gemini configured: " + ConfigReader.hasValidGeminiKey());
}
    
    private void initializeUI() {
        checkAutoDetect = findViewById(R.id.checkAutoDetect);
        checkNotifications = findViewById(R.id.checkNotifications);
        checkDarkMode = findViewById(R.id.checkDarkMode);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        radioCalculationMethod = findViewById(R.id.radioCalculationMethod);
        editLocationLat = findViewById(R.id.editLocationLat);
        editLocationLng = findViewById(R.id.editLocationLng);
        textApiStatus = findViewById(R.id.textApiStatus);
        textAppVersion = findViewById(R.id.textAppVersion);
        btnTestAPI = findViewById(R.id.btnTestAPI);
        btnReset = findViewById(R.id.btnReset);
        btnSave = findViewById(R.id.btnSave);
        btnHelp = findViewById(R.id.btnHelp);
        
        // Configurer les textes
        checkAutoDetect.setText(getString(R.string.auto_detect));
        checkNotifications.setText(getString(R.string.notifications));
        checkDarkMode.setText(getString(R.string.dark_mode));
        btnTestAPI.setText(getString(R.string.button_test));
        btnReset.setText(getString(R.string.button_reset));
        btnSave.setText(getString(R.string.button_save));
        btnHelp.setText(getString(R.string.button_help));
        
        // Configurer les hints
        editLocationLat.setHint(getString(R.string.hint_latitude));
        editLocationLng.setHint(getString(R.string.hint_longitude));
        
        // Configurer le spinner de langue
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            this, R.array.languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);
        
        // Version de l'app
        textAppVersion.setText(getString(R.string.app_version));
    }
    
    private void loadCurrentSettings() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        
        // Langue
        boolean autoDetect = prefs.getBoolean("pref_auto_detect", true);
        String savedLanguage = prefs.getString("pref_language", "fr");
        
        checkAutoDetect.setChecked(autoDetect);
        spinnerLanguage.setEnabled(!autoDetect);
        
        int languagePosition = getLanguagePosition(savedLanguage);
        spinnerLanguage.setSelection(languagePosition);
        
        // Autres préférences
        checkNotifications.setChecked(prefs.getBoolean("pref_notifications", true));
        checkDarkMode.setChecked(prefs.getBoolean("pref_dark_mode", false));
        
        // Méthode de calcul
        String calcMethod = prefs.getString("pref_calculation_method", "MWL");
        setCalculationMethod(calcMethod);
        
        // Localisation
        double lat = prefs.getFloat("pref_latitude", 0f);
        double lng = prefs.getFloat("pref_longitude", 0f);
        if (lat != 0 && lng != 0) {
            editLocationLat.setText(String.valueOf(lat));
            editLocationLng.setText(String.valueOf(lng));
        }
        
        // Écouteur pour la détection automatique
        checkAutoDetect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spinnerLanguage.setEnabled(!isChecked);
        });
    }
    
    private void setupClickListeners() {
        btnTestAPI.setOnClickListener(v -> testAllAPIs());
        btnReset.setOnClickListener(v -> resetSettings());
        btnSave.setOnClickListener(v -> saveSettings());
        btnHelp.setOnClickListener(v -> showHelp());
    }
    
    private void testAllAPIs() {
        btnTestAPI.setEnabled(false);
        textApiStatus.setText(getString(R.string.status_testing_apis));
        
        new Thread(() -> {
            StringBuilder status = new StringBuilder();
            status.append("**").append(getString(R.string.api_status_title)).append("**\n\n");
            
            // Tester Gemini
            String geminiStatus = geminiClient.testConnection();
            boolean geminiWorking = geminiStatus.contains("✅") || geminiStatus.contains("Connecté");
            status.append("🤖 **Gemini AI:** ").append(geminiWorking ? "✅ " + getString(R.string.status_operational) : "❌ " + getString(R.string.status_error)).append("\n");
            
            // Tester Opus
            if (opusClient.isConfigured()) {
                String opusStatus = opusClient.testConnection();
                boolean opusWorking = opusStatus.contains("✅") || opusStatus.contains("successful");
                status.append("🎵 **Opus TTS:** ").append(opusWorking ? "✅ " + getString(R.string.status_operational) : "❌ " + getString(R.string.status_error)).append("\n");
            } else {
                status.append("🎵 **Opus TTS:** ❌ ").append(getString(R.string.api_not_configured)).append("\n");
            }
            
            // Tester Qdrant
            if (qdrantClient.isConfigured()) {
                status.append("🔍 **Qdrant Vector:** ✅ ").append(getString(R.string.status_configured)).append("\n");
            } else {
                status.append("🔍 **Qdrant Vector:** ❌ ").append(getString(R.string.api_not_configured)).append("\n");
            }
            
            // Résumé
            status.append("\n**").append(getString(R.string.summary)).append("**\n");
            if (geminiWorking) {
                status.append("✅ ").append(getString(R.string.ready_for_use)).append("\n");
            } else {
                status.append("⚠️ ").append(getString(R.string.demo_mode_active)).append("\n");
            }
            
            runOnUiThread(() -> {
                textApiStatus.setText(status.toString());
                btnTestAPI.setEnabled(true);
                Toast.makeText(this, R.string.test_completed, Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
    
    private void resetSettings() {
        new android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_confirm_reset))
            .setMessage("Êtes-vous sûr de vouloir réinitialiser tous les paramètres ?")
            .setPositiveButton(getString(R.string.button_ok), (dialog, which) -> {
                SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                prefs.edit().clear().apply();
                loadCurrentSettings();
                LanguageHelper.resetToSystemLanguage(this);
                
                updateApiStatus();
                Toast.makeText(this, R.string.reset_complete, Toast.LENGTH_SHORT).show();
                
                // ✅ RETOURNER à l'activité précédente au lieu de redémarrer
                finish();
            })
            .setNegativeButton(getString(R.string.button_cancel), null)
            .show();
    }
    
    private void saveSettings() {
    SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    
    // Langue
    boolean autoDetect = checkAutoDetect.isChecked();
    String selectedLanguage = getSelectedLanguage();
    
    editor.putBoolean("pref_auto_detect", autoDetect);
    if (!autoDetect) {
        editor.putString("pref_language", selectedLanguage);
    }
    
    // 🌍 APPLIQUER la langue immédiatement
    LanguageHelper.setAppLanguage(this, selectedLanguage, autoDetect);
    
    // Autres préférences
    editor.putBoolean("pref_notifications", checkNotifications.isChecked());
    editor.putBoolean("pref_dark_mode", checkDarkMode.isChecked());
    
    // Méthode de calcul
    String calcMethod = getSelectedCalculationMethod();
    editor.putString("pref_calculation_method", calcMethod);
    
    // Localisation
    try {
        double lat = Double.parseDouble(editLocationLat.getText().toString());
        double lng = Double.parseDouble(editLocationLng.getText().toString());
        editor.putFloat("pref_latitude", (float) lat);
        editor.putFloat("pref_longitude", (float) lng);
    } catch (NumberFormatException e) {
        // Ignorer si non valide
    }
    
    if (editor.commit()) {
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
        
        // 🔄 REDÉMARRER après un court délai
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                LanguageHelper.restartApp(SettingsActivity.this);
            }
        }, 500);
        
    } else {
        Toast.makeText(this, R.string.settings_error, Toast.LENGTH_SHORT).show();
    }
}
    
    private void showHelp() {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }
    
    private void updateApiStatus() {
        StringBuilder status = new StringBuilder();
        status.append("**").append(getString(R.string.current_configuration)).append("**\n\n");
        
        // Statut Gemini
        boolean hasGeminiKey = ConfigReader.hasValidGeminiKey();
        status.append("🤖 **Gemini:** ").append(hasGeminiKey ? "✅ " + getString(R.string.api_configured) : "❌ " + getString(R.string.demo_mode)).append("\n");
        
        // Statut Opus
        boolean hasOpusKey = ConfigReader.hasValidOpusKey();
        status.append("🎵 **Opus TTS:** ").append(hasOpusKey ? "✅ " + getString(R.string.api_configured) : "❌ " + getString(R.string.not_configured)).append("\n");
        
        // Statut Qdrant
        boolean hasQdrantKey = ConfigReader.hasValidQdrantKey();
        status.append("🔍 **Qdrant:** ").append(hasQdrantKey ? "✅ " + getString(R.string.api_configured) : "❌ " + getString(R.string.not_configured)).append("\n");
        
        // Recommandations
        status.append("\n**").append(getString(R.string.recommendations)).append("**\n");
        if (!hasGeminiKey) {
            status.append("• ").append(getString(R.string.configure_gemini)).append("\n");
        }
        if (!hasOpusKey) {
            status.append("• ").append(getString(R.string.configure_opus)).append("\n");
        }
        if (!hasQdrantKey) {
            status.append("• ").append(getString(R.string.configure_qdrant)).append("\n");
        }
        
        // Instructions pour obtenir les clés
        if (!hasGeminiKey || !hasOpusKey || !hasQdrantKey) {
            status.append("\n**").append(getString(R.string.how_to_configure)).append("**\n");
            status.append("1. ").append(getString(R.string.step_gemini)).append("\n");
            status.append("2. ").append(getString(R.string.step_qdrant)).append("\n");
            status.append("3. ").append(getString(R.string.step_opus)).append("\n");
            status.append("4. ").append(getString(R.string.step_restart)).append("\n");
        }
        
        textApiStatus.setText(status.toString());
    }
    
    private int getLanguagePosition(String languageCode) {
        switch (languageCode) {
            case "fr": return 0;
            case "en": return 1;
            case "ar": return 2;
            default: return 0;
        }
    }
    
    private String getSelectedLanguage() {
        int position = spinnerLanguage.getSelectedItemPosition();
        switch (position) {
            case 0: return "fr";
            case 1: return "en";
            case 2: return "ar";
            default: return "fr";
        }
    }
    
    private void setCalculationMethod(String method) {
        int radioId = R.id.radioMWL; // Par défaut
        
        switch (method) {
            case "MWL": radioId = R.id.radioMWL; break;
            case "ISNA": radioId = R.id.radioISNA; break;
            case "Egypt": radioId = R.id.radioEgypt; break;
            case "Makkah": radioId = R.id.radioMakkah; break;
        }
        
        radioCalculationMethod.check(radioId);
    }
    
    private String getSelectedCalculationMethod() {
        int checkedId = radioCalculationMethod.getCheckedRadioButtonId();
        
        if (checkedId == R.id.radioMWL) return "MWL";
        if (checkedId == R.id.radioISNA) return "ISNA";
        if (checkedId == R.id.radioEgypt) return "Egypt";
        if (checkedId == R.id.radioMakkah) return "Makkah";
        
        return "MWL";
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 🌍 Vérifier les changements de langue à chaque retour
        LanguageHelper.checkAndApplyLanguageChange(this);
        updateApiStatus();
    }
}