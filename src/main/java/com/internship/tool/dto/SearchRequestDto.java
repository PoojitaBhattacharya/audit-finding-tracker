package com.internship.tool.dto;

import lombok.Data;

@Data
public class SearchRequestDto {
    private String q;
    private int page = 0;
    private int size = 10;
    private String sortBy = "id";
    private String sortDir = "asc";
}
