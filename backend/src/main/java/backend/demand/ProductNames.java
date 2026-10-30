package backend.demand;

import backend.model.Category;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Normalises what customers type ("Tamatar", "tomatoes ") into one
 *  canonical product key, and knows rough re-purchase cycles. */
public final class ProductNames {

    private static final Map<String, String> SYNONYMS = Map.ofEntries(
            Map.entry("tamatar", "tomato"),
            Map.entry("aloo", "potato"),
            Map.entry("alu", "potato"),
            Map.entry("pyaz", "onion"),
            Map.entry("pyaaz", "onion"),
            Map.entry("palak", "spinach"),
            Map.entry("bhindi", "okra"),
            Map.entry("gobi", "cauliflower"),
            Map.entry("phool gobhi", "cauliflower"),
            Map.entry("kela", "banana"),
            Map.entry("seb", "apple"),
            Map.entry("aam", "mango"),
            Map.entry("dahi", "curd"),
            Map.entry("doodh", "milk"),
            Map.entry("nimbu", "lemon"),
            Map.entry("chilly", "chilli"),
            Map.entry("chillie", "chilli"),
            Map.entry("dhaniya", "coriander"),
            Map.entry("gajar", "carrot"),
            Map.entry("baingan", "brinjal"),
            Map.entry("mirch", "chilli"),
            Map.entry("adrak", "ginger"),
            Map.entry("lehsun", "garlic"),
            Map.entry("matar", "pea"),
            Map.entry("santra", "orange"),
            Map.entry("angoor", "grape"),
            Map.entry("papita", "papaya"),
            Map.entry("amrood", "guava"),
            Map.entry("anar", "pomegranate"),
            Map.entry("tarbooz", "watermelon"),
            Map.entry("mooli", "radish"),
            Map.entry("kheera", "cucumber"),
            Map.entry("patta gobhi", "cabbage"),
            Map.entry("chukandar", "beetroot"),
            Map.entry("टमाटर", "tomato"),
            Map.entry("आलू", "potato"),
            Map.entry("प्याज", "onion"),
            Map.entry("पालक", "spinach"),
            Map.entry("भिंडी", "okra"),
            Map.entry("गोभी", "cauliflower"),
            Map.entry("फूलगोभी", "cauliflower"),
            Map.entry("पत्ता गोभी", "cabbage"),
            Map.entry("केला", "banana"),
            Map.entry("सेब", "apple"),
            Map.entry("आम", "mango"),
            Map.entry("दही", "curd"),
            Map.entry("दूध", "milk"),
            Map.entry("नींबू", "lemon"),
            Map.entry("धनिया", "coriander"),
            Map.entry("पनीर", "paneer"),
            Map.entry("गाजर", "carrot"),
            Map.entry("मिर्च", "chilli"),
            Map.entry("अदरक", "ginger"),
            Map.entry("लहसुन", "garlic"),
            Map.entry("बैंगन", "brinjal"),
            Map.entry("खीरा", "cucumber"),
            Map.entry("मटर", "pea"),
            Map.entry("संतरा", "orange"),
            Map.entry("अंगूर", "grape"),
            Map.entry("पपीता", "papaya"),
            Map.entry("अमरूद", "guava"),
            Map.entry("अनार", "pomegranate"),
            Map.entry("तरबूज", "watermelon"),
            Map.entry("मूली", "radish"),
            Map.entry("चुकंदर", "beetroot"),
            Map.entry("தக்காளி", "tomato"),
            Map.entry("உருளைக்கிழங்கு", "potato"),
            Map.entry("வெங்காயம்", "onion"),
            Map.entry("கீரை", "spinach"),
            Map.entry("வெண்டைக்காய்", "okra"),
            Map.entry("காலிஃபிளவர்", "cauliflower"),
            Map.entry("முட்டைகோஸ்", "cabbage"),
            Map.entry("வாழைப்பழம்", "banana"),
            Map.entry("ஆப்பிள்", "apple"),
            Map.entry("மாம்பழம்", "mango"),
            Map.entry("தயிர்", "curd"),
            Map.entry("பால்", "milk"),
            Map.entry("எலுமிச்சை", "lemon"),
            Map.entry("கொத்தமல்லி", "coriander"),
            Map.entry("கேரட்", "carrot"),
            Map.entry("கத்திரிக்காய்", "brinjal"),
            Map.entry("மிளகாய்", "chilli"),
            Map.entry("இஞ்சி", "ginger"),
            Map.entry("பூண்டு", "garlic"),
            Map.entry("வெள்ளரிக்காய்", "cucumber"),
            Map.entry("ஆரஞ்சு", "orange"),
            Map.entry("திராட்சை", "grape"),
            Map.entry("பப்பாளி", "papaya"),
            Map.entry("கொய்யா", "guava"),
            Map.entry("மாதுளை", "pomegranate"),
            Map.entry("தர்பூசணி", "watermelon"),
            Map.entry("முள்ளங்கி", "radish"),
            Map.entry("பீட்ரூட்", "beetroot"),
            Map.entry("thakkali", "tomato"),
            Map.entry("vengayam", "onion"),
            Map.entry("kothamalli", "coriander"),
            Map.entry("elumichai", "lemon"),
            Map.entry("vazhaipazham", "banana")
    );

