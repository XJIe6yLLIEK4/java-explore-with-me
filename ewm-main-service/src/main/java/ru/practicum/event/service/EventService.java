package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.common.errors.ConflictException;
import ru.practicum.common.errors.ForbiddenOperationException;
import ru.practicum.common.errors.NotFoundException;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.ParticipationRequestRepository;
import ru.practicum.security.ClientIpResolver;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStatsDto;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ParticipationRequestRepository requestRepository;
    private final EventMapper mapper;
    private final StatsClient statsClient;

    @Transactional
    public EventFullDto create(Long userId, NewEventDto dto) {
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id=" + dto.getCategory() + " was not found"));

        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new IllegalArgumentException("Event date must be at least 2 hours in the future");
        }
        Event e = Event.builder()
                .annotation(dto.getAnnotation())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(category)
                .initiator(initiator)
                .eventDate(dto.getEventDate())
                .locationLat(dto.getLocation() == null ? null : dto.getLocation().getLat())
                .locationLon(dto.getLocation() == null ? null : dto.getLocation().getLon())
                .paid(Boolean.TRUE.equals(dto.getPaid()))
                .participantLimit(dto.getParticipantLimit() == null ? 0 : dto.getParticipantLimit())
                .requestModeration(dto.getRequestModeration() == null ? Boolean.TRUE : dto.getRequestModeration())
                .state(EventState.PENDING)
                .build();

        Event saved = eventRepository.save(e);
        return mapper.toFullDto(saved, 0L, 0L);
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        ensureUserExists(userId);
        List<Event> list = eventRepository.findAllByInitiator_Id(userId);
        List<Long> ids = list.stream().map(Event::getId).toList();
        Map<Long, Long> confirmed = countConfirmed(ids);
        Map<Long, Long> views = fetchViews(ids, null, null);
        return list.stream()
                .sorted(Comparator.comparing(Event::getId))
                .skip(from)
                .limit(size)
                .map(e -> mapper.toShortDto(e, views.getOrDefault(e.getId(), 0L), confirmed.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        ensureUserExists(userId);
        Event e = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        if (!e.getInitiator().getId().equals(userId)) {
            throw new ForbiddenOperationException("Only initiator can view own event details endpoint");
        }
        long views = fetchViews(Collections.singletonList(eventId), null, null).getOrDefault(eventId, 0L);
        long confirmed = requestRepository.countByEvent_IdAndStatus(eventId, RequestStatus.CONFIRMED);
        return mapper.toFullDto(e, views, confirmed);
    }

    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest body) {
        ensureUserExists(userId);
        Event e = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        if (!e.getInitiator().getId().equals(userId)) {
            throw new ForbiddenOperationException("Only initiator can edit own event.");
        }
        if (e.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Cannot edit published event");
        }
        if (body.getEventDate() != null && body.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new IllegalArgumentException("Event date must be at least 2 hours in the future");
        }
        applyUserUpdate(e, body);
        Event saved = eventRepository.save(e);
        long confirmed = requestRepository.countByEvent_IdAndStatus(eventId, RequestStatus.CONFIRMED);
        long views = fetchViews(Collections.singletonList(eventId), null, null).getOrDefault(eventId, 0L);
        return mapper.toFullDto(saved, views, confirmed);
    }

    @Transactional(readOnly = true)
    public List<EventFullDto> adminSearch(List<Long> users, List<String> states, List<Long> categories,
                                          LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                          int from, int size) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("rangeStart must be before rangeEnd");
        }

        org.springframework.data.jpa.domain.Specification<Event> spec = org.springframework.data.jpa.domain.Specification.where(null);

        if (users != null && !users.isEmpty()) {
            spec = spec.and((root, q, cb) -> root.get("initiator").get("id").in(users));
        }
        if (states != null && !states.isEmpty()) {
            List<EventState> es = states.stream().map(EventState::valueOf).toList();
            spec = spec.and((root, q, cb) -> root.get("state").in(es));
        }
        if (categories != null && !categories.isEmpty()) {
            spec = spec.and((root, q, cb) -> root.get("category").get("id").in(categories));
        }
        if (rangeStart != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
        }
        if (rangeEnd != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
        }

        int page = from / size;
        var sort = org.springframework.data.domain.Sort.by("id").ascending();
        var pageable = org.springframework.data.domain.PageRequest.of(page, size, sort);

        var pageResult = eventRepository.findAll(spec, pageable);

        List<Long> ids = pageResult.getContent().stream().map(Event::getId).toList();
        Map<Long, Long> confirmed = countConfirmed(ids);
        Map<Long, Long> views = fetchViews(ids, null, null);

        return pageResult.getContent().stream()
                .map(e -> mapper.toFullDto(
                        e,
                        views.getOrDefault(e.getId(), 0L),
                        confirmed.getOrDefault(e.getId(), 0L)))
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public EventFullDto adminUpdate(Long eventId, UpdateEventAdminRequest body) {
        Event e = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        if (body.getStateAction() != null) {
            switch (body.getStateAction()) {
                case PUBLISH_EVENT -> {
                    if (e.getState() != EventState.PENDING) {
                        throw new ConflictException("Only pending event may be published");
                    }
                    e.setState(EventState.PUBLISHED);
                    e.setPublishedOn(LocalDateTime.now());
                }
                case REJECT_EVENT -> {
                    if (e.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Cannot reject published event");
                    }
                    e.setState(EventState.CANCELED);
                }
            }
        }
        if (body.getEventDate() != null && body.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new IllegalArgumentException("Event date must be at least 2 hours in the future");
        }
        applyAdminUpdate(e, body);
        Event saved = eventRepository.save(e);
        long confirmed = requestRepository.countByEvent_IdAndStatus(eventId, RequestStatus.CONFIRMED);
        long views = fetchViews(Collections.singletonList(eventId), null, null).getOrDefault(eventId, 0L);
        return mapper.toFullDto(saved, views, confirmed);
    }

    @Transactional(readOnly = true)
    public List<EventShortDto> publicSearch(String text, List<Long> categories, Boolean paid,
                                            LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                            Boolean onlyAvailable, String sort, int from, int size,
                                            HttpServletRequest request) {
        logHit(request);

        LocalDateTime start = rangeStart;
        LocalDateTime end = rangeEnd;
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("rangeStart must be before rangeEnd");
        }
        if (start == null && end == null) {
            start = LocalDateTime.now();
            end = LocalDateTime.now().plusYears(100);
        } else {
            if (start == null) start = LocalDateTime.now().minusYears(10);
            if (end == null) end = LocalDateTime.now().plusYears(10);
        }

        Specification<Event> spec = Specification.where(
                (root, q, cb) -> cb.equal(root.get("state"), EventState.PUBLISHED)
        );

        if (text != null && !text.isBlank()) {
            String p = "%" + text.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("annotation")), p),
                    cb.like(cb.lower(root.get("description")), p)
            ));
        }
        if (categories != null && !categories.isEmpty()) {
            spec = spec.and((root, q, cb) -> root.get("category").get("id").in(categories));
        }
        if (paid != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("paid"), paid));
        }
        final LocalDateTime fs = start;
        final LocalDateTime fe = end;
        spec = spec.and((root, q, cb) -> cb.between(root.get("eventDate"), fs, fe));

        int page = from / size;
        Sort dbSort = Sort.by("eventDate").ascending();
        var pageable = PageRequest.of(page, size, dbSort);

        var pageData = eventRepository.findAll(spec, pageable);
        List<Event> events = pageData.getContent();

        List<Long> ids = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmed = countConfirmed(ids);
        Map<Long, Long> views = fetchViews(ids, start, end);

        if (Boolean.TRUE.equals(onlyAvailable)) {
            events = events.stream()
                    .filter(e -> e.getParticipantLimit() == null || e.getParticipantLimit() == 0
                            || confirmed.getOrDefault(e.getId(), 0L) < e.getParticipantLimit())
                    .collect(Collectors.toList());
        }

        Comparator<Event> comparator;
        if ("VIEWS".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparing((Event e) -> views.getOrDefault(e.getId(), 0L))
                    .reversed()
                    .thenComparing(Event::getEventDate);
        } else {
            comparator = Comparator.comparing(Event::getEventDate);
        }

        return events.stream()
                .sorted(comparator)
                .map(e -> mapper.toShortDto(
                        e,
                        views.getOrDefault(e.getId(), 0L),
                        confirmed.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventFullDto publicGetById(Long id, HttpServletRequest request) {
        logHit(request);
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));
        if (e.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + id + " was not found");
        }
        long confirmed = requestRepository.countByEvent_IdAndStatus(id, RequestStatus.CONFIRMED);
        long views = fetchViews(Collections.singletonList(id), null, null)
                .getOrDefault(id, 0L);
        return mapper.toFullDto(e, views, confirmed);
    }

    private void applyUserUpdate(Event e, UpdateEventUserRequest body) {
        if (body.getAnnotation() != null) e.setAnnotation(body.getAnnotation());
        if (body.getTitle() != null) e.setTitle(body.getTitle());
        if (body.getDescription() != null) e.setDescription(body.getDescription());
        if (body.getCategory() != null) {
            Category c = categoryRepository.findById(body.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id=" + body.getCategory() + " was not found"));
            e.setCategory(c);
        }
        if (body.getEventDate() != null) e.setEventDate(body.getEventDate());
        if (body.getLocation() != null) {
            e.setLocationLat(body.getLocation().getLat());
            e.setLocationLon(body.getLocation().getLon());
        }
        if (body.getPaid() != null) e.setPaid(body.getPaid());
        if (body.getParticipantLimit() != null) e.setParticipantLimit(body.getParticipantLimit());
        if (body.getRequestModeration() != null) e.setRequestModeration(body.getRequestModeration());
        if (body.getStateAction() != null) {
            switch (body.getStateAction()) {
                case SEND_TO_REVIEW -> e.setState(EventState.PENDING);
                case CANCEL_REVIEW -> e.setState(EventState.CANCELED);
            }
        }
    }

    private void applyAdminUpdate(Event e, UpdateEventAdminRequest body) {
        if (body.getAnnotation() != null) e.setAnnotation(body.getAnnotation());
        if (body.getTitle() != null) e.setTitle(body.getTitle());
        if (body.getDescription() != null) e.setDescription(body.getDescription());
        if (body.getCategory() != null) {
            Category c = categoryRepository.findById(body.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id=" + body.getCategory() + " was not found"));
            e.setCategory(c);
        }
        if (body.getEventDate() != null) e.setEventDate(body.getEventDate());
        if (body.getLocation() != null) {
            e.setLocationLat(body.getLocation().getLat());
            e.setLocationLon(body.getLocation().getLon());
        }
        if (body.getPaid() != null) e.setPaid(body.getPaid());
        if (body.getParticipantLimit() != null) e.setParticipantLimit(body.getParticipantLimit());
        if (body.getRequestModeration() != null) e.setRequestModeration(body.getRequestModeration());
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private Map<Long, Long> countConfirmed(List<Long> ids) {
        Map<Long, Long> map = new HashMap<>();
        for (Long id : ids) {
            long cnt = requestRepository.countByEvent_IdAndStatus(id, RequestStatus.CONFIRMED);
            map.put(id, cnt);
        }
        return map;
    }

    private Map<Long, Long> fetchViews(List<Long> ids, LocalDateTime start, LocalDateTime end) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        if (start == null) start = LocalDateTime.now().minusYears(10);
        if (end == null) end = LocalDateTime.now().plusYears(10);
        List<String> uris = ids.stream().map(id -> "/events/" + id).collect(Collectors.toList());
        List<ViewStatsDto> stats;
        try {
            stats = statsClient.getStats(start, end, uris, true);
        } catch (RuntimeException ex) {
            log.debug("Failed to fetch stats: {}", ex.getMessage());
            stats = Collections.emptyList();
        }
        Map<String, Long> uriToHits = new HashMap<>();
        for (ViewStatsDto s : stats) {
            uriToHits.put(s.getUri(), s.getHits() == null ? 0L : s.getHits());
        }
        Map<Long, Long> result = new HashMap<>();
        for (Long id : ids) {
            result.put(id, uriToHits.getOrDefault("/events/" + id, 0L));
        }
        return result;
    }

    private void logHit(HttpServletRequest request) {
        if (request == null) return;
        String ip = ClientIpResolver.resolve(request);
        String uri = request.getRequestURI();
        EndpointHitDto dto = new EndpointHitDto(null, "ewm-main-service", uri, ip, LocalDateTime.now());
        try {
            statsClient.postHit(dto);
        } catch (RuntimeException ex) {
            log.debug("Failed to log hit: {}", ex.getMessage());
        }
    }

    private static boolean containsIgnoreCase(String source, String needle) {
        if (source == null || needle == null) return false;
        return source.toLowerCase().contains(needle.toLowerCase());
    }
}
