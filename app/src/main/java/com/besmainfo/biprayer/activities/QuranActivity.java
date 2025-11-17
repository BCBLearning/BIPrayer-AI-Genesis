package com.besmainfo.biprayer.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.besmainfo.biprayer.R;

import com.besmainfo.biprayer.services.QuranService;
import com.besmainfo.biprayer.services.QuranService.QuranVerse;
import com.besmainfo.biprayer.utils.LanguageHelper;

import java.util.List;

public class QuranActivity extends AppCompatActivity {
    private static final String TAG = "QuranActivity";
    
    private QuranService quranService;
    private EditText editTextSearch;
    private Button btnSearch, btnDailyVerse, btnRandomVerse, btnThemes;
    private TextView textViewResults, textViewTitle;
    private LinearLayout layoutThemes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 🌍 Appliquer la langue sauvegardée
        LanguageHelper.applySavedLanguage(this);
        
        setContentView(R.layout.activity_quran);
        
        Log.d(TAG, "Démarrage de QuranActivity...");
        
        initializeServices();
        setupViews();
        setupClickListeners();
        
        showDailyVerse();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 🌍 Vérifier les changements de langue à chaque retour
        LanguageHelper.checkAndApplyLanguageChange(this);
    }

    private void initializeServices() {
        quranService = new QuranService();
        Log.d(TAG, "Services Coran initialisés");
    }

    private void setupViews() {
        editTextSearch = findViewById(R.id.editTextSearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnDailyVerse = findViewById(R.id.btnDailyVerse);
        btnRandomVerse = findViewById(R.id.btnRandomVerse);
        btnThemes = findViewById(R.id.btnThemes);
        textViewResults = findViewById(R.id.textViewResults);
        textViewTitle = findViewById(R.id.textViewTitle);
        layoutThemes = findViewById(R.id.layoutThemes);
        
        // Configurer les hints avec les ressources
        editTextSearch.setHint(R.string.hint_search_quran);
    }

    private void setupClickListeners() {
        btnSearch.setOnClickListener(v -> searchVerses());
        btnDailyVerse.setOnClickListener(v -> showDailyVerse());
        btnRandomVerse.setOnClickListener(v -> showRandomVerse());
        btnThemes.setOnClickListener(v -> toggleThemesView());
        
        editTextSearch.setOnEditorActionListener((v, actionId, event) -> {
            searchVerses();
            return true;
        });
    }

    private void searchVerses() {
        String query = editTextSearch.getText().toString().trim();
        if (query.isEmpty()) {
            showToast(getString(R.string.enter_search_query));
            return;
        }

        try {
            List<QuranVerse> results = quranService.searchByKeyword(query);
            if (results.isEmpty()) {
                results = quranService.searchByTheme(query);
            }

            displayResults(results, getString(R.string.search_results_for, query));
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur recherche: " + e.getMessage(), e);
            showToast(getString(R.string.search_error));
        }
    }

    private void showDailyVerse() {
        try {
            QuranVerse dailyVerse = quranService.getVerseOfTheDay();
            String tafsir = quranService.getTafsir(dailyVerse.getReference());
            
            String result = getString(R.string.daily_verse_header) + "\n\n" +
                           dailyVerse.toFormattedString() + "\n" +
                           tafsir + "\n\n" +
                           getString(R.string.daily_verse_footer);
            
            textViewResults.setText(result);
            textViewTitle.setText(getString(R.string.daily_verse_title));
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur verset du jour: " + e.getMessage(), e);
            showToast(getString(R.string.daily_verse_error));
        }
    }

    private void showRandomVerse() {
        try {
            QuranVerse randomVerse = quranService.getRandomVerse();
            String tafsir = quranService.getTafsir(randomVerse.getReference());
            
            String result = getString(R.string.random_verse_header) + "\n\n" +
                           randomVerse.toFormattedString() + "\n" +
                           tafsir + "\n\n" +
                           getString(R.string.random_verse_footer);
            
            textViewResults.setText(result);
            textViewTitle.setText(getString(R.string.random_verse_title));
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur verset aléatoire: " + e.getMessage(), e);
            showToast(getString(R.string.random_verse_error));
        }
    }

    private void toggleThemesView() {
        if (layoutThemes.getVisibility() == View.VISIBLE) {
            layoutThemes.setVisibility(View.GONE);
        } else {
            showThemes();
            layoutThemes.setVisibility(View.VISIBLE);
        }
    }

    private void showThemes() {
        layoutThemes.removeAllViews();
        
        String[] themes = {
            getString(R.string.theme_patience),
            getString(R.string.theme_prayer), 
            getString(R.string.theme_gratitude),
            getString(R.string.theme_hope),
            getString(R.string.theme_wisdom),
            getString(R.string.theme_forgiveness)
        };
        
        for (String theme : themes) {
            Button themeBtn = new Button(this);
            themeBtn.setText(theme);
            themeBtn.setBackgroundResource(R.drawable.button_primary);
            themeBtn.setTextColor(getColor(android.R.color.white));
            themeBtn.setPadding(32, 16, 32, 16);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 8, 0, 8);
            themeBtn.setLayoutParams(params);
            
            themeBtn.setOnClickListener(v -> searchByTheme(theme));
            
            layoutThemes.addView(themeBtn);
        }
    }

    private void searchByTheme(String theme) {
        try {
            List<QuranVerse> results = quranService.searchByTheme(theme.toLowerCase());
            displayResults(results, getString(R.string.theme_results, theme));
            layoutThemes.setVisibility(View.GONE);
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur recherche thème: " + e.getMessage(), e);
            showToast(getString(R.string.theme_search_error));
        }
    }

    private void displayResults(List<QuranVerse> verses, String title) {
        if (verses.isEmpty()) {
            textViewResults.setText(getString(R.string.no_verses_found));
            return;
        }

        StringBuilder result = new StringBuilder();
        result.append("🔍 ").append(title).append("\n\n");
        
        for (int i = 0; i < verses.size(); i++) {
            QuranVerse verse = verses.get(i);
            result.append(verse.toFormattedString());
            
            if (i < verses.size() - 1) {
                result.append("\n━━━━━━━━━━━━━━━━━━━━\n\n");
            }
        }
        
        if (!verses.isEmpty()) {
            String tafsir = quranService.getTafsir(verses.get(0).getReference());
            result.append("\n").append(tafsir);
        }
        
        textViewResults.setText(result.toString());
        textViewTitle.setText(getString(R.string.results_title));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "QuranActivity détruite");
    }
}