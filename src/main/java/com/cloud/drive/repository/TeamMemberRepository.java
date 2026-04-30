package com.cloud.drive.repository;

import com.cloud.drive.model.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByTeamId(Long teamId);
    List<TeamMember> findByUserEmailAndStatus(String userEmail, String status);
    Optional<TeamMember> findByInviteToken(String inviteToken);
    Optional<TeamMember> findByTeamIdAndUserEmail(Long teamId, String userEmail);
    boolean existsByTeamIdAndUserEmail(Long teamId, String userEmail);

    @Query("SELECT DISTINCT tm.teamId FROM TeamMember tm WHERE tm.userEmail = :email AND tm.status = 'ACTIVE'")
    List<Long> findActiveTeamIdsByUserEmail(String email);
}