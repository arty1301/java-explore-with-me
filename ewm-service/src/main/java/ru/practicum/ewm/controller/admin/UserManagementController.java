package ru.practicum.ewm.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.dto.CreateUserRequest;
import ru.practicum.ewm.service.PlatformUserService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final PlatformUserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto registerPlatformUser(@Valid @RequestBody CreateUserRequest userRequest) {
        log.info("Admin: Registering new user with email: {}", userRequest.getEmail());
        UserDto createdUser = userService.registerUser(userRequest);
        log.info("Admin: Successfully registered user with ID: {}", createdUser.getId());
        return createdUser;
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> retrievePlatformUsers(
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Admin: Retrieving users with IDs: {}, from: {}, size: {}", ids, from, size);
        List<UserDto> users = userService.retrieveUsers(ids, from, size);
        log.info("Admin: Retrieved {} users", users.size());
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> removePlatformUser(@PathVariable Long userId) {
        log.info("Admin: Removing user with ID: {}", userId);
        userService.unregisterUser(userId);
        log.info("Admin: Successfully removed user with ID: {}", userId);
        return ResponseEntity.noContent().build();
    }
}