package com.epam.edp.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecialityDishDto {
    private String id;
    private String name;
    private String price;
    private String imageUrl;
}