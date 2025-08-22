package ru.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.RequestStatus;

import java.util.Collection;
import java.util.List;

@Repository
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    boolean existsByEvent_IdAndRequester_Id(Long eventId, Long requesterId);

    long countByEvent_IdAndStatus(Long eventId, RequestStatus status);

    @Query("select pr.event.id as eventId, count(pr.id) as cnt " +
           "from ParticipationRequest pr " +
           "where pr.event.id in :eventIds and pr.status = :status " +
           "group by pr.event.id")
    List<Object[]> countByEventIdsAndStatus(@Param("eventIds") Collection<Long> eventIds,
                                            @Param("status") RequestStatus status);

    List<ParticipationRequest> findAllByRequester_Id(Long requesterId);

    List<ParticipationRequest> findAllByEvent_Id(Long eventId);

    List<ParticipationRequest> findAllByIdIn(Collection<Long> ids);
}
