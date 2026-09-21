package com.cloud.drive.repository;

import com.cloud.drive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByStatus(String status);

    @Query("SELECT FUNCTION('DATE', u.createdAt), COUNT(u) FROM User u WHERE u.createdAt >= :since GROUP BY FUNCTION('DATE', u.createdAt) ORDER BY FUNCTION('DATE', u.createdAt)")
    List<Object[]> countSignupsByDateSince(@Param("since") LocalDateTime since);
}