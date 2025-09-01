package com.autotrack.repository;

import com.autotrack.model.TeamInvitation;
import com.autotrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {
    
    /**
     * Find all invitations for a specific user (both by GitHub URL and user ID)
     */
    @Query("SELECT ti FROM TeamInvitation ti WHERE ti.invitee = :user OR ti.inviteeGithubUrl = :githubUrl")
    List<TeamInvitation> findInvitationsForUser(@Param("user") User user, @Param("githubUrl") String githubUrl);
    
    /**
     * Find pending invitations for a specific user
     */
    @Query("SELECT ti FROM TeamInvitation ti WHERE (ti.invitee = :user OR ti.inviteeGithubUrl = :githubUrl) AND ti.status = 'PENDING'")
    List<TeamInvitation> findPendingInvitationsForUser(@Param("user") User user, @Param("githubUrl") String githubUrl);
    
    /**
     * Find all invitations sent by a specific user
     */
    List<TeamInvitation> findByInviter(User inviter);
    
    /**
     * Find all invitations for a specific team
     */
    @Query("SELECT ti FROM TeamInvitation ti WHERE ti.team.id = :teamId")
    List<TeamInvitation> findByTeamId(@Param("teamId") Long teamId);
    
    /**
     * Check if an invitation already exists for a GitHub URL and team
     */
    @Query("SELECT ti FROM TeamInvitation ti WHERE ti.team.id = :teamId AND ti.inviteeGithubUrl = :githubUrl AND ti.status = 'PENDING'")
    Optional<TeamInvitation> findPendingInvitationByTeamAndGithubUrl(@Param("teamId") Long teamId, @Param("githubUrl") String githubUrl);
    
    /**
     * Find invitations by GitHub URL (for linking when user registers)
     */
    List<TeamInvitation> findByInviteeGithubUrlAndInviteeIsNull(String githubUrl);
    
    /**
     * Count pending invitations for a user
     */
    @Query("SELECT COUNT(ti) FROM TeamInvitation ti WHERE (ti.invitee = :user OR ti.inviteeGithubUrl = :githubUrl) AND ti.status = 'PENDING'")
    long countPendingInvitationsForUser(@Param("user") User user, @Param("githubUrl") String githubUrl);
}
