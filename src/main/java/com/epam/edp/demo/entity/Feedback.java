package com.epam.edp.demo.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(tableName = "tm5-restaurant-feedbacks-table-a4v2")
public class Feedback {

    @DynamoDBHashKey(attributeName = "locationId")
    private String locationId;

    @DynamoDBRangeKey(attributeName = "id")
    private String id;

    @DynamoDBAttribute(attributeName = "type")
    private String type;

    @DynamoDBAttribute(attributeName = "date")
    private String date;

    @DynamoDBAttribute(attributeName = "rate")
    private Integer rate;

    @DynamoDBAttribute(attributeName = "comment")
    private String comment;

    @DynamoDBAttribute(attributeName = "userName")
    private String userName;

    @DynamoDBAttribute(attributeName = "userAvatarUrl")
    private String userAvatarUrl;

    @DynamoDBAttribute(attributeName = "reservationId")
    private String reservationId;

}