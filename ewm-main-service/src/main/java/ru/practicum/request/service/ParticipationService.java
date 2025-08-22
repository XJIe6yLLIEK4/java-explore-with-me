package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.errors.ConflictException;
import ru.practicum.common.errors.ForbiddenOperationException;
import ru.practicum.common.errors.NotFoundException;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.mapper.ParticipationMapper;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.ParticipationRequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ParticipationMapper mapper;

    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        ensureUserExists(userId);
        return requestRepository.findAllByRequester_Id(userId)
                .stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot request participation in own event.");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot participate in unpublished event.");
        }
        if (requestRepository.existsByEvent_IdAndRequester_Id(eventId, userId)) {
            throw new ConflictException("Duplicate participation request.");
        }
        long confirmed = requestRepository.countByEvent_IdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() != null && event.getParticipantLimit() > 0 && confirmed >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }

        ParticipationRequest pr = new ParticipationRequest();
        pr.setEvent(event);
        pr.setRequester(requester);
        if (Boolean.FALSE.equals(event.getRequestModeration())
                || (event.getParticipantLimit() != null && event.getParticipantLimit() == 0)) {
            pr.setStatus(RequestStatus.CONFIRMED);
        } else {
            pr.setStatus(RequestStatus.PENDING);
        }
        return mapper.toDto(requestRepository.save(pr));
    }

    @Transactional
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        ParticipationRequest pr = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));
        if (!pr.getRequester().getId().equals(userId)) {
            throw new ForbiddenOperationException("Only requester can cancel own request.");
        }
        if (pr.getStatus() == RequestStatus.CONFIRMED) {
            throw new ConflictException("Cannot cancel confirmed request");
        }
        pr.setStatus(RequestStatus.CANCELED);
        return mapper.toDto(requestRepository.save(pr));
    }

    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenOperationException("Only initiator can view participation requests for the event.");
        }
        return requestRepository.findAllByEvent_Id(eventId)
                .stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Transactional
    public EventRequestStatusUpdateResult changeStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest body) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenOperationException("Only initiator can change participation requests for the event.");
        }

        if (Boolean.FALSE.equals(event.getRequestModeration())) {
            return new EventRequestStatusUpdateResult();
        }

        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(body.getRequestIds());
        requests = requests.stream()
                .filter(r -> r.getEvent().getId().equals(eventId))
                .collect(Collectors.toList());
        if (requests.isEmpty()) {
            return new EventRequestStatusUpdateResult();
        }
        if (requests.stream().anyMatch(r -> r.getStatus() != RequestStatus.PENDING)) {
            throw new ConflictException("Only pending requests can be changed");
        }

        long confirmed = requestRepository.countByEvent_IdAndStatus(eventId, RequestStatus.CONFIRMED);
        int limit = event.getParticipantLimit() == null ? 0 : event.getParticipantLimit();

        if (limit > 0 && confirmed >= limit && body.getStatus() == EventRequestStatusUpdateRequest.UpdateAction.CONFIRMED) {
            throw new ConflictException("The participant limit has been reached");
        }

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();

        if (body.getStatus() == EventRequestStatusUpdateRequest.UpdateAction.REJECTED) {
            for (ParticipationRequest r : requests) {
                if (r.getStatus() == RequestStatus.PENDING) {
                    r.setStatus(RequestStatus.REJECTED);
                    result.getRejectedRequests().add(mapper.toDto(r));
                }
            }
            requestRepository.saveAll(requests);
            return result;
        }

        for (ParticipationRequest r : requests) {
            if (r.getStatus() != RequestStatus.PENDING) continue;
            if (limit > 0 && confirmed >= limit) {
                r.setStatus(RequestStatus.REJECTED);
                result.getRejectedRequests().add(mapper.toDto(r));
            } else {
                r.setStatus(RequestStatus.CONFIRMED);
                confirmed++;
                result.getConfirmedRequests().add(mapper.toDto(r));
            }
        }
        requestRepository.saveAll(requests);

        if (limit > 0 && confirmed >= limit) {
            List<ParticipationRequest> pending = requestRepository.findAllByEvent_Id(eventId)
                    .stream().filter(r -> r.getStatus() == RequestStatus.PENDING)
                    .collect(Collectors.toList());
            if (!pending.isEmpty()) {
                for (ParticipationRequest r : pending) {
                    r.setStatus(RequestStatus.REJECTED);
                    result.getRejectedRequests().add(mapper.toDto(r));
                }
                requestRepository.saveAll(pending);
            }
        }

        return result;
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }
}
