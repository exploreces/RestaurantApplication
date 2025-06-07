package com.epam.edp.demo.service;


import com.epam.edp.demo.entity.Table;

import java.util.List;

public interface TableService {
        List<Table> getTablesByLocationId(String locationId);
        boolean isValidTableIdForLocationId(String locationId,String tableId);
}
