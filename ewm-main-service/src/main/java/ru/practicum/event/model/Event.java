package ru.practicum.event.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import ru.practicum.category.model.Category;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    @NotBlank
    @Size(min = 20, max = 2000)
    private String annotation;

    @Column(nullable = false, length = 120)
    @NotBlank
    @Size(min = 3, max = 120)
    private String title;

    @Column(nullable = false, length = 7000)
    @NotBlank
    @Size(min = 20, max = 7000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @NotNull
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    @NotNull
    private User initiator;

    @Column(nullable = false)
    @NotNull
    private LocalDateTime eventDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdOn;

    private LocalDateTime publishedOn;

    @Builder.Default
    @Column(nullable = false)
    private Boolean paid = Boolean.FALSE;

    @Builder.Default
    @Column(nullable = false)
    private Integer participantLimit = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean requestModeration = Boolean.TRUE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventState state = EventState.PENDING;

    @Column(name = "location_lat")
    private Double locationLat;

    @Column(name = "location_lon")
    private Double locationLon;
}
