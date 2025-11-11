package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.NewUserRequest;
import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.UserMapper;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDto registerUser(NewUserRequest userRequest) {
        log.info("Registering new user: {}", userRequest.getEmail());

        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new ConflictException("Email already registered");
        }

        validateEmailFormat(userRequest.getEmail());

        User user = userMapper.toUserEntity(userRequest);
        User savedUser = userRepository.save(user);

        log.info("User registered successfully with ID: {}", savedUser.getId());
        return userMapper.toUserDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> retrieveUsers(List<Long> ids, int from, int size) {
        log.info("Retrieving users with IDs: {}, from: {}, size: {}", ids, from, size);

        Pageable pageable = PageRequest.of(from / size, size);

        List<User> users;
        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(pageable).getContent();
        } else {
            users = userRepository.findByIdIn(ids, pageable).getContent();
        }

        return users.stream()
                .map(userMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public void removeUser(Long userId) {
        log.info("Removing user with ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found");
        }

        userRepository.deleteById(userId);
        log.info("User with ID {} removed successfully", userId);
    }

    private void validateEmailFormat(String email) {
        if (email == null || email.isBlank()) {
            throw new ConflictException("Email cannot be empty");
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            throw new ConflictException("Invalid email format");
        }

        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() > 64) {
            throw new ConflictException("Email local part cannot exceed 64 characters");
        }

        String[] domainLabels = domain.split("\\.");
        for (String label : domainLabels) {
            if (label.length() > 63) {
                throw new ConflictException("Email domain label cannot exceed 63 characters");
            }
        }
    }
}