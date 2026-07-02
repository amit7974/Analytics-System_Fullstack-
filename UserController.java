package com.example.analytics.controller;

import com.example.analytics.exception.ResourceNotFoundException;
import com.example.analytics.model.AppUser;
import com.example.analytics.repository.AppUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository appUserRepository;

    public UserController(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @PostMapping
    public ResponseEntity<AppUser> createUser(@Valid @RequestBody CreateUserRequest request) {
        if (appUserRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("username already exists: " + request.getUsername());
        }
        AppUser user = new AppUser();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        AppUser saved = appUserRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<AppUser> listUsers() {
        return appUserRepository.findAll();
    }

    @GetMapping("/{id}")
    public AppUser getUser(@PathVariable Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No user found with id " + id));
    }

    @Getter
    @Setter
    public static class CreateUserRequest {
        @NotBlank(message = "username is required")
        private String username;
        private String email;
    }
}
