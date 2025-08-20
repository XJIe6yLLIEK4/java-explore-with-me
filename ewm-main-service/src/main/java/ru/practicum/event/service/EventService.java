package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
        Event e = new Event();
        e.setAnnotation(dto.getAnnotation());
        e.setTitle(dto.getTitle());
        e.setDescription(dto.getDescription());
        e.setCategory(category);
        e.setInitiator(initiator);
        e.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null) {
            e.setLocationLat(dto.getLocation().getLat());
            e.setLocationLon(dto.getLocation().getLon());
        }
        e.setPaid(Boolean.TRUE.equals(dto.getPaid()));
        e.setParticipantLimit(dto.getParticipantLimit() == null ? 0 : dto.getParticipantLimit());
        e.setRequestModeration(dto.getRequestModeration() == null ? Boolean.TRUE : dto.getRequestModeration());
        e.setState(EventState.PENDING);

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
        List<Event> all = eventRepository.findAll();
        return all.stream()
                .filter(e -> users == null || users.isEmpty() || users.contains(e.getInitiator().getId()))
                .filter(e -> states == null || states.isEmpty() || states.contains(e.getState().name()))
                .filter(e -> categories == null || categories.isEmpty() || categories.contains(e.getCategory().getId()))
                .filter(e -> rangeStart == null || !e.getEventDate().isBefore(rangeStart))
                .filter(e -> rangeEnd == null || !e.getEventDate().isAfter(rangeEnd))
                .sorted(Comparator.comparing(Event::getId))
                .skip(from)
                .limit(size)
                .map(e -> {
                    long confirmed = requestRepository.countByEvent_IdAndStatus(e.getId(), RequestStatus.CONFIRMED);
                    long views = fetchViews(Collections.singletonList(e.getId()), null, null).getOrDefault(e.getId(), 0L);
                    return mapper.toFullDto(e, views, confirmed);
                })
                .collect(Collectors.toList());
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
        List<Event> source = eventRepository.findAllByState(EventState.PUBLISHED);
        LocalDateTime tmpStart = rangeStart;
        LocalDateTime tmpEnd = rangeEnd;
        if (tmpStart != null && tmpEnd != null && tmpStart.isAfter(tmpEnd)) {
            throw new IllegalArgumentException("rangeStart must be before rangeEnd");
        }
        if (tmpStart == null && tmpEnd == null) {
            tmpStart = LocalDateTime.now();
            tmpEnd = LocalDateTime.now().plusYears(100);
        } else {
            if (tmpStart == null) tmpStart = LocalDateTime.now().minusYears(10);
            if (tmpEnd == null) tmpEnd = LocalDateTime.now().plusYears(10);
        }
        final LocalDateTime start = tmpStart;
        final LocalDateTime end = tmpEnd;
        List<Event> filtered = source.stream()
                .filter(e -> text == null || text.isBlank() ||
                        containsIgnoreCase(e.getAnnotation(), text) || containsIgnoreCase(e.getDescription(), text))
                .filter(e -> categories == null || categories.isEmpty() || categories.contains(e.getCategory().getId()))
                .filter(e -> paid == null || Objects.equals(e.getPaid(), paid))
                .filter(e -> !e.getEventDate().isBefore(start) && !e.getEventDate().isAfter(end))
                .collect(Collectors.toList());

        List<Long> ids = filtered.stream().map(Event::getId).toList();
        Map<Long, Long> confirmed = countConfirmed(ids);
        Map<Long, Long> views = fetchViews(ids, start, end);

        if (Boolean.TRUE.equals(onlyAvailable)) {
            filtered = filtered.stream()
                    .filter(e -> e.getParticipantLimit() == null || e.getParticipantLimit() == 0
                            || confirmed.getOrDefault(e.getId(), 0L) < e.getParticipantLimit())
                    .collect(Collectors.toList());
        }

        Comparator<Event> comparator;
        if ("VIEWS".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparing((Event e) -> views.getOrDefault(e.getId(), 0L)).reversed()
                    .thenComparing(Event::getEventDate);
        } else {
            comparator = Comparator.comparing(Event::getEventDate);
        }

        return filtered.stream()
                .sorted(comparator)
                .skip(from)
                .limit(size)
                .map(e -> mapper.toShortDto(e, views.getOrDefault(e.getId(), 0L), confirmed.getOrDefault(e.getId(), 0L)))
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
        } catch (RuntimeException ex) { /* ignore */ }
    }

    private static boolean containsIgnoreCase(String source, String needle) {
        if (source == null || needle == null) return false;
        return source.toLowerCase().contains(needle.toLowerCase());
    }
}
