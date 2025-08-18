package ru.practicum.statsserver.repository;

import org.springframework.data.jpa.repository.Query;
import ru.practicum.statsserver.model.Hit;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.statsserver.model.ViewStatsRow;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface HitRepository extends JpaRepository<Hit, Long> {
    @Query("""
               select h.app as app, h.uri as uri, count(h.id) as hits
               from Hit h
               where h.timestamp between :start and :end
               and h.uri in :uris
               group by h.app, h.uri
               order by hits desc
            """)
    List<ViewStatsRow> findStats(LocalDateTime start, LocalDateTime end, Collection<String> uris);

    @Query("""
               select h.app as app, h.uri as uri, count(distinct h.ip) as hits
               from Hit h
               where h.timestamp between :start and :end
                 and h.uri in :uris
               group by h.app, h.uri
               order by hits desc
            """)
    List<ViewStatsRow> findUniqueStats(LocalDateTime start, LocalDateTime end, Collection<String> uris);

    @Query("""
               select h.app as app, h.uri as uri, count(h.id) as hits
               from Hit h
               where h.timestamp between :start and :end
               group by h.app, h.uri
               order by hits desc
            """)
    List<ViewStatsRow> findAllUriStats(LocalDateTime start, LocalDateTime end);

    @Query("""
               select h.app as app, h.uri as uri, count(distinct h.ip) as hits
               from Hit h
               where h.timestamp between :start and :end
               group by h.app, h.uri
               order by hits desc
            """)
    List<ViewStatsRow> findUniqueAllUriStats(LocalDateTime start, LocalDateTime end);
}


