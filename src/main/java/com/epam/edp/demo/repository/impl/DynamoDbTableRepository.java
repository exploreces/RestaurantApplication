package com.epam.edp.demo.repository.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.epam.edp.demo.entity.Table;
import com.epam.edp.demo.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class DynamoDbTableRepository implements TableRepository {
    public static final String ATTR_TABLES_ID = "id";
    public static final String ATTR_TABLES_LOCATION_ID = "locationId";
    public static final String ATTR_TABLES_CAPACITY = "capacity";
    public static final String ATTR_TABLES_TABLE_NUMBER = "tableNumber";
    private final AmazonDynamoDB dynamoDBClient;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String tablesTableName="tm5-restaurant-tables-table-a4v2";




    @Override
    public List<Table> getAllTables(){
        logger.info("Finding all Tables");
        ScanRequest scanRequest = new ScanRequest()
                .withTableName(tablesTableName);

            ScanResult result = dynamoDBClient.scan(scanRequest);
            List<Table> tables = new ArrayList<>();

            for (Map<String, AttributeValue> item : result.getItems()) {
                tables.add(mapToTables(item));
            }
            logger.info("Found {} TAbles", tables.size());
            return tables;
        }


    @Override
     public List<Table> getTablesByLocationId(String locationId) {
        logger.info("Finding all Tables by locationId" + locationId);


            List<Table> tables = getAllTables();
           return  tables.stream()
                    .filter(table -> table.getLocationId().equalsIgnoreCase(locationId))
                    .collect(Collectors.toList());

    }

    @Override
    public boolean isTableIdValidForLocationId(String locationId,String tableId) {
        List<Table> tableList=getTablesByLocationId(locationId);
        return tableList.stream()
                .anyMatch(t->t.getId().equals(tableId));

    }

    private Table mapToTables(Map<String, AttributeValue> item) {
    Table table = new Table();

        if (item.containsKey(ATTR_TABLES_ID)) {
            table.setId(item.get(ATTR_TABLES_ID).getS());
        }

        if (item.containsKey(ATTR_TABLES_LOCATION_ID)) {
            table.setLocationId(item.get(ATTR_TABLES_LOCATION_ID).getS());
        }

        if (item.containsKey(ATTR_TABLES_CAPACITY)) {
            table.setCapacity(item.get(ATTR_TABLES_CAPACITY).getS());
        }

        if (item.containsKey(ATTR_TABLES_TABLE_NUMBER)) {
            table.setTableNumber(item.get(ATTR_TABLES_TABLE_NUMBER).getS());
        }



        return table;
}


    }
