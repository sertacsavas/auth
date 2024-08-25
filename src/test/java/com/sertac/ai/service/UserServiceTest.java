package com.sertac.ai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sertac.ai.model.entity.User;
import com.sertac.ai.repository.UserRepository;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createUser_ValidUser_ReturnsCreatedUser() {
        User user = new User();
        user.setEmail("test@example.com");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(null);
        when(userRepository.save(user)).thenReturn(user);

        User createdUser = userService.createUser(user);

        assertNotNull(createdUser);
        assertEquals(user.getEmail(), createdUser.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void createUser_NullUser_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(null));
    }

    @Test
    void createUser_EmptyEmail_ThrowsIllegalArgumentException() {
        User user = new User();
        user.setEmail("");
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(user));
    }

    @Test
    void createUser_InvalidEmail_ThrowsIllegalArgumentException() {
        User user = new User();
        user.setEmail("invalid-email");
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(user));
    }

    @Test
    void createUser_ExistingEmail_ThrowsIllegalStateException() {
        User user = new User();
        user.setEmail("existing@example.com");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(new User());

        assertThrows(IllegalStateException.class, () -> userService.createUser(user));
    }

    @Test
    void findByEmail_ValidEmail_ReturnsUser() {
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        when(userRepository.findByEmail(email)).thenReturn(user);

        User foundUser = userService.findByEmail(email);

        assertNotNull(foundUser);
        assertEquals(email, foundUser.getEmail());
    }

    @Test
    void findByEmail_NullEmail_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> userService.findByEmail(null));
    }

    @Test
    void findByEmail_EmptyEmail_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> userService.findByEmail(""));
    }

    @Test
    void findByEmail_InvalidEmail_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> userService.findByEmail("invalid-email"));
    }
}