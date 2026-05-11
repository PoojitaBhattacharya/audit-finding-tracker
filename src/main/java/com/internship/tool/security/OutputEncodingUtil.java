package com.internship.tool.security;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for context-aware output encoding to prevent XSS attacks.
 * Provides encoding methods for different contexts (HTML, JavaScript, URL, CSS).
 */
public class OutputEncodingUtil {

    // HTML Entity Encoding Map
    private static final Map<Character, String> HTML_ENTITY_MAP = new HashMap<>();
    static {
        HTML_ENTITY_MAP.put('&', "&amp;");
        HTML_ENTITY_MAP.put('<', "&lt;");
        HTML_ENTITY_MAP.put('>', "&gt;");
        HTML_ENTITY_MAP.put('"', "&quot;");
        HTML_ENTITY_MAP.put('\'', "&#x27;");
        HTML_ENTITY_MAP.put('/', "&#x2F;");
    }

    /**
     * Encode string for safe insertion into HTML content (text nodes).
     * This prevents HTML injection and XSS attacks.
     *
     * @param input The string to encode
     * @return HTML-encoded string
     */
    public static String encodeForHtml(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            String encoded = HTML_ENTITY_MAP.get(c);
            if (encoded != null) {
                sb.append(encoded);
            } else if (c > 127) {
                // Encode non-ASCII characters
                sb.append("&#").append((int) c).append(";");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Encode string for safe insertion into HTML attributes.
     * Double-quoted attributes are most secure.
     *
     * @param input The string to encode
     * @return HTML attribute-encoded string
     */
    public static String encodeForHtmlAttribute(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c == '"') {
                sb.append("&quot;");
            } else if (c == '\'') {
                sb.append("&#x27;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '&') {
                sb.append("&amp;");
            } else if (c == '/') {
                sb.append("&#x2F;");
            } else if (c > 127) {
                sb.append("&#").append((int) c).append(";");
            } else if (c < 32 && c != '\t' && c != '\n' && c != '\r') {
                // Encode control characters
                sb.append("&#").append((int) c).append(";");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Encode string for safe use in JavaScript strings (inside double quotes).
     *
     * @param input The string to encode
     * @return JavaScript-encoded string
     */
    public static String encodeForJavaScript(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\'':
                    sb.append("\\'");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 32 || c >= 127) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Encode string for safe use in URLs (URL encoding).
     *
     * @param input The string to encode
     * @return URL-encoded string
     */
    public static String encodeForUrl(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : input.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            char c = (char) b;
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.' || c == '~') {
                sb.append(c);
            } else {
                sb.append('%').append(String.format("%02X", b & 0xFF));
            }
        }
        return sb.toString();
    }

    /**
     * Encode string for safe use in CSS (prevents CSS injection).
     *
     * @param input The string to encode
     * @return CSS-encoded string
     */
    public static String encodeForCss(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= 0x30 && c <= 0x39 || // 0-9
                    c >= 0x41 && c <= 0x5A || // A-Z
                    c >= 0x61 && c <= 0x7A) { // a-z
                sb.append(c);
            } else {
                sb.append(String.format("\\%x ", c));
            }
        }
        return sb.toString();
    }

    /**
     * Encode string for safe use in LDAP queries.
     *
     * @param input The string to encode
     * @return LDAP-encoded string
     */
    public static String encodeForLdap(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '\\':
                    sb.append("\\5c");
                    break;
                case '*':
                    sb.append("\\2a");
                    break;
                case '(':
                    sb.append("\\28");
                    break;
                case ')':
                    sb.append("\\29");
                    break;
                case '\0':
                    sb.append("\\00");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Strip all HTML tags from input (use when HTML is not needed).
     *
     * @param input The string to strip
     * @return String with HTML tags removed
     */
    public static String stripHtmlTags(String input) {
        if (input == null) {
            return null;
        }

        return input.replaceAll("<[^>]*>", "");
    }

    /**
     * Validate that input contains only alphanumeric characters and basic punctuation.
     *
     * @param input The string to validate
     * @return true if input is safe, false otherwise
     */
    public static boolean isSafeAlphanumeric(String input) {
        if (input == null) {
            return false;
        }

        return input.matches("^[a-zA-Z0-9\\s.,!?@#()\\-_:;/]*$");
    }
}
