package com.epam.edp.demo.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import lombok.Data;

@Data
public class NewFeedbackRequestDto {

    @NotBlank(message = "Please provide cuisine feedback")
    @JsonProperty
    private String cuisineComment;

    @NotBlank(message = "Please provide cuisine rating")
    @Pattern(regexp = "^[1-5]$", message = "Cuisine rating must be between 1 to 5")
    @JsonProperty
    private String cuisineRating;

    @NotBlank(message = "Reservation ID is required")
    @JsonProperty
    private String reservationId;

    @NotBlank(message = "Please provide service feedback")
    @JsonProperty
    private String serviceComment;

    @NotBlank(message = "Please provide service rating")
    @Pattern(regexp = "^[1-5]$", message = "Service rating must be between 1 to 5")
    @JsonProperty
    private String serviceRating;
}
