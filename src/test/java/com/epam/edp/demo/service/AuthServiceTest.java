package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.request.SignInRequest;
import com.epam.edp.demo.dto.request.SignUpRequest;
import com.epam.edp.demo.dto.request.UserProfileDto;
import com.epam.edp.demo.dto.response.AuthResponse;
import com.epam.edp.demo.dto.response.SignInResponse;
import com.epam.edp.demo.dto.response.UserProfileResponse;
import com.epam.edp.demo.entity.Roles;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.exceptions.ResourceNotFoundException;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.repository.WaiterRepository;
import com.epam.edp.demo.service.impl.AuthService;
import com.epam.edp.demo.utility.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

 class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WaiterRepository waiterRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private SignUpRequest signUpRequest;
    private SignInRequest signInRequest;

    @BeforeEach
     void setup() {
        MockitoAnnotations.openMocks(this);

        // Setup test user
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPassword("Password123!");
        testUser.setRoles(Roles.CUSTOMER);

        // Setup sign up request
        signUpRequest = new SignUpRequest();
        signUpRequest.setEmail("test@example.com");
        signUpRequest.setFirstName("John");
        signUpRequest.setLastName("Doe");
        signUpRequest.setPassword("Password123!");

        // Setup sign in request
        signInRequest = new SignInRequest();
        signInRequest.setEmail("test@example.com");
        signInRequest.setPassword("Password123!");

        // Mock ObjectMapper to return our test user
        when(objectMapper.convertValue(any(SignUpRequest.class), eq(User.class))).thenReturn(testUser);
    }

    @Test
     void testSignup_Success() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(waiterRepository.isWaiter(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("Password123!");

        // Act
        AuthResponse response = authService.signup(signUpRequest);

        // Assert
        assertNotNull(response);
        assertEquals("User registered successfully", response.getMessage());
        verify(userRepository).existsByEmail("test@example.com");
        verify(waiterRepository).isWaiter("test@example.com");
        verify(passwordEncoder).encode(signUpRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
     void testSignup_UserExists() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.signup(signUpRequest);
        });
        assertEquals("A user with this email address already exists.", exception.getReason());
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
     void testSignup_InvalidEmail() {
        // Arrange
        signUpRequest.setEmail("invalid-email");

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.signup(signUpRequest);
        });
        assertEquals("Invalid email format", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
     void testSignup_WeakPassword() {
        // Arrange
        signUpRequest.setPassword("weak");

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.signup(signUpRequest);
        });
        assertEquals("Password must be at least 8 characters long", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
     void testSignin_Success() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(testUser);
        when(waiterRepository.isWaiter(anyString())).thenReturn(false);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        // Act
        SignInResponse response = authService.signin(signInRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getData().getAccessToken());
        assertEquals("test@example.com",  response.getData().getUsername());
        assertEquals("CUSTOMER",  response.getData().getRole());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail("test@example.com");
        verify(jwtService).generateToken(testUser);
    }

    @Test
     void testSignin_EmptyEmail() {
        // Arrange
        signInRequest.setEmail("");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.signin(signInRequest);
        });
        assertEquals("Email is required", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
     void testSignin_EmptyPassword() {
        // Arrange
        signInRequest.setPassword("");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.signin(signInRequest);
        });
        assertEquals("Password is required", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any());
    }


    @Test
     void testSignin_WaiterRoleUpdate() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(testUser);
        when(waiterRepository.isWaiter(anyString())).thenReturn(true);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        // Act
        SignInResponse response = authService.signin(signInRequest);

        // Assert
        assertNotNull(response);
        verify(userRepository).save(testUser);
        assertEquals(Roles.WAITER, testUser.getRoles());
    }

    @Test
     void testExtractUserEmailFromToken() {
        // Arrange
        when(jwtService.extractUserName(anyString())).thenReturn("test@example.com");

        // Act
        String email = authService.extractUserEmailFromToken("test-token");

        // Assert
        assertEquals("test@example.com", email);
        verify(jwtService).extractUserName("test-token");
    }


    @Test
     void testUpdate_UserNotFound() {
        // Arrange
        UserProfileDto profileDto = new UserProfileDto();
        when(userRepository.findByEmail(anyString())).thenReturn(null);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            authService.update("nonexistent@example.com", profileDto);
        });
        assertEquals("User not found with email: nonexistent@example.com", exception.getMessage());
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(userRepository, never()).save(any(User.class));
    }



    @Test
     void testDeleteUser_Success() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(testUser);
        doNothing().when(userRepository).deleteUser(anyString());

        // Act
        authService.deleteUser("test@example.com");

        // Assert
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).deleteUser("test@example.com");
    }

    @Test
     void testDeleteUser_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(null);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            authService.deleteUser("nonexistent@example.com");
        });
        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(userRepository, never()).deleteUser(anyString());
    }

    @Test
     void testUpdateUserPassword_Success() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(testUser);
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");

        // Act
        authService.updateUserPassword("test@example.com", "currentPassword", "NewPassword123!");

        // Assert
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).encode("NewPassword123!");
        verify(userRepository).save(testUser);
        assertEquals("newEncodedPassword", testUser.getPassword());
    }

    @Test
     void testUpdateUserPassword_EmptyEmail() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.updateUserPassword("", "currentPassword", "NewPassword123!");
        });
        assertEquals("Email cannot be null or empty", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
     void testUpdateUserPassword_EmptyCurrentPassword() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.updateUserPassword("test@example.com", "", "NewPassword123!");
        });
        assertEquals("Current password is required", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
     void testUpdateUserPassword_EmptyNewPassword() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.updateUserPassword("test@example.com", "currentPassword", "");
        });
        assertEquals("New password is required", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
     void testUpdateUserPassword_WeakNewPassword() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.updateUserPassword("test@example.com", "currentPassword", "weak");
        });
        assertEquals("Password must be at least 8 characters long", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
     void testUpdateUserPassword_SameAsOldPassword() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.updateUserPassword("test@example.com", "Password123!", "Password123!");
        });
        assertEquals("New password must be different from current password", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
     void testUpdateUserPassword_IncorrectCurrentPassword() {
        // Arrange
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.updateUserPassword("test@example.com", "wrongPassword", "NewPassword123!");
        });
        assertEquals("Current password is incorrect", exception.getMessage());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).save(any(User.class));
    }
    @Test
     void testSignup_NullFirstName() {
        // Arrange
        signUpRequest.setFirstName(null);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.signup(signUpRequest);
        });
        assertEquals("First name is required", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
     void testSignup_EmptyFirstName() {
        // Arrange
        signUpRequest.setFirstName("   ");

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.signup(signUpRequest);
        });
        assertEquals("First name cannot be empty", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
     void testSignup_NullLastName() {
        // Arrange
        signUpRequest.setLastName(null);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.signup(signUpRequest);
        });
        assertEquals("Last name is required", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
     void testSignup_NullEmail() {
        // Arrange
        signUpRequest.setEmail(null);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.signup(signUpRequest);
        });
        assertEquals("Email is required", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
     void testSignup_EmptyEmail() {
        // Arrange
        signUpRequest.setEmail("   ");

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.signup(signUpRequest);
        });
        assertEquals("Email cannot be empty", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
     void testSignup_NullPassword() {
        // Arrange
        signUpRequest.setPassword(null);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.signup(signUpRequest);
        });
        assertEquals("Password is required", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
     void testSignup_FirstNameTooLong() {
        // Arrange
        signUpRequest.setFirstName("a".repeat(51)); // 51 characters

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.signup(signUpRequest);
        });
        assertEquals("First name cannot exceed 50 characters", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
     void testSignup_LastNameTooLong() {
        // Arrange
        signUpRequest.setLastName("a".repeat(51)); // 51 characters

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.signup(signUpRequest);
        });
        assertEquals("Last name cannot exceed 50 characters", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
     void testSignup_PasswordTooLong() {
        // Arrange
        signUpRequest.setPassword("Abcdef1!".repeat(3)); // 24 characters

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.signup(signUpRequest);
        });
        assertEquals("Password must be at less than 16 characters", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
     void testSignup_PasswordNoUppercase() {
        // Arrange
        signUpRequest.setPassword("password123!");

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.signup(signUpRequest);
        });
        assertEquals("Password must contain at least one uppercase letter", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
     void testSignup_PasswordNoLowercase() {
        // Arrange
        signUpRequest.setPassword("PASSWORD123!");

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.signup(signUpRequest);
        });
        assertEquals("Password must contain at least one lowercase letter", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
     void testSignup_PasswordNoDigit() {
        // Arrange
        signUpRequest.setPassword("Password!");

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.signup(signUpRequest);
        });
        assertEquals("Password must contain at least one number", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
     void testSignup_PasswordNoSpecialChar() {
        // Arrange
        signUpRequest.setPassword("Password123");

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.signup(signUpRequest);
        });
        assertEquals("Password must contain at least one special character", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
     void testSignin_NullEmail() {
        // Arrange
        signInRequest.setEmail(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.signin(signInRequest);
        });
        assertEquals("Email is required", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
     void testSignin_NullPassword() {
        // Arrange
        signInRequest.setPassword(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.signin(signInRequest);
        });
        assertEquals("Password is required", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
     void testUpdateUserPassword_NullEmail() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.updateUserPassword(null, "currentPassword", "NewPassword123!");
        });
        assertEquals("Email cannot be null or empty", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
     void testUpdateUserPassword_NullCurrentPassword() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.updateUserPassword("test@example.com", null, "NewPassword123!");
        });
        assertEquals("Current password is required", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any());
    }


}