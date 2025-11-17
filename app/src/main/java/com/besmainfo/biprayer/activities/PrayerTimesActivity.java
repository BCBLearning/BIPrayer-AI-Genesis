package com.besmainfo.biprayer.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.besmainfo.biprayer.R;
import com.besmainfo.biprayer.services.PrayerTimesService;
import com.besmainfo.biprayer.services.PrayerTimesService.PrayerTime;
import com.besmainfo.biprayer.utils.LanguageHelper;


import java.util.Map;

public class PrayerTimesActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "PrayerTimesActivity";
    
    private PrayerTimesService prayerService;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    
    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];
    
    private boolean hasAccelerometer = false;
    private boolean hasMagnetometer = false;
    
    private TextView textViewPrayerTimes, textViewNextPrayer, textViewQibla;
    private View viewCompass;
    private Button btnRefresh, btnQibla;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 🌍 Appliquer la langue sauvegardée
        LanguageHelper.applySavedLanguage(this);
        
        setContentView(R.layout.activity_prayer_times);
        
        Log.d(TAG, "Démarrage PrayerTimesActivity...");
        
        initializeServices();
        setupViews();
        setupSensors();
        updatePrayerTimes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 🌍 Vérifier les changements de langue à chaque retour
        LanguageHelper.checkAndApplyLanguageChange(this);
        
        Log.d(TAG, "onResume - Redémarrage des capteurs si nécessaire");
        
        if (textViewQibla.getVisibility() == View.VISIBLE) {
            startSensors();
        }
        
        // Rafraîchir les horaires à chaque retour sur l'activité
        updatePrayerTimes();
    }

    private void initializeServices() {
        prayerService = new PrayerTimesService(this);
        
        // Simulation de localisation (Paris)
        Location demoLocation = new Location("demo");
        demoLocation.setLatitude(48.8566);
        demoLocation.setLongitude(2.3522);
        prayerService.setLocation(demoLocation);
        
        Log.d(TAG, "Services horaires initialisés");
    }

    private void setupViews() {
        textViewPrayerTimes = findViewById(R.id.textViewPrayerTimes);
        textViewNextPrayer = findViewById(R.id.textViewNextPrayer);
        textViewQibla = findViewById(R.id.textViewQibla);
        viewCompass = findViewById(R.id.viewCompass);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnQibla = findViewById(R.id.btnQibla);
        
        // Setup des listeners
        btnRefresh.setOnClickListener(v -> updatePrayerTimes());
        btnQibla.setOnClickListener(v -> toggleQiblaMode());
        
        // Cacher la section Qibla au démarrage
        textViewQibla.setVisibility(View.GONE);
        viewCompass.setVisibility(View.GONE);
        
        Log.d(TAG, "Vues initialisées");
    }

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            
            hasAccelerometer = (accelerometer != null);
            hasMagnetometer = (magnetometer != null);
        }
        
        Log.d(TAG, "Capteurs - Accéléromètre: " + hasAccelerometer + ", Magnétomètre: " + hasMagnetometer);
        
        if (!hasAccelerometer || !hasMagnetometer) {
            Log.w(TAG, "Capteurs de boussole non disponibles sur cet appareil");
            showToast(getString(R.string.compass_not_available));
        }
    }

    private void updatePrayerTimes() {
        try {
            Log.d(TAG, "Mise à jour des horaires de prière...");
            
            // Obtenir les notifications d'horaires
            String notifications = prayerService.getPrayerNotifications();
            textViewPrayerTimes.setText(notifications);
            
            // Obtenir la prochaine prière
            PrayerTime nextPrayer = prayerService.getNextPrayer();
            String timeUntil = prayerService.getTimeUntilNextPrayer();
            
            String nextPrayerText = getString(R.string.next_prayer_format, 
                nextPrayer.getName(), nextPrayer.getTime(), timeUntil);
            textViewNextPrayer.setText(nextPrayerText);
            
            Log.d(TAG, "Horaires mis à jour - Prochaine prière: " + nextPrayer.getName());
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur mise à jour horaires", e);
            showToast(getString(R.string.prayer_times_error));
            textViewPrayerTimes.setText(getString(R.string.prayer_times_loading_error));
        }
    }

    private void toggleQiblaMode() {
        if (textViewQibla.getVisibility() == View.VISIBLE) {
            // Mode Qibla actif → désactiver
            hideQiblaMode();
        } else {
            // Mode Qibla inactif → activer
            showQiblaMode();
        }
    }

    private void showQiblaMode() {
        try {
            textViewQibla.setVisibility(View.VISIBLE);
            viewCompass.setVisibility(View.VISIBLE);
            startSensors();
            updateQiblaDirection();
            btnQibla.setText(getString(R.string.back_button));
            
            Log.d(TAG, "Mode Qibla activé");
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur activation mode Qibla", e);
            showToast(getString(R.string.compass_activation_error));
        }
    }

    private void hideQiblaMode() {
        textViewQibla.setVisibility(View.GONE);
        viewCompass.setVisibility(View.GONE);
        stopSensors();
        btnQibla.setText(getString(R.string.button_qibla));
        
        Log.d(TAG, "Mode Qibla désactivé");
    }

    private void startSensors() {
        if (hasAccelerometer) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            Log.d(TAG, "Accéléromètre démarré");
        }
        if (hasMagnetometer) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
            Log.d(TAG, "Magnétomètre démarré");
        }
    }

    private void stopSensors() {
        sensorManager.unregisterListener(this);
        Log.d(TAG, "Capteurs arrêtés");
    }

    private void updateQiblaDirection() {
        try {
            double qiblaDirection = prayerService.calculateQiblaDirection();
            String directionText = getString(R.string.qibla_direction_format, qiblaDirection);
            textViewQibla.setText(directionText);
            
            Log.d(TAG, "Direction Qibla mise à jour: " + qiblaDirection + "°");
            
        } catch (Exception e) {
            Log.e(TAG, "Erreur calcul direction Qibla", e);
            textViewQibla.setText(getString(R.string.qibla_calculation_error));
        }
    }

    // SensorEventListener methods
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (textViewQibla.getVisibility() != View.VISIBLE) return;
        
        try {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
            }
            
            if (hasAccelerometer && hasMagnetometer && 
                lastAccelerometer != null && lastMagnetometer != null) {
                
                boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, 
                        lastAccelerometer, lastMagnetometer);
                
                if (success) {
                    SensorManager.getOrientation(rotationMatrix, orientation);
                    float azimuth = (float) Math.toDegrees(orientation[0]);
                    
                    // Mettre à jour l'affichage de la direction
                    double qiblaDirection = prayerService.calculateQiblaDirection();
                    double relativeDirection = (qiblaDirection - azimuth + 360) % 360;
                    
                    String directionInfo = getString(R.string.qibla_direction_detailed, 
                        qiblaDirection, azimuth);
                    textViewQibla.setText(directionInfo);
                    
                    // Rotation simple de la vue boussole
                    viewCompass.setRotation((float)relativeDirection);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur traitement capteurs", e);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Log du changement de précision
        Log.d(TAG, "Précision capteur " + sensor.getName() + ": " + accuracy);
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(PrayerTimesActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause - Arrêt des capteurs");
        stopSensors();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy - Nettoyage des ressources");
        stopSensors();
    }
}