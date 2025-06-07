package com.epam.edp.demo.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.epam.edp.demo.entity.Feedback;
import com.epam.edp.demo.repository.FeedbackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class FeedbackRepository  {
    public static final String SORT_ORDER_ASC = "asc";
    public static final String SORT_ORDER_DESC = "desc";
    public static final String ATTR_FEEDBACK_ID = "id";
    public static final String ATTR_FEEDBACK_RATE = "rate";
    public static final String ATTR_FEEDBACK_COMMENT = "comment";
    public static final String ATTR_FEEDBACK_USER_NAME = "userName";
    public static final String ATTR_FEEDBACK_USER_AVATAR_URL = "userAvatarUrl";
    public static final String ATTR_FEEDBACK_DATE = "date";
    public static final String ATTR_FEEDBACK_TYPE = "type";
    public static final String ATTR_FEEDBACK_RESERVATION_ID = "reservationId";
    public static final String ATTR_FEEDBACK_LOCATION_ID = "locationId";
    public static final String ENV_DYNAMODB_FEEDBACKS_TABLE = "tm5-restaurant-feedbacks-table-a4v2";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AmazonDynamoDB dynamoDbClient;
    private final String feedbacksTableName;
    private static final int MAX_RESULTS = 100;

    public FeedbackRepository(AmazonDynamoDB dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.feedbacksTableName = ENV_DYNAMODB_FEEDBACKS_TABLE;
        logger.info("Initialized DynamoDbFeedbackRepository with table name: {}", feedbacksTableName);
    }

    public Map<String, Object> findByLocationId(String locationId, String type, int page, int size, String sortBy, String sortOrder) {
        logger.info("Fetching feedbacks for locationId: {}, type: {}, page: {}, size: {}, sortBy: {}, sortOrder: {}",
                locationId, type, page, size, sortBy, sortOrder);

        try {
            // Build the base query
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":locationId", new AttributeValue(locationId));

            String keyConditionExpression = "locationId = :locationId";
            String filterExpression = null;
            Map<String, String> expressionAttributeNames = new HashMap<>();
            String indexName = null;
            boolean scanIndexForward = !SORT_ORDER_DESC.equalsIgnoreCase(sortOrder);

            // Determine which index to use and build filter expressions
            if (type != null && !type.isEmpty()) {
                expressionAttributeValues.put(":type", new AttributeValue(type));
                expressionAttributeNames.put("#typeAttr", "type");
                keyConditionExpression = "locationId = :locationId AND #typeAttr = :type";
                indexName = "type-index";
            }
            else if ("date".equals(sortBy)) {
                indexName = "date-index";
            } else if ("rate".equals(sortBy)) {
                indexName = "rate-index";
            }

            // Build the count query
            QueryRequest countRequest = new QueryRequest()
                    .withTableName(feedbacksTableName)
                    .withKeyConditionExpression(keyConditionExpression)
                    .withExpressionAttributeValues(expressionAttributeValues)
                    .withSelect(Select.COUNT);

            if (!expressionAttributeNames.isEmpty()) {
                countRequest.setExpressionAttributeNames(expressionAttributeNames);
            }

            if (filterExpression != null) {
                countRequest.setFilterExpression(filterExpression);
            }

            if (indexName != null) {
                countRequest.setIndexName(indexName);
            }

            QueryResult countResponse = dynamoDbClient.query(countRequest);
            long totalItems = countResponse.getCount().longValue();
            int totalPages = (int) Math.ceil((double) totalItems / size);

            logger.info("Total feedback count: {}, total pages: {}", totalItems, totalPages);

            int limit = size;
            int offset = page * size;

            QueryRequest queryRequest = new QueryRequest()
                    .withTableName(feedbacksTableName)
                    .withKeyConditionExpression(keyConditionExpression)
                    .withExpressionAttributeValues(expressionAttributeValues)
                    .withScanIndexForward(scanIndexForward)
                    .withLimit(limit);

            if (!expressionAttributeNames.isEmpty()) {
                queryRequest.setExpressionAttributeNames(expressionAttributeNames);
            }

            if (filterExpression != null) {
                queryRequest.setFilterExpression(filterExpression);
            }

            if (indexName != null) {
                queryRequest.setIndexName(indexName);
            }

            // Handle pagination
            List<Map<String, AttributeValue>> items = new ArrayList<>();
            Map<String, AttributeValue> lastEvaluatedKey = null;
            int itemsSkipped = 0;

            while (itemsSkipped < offset) {
                queryRequest.setExclusiveStartKey(lastEvaluatedKey);
                queryRequest.setLimit(Math.min(MAX_RESULTS, offset - itemsSkipped));

                QueryResult response = dynamoDbClient.query(queryRequest);
                itemsSkipped += response.getCount();
                lastEvaluatedKey = response.getLastEvaluatedKey();

                if (lastEvaluatedKey == null || response.getItems().isEmpty()) {
                    // No more items to skip
                    break;
                }
            }

            queryRequest.setExclusiveStartKey(lastEvaluatedKey);
            queryRequest.setLimit(limit);

            QueryResult response = dynamoDbClient.query(queryRequest);
            items = response.getItems();


            List<Feedback> feedbacks = items.stream()
                    .map(this::mapToFeedback)
                    .collect(Collectors.toList());

            logger.info("Final feedback list size for the requested page: {}", feedbacks.size());

            Map<String, Object> result = new HashMap<>();
            result.put("content", feedbacks);
            result.put("totalElements", totalItems);
            result.put("totalPages", totalPages);
            result.put("number", page);
            result.put("size", size);
            result.put("numberOfElements", feedbacks.size());
            result.put("first", page == 0);
            result.put("last", page >= totalPages - 1);
            result.put("empty", feedbacks.isEmpty());
            result.put("sort", Collections.singletonList(
                    Map.of("direction", sortOrder.toUpperCase(), "property", sortBy != null ? sortBy : "date")
            ));
            result.put("pageable", Map.of(
                    "sort", Map.of("sorted", true),
                    "pageNumber", page,
                    "pageSize", size,
                    "offset", offset,
                    "paged", true
            ));

            return result;
        } catch (Exception e) {
            logger.error("Error querying DynamoDB: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching feedbacks", e);
        }
    }

    private Feedback mapToFeedback(Map<String, AttributeValue> item) {
        Feedback feedback = new Feedback();

        if (item.containsKey(ATTR_FEEDBACK_ID)) {
            feedback.setId(item.get(ATTR_FEEDBACK_ID).getS());
        }

        if (item.containsKey(ATTR_FEEDBACK_LOCATION_ID)) {
            feedback.setLocationId(item.get(ATTR_FEEDBACK_LOCATION_ID).getS());
        }

        if (item.containsKey(ATTR_FEEDBACK_TYPE)) {
            feedback.setType(item.get(ATTR_FEEDBACK_TYPE).getS());
        }

        if (item.containsKey(ATTR_FEEDBACK_DATE)) {
            feedback.setDate(item.get(ATTR_FEEDBACK_DATE).getS());
        }

        if (item.containsKey(ATTR_FEEDBACK_RATE)) {
            try {
                feedback.setRate(Integer.parseInt(item.get(ATTR_FEEDBACK_RATE).getN()));
            } catch (NumberFormatException e) {
                logger.error("Error parsing rate value: {}", e.getMessage());
                feedback.setRate(null);
            }
        }

        if (item.containsKey(ATTR_FEEDBACK_COMMENT)) {
            feedback.setComment(item.get(ATTR_FEEDBACK_COMMENT).getS());
        }

        if (item.containsKey(ATTR_FEEDBACK_USER_NAME)) {
            feedback.setUserName(item.get(ATTR_FEEDBACK_USER_NAME).getS());
        }

        if (item.containsKey(ATTR_FEEDBACK_USER_AVATAR_URL)) {
            feedback.setUserAvatarUrl(item.get(ATTR_FEEDBACK_USER_AVATAR_URL).getS());
        }

        if (item.containsKey(ATTR_FEEDBACK_RESERVATION_ID)) {
            feedback.setReservationId(item.get(ATTR_FEEDBACK_RESERVATION_ID).getS());
        }

        logger.debug("Mapped feedback: {}", feedback);
        return feedback;
    }

    public Feedback save(Feedback feedback) {
        logger.info("Saving feedback: {}", feedback);
        Map<String, AttributeValue> item = new HashMap<>();

        if (feedback.getId() != null) {
            item.put(ATTR_FEEDBACK_ID, new AttributeValue(feedback.getId()));
        }

        if (feedback.getLocationId() != null) {
            item.put(ATTR_FEEDBACK_LOCATION_ID, new AttributeValue(feedback.getLocationId()));
        }

        if (feedback.getType() != null) {
            item.put(ATTR_FEEDBACK_TYPE, new AttributeValue(feedback.getType()));
        }

        if (feedback.getDate() != null) {
            item.put(ATTR_FEEDBACK_DATE, new AttributeValue(feedback.getDate()));
        }

        if (feedback.getRate() != null) {
            item.put(ATTR_FEEDBACK_RATE, new AttributeValue().withN(feedback.getRate().toString()));
        }

        if (feedback.getComment() != null) {
            item.put(ATTR_FEEDBACK_COMMENT, new AttributeValue(feedback.getComment()));
        }

        if (feedback.getUserName() != null) {
            item.put(ATTR_FEEDBACK_USER_NAME, new AttributeValue(feedback.getUserName()));
        }

        if (feedback.getUserAvatarUrl() != null) {
            item.put(ATTR_FEEDBACK_USER_AVATAR_URL, new AttributeValue(feedback.getUserAvatarUrl()));
        }

        if (feedback.getReservationId() != null) {
            item.put(ATTR_FEEDBACK_RESERVATION_ID, new AttributeValue(feedback.getReservationId()));
        }

        PutItemRequest putItemRequest = new PutItemRequest(feedbacksTableName, item);
        dynamoDbClient.putItem(putItemRequest);
        logger.info("Feedback saved successfully: {}", feedback.getId());
        return feedback;
    }
}