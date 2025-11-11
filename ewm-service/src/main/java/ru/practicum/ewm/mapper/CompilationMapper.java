package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.NewCompilationDto;
import ru.practicum.ewm.model.Compilation;

@Mapper(componentModel = "spring", uses = {EventMapper.class})
public interface CompilationMapper {

    @Mapping(source = "events", target = "events")
    CompilationDto toCompilationDto(Compilation compilation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    @Mapping(source = "pinned", target = "pinned", qualifiedByName = "defaultPinned")
    Compilation toCompilationEntity(NewCompilationDto newCompilationDto);

    @Named("defaultPinned")
    default Boolean defaultPinned(Boolean pinned) {
        return pinned != null ? pinned : false;
    }
}