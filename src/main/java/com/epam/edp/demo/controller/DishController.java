package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.request.DishRequestDto;
import com.epam.edp.demo.dto.response.DishResponseDto;
import com.epam.edp.demo.exceptions.ServiceException;
import com.epam.edp.demo.service.DishService;
import com.epam.edp.demo.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DishController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DishService dishService;
    private final LocationService locationService;

    @Autowired
    public DishController(DishService dishService, LocationService locationService) {
        this.dishService = dishService;
        this.locationService = locationService;
    }

    @PostMapping("/dishes")
    public ResponseEntity<DishResponseDto> createDish(@RequestBody DishRequestDto dishRequestDto) {
        try {
            logger.info("Creating new dish");

            if (dishRequestDto.getName() == null || dishRequestDto.getName().trim().isEmpty()) {
                logger.warn("Dish name is required");
                return ResponseEntity.badRequest().build();
            }

            if (dishRequestDto.getLocationId() == null || dishRequestDto.getLocationId().trim().isEmpty()) {
                logger.warn("Location ID is required");
                return ResponseEntity.badRequest().build();
            }

            DishResponseDto createdDish = dishService.createDish(dishRequestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDish);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid input for dish creation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (ServiceException e) {
            logger.error("Service error creating dish: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Unexpected error creating dish: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/locations/{id}/dishes")
    public ResponseEntity<DishResponseDto> getDishByLocationId(@PathVariable("id") String locationId) {
        try {
            if (locationId == null || locationId.trim().isEmpty()) {
                logger.warn("Location ID is missing or empty");
                return ResponseEntity.badRequest().build();
            }

            // Check if location exists
            if (!locationService.existsById(locationId)) {
                logger.warn("Attempted to get dish from invalid location: {}", locationId);
                return ResponseEntity.notFound().build();
            }

            logger.info("Getting dishes by location id: {}", locationId);

            DishResponseDto dishes = dishService.getDishesByLocationId(locationId);
            return ResponseEntity.ok(dishes);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid location ID: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (ServiceException e) {
            logger.error("Service error getting dishes by location: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Unexpected error getting dishes by location: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/dishes/{id}")
    public ResponseEntity<DishResponseDto> getDishById(@PathVariable("id") String dishId) {
        try {
            if (dishId == null || dishId.trim().isEmpty()) {
                logger.warn("Dish ID is missing or empty");
                return ResponseEntity.badRequest().build();
            }

            logger.info("Getting dish by id: {}", dishId);

            DishResponseDto dish = dishService.getDishById(dishId);
            if (dish == null) {
                logger.info("Dish not found with id: {}", dishId);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(dish);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid dish ID: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (ServiceException e) {
            logger.error("Service error getting dish by ID: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Unexpected error getting dish by ID: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/locations/{id}/specialty-dishes")
    public ResponseEntity<DishResponseDto> getSpecialtyDishes(@PathVariable("id") String locationId) {
        try {
            if (locationId == null || locationId.trim().isEmpty()) {
                logger.warn("Location ID is missing or empty");
                return ResponseEntity.badRequest().build();
            }

            // Check if location exists
            if (!locationService.existsById(locationId)) {
                logger.warn("Attempted to get dish from invalid location: {}", locationId);
                return ResponseEntity.notFound().build();
            }

            logger.debug("Getting specialty dishes for location id: {}", locationId);

            DishResponseDto specialtyDishes = dishService.getSpecialtyDishes(locationId);
            return ResponseEntity.ok(specialtyDishes);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid location ID for specialty dishes: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (ServiceException e) {
            logger.error("Service error getting specialty dishes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Unexpected error getting specialty dishes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/dishes/popular")
    public ResponseEntity<DishResponseDto> getPopularDishes() {
        try {
            logger.info("Getting popular dishes");

            DishResponseDto popularDishes = dishService.getPopularDishes();
            return ResponseEntity.ok(popularDishes);
        } catch (ServiceException e) {
            logger.error("Service error getting popular dishes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Unexpected error getting popular dishes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/dishes")
    public ResponseEntity<DishResponseDto> getAllDishes(
            @RequestParam(required = false) String dishType,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDirection) {
        try {
            DishResponseDto dishes;

            if (dishType != null || !"name".equals(sortBy) || !"asc".equals(sortDirection)) {
                dishes = dishService.getFilteredAndSortedDishes(dishType, sortBy, sortDirection);
            } else {
                dishes = dishService.getAllDishes();
            }

            return ResponseEntity.ok(dishes);
        } catch (ServiceException e) {
            logger.error("Service error getting dishes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Unexpected error getting dishes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/dishes/{id}")
    public ResponseEntity<Void> deleteDish(@PathVariable("id") String dishId) {
        try {
            if (dishId == null || dishId.trim().isEmpty()) {
                logger.warn("Dish ID is missing or empty");
                return ResponseEntity.badRequest().build();
            }

            logger.info("Deleting dish with id: {}", dishId);

            // Check if dish exists before deletion
            DishResponseDto dish = dishService.getDishById(dishId);
            if (dish == null) {
                logger.info("Dish not found with id: {}", dishId);
                return ResponseEntity.notFound().build();
            }

            dishService.deleteDish(dishId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid dish ID for deletion: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (ServiceException e) {
            logger.error("Service error deleting dish: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Unexpected error deleting dish: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}