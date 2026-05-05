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

    public static String getEmoji(String element) {
        switch (element) {
            case "Api":    return "🔥";
            case "Air":    return "💧";
            case "Tanah":  return "🪨";
            case "Daun":   return "🌿";
            case "Cahaya": return "✨";
            case "Gelap":  return "🌑";
            default:       return "❓";
        }
    }
}
