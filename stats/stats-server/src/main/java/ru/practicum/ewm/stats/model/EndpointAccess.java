package ru.practicum.ewm.stats.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "endpoint_accesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EndpointAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_name", nullable = false, length = 100)
    private String application;

    @Column(name = "uri_path", nullable = false, length = 512)
    private String uri;

    @Column(name = "client_ip", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "access_timestamp", nullable = false)
    private LocalDateTime accessedAt;
}