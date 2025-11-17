package com.besmainfo.biprayer.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.besmainfo.biprayer.R;

import com.besmainfo.biprayer.utils.LanguageHelper;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 🌍 Appliquer la langue sauvegardée
        LanguageHelper.applySavedLanguage(this);
        
        setContentView(R.layout.activity_help);
        
        TextView textViewHelp = findViewById(R.id.textViewHelp);
        textViewHelp.setMovementMethod(LinkMovementMethod.getInstance());
        
        // 🌍 Construction du texte d'aide multilingue
        StringBuilder helpBuilder = new StringBuilder();
        
        helpBuilder.append(getString(R.string.help_title_main)).append("\n\n");
        helpBuilder.append(getString(R.string.help_features_title)).append("\n\n");
        helpBuilder.append(getString(R.string.help_features_content)).append("\n\n");
        helpBuilder.append(getString(R.string.help_config_title)).append("\n\n");
        helpBuilder.append(getString(R.string.help_config_content)).append("\n\n");
        helpBuilder.append(getString(R.string.help_troubleshooting_title)).append("\n\n");
        helpBuilder.append(getString(R.string.help_troubleshooting_content)).append("\n\n");
        helpBuilder.append(getString(R.string.help_languages_title)).append("\n\n");
        helpBuilder.append(getString(R.string.help_languages_content)).append("\n\n");
        helpBuilder.append(getString(R.string.help_support_title)).append("\n\n");
        helpBuilder.append(getString(R.string.help_support_content)).append("\n\n");
        helpBuilder.append(getString(R.string.help_footer));
        
        textViewHelp.setText(helpBuilder.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 🌍 Vérifier les changements de langue à chaque retour
        LanguageHelper.checkAndApplyLanguageChange(this);
    }
}