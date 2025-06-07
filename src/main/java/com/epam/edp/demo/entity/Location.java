package com.epam.edp.demo.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(tableName = "tm5-restaurant-locations-table-a4v2")
public class Location {

    @DynamoDBHashKey(attributeName = "id")
    private String id;

    @DynamoDBAttribute(attributeName = "address")
    private String address;

    @DynamoDBAttribute(attributeName = "description")
    private String description;

    @DynamoDBAttribute(attributeName = "totalCapacity")
    private String totalCapacity;

    @DynamoDBAttribute(attributeName = "averageOccupancy")
    private String averageOccupancy;

    @DynamoDBAttribute(attributeName = "imageUrl")
    private String imageUrl;

    @DynamoDBAttribute(attributeName = "rating")
    private String rating;
}