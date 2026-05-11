package com.internship.tool.dto;

public class PiiMaskingUtil {

    private PiiMaskingUtil() {
        // Prevent instantiation of utility class
    }

    /**
     * Masks a sensitive string like a credit card number or a generic ID.
     * Keeps the last 4 characters visible, replaces the rest with '*'.
     *
     * @param pii The sensitive string to mask.
     * @return The masked string, or null if the input is null.
     */
    public static String maskSensitiveData(String pii) {
        if (pii == null) {
            return null;
        }
        
        int length = pii.length();
        if (length <= 4) {
            return pii;
        }
        
        String maskedPart = "*".repeat(length - 4);
        String visiblePart = pii.substring(length - 4);
        
        return maskedPart + visiblePart;
    }
}
