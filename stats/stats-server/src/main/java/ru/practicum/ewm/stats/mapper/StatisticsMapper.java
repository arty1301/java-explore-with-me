package ru.practicum.ewm.stats.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.stats.client.EndpointHit;
import ru.practicum.ewm.stats.model.EndpointAccess;

@Mapper(componentModel = "spring")
public interface StatisticsMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "app", target = "application")
    @Mapping(source = "ip", target = "ipAddress")
    @Mapping(source = "timestamp", target = "accessedAt")
    EndpointAccess toEndpointAccess(EndpointHit hit);
}