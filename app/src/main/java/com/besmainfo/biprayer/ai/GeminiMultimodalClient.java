package com.besmainfo.biprayer.ai;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class GeminiMultimodalClient {
    private static final String TAG = "GeminiMultimodal";
    private final String apiKey;

    public GeminiMultimodalClient(String apiKey) {
        this.apiKey = apiKey;
        Log.d(TAG, "GeminiMultimodalClient initialized");
    }

    public String analyzeImageAndText(Bitmap image, String question) {
        if (!isApiKeyValid()) {
            return "❌ Gemini API key not configured";
        }

        HttpURLConnection conn = null;
        try {
            String imageBase64 = bitmapToBase64(image);
            
            URL url = new URL(
               "https://generativelanguage.googleapis.com/v1/models/gemini-pro-vision:generateContent?key=" + apiKey
            );
            
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);

            JSONObject requestBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();

            // Partie image
            JSONObject imagePart = new JSONObject();
            JSONObject imageData = new JSONObject();
            imageData.put("mime_type", "image/jpeg");
            imageData.put("data", imageBase64);
            imagePart.put("inline_data", imageData);
            parts.put(imagePart);

            // Partie texte
            JSONObject textPart = new JSONObject();
            textPart.put("text", question);
            parts.put(textPart);

            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            String jsonInput = requestBody.toString();

            OutputStream os = conn.getOutputStream();
            os.write(jsonInput.getBytes("utf-8"));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
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

                return extractTextFromResponse(response.toString());
                
            } else {
                Log.e(TAG, "HTTP error: " + responseCode);
                return getDemoPrayerTimes();
            }

        } catch (Exception e) {
            Log.e(TAG, "Analysis exception", e);
            return getDemoPrayerTimes();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public String extractPrayerTimesFromImage(Bitmap image) {
        String prompt = "Analyze this image and extract prayer times if it's a prayer timetable. " +
                       "Look for Fajr, Dhuhr, Asr, Maghrib, Isha times. " +
                       "If no prayer times found, describe what you see in the image from a spiritual perspective.";

        String result = analyzeImageAndText(image, prompt);
        return result;
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private String extractTextFromResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray candidates = jsonObject.getJSONArray("candidates");
            
            if (candidates.length() > 0) {
                JSONObject candidate = candidates.getJSONObject(0);
                JSONObject content = candidate.getJSONObject("content");
                JSONArray parts = content.getJSONArray("parts");
                
                if (parts.length() > 0) {
                    JSONObject part = parts.getJSONObject(0);
                    String text = part.getString("text");
                    return text.replace("\\n", "\n").trim();
                }
            }
            
            return "⚠️ No text found in the AI response";
            
        } catch (Exception e) {
            Log.e(TAG, "Extraction error", e);
            return getDemoPrayerTimes();
        }
    }

    private String getDemoPrayerTimes() {
        return "🕌 **Prayer Times Extracted (Demo Mode)**\n\n" +
               "Fajr: 5:30 AM 🌅\n" +
               "Sunrise: 6:45 AM\n" +
               "Dhuhr: 12:15 PM ☀️\n" + 
               "Asr: 3:45 PM ⛅\n" +
               "Maghrib: 6:20 PM 🌇\n" +
               "Isha: 7:45 PM 🌙\n\n" +
               "📍 **Note**: This is demo data. For real prayer times analysis, ensure:\n" +
               "• You have a valid Gemini API key\n" +
               "• The image contains clear prayer timings\n" +
               "• Good lighting and image quality";
    }

    public boolean isApiKeyValid() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("") &&
               !apiKey.contains("your_actual_") && apiKey.length() > 20;
    }
}