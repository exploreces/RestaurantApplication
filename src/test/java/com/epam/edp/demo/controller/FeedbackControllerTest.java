package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.request.FeedbackRequestDto;
import com.epam.edp.demo.dto.request.NewFeedbackRequestDto;
import com.epam.edp.demo.dto.response.ApiResponse;
import com.epam.edp.demo.dto.response.FeedbackResponseDto;
import com.epam.edp.demo.dto.response.PageFeedbackResponseDto;
import com.epam.edp.demo.service.impl.FeedbackService;
import com.epam.edp.demo.utility.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


class FeedbackControllerTest {

    private FeedbackService feedbackService;
    private JwtService jwtService;
    private FeedbackController controller;

    @BeforeEach
    void setUp() {
        feedbackService = mock(FeedbackService.class);
        jwtService       = mock(JwtService.class);
        controller       = new FeedbackController(feedbackService, jwtService);
    }

    //----------------------------------------------------------------------
    // createFeedback() — success
    //----------------------------------------------------------------------
    @Test
    void createFeedback_success() {
        String rawToken = "Bearer abc.def.ghi";
        String email = "user@example.com";

        // New feedback request input
        NewFeedbackRequestDto newReq = new NewFeedbackRequestDto();
        newReq.setReservationId("res123");
        newReq.setCuisineRating("5");
        newReq.setCuisineComment("Tasty");
        newReq.setServiceRating("4");
        newReq.setServiceComment("Quick");

        // Mocked request DTOs for cuisine and service
        FeedbackRequestDto cuisineReq = new FeedbackRequestDto();
        FeedbackRequestDto serviceReq = new FeedbackRequestDto();

        // Mocked response DTOs with IDs
        FeedbackResponseDto cuisineResp = new FeedbackResponseDto();
        FeedbackResponseDto serviceResp = new FeedbackResponseDto();
        cuisineResp.setId("cid-1");
        serviceResp.setId("sid-1");

        // Mock JWT extraction
        when(jwtService.extractUserName("abc.def.ghi")).thenReturn(email);

        // Mock creation of FeedbackRequestDto
        when(feedbackService.createFeedbackRequestDto(newReq, email, "cuisine"))
                .thenReturn(cuisineReq);
        when(feedbackService.createFeedbackRequestDto(newReq, email, "service"))
                .thenReturn(serviceReq);

        // Mock service feedback creation - return cuisineResp first, then serviceResp
        when(feedbackService.createFeedback(any())).thenReturn(cuisineResp, serviceResp);

        // Call controller
        ResponseEntity<ApiResponse> response = controller.createFeedback(rawToken, newReq);

        // Assertions
        assertEquals(201, response.getStatusCodeValue());
        ApiResponse body = response.getBody();
        assertNotNull(body);

        var data = body.getData();
        assertEquals("Feedback submitted successfully", data.get("message"));
        assertEquals("sid-1", data.get("serviceFeedbackId"));
        assertEquals("cid-1", data.get("cuisineFeedbackId"));

        // Verify behavior
        verify(jwtService).extractUserName("abc.def.ghi");
        verify(feedbackService).createFeedbackRequestDto(newReq, email, "cuisine");
        verify(feedbackService).createFeedbackRequestDto(newReq, email, "service");
        verify(feedbackService, times(2)).createFeedback(any());
        verifyNoMoreInteractions(feedbackService, jwtService);
    }


    //----------------------------------------------------------------------
    // createFeedback() — failure (invalid JWT)
    //----------------------------------------------------------------------
    @Test
    void createFeedback_invalidToken_throws() {
        String rawToken = "Bearer bad.token";
        NewFeedbackRequestDto newReq = new NewFeedbackRequestDto();

        when(jwtService.extractUserName("bad.token"))
                .thenThrow(new RuntimeException("Invalid JWT"));

        var ex = assertThrows(RuntimeException.class, () -> {
            controller.createFeedback(rawToken, newReq);
        });
        assertEquals("Invalid JWT", ex.getMessage());
        verify(jwtService).extractUserName("bad.token");
        verifyNoMoreInteractions(feedbackService);
    }

