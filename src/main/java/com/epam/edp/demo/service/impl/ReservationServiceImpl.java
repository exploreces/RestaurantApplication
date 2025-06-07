package com.epam.edp.demo.service.impl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.epam.edp.demo.dto.request.AnonymousVisitorReservationDto;
import com.epam.edp.demo.dto.request.ReservationRequestDto;
import com.epam.edp.demo.dto.request.WaiterReservationRequestDto;
import com.epam.edp.demo.dto.response.ReservationResponse;
import com.epam.edp.demo.dto.response.TableResponseDto;
import com.epam.edp.demo.entity.Reservation;
import com.epam.edp.demo.entity.Table;
import com.epam.edp.demo.exception.ForbiddenException;
import com.epam.edp.demo.exception.TooLateForCancellationException;
import com.epam.edp.demo.exception.ValidationException;
import com.epam.edp.demo.repository.LocationRepository;
import com.epam.edp.demo.repository.ReservationRepository;
import com.epam.edp.demo.repository.TableRepository;
import com.epam.edp.demo.repository.WaiterRepository;
import com.epam.edp.demo.repository.impl.DynamoDbReservationRepository;
import com.epam.edp.demo.service.ReservationService;
import com.epam.edp.demo.validation.Validation;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ReservationServiceImpl implements ReservationService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ReservationRepository reservationRepository;
    private final TableRepository tableRepository;
    private final LocationRepository locationRepository;
    private final WaiterRepository waiterRepository;
    private final Validation validation;
    private final AmazonDynamoDB amazonDynamoDB;



    @Override
    public List<Reservation> getAllReservation() {
        logger.info("Getting all Reservation");
        List<Reservation> reservations = reservationRepository.getAllReservation();
        return reservations;
    }

    @Override
    public boolean statusChange(String id,String status,String email){
       return  reservationRepository.statusChange(id,status,email);
    }

    @Override
    public boolean deleteReservationOfUser(String id){
        reservationRepository.deleteReservation(id);

        return true;
    }

    @Override
    public List<Reservation> getReservationByLocationIdDateTableId(String locationId, String date, String tableId)
    {
        List<Reservation> allReservation=getAllReservation();
       return  allReservation.stream()
                .filter(reservation -> reservation.getLocationId().equals(locationId) &&
                        reservation.getDate().equals(date)
                        && reservation.getTableNumber().equals(tableId)
                && reservation.getStatus().equals("CONFIRMED")||reservation.getStatus().equalsIgnoreCase("IN_PROGRESS"))
                .collect(Collectors.toList());

    }

    @Override
    public List<String> getAvailableTimeSlot(List<Reservation> conflictDateReservation,String time,String date){
        List<String> availableSlot=new ArrayList<>(Arrays.asList(
                "10:30", "12:15", "14:00", "15:45", "17:30", "19:15", "21:00"
        ));

        //Filter According to time
        availableSlot.removeIf(slot -> validation.isAfterTime(time, slot));
        //Filter the Time accoding to Date also
        // Check if the reservation date is today
        LocalDate reservationDate = LocalDate.parse(date); // assumes "yyyy-MM-dd" format
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        if (reservationDate.isEqual(today)) {
            LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));
            availableSlot.removeIf(slot -> LocalTime.parse(slot).isBefore(currentTime));
        }

        if(conflictDateReservation.isEmpty())
            return availableSlot;
        for(Reservation reservation:conflictDateReservation){
            availableSlot.remove(reservation.getTimeFrom());

        }
        return availableSlot;

    }


    @Override
    public List<TableResponseDto> getAvailableSlots(String locationId, String date, String time, String guests){
        logger.info("In the Reseravtion Service Impl ");
        List<Table> tablesByLocationId=tableRepository.getTablesByLocationId(locationId);
        List<Table> tables=tablesByLocationId.stream()
                .filter(t->Integer.parseInt(t.getCapacity())>=Integer.parseInt(guests))
                .collect(Collectors.toList());
        List<TableResponseDto> tableResponeDTOS=new ArrayList<>();
        for(Table table:tables){
            TableResponseDto tableResponeDTO=new TableResponseDto();
            tableResponeDTO.setLocationId(locationId);
            tableResponeDTO.setLocationAddress(locationRepository.findById(locationId).getAddress());
            tableResponeDTO.setCapacity(table.getCapacity());
            tableResponeDTO.setTableNumber(table.getId());
            tableResponeDTO.setAvailableSlots(getAvailableTimeSlot(getReservationByLocationIdDateTableId(locationId,date,table.getId()),time,date));
            if(!tableResponeDTO.getAvailableSlots().isEmpty())
                tableResponeDTOS.add(tableResponeDTO);


        }
        return tableResponeDTOS;
    }

    @Override
    public ReservationResponse createReservation(ReservationRequestDto reservationRequestDto, String email){
        Reservation reservation = new Reservation();
        String reservationId= UUID.randomUUID().toString();
        reservation.setReservationId(reservationId);
        reservation.setDate(reservationRequestDto.getDate());
        reservation.setTableNumber(reservationRequestDto.getTableNumber());//TableId
        reservation.setGuestNumber(reservationRequestDto.getGuestsNumber());
        reservation.setLocationId(reservationRequestDto.getLocationId());
        reservation.setStatus("CONFIRMED");
        reservation.setTimeFrom(reservationRequestDto.getTimeFrom());
        reservation.setTimeTo(reservationRequestDto.getTimeTo());
        String createdAt= LocalDateTime.now(ZoneId.of("Asia/Kolkata")).toString();
        reservation.setCreatedAt(createdAt);
        reservation.setUserId(email);
        reservation.setWaiterId(waiterRepository.getLeastBusyWaiterForLocation(reservationRequestDto.getLocationId()));// Get Method for least busy method.

        logger.info("Reservation Created ");
        reservationRepository.saveReservation(reservation);
        return mapToReservationResponse(reservation);




    }




    public ReservationResponse updateReservation(String reservationId, String userEmail, ReservationRequestDto updateRequest) {
        logger.info("Updating reservation with ID: {}", reservationId);

        // First, get all reservations and find the one with matching ID
        List<Reservation> allReservations = getAllReservation();
        Reservation existingReservation = allReservations.stream()
                .filter(r -> r.getReservationId().equals(reservationId))
                .findFirst()
                .orElse(null);

        // Check if reservation exists
        if (existingReservation == null) {
            logger.warn("Reservation with ID {} not found", reservationId);
            throw new com.epam.edp.demo.exception.ResourceNotFoundException("Reservation Id not found");
        }
        if(!validation.isWithin30MinutesOfCreation(existingReservation.getCreatedAt())){
            throw  new TooLateForCancellationException("Cannot Edit the Reservation after 30 minute");
        }

        // Check if the reservation belongs to the user
        if (!existingReservation.getUserId().equals(userEmail)) {
            logger.warn("User {} attempted to update reservation {} belonging to {}",
                    userEmail, reservationId, existingReservation.getUserId());
            throw new ForbiddenException("You do not have permission to update the reservations made by other users");
        }

        // Check if the reservation is already cancelled
        if ("CANCELLED".equals(existingReservation.getStatus())) {
            logger.warn("Cannot update cancelled reservation {}", reservationId);
            throw new ValidationException("Cannot update a cancelled reservation");
        }


        // Create a copy of the existing reservation to update

        Reservation updatedReservation = new Reservation();
        updatedReservation.setReservationId(reservationId);
        updatedReservation.setUserId(existingReservation.getUserId());
        updatedReservation.setCreatedAt(existingReservation.getCreatedAt());
        updatedReservation.setStatus(existingReservation.getStatus());
        updatedReservation.setWaiterId(existingReservation.getWaiterId());
        updatedReservation.setFeedbackId(existingReservation.getFeedbackId());
        updatedReservation.setOrders(existingReservation.getOrders());
        updatedReservation.setDate(existingReservation.getDate());
        updatedReservation.setLocationId(existingReservation.getLocationId());

        // Update fields from the request, or keep existing values if not provided

        validation.validateTableId(existingReservation.getLocationId(),updateRequest.getTableNumber());
        updatedReservation.setTableNumber(existingReservation.getTableNumber());

        validation.validateGuests(updateRequest.getGuestsNumber());
        updatedReservation.setGuestNumber(updateRequest.getGuestsNumber());


        validation.validateTimeFromReservation(updateRequest.getTimeFrom());
        updatedReservation.setTimeFrom(updateRequest.getTimeFrom());

        validation.validateTimeTo(updateRequest.getTimeTo(),updateRequest.getTimeFrom());
        updatedReservation.setTimeTo(updateRequest.getTimeTo());

            updatedReservation.setWaiterId(
                    waiterRepository.getLeastBusyWaiterForLocation(existingReservation.getLocationId()));

        // Save the updated reservation
        Reservation result = reservationRepository.updateReservation(updatedReservation);

        logger.info("Successfully updated reservation {}", reservationId);
        return mapToReservationResponse(result);
    }


    @Override
    public List<ReservationResponse> getAllReservation(String email){
       List<Reservation> reservations= getAllReservation();
       return reservations.stream()
               .filter(reservation -> reservation.getUserId().equals(email))
               .map(r->mapToReservationResponse(r))
               .collect(Collectors.toList());
    }

    private ReservationResponse mapToReservationResponse(Reservation reservation){
        return new ReservationResponse(
                reservation.getReservationId(),  // id
                reservation.getStatus(),         // status
                locationRepository.findById(reservation.getLocationId()).getAddress(),  // locationAddress (you'll need to implement this method)
                reservation.getDate(),           // date
                reservation.getTimeFrom() + " - " + reservation.getTimeTo(),  // timeSlot
                "",                              // preOrder (not in Reservation model, defaulting to empty)
                reservation.getGuestNumber(),    // guestNumber
                ""                               // feedbackId (not in Reservation model, defaulting to empty)
                        ,reservation.getLocationId()
        );
    }

    @Override
    public ReservationResponse createReservationByWaiter(WaiterReservationRequestDto requestDto,
                                                         String customerEmail, String waiterEmail) {
        logger.info("Creating reservation for customer {} by waiter {}", customerEmail, waiterEmail);

        // Create a new reservation
        Reservation reservation = new Reservation();
        String reservationId = UUID.randomUUID().toString();
        reservation.setReservationId(reservationId);
        reservation.setDate(requestDto.getDate());
        reservation.setTableNumber(requestDto.getTableNumber());
        reservation.setGuestNumber(requestDto.getGuestsNumber());
        reservation.setLocationId(requestDto.getLocationId());
        reservation.setStatus("CONFIRMED");
        reservation.setTimeFrom(requestDto.getTimeFrom());
        reservation.setTimeTo(requestDto.getTimeTo());
        reservation.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")).toString());
        reservation.setUserId(customerEmail);
        reservation.setWaiterId(waiterEmail);

        // Save the reservation
        reservationRepository.saveReservation(reservation);
        logger.info("Successfully created reservation {} for customer {} by waiter {}",
                reservationId, customerEmail, waiterEmail);

        // Return the response
        return mapToReservationResponse(reservation);
    }



    @Override
    public Reservation findReservationById(String reservationId) {
        logger.info("Finding reservation by ID: {}", reservationId);
        List<Reservation> allReservations = getAllReservation();

        return allReservations.stream()
                .filter(r -> r.getReservationId().equals(reservationId))
                .findFirst()
                .orElse(null);
    }
    @Override
    public List<ReservationResponse> getReservationsByWaiter(String waiterEmail) {
        logger.info("Getting all reservations for waiter: {}", waiterEmail);
        List<Reservation> allReservations = getAllReservation();

        // Filter reservations by waiter email and map to response objects
        return allReservations.stream()
                .filter(reservation -> waiterEmail.equals(reservation.getWaiterId()))
                .map(this::mapToReservationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Deletes a reservation from DynamoDB by ID
     * @param id The ID of the reservation to delete
     * @return true if deletion was successful, false otherwise
     */
    @Override
    public boolean deleteReservation(String id) {
        logger.info("Deleting reservation with ID: {}", id);

        try {
            // Create a delete request for DynamoDB
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("reservationId", new AttributeValue().withS(id));

            DeleteItemRequest deleteRequest = new DeleteItemRequest()
                    .withTableName("Reservations")  // Replace with your actual table name
                    .withKey(key)
                    .withReturnValues(ReturnValue.ALL_OLD);  // This returns the deleted item

            // Execute the delete operation
            DeleteItemResult result = amazonDynamoDB.deleteItem(deleteRequest);

            // Check if the item was actually deleted by examining the returned attributes
            Map<String, AttributeValue> returnedAttributes = result.getAttributes();
            boolean wasDeleted = returnedAttributes != null && !returnedAttributes.isEmpty();

            if (wasDeleted) {
                logger.info("Successfully deleted reservation with ID: {}", id);
            } else {
                logger.warn("No reservation found with ID: {} to delete", id);
            }

            return wasDeleted;
        } catch (AmazonServiceException e) {
            logger.error("Amazon service error deleting reservation {}: {}", id, e.getMessage(), e);
            return false;
        } catch (AmazonClientException e) {
            logger.error("Amazon client error deleting reservation {}: {}", id, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error deleting reservation {}: {}", id, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean cancelReservation(String reservationId) {
        logger.info("Cancelling reservation with ID: {}", reservationId);

        try {
            // Get the reservation to verify it exists
            Reservation reservation = findReservationById(reservationId);
            if (reservation == null) {
                logger.warn("Reservation with ID {} not found", reservationId);
                return false;
            }

            // Use the new method to completely delete the reservation
            boolean deleted = ((DynamoDbReservationRepository)reservationRepository).completelyDeleteReservation(reservationId);
            if (deleted) {
                logger.info("Successfully deleted reservation {}", reservationId);
            } else {
                logger.error("Failed to delete reservation {}", reservationId);
            }

            return deleted;
        } catch (Exception e) {
            logger.error("Error cancelling reservation {}: {}", reservationId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public ReservationResponse createAnonymousReservation(AnonymousVisitorReservationDto requestDto,
                                                          String visitorName, String waiterEmail) {
        logger.info("Creating reservation for anonymous visitor {} by waiter {}", visitorName, waiterEmail);

        // Create a new reservation
        Reservation reservation = new Reservation();
        String reservationId = UUID.randomUUID().toString();
        reservation.setReservationId(reservationId);
        reservation.setDate(requestDto.getDate());
        reservation.setTableNumber(requestDto.getTableNumber());
        reservation.setGuestNumber(requestDto.getGuestsNumber());
        reservation.setLocationId(requestDto.getLocationId());
        reservation.setStatus("CONFIRMED");
        reservation.setTimeFrom(requestDto.getTimeFrom());
        reservation.setTimeTo(requestDto.getTimeTo());
        reservation.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")).toString());

        // For anonymous visitors, we'll use a special format for userId
        // This helps us identify it's an anonymous reservation
        reservation.setUserId("ANONYMOUS:" + visitorName);

        // Set the waiter who created this reservation
        reservation.setWaiterId(waiterEmail);

        // Save the reservation
        reservationRepository.saveReservation(reservation);
        logger.info("Successfully created anonymous reservation {} for visitor {} by waiter {}",
                reservationId, visitorName, waiterEmail);

        // Return the response
        return mapToReservationResponse(reservation);
    }

    @Override
    public ReservationResponse postponeReservation(String reservationId, String newDate, String newTimeFrom, String newTimeTo) {
        logger.info("Postponing reservation {} to date: {}, time: {} - {}",
                reservationId, newDate, newTimeFrom, newTimeTo);

        // Find the existing reservation
        Reservation reservation = findReservationById(reservationId);
        if (reservation == null) {
            logger.error("Reservation with ID {} not found", reservationId);
            throw new ResourceNotFoundException("Reservation not found with id: " + reservationId);
        }

        // Update the reservation with new date and time
        reservation.setDate(newDate);
        reservation.setTimeFrom(newTimeFrom);
        reservation.setTimeTo(newTimeTo);

        // Set status to POSTPONED
        reservation.setStatus("POSTPONED");

        // Save the updated reservation
        reservationRepository.saveReservation(reservation);
        logger.info("Successfully postponed reservation {}", reservationId);

        // Return the updated reservation
        return mapToReservationResponse(reservation);
    }

}
