package com.besmainfo.biprayer.ai;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.besmainfo.biprayer.R;

public class OpusClient {
    private static final String TAG = "OpusClient";
    private final String apiKey;
    private final Context context;

    public OpusClient(Context context, String apiKey) {
        this.context = context;
        this.apiKey = apiKey;
        Log.d(TAG, "OpusClient initialisé - Mode: " + (isConfigured() ? "RÉEL" : "DÉMO"));
    }

    /**
     * Synthèse vocale complète avec voix et langue - MODE RÉEL
     */
    public String synthesizeSpeech(String text, String voice, String language) {
        if (!isConfigured()) {
            Log.e(TAG, "Opus non configuré - Mode démo");
            return context.getString(R.string.opus_not_configured);
        }

        if (text == null || text.trim().isEmpty()) {
            return context.getString(R.string.opus_no_text);
        }

        Log.d(TAG, "🚀 Synthèse vocale réelle - Texte: " + text.substring(0, Math.min(50, text.length())) + "...");

        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://api.opus.ai/v1/synthesize");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);

            // Construction de la requête complète
            JSONObject requestBody = new JSONObject();
            requestBody.put("text", text);
            requestBody.put("voice", voice);
            requestBody.put("language", language);
            requestBody.put("output_format", "mp3");
            requestBody.put("speed", 1.0);
            requestBody.put("pitch", 1.0);
            requestBody.put("volume", 1.0);

            // Configuration audio avancée
            JSONObject audioConfig = new JSONObject();
            audioConfig.put("sample_rate", 22050);
            audioConfig.put("bit_depth", 16);
            audioConfig.put("channels", 1);
            requestBody.put("audio_config", audioConfig);

            String jsonInput = requestBody.toString();
            Log.d(TAG, "📤 Envoi requête synthèse vocale");

            // Envoi de la requête
            OutputStream os = conn.getOutputStream();
            os.write(jsonInput.getBytes("utf-8"));
            os.flush();
            os.close();

            // Lecture de la réponse
            int responseCode = conn.getResponseCode();
            Log.d(TAG, "📥 Réponse code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8")
                );
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line.trim());
                }
                reader.close();

                String result = parseOpusResponse(response.toString());
                Log.d(TAG, "✅ Synthèse vocale réussie");
                return result;
                
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                return context.getString(R.string.opus_unauthorized);
            } else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                return context.getString(R.string.opus_bad_request);
            } else {
                // Lecture de l'erreur détaillée
                BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), "utf-8")
                );
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;
                
                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine.trim());
                }
                errorReader.close();
                
                Log.e(TAG, "❌ Erreur API Opus: " + errorResponse.toString());
                return context.getString(R.string.opus_api_error) + " (" + responseCode + "): " + getOpusErrorMessage(responseCode);
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Exception synthèse vocale", e);
            return context.getString(R.string.opus_connection_error) + e.getMessage();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Synthèse vocale simplifiée (surcharge)
     */
    public String synthesizeSpeech(String text) {
        return synthesizeSpeech(text, "en-US-Standard-B", "en");
    }

    /**
     * Test de connexion à l'API Opus - MODE RÉEL
     */
    public String testConnection() {
        if (!isConfigured()) {
            return context.getString(R.string.opus_not_configured);
        }

        Log.d(TAG, "🧪 Test connexion Opus API");

        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://api.opus.ai/v1/voices");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(15000);

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "📥 Test connexion - Code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8")
                );
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line.trim());
                }
                reader.close();

                // Analyser la réponse pour extraire le nombre de voix
                try {
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    int voiceCount = jsonResponse.optJSONArray("voices").length();
                    return "✅ " + context.getString(R.string.opus_connection_success) + " (" + voiceCount + " voix disponibles)";
                } catch (Exception e) {
                    return "✅ " + context.getString(R.string.opus_connection_success);
                }
            } else {
                return context.getString(R.string.opus_connection_failed) + responseCode;
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Test connexion échoué", e);
            return context.getString(R.string.opus_connection_test_failed) + e.getMessage();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String parseOpusResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            
            if (jsonObject.has("audio_url")) {
                String audioUrl = jsonObject.getString("audio_url");
                return "✅ " + context.getString(R.string.opus_audio_generated) + "\n🔗 " + audioUrl;
            } 
            else if (jsonObject.has("audio_data")) {
                String audioData = jsonObject.getString("audio_data");
                return "✅ " + context.getString(R.string.opus_audio_generated_base64) + " (" + audioData.length() + " caractères)";
            }
            else if (jsonObject.has("id")) {
                String jobId = jsonObject.getString("id");
                return "✅ " + context.getString(R.string.opus_job_created) + jobId;
            }
            else if (jsonObject.has("status") && jsonObject.getString("status").equals("completed")) {
                return "✅ " + context.getString(R.string.opus_audio_completed);
            }
            else {
                return "✅ " + context.getString(R.string.opus_audio_generated) + jsonResponse;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur parsing réponse", e);
            return "✅ " + context.getString(R.string.opus_audio_completed) + jsonResponse;
        }
    }

    private String getOpusErrorMessage(int responseCode) {
        switch (responseCode) {
            case 400: return context.getString(R.string.opus_error_400);
            case 401: return context.getString(R.string.opus_error_401);
            case 403: return context.getString(R.string.opus_error_403);
            case 404: return context.getString(R.string.opus_error_404);
            case 429: return context.getString(R.string.opus_error_429);
            case 500: return context.getString(R.string.opus_error_500);
            case 503: return context.getString(R.string.opus_error_503);
            default: return context.getString(R.string.opus_error_unknown);
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("") && !apiKey.contains("your_actual_");
    }

    /**
     * Obtenir la voix recommandée pour une langue
     */
    public String getRecommendedVoice(String language) {
        switch (language.toLowerCase()) {
            case "ar": return "ar-XA-Standard-A";
            case "fr": return "fr-FR-Standard-A";
            case "en": return "en-US-Standard-B";
            case "es": return "es-ES-Standard-A";
            case "de": return "de-DE-Standard-A";
            default: return "en-US-Standard-B";
        }
    }

    public String getCurrentMode() {
        return isConfigured() ? "RÉEL (Opus API)" : "DÉMO";
    }
}