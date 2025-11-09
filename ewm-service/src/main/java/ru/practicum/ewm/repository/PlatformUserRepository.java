package ru.practicum.ewm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.PlatformUser;

import java.util.List;
import java.util.Optional;

public interface PlatformUserRepository extends JpaRepository<PlatformUser, Long> {

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
            "FROM PlatformUser u WHERE u.email = :userEmail")
    boolean existsUserByEmail(@Param("userEmail") String email);

    @Query("SELECT u FROM PlatformUser u WHERE u.id IN :userIds ORDER BY u.id ASC")
    List<PlatformUser> findUsersByIdList(@Param("userIds") List<Long> userIds);

    @Query("SELECT u FROM PlatformUser u ORDER BY u.id ASC")
    Page<PlatformUser> findAllUsers(Pageable pageable);

    @Query("SELECT u FROM PlatformUser u WHERE u.email = :email")
    Optional<PlatformUser> findUserByEmail(@Param("email") String email);
}