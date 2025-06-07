package com.epam.edp.demo.service.impl;


import com.epam.edp.demo.dto.request.FeedbackRequestDto;
import com.epam.edp.demo.dto.request.NewFeedbackRequestDto;
import com.epam.edp.demo.dto.response.FeedbackResponseDto;
import com.epam.edp.demo.dto.response.PageFeedbackResponseDto;
import com.epam.edp.demo.entity.Feedback;
import com.epam.edp.demo.entity.Reservation;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.exceptions.RepositoryException;
import com.epam.edp.demo.exceptions.ResourceNotFoundException;
import com.epam.edp.demo.exceptions.ServiceException;
import com.epam.edp.demo.repository.FeedbackRepository;
import com.epam.edp.demo.repository.ReservationRepository;
import com.epam.edp.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class FeedbackService {
    public static final String SORT_ORDER_DESC = "desc";
    public static final String SORT_BY_DATE = "date";
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int DEFAULT_PAGE_NUMBER = 0; // Changed from 1 to 0
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final FeedbackRepository feedbackRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    @Autowired
    public FeedbackService(FeedbackRepository feedbackRepository,  UserRepository userRepository , ReservationRepository reservationRepository) {
        this.feedbackRepository = feedbackRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        logger.info("FeedbackServiceImpl initialized");
    }


    public PageFeedbackResponseDto getFeedbacksByLocationId(String locationId, String type, Integer page, Integer size, String sortBy, String sortOrder) {
        logger.info("Getting feedbacks for location: {}, type: {}, page: {}, size: {}, sortBy: {}, sortOrder: {}",
                locationId, type, page, size, sortBy, sortOrder);

        // Changed to accept page=0 as valid
        int pageNumber = (page != null) ? page : DEFAULT_PAGE_NUMBER;
        int pageSize = (size != null && size > 0) ? size : DEFAULT_PAGE_SIZE;
        logger.info("Using page number: {} and page size: {}", pageNumber, pageSize);

        String sort = (sortBy != null && !sortBy.isEmpty()) ? sortBy : SORT_BY_DATE;
        String order = (sortOrder != null && !sortOrder.isEmpty()) ? sortOrder : SORT_ORDER_DESC;
        logger.info("Sorting by: {} in {} order", sort, order);

        try {
            Map<String, Object> feedbacksPage = feedbackRepository.findByLocationId(locationId, type, pageNumber, pageSize, sort, order);

            List<Feedback> content = (List<Feedback>) feedbacksPage.getOrDefault("content", Collections.emptyList());
            logger.info("Fetched {} feedbacks from repository", content.size());

            List<FeedbackResponseDto> contentDtos = content.stream()
                    .map(this::convertToResponseDto)
                    .toList();
            logger.info("Converted feedbacks to DTOs");

            // Create the Feedbacks object
            PageFeedbackResponseDto.Feedbacks feedbacks = new PageFeedbackResponseDto.Feedbacks();
            feedbacks.setContent(contentDtos);
            feedbacks.setTotalPages(getIntValue(feedbacksPage, "totalPages", 0));
            feedbacks.setTotalElements(getLongValue(feedbacksPage, "totalElements", 0L));
            feedbacks.setSize(pageSize);
            feedbacks.setNumber(getIntValue(feedbacksPage, "number", pageNumber));

            List<Map<String, Object>> sortList = (List<Map<String, Object>>) feedbacksPage.getOrDefault("sort", Collections.emptyList());
            feedbacks.setSort(sortList);

            feedbacks.setFirst(getBooleanValue(feedbacksPage, "first", pageNumber == 0));
            feedbacks.setLast(getBooleanValue(feedbacksPage, "last", false));
            feedbacks.setNumberOfElements(getIntValue(feedbacksPage, "numberOfElements", 0));

            Map<String, Object> pageable = (Map<String, Object>) feedbacksPage.getOrDefault("pageable", Collections.emptyMap());
            feedbacks.setPageable(pageable);

            feedbacks.setEmpty(getBooleanValue(feedbacksPage, "empty", contentDtos.isEmpty()));

            // Create the Data object and set the Feedbacks
            PageFeedbackResponseDto.Data data = new PageFeedbackResponseDto.Data();
            data.setFeedbacks(feedbacks);

            // Create the PageFeedbackResponseDto and set the Data
            PageFeedbackResponseDto pageDto = new PageFeedbackResponseDto();
            pageDto.setData(data);

            logger.info("Returning page response DTO with {} elements", contentDtos.size());

            return pageDto;
        } catch (Exception e) {
            logger.error("Error fetching feedbacks: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Error fetching feedbacks");
        }
    }


    public String getUserAvatarFromDynamoDB(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            return null;
        }
        logger.info("Retrieving avatar URL for user: {}", userEmail);
        try {
            User user = userRepository.findByEmail(userEmail);

            if (user == null) {
                logger.warn("User not found with email: {}", userEmail);
                return null;
            }
            String imageUrl = user.getImageUrl();

            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                logger.info("No avatar URL found for user: {}", userEmail);
                return null;
            }
            logger.info("Retrieved avatar URL for user {}: {}", userEmail, imageUrl);
            return imageUrl;
        } catch (RepositoryException e) {
            logger.error("Repository error retrieving user avatar for {}: {}", userEmail, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error retrieving user avatar for {}: {}", userEmail, e.getMessage(), e);
            return null;
        }
    }


    public String getLocationIdFromReservation(String reservationId) {
        if (reservationId == null || reservationId.trim().isEmpty()) {
            logger.warn("Cannot retrieve location ID for null or empty reservation ID");
            return null;
        }

        logger.info("Retrieving location ID for reservation: {}", reservationId);

        try {
            List<Reservation> allReservations = reservationRepository.getAllReservation();

            if (allReservations == null || allReservations.isEmpty()) {
                logger.warn("No reservations found in the system");
                return null;
            }

            Optional<Reservation> matchingReservation = allReservations.stream()
                    .filter(r -> reservationId.equals(r.getReservationId()))
                    .findFirst();

            if (matchingReservation.isEmpty()) {
                logger.warn("No reservation found with ID: {}", reservationId);
                return null;
            }

            Reservation reservation = matchingReservation.get();
            String locationId = reservation.getLocationId();

            if (locationId == null || locationId.trim().isEmpty()) {
                logger.warn("Reservation {} has no location ID", reservationId);
                return null;
            }

            logger.info("Retrieved location ID {} for reservation {}", locationId, reservationId);
            return locationId;
        } catch (Exception e) {
            logger.error("Error retrieving location ID for reservation {}: {}", reservationId, e.getMessage(), e);
            return null;
        }
    }


    public String getUserFromDynamoDB(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            logger.warn("Cannot retrieve avatar for null or empty email");
            return null;
        }
        User user = userRepository.findByEmail(userEmail);

        if (user == null) {
            return null;
        }
        return user.getFirstName();
    }


    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private long getLongValue(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    private boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }


    public FeedbackResponseDto createFeedback(FeedbackRequestDto feedbackRequest) {
        logger.info("Creating feedback for location: {}, type: {}",
                feedbackRequest.getLocationId(), feedbackRequest.getType());

        Reservation reservationId = reservationRepository.findByReservationId(feedbackRequest.getReservationId());
        if(reservationId == null) {
            logger.error("Reservation not found with ID: {}", feedbackRequest.getReservationId());
            throw new ResourceNotFoundException("Reservation not found");
        }

        try {
            Feedback feedback = new Feedback();
            String feedbackId = UUID.randomUUID().toString();
            logger.info("Generated feedback ID: {}", feedbackId);

            feedback.setId(feedbackId);
            feedback.setLocationId(feedbackRequest.getLocationId());
            feedback.setType(feedbackRequest.getType());
            feedback.setDate(LocalDateTime.now().toString());
            feedback.setRate(feedbackRequest.getRate());
            feedback.setComment(feedbackRequest.getComment());
            feedback.setUserName(feedbackRequest.getUserName());
            feedback.setUserAvatarUrl(feedbackRequest.getUserAvatarUrl());
            feedback.setReservationId(feedbackRequest.getReservationId());

            Feedback savedFeedback = feedbackRepository.save(feedback);
            logger.info("Saved feedback with ID: {}", savedFeedback.getId());

            return convertToResponseDto(savedFeedback);
        } catch (Exception e) {
            throw new ServiceException("Error creating feedback", e);
        }
    }

    public FeedbackRequestDto createFeedbackRequestDto(NewFeedbackRequestDto newFeedbackRequest, String userName, String type) {
        FeedbackRequestDto feedbackRequestDto = new FeedbackRequestDto();
        feedbackRequestDto.setLocationId(getLocationIdFromReservation(newFeedbackRequest.getReservationId()));
        feedbackRequestDto.setType(type);

        // Handle empty rating strings
        if ("cuisine".equals(type)) {
            String cuisineRating = newFeedbackRequest.getCuisineRating();
            if (cuisineRating == null || cuisineRating.isEmpty()) {
                throw new IllegalArgumentException("Cuisine rating cannot be empty");
            }
            feedbackRequestDto.setRate(Integer.parseInt(cuisineRating));
        } else {
            String serviceRating = newFeedbackRequest.getServiceRating();
            if (serviceRating == null || serviceRating.isEmpty()) {
                throw new IllegalArgumentException("Service rating cannot be empty");
            }
            feedbackRequestDto.setRate(Integer.parseInt(serviceRating));
        }

        feedbackRequestDto.setComment("cuisine".equals(type) ?
                newFeedbackRequest.getCuisineComment() :
                newFeedbackRequest.getServiceComment());
        feedbackRequestDto.setUserName(userName);
        feedbackRequestDto.setUserAvatarUrl(getUserAvatarFromDynamoDB(userName));
        feedbackRequestDto.setReservationId(newFeedbackRequest.getReservationId());
        return feedbackRequestDto;
    }


    private FeedbackResponseDto convertToResponseDto(Feedback feedback) {
        try {
            FeedbackResponseDto responseDto = new FeedbackResponseDto();
            responseDto.setId(feedback.getId());
            responseDto.setLocationId(feedback.getLocationId());
            responseDto.setType(feedback.getType());
            responseDto.setDate(feedback.getDate());
            responseDto.setRate(feedback.getRate());
            responseDto.setComment(feedback.getComment());
            responseDto.setUserName(feedback.getUserName());
            responseDto.setUserAvatarUrl(feedback.getUserAvatarUrl());
            responseDto.setReservationId(feedback.getReservationId());

            logger.info("Successfully converted feedback to DTO");
            return responseDto;
        } catch (Exception e) {
            throw new ServiceException("Error converting feedback to response DTO", e);
        }
    }
}