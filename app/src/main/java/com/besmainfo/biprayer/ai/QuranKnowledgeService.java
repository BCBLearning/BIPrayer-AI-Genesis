package com.besmainfo.biprayer.ai;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class QuranKnowledgeService {
    private static final String TAG = "QuranKnowledge";
    private final QdrantClient qdrantClient;
    private boolean isInitialized = false;

    public QuranKnowledgeService(QdrantClient qdrantClient) {
        this.qdrantClient = qdrantClient;
        initializeKnowledgeBase();
    }

    private void initializeKnowledgeBase() {
        new Thread(() -> {
            try {
                if (qdrantClient.isConfigured()) {
                    qdrantClient.createCollection("quran_knowledge", 384);
                    addSampleQuranData();
                    isInitialized = true;
                    Log.d(TAG, "Base Coran initialisée");
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur init base", e);
            }
        }).start();
    }

    public List<String> searchQuranWisdom(String query) {
        List<String> results = new ArrayList<>();

        if (!qdrantClient.isConfigured()) {
            return getFallbackVerses(query);
        }

        try {
            float[] queryVector = generateEmbeddingFromQuery(query);

            List<QdrantClient.SearchResult> searchResults =
                    qdrantClient.search("quran_knowledge", queryVector, 3);

            if (searchResults == null || searchResults.isEmpty() || searchResults.get(0).score < 0.1f) {
                return getFallbackVerses(query);
            }

            for (QdrantClient.SearchResult result : searchResults) {
                if (result.score > 0.3f) {
                    results.add("📖 " + result.text);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur recherche", e);
            results = getFallbackVerses(query);
        }

        return results;
    }

    private List<String> getFallbackVerses(String query) {
        List<String> verses = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        if (lowerQuery.contains("prière") || lowerQuery.contains("salah")) {
            verses.add("📖 «Récite ce qui t'est révélé du Livre et accomplis la prière...» (29:45)");
            verses.add("📖 «La prière préserve de la turpitude et du blâmable...»");
        }
        else if (lowerQuery.contains("patience") || lowerQuery.contains("sabr")) {
            verses.add("📖 «Ô croyants! Cherchez secours dans la patience et la prière...» (2:153)");
        }
        else if (lowerQuery.contains("guidance") || lowerQuery.contains("hidaya")) {
            verses.add("📖 «Guide-nous dans le droit chemin...» (1:6)");
        }
        else {
            verses.add("📖 «Et invoque ton Seigneur en toi-même, avec humilité et crainte...» (7:205)");
            verses.add("📖 «La récitation du Coran est une lumière...»");
        }

        return verses;
    }

    private void addSampleQuranData() {
        String[] verses = {
                "«Ô croyants! Cherchez secours dans la patience et la prière...» (2:153)",
                "«Récite ce qui t'est révélé du Livre et accomplis la prière...» (29:45)",
                "«Et invoque ton Seigneur en toi-même, avec humilité et crainte...» (7:205)",
                "«La récitation du Coran est une lumière...»",
                "«Les anges descendent pendant la nuit du destin...» (97:4)",
                "«Certes, la prière est une prescription...» (4:103)",
                "«Et cherchez secours dans l'endurance et la prière...» (2:45)",
                "«Garde la prière, car la prière préserve... »"
        };

        for (int i = 0; i < verses.length; i++) {
            addVerseToKnowledgeBase(verses[i], i);
        }
    }

    private void addVerseToKnowledgeBase(String verse, int id) {
        try {
            float[] embedding = generateEmbeddingFromQuery(verse);
        } catch (Exception e) {
            Log.e(TAG, "Erreur ajout verset", e);
        }
    }

    private float[] generateEmbeddingFromQuery(String text) {
        float[] embedding = new float[384];
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = (float) Math.random() - 0.5f;
        }
        return embedding;
    }

    public boolean isReady() {
        return qdrantClient.isConfigured() && isInitialized;
    }
}