    //----------------------------------------------------------------------
    // getFeedbacks() — success with defaults
    //----------------------------------------------------------------------
    @Test
    void getFeedbacks_defaultParams_success() {
        // Create feedbacks list
        List<FeedbackResponseDto> feedbackList = Collections.emptyList();

        // Create the nested structure
        PageFeedbackResponseDto.Feedbacks feedbacks = new PageFeedbackResponseDto.Feedbacks();
        feedbacks.setContent(feedbackList);
        feedbacks.setTotalElements(0L);
        feedbacks.setTotalPages(0);
        feedbacks.setNumber(0);
        feedbacks.setSize(10);
        feedbacks.setEmpty(true);

        PageFeedbackResponseDto.Data data = new PageFeedbackResponseDto.Data();
        data.setFeedbacks(feedbacks);

        PageFeedbackResponseDto pageDto = new PageFeedbackResponseDto();
        pageDto.setData(data);

        when(feedbackService.getFeedbacksByLocationId(
                "loc1", null, 0, 10, "date", "desc"))
                .thenReturn(pageDto);

        ResponseEntity<PageFeedbackResponseDto> response =
                controller.getFeedbacks("loc1", null, 0, 10, "date", "desc");

        assertEquals(200, response.getStatusCodeValue());
        assertSame(pageDto, response.getBody());
        assertNotNull(response.getBody().getData());
        assertNotNull(response.getBody().getData().getFeedbacks());
        assertTrue(response.getBody().getData().getFeedbacks().isEmpty());

        verify(feedbackService)
                .getFeedbacksByLocationId("loc1", null, 0, 10, "date", "desc");
    }

    //----------------------------------------------------------------------
    // getFeedbacks() — success with custom params
    //----------------------------------------------------------------------
    @Test
    void getFeedbacks_customParams_success() {
        // Create the nested structure
        PageFeedbackResponseDto.Feedbacks feedbacks = new PageFeedbackResponseDto.Feedbacks();
        PageFeedbackResponseDto.Data data = new PageFeedbackResponseDto.Data();
        data.setFeedbacks(feedbacks);

        PageFeedbackResponseDto pageDto = new PageFeedbackResponseDto();
        pageDto.setData(data);

        when(feedbackService.getFeedbacksByLocationId(
                "loc2", "cuisine", 1, 5, "type", "asc"))
                .thenReturn(pageDto);

        ResponseEntity<PageFeedbackResponseDto> response =
                controller.getFeedbacks("loc2", "cuisine", 1, 5, "type", "asc");

        assertEquals(200, response.getStatusCodeValue());
        assertSame(pageDto, response.getBody());
        assertNotNull(response.getBody().getData());
        assertNotNull(response.getBody().getData().getFeedbacks());

        verify(feedbackService)
                .getFeedbacksByLocationId("loc2", "cuisine", 1, 5, "type", "asc");
    }

    //----------------------------------------------------------------------
    // createVisitorFeedback() — success
    //----------------------------------------------------------------------
    @Test
    void createVisitorFeedback_success() {
        // Prepare test input
        NewFeedbackRequestDto newReq = new NewFeedbackRequestDto();
        newReq.setReservationId("resX");
        newReq.setCuisineRating("3");
        newReq.setCuisineComment("OK");
        newReq.setServiceRating("2");
        newReq.setServiceComment("Slow");

        // Mock request DTOs
        FeedbackRequestDto cuisineReq = new FeedbackRequestDto();
        FeedbackRequestDto serviceReq = new FeedbackRequestDto();

        // Mock response DTOs
        FeedbackResponseDto cuisineResp = new FeedbackResponseDto();
        cuisineResp.setId("v-cid");
        FeedbackResponseDto serviceResp = new FeedbackResponseDto();
        serviceResp.setId("v-sid");

        // Mock creation of request DTOs
        when(feedbackService.createFeedbackRequestDto(newReq, "Anonymous", "cuisine"))
                .thenReturn(cuisineReq);
        when(feedbackService.createFeedbackRequestDto(newReq, "Anonymous", "service"))
                .thenReturn(serviceReq);

        // Return responses in sequence
        when(feedbackService.createFeedback(any())).thenReturn(cuisineResp, serviceResp);

        // Call controller
        ResponseEntity<ApiResponse> response = controller.createVisitorFeedback(newReq);

        // Assertions
        assertEquals(201, response.getStatusCodeValue());
        var data = response.getBody().getData();

        assertEquals("Feedback submitted successfully", data.get("message"));
        assertEquals("v-cid", data.get("cuisineFeedbackId"));
        assertEquals("v-sid", data.get("serviceFeedbackId"));

        // Verify calls
        verify(feedbackService).createFeedbackRequestDto(newReq, "Anonymous", "cuisine");
        verify(feedbackService).createFeedbackRequestDto(newReq, "Anonymous", "service");
        verify(feedbackService, times(2)).createFeedback(any());
        verifyNoMoreInteractions(feedbackService);
    }

    //----------------------------------------------------------------------
    // createVisitorFeedback() — failure in service
    //----------------------------------------------------------------------
    @Test
    void createVisitorFeedback_serviceThrows_throws() {
        NewFeedbackRequestDto newReq = new NewFeedbackRequestDto();
        when(feedbackService.createFeedbackRequestDto(newReq, "Anonymous", "cuisine"))
                .thenReturn(new com.epam.edp.demo.dto.request.FeedbackRequestDto());
        when(feedbackService.createFeedback(any()))
                .thenThrow(new RuntimeException("DB down"));

        var ex = assertThrows(RuntimeException.class, () -> {
            controller.createVisitorFeedback(newReq);
        });
        assertEquals("DB down", ex.getMessage());
    }
}