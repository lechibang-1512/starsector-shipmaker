package oth.shipeditor.parsing;

import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public final class JsonProcessor {

    private static final Pattern LETTERS_AFTER_DIGIT = Pattern.compile("(\\b\\d+(?:\\.\\d*)?|\\.\\d+)[fFdD](?![a-zA-Z_])");
    private static final Pattern PERIOD_BEFORE_COMMA = Pattern.compile("(?<=\\d)\\.(?=\\s*,)");
    private static final Pattern PERIOD_BEFORE_SEPARATOR = Pattern.compile("(?<=\\d)\\.(?=\\s*[\r\n}\\]\\$])");

    private JsonProcessor() {
    }

    public static String straightenMalformed(File input) {
        String text = JsonProcessor.readFile(input);

        String preprocessed = JsonProcessor.correctCommentsUnquotedValuesAndSeparators(text);
        preprocessed = JsonProcessor.correctNumberLetterSignums(preprocessed);
        preprocessed = JsonProcessor.correctTrailingPeriods(preprocessed);

        return preprocessed;
    }

    private static String readFile(File input) {
        if (input == null || !input.exists()) {
            log.error("Failed to read file: file does not exist: {}", input != null ? input.getPath() : "null");
            return "";
        }
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(input.toPath());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read file: {}", input.getName(), e);
            return "";
        }
    }

    private static boolean isWordChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }

    /**
     * Replaces comments starting with '#', translates spurious semicolons outside quotes to commas,
     * and wraps unquoted alphanumeric keys/values in double quotes—all in a single fast, linear O(N) sweep.
     * This avoids the catastrophic backtracking lookup regex engine stalls.
     */
    private static String correctCommentsUnquotedValuesAndSeparators(String input) {
        if (input == null || input.isEmpty()) return "";
        
        StringBuilder sb = new StringBuilder(input.length() + 128);
        int len = input.length();
        boolean inQuotes = false;
        boolean escape = false;

        for (int i = 0; i < len; ) {
            char c = input.charAt(i);

            // 1. Quoted block state processing
            if (inQuotes) {
                if (c == '\\' && !escape) {
                    escape = true;
                } else {
                    escape = false;
                }
                if (c == '"' && !escape) {
                    inQuotes = false;
                }
                sb.append(c);
                i++;
                continue;
            }

            // 2. We are outside quotes
            if (c == '"') {
                inQuotes = true;
                sb.append(c);
                i++;
                continue;
            }

            // Skip comments starting with '#'
            if (c == '#') {
                while (i < len && input.charAt(i) != '\n' && input.charAt(i) != '\r') {
                    i++;
                }
                continue;
            }

            // Translate semicolons to commas
            if (c == ';') {
                sb.append(',');
                i++;
                continue;
            }

            // Check if it's the start of an identifier: [a-zA-Z_] at a word boundary, and not preceded by a dot
            if (((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_') &&
                (i == 0 || (input.charAt(i - 1) != '.' && !isWordChar(input.charAt(i - 1))))) {
                int start = i;
                while (i < len && isWordChar(input.charAt(i))) {
                    i++;
                }
                String word = input.substring(start, i);

                // Check if this word should be quoted
                boolean shouldQuote = true;
                if ("true".equals(word) || "false".equals(word) || "null".equals(word)) {
                    shouldQuote = false;
                }
                // Exclude words immediately preceded or followed by double quotes
                if (start > 0 && input.charAt(start - 1) == '"') {
                    shouldQuote = false;
                }
                if (i < len && input.charAt(i) == '"') {
                    shouldQuote = false;
                }

                if (shouldQuote) {
                    sb.append('"').append(word).append('"');
                } else {
                    sb.append(word);
                }
                continue;
            }

            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static String correctNumberLetterSignums(CharSequence inputJSON) {
        Matcher matcher = LETTERS_AFTER_DIGIT.matcher(inputJSON);
        return matcher.replaceAll("$1");
    }

    private static String correctTrailingPeriods(String inputJSON) {
        String result = PERIOD_BEFORE_COMMA.matcher(inputJSON).replaceAll("");
        result = PERIOD_BEFORE_SEPARATOR.matcher(result).replaceAll(",");
        return result;
    }

}
