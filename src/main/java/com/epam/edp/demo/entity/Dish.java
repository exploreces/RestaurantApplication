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
@DynamoDBTable(tableName = "tm5-restaurant-dishes-table-a4v2")
public class Dish {

    @DynamoDBRangeKey(attributeName = "id")
    private String id;

    @DynamoDBAttribute(attributeName = "name")
    private String name;

    @DynamoDBAttribute(attributeName = "description")
    private String description;

    @DynamoDBAttribute(attributeName = "price")
    private String price;

    @DynamoDBAttribute(attributeName = "weight")
    private String weight;

    @DynamoDBAttribute(attributeName = "calories")
    private String calories;

    @DynamoDBAttribute(attributeName = "carbohydrates")
    private String carbohydrates;

    @DynamoDBAttribute(attributeName = "proteins")
    private String proteins;

    @DynamoDBAttribute(attributeName = "fats")
    private String fats;

    @DynamoDBAttribute(attributeName = "vitamins")
    private String vitamins;

    @DynamoDBAttribute(attributeName = "dishType")
    private String dishType;

    @DynamoDBAttribute(attributeName = "state")
    private String state;

    @DynamoDBAttribute(attributeName = "imageUrl")
    private String imageUrl;

    @DynamoDBHashKey(attributeName = "locationId")
    private String locationId;

    @DynamoDBAttribute(attributeName = "isSpecialty")
    private boolean isSpecialty;

    @DynamoDBAttribute(attributeName = "isPopular")
    private boolean isPopular;

    @DynamoDBAttribute(attributeName = "rating")
    private String rating;
}