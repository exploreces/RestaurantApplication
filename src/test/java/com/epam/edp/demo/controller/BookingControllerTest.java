package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.request.ReservationRequestDto;
import com.epam.edp.demo.dto.response.ReservationResponse;
import com.epam.edp.demo.dto.response.TableResponseDto;
import com.epam.edp.demo.exception.ResourceNotFoundException;
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
public class BookingControllerTest {

    @Mock
    private Validation validation;

    @Mock
    private ReservationService reservationService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private BookingController bookingController;

    private String validLocationId;
    private String validDate;
    private String validTime;
    private String validGuests;
    private List<TableResponseDto> mockTables;
    private ReservationRequestDto validReservationRequest;
    private ReservationResponse mockReservationResponse;
    private String validToken;
    private String validEmail;

    @BeforeEach
    void setUp() {
        validLocationId = "loc123";
        validDate = "2023-12-25";
        validTime = "14:00";
        validGuests = "4";

        // Setup mock tables with the correct structure
        mockTables = Arrays.asList(
                TableResponseDto.builder()
                        .locationId(validLocationId)
                        .locationAddress("123 Main St")
                        .tableNumber("table1")
                        .capacity("4")
                        .availableSlots(Arrays.asList("14:00", "15:45", "17:30"))
                        .build(),
                TableResponseDto.builder()
                        .locationId(validLocationId)
                        .locationAddress("123 Main St")
                        .tableNumber("table2")
                        .capacity("6")
                        .availableSlots(Arrays.asList("12:15", "14:00", "15:45"))
                        .build()
        );

        // Setup mock reservation request with the correct structure
        validReservationRequest = ReservationRequestDto.builder()
                .locationId(validLocationId)
                .tableNumber("table1")
                .date(validDate)
                .timeFrom(validTime)
                .timeTo("15:30")
                .guestsNumber(validGuests)
                .build();

        // Setup mock reservation response with the correct structure
        mockReservationResponse = ReservationResponse.builder()
                .id("res123")
                .status("CONFIRMED")
                .locationAddress("123 Main St")
                .date(validDate)
                .timeSlot(validTime + " - 15:30")
                .preOrder(null)
                .guestNumber(validGuests)
                .feedbackId(null)
                .locationId(validLocationId)
                .build();

        validToken = "valid_token";
        validEmail = "user@example.com";
    }

    @Test
    void getAvailableTable_Success() {
        // Arrange
        doNothing().when(validation).validateLocation(validLocationId);
        doNothing().when(validation).validateTimeFrom(validTime);
        doNothing().when(validation).validateGuests(validGuests);
        doNothing().when(validation).validateDate(validDate);
        when(reservationService.getAvailableSlots(validLocationId, validDate, validTime, validGuests))
                .thenReturn(mockTables);

        // Act
        ResponseEntity<Map<String, Map<String, List<TableResponseDto>>>> response =
                bookingController.getAvailableTable(validDate, validLocationId, validTime, validGuests);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("data"));
        assertTrue(response.getBody().get("data").containsKey("tables"));
        assertEquals(mockTables, response.getBody().get("data").get("tables"));

