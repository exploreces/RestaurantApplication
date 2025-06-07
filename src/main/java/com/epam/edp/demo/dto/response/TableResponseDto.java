package com.epam.edp.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableResponseDto {

    private String locationId;
    private String locationAddress;
    private String tableNumber;
    private String capacity;
    private List<String> availableSlots;

//    public String getTableNumber() {
//        return tableNumber;
//    }
//
//    public void setTableNumber(String tableNumber) {
//        this.tableNumber = tableNumber;
//    }
//
//    public String getLocationId() {
//        return locationId;
//    }
//
//    public void setLocationId(String locationId) {
//        this.locationId = locationId;
//    }
//
//    public String getLocationAddress() {
//        return locationAddress;
//    }
//
//    public void setLocationAddress(String locationAddress) {
//        this.locationAddress = locationAddress;
//    }
//
//
//    public String getCapacity() {
//        return capacity;
//    }
//
//    public void setCapacity(String capacity) {
//        this.capacity = capacity;
//    }
//
//    public List<String> getAvailableSlots() {
//        return availableSlots;
//    }
//
//    public void setAvailableSlots(List<String> availableSlots) {
//        this.availableSlots = availableSlots;
//    }
}
