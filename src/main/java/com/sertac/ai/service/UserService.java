package com.sertac.ai.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sertac.ai.email.EmailUtils;
import com.sertac.ai.model.entity.User;
import com.sertac.ai.repository.UserRepository;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(User user) {
        // Check if user is null
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        // Check if email is null or empty
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        // Check if email is valid
        if (!EmailUtils.isValidEmail(user.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Check if email already exists
        if (userRepository.findByEmail(user.getEmail()) != null) {
            throw new IllegalStateException("Email already exists");
        }


        return userRepository.save(user);
    }



    public User findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        if (!EmailUtils.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        return userRepository.findByEmail(email);
    }
    
}