package com.epam.edp.demo.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PasswordUpdateRequest {
    @NotBlank(message = "please provide current password")
    private String currentPassword;

    @NotNull(message = "Password cannot be null")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,16}$",
            message = "Password must be 8-16 characters and contain at least one number, one uppercase letter, and one special character"
    )
    private String newPassword;

}