package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.dto.UserShortDto;
import ru.practicum.ewm.dto.NewUserRequest;
import ru.practicum.ewm.model.PlatformUser;

@Mapper(componentModel = "spring")
public interface PlatformUserMapper {

    UserDto convertToDto(PlatformUser user);

    UserShortDto convertToBriefDto(PlatformUser user);

    @Mapping(target = "id", ignore = true)
    PlatformUser convertToEntity(NewUserRequest request);

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