        // Verify validations were called
        verify(validation).validateLocation(validLocationId);
        verify(validation).validateTimeFrom(validTime);
        verify(validation).validateGuests(validGuests);
        verify(validation).validateDate(validDate);
        verify(reservationService).getAvailableSlots(validLocationId, validDate, validTime, validGuests);
    }

    @Test
    void getAvailableTable_ValidationFails_LocationId() {
        // Arrange
        doThrow(new ValidationException("Location ID is required"))
                .when(validation).validateLocation(null);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
                bookingController.getAvailableTable(validDate, null, validTime, validGuests));

        assertEquals("Location ID is required", exception.getMessage());
        verify(validation).validateLocation(null);
        verifyNoMoreInteractions(validation);
        verifyNoInteractions(reservationService);
    }





    @Test
    void getAvailableTable_LocationNotFound() {
        // Arrange
        doThrow(new ResourceNotFoundException("Location not found"))
                .when(validation).validateLocation(validLocationId);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                bookingController.getAvailableTable(validDate, validLocationId, validTime, validGuests));

        assertEquals("Location not found", exception.getMessage());
        verify(validation).validateLocation(validLocationId);
        verifyNoMoreInteractions(validation);
        verifyNoInteractions(reservationService);
    }

    @Test
    void createReservation_Success() {
        // Arrange
        String authHeader = "Bearer " + validToken;
        when(authService.extractUserEmailFromToken(validToken)).thenReturn(validEmail);
        doNothing().when(validation).validateReservationRequest(validReservationRequest);
        when(reservationService.createReservation(validReservationRequest, validEmail))
                .thenReturn(mockReservationResponse);

        // Act
        ResponseEntity<Map<String, ReservationResponse>> response =
                bookingController.createReservation(validReservationRequest, authHeader);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("data"));
        assertEquals(mockReservationResponse, response.getBody().get("data"));

        // Verify interactions
        verify(authService).extractUserEmailFromToken(validToken);
        verify(validation).validateReservationRequest(validReservationRequest);
        verify(reservationService).createReservation(validReservationRequest, validEmail);
    }

    @Test
    void createReservation_MissingAuthHeader() {
        // Act & Assert
        UnAuthorizedException exception = assertThrows(UnAuthorizedException.class, () ->
                bookingController.createReservation(validReservationRequest, null));

        assertEquals("Authorization token is missing or invalid", exception.getMessage());
        verifyNoInteractions(authService);
        verifyNoInteractions(validation);
        verifyNoInteractions(reservationService);
    }

    @Test
    void createReservation_InvalidAuthHeader() {
        // Arrange
        String invalidAuthHeader = "InvalidHeader";

        // Act & Assert
        UnAuthorizedException exception = assertThrows(UnAuthorizedException.class, () ->
                bookingController.createReservation(validReservationRequest, invalidAuthHeader));

        assertEquals("Authorization token is missing or invalid", exception.getMessage());
        verifyNoInteractions(authService);
        verifyNoInteractions(validation);
        verifyNoInteractions(reservationService);
    }

    @Test
    void createReservation_AuthServiceThrowsException() {
        // Arrange
        String authHeader = "Bearer " + validToken;
        when(authService.extractUserEmailFromToken(validToken))
                .thenThrow(new RuntimeException("Token validation failed"));

        // Act & Assert
        UnAuthorizedException exception = assertThrows(UnAuthorizedException.class, () ->
                bookingController.createReservation(validReservationRequest, authHeader));

        assertEquals("Authorization token is missing or invalid", exception.getMessage());
        verify(authService).extractUserEmailFromToken(validToken);
        verifyNoInteractions(validation);
        verifyNoInteractions(reservationService);
    }

    @Test
    void createReservation_ValidationFails() {
        // Arrange
        String authHeader = "Bearer " + validToken;
        when(authService.extractUserEmailFromToken(validToken)).thenReturn(validEmail);
        doThrow(new ValidationException("Invalid reservation data"))
                .when(validation).validateReservationRequest(validReservationRequest);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () ->
                bookingController.createReservation(validReservationRequest, authHeader));

        assertEquals("Invalid reservation data", exception.getMessage());
        verify(authService).extractUserEmailFromToken(validToken);
        verify(validation).validateReservationRequest(validReservationRequest);
        verifyNoInteractions(reservationService);
    }

    @Test
    void createReservation_ResourceNotFound() {
        // Arrange
        String authHeader = "Bearer " + validToken;
        when(authService.extractUserEmailFromToken(validToken)).thenReturn(validEmail);
        doNothing().when(validation).validateReservationRequest(validReservationRequest);
        when(reservationService.createReservation(validReservationRequest, validEmail))
                .thenThrow(new ResourceNotFoundException("Table not found"));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                bookingController.createReservation(validReservationRequest, authHeader));

        assertEquals("Table not found", exception.getMessage());
        verify(authService).extractUserEmailFromToken(validToken);
        verify(validation).validateReservationRequest(validReservationRequest);
        verify(reservationService).createReservation(validReservationRequest, validEmail);
    }
}