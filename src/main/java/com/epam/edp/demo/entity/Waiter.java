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
@DynamoDBTable(tableName = "tm5-restaurant-waiters-table-a4v2")
public class Waiter {
    @DynamoDBHashKey(attributeName = "email")
    private String email;
    @DynamoDBAttribute(attributeName = "locationId")
    private String locationId;
}