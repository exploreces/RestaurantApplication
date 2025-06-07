package com.epam.edp.demo.entity;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(tableName = "tm5-restaurant-reservations-table-a4v2")
public class Reservation {



    @DynamoDBHashKey(attributeName = "reservationId")
    private String reservationId;

    @DynamoDBAttribute(attributeName = "userId")
    private String userId;

    @DynamoDBAttribute(attributeName = "locationId")
    private String locationId;

    @DynamoDBAttribute(attributeName = "waiterId")
    private String waiterId;

    @DynamoDBAttribute(attributeName = "createdAt")
    private String createdAt;

    @DynamoDBAttribute(attributeName = "tableNumber")
    private String tableNumber;

    @DynamoDBAttribute(attributeName = "date")
    private String date;

    @DynamoDBAttribute(attributeName = "guestNumber")
    private String guestNumber;

    @DynamoDBAttribute(attributeName = "timeFrom")
    private String timeFrom;

    @DynamoDBAttribute(attributeName = "timeTo")
    private String timeTo;

    @DynamoDBAttribute(attributeName = "status")
    private String status;

    @DynamoDBAttribute(attributeName = "feedbackId")
    private String feedbackId;

    @DynamoDBAttribute(attributeName = "orders")
    private List<String> orders;




}
