package ru.practicum.ewm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Compilation;

import java.util.List;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    Page<Compilation> findAll(Pageable pageable);

    @Query("SELECT c FROM Compilation c WHERE " +
            "(:pinned IS NULL OR c.pinned = :pinned)")
    Page<Compilation> findCompilationsByPinnedStatus(@Param("pinned") Boolean pinned, Pageable pageable);

    List<Compilation> findByPinned(Boolean pinned);

    @Query("SELECT c FROM Compilation c WHERE c.title LIKE %:title%")
    Page<Compilation> findByTitleContaining(@Param("title") String title, Pageable pageable);

    boolean existsByTitle(String title);

    @Query("SELECT c FROM Compilation c WHERE c.pinned = :pinned")
    List<Compilation> findCompilationsByPinned(@Param("pinned") boolean pinned, Pageable pageable);
}