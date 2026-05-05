package com.internship.tool.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDto {
    private long totalFindings;
    private long openFindings;
    private long closedFindings;
    private long overdueFindings;
    private long criticalFindings;
    private long highFindings;
    private long mediumFindings;
    private long lowFindings;
}
