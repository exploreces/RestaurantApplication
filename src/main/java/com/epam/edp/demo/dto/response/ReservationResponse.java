package com.epam.edp.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReservationResponse {

    private String id;
    private String status;
    private String locationAddress;
    private String date;
    private String timeSlot;
    private String preOrder;
    private String guestNumber;
    private String feedbackId;
    private String locationId;

}
