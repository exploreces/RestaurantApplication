package com.epam.edp.demo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostponeReservationDto {
    private String reservationId;
    private String newDate;
    private String newTimeFrom;
    private String newTimeTo;
}