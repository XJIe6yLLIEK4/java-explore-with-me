package ru.practicum.compilation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.compilation.model.Compilation;

@Repository
public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    @EntityGraph(attributePaths = {"events", "events.category", "events.initiator"})
    Page<Compilation> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"events", "events.category", "events.initiator"})
    Page<Compilation> findAllByPinned(boolean pinned, Pageable pageable);
}
