package com.epam.edp.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationComparisonEntry {
    private String locationName;
    private LocalDate reportStartDate;
    private LocalDate reportEndDate;
    private Integer ordersProcessed;
    private Double ordersProcessedDeltaPercent;
    private Double averageCuisineFeedback;
    private Integer minimumCuisineFeedback;
    private Double averageCuisineFeedbackDeltaPercent;
    private Double revenue;
    private Double revenueDeltaPercent;
}