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
import java.util.ArrayList;
import java.util.List;

import com.besmainfo.biprayer.R;

public class QdrantClient {
    private static final String TAG = "QdrantClient";
    private final String baseUrl;
    private final String apiKey;
    private final Context context;

    public QdrantClient(Context context, String baseUrl, String apiKey) {
        this.context = context;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        Log.d(TAG, "QdrantClient initialisé - Mode: " + (isConfigured() ? "RÉEL" : "DÉMO") + " - URL: " + baseUrl);
    }

    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isEmpty() && !baseUrl.equals("") && !baseUrl.contains("your_actual_");
    }

    /**
     * Recherche vectorielle - MODE RÉEL
     */
    public List<SearchResult> search(String collectionName, float[] vector, int limit) {
        List<SearchResult> results = new ArrayList<>();
        
        if (!isConfigured()) {
            Log.e(TAG, "Qdrant non configuré - Mode démo");
            results.add(new SearchResult(context.getString(R.string.qdrant_not_configured), 0.0f));
            return results;
        }

        Log.d(TAG, "🚀 Recherche vectorielle réelle - Collection: " + collectionName + ", Vecteur: " + vector.length + " dimensions");

        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/collections/" + collectionName + "/points/search");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            
            if (apiKey != null && !apiKey.isEmpty()) {
                conn.setRequestProperty("api-key", apiKey);
            }
            
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            conn.setDoOutput(true);

            // Construction de la requête de recherche
            JSONObject requestBody = new JSONObject();
            JSONArray vectorArray = new JSONArray();
            for (float v : vector) {
                vectorArray.put(v);
            }
            
            requestBody.put("vector", vectorArray);
            requestBody.put("limit", limit);
            requestBody.put("with_payload", true);
            requestBody.put("with_vector", false);
            requestBody.put("score_threshold", 0.3); // Seuil de pertinence

            String jsonInput = requestBody.toString();
            Log.d(TAG, "📤 Envoi requête recherche");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

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

                List<SearchResult> searchResults = parseSearchResults(response.toString());
                Log.d(TAG, "✅ Recherche réussie - " + searchResults.size() + " résultats");
                return searchResults;
                
            } else {
                Log.e(TAG, "❌ Erreur recherche: " + responseCode);
                results.add(new SearchResult(context.getString(R.string.qdrant_search_error) + responseCode, 0.0f));
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Exception recherche", e);
            results.add(new SearchResult(context.getString(R.string.qdrant_error) + e.getMessage(), 0.0f));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        
        return results;
    }

    /**
     * Création de collection - MODE RÉEL
     */
    public boolean createCollection(String collectionName, int vectorSize) {
        if (!isConfigured()) {
            Log.e(TAG, "Qdrant non configuré");
            return false;
        }

        Log.d(TAG, "🚀 Création collection: " + collectionName + " (" + vectorSize + " dimensions)");

        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/collections/" + collectionName);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");
            
            if (apiKey != null && !apiKey.isEmpty()) {
                conn.setRequestProperty("api-key", apiKey);
            }
            
            conn.setDoOutput(true);

            // Configuration de la collection
            JSONObject config = new JSONObject();
            JSONObject params = new JSONObject();
            JSONObject vectors = new JSONObject();
            
            vectors.put("size", vectorSize);
            vectors.put("distance", "Cosine"); // Meilleure pour la sémantique
            params.put("vectors", vectors);
            
            // Optimisations pour la recherche
            JSONObject optimizersConfig = new JSONObject();
            optimizersConfig.put("default_segment_number", 2);
            params.put("optimizers_config", optimizersConfig);
            
            config.put("params", params);

            String jsonInput = config.toString();

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "📥 Création collection - Code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "✅ Collection créée: " + collectionName);
                return true;
            } else if (responseCode == 400) {
                Log.w(TAG, "⚠️ Collection existe déjà: " + collectionName);
                return true; // La collection existe déjà
            } else {
                Log.e(TAG, "❌ Erreur création collection: " + responseCode);
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Exception création collection", e);
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Test de connexion - MODE RÉEL
     */
    public String testConnection() {
        if (!isConfigured()) {
            return context.getString(R.string.qdrant_not_configured);
        }

        Log.d(TAG, "🧪 Test connexion Qdrant");

        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/collections");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            
            if (apiKey != null && !apiKey.isEmpty()) {
                conn.setRequestProperty("api-key", apiKey);
            }
            
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

                // Analyser la réponse pour compter les collections
                try {
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONObject result = jsonResponse.getJSONObject("result");
                    JSONArray collections = result.getJSONArray("collections");
                    return "✅ Connexion Qdrant réussie (" + collections.length() + " collections)";
                } catch (Exception e) {
                    return "✅ Connexion Qdrant réussie";
                }
            } else {
                return "❌ Erreur connexion Qdrant: " + responseCode;
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Test connexion échoué", e);
            return "❌ Erreur connexion Qdrant: " + e.getMessage();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private List<SearchResult> parseSearchResults(String jsonResponse) {
        List<SearchResult> results = new ArrayList<>();
        
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject result = jsonObject.getJSONObject("result");
            JSONArray resultArray = result.getJSONArray("result");
            
            Log.d(TAG, "📊 " + resultArray.length() + " résultats trouvés");
            
            for (int i = 0; i < resultArray.length(); i++) {
                JSONObject searchResult = resultArray.getJSONObject(i);
                JSONObject payload = searchResult.getJSONObject("payload");
                float score = (float) searchResult.getDouble("score");
                
                String text = payload.optString("text", "No text");
                String id = searchResult.optString("id", "unknown");
                
                Log.d(TAG, "🔍 Résultat " + i + ": Score=" + score + ", ID=" + id);
                results.add(new SearchResult(text, score));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur parsing résultats", e);
            results.add(new SearchResult("Error parsing: " + e.getMessage(), 0.0f));
        }
        
        return results;
    }

    public static class SearchResult {
        public final String text;
        public final float score;

        public SearchResult(String text, float score) {
            this.text = text;
            this.score = score;
        }

        @Override
        public String toString() {
            return String.format("[%.2f] %s", score, text);
        }
    }

    public String getCurrentMode() {
        return isConfigured() ? "RÉEL (Qdrant API)" : "DÉMO";
    }
}