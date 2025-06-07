package com.epam.edp.demo.repository;


import com.epam.edp.demo.entity.Table;

import java.util.List;

public interface TableRepository {
    List<Table> getTablesByLocationId(String locationId);
    List<Table> getAllTables();
    boolean isTableIdValidForLocationId(String locationId,String tableId);
}
