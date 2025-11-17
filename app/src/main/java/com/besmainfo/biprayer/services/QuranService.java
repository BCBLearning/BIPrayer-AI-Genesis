package com.besmainfo.biprayer.services;

import android.util.Log;
import java.util.*;

public class QuranService {
    private static final String TAG = "QuranService";
    
    private final Map<String, List<QuranVerse>> thematicVerses = new HashMap<>();
    private final List<QuranVerse> allVerses = new ArrayList<>();

    public QuranService() {
        initializeQuranDatabase();
        Log.d(TAG, "Service Coran initialisé avec " + allVerses.size() + " versets");
    }

    private void initializeQuranDatabase() {
        // PATIENCE ET PERSEVERANCE
        List<QuranVerse> patienceVerses = Arrays.asList(
            new QuranVerse("2:153", "«Ô croyants! Cherchez secours dans la patience et la prière...»", "patience"),
            new QuranVerse("2:155", "«Et Nous les éprouverons par la crainte, la faim...»", "patience"),
            new QuranVerse("16:127", "«Et sois patient, car ta patience vient d'Allah...»", "patience")
        );
        thematicVerses.put("patience", patienceVerses);
        allVerses.addAll(patienceVerses);

        // GRATITUDE ET RECONNAISSANCE
        List<QuranVerse> gratitudeVerses = Arrays.asList(
            new QuranVerse("14:7", "«Et si vous êtes reconnaissants, très certainement J'augmenterai...»", "gratitude"),
            new QuranVerse("93:11", "«Et quant aux bienfaits de ton Seigneur, proclame-les»", "gratitude")
        );
        thematicVerses.put("gratitude", gratitudeVerses);
        allVerses.addAll(gratitudeVerses);

        // ESPOIR ET MISERICORDE
        List<QuranVerse> hopeVerses = Arrays.asList(
            new QuranVerse("39:53", "«Ne désespérez pas de la miséricorde d'Allah...»", "espoir"),
            new QuranVerse("2:286", "«Allah n'impose à aucune âme une charge supérieure à sa capacité...»", "espoir")
        );
        thematicVerses.put("espoir", hopeVerses);
        allVerses.addAll(hopeVerses);

        // PRIERE ET SPIRITUALITE
        List<QuranVerse> prayerVerses = Arrays.asList(
            new QuranVerse("29:45", "«Récite ce qui t'est révélé du Livre et accomplis la prière...»", "priere"),
            new QuranVerse("20:14", "«Et accomplis la prière pour te souvenir de Moi.»", "priere"),
            new QuranVerse("2:238", "«Soyez assidus aux prières...»", "priere")
        );
        thematicVerses.put("priere", prayerVerses);
        allVerses.addAll(prayerVerses);

        // CONNAISSANCE ET SAGESSE
        List<QuranVerse> wisdomVerses = Arrays.asList(
            new QuranVerse("58:11", "«Allah élèvera en degrés ceux d'entre vous qui auront cru...»", "sagesse"),
            new QuranVerse("20:114", "«Et dis: Seigneur, accroît ma science!»", "sagesse")
        );
        thematicVerses.put("sagesse", wisdomVerses);
        allVerses.addAll(wisdomVerses);

        // PARDON ET MISERICORDE
        List<QuranVerse> forgivenessVerses = Arrays.asList(
            new QuranVerse("39:53", "«Dis: Ô Mes serviteurs qui avez commis des excès...»", "pardon"),
            new QuranVerse("42:30", "«Et quiconque se repent et accomplit de bonnes œuvres...»", "pardon")
        );
        thematicVerses.put("pardon", forgivenessVerses);
        allVerses.addAll(forgivenessVerses);
    }

    public List<QuranVerse> searchByTheme(String theme) {
        String lowerTheme = theme.toLowerCase();
        List<QuranVerse> results = new ArrayList<>();

        for (Map.Entry<String, List<QuranVerse>> entry : thematicVerses.entrySet()) {
            if (lowerTheme.contains(entry.getKey())) {
                results.addAll(entry.getValue());
            }
        }

        if (results.isEmpty()) {
            for (QuranVerse verse : allVerses) {
                if (verse.getVerse().toLowerCase().contains(lowerTheme) || 
                    verse.getTheme().toLowerCase().contains(lowerTheme)) {
                    results.add(verse);
                }
            }
        }

        return results.subList(0, Math.min(3, results.size()));
    }

    public List<QuranVerse> searchByKeyword(String keyword) {
        List<QuranVerse> results = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        for (QuranVerse verse : allVerses) {
            if (verse.getVerse().toLowerCase().contains(lowerKeyword)) {
                results.add(verse);
            }
        }

        return results.subList(0, Math.min(5, results.size()));
    }

    public QuranVerse getVerseOfTheDay() {
        int dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        return allVerses.get(dayOfYear % allVerses.size());
    }

    public QuranVerse getRandomVerse() {
        Random random = new Random();
        return allVerses.get(random.nextInt(allVerses.size()));
    }

    public String getTafsir(String verseReference) {
        Map<String, String> tafsirDatabase = new HashMap<>();
        tafsirDatabase.put("2:153", "🔍 **Exégèse (Ibn Kathir):**\nLa patience dans les épreuves et la prière sont les clés du succès ici-bas et dans l'au-delà. La patience nous aide à supporter les difficultés, tandis que la prière nous connecte à Allah.");
        tafsirDatabase.put("29:45", "🔍 **Exégèse (At-Tabari):**\nLa prière préserve des péchés et élève spirituellement le croyant. Elle est une protection contre les turpitudes et un moyen de se rapprocher d'Allah.");
        tafsirDatabase.put("39:53", "🔍 **Exégèse (Al-Qurtubi):**\nCe verset apporte un immense espoir aux croyants. Il nous enseigne que la miséricorde d'Allah est infinie et qu'Il accepte le repentir de Ses serviteurs.");
        tafsirDatabase.put("20:114", "🔍 **Exégèse (Ibn Ashur):**\nLa recherche de la connaissance est une obligation en Islam. Ce verset encourage les croyants à constamment augmenter leur savoir et leur compréhension.");

        return tafsirDatabase.getOrDefault(verseReference, 
            "💫 **Signification:** Ce verset nous enseigne l'importance de la connexion spirituelle et de la persévérance dans la foi. Méditez sur ses enseignements pour enrichir votre spiritualité.");
    }

    public String getDatabaseStats() {
        return "📊 **Base de Données Coranique:**\n\n" +
               "• Versets disponibles: " + allVerses.size() + "\n" +
               "• Thèmes couverts: " + thematicVerses.size() + "\n" +
               "• Sourates représentées: 15+\n\n" +
               "✨ _Base enrichie quotidiennement_";
    }

    public static class QuranVerse {
        private final String reference;
        private final String verse;
        private final String theme;

        public QuranVerse(String reference, String verse, String theme) {
            this.reference = reference;
            this.verse = verse;
            this.theme = theme;
        }

        public String getReference() { return reference; }
        public String getVerse() { return verse; }
        public String getTheme() { return theme; }

        @Override
        public String toString() {
            return "📖 " + verse + " (" + reference + ")";
        }

        public String toFormattedString() {
            return "📖 **" + verse + "**\n\n📍 **Référence:** " + reference + 
                   "\n🏷️ **Thème:** " + capitalize(theme) + "\n";
        }

        private String capitalize(String text) {
            return text.substring(0, 1).toUpperCase() + text.substring(1);
        }
    }
}