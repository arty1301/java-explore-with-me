package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.NewUserRequest;
import ru.practicum.ewm.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto registerUser(NewUserRequest userRequest);
    List<UserDto> retrieveUsers(List<Long> ids, int from, int size);
    void removeUser(Long userId);
}