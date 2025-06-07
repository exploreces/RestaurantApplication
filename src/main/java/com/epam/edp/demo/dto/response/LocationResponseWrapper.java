package com.epam.edp.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponseWrapper {
    private DataWrapper data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataWrapper {
        private LocationResponseDto location;
    }

    // Convenience constructor
    public static LocationResponseWrapper from(LocationResponseDto locationResponseDto) {
        return new LocationResponseWrapper(new DataWrapper(locationResponseDto));
    }
}