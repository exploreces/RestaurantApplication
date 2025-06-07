package com.epam.edp.demo.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.epam.edp.demo.entity.Waiter;
import com.epam.edp.demo.exception.ResourceNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class WaiterRepository {
    public static final String ENV_DYNAMODB_WAITERS_TABLE = "tm5-restaurant-waiters-table-a4v2";
    private final String waiterTableName;
    public static final String ATTR_EMAIL = "email";
    public static final String ATTR_LOCATION_ID = "locationId";
    public static final String ATTR_RESERVATION_COUNT = "reservationId";
    public static final String ATTR_PASSWORD = "password"; // For storing hashed passwords

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AmazonDynamoDB dynamoDbClient;

    @Autowired
    public WaiterRepository(AmazonDynamoDB dynamoDBClient) {
        this.dynamoDbClient = dynamoDBClient;
        this.waiterTableName = ENV_DYNAMODB_WAITERS_TABLE;
        logger.info("Initialized DynamoDbWaiterRepository with table name: {}", waiterTableName);
    }

    /**
     * Checks if a user with the given email is a waiter
     * @param email The email to check
     * @return true if the user is a waiter, false otherwise
     */
    public boolean isWaiter(String email) {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put(ATTR_EMAIL, new AttributeValue(email));

            GetItemRequest request = new GetItemRequest()
                    .withTableName(waiterTableName)
                    .withKey(key);

            GetItemResult response = dynamoDbClient.getItem(request);
            boolean isWaiter = response.getItem() != null && !response.getItem().isEmpty();
            logger.info("Checked if {} is a waiter: {}", email, isWaiter);
            return isWaiter;
        } catch (Exception e) {
            logger.error("Error checking waiter status: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Gets the least busy waiter for a specific location
     * @param locationId The ID of the location
     * @return The email of the least busy waiter
     * @throws ResourceNotFoundException if no waiters are available at the location
     */
    public String getLeastBusyWaiterForLocation(String locationId) {
        try {
            logger.info("Finding least busy waiter for location: {}", locationId);

            // Scan for waiters at the given location
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":locationId", new AttributeValue(locationId));

            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#locationId", ATTR_LOCATION_ID);

            ScanRequest scanRequest = new ScanRequest()
                    .withTableName(waiterTableName)
                    .withFilterExpression("#locationId = :locationId")
                    .withExpressionAttributeNames(expressionAttributeNames)
                    .withExpressionAttributeValues(expressionAttributeValues);

            ScanResult response = dynamoDbClient.scan(scanRequest);
            List<Map<String, AttributeValue>> waiters = new ArrayList<>(response.getItems());

            if (waiters.isEmpty()) {
                logger.warn("No waiters found for location: {}", locationId);
                throw new ResourceNotFoundException("No waiters available at this location");
            }

            // First, check if all waiters have zero reservations
            boolean allZeroReservations = waiters.stream()
                    .allMatch(waiter -> !waiter.containsKey(ATTR_RESERVATION_COUNT) ||
                            Integer.parseInt(waiter.get(ATTR_RESERVATION_COUNT).getN()) == 0);

            String selectedWaiterEmail;
            if (allZeroReservations) {
                // If all have zero reservations, get the first waiter
                selectedWaiterEmail = waiters.get(0).get(ATTR_EMAIL).getS();
                logger.info("All waiters have zero reservations. Selecting first available waiter: {}",
                        selectedWaiterEmail);
            } else {
                // Find waiter with minimum reservation count
                Map<String, AttributeValue> leastBusyWaiter = waiters.stream()
                        .min((w1, w2) -> {
                            int count1 = w1.containsKey(ATTR_RESERVATION_COUNT) ?
                                    Integer.parseInt(w1.get(ATTR_RESERVATION_COUNT).getN()) : 0;
                            int count2 = w2.containsKey(ATTR_RESERVATION_COUNT) ?
                                    Integer.parseInt(w2.get(ATTR_RESERVATION_COUNT).getN()) : 0;
                            return Integer.compare(count1, count2);
                        })
                        .orElseThrow(() -> new RuntimeException("Error finding least busy waiter"));

                selectedWaiterEmail = leastBusyWaiter.get(ATTR_EMAIL).getS();
                logger.info("Selected least busy waiter: {}", selectedWaiterEmail);
            }

            // Update the reservation count for the selected waiter
            updateWaiterReservationCount(selectedWaiterEmail);

            return selectedWaiterEmail;

        } catch (Exception e) {
            logger.error("Error in getLeastBusyWaiterForLocation: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting least busy waiter", e);
        }
    }

    private void updateWaiterReservationCount(String email) {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put(ATTR_EMAIL, new AttributeValue(email));

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":inc", new AttributeValue().withN("1"));
            expressionAttributeValues.put(":zero", new AttributeValue().withN("0"));

            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#reservationId", "reservationId");

            UpdateItemRequest updateRequest = new UpdateItemRequest()
                    .withTableName(waiterTableName)
                    .withKey(key)
                    .withUpdateExpression("SET #reservationId = if_not_exists(#reservationId, :zero) + :inc")
                    .withExpressionAttributeNames(expressionAttributeNames)
                    .withExpressionAttributeValues(expressionAttributeValues);

            dynamoDbClient.updateItem(updateRequest);
            logger.info("Successfully updated reservation count for waiter: {}", email);

        } catch (Exception e) {
            logger.error("Error updating reservation count for waiter {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Error updating waiter reservation count", e);
        }
    }

    /**
     * Get all waiters in the system
     * @return List of waiter email addresses
     */
    public List<String> getAllWaiters() {
        try {
            logger.info("Getting all waiters");

            ScanRequest scanRequest = new ScanRequest()
                    .withTableName(waiterTableName);

            ScanResult result = dynamoDbClient.scan(scanRequest);

            List<String> waiterEmails = result.getItems().stream()
                    .map(item -> item.get(ATTR_EMAIL).getS())
                    .collect(Collectors.toList());

            logger.info("Retrieved {} waiters", waiterEmails.size());
            return waiterEmails;
        } catch (Exception e) {
            logger.error("Error getting all waiters: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve waiters", e);
        }
    }

    /**
     * Add a new waiter to the system
     * @param email The waiter's email
     * @param password The waiter's password (should be pre-hashed)
     */
    public void addWaiter(String email, String password) {
        try {
            logger.info("Adding new waiter with email: {}", email);

            // Check if waiter already exists
            if (isWaiter(email)) {
                logger.warn("Waiter with email {} already exists", email);
                throw new IllegalArgumentException("Waiter with this email already exists");
            }

            // Create item for new waiter
            Map<String, AttributeValue> item = new HashMap<>();
            item.put(ATTR_EMAIL, new AttributeValue(email));
            item.put(ATTR_PASSWORD, new AttributeValue(password));
            item.put(ATTR_RESERVATION_COUNT, new AttributeValue().withN("0"));

            // Add a default location ID if needed
            // item.put(ATTR_LOCATION_ID, new AttributeValue("default-location"));

            PutItemRequest putItemRequest = new PutItemRequest()
                    .withTableName(waiterTableName)
                    .withItem(item);

            dynamoDbClient.putItem(putItemRequest);
            logger.info("Successfully added waiter: {}", email);
        } catch (Exception e) {
            logger.error("Error adding waiter {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to add waiter", e);
        }
    }

    /**
     * Remove a waiter from the system
     * @param email The email of the waiter to remove
     */
    public void removeWaiter(String email) {
        try {
            logger.info("Removing waiter with email: {}", email);

            // Check if waiter exists
            if (!isWaiter(email)) {
                logger.warn("Waiter with email {} does not exist", email);
                throw new ResourceNotFoundException("Waiter not found with email: " + email);
            }

            // Create key for the waiter to delete
            Map<String, AttributeValue> key = new HashMap<>();
            key.put(ATTR_EMAIL, new AttributeValue(email));

            DeleteItemRequest deleteItemRequest = new DeleteItemRequest()
                    .withTableName(waiterTableName)
                    .withKey(key);

            dynamoDbClient.deleteItem(deleteItemRequest);
            logger.info("Successfully removed waiter: {}", email);
        } catch (ResourceNotFoundException e) {
            logger.warn(e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error removing waiter {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to remove waiter", e);
        }
    }
}