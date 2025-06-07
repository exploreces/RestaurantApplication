package com.epam.edp.demo.dto.request;

import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReservationRequestDto {
    @Nonnull
    private String locationId;
    @Nonnull
    private String tableNumber ;
    @Nonnull
    private String date;
    @Nonnull
    private String guestsNumber;
    @Nonnull
    private String timeFrom ;
    @Nonnull
    private String timeTo;


}
