package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.request.AnonymousVisitorReservationDto;
import com.epam.edp.demo.dto.request.CustomerVerificationRequest;
import com.epam.edp.demo.dto.request.PostponeReservationDto;
import com.epam.edp.demo.dto.request.WaiterRequest;
import com.epam.edp.demo.dto.request.WaiterReservationRequestDto;
import com.epam.edp.demo.dto.response.ReservationResponse;
import com.epam.edp.demo.entity.Reservation;
import com.epam.edp.demo.exception.UnAuthorizedException;
import com.epam.edp.demo.exception.ValidationException;
import com.epam.edp.demo.service.impl.CustomerVerificationService;
import com.epam.edp.demo.service.ReservationService;
import com.epam.edp.demo.service.WaiterService;
import com.epam.edp.demo.service.impl.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WaiterControllerTest {

    @Mock
    private WaiterService waiterService;

    @Mock
    private CustomerVerificationService customerVerificationService;

    @Mock
    private ReservationService reservationService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private WaiterController waiterController;

    private String validToken;
    private String waiterEmail;
    private String customerEmail;
    private String customerName;
    private String reservationId;
    private WaiterReservationRequestDto validReservationRequest;
    private AnonymousVisitorReservationDto validAnonymousReservationRequest;
    private PostponeReservationDto validPostponeRequest;
    private Reservation existingReservation;
    private ReservationResponse reservationResponse;

    @BeforeEach
    void setUp() {
        validToken = "Bearer valid-token";
        waiterEmail = "waiter@example.com";
        customerEmail = "customer@example.com";
        customerName = "John Doe";
        reservationId = "reservation-123";

        // Setup valid reservation request
        validReservationRequest = new WaiterReservationRequestDto();
        validReservationRequest.setLocationId("location-123");
        validReservationRequest.setTableNumber("table-1");
        validReservationRequest.setDate(LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        validReservationRequest.setTimeFrom("18:00");
        validReservationRequest.setTimeTo("20:00");
        validReservationRequest.setGuestsNumber("4");
        validReservationRequest.setCustomerEmail(customerEmail);

        // Setup valid anonymous reservation request
        validAnonymousReservationRequest = new AnonymousVisitorReservationDto();
        validAnonymousReservationRequest.setLocationId("location-123");
        validAnonymousReservationRequest.setTableNumber("table-1");
        validAnonymousReservationRequest.setDate(LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        validAnonymousReservationRequest.setTimeFrom("18:00");
        validAnonymousReservationRequest.setTimeTo("20:00");
        validAnonymousReservationRequest.setGuestsNumber("4");
        validAnonymousReservationRequest.setVisitorName("Anonymous Visitor");

        // Setup valid postpone request
        validPostponeRequest = new PostponeReservationDto();
        validPostponeRequest.setReservationId(reservationId);
        validPostponeRequest.setNewDate(LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE));
        validPostponeRequest.setNewTimeFrom("19:00");
        validPostponeRequest.setNewTimeTo("21:00");

        // Setup existing reservation
        existingReservation = new Reservation();
        existingReservation.setReservationId(reservationId);
        existingReservation.setUserId(customerEmail);
        existingReservation.setWaiterId(waiterEmail);
        existingReservation.setLocationId("location-123");
        existingReservation.setTableNumber("table-1");
        existingReservation.setDate(LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        existingReservation.setTimeFrom("18:00");
        existingReservation.setTimeTo("20:00");
        existingReservation.setGuestNumber("4");
        existingReservation.setStatus("CONFIRMED");

        // Setup reservation response
        reservationResponse = new ReservationResponse();
        reservationResponse.setId(reservationId);
        reservationResponse.setStatus("CONFIRMED");
        reservationResponse.setLocationAddress("123 Main St");
        reservationResponse.setDate(LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        reservationResponse.setTimeSlot("18:00 - 20:00");
        reservationResponse.setGuestNumber("4");
    }

    // Tests for getAllWaiters
    @Test
    void getAllWaiters_Success() {
        // Arrange
        List<String> waitersList = Arrays.asList("waiter1@example.com", "waiter2@example.com");
        when(waiterService.getAllWaiters()).thenReturn(waitersList);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.getAllWaiters();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("waiters"));
        assertEquals(waitersList, response.getBody().get("waiters"));
        verify(waiterService).getAllWaiters();
    }

    @Test
    void getAllWaiters_Exception() {
        // Arrange
        when(waiterService.getAllWaiters()).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.getAllWaiters();

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("error"));
        assertTrue(response.getBody().get("error").toString().contains("Error getting waiters"));
        verify(waiterService).getAllWaiters();
    }

    // Tests for addWaiter
    @Test
    void addWaiter_Success() {
        // Arrange
        WaiterRequest request = new WaiterRequest();
        request.setEmail("newwaiter@example.com");
        request.setPassword("password123");
        doNothing().when(waiterService).addWaiter(anyString(), anyString());

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.addWaiter(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Waiter added successfully", response.getBody().get("message"));
        assertEquals("newwaiter@example.com", response.getBody().get("email"));
        verify(waiterService).addWaiter("newwaiter@example.com", "password123");
    }

    @Test
    void addWaiter_InvalidRequest() {
        // Arrange
        WaiterRequest request = new WaiterRequest();
        // Email is missing

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.addWaiter(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("error"));
        verify(waiterService, never()).addWaiter(anyString(), anyString());
    }

    @Test
    void addWaiter_Exception() {
        // Arrange
        WaiterRequest request = new WaiterRequest();
        request.setEmail("newwaiter@example.com");
        request.setPassword("password123");
        doThrow(new RuntimeException("Database error")).when(waiterService).addWaiter(anyString(), anyString());

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.addWaiter(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("error"));
        assertTrue(response.getBody().get("error").toString().contains("Error adding waiter"));
        verify(waiterService).addWaiter("newwaiter@example.com", "password123");
    }

    // Tests for removeWaiter
    @Test
    void removeWaiter_Success() {
        // Arrange
        String email = "waiter@example.com";
        doNothing().when(waiterService).removeWaiter(email);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.removeWaiter(email);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Waiter removed successfully", response.getBody().get("message"));
        assertEquals(email, response.getBody().get("email"));
        verify(waiterService).removeWaiter(email);
    }

    @Test
    void removeWaiter_Exception() {
        // Arrange
        String email = "waiter@example.com";
        doThrow(new RuntimeException("Database error")).when(waiterService).removeWaiter(email);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.removeWaiter(email);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("error"));
        assertTrue(response.getBody().get("error").toString().contains("Error removing waiter"));
        verify(waiterService).removeWaiter(email);
    }

    // Tests for verifyCustomer
    @Test
    void verifyCustomer_Success_CustomerExists() {
        // Arrange
        CustomerVerificationRequest request = new CustomerVerificationRequest();
        request.setEmail(customerEmail);
        when(customerVerificationService.isCustomer(customerEmail)).thenReturn(true);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.verifyCustomer(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("isCustomer"));
        assertEquals(customerEmail, response.getBody().get("email"));
        verify(customerVerificationService).isCustomer(customerEmail);
    }

    @Test
    void verifyCustomer_Success_CustomerDoesNotExist() {
        // Arrange
        CustomerVerificationRequest request = new CustomerVerificationRequest();
        request.setEmail(customerEmail);
        when(customerVerificationService.isCustomer(customerEmail)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.verifyCustomer(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().get("isCustomer"));
        assertEquals(customerEmail, response.getBody().get("email"));
        verify(customerVerificationService).isCustomer(customerEmail);
    }

    @Test
    void verifyCustomer_InvalidRequest() {
        // Arrange
        CustomerVerificationRequest request = new CustomerVerificationRequest();
        // Email is missing

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.verifyCustomer(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("error"));
        verify(customerVerificationService, never()).isCustomer(anyString());
    }

    @Test
    void verifyCustomer_Exception() {
        // Arrange
        CustomerVerificationRequest request = new CustomerVerificationRequest();
        request.setEmail(customerEmail);
        when(customerVerificationService.isCustomer(customerEmail)).thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.verifyCustomer(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("error"));
        assertTrue(response.getBody().get("error").toString().contains("Error verifying customer"));
        verify(customerVerificationService).isCustomer(customerEmail);
    }

    // Tests for createReservationForCustomer
    @Test
    void createReservationForCustomer_Success() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);
        when(customerVerificationService.isCustomer(customerEmail)).thenReturn(true);
        when(customerVerificationService.getCustomerFullName(customerEmail)).thenReturn(customerName);
        when(reservationService.createReservationByWaiter(any(WaiterReservationRequestDto.class), eq(customerEmail), eq(waiterEmail)))
                .thenReturn(reservationResponse);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.createReservationForCustomer(validReservationRequest, validToken);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("reservation"));
        assertTrue(response.getBody().containsKey("message"));

        Map<String, Object> reservationMap = (Map<String, Object>) response.getBody().get("reservation");
        assertEquals(reservationId, reservationMap.get("id"));
        assertEquals(customerName, reservationMap.get("userInfo"));
        assertEquals("table-1", reservationMap.get("tableNumber"));
        assertEquals(false, reservationMap.get("isAnonymous"));

        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(customerVerificationService).isCustomer(customerEmail);
        verify(customerVerificationService).getCustomerFullName(customerEmail);
        verify(reservationService).createReservationByWaiter(any(WaiterReservationRequestDto.class), eq(customerEmail), eq(waiterEmail));
    }

    @Test
    void createReservationForCustomer_InvalidToken() {
        // Arrange
        String invalidToken = "Invalid token format";

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.createReservationForCustomer(validReservationRequest, invalidToken);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("Authorization token is missing or invalid"));
        verify(authService, never()).extractUserEmailFromToken(anyString());
    }

    @Test
    void createReservationForCustomer_NotWaiter() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.createReservationForCustomer(validReservationRequest, validToken);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("not belong to a registered waiter"));
        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(customerVerificationService, never()).isCustomer(anyString());
    }

    @Test
    void createReservationForCustomer_MissingCustomerEmail() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);

        WaiterReservationRequestDto requestWithoutEmail = new WaiterReservationRequestDto();
        requestWithoutEmail.setLocationId("location-123");
        requestWithoutEmail.setTableNumber("table-1");
        requestWithoutEmail.setDate(LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        requestWithoutEmail.setTimeFrom("18:00");
        requestWithoutEmail.setTimeTo("20:00");
        requestWithoutEmail.setGuestsNumber("4");
        // Customer email is missing

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.createReservationForCustomer(requestWithoutEmail, validToken);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("Customer email is required"));
        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(customerVerificationService, never()).isCustomer(anyString());
    }

    @Test
    void createReservationForCustomer_NotACustomer() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);
        when(customerVerificationService.isCustomer(customerEmail)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.createReservationForCustomer(validReservationRequest, validToken);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("does not belong to a registered customer"));
        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(customerVerificationService).isCustomer(customerEmail);
        verify(customerVerificationService, never()).getCustomerFullName(anyString());
    }

    @Test
    void createReservationForCustomer_ValidationError() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);
        when(customerVerificationService.isCustomer(customerEmail)).thenReturn(true);

        // Create an invalid request with past date
        WaiterReservationRequestDto invalidRequest = new WaiterReservationRequestDto();
        invalidRequest.setLocationId("location-123");
        invalidRequest.setTableNumber("table-1");
        invalidRequest.setDate("2020-01-01"); // Past date
        invalidRequest.setTimeFrom("18:00");
        invalidRequest.setTimeTo("20:00");
        invalidRequest.setGuestsNumber("4");
        invalidRequest.setCustomerEmail(customerEmail);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.createReservationForCustomer(invalidRequest, validToken);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("Cannot create reservation for a date and time that has already passed"));
        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(customerVerificationService).isCustomer(customerEmail);
        verify(reservationService, never()).createReservationByWaiter(any(), anyString(), anyString());
    }

    // Tests for getWaiterReservations
    @Test
    void getWaiterReservations_Success() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);
        when(reservationService.getReservationsByWaiter(waiterEmail)).thenReturn(Collections.singletonList(reservationResponse));
        when(reservationService.findReservationById(reservationId)).thenReturn(existingReservation);
        when(customerVerificationService.getCustomerFullName(customerEmail)).thenReturn(customerName);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.getWaiterReservations(validToken);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("reservations"));
        assertTrue(response.getBody().containsKey("count"));
        assertEquals(1, response.getBody().get("count"));

        List<Map<String, Object>> reservations = (List<Map<String, Object>>) response.getBody().get("reservations");
        assertEquals(1, reservations.size());
        Map<String, Object> reservation = reservations.get(0);
        assertEquals(reservationId, reservation.get("id"));
        assertEquals(customerName, reservation.get("userInfo"));
        assertEquals(false, reservation.get("isAnonymous"));

        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService).getReservationsByWaiter(waiterEmail);
        verify(reservationService).findReservationById(reservationId);
        verify(customerVerificationService).getCustomerFullName(customerEmail);
    }

    @Test
    void getWaiterReservations_WithAnonymousReservation() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);
        when(reservationService.getReservationsByWaiter(waiterEmail)).thenReturn(Collections.singletonList(reservationResponse));

        // Create an anonymous reservation
        Reservation anonymousReservation = new Reservation();
        anonymousReservation.setReservationId(reservationId);
        anonymousReservation.setUserId("ANONYMOUS:Anonymous Visitor");
        anonymousReservation.setWaiterId(waiterEmail);
        anonymousReservation.setTableNumber("table-1");

        when(reservationService.findReservationById(reservationId)).thenReturn(anonymousReservation);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.getWaiterReservations(validToken);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        List<Map<String, Object>> reservations = (List<Map<String, Object>>) response.getBody().get("reservations");
        assertEquals(1, reservations.size());
        Map<String, Object> reservation = reservations.get(0);
        assertEquals(reservationId, reservation.get("id"));
        assertEquals("Anonymous Visitor", reservation.get("userInfo"));
        assertEquals(true, reservation.get("isAnonymous"));

        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService).getReservationsByWaiter(waiterEmail);
        verify(reservationService).findReservationById(reservationId);
        verify(customerVerificationService, never()).getCustomerFullName(anyString());
    }

    @Test
    void getWaiterReservations_InvalidToken() {
        // Arrange
        String invalidToken = "Invalid token format";

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.getWaiterReservations(invalidToken);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("Authorization token is missing or invalid"));
        verify(authService, never()).extractUserEmailFromToken(anyString());
    }

    @Test
    void getWaiterReservations_NotWaiter() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.getWaiterReservations(validToken);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("not belong to a registered waiter"));
        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService, never()).getReservationsByWaiter(anyString());
    }

    // Tests for cancelReservation
    @Test
    void cancelReservation_Success() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);
        when(reservationService.findReservationById(reservationId)).thenReturn(existingReservation);
        when(reservationService.cancelReservation(reservationId)).thenReturn(true);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.cancelReservation(reservationId, validToken);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Reservation cancelled successfully", response.getBody().get("message"));
        assertEquals(reservationId, response.getBody().get("reservationId"));

        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService).findReservationById(reservationId);
        verify(reservationService).cancelReservation(reservationId);
    }

    @Test
    void cancelReservation_InvalidToken() {
        // Arrange
        String invalidToken = "Invalid token format";

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.cancelReservation(reservationId, invalidToken);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("Authorization token is missing or invalid"));
        verify(authService, never()).extractUserEmailFromToken(anyString());
    }

    @Test
    void cancelReservation_NotWaiter() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.cancelReservation(reservationId, validToken);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("not belong to a registered waiter"));
        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService, never()).findReservationById(anyString());
    }

    @Test
    void cancelReservation_ReservationNotFound() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);
        when(reservationService.findReservationById(reservationId)).thenReturn(null);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.cancelReservation(reservationId, validToken);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("Reservation not found"));
        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService).findReservationById(reservationId);
        verify(reservationService, never()).cancelReservation(anyString());
    }

    @Test
    void cancelReservation_NotAuthorized() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);

        // Create a reservation belonging to a different waiter
        Reservation otherWaiterReservation = new Reservation();
        otherWaiterReservation.setReservationId(reservationId);
        otherWaiterReservation.setWaiterId("other-waiter@example.com");

        when(reservationService.findReservationById(reservationId)).thenReturn(otherWaiterReservation);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.cancelReservation(reservationId, validToken);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("not authorized to cancel this reservation"));

        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService).findReservationById(reservationId);
        verify(reservationService, never()).cancelReservation(anyString());
    }

    @Test
    void cancelReservation_CancellationFailed() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);
        when(reservationService.findReservationById(reservationId)).thenReturn(existingReservation);
        when(reservationService.cancelReservation(reservationId)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.cancelReservation(reservationId, validToken);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("Failed to cancel reservation"));

        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService).findReservationById(reservationId);
        verify(reservationService).cancelReservation(reservationId);
    }

    // Tests for createAnonymousReservation
    @Test
    void createAnonymousReservation_Success() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);
        when(reservationService.createAnonymousReservation(any(AnonymousVisitorReservationDto.class), anyString(), eq(waiterEmail)))
                .thenReturn(reservationResponse);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.createAnonymousReservation(validAnonymousReservationRequest, validToken);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("reservation"));
        assertTrue(response.getBody().containsKey("message"));

        Map<String, Object> reservationMap = (Map<String, Object>) response.getBody().get("reservation");
        assertEquals(reservationId, reservationMap.get("id"));
        assertEquals("Anonymous Visitor", reservationMap.get("userInfo"));
        assertEquals("table-1", reservationMap.get("tableNumber"));
        assertEquals(true, reservationMap.get("isAnonymous"));

        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService).createAnonymousReservation(any(AnonymousVisitorReservationDto.class), eq("Anonymous Visitor"), eq(waiterEmail));
    }

    @Test
    void createAnonymousReservation_InvalidToken() {
        // Arrange
        String invalidToken = "Invalid token format";

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.createAnonymousReservation(validAnonymousReservationRequest, invalidToken);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("Authorization token is missing or invalid"));
        verify(authService, never()).extractUserEmailFromToken(anyString());
    }

    @Test
    void createAnonymousReservation_NotWaiter() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.createAnonymousReservation(validAnonymousReservationRequest, validToken);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("not belong to a registered waiter"));
        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService, never()).createAnonymousReservation(any(), anyString(), anyString());
    }

    @Test
    void createAnonymousReservation_MissingVisitorName() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);

        AnonymousVisitorReservationDto requestWithoutName = new AnonymousVisitorReservationDto();
        requestWithoutName.setLocationId("location-123");
        requestWithoutName.setTableNumber("table-1");
        requestWithoutName.setDate(LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        requestWithoutName.setTimeFrom("18:00");
        requestWithoutName.setTimeTo("20:00");
        requestWithoutName.setGuestsNumber("4");
        // Visitor name is missing

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.createAnonymousReservation(requestWithoutName, validToken);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("Visitor name is required"));
        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService, never()).createAnonymousReservation(any(), anyString(), anyString());
    }

    @Test
    void createAnonymousReservation_ValidationError() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);

        // Create an invalid request with past date
        AnonymousVisitorReservationDto invalidRequest = new AnonymousVisitorReservationDto();
        invalidRequest.setLocationId("location-123");
        invalidRequest.setTableNumber("table-1");
        invalidRequest.setDate("2020-01-01"); // Past date
        invalidRequest.setTimeFrom("18:00");
        invalidRequest.setTimeTo("20:00");
        invalidRequest.setGuestsNumber("4");
        invalidRequest.setVisitorName("Anonymous Visitor");

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.createAnonymousReservation(invalidRequest, validToken);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("Cannot create reservation for a date and time that has already passed"));
        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService, never()).createAnonymousReservation(any(), anyString(), anyString());
    }

    // Tests for postponeReservation
    @Test
    void postponeReservation_Success() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);
        when(reservationService.findReservationById(reservationId)).thenReturn(existingReservation);
        when(reservationService.postponeReservation(eq(reservationId), anyString(), anyString(), anyString()))
                .thenReturn(reservationResponse);
        when(customerVerificationService.getCustomerFullName(customerEmail)).thenReturn(customerName);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.postponeReservation(validPostponeRequest, validToken);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("reservation"));
        assertTrue(response.getBody().containsKey("message"));
        assertEquals("Reservation successfully postponed", response.getBody().get("message"));

        Map<String, Object> reservationMap = (Map<String, Object>) response.getBody().get("reservation");
        assertEquals(reservationId, reservationMap.get("id"));
        assertEquals(customerName, reservationMap.get("userInfo"));
        assertEquals("table-1", reservationMap.get("tableNumber"));
        assertEquals(false, reservationMap.get("isAnonymous"));

        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService).findReservationById(reservationId);
        verify(reservationService).postponeReservation(
                eq(reservationId),
                eq(validPostponeRequest.getNewDate()),
                eq(validPostponeRequest.getNewTimeFrom()),
                eq(validPostponeRequest.getNewTimeTo())
        );
        verify(customerVerificationService).getCustomerFullName(customerEmail);
    }

    @Test
    void postponeReservation_WithAnonymousReservation() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);

        // Create an anonymous reservation
        Reservation anonymousReservation = new Reservation();
        anonymousReservation.setReservationId(reservationId);
        anonymousReservation.setUserId("ANONYMOUS:Anonymous Visitor");
        anonymousReservation.setWaiterId(waiterEmail);
        anonymousReservation.setTableNumber("table-1");

        when(reservationService.findReservationById(reservationId)).thenReturn(anonymousReservation);
        when(reservationService.postponeReservation(eq(reservationId), anyString(), anyString(), anyString()))
                .thenReturn(reservationResponse);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.postponeReservation(validPostponeRequest, validToken);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> reservationMap = (Map<String, Object>) response.getBody().get("reservation");
        assertEquals(reservationId, reservationMap.get("id"));
        assertEquals("Anonymous Visitor", reservationMap.get("userInfo"));
        assertEquals(true, reservationMap.get("isAnonymous"));

        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService).findReservationById(reservationId);
        verify(reservationService).postponeReservation(
                eq(reservationId),
                eq(validPostponeRequest.getNewDate()),
                eq(validPostponeRequest.getNewTimeFrom()),
                eq(validPostponeRequest.getNewTimeTo())
        );
        verify(customerVerificationService, never()).getCustomerFullName(anyString());
    }

    @Test
    void postponeReservation_InvalidToken() {
        // Arrange
        String invalidToken = "Invalid token format";

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.postponeReservation(validPostponeRequest, invalidToken);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("Authorization token is missing or invalid"));
        verify(authService, never()).extractUserEmailFromToken(anyString());
    }

    @Test
    void postponeReservation_NotWaiter() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.postponeReservation(validPostponeRequest, validToken);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("not belong to a registered waiter"));
        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService, never()).findReservationById(anyString());
    }

    @Test
    void postponeReservation_MissingReservationId() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);

        PostponeReservationDto requestWithoutId = new PostponeReservationDto();
        requestWithoutId.setNewDate(LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE));
        requestWithoutId.setNewTimeFrom("19:00");
        requestWithoutId.setNewTimeTo("21:00");
        // Reservation ID is missing

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.postponeReservation(requestWithoutId, validToken);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("Reservation ID is required"));
        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService, never()).findReservationById(anyString());
    }

    @Test
    void postponeReservation_ValidationError() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);

        // Create an invalid request with past date
        PostponeReservationDto invalidRequest = new PostponeReservationDto();
        invalidRequest.setReservationId(reservationId);
        invalidRequest.setNewDate("2020-01-01"); // Past date
        invalidRequest.setNewTimeFrom("19:00");
        invalidRequest.setNewTimeTo("21:00");

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.postponeReservation(invalidRequest, validToken);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("Cannot postpone reservation to a date and time that has already passed"));
        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService, never()).findReservationById(anyString());
    }

    @Test
    void postponeReservation_ReservationNotFound() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);
        when(reservationService.findReservationById(reservationId)).thenReturn(null);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.postponeReservation(validPostponeRequest, validToken);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("Reservation not found"));
        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService).findReservationById(reservationId);
        verify(reservationService, never()).postponeReservation(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void postponeReservation_NotAuthorized() {
        // Arrange
        when(authService.extractUserEmailFromToken("valid-token")).thenReturn(waiterEmail);
        when(waiterService.isWaiter(waiterEmail)).thenReturn(true);

        // Create a reservation belonging to a different waiter
        Reservation otherWaiterReservation = new Reservation();
        otherWaiterReservation.setReservationId(reservationId);
        otherWaiterReservation.setWaiterId("other-waiter@example.com");

        when(reservationService.findReservationById(reservationId)).thenReturn(otherWaiterReservation);

        // Act
        ResponseEntity<Map<String, Object>> response = waiterController.postponeReservation(validPostponeRequest, validToken);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertTrue(response.getBody().get("message").toString().contains("not authorized to modify this reservation"));

        verify(authService).extractUserEmailFromToken("valid-token");
        verify(waiterService).isWaiter(waiterEmail);
        verify(reservationService).findReservationById(reservationId);
        verify(reservationService, never()).postponeReservation(anyString(), anyString(), anyString(), anyString());
    }
}