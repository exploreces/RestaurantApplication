package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.request.DishRequestDto;
import com.epam.edp.demo.dto.response.DishResponseDto;
import com.epam.edp.demo.exceptions.ServiceException;
import com.epam.edp.demo.service.DishService;
import com.epam.edp.demo.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DishControllerTest {

    @Mock
    private DishService dishService;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private DishController dishController;

    private DishRequestDto validDishRequest;
    private DishResponseDto sampleDishResponse;
    private DishResponseDto.Dish sampleDish;
    private List<DishResponseDto.Dish> dishList;

    @BeforeEach
    void setUp() {
        // Setup valid dish request
        validDishRequest = new DishRequestDto();
        validDishRequest.setName("Test Dish");
        validDishRequest.setLocationId("loc123");
        validDishRequest.setPrice("10.99");
        validDishRequest.setDescription("Test Description");

        // Setup sample dish
        sampleDish = DishResponseDto.Dish.builder()
                .id("dish123")
                .name("Test Dish")
                .description("Test Description")
                .price("10.99")
                .locationId("loc123")
                .dishType("Main Course")
                .isPopular(false)
                .isSpecialty(false)
                .build();

        // Setup dish list
        dishList = new ArrayList<>();
        dishList.add(sampleDish);

        // Setup sample dish response
        sampleDishResponse = DishResponseDto.create(dishList);
    }

    @Nested
    @DisplayName("Create Dish Tests")
    class CreateDishTests {

        @Test
        @DisplayName("Should create dish successfully")
        void shouldCreateDishSuccessfully() {
            // Given
            when(dishService.createDish(any(DishRequestDto.class))).thenReturn(sampleDishResponse);

            // When
            ResponseEntity<DishResponseDto> response = dishController.createDish(validDishRequest);

            // Then
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(sampleDishResponse, response.getBody());
            verify(dishService).createDish(validDishRequest);
        }

        @Test
        @DisplayName("Should return bad request when dish name is null")
        void shouldReturnBadRequestWhenDishNameIsNull() {
            // Given
            validDishRequest.setName(null);

            // When
            ResponseEntity<DishResponseDto> response = dishController.createDish(validDishRequest);

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNull(response.getBody());
            verify(dishService, never()).createDish(any());
        }

        @Test
        @DisplayName("Should return bad request when dish name is empty")
        void shouldReturnBadRequestWhenDishNameIsEmpty() {
            // Given
            validDishRequest.setName("  ");

            // When
            ResponseEntity<DishResponseDto> response = dishController.createDish(validDishRequest);

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNull(response.getBody());
            verify(dishService, never()).createDish(any());
        }

        @Test
        @DisplayName("Should return bad request when location ID is null")
        void shouldReturnBadRequestWhenLocationIdIsNull() {
            // Given
            validDishRequest.setLocationId(null);

            // When
            ResponseEntity<DishResponseDto> response = dishController.createDish(validDishRequest);

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNull(response.getBody());
            verify(dishService, never()).createDish(any());
        }

        @Test
        @DisplayName("Should return bad request when location ID is empty")
        void shouldReturnBadRequestWhenLocationIdIsEmpty() {
            // Given
            validDishRequest.setLocationId("  ");

            // When
            ResponseEntity<DishResponseDto> response = dishController.createDish(validDishRequest);

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNull(response.getBody());
            verify(dishService, never()).createDish(any());
        }

        @Test
        @DisplayName("Should return bad request when service throws IllegalArgumentException")
        void shouldReturnBadRequestWhenServiceThrowsIllegalArgumentException() {
            // Given
            when(dishService.createDish(any(DishRequestDto.class)))
                    .thenThrow(new IllegalArgumentException("Invalid input"));

            // When
            ResponseEntity<DishResponseDto> response = dishController.createDish(validDishRequest);

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNull(response.getBody());
        }

        @Test
        @DisplayName("Should return internal server error when service throws ServiceException")
        void shouldReturnInternalServerErrorWhenServiceThrowsServiceException() {
            // Given
            when(dishService.createDish(any(DishRequestDto.class)))
                    .thenThrow(new ServiceException("Service error"));

            // When
            ResponseEntity<DishResponseDto> response = dishController.createDish(validDishRequest);

            // Then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNull(response.getBody());
        }

        @Test
        @DisplayName("Should return internal server error when service throws unexpected exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsUnexpectedException() {
            // Given
            when(dishService.createDish(any(DishRequestDto.class)))
                    .thenThrow(new RuntimeException("Unexpected error"));

            // When
            ResponseEntity<DishResponseDto> response = dishController.createDish(validDishRequest);

            // Then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("Get Dish By Location ID Tests")
    class GetDishByLocationIdTests {

        @Test
        @DisplayName("Should get dishes by location ID successfully")
        void shouldGetDishesByLocationIdSuccessfully() {
            // Given
            String locationId = "loc123";
            when(locationService.existsById(locationId)).thenReturn(true);
            when(dishService.getDishesByLocationId(locationId)).thenReturn(sampleDishResponse);

            // When
            ResponseEntity<DishResponseDto> response = dishController.getDishByLocationId(locationId);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(sampleDishResponse, response.getBody());
            verify(locationService).existsById(locationId);
            verify(dishService).getDishesByLocationId(locationId);
        }

        @Test
        @DisplayName("Should return bad request when location ID is null")
        void shouldReturnBadRequestWhenLocationIdIsNull() {
            // When
            ResponseEntity<DishResponseDto> response = dishController.getDishByLocationId(null);

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNull(response.getBody());
            verify(locationService, never()).existsById(anyString());
            verify(dishService, never()).getDishesByLocationId(anyString());
        }

        @Test
        @DisplayName("Should return bad request when location ID is empty")
        void shouldReturnBadRequestWhenLocationIdIsEmpty() {
            // When
            ResponseEntity<DishResponseDto> response = dishController.getDishByLocationId("  ");

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNull(response.getBody());
            verify(locationService, never()).existsById(anyString());
            verify(dishService, never()).getDishesByLocationId(anyString());
        }

        @Test
        @DisplayName("Should return not found when location doesn't exist")
        void shouldReturnNotFoundWhenLocationDoesNotExist() {
            // Given
            String locationId = "nonexistent";
            when(locationService.existsById(locationId)).thenReturn(false);

            // When
            ResponseEntity<DishResponseDto> response = dishController.getDishByLocationId(locationId);

            // Then
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNull(response.getBody());
            verify(locationService).existsById(locationId);
            verify(dishService, never()).getDishesByLocationId(anyString());
        }

        @Test
        @DisplayName("Should return internal server error when service throws ServiceException")
        void shouldReturnInternalServerErrorWhenServiceThrowsServiceException() {
            // Given
            String locationId = "loc123";
            when(locationService.existsById(locationId)).thenReturn(true);
            when(dishService.getDishesByLocationId(locationId)).thenThrow(new ServiceException("Service error"));

            // When
            ResponseEntity<DishResponseDto> response = dishController.getDishByLocationId(locationId);

            // Then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("Get Dish By ID Tests")
    class GetDishByIdTests {

        @Test
        @DisplayName("Should get dish by ID successfully")
        void shouldGetDishByIdSuccessfully() {
            // Given
            String dishId = "dish123";
            when(dishService.getDishById(dishId)).thenReturn(sampleDishResponse);

            // When
            ResponseEntity<DishResponseDto> response = dishController.getDishById(dishId);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(sampleDishResponse, response.getBody());
            verify(dishService).getDishById(dishId);
        }

        @Test
        @DisplayName("Should return bad request when dish ID is null")
        void shouldReturnBadRequestWhenDishIdIsNull() {
            // When
            ResponseEntity<DishResponseDto> response = dishController.getDishById(null);

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNull(response.getBody());
            verify(dishService, never()).getDishById(anyString());
        }

        @Test
        @DisplayName("Should return bad request when dish ID is empty")
        void shouldReturnBadRequestWhenDishIdIsEmpty() {
            // When
            ResponseEntity<DishResponseDto> response = dishController.getDishById("  ");

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNull(response.getBody());
            verify(dishService, never()).getDishById(anyString());
        }

        @Test
        @DisplayName("Should return not found when dish doesn't exist")
        void shouldReturnNotFoundWhenDishDoesNotExist() {
            // Given
            String dishId = "nonexistent";
            when(dishService.getDishById(dishId)).thenReturn(null);

            // When
            ResponseEntity<DishResponseDto> response = dishController.getDishById(dishId);

            // Then
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNull(response.getBody());
            verify(dishService).getDishById(dishId);
        }

        @Test
        @DisplayName("Should return internal server error when service throws ServiceException")
        void shouldReturnInternalServerErrorWhenServiceThrowsServiceException() {
            // Given
            String dishId = "dish123";
            when(dishService.getDishById(dishId)).thenThrow(new ServiceException("Service error"));

            // When
            ResponseEntity<DishResponseDto> response = dishController.getDishById(dishId);

            // Then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("Get Specialty Dishes Tests")
    class GetSpecialtyDishesTests {

        @Test
        @DisplayName("Should get specialty dishes successfully")
        void shouldGetSpecialtyDishesSuccessfully() {
            // Given
            String locationId = "loc123";
            when(locationService.existsById(locationId)).thenReturn(true);
            when(dishService.getSpecialtyDishes(locationId)).thenReturn(sampleDishResponse);

            // When
            ResponseEntity<DishResponseDto> response = dishController.getSpecialtyDishes(locationId);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(sampleDishResponse, response.getBody());
            verify(locationService).existsById(locationId);
            verify(dishService).getSpecialtyDishes(locationId);
        }

        @Test
        @DisplayName("Should return bad request when location ID is null")
        void shouldReturnBadRequestWhenLocationIdIsNull() {
            // When
            ResponseEntity<DishResponseDto> response = dishController.getSpecialtyDishes(null);

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNull(response.getBody());
            verify(locationService, never()).existsById(anyString());
            verify(dishService, never()).getSpecialtyDishes(anyString());
        }

        @Test
        @DisplayName("Should return bad request when location ID is empty")
        void shouldReturnBadRequestWhenLocationIdIsEmpty() {
            // When
            ResponseEntity<DishResponseDto> response = dishController.getSpecialtyDishes("  ");

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNull(response.getBody());
            verify(locationService, never()).existsById(anyString());
            verify(dishService, never()).getSpecialtyDishes(anyString());
        }

        @Test
        @DisplayName("Should return not found when location doesn't exist")
        void shouldReturnNotFoundWhenLocationDoesNotExist() {
            // Given
            String locationId = "nonexistent";
            when(locationService.existsById(locationId)).thenReturn(false);

            // When
            ResponseEntity<DishResponseDto> response = dishController.getSpecialtyDishes(locationId);

            // Then
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNull(response.getBody());
            verify(locationService).existsById(locationId);
            verify(dishService, never()).getSpecialtyDishes(anyString());
        }

        @Test
        @DisplayName("Should return internal server error when service throws ServiceException")
        void shouldReturnInternalServerErrorWhenServiceThrowsServiceException() {
            // Given
            String locationId = "loc123";
            when(locationService.existsById(locationId)).thenReturn(true);
            when(dishService.getSpecialtyDishes(locationId)).thenThrow(new ServiceException("Service error"));

            // When
            ResponseEntity<DishResponseDto> response = dishController.getSpecialtyDishes(locationId);

            // Then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("Get Popular Dishes Tests")
    class GetPopularDishesTests {

        @Test
        @DisplayName("Should get popular dishes successfully")
        void shouldGetPopularDishesSuccessfully() {
            // Given
            when(dishService.getPopularDishes()).thenReturn(sampleDishResponse);

            // When
            ResponseEntity<DishResponseDto> response = dishController.getPopularDishes();

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(sampleDishResponse, response.getBody());
            verify(dishService).getPopularDishes();
        }

        @Test
        @DisplayName("Should return internal server error when service throws ServiceException")
        void shouldReturnInternalServerErrorWhenServiceThrowsServiceException() {
            // Given
            when(dishService.getPopularDishes()).thenThrow(new ServiceException("Service error"));

            // When
            ResponseEntity<DishResponseDto> response = dishController.getPopularDishes();

            // Then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNull(response.getBody());
        }

        @Test
        @DisplayName("Should return internal server error when service throws unexpected exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsUnexpectedException() {
            // Given
            when(dishService.getPopularDishes()).thenThrow(new RuntimeException("Unexpected error"));

            // When
            ResponseEntity<DishResponseDto> response = dishController.getPopularDishes();

            // Then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("Get All Dishes Tests")
    class GetAllDishesTests {

        @Test
        @DisplayName("Should get all dishes successfully")
        void shouldGetAllDishesSuccessfully() {
            // Given
            when(dishService.getAllDishes()).thenReturn(sampleDishResponse);

            // When
            ResponseEntity<DishResponseDto> response = dishController.getAllDishes(null, "name", "asc");

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(sampleDishResponse, response.getBody());
            verify(dishService).getAllDishes();
            verify(dishService, never()).getFilteredAndSortedDishes(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should get filtered and sorted dishes when parameters are provided")
        void shouldGetFilteredAndSortedDishesWhenParametersAreProvided() {
            // Given
            String dishType = "Main Course";
            String sortBy = "price";
            String sortDirection = "desc";
            when(dishService.getFilteredAndSortedDishes(dishType, sortBy, sortDirection)).thenReturn(sampleDishResponse);

            // When
            ResponseEntity<DishResponseDto> response = dishController.getAllDishes(dishType, sortBy, sortDirection);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(sampleDishResponse, response.getBody());
            verify(dishService).getFilteredAndSortedDishes(dishType, sortBy, sortDirection);
            verify(dishService, never()).getAllDishes();
        }

        @Test
        @DisplayName("Should get filtered dishes when only dish type is provided")
        void shouldGetFilteredDishesWhenOnlyDishTypeIsProvided() {
            // Given
            String dishType = "Main Course";
            when(dishService.getFilteredAndSortedDishes(dishType, "name", "asc")).thenReturn(sampleDishResponse);

            // When
            ResponseEntity<DishResponseDto> response = dishController.getAllDishes(dishType, "name", "asc");

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(sampleDishResponse, response.getBody());
            verify(dishService).getFilteredAndSortedDishes(dishType, "name", "asc");
            verify(dishService, never()).getAllDishes();
        }

        @Test
        @DisplayName("Should return internal server error when service throws ServiceException")
        void shouldReturnInternalServerErrorWhenServiceThrowsServiceException() {
            // Given
            when(dishService.getAllDishes()).thenThrow(new ServiceException("Service error"));

            // When
            ResponseEntity<DishResponseDto> response = dishController.getAllDishes(null, "name", "asc");

            // Then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("Delete Dish Tests")
    class DeleteDishTests {

        @Test
        @DisplayName("Should delete dish successfully")
        void shouldDeleteDishSuccessfully() {
            // Given
            String dishId = "dish123";
            when(dishService.getDishById(dishId)).thenReturn(sampleDishResponse);
            doNothing().when(dishService).deleteDish(dishId);

            // When
            ResponseEntity<Void> response = dishController.deleteDish(dishId);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(dishService).getDishById(dishId);
            verify(dishService).deleteDish(dishId);
        }

        @Test
        @DisplayName("Should return bad request when dish ID is null")
        void shouldReturnBadRequestWhenDishIdIsNull() {
            // When
            ResponseEntity<Void> response = dishController.deleteDish(null);

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verify(dishService, never()).getDishById(anyString());
            verify(dishService, never()).deleteDish(anyString());
        }

        @Test
        @DisplayName("Should return bad request when dish ID is empty")
        void shouldReturnBadRequestWhenDishIdIsEmpty() {
            // When
            ResponseEntity<Void> response = dishController.deleteDish("  ");

            // Then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verify(dishService, never()).getDishById(anyString());
            verify(dishService, never()).deleteDish(anyString());
        }

        @Test
        @DisplayName("Should return not found when dish doesn't exist")
        void shouldReturnNotFoundWhenDishDoesNotExist() {
            // Given
            String dishId = "nonexistent";
            when(dishService.getDishById(dishId)).thenReturn(null);

            // When
            ResponseEntity<Void> response = dishController.deleteDish(dishId);

            // Then
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            verify(dishService).getDishById(dishId);
            verify(dishService, never()).deleteDish(anyString());
        }

        @Test
        @DisplayName("Should return internal server error when service throws ServiceException")
        void shouldReturnInternalServerErrorWhenServiceThrowsServiceException() {
            // Given
            String dishId = "dish123";
            when(dishService.getDishById(dishId)).thenReturn(sampleDishResponse);
            doThrow(new ServiceException("Service error")).when(dishService).deleteDish(dishId);

            // When
            ResponseEntity<Void> response = dishController.deleteDish(dishId);

            // Then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return internal server error when service throws unexpected exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsUnexpectedException() {
            // Given
            String dishId = "dish123";
            when(dishService.getDishById(dishId)).thenReturn(sampleDishResponse);
            doThrow(new RuntimeException("Unexpected error")).when(dishService).deleteDish(dishId);

            // When
            ResponseEntity<Void> response = dishController.deleteDish(dishId);

            // Then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }
}