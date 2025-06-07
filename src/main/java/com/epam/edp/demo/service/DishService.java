package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.request.DishRequestDto;
import com.epam.edp.demo.dto.response.DishResponseDto;
import com.epam.edp.demo.entity.Dish;
import com.epam.edp.demo.exceptions.RepositoryException;
import com.epam.edp.demo.exceptions.ServiceException;
import com.epam.edp.demo.repository.DishRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DishService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DishRepository dishRepository;

    public DishService(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    public DishResponseDto createDish(DishRequestDto dishRequestDto) {
        try {
            logger.info("Creating new dish: {}", dishRequestDto.getName());

            if (dishRequestDto.getName() == null || dishRequestDto.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Dish name is required");
            }

            if (dishRequestDto.getLocationId() == null || dishRequestDto.getLocationId().trim().isEmpty()) {
                throw new IllegalArgumentException("Location ID is required");
            }

            Dish dish = convertToEntity(dishRequestDto);
            Dish savedDish = dishRepository.save(dish);

            return convertToSingleDishResponseDto(savedDish);
        } catch (RepositoryException e) {
            logger.error("Repository error while creating dish: {}", e.getMessage(), e);
            throw new ServiceException("Error saving dish to database", e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid input for dish creation: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating dish: {}", e.getMessage(), e);
            throw new ServiceException("Unexpected error creating dish", e);
        }
    }

    public DishResponseDto getPopularDishes() {
        try {
            logger.info("Getting popular dishes");

            List<Dish> popularDishes = dishRepository.findPopularDishes();
            return convertToMultiDishResponseDto(popularDishes);
        } catch (RepositoryException e) {
            logger.error("Repository error while getting popular dishes: {}", e.getMessage(), e);
            throw new ServiceException("Error retrieving popular dishes from database", e);
        } catch (Exception e) {
            logger.error("Unexpected error getting popular dishes: {}", e.getMessage(), e);
            throw new ServiceException("Unexpected error retrieving popular dishes", e);
        }
    }

    public DishResponseDto getDishesByLocationId(String locationId) {
        try {
            if (locationId == null || locationId.trim().isEmpty()) {
                throw new IllegalArgumentException("Location ID is required");
            }

            logger.info("Getting all dishes for locationId: {}", locationId);

            List<Dish> dishes = dishRepository.findByLocationId(locationId);
            return convertToMultiDishResponseDto(dishes);
        } catch (RepositoryException e) {
            logger.error("Repository error while getting dishes by location: {}", e.getMessage(), e);
            throw new ServiceException("Error retrieving dishes by location from database", e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid location ID: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error getting dishes by location: {}", e.getMessage(), e);
            throw new ServiceException("Unexpected error retrieving dishes by location", e);
        }
    }

    public DishResponseDto getDishById(String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Dish ID is required");
            }

            logger.info("Getting dish by id: {}", id);

            return dishRepository.findById(id)
                    .map(this::convertToSingleDishResponseDto)
                    .orElse(null);
        } catch (RepositoryException e) {
            logger.error("Repository error while getting dish by ID: {}", e.getMessage(), e);
            throw new ServiceException("Error retrieving dish from database", e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid dish ID: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error getting dish by ID: {}", e.getMessage(), e);
            throw new ServiceException("Unexpected error retrieving dish", e);
        }
    }

    public DishResponseDto getSpecialtyDishes(String locationId) {
        try {
            if (locationId == null || locationId.trim().isEmpty()) {
                throw new IllegalArgumentException("Location ID is required");
            }

            logger.info("Fetching specialty dishes for location: {}", locationId);

            List<Dish> specialtyDishes = dishRepository.findSpecialtyDishesByLocationId(locationId);
            return convertToMultiDishResponseDto(specialtyDishes);
        } catch (RepositoryException e) {
            logger.error("Repository error while getting specialty dishes: {}", e.getMessage(), e);
            throw new ServiceException("Error retrieving specialty dishes from database", e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid location ID for specialty dishes: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error getting specialty dishes: {}", e.getMessage(), e);
            throw new ServiceException("Unexpected error retrieving specialty dishes", e);
        }
    }

    public DishResponseDto getFilteredAndSortedDishes(String dishType, String sortBy, String sortDirection) {
        try {
            logger.info("Getting filtered and sorted dishes - type: {}, sortBy: {}, direction: {}",
                    dishType, sortBy, sortDirection);

            List<Dish> dishes = dishRepository.findAll();

            // Apply type filter if provided
            if (dishType != null && !dishType.isEmpty()) {
                dishes = dishes.stream()
                        .filter(dish -> dishType.equalsIgnoreCase(dish.getDishType()))
                        .collect(Collectors.toList());
            }

            // Apply sorting
            dishes = sortDishes(dishes, sortBy, sortDirection);

            return convertToMultiDishResponseDto(dishes);
        } catch (RepositoryException e) {
            logger.error("Repository error while getting filtered dishes: {}", e.getMessage(), e);
            throw new ServiceException("Error retrieving dishes from database", e);
        } catch (Exception e) {
            logger.error("Unexpected error getting filtered dishes: {}", e.getMessage(), e);
            throw new ServiceException("Unexpected error retrieving filtered dishes", e);
        }
    }

    public DishResponseDto getAllDishes() {
        try {
            logger.info("Getting all dishes");

            List<Dish> dishes = dishRepository.findAll();
            return convertToMultiDishResponseDto(dishes);
        } catch (RepositoryException e) {
            logger.error("Repository error while getting all dishes: {}", e.getMessage(), e);
            throw new ServiceException("Error retrieving all dishes from database", e);
        } catch (Exception e) {
            logger.error("Unexpected error getting all dishes: {}", e.getMessage(), e);
            throw new ServiceException("Unexpected error retrieving all dishes", e);
        }
    }

    public void deleteDish(String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Dish ID is required");
            }

            logger.info("Deleting dish with id: {}", id);
            dishRepository.deleteDish(id);
        } catch (RepositoryException e) {
            logger.error("Repository error while deleting dish: {}", e.getMessage(), e);
            throw new ServiceException("Error deleting dish from database", e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid dish ID for deletion: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error deleting dish: {}", e.getMessage(), e);
            throw new ServiceException("Unexpected error deleting dish", e);
        }
    }

    private List<Dish> sortDishes(List<Dish> dishes, String sortBy, String sortDirection) {
        Comparator<Dish> comparator;

        switch (sortBy != null ? sortBy.toLowerCase() : "name") {
            case "price":
                comparator = Comparator.comparing(dish -> {
                    try {
                        return Double.parseDouble(dish.getPrice().replaceAll("[^\\d.]", ""));
                    } catch (NumberFormatException | NullPointerException e) {
                        return 0.0;
                    }
                });
                break;
            case "rating":
                comparator = Comparator.comparing(dish -> {
                    try {
                        return dish.getRating() != null ? dish.getRating() : "";
                    } catch (Exception e) {
                        return "";
                    }
                }, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "name":
            default:
                comparator = Comparator.comparing(dish -> {
                    try {
                        return dish.getName() != null ? dish.getName() : "";
                    } catch (Exception e) {
                        return "";
                    }
                }, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
        }

        if ("desc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }

        return dishes.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private Dish convertToEntity(DishRequestDto dto) {
        Dish dish = new Dish();
        dish.setName(dto.getName());
        dish.setDescription(dto.getDescription());
        dish.setPrice(dto.getPrice());
        dish.setWeight(dto.getWeight());
        dish.setCalories(dto.getCalories());
        dish.setCarbohydrates(dto.getCarbohydrates());
        dish.setProteins(dto.getProteins());
        dish.setFats(dto.getFats());
        dish.setVitamins(dto.getVitamins());
        dish.setDishType(dto.getDishType());
        dish.setState(dto.getState());
        dish.setImageUrl(dto.getImageUrl());
        dish.setLocationId(dto.getLocationId());
        dish.setSpecialty(dto.isSpecialty());
        dish.setPopular(dto.isPopular());
        dish.setRating(dto.getRating());
        return dish;
    }

    private DishResponseDto.Dish convertToDishDto(Dish dish) {
        return DishResponseDto.Dish.builder()
                .id(dish.getId())
                .name(dish.getName())
                .description(dish.getDescription())
                .price(dish.getPrice())
                .weight(dish.getWeight())
                .calories(dish.getCalories())
                .carbohydrates(dish.getCarbohydrates())
                .proteins(dish.getProteins())
                .fats(dish.getFats())
                .vitamins(dish.getVitamins())
                .dishType(dish.getDishType())
                .state(dish.getState())
                .imageUrl(dish.getImageUrl())
                .locationId(dish.getLocationId())
                .isSpecialty(dish.isSpecialty())
                .isPopular(dish.isPopular())
                .popular(dish.isPopular())
                .specialty(dish.isSpecialty())
                .rating(dish.getRating())
                .build();
    }

    private DishResponseDto convertToSingleDishResponseDto(Dish dish) {
        DishResponseDto.Dish dishDto = convertToDishDto(dish);

        List<DishResponseDto.Dish> dishes = new ArrayList<>();
        dishes.add(dishDto);

        DishResponseDto.DishData dishData = new DishResponseDto.DishData();
        dishData.setDishes(dishes);

        DishResponseDto response = new DishResponseDto();
        response.setData(dishData);

        return response;
    }

    private DishResponseDto convertToMultiDishResponseDto(List<Dish> dishes) {
        List<DishResponseDto.Dish> dishDtos = dishes.stream()
                .map(this::convertToDishDto)
                .collect(Collectors.toList());

        DishResponseDto.DishData dishData = new DishResponseDto.DishData();
        dishData.setDishes(dishDtos);

        DishResponseDto response = new DishResponseDto();
        response.setData(dishData);

        return response;
    }
}