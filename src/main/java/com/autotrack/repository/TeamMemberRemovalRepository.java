package com.autotrack.repository;

import com.autotrack.model.Team;
import com.autotrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for team member removals.
 */
@Repository
public interface TeamMemberRemovalRepository extends JpaRepository<TeamMemberRemoval, Long> {
    
    List<TeamMemberRemoval> findByTeamOrderByRemovedAtDesc(Team team);
    
    List<TeamMemberRemoval> findByUserOrderByRemovedAtDesc(User user);
    
    Optional<TeamMemberRemoval> findByTeamAndUserAndContributionsRemovedFalse(Team team, User user);
    
    @Query("SELECT tmr FROM TeamMemberRemoval tmr WHERE tmr.team = :team AND tmr.contributionsRemoved = false")
    List<TeamMemberRemoval> findRemovedMembersWithContributions(@Param("team") Team team);
    
    boolean existsByTeamAndUser(Team team, User user);
}
