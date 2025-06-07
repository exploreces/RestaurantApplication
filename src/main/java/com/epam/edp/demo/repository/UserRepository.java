package com.epam.edp.demo.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.epam.edp.demo.entity.Roles;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.exceptions.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class UserRepository {
    public static final String ENV_DYNAMODB_USERS_TABLE = "tm5-restaurant-users-table-a4v2";
    public static final String ATTR_EMAIL = "email";
    public static final String ATTR_FIRST_NAME = "firstName";
    public static final String ATTR_PASSWORD = "password";
    public static final String ATTR_LAST_NAME = "lastName";
    public static final String ATTR_IMG_URL = "imageUrl";
    public static final String ATTR_ROLE = "role";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AmazonDynamoDB dynamoDBClient;
    private final String usersTableName;

    public UserRepository(AmazonDynamoDB dynamoDBClient) {
        this.dynamoDBClient = dynamoDBClient;
        this.usersTableName = ENV_DYNAMODB_USERS_TABLE;
        logger.info("Initialized DynamoDbUserRepository with table name: {}", usersTableName);
    }

    public User findByEmail(String email) {
        if (email == null || email.isEmpty()) {
            logger.error("Cannot find user with null or empty email");
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        logger.info("Finding user by email: {}", email);

        Map<String, AttributeValue> key = new HashMap<>();
        key.put(ATTR_EMAIL, new AttributeValue(email));
        GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(usersTableName)
                .withKey(key);
        try {
            GetItemResult result = dynamoDBClient.getItem(getItemRequest);

            if (result.getItem() == null || result.getItem().isEmpty()) {
                logger.info("User not found with email: {}", email);
                return null;
            }
            User user = new User();
            user.setEmail(email);

            Map<String, AttributeValue> item = result.getItem();

            if (item.containsKey("userId")) {
                user.setId(item.get("userId").getS());
            }
            if (item.containsKey(ATTR_FIRST_NAME)) {
                user.setFirstName(item.get(ATTR_FIRST_NAME).getS());
            }
            if (item.containsKey(ATTR_LAST_NAME)) {
                user.setLastName(item.get(ATTR_LAST_NAME).getS());
            }
            if (item.containsKey(ATTR_PASSWORD)) {
                user.setPassword(item.get(ATTR_PASSWORD).getS());
            }
            if (item.containsKey(ATTR_ROLE)) {
                user.setRoles(Roles.valueOf(item.get(ATTR_ROLE).getS()));
            }
            if (item.containsKey(ATTR_IMG_URL)) {
                user.setImageUrl(item.get(ATTR_IMG_URL).getS());
            }

            logger.info("Found user: {}", user.getEmail());
            return user;

        } catch (ResourceNotFoundException e) {
            throw new RepositoryException("DynamoDB table not found", e);
        } catch (ProvisionedThroughputExceededException e) {
            throw new RepositoryException("DynamoDB throughput exceeded", e);
        } catch (AmazonDynamoDBException e) {
            throw new RepositoryException("DynamoDB error finding user", e);
        } catch (Exception e) {
            throw new RepositoryException("Unexpected error finding user", e);
        }
    }

    public void save(User user) {
        if (user == null) {
            logger.error("Cannot save null user");
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            logger.error("Cannot save user with null or empty email");
            throw new IllegalArgumentException("User email cannot be null or empty");
        }
        logger.info("Saving user to DynamoDB: {}", user.getEmail());


        try {
            Map<String, AttributeValue> item = new HashMap<>();

            item.put(ATTR_EMAIL, new AttributeValue(user.getEmail()));
            item.put("userId", new AttributeValue(UUID.randomUUID().toString()));

            if (user.getFirstName() != null) {
                item.put(ATTR_FIRST_NAME, new AttributeValue(user.getFirstName()));
            }
            if (user.getImageUrl() != null) {
                item.put(ATTR_IMG_URL, new AttributeValue(user.getImageUrl()));
            }
            if (user.getLastName() != null) {
                item.put(ATTR_LAST_NAME, new AttributeValue(user.getLastName()));
            }
            if (user.getPassword() != null) {
                item.put(ATTR_PASSWORD, new AttributeValue(user.getPassword()));
            }
            if (user.getRoles() != null) {
                item.put(ATTR_ROLE, new AttributeValue(user.getRoles().name()));
            }
            PutItemRequest putItemRequest = new PutItemRequest()
                    .withTableName(usersTableName)
                    .withItem(item);

            dynamoDBClient.putItem(putItemRequest);
            logger.info("User saved successfully: {}", user.getEmail());
        } catch (AmazonDynamoDBException e) {
            throw new RepositoryException("DynamoDB error saving user", e);
        } catch (RuntimeException e) {
            throw new RepositoryException("Unexpected error saving user", e);
        }
    }

    public boolean existsByEmail(String email) {
        if (email == null || email.isEmpty()) {
            logger.error("Cannot check existence with null or empty email");
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        logger.info("Checking if user exists by email: {}", email);

        Map<String, AttributeValue> key = new HashMap<>();
        key.put(ATTR_EMAIL, new AttributeValue(email));

        GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(usersTableName)
                .withKey(key);

        try {
            GetItemResult result = dynamoDBClient.getItem(getItemRequest);
            boolean exists = result.getItem() != null && !result.getItem().isEmpty();
            logger.info("User exists check for {}: {}", email, exists);
            return exists;
        } catch (ResourceNotFoundException e) {
            throw new RepositoryException("DynamoDB table not found", e);
        } catch (AmazonDynamoDBException e) {
            throw new RepositoryException("DynamoDB error checking user existence", e);
        } catch (RuntimeException e) {
            throw new RepositoryException("Unexpected error checking user existence", e);
        }
    }

    public void deleteUser(String email) {
        if (email == null || email.isEmpty()) {
            logger.error("Cannot delete user with null or empty email");
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        logger.info("Deleting user with email: {}", email);

        Map<String, AttributeValue> key = new HashMap<>();
        key.put(ATTR_EMAIL, new AttributeValue(email));

        try {
            DeleteItemRequest deleteItemRequest = new DeleteItemRequest()
                    .withTableName(usersTableName)
                    .withKey(key);

            dynamoDBClient.deleteItem(deleteItemRequest);
            logger.info("User with email {} deleted successfully", email);
        } catch (ResourceNotFoundException e) {
            throw new RepositoryException("DynamoDB table not found", e);
        } catch (ConditionalCheckFailedException e) {
            throw new RepositoryException("Conditional check failed during deletion", e);
        } catch (ProvisionedThroughputExceededException e) {
            throw new RepositoryException("DynamoDB throughput exceeded", e);
        } catch (AmazonDynamoDBException e) {
            throw new RepositoryException("DynamoDB error deleting user", e);
        } catch (RuntimeException e) {
            throw new RepositoryException("Unexpected error deleting user", e);
        }
    }

        public List<String> getAllUserEmails () {
            logger.info("Retrieving all user emails from table: {}", usersTableName);
            List<String> userEmails = new ArrayList<>();

            try {
                // Create scan request to get all items from the table
                ScanRequest scanRequest = new ScanRequest()
                        .withTableName(usersTableName)
                        .withProjectionExpression(ATTR_EMAIL);

                boolean done = false;
                while (!done) {
                    ScanResult scanResult = dynamoDBClient.scan(scanRequest);

                    for (Map<String, AttributeValue> item : scanResult.getItems()) {
                        if (item.containsKey(ATTR_EMAIL)) {
                            userEmails.add(item.get(ATTR_EMAIL).getS());
                        }
                    }


                    done = scanResult.getLastEvaluatedKey() == null;

                    if (!done) {
                        scanRequest.setExclusiveStartKey(scanResult.getLastEvaluatedKey());
                    }
                }

                logger.info("Successfully retrieved {} user emails", userEmails.size());
                return userEmails;

            } catch (ResourceNotFoundException e) {
                throw new RepositoryException("DynamoDB table not found", e);
            } catch (ProvisionedThroughputExceededException e) {
                throw new RepositoryException("DynamoDB throughput exceeded", e);
            } catch (AmazonDynamoDBException e) {
                throw new RepositoryException("DynamoDB error retrieving user emails", e);
            } catch (Exception e) {
                throw new RepositoryException("Unexpected error retrieving user emails", e);
            }
        }
    }

