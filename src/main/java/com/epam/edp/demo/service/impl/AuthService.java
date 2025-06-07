package com.epam.edp.demo.service.impl;

import com.epam.edp.demo.dto.request.SignInRequest;
import com.epam.edp.demo.dto.request.SignUpRequest;
import com.epam.edp.demo.dto.request.UserProfileDto;
import com.epam.edp.demo.dto.response.AuthResponse;
import com.epam.edp.demo.dto.response.SignInResponse;
import com.epam.edp.demo.dto.response.UserProfileResponse;
import com.epam.edp.demo.entity.Roles;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.exceptions.ResourceNotFoundException;
import com.epam.edp.demo.exceptions.ServiceException;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.repository.WaiterRepository;
import com.epam.edp.demo.service.AuthServices;
import com.epam.edp.demo.utility.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService implements AuthServices {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final WaiterRepository waiterRepository;

    // Email validation regex
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       ObjectMapper objectMapper,
                       WaiterRepository waiterRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.waiterRepository = waiterRepository;
    }

    public AuthResponse signup(SignUpRequest authRequest) {
        // Validate required fields
        validateSignUpRequest(authRequest);

        User user = objectMapper.convertValue(authRequest, User.class);

        if(userRepository.existsByEmail(authRequest.getEmail())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email address already exists.");
        }

        // Check if the user is a waiter
        boolean isWaiter = waiterRepository.isWaiter(authRequest.getEmail());
        if (isWaiter) {
            user.setRoles(Roles.WAITER);
        } else {
            user.setRoles(Roles.CUSTOMER);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return new AuthResponse("User registered successfully");
    }

    private void validateSignUpRequest(SignUpRequest request) {
        // Check for null or empty fields
        if (request.getFirstName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First name is required");
        }
        if (request.getFirstName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First name cannot be empty");
        }
        if (request.getLastName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Last name is required");
        }
        if (request.getEmail() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (request.getEmail().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be empty");
        }
        if (request.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        if (request.getFirstName().length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First name cannot exceed 50 characters");
        }
        if (request.getLastName().length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Last name cannot exceed 50 characters");
        }

        if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email format");
        }

        validatePassword(request.getPassword());
    }

    private void validatePassword(String password) {
        if (password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long");
        }
        if (password.length() > 16) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at less than 16 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least one number");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least one special character");
        }
    }

    public SignInResponse signin(SignInRequest signInRequest) {
        if (signInRequest.getEmail() == null || signInRequest.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (signInRequest.getPassword() == null || signInRequest.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        // Check if user exists first
        User user = userRepository.findByEmail(signInRequest.getEmail());
        if (user == null) {
            // Return 401 Unauthorized for unregistered users
            throw new BadCredentialsException("User not found with provided email");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            signInRequest.getEmail(),
                            signInRequest.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }

        boolean isWaiter = waiterRepository.isWaiter(signInRequest.getEmail());
        if (isWaiter && user.getRoles() != Roles.WAITER) {
            user.setRoles(Roles.WAITER);
            userRepository.save(user);
        } else if (!isWaiter && user.getRoles() == Roles.WAITER) {
            user.setRoles(Roles.CUSTOMER);
            userRepository.save(user);
        }

        String jwt = jwtService.generateToken(user);
        return new SignInResponse(jwt, user.getUsername(), user.getRoles().toString());
    }



    public String extractUserEmailFromToken(String token) {
        return jwtService.extractUserName(token);
    }

    public List<String> getAllUsers(){
        return userRepository.getAllUserEmails();

    }


    public UserProfileResponse update(String email, UserProfileDto userProfileDto) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }

        String firstName = Optional.ofNullable(userProfileDto.getFirstName())
                .map(String::trim)
                .filter(fn -> !fn.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException("First name is required and cannot be empty"));

        if (firstName.length() < 5 || firstName.length() > 50) {
            throw new IllegalArgumentException("First name must be between 5 and 50 characters");
        }

        String lastName = Optional.ofNullable(userProfileDto.getLastName())
                .map(String::trim)
                .filter(ln -> !ln.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException("Last name is required and cannot be empty"));

        if (lastName.length() < 5 || lastName.length() > 50) {
            throw new IllegalArgumentException("Last name must be between 5 and 50 characters");
        }

        user.setFirstName(firstName);
        user.setLastName(lastName);

        if (userProfileDto.getImageUrl() != null && !userProfileDto.getImageUrl().trim().isEmpty()) {
            user.setImageUrl(userProfileDto.getImageUrl().trim());
        } else {
            user.setImageUrl(null);
        }

        userRepository.save(user);

        UserProfileResponse.UserProfile userProfile = new UserProfileResponse.UserProfile(user.getFirstName(), user.getLastName(), user.getImageUrl());
        return new UserProfileResponse("User profile updated successfully", new UserProfileResponse.UserProfileData(userProfile));
    }



    @Override
    public UserProfileResponse getUserDetails(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }

        UserProfileResponse.UserProfile userProfile = new UserProfileResponse.UserProfile(
                user.getFirstName(),
                user.getLastName(),
                user.getImageUrl()
        );

        return new UserProfileResponse("User details fetched successfully", new UserProfileResponse.UserProfileData(userProfile));
    }



    @Override
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
        userRepository.deleteUser(email);
    }

    @Override
    public void updateUserPassword(String email, String currentPassword, String newPassword) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (currentPassword == null || currentPassword.isEmpty()) {
            throw new IllegalArgumentException("Current password is required");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("New password is required");
        }

        validatePasswordComplexity(newPassword, currentPassword);

        try {
            // Verify current password using Spring Security
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    email, currentPassword));

            User user = userRepository.findByEmail(email);
            if (user == null) {
                throw new ResourceNotFoundException("User not found");
            }

            // Update password
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

        } catch (BadCredentialsException e) {
            throw new IllegalArgumentException("Current password is incorrect");
        } catch (Exception e) {
            throw new ServiceException("Unexpected error updating password", e);
        }
    }

    private void validatePasswordComplexity(String newPassword, String currentPassword) {
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (newPassword.length() > 15) {
            throw new IllegalArgumentException("Password must be less than 16 characters");
        }
        if (!newPassword.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!newPassword.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!newPassword.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Password must contain at least one number");
        }
        if (!newPassword.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
        if (newPassword.equals(currentPassword)) {
            throw new IllegalArgumentException("New password must be different from current password");
        }
    }
}
