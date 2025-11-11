package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.NewUserRequest;
import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.exception.ConditionsNotMetException;
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
        log.info("Registering new user with email: {}", userRequest.getEmail());

        validateUserRegistration(userRequest);

        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new ConflictException("Email must be unique");
        }

        User user = userMapper.toUserEntity(userRequest);
        User savedUser = userRepository.save(user);

        log.info("User registered successfully with ID: {}", savedUser.getId());
        return userMapper.toUserDto(savedUser);
    }

    @Override
    public void removeUser(Long userId) {
        log.info("Removing user with ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found");
        }

        userRepository.deleteById(userId);
        log.info("User with ID: {} removed successfully", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> retrieveUsers(List<Long> userIds, int from, int size) {
        log.info("Retrieving users - IDs: {}, from: {}, size: {}", userIds, from, size);

        Pageable pageable = PageRequest.of(from / size, size);

        if (userIds == null || userIds.isEmpty()) {
            return userRepository.findAll(pageable).stream()
                    .map(userMapper::toUserDto)
                    .collect(Collectors.toList());
        } else {
            return userRepository.findByIdIn(userIds).stream()
                    .map(userMapper::toUserDto)
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userExists(Long userId) {
        return userRepository.existsById(userId);
    }

    private void validateUserRegistration(NewUserRequest userRequest) {
        validateEmailFormat(userRequest.getEmail());
        validateName(userRequest.getName());
    }

    private void validateEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ConditionsNotMetException("Email cannot be empty");
        }

        int atIndex = email.indexOf('@');
        if (atIndex == -1) {
            throw new ConditionsNotMetException("Invalid email format");
        }

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex + 1);

        if (localPart.length() > 64) {
            throw new ConditionsNotMetException("Email local part cannot exceed 64 characters");
        }

        String[] domainLabels = domainPart.split("\\.");
        for (String label : domainLabels) {
            if (label.length() > 63) {
                throw new ConditionsNotMetException("Email domain label cannot exceed 63 characters");
            }
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ConditionsNotMetException("Name cannot be empty");
        }

        if (name.length() < 2 || name.length() > 250) {
            throw new ConditionsNotMetException("Name must be between 2 and 250 characters");
        }
    }
}