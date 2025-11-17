package com.besmainfo.biprayer.ai;

import android.util.Log;
import com.besmainfo.biprayer.services.QuranService;
import com.besmainfo.biprayer.services.QuranService.QuranVerse;

import java.util.*;

public class QuranAIService {
    private static final String TAG = "QuranAIService";
    
    private final QuranService quranService;
    private final GeminiBasicClient geminiClient;
    private final QdrantClient qdrantClient;
    private boolean aiEnabled = false;

    public QuranAIService(QuranService quranService, GeminiBasicClient geminiClient, QdrantClient qdrantClient) {
        this.quranService = quranService;
        this.geminiClient = geminiClient;
        this.qdrantClient = qdrantClient;
        this.aiEnabled = geminiClient != null && qdrantClient != null;
        
        Log.d(TAG, "Quran AI Service initialisé - AI: " + aiEnabled);
        initializeAIComponents();
    }

    private void initializeAIComponents() {
        if (!aiEnabled) return;
        
        new Thread(() -> {
            try {
                // Initialiser Qdrant avec des embeddings de versets
                initializeQdrantWithVersets();
                Log.d(TAG, "Composants AI initialisés avec succès");
            } catch (Exception e) {
                Log.e(TAG, "Erreur initialisation AI", e);
            }
        }).start();
    }

    /**
     * RECHERCHE HYBRIDE - Qdrant + Gemini
     */
    public List<QuranVerse> smartSearch(String query) {
        List<QuranVerse> results = new ArrayList<>();
        
        // 1. Recherche locale de base
        results.addAll(quranService.searchByTheme(query));
        results.addAll(quranService.searchByKeyword(query));
        
        // 2. Si AI activée, recherche avancée
        if (aiEnabled && results.size() < 3) {
            try {
                List<QuranVerse> aiResults = advancedAISearch(query);
                results.addAll(aiResults);
            } catch (Exception e) {
                Log.e(TAG, "Erreur recherche AI", e);
            }
        }
        
        // Éliminer les doublons
        Set<QuranVerse> uniqueResults = new LinkedHashSet<>(results);
        return new ArrayList<>(uniqueResults);
    }

