package com.epam.edp.demo.dto.response;

public class SignInResponse {
    private String message = "Sign in successful";
    private Data data;

    public SignInResponse(String accessToken, String username, String role) {
        this.data = new Data(accessToken, username, role);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        private String accessToken;
        private String username;
        private String role;

        public Data(String accessToken, String username, String role) {
            this.accessToken = accessToken;
            this.username = username;
            this.role = role;
        }
        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}