    private static final Set<String> KNOWN_VEGETABLES = Set.of(
            "tomato", "potato", "onion", "spinach", "okra", "cauliflower", "cabbage", "carrot",
            "brinjal", "capsicum", "chilli", "coriander", "ginger", "garlic", "cucumber", "pea",
            "bean", "pumpkin", "radish", "beetroot", "lemon", "gourd", "peas");

    private static final Set<String> KNOWN_FRUITS = Set.of(
            "banana", "apple", "mango", "orange", "grape", "papaya", "guava", "pomegranate",
            "watermelon", "melon", "pineapple", "lemon", "sapota", "pear", "kiwi");

    private ProductNames() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{M}\\p{N}\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (s.isEmpty()) {
            return "";
        }
        String mapped = SYNONYMS.get(s);
        if (mapped != null) {
            return mapped;
        }
        s = singular(s);
        mapped = SYNONYMS.get(s);
        return mapped != null ? mapped : s;
    }

    private static String singular(String s) {
        if (s.length() > 4 && s.endsWith("oes")) {
            return s.substring(0, s.length() - 2);
        }
        if (s.length() > 4 && s.endsWith("ies")) {
            return s.substring(0, s.length() - 3) + "y";
        }
        if (s.length() > 3 && s.endsWith("s") && !s.endsWith("ss")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

    public static String displayName(String normalized) {
        if (normalized == null || normalized.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(normalized.length());
        boolean up = true;
        for (char ch : normalized.toCharArray()) {
            sb.append(up ? Character.toUpperCase(ch) : ch);
            up = ch == ' ' || ch == '-';
        }
        return sb.toString();
    }

    public static Category inferCategory(String normalized) {
        if (KNOWN_VEGETABLES.contains(normalized)) {
            return Category.VEGETABLES;
        }
        if (KNOWN_FRUITS.contains(normalized)) {
            return Category.FRUITS;
        }
        if (normalized.equals("milk") || normalized.equals("curd") || normalized.equals("paneer")
                || normalized.equals("butter") || normalized.equals("egg")) {
            return Category.DAIRY;
        }
        return null;
    }

    /** Rough days between re-purchases of a product. A planning heuristic, not a measurement. */
    public static double cycleDays(String normalized, Category category) {
        Category c = category != null ? category : inferCategory(normalized);
        if (c == null) {
            return 7;
        }
        return switch (c) {
            case VEGETABLES -> 3;
            case FRUITS -> 4;
            case DAIRY -> 1;
            case TEA, JUICE -> 2;
            case SNACKS -> 5;
            case GROCERY -> 14;
            case OTHER -> 7;
        };
    }
}
