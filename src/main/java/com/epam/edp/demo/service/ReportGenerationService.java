package com.epam.edp.demo.service;

import com.epam.edp.demo.entity.*;
import com.epam.edp.demo.repository.FeedbackRepository;
import com.epam.edp.demo.repository.LocationRepository;
import com.epam.edp.demo.repository.ReservationRepository;
import com.epam.edp.demo.repository.WaiterRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportGenerationService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ReservationRepository reservationRepository;
    private final FeedbackRepository feedbackRepository;
    private final LocationRepository locationRepository;
    private final WaiterRepository waiterRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    public ReportPeriod calculateReportPeriod(int daysBack, int periodLength) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(daysBack);
        LocalDate previousEndDate = startDate.minusDays(1);
        LocalDate previousStartDate = previousEndDate.minusDays(periodLength - 1);

        return ReportPeriod.builder()
                .startDate(startDate)
                .endDate(endDate)
                .previousStartDate(previousStartDate)
                .previousEndDate(previousEndDate)
                .build();
    }

    public List<StaffPerformanceEntry> generateStaffPerformanceReport(ReportPeriod period) {
        logger.info("Generating staff performance report for period: {} to {}",
                period.getStartDate(), period.getEndDate());

        List<StaffPerformanceEntry> entries = new ArrayList<>();
        List<Location> locations = locationRepository.findAll();

        for (Location location : locations) {
            // Get all reservations for this location in both periods
            List<Reservation> currentReservations = filterReservationsByDateRange(
                    location.getId(), period.getStartDate(), period.getEndDate());
            List<Reservation> previousReservations = filterReservationsByDateRange(
                    location.getId(), period.getPreviousStartDate(), period.getPreviousEndDate());

            // Group reservations by waiter
            Map<String, List<Reservation>> currentReservationsByWaiter =
                    groupReservationsByWaiter(currentReservations);
            Map<String, List<Reservation>> previousReservationsByWaiter =
                    groupReservationsByWaiter(previousReservations);

            // Process each waiter
            for (Map.Entry<String, List<Reservation>> entry : currentReservationsByWaiter.entrySet()) {
                String waiterId = entry.getKey();
                List<Reservation> waiterCurrentReservations = entry.getValue();
                List<Reservation> waiterPreviousReservations =
                        previousReservationsByWaiter.getOrDefault(waiterId, Collections.emptyList());

                // Calculate metrics
                int currentOrdersCount = waiterCurrentReservations.size();
                int previousOrdersCount = waiterPreviousReservations.size();
                double ordersDeltaPercent = calculatePercentDelta(currentOrdersCount, previousOrdersCount);

                // Get feedback metrics
                List<Feedback> currentFeedbacks = getServiceFeedbacksForReservations(waiterCurrentReservations);
                List<Feedback> previousFeedbacks = getServiceFeedbacksForReservations(waiterPreviousReservations);
                double averageFeedback = calculateAverageFeedback(currentFeedbacks);
                Integer minFeedback = calculateMinimumFeedback(currentFeedbacks);
                Integer previousMinFeedback = calculateMinimumFeedback(previousFeedbacks);
                double minFeedbackDeltaPercent = (minFeedback != null && previousMinFeedback != null) ?
                        calculatePercentDelta(minFeedback, previousMinFeedback) : 0.0;

                // Estimate hours worked (1 hour per reservation as approximation)
                int hoursWorked = estimateHoursWorked(waiterCurrentReservations);

                entries.add(StaffPerformanceEntry.builder()
                        .locationName(location.getAddress())
                        .waiterEmail(waiterId)
                        .waiterName(getWaiterName(waiterId))
                        .reportStartDate(period.getStartDate())
                        .reportEndDate(period.getEndDate())
                        .hoursWorked(hoursWorked)
                        .ordersProcessed(currentOrdersCount)
                        .ordersProcessedDeltaPercent(ordersDeltaPercent)
                        .averageServiceFeedback(averageFeedback)
                        .minimumServiceFeedback(minFeedback)
                        .minimumServiceFeedbackDeltaPercent(minFeedbackDeltaPercent)
                        .build());
            }
        }

        return entries;
    }

    public List<LocationComparisonEntry> generateLocationComparisonReport(ReportPeriod period) {
        logger.info("Generating location comparison report for period: {} to {}",
                period.getStartDate(), period.getEndDate());

        List<LocationComparisonEntry> entries = new ArrayList<>();
        List<Location> locations = locationRepository.findAll();

        for (Location location : locations) {
            // Get all reservations for both periods
            List<Reservation> currentReservations = filterReservationsByDateRange(
                    location.getId(), period.getStartDate(), period.getEndDate());
            List<Reservation> previousReservations = filterReservationsByDateRange(
                    location.getId(), period.getPreviousStartDate(), period.getPreviousEndDate());

            // Calculate metrics
            int currentOrdersCount = currentReservations.size();
            int previousOrdersCount = previousReservations.size();
            double ordersDeltaPercent = calculatePercentDelta(currentOrdersCount, previousOrdersCount);

            // Get feedback metrics
            List<Feedback> currentFeedbacks = getCuisineFeedbacksForLocation(location.getId(),
                    period.getStartDate(), period.getEndDate());
            List<Feedback> previousFeedbacks = getCuisineFeedbacksForLocation(location.getId(),
                    period.getPreviousStartDate(), period.getPreviousEndDate());
            double currentAverageFeedback = calculateAverageFeedback(currentFeedbacks);
            double previousAverageFeedback = calculateAverageFeedback(previousFeedbacks);
            Integer currentMinFeedback = calculateMinimumFeedback(currentFeedbacks);
            double avgFeedbackDeltaPercent = calculatePercentDelta(currentAverageFeedback, previousAverageFeedback);

            // Calculate revenue
            double currentRevenue = calculateRevenue(currentReservations);
            double previousRevenue = calculateRevenue(previousReservations);
            double revenueDeltaPercent = calculatePercentDelta(currentRevenue, previousRevenue);

            entries.add(LocationComparisonEntry.builder()
                    .locationName(location.getAddress())
                    .reportStartDate(period.getStartDate())
                    .reportEndDate(period.getEndDate())
                    .ordersProcessed(currentOrdersCount)
                    .ordersProcessedDeltaPercent(ordersDeltaPercent)
                    .averageCuisineFeedback(currentAverageFeedback)
                    .minimumCuisineFeedback(currentMinFeedback)
                    .averageCuisineFeedbackDeltaPercent(avgFeedbackDeltaPercent)
                    .revenue(currentRevenue)
                    .revenueDeltaPercent(revenueDeltaPercent)
                    .build());
        }

        return entries;
    }

    // Helper methods
    private List<Reservation> filterReservationsByDateRange(String locationId, LocalDate start, LocalDate end) {
        return reservationRepository.getAllReservation().stream()
                .filter(r -> r.getLocationId().equals(locationId))
                .filter(r -> {
                    LocalDate reservationDate = parseDate(r.getDate());
                    return !reservationDate.isBefore(start) && !reservationDate.isAfter(end);
                })
                .collect(Collectors.toList());
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            // Handle format differences if needed
            return LocalDate.parse(dateStr.split("T")[0]);
        }
    }

    private Map<String, List<Reservation>> groupReservationsByWaiter(List<Reservation> reservations) {
        return reservations.stream()
                .filter(r -> r.getWaiterId() != null)
                .collect(Collectors.groupingBy(Reservation::getWaiterId));
    }

    private List<Feedback> getServiceFeedbacksForReservations(List<Reservation> reservations) {
        List<Feedback> feedbacks = new ArrayList<>();
        for (Reservation reservation : reservations) {
            if (reservation.getFeedbackId() != null) {
                // In real implementation, query the feedback repository
                // This is simplified for demonstration
                Map<String, Object> params = new HashMap<>();
                params.put("id", reservation.getFeedbackId());
                params.put("type", "service");
                // Add feedback to list if found
            }
        }
        return feedbacks;
    }

    private List<Feedback> getCuisineFeedbacksForLocation(String locationId, LocalDate start, LocalDate end) {
        Map<String, Object> feedbackResults = feedbackRepository.findByLocationId(
                locationId, "cuisine", 0, 1000, "date", "desc");

        @SuppressWarnings("unchecked")
        List<Feedback> allFeedbacks = (List<Feedback>) feedbackResults.get("content");

        return allFeedbacks.stream()
                .filter(f -> {
                    if (f.getDate() == null) return false;
                    LocalDate feedbackDate = parseDate(f.getDate());
                    return !feedbackDate.isBefore(start) && !feedbackDate.isAfter(end);
                })
                .collect(Collectors.toList());
    }

    private int estimateHoursWorked(List<Reservation> reservations) {
        // Simplified: 1 hour per reservation
        return reservations.size();
    }

    private double calculateAverageFeedback(List<Feedback> feedbacks) {
        if (feedbacks.isEmpty()) return 0.0;
        return feedbacks.stream()
                .mapToInt(Feedback::getRate)
                .average()
                .orElse(0.0);
    }

    private Integer calculateMinimumFeedback(List<Feedback> feedbacks) {
        if (feedbacks.isEmpty()) return null;
        return feedbacks.stream()
                .mapToInt(Feedback::getRate)
                .min()
                .orElse(0);
    }

    private double calculatePercentDelta(double current, double previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return ((current - previous) / previous) * 100.0;
    }

    private double calculateRevenue(List<Reservation> reservations) {
        // In a real implementation, calculate actual revenue from orders
        // Simplified: $95 per reservation
        return reservations.size() * 95.0;
    }

    private String getWaiterName(String waiterEmail) {
        // In real implementation, get name from profile
        return waiterEmail.split("@")[0];
    }
}