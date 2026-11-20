package Utils;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Theme {

    private static final Map<String, Color> vars = new HashMap<>();

    static {
        loadFromCss("css/style.css");
    }

    private static void loadFromCss(String path) {
        try (InputStream is = Theme.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) return;

            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            String line;
            Pattern p = Pattern.compile("--([a-zA-Z0-9-]+)\\s*:\\s*([^;]+);?");

            while ((line = r.readLine()) != null) {
                Matcher m = p.matcher(line);
                if (m.find()) {
                    String name = m.group(1).trim();
                    String value = m.group(2).trim();
                    Color c = parseColor(value);
                    if (c != null) vars.put(name, c);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static Color parseColor(String s) {
        if (s.startsWith("#")) {
            String hex = s.substring(1).trim();
            if (hex.length() == 3) {
                String r = hex.substring(0, 1);
                String g = hex.substring(1, 2);
                String b = hex.substring(2, 3);
                hex = r + r + g + g + b + b;
            }
            try {
                int rgb = Integer.parseInt(hex, 16);
                return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Color get(String var, Color fallback) {
        return vars.getOrDefault(var.replaceFirst("^--", ""), fallback);
    }

    public static final Color BACKGROUND = get("background", new Color(15, 23, 42));
    public static final Color CARD = get("card", new Color(30, 41, 59));
    public static final Color PRIMARY = get("primary", new Color(59, 130, 246));
    public static final Color TEXT = get("text", new Color(248, 250, 252));
    public static final Color MUTED = get("muted", new Color(148, 163, 184));
    public static final Color ERROR = get("error", new Color(248, 113, 113));
    public static final Color SIDEBAR = get("sidebar", new Color(17, 24, 39));
}
