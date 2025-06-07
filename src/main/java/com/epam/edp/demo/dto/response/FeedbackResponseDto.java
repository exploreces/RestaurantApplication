package com.epam.edp.demo.dto.response;

import org.springframework.stereotype.Component;

@Component
public class FeedbackResponseDto {
    private String id;
    private Integer rate; // Changed from String to Integer
    private String comment;
    private String userName;
    private String userAvatarUrl;
    private String date;
    private String type;
    private String locationId;
    private String reservationId;

    public FeedbackResponseDto() {
    }

    public FeedbackResponseDto(String id, Integer rate, String comment, String userName, String userAvatarUrl, String date, String type, String locationId , String reservationId) {
        this.id = id;
        this.rate = rate;
        this.comment = comment;
        this.userName = userName;
        this.userAvatarUrl = userAvatarUrl;
        this.date = date;
        this.type = type;
        this.locationId = locationId;
        this.reservationId=reservationId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getRate() { // Changed return type
        return rate;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public void setRate(Integer rate) { // Changed parameter type
        this.rate = rate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }
}