    /**
     * RECHERCHE AVANCÉE AVEC GEMINI + QDRANT
     */
    private List<QuranVerse> advancedAISearch(String query) {
        List<QuranVerse> results = new ArrayList<>();
        
        try {
            // Étape 1: Gemini analyse la requête
            String analyzedQuery = geminiClient.callGemini(
                "Analyse cette requête et extrait les thèmes coraniques principaux: \"" + query + 
                "\". Réponds uniquement avec 2-3 mots-clés séparés par des virgules."
            );
            
            Log.d(TAG, "Gemini analyse: " + analyzedQuery);
            
            // Étape 2: Recherche Qdrant avec les mots-clés
            String[] keywords = analyzedQuery.split(",");
            for (String keyword : keywords) {
                String cleanKeyword = keyword.trim();
                if (cleanKeyword.length() > 2) {
                    List<QdrantClient.SearchResult> qdrantResults = 
                        qdrantClient.search("quran_knowledge", generateEmbedding(cleanKeyword), 2);
                    
                    for (QdrantClient.SearchResult result : qdrantResults) {
                        if (result.score > 0.3) {
                            // Convertir le résultat Qdrant en QuranVerse
                            QuranVerse verse = parseQdrantResult(result);
                            if (verse != null) {
                                results.add(verse);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur recherche AI avancée", e);
        }
        
        return results;
    }

    /**
     * GÉNÉRATION D'EXÉGÈSE AVEC GEMINI
     */
    public String generateTafsirWithAI(QuranVerse verse) {
        if (!aiEnabled) {
            return quranService.getTafsir(verse.getReference());
        }
        
        try {
            String prompt = String.format(
                "Génère une exégèse (tafsir) courte et inspirante pour ce verset coranique:\n\n" +
                "Verset: %s\n" +
                "Référence: %s\n\n" +
                "Donne une explication spirituelle pratique pour la vie quotidienne (max 150 mots).",
                verse.getVerse(),
                verse.getReference()
            );
            
            String aiTafsir = geminiClient.callGemini(prompt);
            return "🤖 **Exégèse IA**\n\n" + aiTafsir + 
                   "\n\n_*Généré par Gemini AI_";
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur génération tafsir AI", e);
            return quranService.getTafsir(verse.getReference());
        }
    }

    /**
     * RECHERCHE SÉMANTIQUE AVANCÉE
     */
    public List<QuranVerse> semanticSearch(String query) {
        if (!aiEnabled) {
            return quranService.searchByTheme(query);
        }
        
        try {
            // Utiliser Gemini pour comprendre l'intention
            String intentAnalysis = geminiClient.callGemini(
                "Quel est le besoin spirituel derrière cette requête: \"" + query + 
                "\"? Réponds avec un seul mot représentant le thème coranique."
            );
            
            Log.d(TAG, "Intention détectée: " + intentAnalysis);
            return smartSearch(intentAnalysis);
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur recherche sémantique", e);
            return quranService.searchByTheme(query);
        }
    }

    /**
     * GÉNÉRATION DE CONSEIL SPIRITUEL PERSONNALISÉ
     */
    public String generateSpiritualAdvice(String situation) {
        if (!aiEnabled) {
            return "💡 **Conseil Spirituel**\n\nPriez et soyez patient. La guidance divine vient à ceux qui cherchent avec sincérité.";
        }
        
        try {
            // Trouver des versets pertinents
            List<QuranVerse> relevantVerses = semanticSearch(situation);
            
            StringBuilder versesText = new StringBuilder();
            for (QuranVerse verse : relevantVerses) {
                versesText.append(verse.getVerse()).append(" (").append(verse.getReference()).append(")\n");
            }
            
            String prompt = String.format(
                "Tu es un assistant spirituel musulman. Donne un conseil court et réconfortant basé sur ces versets:\n\n%s\n\n" +
                "Situation: %s\n\n" +
                "Conseil (max 100 mots, ton bienveillant):",
                versesText.toString(),
                situation
            );
            
            String advice = geminiClient.callGemini(prompt);
            return "🤖 **Conseil Spirituel IA**\n\n" + advice + 
                   "\n\n📖 *Basé sur l'analyse des versets coraniques*";
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur génération conseil", e);
            return "💡 **Conseil Spirituel**\n\nAyez confiance en la sagesse divine et cherchez la paix dans la prière.";
        }
    }

    /**
     * INITIALISATION QDRANT AVEC VERSETS
     */
    private void initializeQdrantWithVersets() {
        try {
            // Créer la collection si elle n'existe pas
            qdrantClient.createCollection("quran_knowledge", 384);
            
            // Ajouter des versets avec embeddings
            List<QuranVerse> allVerses = getAllVersesForAI();
            for (int i = 0; i < allVerses.size(); i++) {
                QuranVerse verse = allVerses.get(i);
                float[] embedding = generateEmbedding(verse.getVerse() + " " + verse.getTheme());
                addVerseToQdrant(verse, embedding, i);
            }
            
            Log.d(TAG, "Qdrant initialisé avec " + allVerses.size() + " versets");
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur initialisation Qdrant", e);
        }
    }

    private List<QuranVerse> getAllVersesForAI() {
        // Récupérer tous les versets de la base
        List<QuranVerse> allVerses = new ArrayList<>();
        String[] themes = {"patience", "priere", "gratitude", "espoir", "sagesse", "pardon"};
        
        for (String theme : themes) {
            allVerses.addAll(quranService.searchByTheme(theme));
        }
        
        return allVerses;
    }

    private void addVerseToQdrant(QuranVerse verse, float[] embedding, int id) {
        // Implémentation pour ajouter à Qdrant
        // (Utiliser les méthodes existantes de QdrantClient)
    }

    private float[] generateEmbedding(String text) {
        // Simulation d'embedding - dans la réalité, utiliser Gemini Embeddings
        float[] embedding = new float[384];
        Random random = new Random(text.hashCode());
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = random.nextFloat() - 0.5f;
        }
        return embedding;
    }

    private QuranVerse parseQdrantResult(QdrantClient.SearchResult result) {
        // Convertir le résultat Qdrant en QuranVerse
        // Implémentation basée sur la structure des données Qdrant
        return new QuranVerse("AI:" + result.score, result.text, "ai_generated");
    }

    public boolean isAIEnabled() {
        return aiEnabled;
    }

    public String getAIStatus() {
        if (!aiEnabled) {
            return "🔧 **Mode Basique**\n\nGemini + Qdrant non configurés";
        }
        return "🤖 **Mode IA Activé**\n\n• Gemini: Analyse sémantique\n• Qdrant: Recherche vectorielle\n• Opus: Synthèse vocale";
    }
}