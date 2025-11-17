package com.besmainfo.biprayer.services;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.*;

public class PrayerTimesService {
    private static final String TAG = "PrayerTimesService";
    private final Context context;
    private Location currentLocation;
    private String calculationMethod = "MWL";

    public PrayerTimesService(Context context) {
        this.context = context;
        Log.d(TAG, "Service Horaires de Prière initialisé");
    }

    public void setLocation(Location location) {
        this.currentLocation = location;
        Log.d(TAG, "Position mise à jour: " + location.getLatitude() + ", " + location.getLongitude());
    }

    public Map<String, PrayerTime> calculatePrayerTimes() {
        Map<String, PrayerTime> prayerTimes = new LinkedHashMap<>();
        
        if (currentLocation == null) {
            return getDemoPrayerTimes();
        }

        try {
            double latitude = currentLocation.getLatitude();
            double longitude = currentLocation.getLongitude();
            Calendar calendar = Calendar.getInstance();
            
            prayerTimes.put("Fajr", calculatePrayerTime("Fajr", 5, 30, latitude, longitude, calendar));
            prayerTimes.put("Sunrise", calculatePrayerTime("Sunrise", 6, 45, latitude, longitude, calendar));
            prayerTimes.put("Dhuhr", calculatePrayerTime("Dhuhr", 12, 15, latitude, longitude, calendar));
            prayerTimes.put("Asr", calculatePrayerTime("Asr", 15, 45, latitude, longitude, calendar));
            prayerTimes.put("Maghrib", calculatePrayerTime("Maghrib", 18, 20, latitude, longitude, calendar));
            prayerTimes.put("Isha", calculatePrayerTime("Isha", 19, 45, latitude, longitude, calendar));

        } catch (Exception e) {
            Log.e(TAG, "Erreur calcul horaires", e);
            return getDemoPrayerTimes();
        }

        return prayerTimes;
    }

    private PrayerTime calculatePrayerTime(String prayerName, int baseHour, int baseMinute, 
                                         double lat, double lng, Calendar cal) {
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
        double latAdjustment = Math.sin(Math.toRadians(lat)) * 30;
        double seasonalAdjustment = Math.sin(dayOfYear / 58.09) * 20;
        
        int totalMinutes = baseHour * 60 + baseMinute + (int)latAdjustment + (int)seasonalAdjustment;
        int hour = (totalMinutes / 60) % 24;
        int minute = totalMinutes % 60;
        
        String time = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        return new PrayerTime(prayerName, time, getPrayerEmoji(prayerName));
    }

    public PrayerTime getNextPrayer() {
        Map<String, PrayerTime> times = calculatePrayerTimes();
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        
        for (PrayerTime prayer : times.values()) {
            if (prayer.getTime().compareTo(currentTime) > 0) {
                return prayer;
            }
        }
        
        return times.get("Fajr");
    }

    public String getTimeUntilNextPrayer() {
        PrayerTime nextPrayer = getNextPrayer();
        try {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date current = format.parse(format.format(new Date()));
            Date prayerTime = format.parse(nextPrayer.getTime());
            
            long diff = prayerTime.getTime() - current.getTime();
            if (diff < 0) diff += 24 * 60 * 60 * 1000;
            
            long hours = diff / (60 * 60 * 1000);
            long minutes = (diff % (60 * 60 * 1000)) / (60 * 1000);
            
            return String.format("%02d:%02d", hours, minutes);
            
        } catch (Exception e) {
            return "00:00";
        }
    }

    public String getPrayerNotifications() {
        Map<String, PrayerTime> times = calculatePrayerTimes();
        PrayerTime nextPrayer = getNextPrayer();
        String timeUntil = getTimeUntilNextPrayer();
        
        StringBuilder notification = new StringBuilder();
        notification.append("🕌 **Vos Horaires de Prière**\n\n");
        
        for (PrayerTime prayer : times.values()) {
            String status = prayer.getName().equals(nextPrayer.getName()) ? " ⏰ (Prochaine)" : "";
            notification.append(prayer.getEmoji()).append(" **")
                       .append(prayer.getName()).append(":** ")
                       .append(prayer.getTime()).append(status).append("\n");
        }
        
        notification.append("\n⏱️ **Prochaine prière:** ").append(nextPrayer.getName())
                   .append(" dans ").append(timeUntil);
        
        if (currentLocation != null) {
            notification.append("\n📍 **Position:** ")
                       .append(String.format("%.2f, %.2f", 
                               currentLocation.getLatitude(), currentLocation.getLongitude()));
        } else {
            notification.append("\n📍 **Mode:** Démonstration");
        }
        
        return notification.toString();
    }

    public double calculateQiblaDirection() {
        if (currentLocation == null) return 0.0;
        
        double kaabaLat = 21.4225;
        double kaabaLng = 39.8262;
        
        double lat = Math.toRadians(currentLocation.getLatitude());
        double lng = Math.toRadians(currentLocation.getLongitude());
        double kaabaLatRad = Math.toRadians(kaabaLat);
        double kaabaLngRad = Math.toRadians(kaabaLng);
        
        double y = Math.sin(kaabaLngRad - lng);
        double x = Math.cos(lat) * Math.tan(kaabaLatRad) - Math.sin(lat) * Math.cos(kaabaLngRad - lng);
        
        double direction = Math.toDegrees(Math.atan2(y, x));
        return (direction + 360) % 360;
    }

    private Map<String, PrayerTime> getDemoPrayerTimes() {
        Map<String, PrayerTime> demoTimes = new LinkedHashMap<>();
        demoTimes.put("Fajr", new PrayerTime("Fajr", "05:30", "🌅"));
        demoTimes.put("Sunrise", new PrayerTime("Sunrise", "06:45", "🌄"));
        demoTimes.put("Dhuhr", new PrayerTime("Dhuhr", "12:15", "☀️"));
        demoTimes.put("Asr", new PrayerTime("Asr", "15:45", "⛅"));
        demoTimes.put("Maghrib", new PrayerTime("Maghrib", "18:20", "🌇"));
        demoTimes.put("Isha", new PrayerTime("Isha", "19:45", "🌙"));
        return demoTimes;
    }

    private String getPrayerEmoji(String prayerName) {
        switch (prayerName) {
            case "Fajr": return "🌅";
            case "Dhuhr": return "☀️";
            case "Asr": return "⛅";
            case "Maghrib": return "🌇";
            case "Isha": return "🌙";
            case "Sunrise": return "🌄";
            default: return "🕌";
        }
    }

    public static class PrayerTime {
        private final String name;
        private final String time;
        private final String emoji;

        public PrayerTime(String name, String time, String emoji) {
            this.name = name;
            this.time = time;
            this.emoji = emoji;
        }

        public String getName() { return name; }
        public String getTime() { return time; }
        public String getEmoji() { return emoji; }

        @Override
        public String toString() {
            return emoji + " " + name + ": " + time;
        }
    }
}