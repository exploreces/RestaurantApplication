package com.epam.edp.demo.service.impl;

import com.epam.edp.demo.entity.Table;
import com.epam.edp.demo.repository.TableRepository;
import com.epam.edp.demo.service.TableService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class TableServiceImpl implements TableService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TableRepository tableRepository;


    @Override
    public List<Table> getTablesByLocationId(String locationId){
        logger.info("Finding all Tables by locationId"+locationId);
        return tableRepository.getTablesByLocationId(locationId);


    }

    @Override
    public boolean isValidTableIdForLocationId(String locationId,String tableId) {
        logger.info("Checking the tableId "+tableId+" is valid for locationId "+locationId+" or not ");
        return tableRepository.isTableIdValidForLocationId(locationId,tableId);
    }
}
