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

    private static final Pattern CC_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");

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

        Matcher matcher = CC_PATTERN.matcher(body);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String match = matcher.group();
            String digitsOnly = match.replaceAll("[\\s-]", "");
            if (digitsOnly.length() >= 13 && digitsOnly.length() <= 16) {
                String masked = "****-****-****-" + digitsOnly.substring(digitsOnly.length() - 4);
                matcher.appendReplacement(sb, masked);
            } else {
                matcher.appendReplacement(sb, match);
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
