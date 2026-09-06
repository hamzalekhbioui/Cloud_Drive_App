package com.cloud.drive.repository;

import com.cloud.drive.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<Plan> findBySlug(String slug);
    List<Plan> findByActiveTrueOrderByIdAsc();
}
