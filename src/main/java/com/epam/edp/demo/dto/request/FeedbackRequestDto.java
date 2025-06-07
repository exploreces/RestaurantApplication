package com.epam.edp.demo.dto.request;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FeedbackRequestDto {

    @JsonProperty
    private String locationId;
    @JsonProperty
    private String type;
    @JsonProperty
    private Integer rate;
    @JsonProperty
    private String comment;
    @JsonProperty
    private String userName;
    @JsonProperty
    private String userAvatarUrl;
    @JsonProperty
    private String reservationId;


    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }


    public String getLocationId() {
        return locationId;
    }

    public String getType() {
        return type;
    }

    public Integer getRate() {
        return rate;
    }

    public String getComment() {
        return comment;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setRate(Integer rate) {
        this.rate = rate;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }
}
