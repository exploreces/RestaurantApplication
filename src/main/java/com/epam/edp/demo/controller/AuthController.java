package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.request.PasswordUpdateRequest;
import com.epam.edp.demo.dto.request.SignInRequest;
import com.epam.edp.demo.dto.request.SignUpRequest;
import com.epam.edp.demo.dto.request.UserProfileDto;
import com.epam.edp.demo.dto.response.AuthResponse;
import com.epam.edp.demo.dto.response.SignInResponse;
import com.epam.edp.demo.dto.response.UserProfileResponse;
import com.epam.edp.demo.service.impl.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")

public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/sign-up")
    public ResponseEntity<AuthResponse> signUp(@RequestBody SignUpRequest signupRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(signupRequest));
    }

    @PostMapping("/auth/sign-in")
    public ResponseEntity<?> signIn(@RequestBody SignInRequest signInRequest) {
        try {
            SignInResponse response = authService.signin(signInRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Something went wrong"));
        }
    }


    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/users/profile")
    public ResponseEntity<UserProfileResponse> updateUserProfile(@RequestHeader("Authorization") String token,
                                                                 @RequestBody UserProfileDto userProfileDto) {
        String email = authService.extractUserEmailFromToken(token.substring(7));
        return ResponseEntity.ok(authService.update(email, userProfileDto));
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/users/profile")
    public ResponseEntity<UserProfileResponse> getUserDetails(@RequestHeader("Authorization") String token) {
        String email = authService.extractUserEmailFromToken(token.substring(7));
        return ResponseEntity.ok(authService.getUserDetails(email));
    }

    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/users/profile")
    public ResponseEntity<Void> deleteUser(@RequestHeader("Authorization") String token) {
        String email = authService.extractUserEmailFromToken(token.substring(7));
        authService.deleteUser(email);
        return ResponseEntity.noContent().build();
    }

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/users/updatePassword")
    public ResponseEntity<Map<String, String>> updateUserPassword(@RequestHeader("Authorization") String token,
                                                                  @RequestBody PasswordUpdateRequest passwordUpdateRequest) {
        String email = authService.extractUserEmailFromToken(token.substring(7));
        authService.updateUserPassword(email,
                passwordUpdateRequest.getCurrentPassword(),
                passwordUpdateRequest.getNewPassword());

        return ResponseEntity.ok(Collections.singletonMap("message", "Password updated successfully"));
    }


    @GetMapping("/users")
    public ResponseEntity<List<String>> getEmails(){
        List<String> users = authService.getAllUsers();
        return ResponseEntity.ok(users);
    }


}
