package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.request.PasswordUpdateRequest;
import com.epam.edp.demo.dto.request.SignInRequest;
import com.epam.edp.demo.dto.request.SignUpRequest;
import com.epam.edp.demo.dto.request.UserProfileDto;
import com.epam.edp.demo.dto.response.AuthResponse;
import com.epam.edp.demo.dto.response.SignInResponse;
import com.epam.edp.demo.dto.response.UserProfileResponse;
import com.epam.edp.demo.service.impl.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    public void testSignUp_Success() throws Exception {
        // Arrange
        SignUpRequest request = new SignUpRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        request.setFirstName("John");
        request.setLastName("Doe");

        AuthResponse expectedResponse = new AuthResponse("User registered successfully");
        when(authService.signup(any(SignUpRequest.class))).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        verify(authService, times(1)).signup(any(SignUpRequest.class));
    }

    @Test
    public void testSignIn_Success() throws Exception {
        // Arrange
        SignInRequest request = new SignInRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");

        SignInResponse response = new SignInResponse("jwt-token", "test@example.com", "CUSTOMER");
        when(authService.signin(any(SignInRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService, times(1)).signin(any(SignInRequest.class));
    }

    @Test
    public void testSignIn_BadCredentials() throws Exception {
        // Arrange
        SignInRequest request = new SignInRequest();
        request.setEmail("test@example.com");
        request.setPassword("WrongPassword123!");

        when(authService.signin(any(SignInRequest.class))).thenThrow(new BadCredentialsException("Invalid email or password"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        verify(authService, times(1)).signin(any(SignInRequest.class));
    }

    @Test
    public void testSignIn_IllegalArgument() throws Exception {
        // Arrange
        SignInRequest request = new SignInRequest();
        request.setEmail("");
        request.setPassword("Password123!");

        when(authService.signin(any(SignInRequest.class))).thenThrow(new IllegalArgumentException("Email is required"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is required"));

        verify(authService, times(1)).signin(any(SignInRequest.class));
    }


    @Test
    public void testDeleteUser_Success() throws Exception {
        // Arrange
        when(authService.extractUserEmailFromToken(anyString())).thenReturn("test@example.com");
        doNothing().when(authService).deleteUser("test@example.com");

        // Act & Assert
        mockMvc.perform(delete("/api/users/profile")
                        .header("Authorization", "Bearer mock-jwt-token"))
                .andExpect(status().isNoContent());

        verify(authService, times(1)).extractUserEmailFromToken("mock-jwt-token");
        verify(authService, times(1)).deleteUser("test@example.com");
    }

    @Test
    public void testUpdateUserPassword_Success() throws Exception {
        // Arrange
        PasswordUpdateRequest request = new PasswordUpdateRequest();
        request.setCurrentPassword("OldPassword123!");
        request.setNewPassword("NewPassword123!");

        when(authService.extractUserEmailFromToken(anyString())).thenReturn("test@example.com");
        doNothing().when(authService).updateUserPassword(
                eq("test@example.com"),
                eq("OldPassword123!"),
                eq("NewPassword123!")
        );

        // Act & Assert
        mockMvc.perform(put("/api/users/updatePassword")
                        .header("Authorization", "Bearer mock-jwt-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService, times(1)).extractUserEmailFromToken("mock-jwt-token");
        verify(authService, times(1)).updateUserPassword(
                eq("test@example.com"),
                eq("OldPassword123!"),
                eq("NewPassword123!")
        );
    }
}