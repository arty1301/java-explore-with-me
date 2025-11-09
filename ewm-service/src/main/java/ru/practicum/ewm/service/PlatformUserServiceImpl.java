package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.dto.CreateUserRequest;
import ru.practicum.ewm.exception.DataConflictException;
import ru.practicum.ewm.exception.ResourceNotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.mapper.PlatformUserMapper;
import ru.practicum.ewm.model.PlatformUser;
import ru.practicum.ewm.repository.PlatformUserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PlatformUserServiceImpl implements PlatformUserService {

    private final PlatformUserRepository userRepository;
    private final PlatformUserMapper userMapper;

    @Override
    public UserDto registerUser(CreateUserRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        validateUserEmail(request.getEmail());

        if (userRepository.existsUserByEmail(request.getEmail())) {
            throw new DataConflictException("User with email already exists: " + request.getEmail());
        }

        PlatformUser newUser = userMapper.convertToEntity(request);
        PlatformUser savedUser = userRepository.save(newUser);

        log.info("Successfully registered user with ID: {}", savedUser.getId());
        return userMapper.convertToDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> retrieveUsers(List<Long> userIds, int startingFrom, int pageSize) {
        log.info("Retrieving users with IDs: {}, from: {}, size: {}", userIds, startingFrom, pageSize);

        if (userIds == null || userIds.isEmpty()) {
            Pageable pageable = PageRequest.of(startingFrom / pageSize, pageSize);
            return userRepository.findAllUsers(pageable)
                    .getContent()
                    .stream()
                    .map(userMapper::convertToDto)
                    .collect(Collectors.toList());
        } else {
            return userRepository.findUsersByIdList(userIds)
                    .stream()
                    .map(userMapper::convertToDto)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void unregisterUser(Long userId) {
        log.info("Unregistering user with ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        userRepository.deleteById(userId);
        log.info("Successfully unregistered user with ID: {}", userId);
    }

    private void validateUserEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email cannot be null or blank");
        }

        int atIndex = email.indexOf('@');
        if (atIndex == -1) {
            throw new ValidationException("Invalid email format: missing @ symbol");
        }

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex + 1);

        if (localPart.length() > 64) {
            throw new ValidationException("Email local part exceeds 64 characters");
        }

        String[] domainLabels = domainPart.split("\\.");
        for (String label : domainLabels) {
            if (label.length() > 63) {
                throw new ValidationException("Domain label exceeds 63 characters: " + label);
            }
        }

        log.debug("Email validation passed for: {}", email);
    }
}