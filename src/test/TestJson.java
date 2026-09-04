public class TestJson {
    public static void main(String[] args) {
        String json = "{\"type\": RENDER_LOADED_MISSILES, \"renderHints\": [ROUGH], \"test\": 25f}";
        System.out.println(straightenMalformedText(json));
    }
    public static String straightenMalformedText(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(input.length() + 128);
        int len = input.length();
        boolean inQuotes = false;
        boolean escape = false;
        for (int i = 0; i < len; ) {
            char c = input.charAt(i);
            if (inQuotes) {
                if (escape) { escape = false; }
                else if (c == '\\') { escape = true; }
                else if (c == '"') { inQuotes = false; }
                sb.append(c);
                i++;
                continue;
            }
            if (c == '"') { inQuotes = true; sb.append(c); i++; continue; }
            if (c == '#') { while (i < len && input.charAt(i) != '\n' && input.charAt(i) != '\r') { i++; } continue; }
            if (c == ';') { sb.append(','); i++; continue; }
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.') {
                int start = i;
                int curr = i;
                boolean hasDigit = false;
                loop:
                while (curr < len) {
                    char nc = input.charAt(curr);
                    switch (nc) {
                        case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> { hasDigit = true; curr++; }
                        case '.' -> curr++;
                        case 'e', 'E' -> { curr++; if (curr < len && (input.charAt(curr) == '+' || input.charAt(curr) == '-')) { curr++; } }
                        default -> { break loop; }
                    }
                }
                if (hasDigit) {
                    int numEnd = curr;
                    if (curr < len) {
                        char nc = input.charAt(curr);
                        if (nc == 'f' || nc == 'F' || nc == 'd' || nc == 'D') {
                            if (curr + 1 == len || !isWordChar(input.charAt(curr + 1))) { curr++; }
                        }
                    }
                    if (numEnd > start && input.charAt(numEnd - 1) == '.') { numEnd--; }
                    sb.append(input, start, numEnd);
                    i = curr;
                    continue;
                }
            }
            if (((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_') &&
                (i == 0 || (input.charAt(i - 1) != '.' && !isWordChar(input.charAt(i - 1))))) {
                int start = i;
                while (i < len && isWordChar(input.charAt(i))) { i++; }
                String word = input.substring(start, i);
                boolean shouldQuote = true;
                if ("true".equals(word) || "false".equals(word) || "null".equals(word)) { shouldQuote = false; }
                if (start > 0 && input.charAt(start - 1) == '"') { shouldQuote = false; }
                if (i < len && input.charAt(i) == '"') { shouldQuote = false; }
                if (shouldQuote) { sb.append('"').append(word).append('"'); }
                else { sb.append(word); }
                continue;
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }
    private static boolean isWordChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }
}
