package com.epam.edp.demo.controller;

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.epam.edp.demo.dto.request.ReservationRequestDto;
import com.epam.edp.demo.dto.response.ReservationResponse;
import com.epam.edp.demo.exception.UnAuthorizedException;
import com.epam.edp.demo.exception.ValidationException;
import com.epam.edp.demo.service.ReservationService;
import com.epam.edp.demo.service.impl.AuthService;
import com.epam.edp.demo.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationControllerTest {

    @Mock
    private ReservationService reservationService;

    @Mock
    private AuthService authService;

    @Mock
    private Validation validation;

    @InjectMocks
    private ReservationController reservationController;

    private String validToken;
    private String validEmail;
    private String validReservationId;
    private List<ReservationResponse> mockReservations;
    private ReservationRequestDto validUpdateRequest;
    private ReservationResponse mockUpdatedReservation;

    @BeforeEach
    void setUp() {
        validToken = "valid_token";
        validEmail = "user@example.com";
        validReservationId = "res123";

        // Setup mock reservations
        mockReservations = Arrays.asList(
                ReservationResponse.builder()
                        .id("res123")
                        .status("CONFIRMED")
                        .locationAddress("123 Main St")
                        .date("2023-12-25")
                        .timeSlot("14:00 - 15:30")
                        .guestNumber("4")
                        .locationId("loc123")
                        .build(),
                ReservationResponse.builder()
                        .id("res456")
                        .status("CONFIRMED")
                        .locationAddress("456 Oak Ave")
                        .date("2023-12-26")
                        .timeSlot("18:00 - 19:30")
                        .guestNumber("2")
                        .locationId("loc456")
                        .build()
        );

        // Setup valid update request
        validUpdateRequest = ReservationRequestDto.builder()
                .locationId("loc123")
                .tableNumber("table1")
                .date("2023-12-25")
                .timeFrom("14:00")
                .timeTo("15:30")
                .guestsNumber("6") // Updated guest number
                .build();

        // Setup mock updated reservation
        mockUpdatedReservation = ReservationResponse.builder()
                .id(validReservationId)
                .status("CONFIRMED")
                .locationAddress("123 Main St")
                .date("2023-12-25")
                .timeSlot("14:00 - 15:30")
                .guestNumber("6") // Updated
                .locationId("loc123")
                .build();
    }

    @Test
    void getReservations_Success() {
        // Arrange
        String authHeader = "Bearer " + validToken;
        when(authService.extractUserEmailFromToken(validToken)).thenReturn(validEmail);
        when(reservationService.getAllReservation(validEmail)).thenReturn(mockReservations);

        // Act
        ResponseEntity<Map<String, Map<String, List<ReservationResponse>>>> response =
                reservationController.getReservations(authHeader);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("data"));
        assertTrue(response.getBody().get("data").containsKey("reservation"));
        assertEquals(mockReservations, response.getBody().get("data").get("reservation"));

        // Verify interactions
        verify(authService).extractUserEmailFromToken(validToken);
        verify(reservationService).getAllReservation(validEmail);
    }

    @Test
    void getReservations_MissingAuthHeader() {
        try {
            // Act
            reservationController.getReservations(null);
            fail("Expected UnAuthorizedException was not thrown");
        } catch (UnAuthorizedException e) {
            // Assert
            assertEquals("Authorization token is missing or invalid", e.getMessage());
            verifyNoInteractions(authService);
            verifyNoInteractions(reservationService);
        }
    }

    @Test
    void getReservations_InvalidAuthHeader() {
        // Arrange
        String invalidAuthHeader = "InvalidHeader";

        try {
            // Act
            reservationController.getReservations(invalidAuthHeader);
            fail("Expected UnAuthorizedException was not thrown");
        } catch (UnAuthorizedException e) {
            // Assert
            assertEquals("Authorization token is missing or invalid", e.getMessage());
            verifyNoInteractions(authService);
            verifyNoInteractions(reservationService);
        }
    }

    @Test
    void getReservations_AuthServiceThrowsException() {
        // Arrange
        String authHeader = "Bearer " + validToken;
        when(authService.extractUserEmailFromToken(validToken))
                .thenThrow(new RuntimeException("Token validation failed"));

        try {
            // Act
            reservationController.getReservations(authHeader);
            fail("Expected UnAuthorizedException was not thrown");
        } catch (UnAuthorizedException e) {
            // Assert
            assertEquals("Authorization token is missing or invalid", e.getMessage());
            verify(authService).extractUserEmailFromToken(validToken);
            verifyNoInteractions(reservationService);
        }
    }

    @Test
    void deleteReservation_Success() {
        // Arrange
        String authHeader = "Bearer " + validToken;
        when(authService.extractUserEmailFromToken(validToken)).thenReturn(validEmail);
        when(reservationService.deleteReservationOfUser(validReservationId)).thenReturn(true);

        // Act
        ResponseEntity<Map<String, String>> response =
                reservationController.getReservations(validReservationId, authHeader);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Deleted Reservation Successfully", response.getBody().get("message"));

        // Verify interactions
        verify(authService).extractUserEmailFromToken(validToken);
        verify(reservationService).deleteReservationOfUser(validReservationId);
    }

    @Test
    void deleteReservation_Failure() {
        // Arrange
        String authHeader = "Bearer " + validToken;
        when(authService.extractUserEmailFromToken(validToken)).thenReturn(validEmail);
        when(reservationService.deleteReservationOfUser(validReservationId)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, String>> response =
                reservationController.getReservations(validReservationId, authHeader);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Could'nt Cancel the Reservation", response.getBody().get("message"));

        // Verify interactions
        verify(authService).extractUserEmailFromToken(validToken);
        verify(reservationService).deleteReservationOfUser(validReservationId);
    }



    @Test
    void deleteReservation_MissingAuthHeader() {
        try {
            // Act
            reservationController.getReservations(validReservationId, null);
            fail("Expected UnAuthorizedException was not thrown");
        } catch (UnAuthorizedException e) {
            // Assert
            assertEquals("Authorization token is missing or invalid", e.getMessage());
            verifyNoInteractions(authService);
            verifyNoInteractions(reservationService);
        }
    }

    @Test
    void deleteReservation_InvalidAuthHeader() {
        // Arrange
        String invalidAuthHeader = "InvalidHeader";

        try {
            // Act
            reservationController.getReservations(validReservationId, invalidAuthHeader);
            fail("Expected UnAuthorizedException was not thrown");
        } catch (UnAuthorizedException e) {
            // Assert
            assertEquals("Authorization token is missing or invalid", e.getMessage());
            verifyNoInteractions(authService);
            verifyNoInteractions(reservationService);
        }
    }

    @Test
    void deleteReservation_AuthServiceThrowsException() {
        // Arrange
        String authHeader = "Bearer " + validToken;
        when(authService.extractUserEmailFromToken(validToken))
                .thenThrow(new RuntimeException("Token validation failed"));

        try {
            // Act
            reservationController.getReservations(validReservationId, authHeader);
            fail("Expected UnAuthorizedException was not thrown");
        } catch (UnAuthorizedException e) {
            // Assert
            assertEquals("Authorization token is missing or invalid", e.getMessage());
            verify(authService).extractUserEmailFromToken(validToken);
            verifyNoInteractions(reservationService);
        }
    }

    @Test
    void updateReservation_Success() {
        // Arrange
        String authHeader = "Bearer " + validToken;
        when(authService.extractUserEmailFromToken(validToken)).thenReturn(validEmail);
        doNothing().when(validation).validateTimeFromReservation(validUpdateRequest.getTimeFrom());
        doNothing().when(validation).validateGuests(validUpdateRequest.getGuestsNumber());
        doNothing().when(validation).validateTimeTo(validUpdateRequest.getTimeTo(), validUpdateRequest.getTimeFrom());
        when(reservationService.updateReservation(validReservationId, validEmail, validUpdateRequest))
                .thenReturn(mockUpdatedReservation);

        // Act
        ResponseEntity<Map<String, ReservationResponse>> response =
                reservationController.updateReservation(validReservationId, validUpdateRequest, authHeader);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("data"));
        assertEquals(mockUpdatedReservation, response.getBody().get("data"));

        // Verify interactions
        verify(authService).extractUserEmailFromToken(validToken);
        verify(validation).validateTimeFromReservation(validUpdateRequest.getTimeFrom());
        verify(validation).validateGuests(validUpdateRequest.getGuestsNumber());
        verify(validation).validateTimeTo(validUpdateRequest.getTimeTo(), validUpdateRequest.getTimeFrom());
        verify(reservationService).updateReservation(validReservationId, validEmail, validUpdateRequest);
    }


    @Test
    void updateReservation_ValidationFails_TimeFrom() {
        // Arrange
        String authHeader = "Bearer " + validToken;
        when(authService.extractUserEmailFromToken(validToken)).thenReturn(validEmail);
        doThrow(new ValidationException("Invalid time slot"))
                .when(validation).validateTimeFromReservation(validUpdateRequest.getTimeFrom());

        try {
            // Act
            reservationController.updateReservation(validReservationId, validUpdateRequest, authHeader);
            fail("Expected ValidationException was not thrown");
        } catch (ValidationException e) {
            // Assert
            assertEquals("Invalid time slot", e.getMessage());

            // Verify interactions
            verify(authService).extractUserEmailFromToken(validToken);
            verify(validation).validateTimeFromReservation(validUpdateRequest.getTimeFrom());
            verifyNoMoreInteractions(validation);
            verifyNoInteractions(reservationService);
        }
    }

    @Test
    void updateReservation_ValidationFails_Guests() {
        // Arrange
        String authHeader = "Bearer " + validToken;
        when(authService.extractUserEmailFromToken(validToken)).thenReturn(validEmail);
        doNothing().when(validation).validateTimeFromReservation(validUpdateRequest.getTimeFrom());
        doThrow(new ValidationException("Invalid number of guests"))
                .when(validation).validateGuests(validUpdateRequest.getGuestsNumber());

        try {
            // Act
            reservationController.updateReservation(validReservationId, validUpdateRequest, authHeader);
            fail("Expected ValidationException was not thrown");
        } catch (ValidationException e) {
            // Assert
            assertEquals("Invalid number of guests", e.getMessage());

            // Verify interactions
            verify(authService).extractUserEmailFromToken(validToken);
            verify(validation).validateTimeFromReservation(validUpdateRequest.getTimeFrom());
            verify(validation).validateGuests(validUpdateRequest.getGuestsNumber());
            verifyNoMoreInteractions(validation);
            verifyNoInteractions(reservationService);
        }
    }

    @Test
    void updateReservation_ValidationFails_TimeTo() {
        // Arrange
        String authHeader = "Bearer " + validToken;
        when(authService.extractUserEmailFromToken(validToken)).thenReturn(validEmail);
        doNothing().when(validation).validateTimeFromReservation(validUpdateRequest.getTimeFrom());
        doNothing().when(validation).validateGuests(validUpdateRequest.getGuestsNumber());
        doThrow(new ValidationException("Invalid end time"))
                .when(validation).validateTimeTo(validUpdateRequest.getTimeTo(), validUpdateRequest.getTimeFrom());

        try {
            // Act
            reservationController.updateReservation(validReservationId, validUpdateRequest, authHeader);
            fail("Expected ValidationException was not thrown");
        } catch (ValidationException e) {
            // Assert
            assertEquals("Invalid end time", e.getMessage());

            // Verify interactions
            verify(authService).extractUserEmailFromToken(validToken);
            verify(validation).validateTimeFromReservation(validUpdateRequest.getTimeFrom());
            verify(validation).validateGuests(validUpdateRequest.getGuestsNumber());
            verify(validation).validateTimeTo(validUpdateRequest.getTimeTo(), validUpdateRequest.getTimeFrom());
            verifyNoInteractions(reservationService);
        }
    }

    @Test
    void updateReservation_MissingAuthHeader() {
        try {
            // Act
            reservationController.updateReservation(validReservationId, validUpdateRequest, null);
            fail("Expected UnAuthorizedException was not thrown");
        } catch (UnAuthorizedException e) {
            // Assert
            assertEquals("Authorization token is missing or invalid", e.getMessage());
            verifyNoInteractions(authService);
            verifyNoInteractions(validation);
            verifyNoInteractions(reservationService);
        }
    }

    @Test
    void updateReservation_InvalidAuthHeader() {
        // Arrange
        String invalidAuthHeader = "InvalidHeader";

        try {
            // Act
            reservationController.updateReservation(validReservationId, validUpdateRequest, invalidAuthHeader);
            fail("Expected UnAuthorizedException was not thrown");
        } catch (UnAuthorizedException e) {
            // Assert
            assertEquals("Authorization token is missing or invalid", e.getMessage());
            verifyNoInteractions(authService);
            verifyNoInteractions(validation);
            verifyNoInteractions(reservationService);
        }
    }

    @Test
    void updateReservation_AuthServiceThrowsException() {
        // Arrange
        String authHeader = "Bearer " + validToken;
        when(authService.extractUserEmailFromToken(validToken))
                .thenThrow(new RuntimeException("Token validation failed"));

        try {
            // Act
            reservationController.updateReservation(validReservationId, validUpdateRequest, authHeader);
            fail("Expected UnAuthorizedException was not thrown");
        } catch (UnAuthorizedException e) {
            // Assert
            assertEquals("Authorization token is missing or invalid", e.getMessage());
            verify(authService).extractUserEmailFromToken(validToken);
            verifyNoInteractions(validation);
            verifyNoInteractions(reservationService);
        }
    }
}