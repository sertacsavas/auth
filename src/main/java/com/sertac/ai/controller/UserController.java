package com.sertac.ai.controller;

import com.sertac.ai.mapper.UserMapper;
import com.sertac.ai.service.UserService;
import com.sertac.ai.service.AuthService; // Import AuthService
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://192.168.1.3:3000"}, allowCredentials = "true")
public class UserController {

    private final UserService userService;
    private final AuthService authService; // Add this line

    public UserController(UserService userService, AuthService authService) { // Update constructor
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        String userEmail = authService.getUserEmailFromToken(token);
        return ResponseEntity.ok(UserMapper.mapUserToUserResponse(userService.findByEmail(userEmail)));
    }

    // Add more user-related endpoints as needed
}