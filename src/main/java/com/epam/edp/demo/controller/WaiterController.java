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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/api/waiters")
@Tag(name = "Waiter Management", description = "APIs for waiter operations")
public class WaiterController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private WaiterService waiterService;

    @Autowired
    private CustomerVerificationService customerVerificationService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private AuthService authService;

    /**
     * Get all waiters in the system
     */
    @GetMapping
    @Operation(summary = "Get all waiters", description = "Returns a list of all waiters in the system")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> getAllWaiters() {
        try {
            logger.info("Getting all waiters");
            List<String> waiters = waiterService.getAllWaiters();

            Map<String, Object> response = new HashMap<>();
            response.put("waiters", waiters);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting all waiters: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error getting waiters: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Add a new waiter
     */
    @PostMapping
    @Operation(summary = "Add a new waiter", description = "Adds a new waiter to the system")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> addWaiter(@RequestBody WaiterRequest waiterRequest) {
        try {
            logger.info("Adding waiter");

            // Validate request
            if (waiterRequest == null || waiterRequest.getEmail() == null || waiterRequest.getPassword() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
            }

            // Add waiter with password
            waiterService.addWaiter(waiterRequest.getEmail(), waiterRequest.getPassword());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Waiter added successfully");
            response.put("email", waiterRequest.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error adding waiter: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error adding waiter: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Remove a waiter
     */
    @DeleteMapping("/{email}")
    @Operation(summary = "Remove a waiter", description = "Removes a waiter from the system")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> removeWaiter(@PathVariable String email) {
        try {
            logger.info("Removing waiter: {}", email);

            waiterService.removeWaiter(email);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Waiter removed successfully");
            response.put("email", email);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error removing waiter: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error removing waiter: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Verify if an email belongs to a customer
     */
    @PostMapping("/verify-customer")
    @Operation(summary = "Verify customer", description = "Verifies if an email belongs to a registered customer")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> verifyCustomer(@RequestBody CustomerVerificationRequest verificationRequest) {
        try {
            logger.info("Received customer verification request");

            if (verificationRequest == null || verificationRequest.getEmail() == null || verificationRequest.getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }

            String email = verificationRequest.getEmail();
            boolean isCustomer = customerVerificationService.isCustomer(email);
            logger.info("Customer verification result for {}: {}", email, isCustomer);

            Map<String, Object> response = new HashMap<>();
            response.put("isCustomer", isCustomer);
            response.put("email", email);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing customer verification request: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error verifying customer: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Create a reservation for an existing customer
     */
    @PostMapping("/reservations/customer")
    @Operation(summary = "Create customer reservation", description = "Creates a reservation for an existing customer")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> createReservationForCustomer(
            @RequestBody WaiterReservationRequestDto reservationRequestDto,
            @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("Received request for waiter to create reservation for customer");

            // Extract token and validate waiter
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new UnAuthorizedException("Authorization token is missing or invalid");
            }

            String token = authHeader.substring(7);
            String waiterEmail = authService.extractUserEmailFromToken(token);

            if (waiterEmail == null || !waiterService.isWaiter(waiterEmail)) {
                logger.warn("Email {} does not belong to a waiter", waiterEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Provided token does not belong to a registered waiter"));
            }

            // Extract customer email from request
            String customerEmail = reservationRequestDto.getCustomerEmail();
            if (customerEmail == null || customerEmail.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Customer email is required"));
            }

            // Verify the email belongs to a customer
            if (!customerVerificationService.isCustomer(customerEmail)) {
                logger.warn("Email {} does not belong to a registered customer", customerEmail);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Provided email does not belong to a registered customer"));
            }

            // Validate the reservation request
            validateWaiterReservationRequest(reservationRequestDto);

            // Get customer's full name
            String userFullName = customerVerificationService.getCustomerFullName(customerEmail);
            logger.info("Retrieved customer name for {}: {}", customerEmail, userFullName);

            // Create the reservation
            ReservationResponse createdReservation = reservationService.createReservationByWaiter(
                    reservationRequestDto, customerEmail, waiterEmail);

            // Create response
            Map<String, Object> enhancedReservation = createEnhancedReservationResponse(
                    createdReservation,
                    userFullName,
                    reservationRequestDto.getTableNumber(),
                    false);

            Map<String, Object> response = new HashMap<>();
            response.put("reservation", enhancedReservation);
            response.put("message", "Reservation created successfully by waiter for the customer: " + customerEmail);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ValidationException e) {
            logger.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (UnAuthorizedException e) {
            logger.warn("Authorization error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating reservation for customer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error creating reservation: " + e.getMessage()));
        }
    }

    /**
     * Get all reservations for the authenticated waiter
     */
    @GetMapping("/reservations")
    @Operation(summary = "Get waiter's reservations", description = "Returns all reservations created by the authenticated waiter")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> getWaiterReservations(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            logger.info("Getting reservations for waiter");

            // Check if Authorization header exists
            if (authHeader == null || authHeader.isEmpty()) {
                logger.warn("Authorization header is missing");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Authorization token is missing or invalid"));
            }

            // Extract token and validate waiter
            if (!authHeader.startsWith("Bearer ")) {
                logger.warn("Invalid authorization format");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Authorization token is missing or invalid"));
            }

            String token = authHeader.substring(7);
            String waiterEmail = authService.extractUserEmailFromToken(token);

            if (waiterEmail == null || !waiterService.isWaiter(waiterEmail)) {
                logger.warn("Email {} does not belong to a waiter", waiterEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Provided token does not belong to a registered waiter"));
            }

            logger.info("Fetching reservations for waiter: {}", waiterEmail);

            // Get the reservations for this waiter
            List<ReservationResponse> reservations = reservationService.getReservationsByWaiter(waiterEmail);

            // Enhance each reservation with customer information
            List<Map<String, Object>> enhancedReservations = new ArrayList<>();
            for (ReservationResponse reservation : reservations) {
                // Get the original reservation to access the userId (customer email or anonymous identifier)
                Reservation originalReservation = reservationService.findReservationById(reservation.getId());
                if (originalReservation != null) {
                    String userId = originalReservation.getUserId();

                    // Check if this is an anonymous reservation
                    boolean isAnonymous = userId != null && userId.startsWith("ANONYMOUS:");
                    String userInfo = isAnonymous
                            ? userId.substring("ANONYMOUS:".length())
                            : customerVerificationService.getCustomerFullName(userId);

                    Map<String, Object> enhancedReservation = createEnhancedReservationResponse(
                            reservation,
                            userInfo,
                            originalReservation.getTableNumber(),
                            isAnonymous);

                    enhancedReservations.add(enhancedReservation);
                }
            }

            // Return the response
            Map<String, Object> response = new HashMap<>();
            response.put("reservations", enhancedReservations);
            response.put("count", enhancedReservations.size());

            return ResponseEntity.ok(response);
        } catch (UnAuthorizedException e) {
            logger.warn("Authorization error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error getting waiter reservations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving reservations: " + e.getMessage()));
        }
    }

    /**
     * Cancel a reservation
     */
    @DeleteMapping("/reservations/{id}")
    @Operation(summary = "Cancel reservation", description = "Cancels a reservation created by the authenticated waiter")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> cancelReservation(
            @PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("Processing waiter reservation cancellation request for ID: {}", id);

            // Extract token and validate waiter
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new UnAuthorizedException("Authorization token is missing or invalid");
            }

            String token = authHeader.substring(7);
            String waiterEmail = authService.extractUserEmailFromToken(token);

            if (waiterEmail == null || !waiterService.isWaiter(waiterEmail)) {
                logger.warn("Email {} does not belong to a waiter", waiterEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Provided token does not belong to a registered waiter"));
            }

            // Get the reservation
            Reservation reservation = reservationService.findReservationById(id);
            if (reservation == null) {
                logger.warn("Reservation with ID {} not found", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Reservation not found"));
            }

            // Verify the reservation was created by this waiter
            if (!waiterEmail.equals(reservation.getWaiterId())) {
                logger.warn("Waiter {} is not authorized to cancel reservation {}", waiterEmail, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You are not authorized to cancel this reservation"));
            }

            // Cancel the reservation
            boolean cancelled = reservationService.cancelReservation(id);
            if (!cancelled) {
                logger.error("Failed to cancel reservation {}", id);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Failed to cancel reservation"));
            }

            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Reservation cancelled successfully");
            response.put("reservationId", id);

            return ResponseEntity.ok(response);
        } catch (UnAuthorizedException e) {
            logger.warn("Authorization error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error cancelling reservation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error cancelling reservation: " + e.getMessage()));
        }
    }

    /**
     * Create a reservation for an anonymous visitor
     */
    @PostMapping("/reservations/anonymous")
    @Operation(summary = "Create anonymous reservation", description = "Creates a reservation for an anonymous visitor")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> createAnonymousReservation(
            @RequestBody AnonymousVisitorReservationDto reservationRequestDto,
            @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("Received request for waiter to create reservation for anonymous visitor");

            // Extract token and validate waiter
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new UnAuthorizedException("Authorization token is missing or invalid");
            }

            String token = authHeader.substring(7);
            String waiterEmail = authService.extractUserEmailFromToken(token);

            if (waiterEmail == null || !waiterService.isWaiter(waiterEmail)) {
                logger.warn("Email {} does not belong to a waiter", waiterEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Provided token does not belong to a registered waiter"));
            }

            // Extract visitor name from request
            String visitorName = reservationRequestDto.getVisitorName();
            if (visitorName == null || visitorName.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Visitor name is required"));
            }

            // Validate the reservation request
            validateAnonymousReservationRequest(reservationRequestDto);

            // Create the reservation for the anonymous visitor
            ReservationResponse createdReservation = reservationService.createAnonymousReservation(
                    reservationRequestDto, visitorName, waiterEmail);

            // Create response
            Map<String, Object> enhancedReservation = createEnhancedReservationResponse(
                    createdReservation,
                    visitorName,
                    reservationRequestDto.getTableNumber(),
                    true);

            Map<String, Object> response = new HashMap<>();
            response.put("reservation", enhancedReservation);
            response.put("message", "Reservation created successfully by waiter for anonymous visitor: " + visitorName);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ValidationException e) {
            logger.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (UnAuthorizedException e) {
            logger.warn("Authorization error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating reservation for anonymous visitor: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error creating reservation: " + e.getMessage()));
        }
    }

    /**
     * Postpone a reservation
     */
    @PutMapping("/reservations/postpone")
    @Operation(summary = "Postpone reservation", description = "Postpones a reservation to a new date and time")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> postponeReservation(
            @RequestBody PostponeReservationDto postponeRequestDto,
            @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("Received request to postpone reservation");

            // Extract token and validate waiter
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new UnAuthorizedException("Authorization token is missing or invalid");
            }

            String token = authHeader.substring(7);
            String waiterEmail = authService.extractUserEmailFromToken(token);

            if (waiterEmail == null || !waiterService.isWaiter(waiterEmail)) {
                logger.warn("Email {} does not belong to a waiter", waiterEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Provided token does not belong to a registered waiter"));
            }

            // Extract reservation ID from request
            String reservationId = postponeRequestDto.getReservationId();
            if (reservationId == null || reservationId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Reservation ID is required"));
            }

            // Validate the new date and time
            validatePostponeRequest(postponeRequestDto);

            // Check if the reservation exists and belongs to this waiter
            Reservation existingReservation = reservationService.findReservationById(reservationId);
            if (existingReservation == null) {
                logger.warn("Reservation with ID {} not found", reservationId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Reservation not found"));
            }

            // Check if the reservation belongs to this waiter
            if (!existingReservation.getWaiterId().equals(waiterEmail)) {
                logger.warn("Reservation {} does not belong to waiter {}", reservationId, waiterEmail);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "You are not authorized to modify this reservation"));
            }

            // Postpone the reservation
            ReservationResponse updatedReservation = reservationService.postponeReservation(
                    reservationId,
                    postponeRequestDto.getNewDate(),
                    postponeRequestDto.getNewTimeFrom(),
                    postponeRequestDto.getNewTimeTo()
            );

            // Check if this is an anonymous reservation
            String userId = existingReservation.getUserId();
            boolean isAnonymous = userId != null && userId.startsWith("ANONYMOUS:");
            String userInfo = isAnonymous
                    ? userId.substring("ANONYMOUS:".length())
                    : customerVerificationService.getCustomerFullName(userId);

            // Create response
            Map<String, Object> enhancedReservation = createEnhancedReservationResponse(
                    updatedReservation,
                    userInfo,
                    existingReservation.getTableNumber(),
                    isAnonymous);

            Map<String, Object> response = new HashMap<>();
            response.put("reservation", enhancedReservation);
            response.put("message", "Reservation successfully postponed");

            return ResponseEntity.ok(response);
        } catch (ValidationException e) {
            logger.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (UnAuthorizedException e) {
            logger.warn("Authorization error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error postponing reservation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error postponing reservation: " + e.getMessage()));
        }
    }
    /**
     * Helper method to create an enhanced reservation response
     */
    private Map<String, Object> createEnhancedReservationResponse(
            ReservationResponse reservation,
            String userInfo,
            String tableNumber,
            boolean isAnonymous) {

        Map<String, Object> enhancedReservation = new HashMap<>();
        enhancedReservation.put("id", reservation.getId());
        enhancedReservation.put("status", reservation.getStatus());
        enhancedReservation.put("locationAddress", reservation.getLocationAddress());
        enhancedReservation.put("date", reservation.getDate());
        enhancedReservation.put("timeSlot", reservation.getTimeSlot());
        enhancedReservation.put("preOrder", reservation.getPreOrder());
        enhancedReservation.put("guestNumber", reservation.getGuestNumber());
        enhancedReservation.put("feedbackId", reservation.getFeedbackId());
        enhancedReservation.put("userInfo", userInfo);
        enhancedReservation.put("tableNumber", tableNumber);
        enhancedReservation.put("isAnonymous", isAnonymous);

        return enhancedReservation;
    }

    /**
     * Validates the waiter reservation request
     */
    private void validateWaiterReservationRequest(WaiterReservationRequestDto request) throws ValidationException {
        List<String> errors = new ArrayList<>();

        if (request.getLocationId() == null || request.getLocationId().isEmpty()) {
            errors.add("Location ID is required");
        }
        if (request.getTableNumber() == null || request.getTableNumber().isEmpty()) {
            errors.add("Table number is required");
        }
        if (request.getDate() == null || request.getDate().isEmpty()) {
            errors.add("Date is required");
        }
        if (request.getTimeFrom() == null || request.getTimeFrom().isEmpty()) {
            errors.add("Start time is required");
        }
        if (request.getTimeTo() == null || request.getTimeTo().isEmpty()) {
            errors.add("End time is required");
        }
        if (request.getGuestsNumber() == null || request.getGuestsNumber().isEmpty()) {
            errors.add("Guest number is required");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(String.join(", ", errors));
        }

        try {
            int guestNum = Integer.parseInt(request.getGuestsNumber());
            if (guestNum <= 0) {
                throw new ValidationException("Guest number must be greater than zero");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Guest number must be a valid number");
        }

        // Add validation for past date and time
        try {
            // Get current date and time
            LocalDateTime now = LocalDateTime.now();

            // Parse reservation date
            LocalDate reservationDate = LocalDate.parse(request.getDate());

            // Parse reservation time
            LocalTime reservationTime;
            try {
                // Try standard format first (e.g., "13:00")
                reservationTime = LocalTime.parse(request.getTimeFrom());
            } catch (DateTimeParseException e) {
                // Try alternative format (e.g., "1:00")
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
                    reservationTime = LocalTime.parse(request.getTimeFrom(), formatter);
                } catch (DateTimeParseException ex) {
                    throw new ValidationException("Invalid time format. Please use HH:MM format (e.g., 13:00)");
                }
            }

            // Combine date and time for easier comparison
            LocalDateTime reservationDateTime = LocalDateTime.of(reservationDate, reservationTime);

            // Check if reservation date/time is in the past
            if (reservationDateTime.isBefore(now)) {
                logger.warn("Attempted to create reservation for past date/time: {}", reservationDateTime);
                throw new ValidationException("Cannot create reservation for a date and time that has already passed");
            }

            logger.info("Date and time validation passed for reservation");
        } catch (DateTimeParseException e) {
            logger.error("Error parsing date or time: {}", e.getMessage());
            throw new ValidationException("Invalid date or time format. Date should be in YYYY-MM-DD format and time in HH:MM format");
        }
    }

    /**
     * Validates the anonymous visitor reservation request
     */
    private void validateAnonymousReservationRequest(AnonymousVisitorReservationDto request) throws ValidationException {
        List<String> errors = new ArrayList<>();

        if (request.getLocationId() == null || request.getLocationId().isEmpty()) {
            errors.add("Location ID is required");
        }
        if (request.getTableNumber() == null || request.getTableNumber().isEmpty()) {
            errors.add("Table number is required");
        }
        if (request.getDate() == null || request.getDate().isEmpty()) {
            errors.add("Date is required");
        }
        if (request.getTimeFrom() == null || request.getTimeFrom().isEmpty()) {
            errors.add("Start time is required");
        }
        if (request.getTimeTo() == null || request.getTimeTo().isEmpty()) {
            errors.add("End time is required");
        }
        if (request.getGuestsNumber() == null || request.getGuestsNumber().isEmpty()) {
            errors.add("Guest number is required");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(String.join(", ", errors));
        }

        // Additional validations - instead of calling validateWaiterReservationRequest
        try {
            int guestNum = Integer.parseInt(request.getGuestsNumber());
            if (guestNum <= 0) {
                throw new ValidationException("Guest number must be greater than zero");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Guest number must be a valid number");
        }

        // Add validation for past date and time
        try {
            // Get current date and time
            LocalDateTime now = LocalDateTime.now();

            // Parse reservation date
            LocalDate reservationDate = LocalDate.parse(request.getDate());

            // Parse reservation time
            LocalTime reservationTime;
            try {
                // Try standard format first (e.g., "13:00")
                reservationTime = LocalTime.parse(request.getTimeFrom());
            } catch (DateTimeParseException e) {
                // Try alternative format (e.g., "1:00")
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
                    reservationTime = LocalTime.parse(request.getTimeFrom(), formatter);
                } catch (DateTimeParseException ex) {
                    throw new ValidationException("Invalid time format. Please use HH:MM format (e.g., 13:00)");
                }
            }

            // Combine date and time for easier comparison
            LocalDateTime reservationDateTime = LocalDateTime.of(reservationDate, reservationTime);

            // Check if reservation date/time is in the past
            if (reservationDateTime.isBefore(now)) {
                logger.warn("Attempted to create reservation for past date/time: {}", reservationDateTime);
                throw new ValidationException("Cannot create reservation for a date and time that has already passed");
            }

            logger.info("Date and time validation passed for reservation");
        } catch (DateTimeParseException e) {
            logger.error("Error parsing date or time: {}", e.getMessage());
            throw new ValidationException("Invalid date or time format. Date should be in YYYY-MM-DD format and time in HH:MM format");
        }
    }

    /**
     * Validates the postpone reservation request
     */
    private void validatePostponeRequest(PostponeReservationDto request) throws ValidationException {
        List<String> errors = new ArrayList<>();

        if (request.getNewDate() == null || request.getNewDate().isEmpty()) {
            errors.add("New date is required");
        }
        if (request.getNewTimeFrom() == null || request.getNewTimeFrom().isEmpty()) {
            errors.add("New start time is required");
        }
        if (request.getNewTimeTo() == null || request.getNewTimeTo().isEmpty()) {
            errors.add("New end time is required");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(String.join(", ", errors));
        }

        // Validate the new date and time are not in the past
        try {
            // Get current date and time
            LocalDateTime now = LocalDateTime.now();

            // Parse new reservation date
            LocalDate newDate = LocalDate.parse(request.getNewDate());

            // Parse new reservation time
            LocalTime newTimeFrom;
            try {
                // Try standard format first (e.g., "13:00")
                newTimeFrom = LocalTime.parse(request.getNewTimeFrom());
            } catch (DateTimeParseException e) {
                // Try alternative format (e.g., "1:00")
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
                    newTimeFrom = LocalTime.parse(request.getNewTimeFrom(), formatter);
                } catch (DateTimeParseException ex) {
                    throw new ValidationException("Invalid time format. Please use HH:MM format (e.g., 13:00)");
                }
            }

            // Combine date and time for easier comparison
            LocalDateTime newDateTime = LocalDateTime.of(newDate, newTimeFrom);

            // Check if new reservation date/time is in the past
            if (newDateTime.isBefore(now)) {
                logger.warn("Attempted to postpone reservation to a past date/time: {}", newDateTime);
                throw new ValidationException("Cannot postpone reservation to a date and time that has already passed");
            }

            logger.info("Date and time validation passed for postpone request");
        } catch (DateTimeParseException e) {
            logger.error("Error parsing date or time: {}", e.getMessage());
            throw new ValidationException("Invalid date or time format. Date should be in YYYY-MM-DD format and time in HH:MM format");
        }
    }
}
