package com.epam.edp.demo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationRequestDTO {
    private String address;
    private String description;
    private String totalCapacity;
    private String averageOccupancy;
    private String imageUrl;
    private String rating;
}