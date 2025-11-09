package ru.practicum.ewm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.EventCollection;

import java.util.Optional;

public interface EventCollectionRepository extends JpaRepository<EventCollection, Long> {

    @Query("SELECT c FROM EventCollection c WHERE :pinned IS NULL OR c.pinned = :pinned")
    Page<EventCollection> findCollectionsByPinnedStatus(@Param("pinned") Boolean pinned, Pageable pageable);

    @Query("SELECT c FROM EventCollection c LEFT JOIN FETCH c.events WHERE c.id = :collectionId")
    Optional<EventCollection> findCollectionWithEvents(@Param("collectionId") Long collectionId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM EventCollection c WHERE c.title = :title")
    boolean existsByCollectionTitle(@Param("title") String title);

    @Query("SELECT c FROM EventCollection c ORDER BY c.id ASC")
    Page<EventCollection> findAllCollections(Pageable pageable);
}