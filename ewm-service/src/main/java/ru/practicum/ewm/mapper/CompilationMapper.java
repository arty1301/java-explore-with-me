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
    Compilation toCompilationEntity(NewCompilationDto newCompilationDto);

    @Named("updateCompilationFromDto")
    default Compilation updateCompilationFromDto(Compilation compilation, Compilation updates) {
        if (updates == null) {
            return compilation;
        }

        if (updates.getTitle() != null) {
            compilation.setTitle(updates.getTitle());
        }

        if (updates.getPinned() != null) {
            compilation.setPinned(updates.getPinned());
        }

        if (updates.getEvents() != null) {
            compilation.setEvents(updates.getEvents());
        }

        return compilation;
    }
}