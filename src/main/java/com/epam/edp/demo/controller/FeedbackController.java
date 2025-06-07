package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.request.NewFeedbackRequestDto;
import com.epam.edp.demo.dto.response.ApiResponse;
import com.epam.edp.demo.dto.response.FeedbackResponseDto;
import com.epam.edp.demo.dto.response.PageFeedbackResponseDto;
import com.epam.edp.demo.service.impl.FeedbackService;
import com.epam.edp.demo.utility.JwtService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final JwtService jwtService;

    public FeedbackController(FeedbackService feedbackService, JwtService jwtService) {
        this.feedbackService = feedbackService;
        this.jwtService = jwtService;
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/feedbacks")
    public ResponseEntity<ApiResponse> createFeedback(@RequestHeader("Authorization") String token,
                                                     @Valid @RequestBody NewFeedbackRequestDto newFeedbackRequest) {
        try {
            String email = jwtService.extractUserName(token.substring(7));
            FeedbackResponseDto cuisineFeedback = feedbackService.createFeedback(
                    feedbackService.createFeedbackRequestDto(newFeedbackRequest, email, "cuisine"));
            FeedbackResponseDto serviceFeedback = feedbackService.createFeedback(
                    feedbackService.createFeedbackRequestDto(newFeedbackRequest, email, "service"));

            ApiResponse response = new ApiResponse();
            response.addData("message", "Feedback submitted successfully");
            response.addData("cuisineFeedbackId", cuisineFeedback.getId());
            response.addData("serviceFeedbackId", serviceFeedback.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (NumberFormatException e) {
            ApiResponse response = new ApiResponse();
            response.addData("message", "Rating must be a number between 1 and 5");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/locations/{locationId}/feedbacks")
    public ResponseEntity<PageFeedbackResponseDto> getFeedbacks(
            @PathVariable String locationId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        PageFeedbackResponseDto feedbacks = feedbackService.getFeedbacksByLocationId(
                locationId, type, page, size, sortBy, sortOrder);
        return ResponseEntity.ok(feedbacks);
    }

    @PostMapping("/feedbacks/visitor")
    public ResponseEntity<ApiResponse> createVisitorFeedback(@Valid @RequestBody NewFeedbackRequestDto newFeedbackRequest) {
        try {
            FeedbackResponseDto cuisineFeedback = feedbackService.createFeedback(
                    feedbackService.createFeedbackRequestDto(newFeedbackRequest, "Anonymous", "cuisine"));
            FeedbackResponseDto serviceFeedback = feedbackService.createFeedback(
                    feedbackService.createFeedbackRequestDto(newFeedbackRequest, "Anonymous", "service"));

            ApiResponse response = new ApiResponse();
            response.addData("message", "Feedback submitted successfully");
            response.addData("cuisineFeedbackId", cuisineFeedback.getId());
            response.addData("serviceFeedbackId", serviceFeedback.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (NumberFormatException e) {
            ApiResponse response = new ApiResponse();
            response.addData("message", "Rating must be a number between 1 and 5");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
