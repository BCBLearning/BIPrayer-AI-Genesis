package com.besmainfo.biprayer.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.besmainfo.biprayer.ai.GeminiBasicClient;
import com.besmainfo.biprayer.utils.ConfigReader;
import com.besmainfo.biprayer.utils.LanguageHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.besmainfo.biprayer.R;
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    private EditText editTextPrompt;
    private Button buttonSend;
    private TextView textViewResponse, textViewStatus;
    private LinearLayout cardQuran, cardPrayerTimes, cardCamera, cardSettings;
    
    private GeminiBasicClient geminiClient;
    private ExecutorService executor;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Appliquer la langue sauvegardée avant de créer la vue
        LanguageHelper.applySavedLanguage(this);
        
        try {
            Log.d(TAG, "🚀 Démarrage de MainActivity...");
            
            // Vérifier que l'application est initialisée
            if (!ConfigReader.isInitialized()) {
                Log.w(TAG, "ConfigReader non initialisé, initialisation maintenant...");
                ConfigReader.initialize(this);
            }
            
            setContentView(R.layout.activity_main);
            
            // Initialiser après setContentView pour éviter les NPE
            mainHandler = new Handler(Looper.getMainLooper());
            executor = Executors.newSingleThreadExecutor();
            
            setupViews();
            setupCardClicks();
            setupGemini();
            
            // Test automatique retardé
            new Handler().postDelayed(this::runConnectionTest, 2000);
            
            Log.d(TAG, "✅ MainActivity créée avec succès!");
            
        } catch (Exception e) {
            Log.e(TAG, "💥 ERREUR CRITIQUE dans onCreate: " + e.getMessage(), e);
            showErrorToast(getString(R.string.error_connection, e.getMessage()));
            
            // Afficher un écran d'erreur simple
            showErrorScreen(e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 🌍 Vérifier les changements de langue à chaque retour
        LanguageHelper.checkAndApplyLanguageChange(this);
        
        // Rafraîchir le statut
        if (geminiClient != null) {
            String status = ConfigReader.hasValidGeminiKey() ? 
                getString(R.string.status_initialized) : getString(R.string.status_demo_mode);
            updateStatus(status);
        }
    }

    private void setupViews() {
        try {
            Log.d(TAG, "🔧 Setup des vues...");
            
            editTextPrompt = findViewById(R.id.editTextPrompt);
            buttonSend = findViewById(R.id.buttonSend);
            textViewResponse = findViewById(R.id.textViewResponse);
            textViewStatus = findViewById(R.id.textViewStatus);
            
            cardQuran = findViewById(R.id.cardQuran);
            cardPrayerTimes = findViewById(R.id.cardPrayerTimes);
            cardCamera = findViewById(R.id.cardCamera);
            cardSettings = findViewById(R.id.cardSettings);
            
            // Vérifier que toutes les vues sont trouvées
            if (editTextPrompt == null) throw new RuntimeException("editTextPrompt non trouvé");
            if (buttonSend == null) throw new RuntimeException("buttonSend non trouvé");
            if (textViewResponse == null) throw new RuntimeException("textViewResponse non trouvé");
            if (textViewStatus == null) throw new RuntimeException("textViewStatus non trouvé");
            
            // Configurer le hint avec la ressource string
            editTextPrompt.setHint(R.string.hint_question);
            
            buttonSend.setOnClickListener(v -> askQuestion());
            
            Log.d(TAG, "✅ Vues initialisées");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur setupViews: " + e.getMessage(), e);
            throw new RuntimeException("Erreur initialisation interface: " + e.getMessage(), e);
        }
    }

    private void setupCardClicks() {
        try {
            Log.d(TAG, "🔧 Setup des clics sur cartes...");
            
            if (cardQuran != null) {
                cardQuran.setOnClickListener(v -> {
                    showToast(getString(R.string.opening_quran));
                    try {
                        Intent intent = new Intent(MainActivity.this, QuranActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur ouverture QuranActivity", e);
                        showToast(getString(R.string.error_opening_quran));
                    }
                });
            }

            if (cardPrayerTimes != null) {
                cardPrayerTimes.setOnClickListener(v -> {
                    showToast(getString(R.string.opening_prayer_times));
                    try {
                        Intent intent = new Intent(MainActivity.this, PrayerTimesActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur ouverture PrayerTimesActivity", e);
                        showToast(getString(R.string.error_opening_prayer_times));
                    }
                });
            }

            if (cardCamera != null) {
                cardCamera.setOnClickListener(v -> {
                    showToast(getString(R.string.opening_camera));
                    try {
                        Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur ouverture CameraActivity", e);
                        showToast(getString(R.string.error_opening_camera));
                    }
                });
            }

            if (cardSettings != null) {
                cardSettings.setOnClickListener(v -> {
                    showToast(getString(R.string.opening_settings));
                    try {
                        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur ouverture SettingsActivity", e);
                        showToast(getString(R.string.error_opening_settings));
                    }
                });
            }
            
            Log.d(TAG, "✅ Clics sur cartes configurés");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur setupCardClicks: " + e.getMessage(), e);
        }
    }

    private void setupGemini() {
    try {
        Log.d(TAG, "🔧 Setup de Gemini...");
        
        String apiKey = ConfigReader.getGeminiApiKey();
        Log.d(TAG, "🔑 Clé API: " + (apiKey != null ? "PRÉSENTE" : "NULL"));
        
        // Passer le contexte au client Gemini
        geminiClient = new GeminiBasicClient(MainActivity.this, apiKey);
        
        String status = ConfigReader.hasValidGeminiKey() ? 
            getString(R.string.api_key_detected) : getString(R.string.demo_mode);
        updateStatus(getString(R.string.initialized) + " - " + status);
        
        Log.d(TAG, "✅ Gemini initialisé");
        
    } catch (Exception e) {
        Log.e(TAG, "❌ Erreur setupGemini: " + e.getMessage(), e);
        updateStatus(getString(R.string.initialization_error));
        showToast(getString(R.string.gemini_initialization_error));
    }
}

    private void runConnectionTest() {
        try {
            Log.d(TAG, "🧪 Début test connexion...");
            
            if (textViewResponse != null) {
                textViewResponse.setText(getString(R.string.testing_connection));
            }
            if (buttonSend != null) {
                buttonSend.setEnabled(false);
            }
            
            executor.execute(() -> {
                try {
                    String testResult = geminiClient.testConnection();
                    Log.d(TAG, "📡 Résultat test: " + testResult);
                    
                    mainHandler.post(() -> {
                        try {
                            if (buttonSend != null) {
                                buttonSend.setEnabled(true);
                            }
                            if (textViewResponse != null) {
                                if (testResult.contains("✅")) {
                                    textViewResponse.setText(getString(R.string.connection_success));
                                    updateStatus(getString(R.string.connected));
                                    showToast(getString(R.string.gemini_operational));
                                } else {
                                    textViewResponse.setText(getConfigurationHelp(testResult));
                                    updateStatus(getString(R.string.configuration));
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Erreur dans mainHandler: " + e.getMessage(), e);
                        }
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "❌ Erreur test connexion: " + e.getMessage(), e);
                    mainHandler.post(() -> {
                        if (buttonSend != null) buttonSend.setEnabled(true);
                        if (textViewResponse != null) {
                            textViewResponse.setText(getString(R.string.test_error, e.getMessage()));
                        }
                        updateStatus(getString(R.string.error_demo_mode));
                    });
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur runConnectionTest: " + e.getMessage(), e);
        }
    }

    private void askQuestion() {
        try {
            String question = editTextPrompt != null ? editTextPrompt.getText().toString().trim() : "";
            if (question.isEmpty()) {
                showToast(getString(R.string.toast_enter_question));
                return;
            }

            if (textViewResponse != null) {
                textViewResponse.setText(getString(R.string.status_thinking));
            }
            updateStatus(getString(R.string.in_progress));
            if (buttonSend != null) {
                buttonSend.setEnabled(false);
            }

            executor.execute(() -> {
                try {
                    String response = geminiClient.callGemini(question);
                    Log.d(TAG, "🤖 Réponse Gemini reçue");
                    
                    mainHandler.post(() -> {
                        try {
                            if (textViewResponse != null) {
                                textViewResponse.setText(response);
                            }
                            if (buttonSend != null) {
                                buttonSend.setEnabled(true);
                            }
                            
                            if (response.contains("❌") || response.contains("🔧")) {
                                updateStatus(getString(R.string.demo_mode));
                            } else {
                                updateStatus(getString(R.string.status_response_received));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Erreur affichage réponse: " + e.getMessage(), e);
                        }
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "❌ Erreur askQuestion: " + e.getMessage(), e);
                    mainHandler.post(() -> {
                        if (textViewResponse != null) {
                            textViewResponse.setText(getString(R.string.error_response, e.getMessage()));
                        }
                        if (buttonSend != null) {
                            buttonSend.setEnabled(true);
                        }
                        updateStatus(getString(R.string.error));
                    });
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur askQuestion: " + e.getMessage(), e);
        }
    }

    private String getConfigurationHelp(String error) {
        return getString(R.string.configuration_required) + "\n\n" +
               getString(R.string.error_label) + error + "\n\n" +
               getString(R.string.activation_steps);
    }

    private void updateStatus(String message) {
        mainHandler.post(() -> {
            try {
                if (textViewStatus != null) {
                    textViewStatus.setText(getString(R.string.status_label, message));
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Erreur updateStatus: " + e.getMessage(), e);
            }
        });
    }

    private void showToast(String message) {
        mainHandler.post(() -> {
            try {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "❌ Erreur showToast: " + e.getMessage(), e);
            }
        });
    }

    private void showErrorToast(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "❌ Impossible d'afficher le toast d'erreur: " + e.getMessage());
        }
    }

    private void showErrorScreen(String errorMessage) {
        try {
            setContentView(R.layout.activity_main); // Réessayer le layout normal
            TextView errorView = new TextView(this);
            errorView.setText(getString(R.string.startup_error, errorMessage));
            errorView.setTextSize(16);
            errorView.setPadding(32, 32, 32, 32);
            setContentView(errorView);
        } catch (Exception e) {
            Log.e(TAG, "❌ Impossible d'afficher l'écran d'erreur", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🛑 Destruction de MainActivity");
        try {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur onDestroy: " + e.getMessage(), e);
        }
    }
}