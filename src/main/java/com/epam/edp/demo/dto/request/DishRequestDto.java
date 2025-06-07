package com.epam.edp.demo.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishRequestDto {
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
    private String rating;

    @JsonProperty("isSpecialty")
    private boolean isSpecialty;

    @JsonProperty("isPopular")
    private boolean isPopular;
}