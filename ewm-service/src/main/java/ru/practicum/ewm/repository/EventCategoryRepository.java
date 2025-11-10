package ru.practicum.ewm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.EventCategory;

import java.util.Optional;

public interface EventCategoryRepository extends JpaRepository<EventCategory, Long> {

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM EventCategory c WHERE LOWER(c.name) = LOWER(:categoryName)")
    boolean existsByCategoryName(@Param("categoryName") String name);

    Page<EventCategory> findAllByOrderByIdAsc(Pageable pageable);

    @Query("SELECT c FROM EventCategory c WHERE c.id = :id")
    Optional<EventCategory> findCategoryById(@Param("id") Long id);

    @Query("SELECT c FROM EventCategory c WHERE LOWER(c.name) = LOWER(:name)")
    Optional<EventCategory> findByExactName(@Param("name") String name);
}