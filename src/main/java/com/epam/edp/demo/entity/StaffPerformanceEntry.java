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
public class StaffPerformanceEntry {
    private String locationName;
    private String waiterName;
    private String waiterEmail;
    private LocalDate reportStartDate;
    private LocalDate reportEndDate;
    private Integer hoursWorked;
    private Integer ordersProcessed;
    private Double ordersProcessedDeltaPercent;
    private Double averageServiceFeedback;
    private Integer minimumServiceFeedback;
    private Double minimumServiceFeedbackDeltaPercent;
}