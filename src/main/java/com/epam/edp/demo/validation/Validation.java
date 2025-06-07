package com.epam.edp.demo.validation;

import com.epam.edp.demo.dto.request.ReservationRequestDto;
import com.epam.edp.demo.entity.Location;
import com.epam.edp.demo.exception.ResourceNotFoundException;
import com.epam.edp.demo.exception.ValidationException;
import com.epam.edp.demo.repository.LocationRepository;
import com.epam.edp.demo.service.TableService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Service
public class Validation {

    private final LocationRepository locationRepository;
    private final TableService tableService;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final List<String> STANDARD_TIME_SLOTS = Arrays.asList(
            "10:30", "12:15", "14:00", "15:45", "17:30", "19:15", "21:00"
    );



    public void validateLocation(String locationId)  {
        logger.info("In Validate Location");
        if (locationId == null || locationId.isEmpty()) {
            throw new ValidationException("Location ID is required");
        }
        Location location;
        try {
             location = locationRepository.findById(locationId);
            if (location == null)
                throw new ResourceNotFoundException("Location not found");

        } catch (Exception e) {
            throw new ResourceNotFoundException("Location not found");
        }

        }

    public void validateGuests(String guestsStr) throws ValidationException {
        if (guestsStr == null || guestsStr.isEmpty()) {
            throw new ValidationException("Number of guests is required");
        }

        try {
            int guests = Integer.parseInt(guestsStr);
            if (guests <= 0) {
                logger.warn("Invalid guests value: " + guests + " (must be positive)");
                throw new ValidationException("Number of guests must be greater than zero.");
            } else if (guests > 20) {
                // Add a reasonable upper limit for number of guests
                logger.warn("Too many guests: " + guests);
                throw new ValidationException("For large parties (over 20 guests), please contact the restaurant directly.");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid guests parameter. Please provide a valid number.");
        }
    }


    public void validateDate(String dateStr) {
            if (dateStr == null || dateStr.isEmpty()) {
                throw new ValidationException("Date is required");
            }

            try {
                // Validate date format (YYYY-MM-DD)
                if (!dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    logger.warn("Invalid date format: {}", dateStr);
                    throw new ValidationException("Invalid date format. Please use YYYY-MM-DD.");
                }

                logger.debug("Checking date format: {}", dateStr);

                // Parse date to ensure it’s valid
                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);

                if (date.isBefore(LocalDate.now(ZoneId.of("Asia/Kolkata")))) {
                    logger.warn("Date is in the past: {}", dateStr);
                    throw new ValidationException("Invalid date. Please select a future date.");
                }

                if (date.isAfter(LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(31))) {
                    logger.warn("Date is too far in the future: {}", dateStr);
                    throw new ValidationException("Invalid date. Please select a date within 31 days.");
                }

            } catch (DateTimeParseException e) {
                logger.warn("Date parse failed: {}, error: {}", dateStr, e.getMessage());
                throw new ValidationException("Invalid date format. Please provide a valid date.");
            }
        }


    public void validateTimeFrom(String timeStr)  {
        if (timeStr == null || timeStr.isEmpty()) {
            throw new ValidationException("Time is required");
        }

        try {
            // Validate time format (HH:MM)
            if (!timeStr.matches("\\d{2}:\\d{2}")) {
                logger.warn("Invalid time format: " + timeStr);
                throw new ValidationException("Invalid time format. Please use HH:MM format.");
            }
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse time: " + timeStr + ", error: " + e.getMessage());
            throw new ValidationException("Invalid time format. Please provide a valid time.");
        }
    }

    public void validateTableId(String locationId, String tableId)  {
        if (!tableService.isValidTableIdForLocationId(locationId, tableId)) {
            throw new ResourceNotFoundException(tableId + " is invalid tableId");
        }
    }

