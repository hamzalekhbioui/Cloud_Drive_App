package com.cloud.drive.repository;

import com.cloud.drive.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByOwnerEmail(String ownerEmail);
}