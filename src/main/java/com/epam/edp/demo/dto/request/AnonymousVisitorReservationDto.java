package com.epam.edp.demo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnonymousVisitorReservationDto {
    private String locationId;
    private String tableNumber;
    private String date;
    private String timeFrom;
    private String timeTo;
    private String guestsNumber;
    private String visitorName;
}