    public  void validateTimeFromReservation(String timeStr)  {
        if (timeStr == null || timeStr.isEmpty()) {
            throw new ValidationException("Start time is required");
        }

        try {
            // Validate time format (HH:MM)
            if (!timeStr.matches("\\d{2}:\\d{2}")) {
                logger.warn("Invalid time format: " + timeStr);
                throw new ValidationException("Invalid time format. Please use HH:MM format.");
            }

            // Parse time to validate it's a real time
            LocalTime time = LocalTime.parse(timeStr);

//            if (!isTimeSlotWithinOpenHours(timeStr)) {
//                logger.warn("Time is not within open hours " + timeStr);
//                throw new ValidationException("Invalid time slot. Please select from 10:30-22:30");
//            }

            if(!isValidTimeSlot(timeStr)){
                logger.warn("Time is not within Slots Choose from 10:30,12:15,14:00, 15:45, 17:30,19:15,21:00 ");
                throw new ValidationException("Invalid time slot. Please select from 10:30,12:15,14:00, 15:45, 17:30,19:15,21:00");
            }
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse time: " + timeStr + ", error: " + e.getMessage());
            throw new ValidationException("Invalid time format. Please provide a valid time.");
        }
    }





    public boolean isValidTimeSlot(String timeSlot) {
        return STANDARD_TIME_SLOTS.contains(timeSlot);
    }


    public void validateReservationRequest(ReservationRequestDto request) {

        // Validate locationId
        validateLocation(request.getLocationId());

        // Validate tableNumber
        if (request.getTableNumber() == null || request.getTableNumber().trim().isEmpty()) {
            throw new ValidationException("Table number is required");
        }
        validateTableId(request.getLocationId(), request.getTableNumber());

        // Validate date
        validateDate(request.getDate());

        // Validate guestsNumber
        validateGuests(request.getGuestsNumber());

        // Validate timeFrom
        validateTimeFromReservation(request.getTimeFrom());

        validateTimeTo(request.getTimeTo(), request.getTimeFrom());


    }


    public void validateTimeTo(String timeTo,String timeFrom){
        // Validate timeTo
        if (timeTo == null || timeTo.trim().isEmpty()) {
            throw new ValidationException("End time is required");
        }

        // Validate time format (HH:MM)
        if (!timeTo.matches("\\d{2}:\\d{2}")) {
            throw new ValidationException("Invalid end time format. Please use HH:MM format");
        }

        try {
            LocalTime endTime = LocalTime.parse(timeTo);
            try {
                LocalTime startTime = LocalTime.parse(timeFrom);
                LocalTime endTime90 = startTime.plusMinutes(90);
                if (endTime.isBefore(startTime) || endTime.equals(startTime) || (!endTime.equals(endTime90))) {
                    throw new ValidationException("End time must be after start time or end time must be of 90 minutes gap");
                }
            } catch (DateTimeParseException e) {
                throw new ValidationException("Invalid end time format. Please provide a valid time");
            }
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid end time format. Please provide a valid time");
        }
    }


    public  boolean isWithin30MinutesOfCreation(String createdAtStr) {
        try {
            // Parse the stored creation timestamp
            LocalDateTime createdAt = LocalDateTime.parse(createdAtStr);
            System.out.println("Created at: " + createdAt);

            // Get current time
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
            System.out.println("Now: " + now);

            // First check if createdAt is in the future
            if (createdAt.isAfter(now)) {
                logger.error("Creation time is in the future!");
                return false;
            }

            // Calculate the duration between creation time and now
            Duration duration = Duration.between(createdAt, now);
            long minutes = duration.toMinutes();
            logger.info("Minutes difference: " + minutes);

            // Check if the duration is less than or equal to 30 minutes
            return minutes <= 30;
        } catch (DateTimeParseException e) {
            logger.error("Error parsing date: " + e.getMessage());
            return false; // If there's an error parsing, assume it's not valid
        }
    }


    public boolean isAfterTime(String time1,String time2){
        LocalTime localTime1=LocalTime.parse(time1);
        LocalTime localTime2=LocalTime.parse(time2);
        return localTime1.isAfter(localTime2);
    }



}

