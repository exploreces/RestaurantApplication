package com.epam.edp.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponseDto {
    private String id;
    private String address;
    private String description;
    private String totalCapacity;
    private String averageOccupancy;
    private String imageUrl;
    private String rating;
}