package com.epam.edp.demo.repository.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.epam.edp.demo.entity.Reservation;
import com.epam.edp.demo.exception.ForbiddenException;
import com.epam.edp.demo.exception.ResourceNotFoundException;
import com.epam.edp.demo.exception.TooLateForCancellationException;
import com.epam.edp.demo.exception.ValidationException;
import com.epam.edp.demo.repository.ReservationRepository;
import com.epam.edp.demo.validation.Validation;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DynamoDbReservationRepository implements ReservationRepository {
    private final String reservationTableName="tm5-restaurant-reservations-table-a4v2";
    public static final String ATTR_RESERVATION_ID = "reservationId";
    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_LOCATION_ID = "locationId";
    public static final String ATTR_TABLE_NUMBER = "tableNumber";
    public static final String ATTR_TIME_FROM = "timeFrom";
    public static final String ATTR_TIME_TO = "timeTo";
    public static final String ATTR_GUEST_NUMBER = "guestNumber";
    public static final String ATTR_STATUS = "status";
    public static final String ATTR_CREATED_AT = "createdAt";
    public static final String ATTR_WAITER_ID = "waiterId";
    public static final String ATTR_DATE = "date";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AmazonDynamoDB dynamoDbClient;
    private final Validation validation;







    @Override
    public List<Reservation> getAllReservation() {
        logger.info("Finding all Reservation");

        ScanRequest scanRequest = new ScanRequest()
                .withTableName(reservationTableName);
            ScanResult result = dynamoDbClient.scan(scanRequest);
            List<Reservation> reservations = new ArrayList<>();

            for (Map<String, AttributeValue> item : result.getItems()) {
                reservations.add(mapToReservation(item));
            }

        


            logger.info("Found {} reservation", reservations.size());
            return reservations;
    }

    @Override
    public Reservation saveReservation(Reservation reservation) {
        logger.info("Saving reservation with ID: {}", reservation.getReservationId());
            Map<String, AttributeValue> item = new HashMap<>();
            item.put(ATTR_RESERVATION_ID, new AttributeValue(reservation.getReservationId()));
            if (reservation.getUserId() != null) item.put(ATTR_USER_ID, new AttributeValue(reservation.getUserId()));
            if (reservation.getLocationId() != null) item.put(ATTR_LOCATION_ID, new AttributeValue(reservation.getLocationId()));
            if (reservation.getTableNumber() != null) item.put(ATTR_TABLE_NUMBER, new AttributeValue(reservation.getTableNumber()));
            if (reservation.getTimeFrom() != null) item.put(ATTR_TIME_FROM, new AttributeValue(reservation.getTimeFrom()));
            if (reservation.getTimeTo() != null) item.put(ATTR_TIME_TO, new AttributeValue(reservation.getTimeTo()));
            if (reservation.getGuestNumber() != null) item.put(ATTR_GUEST_NUMBER, new AttributeValue().withN(reservation.getGuestNumber()));
            if (reservation.getStatus() != null) item.put(ATTR_STATUS, new AttributeValue(reservation.getStatus()));
            if (reservation.getCreatedAt() != null) item.put(ATTR_CREATED_AT, new AttributeValue(reservation.getCreatedAt()));
            if (reservation.getWaiterId() != null) item.put(ATTR_WAITER_ID, new AttributeValue(reservation.getWaiterId()));
            if (reservation.getDate() != null) item.put(ATTR_DATE, new AttributeValue(reservation.getDate()));

            PutItemRequest putItemRequest = new PutItemRequest().withTableName(reservationTableName).withItem(item);
            dynamoDbClient.putItem(putItemRequest);

            logger.info("Successfully saved reservation with ID: {}", reservation.getReservationId());
            return reservation;
    }



    @Override
    public boolean statusChange(String id,String status,String email){

        logger.info("Changing reservation status with ID: {}", id);
        Map<String, AttributeValue> key = Collections.singletonMap(ATTR_RESERVATION_ID, new AttributeValue(id));
        GetItemRequest getItemRequest = new GetItemRequest().withTableName(reservationTableName).withKey(key);
        Map<String, AttributeValue> item = dynamoDbClient.getItem(getItemRequest).getItem();

        if (item == null || item.isEmpty()) {
            logger.warn("Reservation with ID {} not found", id);
            throw new ResourceNotFoundException("Reservation Id not found");
        }
        // Check if the reservation belongs to the user
        Reservation existingReservation=mapToReservation(item);
        if (!existingReservation.getUserId().equals(email)) {
            logger.warn("User {} attempted to update reservation {} belonging to {}",
                    email, id, existingReservation.getUserId());
            throw new ForbiddenException("You do not have permission to delete reservations made by other users");
        }
        try {

            Map<String, AttributeValueUpdate> updates = new HashMap<>();
            updates.put(ATTR_STATUS, new AttributeValueUpdate().withValue(new AttributeValue(status)).withAction(AttributeAction.PUT));
            UpdateItemRequest updateItemRequest = new UpdateItemRequest().withTableName(reservationTableName).withKey(key).withAttributeUpdates(updates);
            dynamoDbClient.updateItem(updateItemRequest);
            logger.info("Successfully :{} reservation with ID: {}", status, id);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }



    @Override
    public boolean deleteReservation(String id) {

        logger.info("Cancelling reservation with ID: {}", id);
            Map<String, AttributeValue> key = Collections.singletonMap(ATTR_RESERVATION_ID, new AttributeValue(id));
            GetItemRequest getItemRequest = new GetItemRequest().withTableName(reservationTableName).withKey(key);
            Map<String, AttributeValue> item = dynamoDbClient.getItem(getItemRequest).getItem();

            if (item == null || item.isEmpty()) {
                logger.warn("Reservation with ID {} not found", id);
                throw new ResourceNotFoundException("Reservation Id not found");
            }
            if(item.get(ATTR_STATUS).getS().equals("CONFIRMED")) {

                if (validation.isWithin30MinutesOfCreation(item.get(ATTR_CREATED_AT).getS())) {
                    statusChange(id, "CANCELLED", item.get(ATTR_USER_ID).getS());
                    return true;
                } else {
                    throw new TooLateForCancellationException("Cannot cancel the reservation after 30 minutes of booking");
                }
            }
            else{
                throw new ValidationException("You cannot Cancel the Reservation whose status is not CONFIRMED");
            }
        }


    public List<Reservation> getReservationByLocationIdDateTableId(String locationId, String date, String tableId)
    {
        List<Reservation> allReservation=getAllReservation();
        return  allReservation.stream()
                .filter(reservation -> reservation.getLocationId().equals(locationId) &&
                        reservation.getDate().equals(date)
                        && reservation.getTableNumber().equals(tableId)
                        && reservation.getStatus().equals("CONFIRMED"))
                .collect(Collectors.toList());

    }

    public List<String> getAvailableTimeSlot(List<Reservation> conflictDateReservation){
        List<String> availableSlot= Arrays.asList("10:30", "12:15", "14:00", "15:45", "17:30", "19:15", "21:00");
        if(conflictDateReservation.isEmpty())
            return availableSlot;

        for(Reservation reservation:conflictDateReservation){
            if(availableSlot.contains(reservation.getTimeFrom())){
                availableSlot.remove(reservation.getTimeFrom());
            }

        }
        return availableSlot;

    }




    @Override
    public Reservation findByReservationId(String reservationId) {
        logger.info("Finding reservation by ID: {}", reservationId);

        Map<String, AttributeValue> key = Map.of(ATTR_RESERVATION_ID, new AttributeValue(reservationId));

        GetItemRequest request = new GetItemRequest()
                .withTableName(reservationTableName)
                .withKey(key);

        Map<String, AttributeValue> item = dynamoDbClient.getItem(request).getItem();

        if (item == null || item.isEmpty()) {
            throw new ResourceNotFoundException("Reservation Id not Found");
        }

        return mapToReservation(item);
    }



    @Override
    public Reservation updateReservation(Reservation updatedReservation) {
        if (updatedReservation.getReservationId() == null) {
            throw new ValidationException("Reservation ID cannot be null for update operation");
        }

        logger.info("Updating reservation with ID: {}", updatedReservation.getReservationId());

        // Create the key for the reservation to update
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(ATTR_RESERVATION_ID, new AttributeValue(updatedReservation.getReservationId()));

        // Create maps for attribute names and values
        Map<String, String> expressionAttributeNames = new HashMap<>();
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

        // Start building the update expression
        StringBuilder updateExpression = new StringBuilder("SET ");

        // Add each non-null field to the update expression
        List<String> updateExpressions = new ArrayList<>();

        // Check each field of the Reservation object and add to the update expression if not null
        if (updatedReservation.getUserId() != null) {
            updateExpressions.add("#userId = :userId");
            expressionAttributeNames.put("#userId", ATTR_USER_ID);
            expressionAttributeValues.put(":userId", new AttributeValue(updatedReservation.getUserId()));
        }

        if (updatedReservation.getLocationId() != null) {
            updateExpressions.add("#locationId = :locationId");
            expressionAttributeNames.put("#locationId", ATTR_LOCATION_ID);
            expressionAttributeValues.put(":locationId", new AttributeValue(updatedReservation.getLocationId()));
        }

        if (updatedReservation.getTableNumber() != null) {
            updateExpressions.add("#tableNumber = :tableNumber");
            expressionAttributeNames.put("#tableNumber", ATTR_TABLE_NUMBER);
            expressionAttributeValues.put(":tableNumber", new AttributeValue(updatedReservation.getTableNumber()));
        }

        if (updatedReservation.getTimeFrom() != null) {
            updateExpressions.add("#timeFrom = :timeFrom");
            expressionAttributeNames.put("#timeFrom", ATTR_TIME_FROM);
            expressionAttributeValues.put(":timeFrom", new AttributeValue(updatedReservation.getTimeFrom()));
        }

        if (updatedReservation.getTimeTo() != null) {
            updateExpressions.add("#timeTo = :timeTo");
            expressionAttributeNames.put("#timeTo", ATTR_TIME_TO);
            expressionAttributeValues.put(":timeTo", new AttributeValue(updatedReservation.getTimeTo()));
        }

        if (updatedReservation.getGuestNumber() != null) {
            updateExpressions.add("#guestNumber = :guestNumber");
            expressionAttributeNames.put("#guestNumber", ATTR_GUEST_NUMBER);
            expressionAttributeValues.put(":guestNumber", new AttributeValue().withN(updatedReservation.getGuestNumber()));
        }

        if (updatedReservation.getStatus() != null) {
            updateExpressions.add("#status = :status");
            expressionAttributeNames.put("#status", ATTR_STATUS);
            expressionAttributeValues.put(":status", new AttributeValue(updatedReservation.getStatus()));
        }

        if (updatedReservation.getCreatedAt() != null) {
            updateExpressions.add("#createdAt = :createdAt");
            expressionAttributeNames.put("#createdAt", ATTR_CREATED_AT);
            expressionAttributeValues.put(":createdAt", new AttributeValue(updatedReservation.getCreatedAt()));
        }

        if (updatedReservation.getWaiterId() != null) {
            updateExpressions.add("#waiterId = :waiterId");
            expressionAttributeNames.put("#waiterId", ATTR_WAITER_ID);
            expressionAttributeValues.put(":waiterId", new AttributeValue(updatedReservation.getWaiterId()));
        }

        if (updatedReservation.getDate() != null) {
            updateExpressions.add("#date = :date");
            expressionAttributeNames.put("#date", ATTR_DATE);
            expressionAttributeValues.put(":date", new AttributeValue(updatedReservation.getDate()));
        }

        // If no attributes to update, return the original reservation
        if (updateExpressions.isEmpty()) {
            logger.info("No attributes to update for reservation ID: {}", updatedReservation.getReservationId());
            return updatedReservation;
        }

        // Join all update expressions with commas
        updateExpression.append(String.join(", ", updateExpressions));

        // Execute the update
            UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                    .withTableName(reservationTableName)
                    .withKey(key)
                    .withUpdateExpression(updateExpression.toString())
                    .withExpressionAttributeNames(expressionAttributeNames)
                    .withExpressionAttributeValues(expressionAttributeValues)
                    .withReturnValues(ReturnValue.ALL_NEW);

            // Use the AmazonDynamoDB client to execute the update operation
            UpdateItemResult updateItemResult = dynamoDbClient.updateItem(updateItemRequest);

            // Map the response to a Reservation object
            Reservation updatedReservationResponse = mapToReservation(updateItemResult.getAttributes());
            logger.info("Successfully updated reservation with ID: {}", updatedReservation.getReservationId());

            return updatedReservationResponse;
        }

    private Reservation mapToReservation(Map<String, AttributeValue> item) {
        Reservation reservation = new Reservation();

        if (item.containsKey(ATTR_RESERVATION_ID)) {
            reservation.setReservationId(item.get(ATTR_RESERVATION_ID).getS());
        }

        if (item.containsKey(ATTR_USER_ID)) {
            reservation.setUserId(item.get(ATTR_USER_ID).getS());
        }

        if (item.containsKey(ATTR_LOCATION_ID)) {
            reservation.setLocationId(item.get(ATTR_LOCATION_ID).getS());
        }

        if (item.containsKey(ATTR_TABLE_NUMBER)) {
            reservation.setTableNumber(item.get(ATTR_TABLE_NUMBER).getS());
        }


        if (item.containsKey(ATTR_TIME_FROM)) {
            reservation.setTimeFrom(item.get(ATTR_TIME_FROM).getS());
        }

        if (item.containsKey(ATTR_TIME_TO)) {
            reservation.setTimeTo(item.get(ATTR_TIME_TO).getS());
        }

        if (item.containsKey(ATTR_GUEST_NUMBER)) {
            reservation.setGuestNumber(item.get(ATTR_GUEST_NUMBER).getN());
        }

        if (item.containsKey(ATTR_STATUS)) {
            reservation.setStatus(item.get(ATTR_STATUS).getS());
        }

        if (item.containsKey(ATTR_CREATED_AT)) {
            reservation.setCreatedAt(item.get(ATTR_CREATED_AT).getS());
        }

        if (item.containsKey(ATTR_WAITER_ID)) {
            reservation.setWaiterId(item.get(ATTR_WAITER_ID).getS());
        }

        if (item.containsKey(ATTR_DATE)) {
            reservation.setDate(item.get(ATTR_DATE).getS());
        }

        return reservation;
    }


    @Override
    public void updateReservationStatusesBasedOnTime(List<Reservation> reservations) {
        logger.info("Starting scheduled update of reservation statuses based on current time...");


        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));;

        for (Reservation reservation : reservations) {
            if(reservation.getStatus().equals("CANCELLED"))
                continue;
            try {
                if (reservation.getDate() == null || reservation.getTimeFrom() == null || reservation.getTimeTo() == null) {
                    logger.warn("Skipping reservation with missing date/time fields: {}", reservation.getReservationId());
                    continue;
                }

                LocalDateTime start = LocalDateTime.parse(reservation.getDate() + "T" + reservation.getTimeFrom());
                LocalDateTime end = LocalDateTime.parse(reservation.getDate() + "T" + reservation.getTimeTo());

                String newStatus = null;

                if (now.isAfter(start) && now.isBefore(end)) {
                    newStatus = "IN_PROGRESS";
                } else if (now.isAfter(end)) {
                    newStatus = "FINISHED";
                }

                if (newStatus != null && !newStatus.equals(reservation.getStatus())) {
                    logger.info("Updating reservation {} status from {} to {}", reservation.getReservationId(), reservation.getStatus(), newStatus);
                    statusChange(reservation.getReservationId(), newStatus,reservation.getUserId());
                }

            } catch (DateTimeParseException e) {
                logger.error("Failed to parse date/time for reservation ID: {}", reservation.getReservationId(), e);
            }
        }

        logger.info("Completed reservation status update.");
    }

    /**
     * Completely removes a reservation from the database without time restrictions
     * @param id The ID of the reservation to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean completelyDeleteReservation(String id) {
        logger.info("Completely deleting reservation with ID: {}", id);

        try {
            // Create a delete request for DynamoDB
            Map<String, AttributeValue> key = new HashMap<>();
            key.put(ATTR_RESERVATION_ID, new AttributeValue(id));

            DeleteItemRequest deleteRequest = new DeleteItemRequest()
                    .withTableName(reservationTableName)
                    .withKey(key)
                    .withReturnValues(ReturnValue.ALL_OLD);  // This returns the deleted item

            // Execute the delete operation
            DeleteItemResult result = dynamoDbClient.deleteItem(deleteRequest);

            // Check if the item was actually deleted by examining the returned attributes
            Map<String, AttributeValue> returnedAttributes = result.getAttributes();
            boolean wasDeleted = returnedAttributes != null && !returnedAttributes.isEmpty();

            if (wasDeleted) {
                logger.info("Successfully deleted reservation with ID: {}", id);
            } else {
                logger.warn("No reservation found with ID: {} to delete", id);
            }

            return wasDeleted;
        } catch (Exception e) {
            logger.error("Error completely deleting reservation {}: {}", id, e.getMessage(), e);
            return false;
        }
    }

}
