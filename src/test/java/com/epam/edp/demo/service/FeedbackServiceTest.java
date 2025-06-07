package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.request.FeedbackRequestDto;
import com.epam.edp.demo.dto.request.NewFeedbackRequestDto;
import com.epam.edp.demo.dto.response.FeedbackResponseDto;
import com.epam.edp.demo.dto.response.PageFeedbackResponseDto;
import com.epam.edp.demo.entity.Feedback;
import com.epam.edp.demo.entity.Reservation;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.exceptions.RepositoryException;
import com.epam.edp.demo.repository.FeedbackRepository;
import com.epam.edp.demo.repository.ReservationRepository;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.service.impl.FeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    private Feedback testFeedback;
    private FeedbackRequestDto testFeedbackRequestDto;
    private NewFeedbackRequestDto testNewFeedbackRequestDto;
    private User testUser;
    private Reservation testReservation;
    private Map<String, Object> testPageData;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup test feedback
        testFeedback = new Feedback();
        testFeedback.setId("feedback-123");
        testFeedback.setLocationId("location-123");
        testFeedback.setType("cuisine");
        testFeedback.setDate(LocalDateTime.now().toString());
        testFeedback.setRate(5);
        testFeedback.setComment("Great food!");
        testFeedback.setUserName("test@example.com");
        testFeedback.setUserAvatarUrl("https://example.com/avatar.jpg");
        testFeedback.setReservationId("reservation-123");

        // Setup test feedback request DTO
        testFeedbackRequestDto = new FeedbackRequestDto();
        testFeedbackRequestDto.setLocationId("location-123");
        testFeedbackRequestDto.setType("cuisine");
        testFeedbackRequestDto.setRate(5);
        testFeedbackRequestDto.setComment("Great food!");
        testFeedbackRequestDto.setUserName("test@example.com");
        testFeedbackRequestDto.setUserAvatarUrl("https://example.com/avatar.jpg");
        testFeedbackRequestDto.setReservationId("reservation-123");

        // Setup test new feedback request DTO
        testNewFeedbackRequestDto = new NewFeedbackRequestDto();
        testNewFeedbackRequestDto.setReservationId("reservation-123");
        testNewFeedbackRequestDto.setCuisineRating("5");
        testNewFeedbackRequestDto.setServiceRating("4");
        testNewFeedbackRequestDto.setCuisineComment("Great food!");
        testNewFeedbackRequestDto.setServiceComment("Good service");

        // Setup test user
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setImageUrl("https://example.com/avatar.jpg");

        // Setup test reservation
        testReservation = new Reservation();
        testReservation.setReservationId("reservation-123");
        testReservation.setLocationId("location-123");

        // Setup test page data
        testPageData = new HashMap<>();
        testPageData.put("content", Collections.singletonList(testFeedback));
        testPageData.put("totalPages", 1);
        testPageData.put("totalElements", 1L);
        testPageData.put("number", 0);
        testPageData.put("size", 20);
        testPageData.put("numberOfElements", 1);
        testPageData.put("first", true);
        testPageData.put("last", true);
        testPageData.put("empty", false);

        Map<String, Object> pageable = new HashMap<>();
        pageable.put("pageNumber", 0);
        pageable.put("pageSize", 20);
        testPageData.put("pageable", pageable);

        List<Map<String, Object>> sortList = new ArrayList<>();
        Map<String, Object> sortItem = new HashMap<>();
        sortItem.put("direction", "DESC");
        sortItem.put("property", "date");
        sortList.add(sortItem);
        testPageData.put("sort", sortList);
    }

    @Test
    void getFeedbacksByLocationId_Success() {
        // Arrange
        when(feedbackRepository.findByLocationId(
                anyString(), anyString(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(testPageData);

        // Act
        PageFeedbackResponseDto result = feedbackService.getFeedbacksByLocationId(
                "location-123", "cuisine", 0, 20, "date", "desc");

        // Assert
        assertNotNull(result);
        assertNotNull(result.getData());
        assertNotNull(result.getData().getFeedbacks());

        PageFeedbackResponseDto.Feedbacks feedbacks = result.getData().getFeedbacks();
        assertEquals(1, feedbacks.getTotalPages());
        assertEquals(1L, feedbacks.getTotalElements());
        assertEquals(0, feedbacks.getNumber());
        assertEquals(20, feedbacks.getSize());
        assertEquals(1, feedbacks.getNumberOfElements());
        assertTrue(feedbacks.isFirst());
        assertFalse(feedbacks.isEmpty());
        assertEquals(1, feedbacks.getContent().size());

        verify(feedbackRepository).findByLocationId(
                "location-123", "cuisine", 0, 20, "date", "desc");
    }

    @Test
    void getUserAvatarFromDynamoDB_Success() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        // Act
        String result = feedbackService.getUserAvatarFromDynamoDB("test@example.com");

        // Assert
        assertEquals("https://example.com/avatar.jpg", result);
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void getUserAvatarFromDynamoDB_NullEmail() {
        // Act
        String result = feedbackService.getUserAvatarFromDynamoDB(null);

        // Assert
        assertNull(result);
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void getUserAvatarFromDynamoDB_EmptyEmail() {
        // Act
        String result = feedbackService.getUserAvatarFromDynamoDB("  ");

        // Assert
        assertNull(result);
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void getUserAvatarFromDynamoDB_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(null);

        // Act
        String result = feedbackService.getUserAvatarFromDynamoDB("unknown@example.com");

        // Assert
        assertNull(result);
        verify(userRepository).findByEmail("unknown@example.com");
    }

    @Test
    void getUserAvatarFromDynamoDB_NoAvatarUrl() {
        // Arrange
        User userWithoutAvatar = new User();
        userWithoutAvatar.setEmail("test@example.com");
        userWithoutAvatar.setImageUrl(null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(userWithoutAvatar);

        // Act
        String result = feedbackService.getUserAvatarFromDynamoDB("test@example.com");

        // Assert
        assertNull(result);
    }

    @Test
    void getUserAvatarFromDynamoDB_RepositoryException() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenThrow(new RepositoryException("Database error"));

        // Act
        String result = feedbackService.getUserAvatarFromDynamoDB("test@example.com");

        // Assert
        assertNull(result);
    }

    @Test
    void getUserAvatarFromDynamoDB_UnexpectedException() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenThrow(new RuntimeException("Unexpected error"));

        // Act
        String result = feedbackService.getUserAvatarFromDynamoDB("test@example.com");

        // Assert
        assertNull(result);
    }

    @Test
    void getLocationIdFromReservation_Success() {
        // Arrange
        List<Reservation> reservations = Collections.singletonList(testReservation);
        when(reservationRepository.getAllReservation()).thenReturn(reservations);

        // Act
        String result = feedbackService.getLocationIdFromReservation("reservation-123");

        // Assert
        assertEquals("location-123", result);
        verify(reservationRepository).getAllReservation();
    }

    @Test
    void getLocationIdFromReservation_NullReservationId() {
        // Act
        String result = feedbackService.getLocationIdFromReservation(null);

        // Assert
        assertNull(result);
        verify(reservationRepository, never()).getAllReservation();
    }

    @Test
    void getLocationIdFromReservation_EmptyReservationId() {
        // Act
        String result = feedbackService.getLocationIdFromReservation("  ");

        // Assert
        assertNull(result);
        verify(reservationRepository, never()).getAllReservation();
    }

    @Test
    void getLocationIdFromReservation_NoReservations() {
        // Arrange
        when(reservationRepository.getAllReservation()).thenReturn(null);

        // Act
        String result = feedbackService.getLocationIdFromReservation("reservation-123");

        // Assert
        assertNull(result);
    }

    @Test
    void getLocationIdFromReservation_EmptyReservationsList() {
        // Arrange
        when(reservationRepository.getAllReservation()).thenReturn(Collections.emptyList());

        // Act
        String result = feedbackService.getLocationIdFromReservation("reservation-123");

        // Assert
        assertNull(result);
    }

    @Test
    void getLocationIdFromReservation_ReservationNotFound() {
        // Arrange
        List<Reservation> reservations = Collections.singletonList(testReservation);
        when(reservationRepository.getAllReservation()).thenReturn(reservations);

        // Act
        String result = feedbackService.getLocationIdFromReservation("nonexistent-reservation");

        // Assert
        assertNull(result);
    }

    @Test
    void getLocationIdFromReservation_NullLocationId() {
        // Arrange
        Reservation reservationWithoutLocation = new Reservation();
        reservationWithoutLocation.setReservationId("reservation-123");
        reservationWithoutLocation.setLocationId(null);

        List<Reservation> reservations = Collections.singletonList(reservationWithoutLocation);
        when(reservationRepository.getAllReservation()).thenReturn(reservations);

        // Act
        String result = feedbackService.getLocationIdFromReservation("reservation-123");

        // Assert
        assertNull(result);
    }

    @Test
    void getLocationIdFromReservation_Exception() {
        // Arrange
        when(reservationRepository.getAllReservation()).thenThrow(new RuntimeException("Database error"));

        // Act
        String result = feedbackService.getLocationIdFromReservation("reservation-123");

        // Assert
        assertNull(result);
    }

    @Test
    void getUserFromDynamoDB_Success() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);

        // Act
        String result = feedbackService.getUserFromDynamoDB("test@example.com");

        // Assert
        assertEquals("Test", result);
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void getUserFromDynamoDB_NullEmail() {
        // Act
        String result = feedbackService.getUserFromDynamoDB(null);

        // Assert
        assertNull(result);
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void getUserFromDynamoDB_EmptyEmail() {
        // Act
        String result = feedbackService.getUserFromDynamoDB("  ");

        // Assert
        assertNull(result);
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void getUserFromDynamoDB_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(null);

        // Act
        String result = feedbackService.getUserFromDynamoDB("unknown@example.com");

        // Assert
        assertNull(result);
    }

    @Test
    void createFeedback_Success() {
        // Arrange
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(testFeedback);
        when(reservationRepository.findByReservationId(testFeedbackRequestDto.getReservationId()))
                .thenReturn(testReservation);

        // Act
        FeedbackResponseDto result = feedbackService.createFeedback(testFeedbackRequestDto);

        // Assert
        assertNotNull(result);
        assertEquals(testFeedback.getId(), result.getId());
        assertEquals(testFeedback.getLocationId(), result.getLocationId());
        assertEquals(testFeedback.getType(), result.getType());
        assertEquals(testFeedback.getRate(), result.getRate());
        assertEquals(testFeedback.getComment(), result.getComment());
        assertEquals(testFeedback.getUserName(), result.getUserName());
        assertEquals(testFeedback.getUserAvatarUrl(), result.getUserAvatarUrl());
        assertEquals(testFeedback.getReservationId(), result.getReservationId());

        // Verify repository interactions
        verify(reservationRepository).findByReservationId(testFeedbackRequestDto.getReservationId());
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    void createFeedback_Exception() {
        // Arrange
        when(feedbackRepository.save(any(Feedback.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                feedbackService.createFeedback(testFeedbackRequestDto)
        );
    }

    @Test
    void createFeedbackRequestDto_CuisineType() {
        // Arrange
        String userName = "test@example.com";
        String type = "cuisine";
        when(userRepository.findByEmail(userName)).thenReturn(testUser);

        List<Reservation> reservations = Collections.singletonList(testReservation);
        when(reservationRepository.getAllReservation()).thenReturn(reservations);

        // Act
        FeedbackRequestDto result = feedbackService.createFeedbackRequestDto(testNewFeedbackRequestDto, userName, type);

        // Assert
        assertNotNull(result);
        assertEquals("location-123", result.getLocationId());
        assertEquals("cuisine", result.getType());
        assertEquals(5, result.getRate());
        assertEquals("Great food!", result.getComment());
        assertEquals(userName, result.getUserName());
        assertEquals("https://example.com/avatar.jpg", result.getUserAvatarUrl());
        assertEquals("reservation-123", result.getReservationId());
    }

    @Test
    void createFeedbackRequestDto_ServiceType() {
        // Arrange
        String userName = "test@example.com";
        String type = "service";
        when(userRepository.findByEmail(userName)).thenReturn(testUser);

        List<Reservation> reservations = Collections.singletonList(testReservation);
        when(reservationRepository.getAllReservation()).thenReturn(reservations);

        // Act
        FeedbackRequestDto result = feedbackService.createFeedbackRequestDto(testNewFeedbackRequestDto, userName, type);

        // Assert
        assertNotNull(result);
        assertEquals("location-123", result.getLocationId());
        assertEquals("service", result.getType());
        assertEquals(4, result.getRate());
        assertEquals("Good service", result.getComment());
        assertEquals(userName, result.getUserName());
        assertEquals("https://example.com/avatar.jpg", result.getUserAvatarUrl());
        assertEquals("reservation-123", result.getReservationId());
    }
}