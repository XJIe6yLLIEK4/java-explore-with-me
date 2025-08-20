package ru.practicum.event.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @EntityGraph(attributePaths = {"category", "initiator"})
    List<Event> findAllByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = {"category", "initiator"})
    List<Event> findAllByInitiator_Id(Long initiatorId);

    @EntityGraph(attributePaths = {"category", "initiator"})
    List<Event> findAllByInitiator_IdOrderByIdAsc(Long initiatorId);

    boolean existsByCategory_Id(Long categoryId);

    long countByIdInAndState(List<Long> ids, EventState state);

    @EntityGraph(attributePaths = {"category", "initiator"})
    List<Event> findAllByStateAndEventDateBetween(EventState state, LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"category", "initiator"})
    List<Event> findAllByState(EventState state);
}
