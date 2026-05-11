package com.internship.tool.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PiiMaskingFilter extends OncePerRequestFilter {

    // Credit Card Pattern (13-16 digits with optional spaces/dashes)
    private static final Pattern CC_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");
    
    // Social Security Number Pattern (XXX-XX-XXXX or XXXXXXXXX)
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b(?:\\d{3}-?\\d{2}-?\\d{4})\\b");
    
    // Email Pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    
    // Phone Number Pattern (various formats)
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(?:\\+?1[-.]?)?\\(?([0-9]{3})\\)?[-.]?([0-9]{3})[-.]?([0-9]{4})\\b");
    
    // Date of Birth Pattern (MM/DD/YYYY or DD/MM/YYYY)
    private static final Pattern DOB_PATTERN = Pattern.compile("\\b(0[1-9]|1[0-2])[-/](0[1-9]|[12][0-9]|3[01])[-/](19|20)\\d{2}\\b");
    
    // Passport/ID Number Pattern
    private static final Pattern PASSPORT_PATTERN = Pattern.compile("\\b[A-Z]{1,2}\\d{6,9}\\b");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        filterChain.doFilter(request, responseWrapper);

        String contentType = responseWrapper.getContentType();
        if (contentType != null && (contentType.contains("application/json") || contentType.contains("text/"))) {
            byte[] responseArray = responseWrapper.getContentAsByteArray();
            String responseBody = new String(responseArray, responseWrapper.getCharacterEncoding());

            String maskedBody = maskPii(responseBody);

            responseWrapper.resetBuffer();
            responseWrapper.getWriter().write(maskedBody);
            responseWrapper.copyBodyToResponse();
        } else {
            responseWrapper.copyBodyToResponse();
        }
    }

    private String maskPii(String body) {
        if (body == null || body.isEmpty()) {
            return body;
        }

        // Apply all PII masking patterns in sequence
        body = maskCreditCards(body);
        body = maskSocialSecurityNumbers(body);
        body = maskEmails(body);
        body = maskPhoneNumbers(body);
        body = maskDateOfBirth(body);
        body = maskPassportNumbers(body);

        return body;
    }

    private String maskCreditCards(String body) {
        Matcher matcher = CC_PATTERN.matcher(body);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String match = matcher.group();
            String digitsOnly = match.replaceAll("[\\s-]", "");
            if (digitsOnly.length() >= 13 && digitsOnly.length() <= 16) {
                String masked = "****-****-****-" + digitsOnly.substring(digitsOnly.length() - 4);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(match));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskSocialSecurityNumbers(String body) {
        Matcher matcher = SSN_PATTERN.matcher(body);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String masked = "***-**-" + matcher.group().substring(matcher.group().length() - 4);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskEmails(String body) {
        Matcher matcher = EMAIL_PATTERN.matcher(body);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String email = matcher.group();
            String[] parts = email.split("@");
            String localPart = parts[0];
            String domain = parts[1];
            
            String maskedLocal = localPart.charAt(0) + "***" + 
                                (localPart.length() > 1 ? localPart.charAt(localPart.length() - 1) : "");
            String masked = maskedLocal + "@" + domain;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskPhoneNumbers(String body) {
        Matcher matcher = PHONE_PATTERN.matcher(body);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String masked = "***-***-" + matcher.group().replaceAll("[^0-9]", "").substring(6);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskDateOfBirth(String body) {
        Matcher matcher = DOB_PATTERN.matcher(body);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String masked = "**/**/****";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskPassportNumbers(String body) {
        Matcher matcher = PASSPORT_PATTERN.matcher(body);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String passport = matcher.group();
            String masked = passport.charAt(0) + "****" + passport.substring(Math.max(0, passport.length() - 2));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
