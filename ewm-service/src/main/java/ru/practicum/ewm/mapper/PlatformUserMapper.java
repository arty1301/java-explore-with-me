package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.dto.UserBriefDto;
import ru.practicum.ewm.dto.CreateUserRequest;
import ru.practicum.ewm.model.PlatformUser;

@Mapper(componentModel = "spring")
public interface PlatformUserMapper {

    UserDto convertToDto(PlatformUser user);

    UserBriefDto convertToBriefDto(PlatformUser user);

    @Mapping(target = "id", ignore = true)
    PlatformUser convertToEntity(CreateUserRequest request);

    @Mapping(target = "id", ignore = true)
    PlatformUser convertToEntity(UserDto dto);

    default PlatformUser updateEntityFromDto(UserDto dto, PlatformUser entity) {
        if (dto == null) {
            return entity;
        }

        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getEmail() != null) {
            entity.setEmail(dto.getEmail());
        }

        return entity;
    }
}