package beastclash.view;

import java.awt.Color;

public class ElementColor {
    public static Color getColor(String element) {
        switch (element) {
            case "Api":    return new Color(220, 80,  30);
            case "Air":    return new Color(30,  120, 220);
            case "Tanah":  return new Color(139, 90,  43);
            case "Daun":   return new Color(34,  139, 34);
            case "Cahaya": return new Color(255, 215, 0);
            case "Gelap":  return new Color(75,  0,   130);
            default:       return new Color(150, 150, 150);
        }
    }

    /**
     * Ganti emoji Unicode dengan teks ASCII sederhana agar tidak muncul kotak
     * di sistem yang fontnya tidak support emoji.
     */
    public static String getEmoji(String element) {
        switch (element) {
            case "Api":    return "Api";
            case "Air":    return "Air";
            case "Tanah":  return "Tanah";
            case "Daun":   return "Daun";
            case "Cahaya": return "Cahaya";
            case "Gelap":  return "Gelap";
            default:       return "?";
        }
    }

    /** Versi pendek untuk label kecil. */
    public static String getShortLabel(String element) {
        switch (element) {
            case "Api":    return "Api";
            case "Air":    return "Air";
            case "Tanah":  return "Tanah";
            case "Daun":   return "Daun";
            case "Cahaya": return "Cahaya";
            case "Gelap":  return "Gelap";
            default:       return "?";
        }
    }
}
