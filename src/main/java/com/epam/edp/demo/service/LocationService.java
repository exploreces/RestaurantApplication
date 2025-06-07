package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.request.LocationRequestDTO;
import com.epam.edp.demo.dto.response.LocationResponseDto;
import com.epam.edp.demo.dto.response.ShortLocationDTO;
import com.epam.edp.demo.dto.response.SpecialityDishDto;
import com.epam.edp.demo.entity.Location;
import com.epam.edp.demo.exceptions.ServiceException;
import com.epam.edp.demo.repository.LocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);
    private final LocationRepository locationRepository;

    @Autowired
    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public List<LocationResponseDto> getAllLocations() {
        logger.info("Getting all locations");
        try {
            List<Location> locations = locationRepository.findAll();
            return locations.stream()
                    .map(this::mapToResponseDto)
                    .toList();
        } catch (Exception e) {
            logger.error("Error getting all locations: {}", e.getMessage(), e);
            throw new ServiceException("Failed to get all locations", e);
        }
    }

    public LocationResponseDto getLocationById(String id) {
        logger.info("Getting location by id: {}", id);
        try {
            Location location = locationRepository.findById(id);
            if (location == null) {
                return null;
            }
            return mapToResponseDto(location);
        } catch (Exception e) {
            logger.error("Error getting location by id {}: {}", id, e.getMessage(), e);
            throw new ServiceException("Failed to get location by id: " + id, e);
        }
    }

    public LocationResponseDto createLocation(LocationRequestDTO createLocationDto) {
        String locationId = UUID.randomUUID().toString();
        logger.info("Creating new location with generated ID: {}", locationId);

        try {
            Location createdLocation = locationRepository.createLocation(locationId, createLocationDto);
            return mapToResponseDto(createdLocation);
        } catch (Exception e) {
            logger.error("Error creating location: {}", e.getMessage(), e);
            throw new ServiceException("Failed to create location", e);
        }
    }

    public List<SpecialityDishDto> getSpecialDishes(String locationId) {
        logger.info("Fetching special dishes for location: {}", locationId);
        try {
            return locationRepository.getSpecialDishes(locationId);
        } catch (Exception e) {
            logger.error("Error getting special dishes for location {}: {}", locationId, e.getMessage(), e);
            throw new ServiceException("Failed to get special dishes for location: " + locationId, e);
        }
    }

    public List<ShortLocationDTO> getShortLocations() {
        logger.info("Getting short location details");
        try {
            List<Location> locations = locationRepository.findAll();
            return locations.stream()
                    .map(location -> new ShortLocationDTO(location.getId(), location.getAddress()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting short locations: {}", e.getMessage(), e);
            throw new ServiceException("Failed to get short locations", e);
        }
    }

    public boolean existsById(String id) {
        try {
            return locationRepository.existsById(id);
        } catch (Exception e) {
            logger.error("Error checking if location exists with id {}: {}", id, e.getMessage(), e);
            throw new ServiceException("Failed to check if location exists", e);
        }
    }

    private LocationResponseDto mapToResponseDto(Location location) {
        return new LocationResponseDto(
                location.getId(),
                location.getAddress(),
                location.getDescription(),
                location.getTotalCapacity(),
                location.getAverageOccupancy(),
                location.getImageUrl(),
                location.getRating()
        );
    }
}