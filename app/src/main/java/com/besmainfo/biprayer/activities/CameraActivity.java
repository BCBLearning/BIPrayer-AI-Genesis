package com.besmainfo.biprayer.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.besmainfo.biprayer.ai.GeminiMultimodalClient;
import com.besmainfo.biprayer.utils.ConfigReader;
import com.besmainfo.biprayer.utils.LanguageHelper;
import com.google.common.util.concurrent.ListenableFuture;
import com.besmainfo.biprayer.R;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    private static final int REQUEST_IMAGE_PICK = 1001;
    
    private PreviewView previewView;
    private ImageView imageViewCaptured;
    private TextView textViewAnalysis;
    private Button btnCapture, btnAnalyze, btnGallery;
    
    private GeminiMultimodalClient geminiVisionClient;
    private Bitmap currentBitmap;
    
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 🌍 Appliquer la langue sauvegardée
        LanguageHelper.applySavedLanguage(this);
        
        setContentView(R.layout.activity_camera);
        
        Log.d(TAG, "Démarrage CameraActivity (Mode Réel)...");
        
        initializeServices();
        setupViews();
        startCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 🌍 Vérifier les changements de langue à chaque retour
        LanguageHelper.checkAndApplyLanguageChange(this);
    }

    private void initializeServices() {
        geminiVisionClient = new GeminiMultimodalClient(ConfigReader.getGeminiApiKey());
        cameraExecutor = Executors.newSingleThreadExecutor();
        Log.d(TAG, "Services caméra initialisés (Mode Réel)");
    }

    private void setupViews() {
        previewView = findViewById(R.id.previewView);
        imageViewCaptured = findViewById(R.id.imageViewCaptured);
        textViewAnalysis = findViewById(R.id.textViewAnalysis);
        btnCapture = findViewById(R.id.btnCapture);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        btnGallery = findViewById(R.id.btnGallery);
        
        btnCapture.setOnClickListener(v -> captureImage());
        btnAnalyze.setOnClickListener(v -> analyzeImage());
        btnGallery.setOnClickListener(v -> loadFromGallery());
        
        imageViewCaptured.setVisibility(View.GONE);
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (Exception e) {
                Log.e(TAG, "Erreur démarrage caméra", e);
                showToast(getString(R.string.toast_camera_error, e.getMessage()));
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        } catch (Exception e) {
            Log.e(TAG, "Erreur liaison caméra", e);
        }
    }

    private void captureImage() {
        if (imageCapture == null) return;

        File photoFile = new File(getExternalFilesDir(null), "prayer_times_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = Uri.fromFile(photoFile);
                        String msg = getString(R.string.photo_captured, savedUri);
                        showToast(msg);
                        Log.d(TAG, msg);

                        // Charger l'image capturée
                        currentBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                        if (currentBitmap != null) {
                            imageViewCaptured.setImageBitmap(currentBitmap);
                            imageViewCaptured.setVisibility(View.VISIBLE);
                            previewView.setVisibility(View.GONE);
                            textViewAnalysis.setText(getString(R.string.photo_captured_ready));
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Erreur capture: " + exception.getMessage(), exception);
                        showToast(getString(R.string.capture_error, exception.getMessage()));
                    }
                });
    }

    private void analyzeImage() {
        if (currentBitmap == null) {
            showToast(getString(R.string.no_image_to_analyze));
            return;
        }
        
        textViewAnalysis.setText(getString(R.string.status_analyzing_image));
        btnAnalyze.setEnabled(false);
        
        new Thread(() -> {
            try {
                String analysis = geminiVisionClient.extractPrayerTimesFromImage(currentBitmap);
                
                runOnUiThread(() -> {
                    textViewAnalysis.setText(analysis);
                    btnAnalyze.setEnabled(true);
                    showToast(getString(R.string.analysis_complete));
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur analyse image", e);
                runOnUiThread(() -> {
                    textViewAnalysis.setText(getString(R.string.analysis_error, e.getMessage()));
                    btnAnalyze.setEnabled(true);
                    showToast(getString(R.string.analysis_failed));
                });
            }
        }).start();
    }

    private void loadFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedImage);
                currentBitmap = BitmapFactory.decodeStream(inputStream);
                
                imageViewCaptured.setImageBitmap(currentBitmap);
                imageViewCaptured.setVisibility(View.VISIBLE);
                previewView.setVisibility(View.GONE);
                textViewAnalysis.setText(getString(R.string.image_selected_ready));
                
            } catch (Exception e) {
                Log.e(TAG, "Erreur chargement galerie", e);
                showToast(getString(R.string.toast_image_error, e.getMessage()));
            }
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        Log.d(TAG, "CameraActivity détruite");
    }
}