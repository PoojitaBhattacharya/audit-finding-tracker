package com.internship.tool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class AiServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AiServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:5000}")
    private String aiServiceUrl;

    public AiServiceClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(10000); // 10 seconds
        this.restTemplate = new RestTemplate(factory);
    }

    public Object describe(String text) {
        return callAiEndpoint("/describe", text);
    }

    public Object categorise(String text) {
        return callAiEndpoint("/categorise", text);
    }

    public Object recommend(String text) {
        return callAiEndpoint("/recommend", text);
    }

    private Object callAiEndpoint(String endpoint, String text) {
        try {
            Map<String, String> request = Map.of("text", text);
            return restTemplate.postForObject(aiServiceUrl + endpoint, request, Map.class);
        } catch (RestClientException e) {
            logger.error("Error calling AI service at {}: {}", endpoint, e.getMessage());
            // Return graceful null to prevent system crashes
            return null;
        }
    }
}
