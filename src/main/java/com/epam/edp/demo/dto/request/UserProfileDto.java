package com.epam.edp.demo.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserProfileDto {
    @NotNull(message = "First Name should not be less than 5")
    @Min(value = 5)
    @Max(value = 50)
    private String firstName;

    @NotNull(message = "Last Name should not be less than 5")
    @Min(value = 5)
    @Max(value = 50)
    private String lastName;

    private String imageUrl;
}
