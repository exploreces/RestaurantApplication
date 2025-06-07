package com.epam.edp.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    private String message;
    private UserProfileData data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserProfileData {
        private UserProfile userProfile;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserProfile {
        private String firstName;
        private String lastName;
        private String imageUrl;
    }
}
