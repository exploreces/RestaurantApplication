package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.request.LocationRequestDTO;
import com.epam.edp.demo.dto.response.*;
import com.epam.edp.demo.exceptions.ServiceException;
import com.epam.edp.demo.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LocationController {

    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);
    private final LocationService locationService;

    @Autowired
    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/locations")
    public ResponseEntity<List<LocationResponseDto>> getLocations() {
        try {
            logger.info("Getting all locations");
            List<LocationResponseDto> locations = locationService.getAllLocations();
            return ResponseEntity.ok(locations);
        } catch (ServiceException e) {
            logger.error("Error getting locations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/locations/select-options")
    public ResponseEntity<Map<String, Map<String, List<ShortLocationDTO>>>> getShortLocations() {
        try {
            logger.info("Getting location select options");
            List<ShortLocationDTO> locations = locationService.getShortLocations();
            Map<String, List<ShortLocationDTO>> innerMap = new HashMap<>();
            innerMap.put("locations", locations);

            Map<String, Map<String, List<ShortLocationDTO>>> response = new HashMap<>();
            response.put("data", innerMap);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ServiceException e) {
            logger.error("Error getting location select options: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/locations")
    public ResponseEntity<LocationResponseDto> createLocation(@RequestBody LocationRequestDTO locationRequestDTO) {
        try {
            logger.info("Creating new location");

            if (locationRequestDTO.getAddress() == null || locationRequestDTO.getAddress().trim().isEmpty()) {
                logger.warn("Address is missing or empty");
                return ResponseEntity.badRequest().build();
            }

            if (locationRequestDTO.getDescription() == null || locationRequestDTO.getDescription().trim().isEmpty()) {
                logger.warn("Description is missing or empty");
                return ResponseEntity.badRequest().build();
            }

            LocationResponseDto createdLocation = locationService.createLocation(locationRequestDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdLocation);
        } catch (ServiceException e) {
            logger.error("Error creating location: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/locations/{id}")
    public ResponseEntity<LocationResponseWrapper> getLocationById(@PathVariable("id") String locationId) {
        try {
            if (locationId == null || locationId.trim().isEmpty()) {
                logger.warn("Location ID is missing or empty");
                return ResponseEntity.badRequest().build();
            }

            logger.info("Getting location by id: {}", locationId);
            LocationResponseDto location = locationService.getLocationById(locationId);

            if (location == null) {
                logger.info("Location not found with id: {}", locationId);
                return ResponseEntity.notFound().build();
            }

            // Wrap the location in the expected response format
            LocationResponseWrapper response = LocationResponseWrapper.from(location);
            return ResponseEntity.ok(response);
        } catch (ServiceException e) {
            logger.error("Error getting location: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/locations/{id}/special-dishes")
    public ResponseEntity<List<SpecialityDishDto>> getSpecialDishes(@PathVariable("id") String locationId) {
        try {
            if (locationId == null || locationId.trim().isEmpty()) {
                logger.warn("Location ID is missing or empty");
                return ResponseEntity.badRequest().build();
            }

            // Check if location exists
            if (!locationService.existsById(locationId)) {
                logger.warn("Attempted to get special dishes from invalid location: {}", locationId);
                return ResponseEntity.notFound().build();
            }

            logger.info("Getting specialty dishes for location id: {}", locationId);
            List<SpecialityDishDto> specialDishes = locationService.getSpecialDishes(locationId);
            return ResponseEntity.ok(specialDishes);
        } catch (ServiceException e) {
            logger.error("Error getting specialty dishes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}