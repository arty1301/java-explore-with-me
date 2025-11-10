package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.dto.NewUserRequest;

import java.util.List;

public interface PlatformUserService {
    UserDto registerUser(NewUserRequest request);

    List<UserDto> retrieveUsers(List<Long> userIds, int startingFrom, int pageSize);

    void unregisterUser(Long userId);
}