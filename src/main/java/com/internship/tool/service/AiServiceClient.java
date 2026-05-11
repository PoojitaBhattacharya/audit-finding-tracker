package com.internship.tool.service;

import com.internship.tool.dto.AiResponseDto;
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

    @Value("${ai.service.url:http://ai-service:5000}")
    private String aiServiceUrl;

    public AiServiceClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds connection timeout
        factory.setReadTimeout(10000);    // 10 seconds read timeout
        this.restTemplate = new RestTemplate(factory);
    }

    public AiResponseDto describe(String text) {
        return callAiEndpoint("/describe", text);
    }

    public AiResponseDto categorise(String text) {
        return callAiEndpoint("/categorise", text);
    }

    public AiResponseDto recommend(String text) {
        return callAiEndpoint("/recommend", text);
    }

    private AiResponseDto callAiEndpoint(String endpoint, String text) {
        try {
            logger.info("Calling AI service endpoint: {}", endpoint);
            Map<String, String> request = Map.of("text", text);
            Map<String, Object> response = restTemplate.postForObject(aiServiceUrl + endpoint, request, Map.class);
            
            if (response != null && response.containsKey("data")) {
                return AiResponseDto.builder()
                        .output(response.get("data").toString())
                        .success(true)
                        .metadata(response.containsKey("meta") ? (Map<String, Object>) response.get("meta") : null)
                        .build();
            } else if (response != null && response.containsKey("output")) {
                // Fallback for simple output structure
                return AiResponseDto.builder()
                        .output(response.get("output").toString())
                        .success(true)
                        .build();
            }
            
            return AiResponseDto.builder()
                    .output("Received empty or invalid response from AI service.")
                    .success(false)
                    .build();
                    
        } catch (RestClientException e) {
            logger.error("Failed to connect to AI service at {}{}: {}", aiServiceUrl, endpoint, e.getMessage());
            return AiResponseDto.fallback(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during AI service call: {}", e.getMessage());
            return AiResponseDto.fallback("Unexpected error: " + e.getMessage());
        }
    }
}
