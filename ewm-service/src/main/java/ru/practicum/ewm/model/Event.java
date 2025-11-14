package ru.practicum.ewm.model;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "annotation", nullable = false, length = 2000)
    private String annotation;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;
    @Column(name = "description", length = 7000)
    private String description;
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;
    @Embedded
    private Location location;
    @Column(name = "paid", nullable = false)
    private Boolean paid;
    @Column(name = "participant_limit")
    private Integer participantLimit;
    @Column(name = "published_on")
    private LocalDateTime publishedOn;
    @Column(name = "request_moderation")
    private Boolean requestModeration;
    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private EventState state;
    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @Transient
    private Long views;

    public enum EventState {
        PENDING, PUBLISHED, CANCELED
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setEvent(this);
    }

    public void removeComment(Comment comment) {
        comments.remove(comment);
        comment.setEvent(null);
    }
}