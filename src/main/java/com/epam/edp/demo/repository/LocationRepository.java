package com.epam.edp.demo.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.epam.edp.demo.dto.request.LocationRequestDTO;
import com.epam.edp.demo.dto.response.SpecialityDishDto;
import com.epam.edp.demo.entity.Location;
import com.epam.edp.demo.exceptions.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class LocationRepository {
    public static final String ENV_DYNAMODB_LOCATIONS_TABLE = "tm5-restaurant-locations-table-a4v2";
    public static final String ATTR_LOCATION_ID = "id";
    public static final String ATTR_LOCATION_ADDRESS = "address";
    public static final String ATTR_LOCATION_DESCRIPTION = "description";
    public static final String ATTR_LOCATION_TOTAL_CAPACITY = "totalCapacity";
    public static final String ATTR_LOCATION_AVERAGE_OCCUPANCY = "averageOccupancy";
    public static final String ATTR_LOCATION_IMAGE_URL = "imageUrl";
    public static final String ATTR_LOCATION_RATING = "rating";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AmazonDynamoDB dynamoDBClient;
    private final String locationsTableName;

    public LocationRepository(AmazonDynamoDB dynamoDBClient) {
        this.dynamoDBClient = dynamoDBClient;
        this.locationsTableName = ENV_DYNAMODB_LOCATIONS_TABLE;
        logger.info("Initialized LocationRepository with table name: {}", locationsTableName);
    }

    public List<Location> findAll() {
        logger.info("Finding all locations");

        try {
            ScanRequest scanRequest = new ScanRequest()
                    .withTableName(locationsTableName);

            ScanResult scanResult = dynamoDBClient.scan(scanRequest);
            List<Location> locations = new ArrayList<>();

            if (scanResult.getItems() != null) {
                for (Map<String, AttributeValue> item : scanResult.getItems()) {
                    locations.add(mapToLocation(item));
                }
            }

            logger.info("Found {} locations", locations.size());
            return locations;
        } catch (ResourceNotFoundException e) {
            logger.error("Table {} does not exist: {}", locationsTableName, e.getMessage());
            throw new RepositoryException("DynamoDB table not found", e);
        } catch (ProvisionedThroughputExceededException e) {
            logger.error("Throughput exceeded for table {}: {}", locationsTableName, e.getMessage());
            throw new RepositoryException("DynamoDB throughput exceeded", e);
        } catch (AmazonDynamoDBException e) {
            logger.error("DynamoDB error finding all locations: {}", e.getMessage(), e);
            throw new RepositoryException("DynamoDB error finding all locations", e);
        } catch (Exception e) {
            logger.error("Unexpected error finding all locations: {}", e.getMessage(), e);
            throw new RepositoryException("Unexpected error finding all locations", e);
        }
    }

    public Location createLocation(String id, LocationRequestDTO createLocationDto) {
        if (createLocationDto == null) {
            logger.error("Cannot create location with null DTO");
            throw new IllegalArgumentException("Location DTO cannot be null");
        }

        if (id == null || id.isEmpty()) {
            logger.error("Cannot create location with null or empty id");
            throw new IllegalArgumentException("Location ID cannot be null or empty");
        }

        logger.info("Creating new location with ID: {}", id);

        try {
            Map<String, AttributeValue> item = new HashMap<>();

            // Add required attributes
            item.put(ATTR_LOCATION_ID, new AttributeValue(id));

            // Add other attributes with null checks
            if (createLocationDto.getAddress() != null) {
                item.put(ATTR_LOCATION_ADDRESS, new AttributeValue(createLocationDto.getAddress()));
            }

            if (createLocationDto.getDescription() != null) {
                item.put(ATTR_LOCATION_DESCRIPTION, new AttributeValue(createLocationDto.getDescription()));
            }

            if (createLocationDto.getTotalCapacity() != null) {
                item.put(ATTR_LOCATION_TOTAL_CAPACITY, new AttributeValue(createLocationDto.getTotalCapacity()));
            }

            if (createLocationDto.getAverageOccupancy() != null) {
                item.put(ATTR_LOCATION_AVERAGE_OCCUPANCY, new AttributeValue(createLocationDto.getAverageOccupancy()));
            }

            if (createLocationDto.getImageUrl() != null) {
                item.put(ATTR_LOCATION_IMAGE_URL, new AttributeValue(createLocationDto.getImageUrl()));
            }

            if (createLocationDto.getRating() != null) {
                item.put(ATTR_LOCATION_RATING, new AttributeValue(createLocationDto.getRating()));
            }

            // Add empty collections like in the original code
            item.put("waiters", new AttributeValue().withL(new ArrayList<>()));
            item.put("dishes", new AttributeValue().withL(new ArrayList<>()));
            item.put("tables", new AttributeValue().withL(new ArrayList<>()));

            PutItemRequest putItemRequest = new PutItemRequest()
                    .withTableName(locationsTableName)
                    .withItem(item);

            dynamoDBClient.putItem(putItemRequest);
            logger.info("Successfully created location with ID: {}", id);

            // Create and return the location object
            Location location = new Location();
            location.setId(id);
            location.setAddress(createLocationDto.getAddress());
            location.setDescription(createLocationDto.getDescription());
            location.setTotalCapacity(createLocationDto.getTotalCapacity());
            location.setAverageOccupancy(createLocationDto.getAverageOccupancy());
            location.setImageUrl(createLocationDto.getImageUrl());
            location.setRating(createLocationDto.getRating());

            return location;
        } catch (ResourceNotFoundException e) {
            logger.error("Table {} does not exist: {}", locationsTableName, e.getMessage());
            throw new RepositoryException("DynamoDB table not found", e);
        } catch (AmazonDynamoDBException e) {
            logger.error("DynamoDB error creating location: {}", e.getMessage(), e);
            throw new RepositoryException("DynamoDB error creating location", e);
        } catch (RuntimeException e) {
            logger.error("Unexpected error creating location: {}", e.getMessage(), e);
            throw new RepositoryException("Unexpected error creating location", e);
        }
    }

    public List<SpecialityDishDto> getSpecialDishes(String locationId) {
        if (locationId == null || locationId.isEmpty()) {
            logger.error("Cannot get special dishes with null or empty locationId");
            throw new IllegalArgumentException("Location ID cannot be null or empty");
        }

        logger.info("Fetching special dishes for location: {}", locationId);

        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put(ATTR_LOCATION_ID, new AttributeValue().withS(locationId));

            GetItemRequest getItemRequest = new GetItemRequest()
                    .withTableName(locationsTableName)
                    .withKey(key);

            GetItemResult response = dynamoDBClient.getItem(getItemRequest);

            if (response.getItem() == null || response.getItem().isEmpty()) {
                logger.info("Location with ID {} not found", locationId);
                return new ArrayList<>();
            }

            if (!response.getItem().containsKey("dishes") ||
                    response.getItem().get("dishes").getL() == null ||
                    response.getItem().get("dishes").getL().isEmpty()) {
                logger.info("No dishes found for location with ID {}", locationId);
                return new ArrayList<>();
            }

            List<AttributeValue> dishes = response.getItem().get("dishes").getL();

            return dishes.stream()
                    .map(dish -> dish.getM())
                    .filter(dish -> dish.containsKey("isSpecial") &&
                            Boolean.TRUE.equals(dish.get("isSpecial").getBOOL()))
                    .map(dish -> new SpecialityDishDto(
                            dish.get("id").getS(),
                            dish.get("name").getS(),
                            dish.get("price").getS(),
                            dish.get("imageUrl").getS()
                    ))
                    .collect(Collectors.toList());

        } catch (ResourceNotFoundException e) {
            logger.error("Table {} does not exist: {}", locationsTableName, e.getMessage());
            throw new RepositoryException("DynamoDB table not found", e);
        } catch (AmazonDynamoDBException e) {
            logger.error("DynamoDB error fetching special dishes: {}", e.getMessage(), e);
            throw new RepositoryException("DynamoDB error fetching special dishes", e);
        } catch (RuntimeException e) {
            logger.error("Unexpected error fetching special dishes: {}", e.getMessage(), e);
            throw new RepositoryException("Unexpected error fetching special dishes", e);
        }
    }

    public Location findById(String id) {
        if (id == null || id.isEmpty()) {
            logger.error("Cannot find location with null or empty id");
            throw new IllegalArgumentException("Location ID cannot be null or empty");
        }

        logger.info("Finding location by id: {}", id);

        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put(ATTR_LOCATION_ID, new AttributeValue().withS(id));

            GetItemRequest getItemRequest = new GetItemRequest()
                    .withTableName(locationsTableName)
                    .withKey(key);

            GetItemResult response = dynamoDBClient.getItem(getItemRequest);

            if (response.getItem() == null || response.getItem().isEmpty()) {
                logger.info("Location not found with id: {}", id);
                return null;
            }

            Location location = mapToLocation(response.getItem());
            logger.info("Found location: {}", location.getAddress());
            return location;
        } catch (ResourceNotFoundException e) {
            logger.error("Table {} does not exist: {}", locationsTableName, e.getMessage());
            throw new RepositoryException("DynamoDB table not found", e);
        } catch (AmazonDynamoDBException e) {
            logger.error("DynamoDB error finding location by id {}: {}", id, e.getMessage(), e);
            throw new RepositoryException("DynamoDB error finding location", e);
        } catch (Exception e) {
            logger.error("Unexpected error finding location by id {}: {}", id, e.getMessage(), e);
            throw new RepositoryException("Unexpected error finding location", e);
        }
    }

    public boolean existsById(String id) {
        if (id == null || id.isEmpty()) {
            logger.error("Cannot check existence with null or empty id");
            throw new IllegalArgumentException("Location ID cannot be null or empty");
        }

        logger.info("Checking if location exists with id: {}", id);
        return findById(id) != null;
    }

    private Location mapToLocation(Map<String, AttributeValue> item) {
        Location location = new Location();

        if (item.containsKey(ATTR_LOCATION_ID)) {
            location.setId(item.get(ATTR_LOCATION_ID).getS());
        }

        if (item.containsKey(ATTR_LOCATION_ADDRESS)) {
            location.setAddress(item.get(ATTR_LOCATION_ADDRESS).getS());
        }

        if (item.containsKey(ATTR_LOCATION_DESCRIPTION)) {
            location.setDescription(item.get(ATTR_LOCATION_DESCRIPTION).getS());
        }

        if (item.containsKey(ATTR_LOCATION_TOTAL_CAPACITY)) {
            location.setTotalCapacity(item.get(ATTR_LOCATION_TOTAL_CAPACITY).getS());
        }

        if (item.containsKey(ATTR_LOCATION_AVERAGE_OCCUPANCY)) {
            location.setAverageOccupancy(item.get(ATTR_LOCATION_AVERAGE_OCCUPANCY).getS());
        }

        if (item.containsKey(ATTR_LOCATION_IMAGE_URL)) {
            location.setImageUrl(item.get(ATTR_LOCATION_IMAGE_URL).getS());
        }

        if (item.containsKey(ATTR_LOCATION_RATING)) {
            location.setRating(item.get(ATTR_LOCATION_RATING).getS());
        }

        return location;
    }
}