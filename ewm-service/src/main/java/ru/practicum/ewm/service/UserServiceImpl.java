package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.NewUserRequest;
import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.exception.ConditionsNotMetException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.mapper.UserMapper;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDto addUser(NewUserRequest dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("Email должен быть уникальный");
        }
        int atIndex = dto.getEmail().indexOf('@');

        String localPart = dto.getEmail().substring(0, atIndex);
        String domainPart = dto.getEmail().substring(atIndex + 1);

        if (localPart.length() > 64) {
            throw new ConditionsNotMetException("Local part email не должна превышать 64 символа");
        }

        String[] domainLabels = domainPart.split("\\.");
        for (String label : domainLabels) {
            if (label.length() > 63) {
                throw new ConditionsNotMetException("Domain label не должен превышать 63 символа");
            }
        }

        User user = userMapper.fromNewUserRequest(dto);
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        if (ids == null || ids.isEmpty()) {
            return userRepository.findAll(PageRequest.of(from / size, size)).stream()
                    .map(userMapper::toDto)
                    .collect(Collectors.toList());
        } else {
            return userRepository.findAllById(ids)
                    .stream()
                    .map(userMapper::toDto)
                    .collect(Collectors.toList());
        }
    }
}