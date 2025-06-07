package com.epam.edp.demo.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.epam.edp.demo.entity.Dish;
import com.epam.edp.demo.exceptions.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class DishRepository {
    // Constants for attribute names - aligned with DynamoDB schema
    public static final String ENV_DYNAMODB_DISHES_TABLE = "tm5-restaurant-dishes-table-a4v2";
    public static final String ATTR_DISH_ID = "id";
    public static final String ATTR_DISH_NAME = "name";
    public static final String ATTR_DISH_DESCRIPTION = "description";
    public static final String ATTR_DISH_PRICE = "price";
    public static final String ATTR_DISH_WEIGHT = "weight";
    public static final String ATTR_DISH_CALORIES = "calories";
    public static final String ATTR_DISH_CARBOHYDRATES = "carbohydrates";
    public static final String ATTR_DISH_PROTEINS = "proteins";
    public static final String ATTR_DISH_FATS = "fats";
    public static final String ATTR_DISH_VITAMINS = "vitamins";
    public static final String ATTR_DISH_TYPE = "dishType";
    public static final String ATTR_DISH_STATE = "state";
    public static final String ATTR_DISH_IMAGE_URL = "imageUrl";
    public static final String ATTR_DISH_LOCATION_ID = "locationId";
    public static final String ATTR_DISH_IS_SPECIALTY = "isSpecialty";
    public static final String ATTR_DISH_IS_POPULAR = "isPopular";
    public static final String ATTR_DISH_RATING = "rating";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AmazonDynamoDB dynamoDBClient;
    private final String dishesTableName;

    public DishRepository(AmazonDynamoDB dynamoDBClient) {
        this.dynamoDBClient = dynamoDBClient;
        this.dishesTableName = ENV_DYNAMODB_DISHES_TABLE;
        logger.info("Initialized DishRepository with table name: {}", dishesTableName);
    }

    public Dish save(Dish dish) {
        if (dish == null) {
            logger.error("Cannot save null dish");
            throw new IllegalArgumentException("Dish cannot be null");
        }

        if (dish.getLocationId() == null || dish.getLocationId().isEmpty()) {
            logger.error("Cannot save dish with null or empty locationId");
            throw new IllegalArgumentException("Dish locationId cannot be null or empty");
        }

        // Generate ID if it's a new dish
        if (dish.getId() == null || dish.getId().isEmpty()) {
            dish.setId(UUID.randomUUID().toString());
        }

        logger.info("Saving dish: {}", dish.getName());

        try {
            Map<String, AttributeValue> item = new HashMap<>();

            // Add required attributes
            item.put(ATTR_DISH_ID, new AttributeValue(dish.getId()));
            item.put(ATTR_DISH_LOCATION_ID, new AttributeValue(dish.getLocationId()));

            // Add other attributes with null checks
            if (dish.getName() != null) {
                item.put(ATTR_DISH_NAME, new AttributeValue(dish.getName()));
            }

            if (dish.getDescription() != null) {
                item.put(ATTR_DISH_DESCRIPTION, new AttributeValue(dish.getDescription()));
            }

            if (dish.getPrice() != null) {
                item.put(ATTR_DISH_PRICE, new AttributeValue(dish.getPrice()));
            }

            if (dish.getWeight() != null) {
                item.put(ATTR_DISH_WEIGHT, new AttributeValue(dish.getWeight()));
            }

            if (dish.getCalories() != null) {
                item.put(ATTR_DISH_CALORIES, new AttributeValue(dish.getCalories()));
            }

            if (dish.getCarbohydrates() != null) {
                item.put(ATTR_DISH_CARBOHYDRATES, new AttributeValue(dish.getCarbohydrates()));
            }

            if (dish.getProteins() != null) {
                item.put(ATTR_DISH_PROTEINS, new AttributeValue(dish.getProteins()));
            }

            if (dish.getFats() != null) {
                item.put(ATTR_DISH_FATS, new AttributeValue(dish.getFats()));
            }

            if (dish.getVitamins() != null) {
                item.put(ATTR_DISH_VITAMINS, new AttributeValue(dish.getVitamins()));
            }

            if (dish.getDishType() != null) {
                item.put(ATTR_DISH_TYPE, new AttributeValue(dish.getDishType()));
            }

            if (dish.getState() != null) {
                item.put(ATTR_DISH_STATE, new AttributeValue(dish.getState()));
            }

            if (dish.getImageUrl() != null) {
                item.put(ATTR_DISH_IMAGE_URL, new AttributeValue(dish.getImageUrl()));
            }

            if (dish.getRating() != null) {
                item.put(ATTR_DISH_RATING, new AttributeValue(dish.getRating()));
            }

            // Add boolean attributes
            item.put(ATTR_DISH_IS_SPECIALTY, new AttributeValue().withBOOL(dish.isSpecialty()));
            item.put(ATTR_DISH_IS_POPULAR, new AttributeValue().withBOOL(dish.isPopular()));

            PutItemRequest putItemRequest = new PutItemRequest()
                    .withTableName(dishesTableName)
                    .withItem(item);

            dynamoDBClient.putItem(putItemRequest);
            logger.info("Successfully saved dish with ID: {} and locationId: {}", dish.getId(), dish.getLocationId());
            return dish;
        } catch (ResourceNotFoundException e) {
            logger.error("Table {} does not exist: {}", dishesTableName, e.getMessage());
            throw new RepositoryException("DynamoDB table not found", e);
        } catch (AmazonDynamoDBException e) {
            logger.error("DynamoDB error saving dish {}: {}", dish.getName(), e.getMessage(), e);
            throw new RepositoryException("DynamoDB error saving dish", e);
        } catch (RuntimeException e) {
            logger.error("Unexpected error saving dish {}: {}", dish.getName(), e.getMessage(), e);
            throw new RepositoryException("Unexpected error saving dish", e);
        }
    }

    public Optional<Dish> findById(String id) {
        if (id == null || id.isEmpty()) {
            logger.error("Cannot find dish with null or empty id");
            throw new IllegalArgumentException("Dish ID cannot be null or empty");
        }

        logger.info("Finding dish by id: {}", id);

        try {
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#id", ATTR_DISH_ID);

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":id", new AttributeValue().withS(id));

            ScanRequest scanRequest = new ScanRequest()
                    .withTableName(dishesTableName)
                    .withFilterExpression("#id = :id")
                    .withExpressionAttributeNames(expressionAttributeNames)
                    .withExpressionAttributeValues(expressionAttributeValues);

            ScanResult scanResult = dynamoDBClient.scan(scanRequest);

            if (scanResult.getItems() == null || scanResult.getItems().isEmpty()) {
                logger.info("Dish not found with id: {}", id);
                return Optional.empty();
            }

            Dish dish = mapToDish(scanResult.getItems().get(0));
            logger.info("Found dish: {}", dish.getName());
            return Optional.of(dish);
        } catch (ResourceNotFoundException e) {
            logger.error("Table {} does not exist: {}", dishesTableName, e.getMessage());
            throw new RepositoryException("DynamoDB table not found", e);
        } catch (ProvisionedThroughputExceededException e) {
            logger.error("Throughput exceeded for table {}: {}", dishesTableName, e.getMessage());
            throw new RepositoryException("DynamoDB throughput exceeded", e);
        } catch (AmazonDynamoDBException e) {
            logger.error("DynamoDB error finding dish by id {}: {}", id, e.getMessage(), e);
            throw new RepositoryException("DynamoDB error finding dish", e);
        } catch (Exception e) {
            logger.error("Unexpected error finding dish by id {}: {}", id, e.getMessage(), e);
            throw new RepositoryException("Unexpected error finding dish", e);
        }
    }

    public List<Dish> findAll() {
        logger.info("Finding all dishes");

        try {
            ScanRequest scanRequest = new ScanRequest()
                    .withTableName(dishesTableName);

            ScanResult scanResult = dynamoDBClient.scan(scanRequest);
            List<Dish> dishes = new ArrayList<>();

            if (scanResult.getItems() != null) {
                for (Map<String, AttributeValue> item : scanResult.getItems()) {
                    dishes.add(mapToDish(item));
                }
            }

            return dishes;
        } catch (ResourceNotFoundException e) {
            logger.error("Table {} does not exist: {}", dishesTableName, e.getMessage());
            throw new RepositoryException("DynamoDB table not found", e);
        } catch (ProvisionedThroughputExceededException e) {
            logger.error("Throughput exceeded for table {}: {}", dishesTableName, e.getMessage());
            throw new RepositoryException("DynamoDB throughput exceeded", e);
        } catch (AmazonDynamoDBException e) {
            logger.error("DynamoDB error finding all dishes: {}", e.getMessage(), e);
            throw new RepositoryException("DynamoDB error finding all dishes", e);
        } catch (Exception e) {
            logger.error("Unexpected error finding all dishes: {}", e.getMessage(), e);
            throw new RepositoryException("Unexpected error finding all dishes", e);
        }
    }

    public List<Dish> findByLocationId(String locationId) {
        if (locationId == null || locationId.isEmpty()) {
            logger.error("Cannot find dishes with null or empty locationId");
            throw new IllegalArgumentException("Location ID cannot be null or empty");
        }

        logger.info("Finding dishes for location: {}", locationId);

        try {
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#locationId", ATTR_DISH_LOCATION_ID);

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":locationId", new AttributeValue().withS(locationId));

            QueryRequest queryRequest = new QueryRequest()
                    .withTableName(dishesTableName)
                    .withKeyConditionExpression("#locationId = :locationId")
                    .withExpressionAttributeNames(expressionAttributeNames)
                    .withExpressionAttributeValues(expressionAttributeValues);

            QueryResult queryResult = dynamoDBClient.query(queryRequest);
            List<Dish> dishes = new ArrayList<>();

            if (queryResult.getItems() != null) {
                for (Map<String, AttributeValue> item : queryResult.getItems()) {
                    dishes.add(mapToDish(item));
                }
            }

            logger.info("Found {} dishes for location {}", dishes.size(), locationId);
            return dishes;
        } catch (ResourceNotFoundException e) {
            logger.error("Table {} does not exist: {}", dishesTableName, e.getMessage());
            throw new RepositoryException("DynamoDB table not found", e);
        } catch (ProvisionedThroughputExceededException e) {
            logger.error("Throughput exceeded for table {}: {}", dishesTableName, e.getMessage());
            throw new RepositoryException("DynamoDB throughput exceeded", e);
        } catch (AmazonDynamoDBException e) {
            logger.error("DynamoDB error finding dishes by location {}: {}", locationId, e.getMessage(), e);
            throw new RepositoryException("DynamoDB error finding dishes by location", e);
        } catch (Exception e) {
            logger.error("Unexpected error finding dishes by location {}: {}", locationId, e.getMessage(), e);
            throw new RepositoryException("Unexpected error finding dishes by location", e);
        }
    }

    public List<Dish> findPopularDishes() {
        logger.info("Finding popular dishes");

        try {
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#isPopular", ATTR_DISH_IS_POPULAR);

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":isPopular", new AttributeValue().withBOOL(true));

            ScanRequest scanRequest = new ScanRequest()
                    .withTableName(dishesTableName)
                    .withFilterExpression("#isPopular = :isPopular")
                    .withExpressionAttributeNames(expressionAttributeNames)
                    .withExpressionAttributeValues(expressionAttributeValues);

            ScanResult scanResult = dynamoDBClient.scan(scanRequest);
            List<Dish> dishes = new ArrayList<>();

            if (scanResult.getItems() != null) {
                for (Map<String, AttributeValue> item : scanResult.getItems()) {
                    dishes.add(mapToDish(item));
                }
            }

            logger.info("Found {} popular dishes", dishes.size());
            return dishes;
        } catch (ResourceNotFoundException e) {
            logger.error("Table {} does not exist: {}", dishesTableName, e.getMessage());
            throw new RepositoryException("DynamoDB table not found", e);
        } catch (ProvisionedThroughputExceededException e) {
            logger.error("Throughput exceeded for table {}: {}", dishesTableName, e.getMessage());
            throw new RepositoryException("DynamoDB throughput exceeded", e);
        } catch (AmazonDynamoDBException e) {
            logger.error("DynamoDB error finding popular dishes: {}", e.getMessage(), e);
            throw new RepositoryException("DynamoDB error finding popular dishes", e);
        } catch (Exception e) {
            logger.error("Unexpected error finding popular dishes: {}", e.getMessage(), e);
            throw new RepositoryException("Unexpected error finding popular dishes", e);
        }
    }

    public List<Dish> findSpecialtyDishesByLocationId(String locationId) {
        if (locationId == null || locationId.isEmpty()) {
            logger.error("Cannot find specialty dishes with null or empty locationId");
            throw new IllegalArgumentException("Location ID cannot be null or empty");
        }

        logger.info("Finding specialty dishes for location: {}", locationId);

        try {
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#locationId", ATTR_DISH_LOCATION_ID);
            expressionAttributeNames.put("#isSpecialty", ATTR_DISH_IS_SPECIALTY);

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":locationId", new AttributeValue().withS(locationId));
            expressionAttributeValues.put(":isSpecialty", new AttributeValue().withBOOL(true));

            QueryRequest queryRequest = new QueryRequest()
                    .withTableName(dishesTableName)
                    .withKeyConditionExpression("#locationId = :locationId")
                    .withFilterExpression("#isSpecialty = :isSpecialty")
                    .withExpressionAttributeNames(expressionAttributeNames)
                    .withExpressionAttributeValues(expressionAttributeValues);

            QueryResult queryResult = dynamoDBClient.query(queryRequest);
            List<Dish> dishes = new ArrayList<>();

            if (queryResult.getItems() != null) {
                for (Map<String, AttributeValue> item : queryResult.getItems()) {
                    dishes.add(mapToDish(item));
                }
            }

            logger.info("Found {} specialty dishes for location {}", dishes.size(), locationId);
            return dishes;
        } catch (ResourceNotFoundException e) {
            logger.error("Table {} does not exist: {}", dishesTableName, e.getMessage());
            throw new RepositoryException("DynamoDB table not found", e);
        } catch (ProvisionedThroughputExceededException e) {
            logger.error("Throughput exceeded for table {}: {}", dishesTableName, e.getMessage());
            throw new RepositoryException("DynamoDB throughput exceeded", e);
        } catch (AmazonDynamoDBException e) {
            logger.error("DynamoDB error finding specialty dishes by location {}: {}", locationId, e.getMessage(), e);
            throw new RepositoryException("DynamoDB error finding specialty dishes by location", e);
        } catch (Exception e) {
            logger.error("Unexpected error finding specialty dishes by location {}: {}", locationId, e.getMessage(), e);
            throw new RepositoryException("Unexpected error finding specialty dishes by location", e);
        }
    }

    public boolean existsByLocationId(String locationId) {
        if (locationId == null || locationId.isEmpty()) {
            logger.error("Cannot check existence with null or empty locationId");
            throw new IllegalArgumentException("Location ID cannot be null or empty");
        }

        logger.info("Checking if dishes exist for location: {}", locationId);

        try {
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":locationId", new AttributeValue().withS(locationId));

            QueryRequest queryRequest = new QueryRequest()
                    .withTableName(dishesTableName)
                    .withKeyConditionExpression("locationId = :locationId")
                    .withExpressionAttributeValues(expressionAttributeValues)
                    .withLimit(1);

            QueryResult queryResult = dynamoDBClient.query(queryRequest);
            boolean exists = queryResult.getItems() != null && !queryResult.getItems().isEmpty();

            logger.info("Dishes exist check for location {}: {}", locationId, exists);
            return exists;
        } catch (ResourceNotFoundException e) {
            logger.error("Table {} does not exist: {}", dishesTableName, e.getMessage());
            throw new RepositoryException("DynamoDB table not found", e);
        } catch (AmazonDynamoDBException e) {
            logger.error("DynamoDB error checking if dishes exist by location {}: {}", locationId, e.getMessage(), e);
            throw new RepositoryException("DynamoDB error checking dishes existence", e);
        } catch (RuntimeException e) {
            logger.error("Unexpected error checking if dishes exist by location {}: {}", locationId, e.getMessage(), e);
            throw new RepositoryException("Unexpected error checking dishes existence", e);
        }
    }

    public void deleteDish(String id) {
        if (id == null || id.isEmpty()) {
            logger.error("Cannot delete dish with null or empty id");
            throw new IllegalArgumentException("Dish ID cannot be null or empty");
        }

        // First, find the dish to get its locationId (needed for the key)
        Optional<Dish> dishOptional = findById(id);
        if (!dishOptional.isPresent()) {
            logger.warn("Dish with id {} not found for deletion", id);
            return;
        }

        Dish dish = dishOptional.get();
        String locationId = dish.getLocationId();

        logger.info("Deleting dish with id: {} and locationId: {}", id, locationId);

        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put(ATTR_DISH_LOCATION_ID, new AttributeValue().withS(locationId));
            key.put(ATTR_DISH_ID, new AttributeValue().withS(id));

            DeleteItemRequest deleteItemRequest = new DeleteItemRequest()
                    .withTableName(dishesTableName)
                    .withKey(key);

            dynamoDBClient.deleteItem(deleteItemRequest);
            logger.info("Dish with id {} deleted successfully", id);
        } catch (ResourceNotFoundException e) {
            logger.error("Table {} does not exist: {}", dishesTableName, e.getMessage());
            throw new RepositoryException("DynamoDB table not found", e);
        } catch (ConditionalCheckFailedException e) {
            logger.error("Conditional check failed when deleting dish {}: {}", id, e.getMessage());
            throw new RepositoryException("Conditional check failed during deletion", e);
        } catch (ProvisionedThroughputExceededException e) {
            logger.error("Throughput exceeded for table {} when deleting dish {}: {}", dishesTableName, id, e.getMessage());
            throw new RepositoryException("DynamoDB throughput exceeded", e);
        } catch (AmazonDynamoDBException e) {
            logger.error("DynamoDB error deleting dish with id {}: {}", id, e.getMessage(), e);
            throw new RepositoryException("DynamoDB error deleting dish", e);
        } catch (RuntimeException e) {
            logger.error("Unexpected error deleting dish with id {}: {}", id, e.getMessage(), e);
            throw new RepositoryException("Unexpected error deleting dish", e);
        }
    }

    private Dish mapToDish(Map<String, AttributeValue> item) {
        Dish dish = new Dish();

        if (item.containsKey(ATTR_DISH_ID)) {
            dish.setId(item.get(ATTR_DISH_ID).getS());
        }

        if (item.containsKey(ATTR_DISH_NAME)) {
            dish.setName(item.get(ATTR_DISH_NAME).getS());
        }

        if (item.containsKey(ATTR_DISH_DESCRIPTION)) {
            dish.setDescription(item.get(ATTR_DISH_DESCRIPTION).getS());
        }

        if (item.containsKey(ATTR_DISH_PRICE)) {
            dish.setPrice(item.get(ATTR_DISH_PRICE).getS());
        }

        if (item.containsKey(ATTR_DISH_WEIGHT)) {
            dish.setWeight(item.get(ATTR_DISH_WEIGHT).getS());
        }

        if (item.containsKey(ATTR_DISH_CALORIES)) {
            dish.setCalories(item.get(ATTR_DISH_CALORIES).getS());
        }

        if (item.containsKey(ATTR_DISH_CARBOHYDRATES)) {
            dish.setCarbohydrates(item.get(ATTR_DISH_CARBOHYDRATES).getS());
        }

        if (item.containsKey(ATTR_DISH_PROTEINS)) {
            dish.setProteins(item.get(ATTR_DISH_PROTEINS).getS());
        }

        if (item.containsKey(ATTR_DISH_FATS)) {
            dish.setFats(item.get(ATTR_DISH_FATS).getS());
        }

        if (item.containsKey(ATTR_DISH_VITAMINS)) {
            dish.setVitamins(item.get(ATTR_DISH_VITAMINS).getS());
        }

        if (item.containsKey(ATTR_DISH_TYPE)) {
            dish.setDishType(item.get(ATTR_DISH_TYPE).getS());
        }

        if (item.containsKey(ATTR_DISH_STATE)) {
            dish.setState(item.get(ATTR_DISH_STATE).getS());
        }

        if (item.containsKey(ATTR_DISH_IMAGE_URL)) {
            dish.setImageUrl(item.get(ATTR_DISH_IMAGE_URL).getS());
        }

        if (item.containsKey(ATTR_DISH_LOCATION_ID)) {
            dish.setLocationId(item.get(ATTR_DISH_LOCATION_ID).getS());
        }

        if (item.containsKey(ATTR_DISH_IS_SPECIALTY)) {
            dish.setSpecialty(item.get(ATTR_DISH_IS_SPECIALTY).getBOOL());
        }

        if (item.containsKey(ATTR_DISH_IS_POPULAR)) {
            dish.setPopular(item.get(ATTR_DISH_IS_POPULAR).getBOOL());
        }

        if (item.containsKey(ATTR_DISH_RATING)) {
            dish.setRating(item.get(ATTR_DISH_RATING).getS());
        }

        return dish;
    }
}