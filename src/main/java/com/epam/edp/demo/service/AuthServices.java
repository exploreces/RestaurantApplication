package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.request.SignInRequest;
import com.epam.edp.demo.dto.request.SignUpRequest;
import com.epam.edp.demo.dto.request.UserProfileDto;
import com.epam.edp.demo.dto.response.AuthResponse;
import com.epam.edp.demo.dto.response.SignInResponse;
import com.epam.edp.demo.dto.response.UserProfileResponse;

public interface AuthServices {

    SignInResponse signin(SignInRequest signInRequest);
    AuthResponse signup(SignUpRequest signUpRequest);

    UserProfileResponse update(String email, UserProfileDto userProfileDto);
    UserProfileResponse getUserDetails(String email);
    void updateUserPassword(String email , String currentPassword , String newPassword);
    void deleteUser(String email);
}
