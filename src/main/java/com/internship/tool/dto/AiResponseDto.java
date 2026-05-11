package com.internship.tool.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiResponseDto {
    private String output;
    private String error;
    private boolean success;
    private Map<String, Object> metadata;

    public static AiResponseDto fallback(String errorMessage) {
        return AiResponseDto.builder()
                .output("AI service temporarily unavailable.")
                .error(errorMessage)
                .success(false)
                .build();
    }
}
