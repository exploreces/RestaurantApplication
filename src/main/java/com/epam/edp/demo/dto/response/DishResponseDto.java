package com.epam.edp.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DishResponseDto {
    private DishData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DishData {
        // Only include the dishes field - remove the dish field completely
        private List<Dish> dishes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Dish {
        private String id;
        private String name;
        private String description;
        private String price;
        private String weight;
        private String calories;
        private String carbohydrates;
        private String proteins;
        private String fats;
        private String vitamins;
        private String dishType;
        private String state;
        private String imageUrl;
        private String locationId;
        private Boolean isSpecialty;
        private Boolean isPopular;
        private String rating;
        private Boolean popular;
        private Boolean specialty;
    }

    // Constructor for creating response with multiple dishes
    public static DishResponseDto create(List<Dish> dishes) {
        DishData data = new DishData();
        data.setDishes(dishes);

        DishResponseDto response = new DishResponseDto();
        response.setData(data);

        return response;
    }
}