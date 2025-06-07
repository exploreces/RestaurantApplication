package com.epam.edp.demo.dto.response;

import java.util.List;

public class LocationsResponse {
    private List<LocationResponseDto> locations;

    public LocationsResponse(List<LocationResponseDto> locations) {
        this.locations = locations;
    }

    public List<LocationResponseDto> getLocations() {
        return locations;
    }

    public void setLocations(List<LocationResponseDto> locations) {
        this.locations = locations;
    }
}
