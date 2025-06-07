package com.epam.edp.demo.service;


import com.epam.edp.demo.dto.request.ReservationRequestDto;
import com.epam.edp.demo.dto.request.WaiterReservationRequestDto;
import com.epam.edp.demo.dto.response.ReservationResponse;
import com.epam.edp.demo.dto.response.TableResponseDto;
import com.epam.edp.demo.entity.Location;
import com.epam.edp.demo.entity.Reservation;
import com.epam.edp.demo.entity.Table;
import com.epam.edp.demo.exception.ForbiddenException;
import com.epam.edp.demo.exception.ResourceNotFoundException;
import com.epam.edp.demo.exception.TooLateForCancellationException;
import com.epam.edp.demo.exception.ValidationException;
import com.epam.edp.demo.repository.LocationRepository;
import com.epam.edp.demo.repository.ReservationRepository;
import com.epam.edp.demo.repository.TableRepository;
import com.epam.edp.demo.repository.WaiterRepository;
import com.epam.edp.demo.service.impl.ReservationServiceImpl;
import com.epam.edp.demo.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private TableRepository tableRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private WaiterRepository waiterRepository;

    @Mock
    private Validation validation;


    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Reservation testReservation;
    private ReservationRequestDto testRequestDto;
    private Location testLocation;
    private Table testTable;
    private String testEmail;
    private String testReservationId;
    private String testLocationId;
    private String testTableId;
    private String testDate;
    private String testTime;
    private String testWaiterId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testReservationId = UUID.randomUUID().toString();
        testEmail = "test@example.com";
        testLocationId = "location-123";
        testTableId = "table-123";
        testDate = LocalDate.now().toString();
        testTime = "14:00";
        testWaiterId = "waiter-123";

        // Setup test location
        testLocation = new Location();
        testLocation.setId(testLocationId);
        testLocation.setAddress("123 Test Street");

        // Setup test table
        testTable = new Table();
        testTable.setId(testTableId);
        testTable.setCapacity("4");
        testTable.setLocationId(testLocationId);

        // Setup test reservation
        testReservation = new Reservation();
        testReservation.setReservationId(testReservationId);
        testReservation.setUserId(testEmail);
        testReservation.setLocationId(testLocationId);
        testReservation.setTableNumber(testTableId);
        testReservation.setDate(testDate);
        testReservation.setTimeFrom(testTime);
        testReservation.setTimeTo("15:45");
        testReservation.setGuestNumber("3");
        testReservation.setStatus("CONFIRMED");
        testReservation.setWaiterId(testWaiterId);
        testReservation.setCreatedAt(LocalDateTime.now().toString());

        // Setup test request DTO
        testRequestDto = new ReservationRequestDto();
        testRequestDto.setLocationId(testLocationId);
        testRequestDto.setTableNumber(testTableId);
        testRequestDto.setDate(testDate);
        testRequestDto.setTimeFrom(testTime);
        testRequestDto.setTimeTo("15:45");
        testRequestDto.setGuestsNumber("3");

        // Setup mocks
        when(locationRepository.findById(testLocationId)).thenReturn(testLocation);
        when(waiterRepository.getLeastBusyWaiterForLocation(testLocationId)).thenReturn(testWaiterId);
    }

    @Test
    void getAllReservation_Success() {
        // Arrange
        List<Reservation> expectedReservations = Arrays.asList(testReservation);
        when(reservationRepository.getAllReservation()).thenReturn(expectedReservations);

        // Act
        List<Reservation> result = reservationService.getAllReservation();

        // Assert
        assertEquals(expectedReservations, result);
        verify(reservationRepository).getAllReservation();
    }

    @Test
    void statusChange_Success() {
        // Arrange
        String status = "CANCELLED";
        when(reservationRepository.statusChange(testReservationId, status, testEmail)).thenReturn(true);

        // Act
        boolean result = reservationService.statusChange(testReservationId, status, testEmail);

        // Assert
        assertTrue(result);
        verify(reservationRepository).statusChange(testReservationId, status, testEmail);
    }

    @Test
    void deleteReservationOfUser_Success() {
        // Arrange
        when(reservationRepository.deleteReservation(testReservationId)).thenReturn(true);

        // Act
        boolean result = reservationService.deleteReservationOfUser(testReservationId);

        // Assert
        assertTrue(result);
        verify(reservationRepository).deleteReservation(testReservationId);
    }

    @Test
    void getReservationByLocationIdDateTableId_Success() {
        // Arrange
        List<Reservation> allReservations = Arrays.asList(
                testReservation,
                createReservation("other-id", "other-location", testDate, testTableId, "CONFIRMED"),
                createReservation("another-id", testLocationId, "2023-01-01", testTableId, "CONFIRMED"),
                createReservation("yet-another-id", testLocationId, testDate, "other-table", "CONFIRMED"),
                createReservation("cancelled-id", testLocationId, testDate, testTableId, "CANCELLED")
        );
        when(reservationRepository.getAllReservation()).thenReturn(allReservations);

        // Act
        List<Reservation> result = reservationService.getReservationByLocationIdDateTableId(
                testLocationId, testDate, testTableId);

        // Assert
        assertEquals(1, result.size());
        assertEquals(testReservationId, result.get(0).getReservationId());
        verify(reservationRepository).getAllReservation();
    }








    @Test
    void getAvailableSlots_NoTablesAvailable() {
        // Arrange
        String guests = "10"; // More than table capacity
        List<Table> tables = Arrays.asList(testTable);

        when(tableRepository.getTablesByLocationId(testLocationId)).thenReturn(tables);

        // Act
        List<TableResponseDto> result = reservationService.getAvailableSlots(
                testLocationId, testDate, testTime, guests);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void createReservation_Success() {
        // Arrange
        when(reservationRepository.saveReservation(any(Reservation.class))).thenReturn(testReservation);

        // Act
        ReservationResponse result = reservationService.createReservation(testRequestDto, testEmail);

        // Assert
        assertNotNull(result);
        assertEquals(testLocation.getAddress(), result.getLocationAddress());
        assertEquals(testRequestDto.getGuestsNumber(), result.getGuestNumber());
        assertEquals("CONFIRMED", result.getStatus());
        verify(reservationRepository).saveReservation(any(Reservation.class));
    }

    @Test
    void updateReservation_Success() {
        // Arrange
        List<Reservation> allReservations = Arrays.asList(testReservation);
        when(reservationRepository.getAllReservation()).thenReturn(allReservations);
        when(validation.isWithin30MinutesOfCreation(anyString())).thenReturn(true);
        when(reservationRepository.updateReservation(any(Reservation.class))).thenReturn(testReservation);

        // Act
        ReservationResponse result = reservationService.updateReservation(
                testReservationId, testEmail, testRequestDto);

        // Assert
        assertNotNull(result);
        assertEquals(testLocation.getAddress(), result.getLocationAddress());
        assertEquals(testRequestDto.getGuestsNumber(), result.getGuestNumber());
        assertEquals("CONFIRMED", result.getStatus());
        verify(reservationRepository).updateReservation(any(Reservation.class));
    }

    @Test
    void updateReservation_ReservationNotFound() {
        // Arrange
        when(reservationRepository.getAllReservation()).thenReturn(new ArrayList<>());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                reservationService.updateReservation(testReservationId, testEmail, testRequestDto)
        );
    }

    @Test
    void updateReservation_NotWithin30Minutes() {
        // Arrange
        List<Reservation> allReservations = Arrays.asList(testReservation);
        when(reservationRepository.getAllReservation()).thenReturn(allReservations);
        when(validation.isWithin30MinutesOfCreation(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(TooLateForCancellationException.class, () ->
                reservationService.updateReservation(testReservationId, testEmail, testRequestDto)
        );
    }

    @Test
    void updateReservation_NotOwnedByUser() {
        // Arrange
        List<Reservation> allReservations = Arrays.asList(testReservation);
        when(reservationRepository.getAllReservation()).thenReturn(allReservations);
        when(validation.isWithin30MinutesOfCreation(anyString())).thenReturn(true);

        // Act & Assert
        String differentEmail = "different@example.com";
        assertThrows(ForbiddenException.class, () ->
                reservationService.updateReservation(testReservationId, differentEmail, testRequestDto)
        );
    }

    @Test
    void updateReservation_AlreadyCancelled() {
        // Arrange
        Reservation cancelledReservation = createReservation(
                testReservationId, testLocationId, testDate, testTableId, "CANCELLED");
        cancelledReservation.setUserId(testEmail);

        List<Reservation> allReservations = Arrays.asList(cancelledReservation);
        when(reservationRepository.getAllReservation()).thenReturn(allReservations);
        when(validation.isWithin30MinutesOfCreation(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(ValidationException.class, () ->
                reservationService.updateReservation(testReservationId, testEmail, testRequestDto)
        );
    }

    @Test
    void getAllReservation_ByEmail_Success() {
        // Arrange
        List<Reservation> allReservations = Arrays.asList(
                testReservation,
                createReservation("other-id", testLocationId, testDate, testTableId, "CONFIRMED", "other@example.com")
        );
        when(reservationRepository.getAllReservation()).thenReturn(allReservations);

        // Act
        List<ReservationResponse> result = reservationService.getAllReservation(testEmail);

        // Assert
        assertEquals(1, result.size());
        assertEquals(testReservationId, result.get(0).getId());
        verify(reservationRepository).getAllReservation();
    }

    @Test
    void createReservationByWaiter_Success() {
        // Arrange
        WaiterReservationRequestDto waiterRequestDto = new WaiterReservationRequestDto();
        waiterRequestDto.setLocationId(testLocationId);
        waiterRequestDto.setTableNumber(testTableId);
        waiterRequestDto.setDate(testDate);
        waiterRequestDto.setTimeFrom(testTime);
        waiterRequestDto.setTimeTo("15:45");
        waiterRequestDto.setGuestsNumber("3");

        String waiterEmail = "waiter@example.com";
        String customerEmail = "customer@example.com";

        when(reservationRepository.saveReservation(any(Reservation.class))).thenReturn(testReservation);

        // Act
        ReservationResponse result = reservationService.createReservationByWaiter(
                waiterRequestDto, customerEmail, waiterEmail);

        // Assert
        assertNotNull(result);
        assertEquals(testLocation.getAddress(), result.getLocationAddress());
        assertEquals(waiterRequestDto.getGuestsNumber(), result.getGuestNumber());
        assertEquals("CONFIRMED", result.getStatus());
        verify(reservationRepository).saveReservation(any(Reservation.class));
    }

    @Test
    void findReservationById_Success() {
        // Arrange
        List<Reservation> allReservations = Arrays.asList(testReservation);
        when(reservationRepository.getAllReservation()).thenReturn(allReservations);

        // Act
        Reservation result = reservationService.findReservationById(testReservationId);

        // Assert
        assertNotNull(result);
        assertEquals(testReservationId, result.getReservationId());
        verify(reservationRepository).getAllReservation();
    }

    @Test
    void findReservationById_NotFound() {
        // Arrange
        List<Reservation> allReservations = Arrays.asList(testReservation);
        when(reservationRepository.getAllReservation()).thenReturn(allReservations);

        // Act
        Reservation result = reservationService.findReservationById("non-existent-id");

        // Assert
        assertNull(result);
        verify(reservationRepository).getAllReservation();
    }

    // Helper method to create test reservations with different properties
    private Reservation createReservation(String id, String locationId, String date, String tableId, String status) {
        return createReservation(id, locationId, date, tableId, status, testEmail);
    }

    private Reservation createReservation(String id, String locationId, String date, String tableId, String status, String email) {
        Reservation reservation = new Reservation();
        reservation.setReservationId(id);
        reservation.setLocationId(locationId);
        reservation.setDate(date);
        reservation.setTableNumber(tableId);
        reservation.setStatus(status);
        reservation.setUserId(email);
        reservation.setTimeFrom(testTime);
        reservation.setTimeTo("15:45");
        reservation.setGuestNumber("3");
        reservation.setWaiterId(testWaiterId);
        reservation.setCreatedAt(LocalDateTime.now().toString());
        return reservation;
    }

}