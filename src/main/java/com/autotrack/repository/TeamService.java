package com.autotrack.service;

import com.autotrack.dto.TeamDTO;
import com.autotrack.model.*;
import com.autotrack.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing teams.
 */
@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final TeamMemberRemovalRepository teamMemberRemovalRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final CommitRepository commitRepository;
    private final NotificationService notificationService;

    public TeamService(TeamRepository teamRepository, UserRepository userRepository, EmailService emailService,
                      TeamMemberRemovalRepository teamMemberRemovalRepository, ProjectRepository projectRepository,
                      TaskRepository taskRepository, CommitRepository commitRepository, NotificationService notificationService) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.teamMemberRemovalRepository = teamMemberRemovalRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.commitRepository = commitRepository;
        this.notificationService = notificationService;
    }

    /**
     * Get all teams.
     */
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    /**
     * Get teams for a user.
     */
    public List<Team> getTeamsByUser(User user) {
        return teamRepository.findTeamsByUser(user);
    }

    /**
     * Get a team by ID.
     */
    public Team getTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found with ID: " + id));
    }

    /**
     * Find a team by ID (alias for getTeamById).
     */
    public Team findById(Long id) {
        return getTeamById(id);
    }

    /**
     * Check if user is a member of the team.
     */
    public boolean isUserMemberOfTeam(User user, Team team) {
        return team.getMembers().contains(user) || team.getOwner().equals(user);
    }

    /**
     * Check if user is the owner of the team.
     */
    public boolean isUserOwnerOfTeam(User user, Team team) {
        return team.getOwner().equals(user);
    }

    /**
     * Check if user can create projects in the team (only owners can create projects).
     */
    public boolean canUserCreateProjects(User user, Team team) {
        return isUserOwnerOfTeam(user, team);
    }

    /**
     * Create a new team.
     */
    @Transactional
    public Team createTeam(TeamDTO teamDTO, User creator) {
        // Get members if any are specified
        List<User> members = teamDTO.getMemberIds() != null ? 
                teamDTO.getMemberIds().stream()
                    .map(id -> userRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("User not found with ID: " + id)))
                    .collect(Collectors.toList()) : 
                new ArrayList<>();
        
        // Make sure creator is in the team
        if (!members.contains(creator)) {
            members.add(creator);
        }
        
        // Make creator a team lead if not already
        if (!creator.hasRole("TEAM_LEAD")) {
            List<Role> roles = creator.getRoles();
            roles.add(Role.TEAM_LEAD);
            creator.setRoles(roles);
            userRepository.save(creator);
        }
        
        // Create the team
        Team team = Team.builder()
                .name(teamDTO.getName())
                .description(teamDTO.getDescription())
                .githubOrganizationUrl(teamDTO.getGithubOrganizationUrl())
                .owner(creator)
                .members(members)
                .build();
        
        return teamRepository.save(team);
    }

    /**
     * Update an existing team.
     */
    @Transactional
    public Team updateTeam(Long id, TeamDTO teamDTO) {
        Team existingTeam = getTeamById(id);
        
        // Get members
        List<User> members = teamDTO.getMemberIds().stream()
                .map(memberId -> userRepository.findById(memberId)
                        .orElseThrow(() -> new RuntimeException("User not found with ID: " + memberId)))
                .collect(Collectors.toList());
        
        // Update team
        existingTeam.setName(teamDTO.getName());
        existingTeam.setDescription(teamDTO.getDescription());
        existingTeam.setGithubOrganizationUrl(teamDTO.getGithubOrganizationUrl());
        existingTeam.setMembers(members);
        
        return teamRepository.save(existingTeam);
    }

    /**
     * Delete a team and all its projects.
     */
    @Transactional
    public void deleteTeam(Long id) {
        Team team = getTeamById(id);
        
        // First, soft delete all projects in this team
        List<Project> teamProjects = projectRepository.findByTeamAndDeletedAtIsNull(team);
        for (Project project : teamProjects) {
            project.setDeleted(true);
            project.setDeletedAt(LocalDateTime.now());
            // Set deletedBy to team leader if available
            if (team.getLeader() != null) {
                project.setDeletedBy(team.getLeader());
            }
            projectRepository.save(project);
        }
        
        // Then delete the team
        teamRepository.delete(team);
    }

    /**
     * Add member to team.
     */
    @Transactional
    public void addMemberToTeam(Team team, User member) {
        if (!team.getMembers().contains(member)) {
            team.getMembers().add(member);
            teamRepository.save(team);
            
            // Send welcome email to the new member
            emailService.sendWelcomeEmail(member, team);
        }
    }

    /**
     * Add member to team with invitation email.
     */
    @Transactional
    public void addMemberToTeamWithInvitation(Team team, User member, User invitedBy, String email) {
        if (!team.getMembers().contains(member)) {
            team.getMembers().add(member);
            teamRepository.save(team);
            
            // Send invitation email if email is provided and different from member's email
            if (email != null && !email.isEmpty() && 
                (member.getEmail() == null || !email.equals(member.getEmail()))) {
                emailService.sendTeamInvitationEmail(email, team, invitedBy);
            }
            
            // Send welcome email to the new member if they have an email
            if (member.getEmail() != null && !member.getEmail().isEmpty()) {
                emailService.sendWelcomeEmail(member, team);
            }
        }
    }

    /**
     * Remove member from team.
     */
    @Transactional
    public void removeMemberFromTeam(Team team, User member) {
        team.getMembers().remove(member);
        teamRepository.save(team);
    }

    /**
     * Check if team has active projects
     */
    public List<Project> getTeamActiveProjects(Long teamId) {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        return projectRepository.findByTeamAndDeletedAtIsNull(team);
    }
    
    /**
     * Delete team (soft delete) - only team lead can delete
     */
    @Transactional
    public void deleteTeam(Long teamId, User currentUser) throws IllegalAccessException {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        if (!team.getOwner().equals(currentUser)) {
            throw new IllegalAccessException("Only team owner can delete the team");
        }
        
        // Soft delete all projects in this team
        List<Project> teamProjects = projectRepository.findByTeamAndDeletedAtIsNull(team);
        for (Project project : teamProjects) {
            project.setDeleted(true);
            project.setDeletedAt(LocalDateTime.now());
            project.setDeletedBy(currentUser);
            projectRepository.save(project);
        }
        
        // Notify all team members about team deletion before removing them
        for (User member : team.getMembers()) {
            if (!member.equals(currentUser)) {
                notificationService.createNotification(
                    member,
                    "Team Deleted",
                    "The team '" + team.getName() + "' has been deleted by the team owner.",
                    "/teams"
                );
            }
        }
        
        // Log removal for each team member
        for (User member : team.getMembers()) {
            TeamMemberRemoval removal = new TeamMemberRemoval();
            removal.setTeam(team);
            removal.setUser(member);
            removal.setRemovedBy(currentUser);
            removal.setRemovalType(RemovalType.TEAM_DELETED);
            removal.setRemovedAt(LocalDateTime.now());
            removal.setRemovalReason("Team deleted by owner");
            teamMemberRemovalRepository.save(removal);
        }
        
        // Clear team members before soft deletion
        team.getMembers().clear();
        teamRepository.save(team);
        
        // Soft delete the team
        team.setDeleted(true);
        team.setDeletedAt(LocalDateTime.now());
        team.setDeletedBy(currentUser);
        teamRepository.save(team);
    }
    
    /**
     * Leave team - member leaves voluntarily
     */
    @Transactional
    public void leaveTeam(Long teamId, User currentUser) throws IllegalAccessException {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        if (!team.getMembers().contains(currentUser)) {
            throw new IllegalArgumentException("User is not a member of this team");
        }
        
        if (team.getLeader().equals(currentUser)) {
            throw new IllegalAccessException("Team leader cannot leave the team. Transfer leadership or delete the team instead.");
        }
        
        // Remove member from team
        team.getMembers().remove(currentUser);
        teamRepository.save(team);
        
        // Soft delete user's projects in this team
        List<Project> userProjects = projectRepository.findByTeamAndCreatedByAndDeletedAtIsNull(team, currentUser);
        for (Project project : userProjects) {
            project.setDeleted(true);
            project.setDeletedAt(LocalDateTime.now());
            project.setDeletedBy(currentUser);
            projectRepository.save(project);
        }
        
        // Notify team leader about member leaving
        notificationService.createNotification(
            team.getLeader(),
            "Member Left Team",
            currentUser.getFullName() + " has left the team '" + team.getName() + "'.",
            "/teams/" + teamId
        );
        
        // Log the removal
        TeamMemberRemoval removal = new TeamMemberRemoval();
        removal.setTeam(team);
        removal.setUser(currentUser);
        removal.setRemovedBy(currentUser);
        removal.setRemovalType(RemovalType.LEFT);
        removal.setRemovedAt(LocalDateTime.now());
        removal.setRemovalReason("Member left voluntarily");
        teamMemberRemovalRepository.save(removal);
    }
    
    /**
     * Kick member from team - only team lead can kick members
     */
    @Transactional
    public void kickMember(Long teamId, Long memberId, User currentUser, String reason) throws IllegalAccessException {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        User memberToKick = userRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        
        if (!team.getLeader().equals(currentUser)) {
            throw new IllegalAccessException("Only team leader can kick members");
        }
        
        if (!team.getMembers().contains(memberToKick)) {
            throw new IllegalArgumentException("User is not a member of this team");
        }
        
        if (memberToKick.equals(currentUser)) {
            throw new IllegalArgumentException("Team leader cannot kick themselves");
        }
        
        // Remove member from team
        team.getMembers().remove(memberToKick);
        teamRepository.save(team);
        
        // Note: We keep their projects and contributions for now
        // They can be explicitly removed later if needed
        
        // Notify the kicked member
        notificationService.createNotification(
            memberToKick,
            "Removed from Team",
            "You have been removed from the team '" + team.getName() + "'" + 
            (reason != null && !reason.trim().isEmpty() ? ". Reason: " + reason : "."),
            "/teams"
        );
        
        // Log the removal
        TeamMemberRemoval removal = new TeamMemberRemoval();
        removal.setTeam(team);
        removal.setUser(memberToKick);
        removal.setRemovedBy(currentUser);
        removal.setRemovalType(RemovalType.KICKED);
        removal.setRemovedAt(LocalDateTime.now());
        removal.setRemovalReason(reason != null ? reason : "No reason provided");
        teamMemberRemovalRepository.save(removal);
    }
    
    /**
     * Transfer team leadership
     */
    @Transactional
    public void transferLeadership(Long teamId, Long newLeaderId, User currentUser) throws IllegalAccessException {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        User newLeader = userRepository.findById(newLeaderId)
            .orElseThrow(() -> new IllegalArgumentException("New leader not found"));
        
        if (!team.getLeader().equals(currentUser)) {
            throw new IllegalAccessException("Only current team leader can transfer leadership");
        }
        
        if (!team.getMembers().contains(newLeader)) {
            throw new IllegalArgumentException("New leader must be a team member");
        }
        
        User previousLeader = team.getLeader();
        team.setLeader(newLeader);
        teamRepository.save(team);
        
        // Notify new leader
        notificationService.createNotification(
            newLeader,
            "Team Leadership Transferred",
            "You are now the leader of team '" + team.getName() + "'.",
            "/teams/" + teamId
        );
        
        // Notify all other team members
        for (User member : team.getMembers()) {
            if (!member.equals(newLeader) && !member.equals(previousLeader)) {
                notificationService.createNotification(
                    member,
                    "Team Leadership Changed",
                    newLeader.getFullName() + " is now the leader of team '" + team.getName() + "'.",
                    "/teams/" + teamId
                );
            }
        }
    }
    
    /**
     * Delete project (soft delete) - only project creator or team owner can delete
     */
    @Transactional
    public void deleteProject(Long projectId, User currentUser) throws IllegalAccessException {
        System.out.println("DEBUG: Attempting to delete project with ID: " + projectId);
        System.out.println("DEBUG: Current user: " + currentUser.getEmail());
        
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        
        System.out.println("DEBUG: Found project: " + project.getName());
        System.out.println("DEBUG: Project owner: " + (project.getOwner() != null ? project.getOwner().getEmail() : "null"));
        
        Team team = project.getTeam();
        System.out.println("DEBUG: Project team: " + (team != null ? team.getName() : "null"));
        
        if (team != null && team.getOwner() != null) {
            System.out.println("DEBUG: Team owner: " + team.getOwner().getEmail());
        }
        
        // Check if user is project owner or team owner
        boolean isProjectOwner = project.getOwner() != null && project.getOwner().equals(currentUser);
        boolean isTeamOwner = team != null && team.getOwner() != null && team.getOwner().equals(currentUser);
        
        if (!isProjectOwner && !isTeamOwner) {
            System.out.println("DEBUG: Authorization failed - user is not project owner or team owner");
            throw new IllegalAccessException("Only project owner or team owner can delete the project");
        }
        
        System.out.println("DEBUG: Authorization successful - proceeding with deletion");
        
        // Soft delete the project
        project.setDeleted(true);
        project.setDeletedAt(LocalDateTime.now());
        project.setDeletedBy(currentUser);
        projectRepository.save(project);
        
        System.out.println("DEBUG: Project marked as deleted");
        
        // Notify team members about project deletion
        if (team != null) {
            for (User member : team.getMembers()) {
                if (!member.equals(currentUser)) {
                    notificationService.createNotification(
                        member,
                        "Project Deleted",
                        "The project '" + project.getName() + "' has been deleted.",
                        "/teams/" + team.getId()
                    );
                }
            }
        }
    }
}
