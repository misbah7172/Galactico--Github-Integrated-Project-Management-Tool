package com.autotrack.service;

import com.autotrack.dto.ProjectDTO;
import com.autotrack.dto.TeamMemberContributionDTO;
import com.autotrack.model.Project;
import com.autotrack.model.ProjectTeamHistory;
import com.autotrack.model.Team;
import com.autotrack.model.User;
import com.autotrack.model.Task;
import com.autotrack.model.TaskStatus;
import com.autotrack.model.Commit;
import com.autotrack.repository.ProjectRepository;
import com.autotrack.repository.ProjectTeamHistoryRepository;
import com.autotrack.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Service for managing projects.
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectTeamHistoryRepository projectTeamHistoryRepository;
    private final TeamRepository teamRepository;
    private final TeamService teamService;

    public ProjectService(ProjectRepository projectRepository, 
                         ProjectTeamHistoryRepository projectTeamHistoryRepository,
                         TeamRepository teamRepository, 
                         TeamService teamService) {
        this.projectRepository = projectRepository;
        this.projectTeamHistoryRepository = projectTeamHistoryRepository;
        this.teamRepository = teamRepository;
        this.teamService = teamService;
    }

    /**
     * Get all projects (non-deleted only).
     */
    public List<Project> getAllProjects() {
        return projectRepository.findAllByDeletedAtIsNull();
    }

    /**
     * Get projects for a user.
     */
    public List<Project> getProjectsByUser(User user) {
        // Get projects owned by the user
        List<Project> ownedProjects = projectRepository.findByOwner(user.getId());
        
        // Get projects from teams the user is a member of
        List<Project> teamProjects = getProjectsByTeamMember(user);
        
        // Combine and deduplicate
        Set<Project> allProjects = new HashSet<>(ownedProjects);
        allProjects.addAll(teamProjects);
        
        return new ArrayList<>(allProjects);
    }

    /**
     * Get projects for a team.
     */
    public List<Project> getProjectsByTeam(Team team) {
        return projectRepository.findByTeam(team);
    }

    /**
     * Get a project by ID.
     */
    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + id));
    }

    /**
     * Create a new project.
     */
    @Transactional
    public Project createProject(ProjectDTO projectDTO, User owner) {
        Team team = null;
        if (projectDTO.getTeamId() != null) {
            team = teamRepository.findById(projectDTO.getTeamId())
                    .orElseThrow(() -> new RuntimeException("Team not found with ID: " + projectDTO.getTeamId()));
            
            // Check if the user can create projects in this team (only team owners can create projects)
            if (!teamService.canUserCreateProjects(owner, team)) {
                throw new RuntimeException("Only team owners can create projects. You are not the owner of this team.");
            }
        }
        
        // Generate a webhook secret
        String webhookSecret = UUID.randomUUID().toString();
        
        Project project = Project.builder()
                .name(projectDTO.getName())
                .gitHubRepoUrl(projectDTO.getGitHubRepoUrl())
                .gitHubRepoId(projectDTO.getGitHubRepoId())
                .gitHubAccessToken(projectDTO.getGitHubAccessToken())
                .webhookSecret(webhookSecret)
                .owner(owner)
                .team(team)
                .build();
        
        return projectRepository.save(project);
    }

    /**
     * Update an existing project.
     */
    @Transactional
    public Project updateProject(Long id, ProjectDTO projectDTO) {
        Project existingProject = getProjectById(id);
        
        Team team = teamRepository.findById(projectDTO.getTeamId())
                .orElseThrow(() -> new RuntimeException("Team not found with ID: " + projectDTO.getTeamId()));
        
        existingProject.setName(projectDTO.getName());
        existingProject.setGitHubRepoUrl(projectDTO.getGitHubRepoUrl());
        
        if (projectDTO.getGitHubRepoId() != null) {
            existingProject.setGitHubRepoId(projectDTO.getGitHubRepoId());
        }
        
        // Handle GitHub access token updates - allow clearing the token
        if (projectDTO.getGitHubAccessToken() != null) {
            if (projectDTO.getGitHubAccessToken().isEmpty()) {
                // Clear the token if empty string is provided
                existingProject.setGitHubAccessToken(null);
            } else {
                // Update with new token
                existingProject.setGitHubAccessToken(projectDTO.getGitHubAccessToken());
            }
        }
        
        existingProject.setTeam(team);
        
        return projectRepository.save(existingProject);
    }

    /**
     * Change project team and preserve contribution history
     */
    @Transactional
    public Project changeProjectTeam(Long projectId, Long newTeamId, User currentUser) {
        Project project = getProjectById(projectId);
        Team newTeam = teamRepository.findById(newTeamId)
                .orElseThrow(() -> new RuntimeException("Team not found with ID: " + newTeamId));
        
        // Check if user is authorized (project owner or current team owner)
        boolean isProjectOwner = project.getOwner().equals(currentUser);
        boolean isCurrentTeamOwner = project.getTeam() != null && 
                                   project.getTeam().getOwner().equals(currentUser);
        boolean isNewTeamOwner = newTeam.getOwner().equals(currentUser);
        
        if (!isProjectOwner && !isCurrentTeamOwner && !isNewTeamOwner) {
            throw new RuntimeException("Only project owner or team owners can change project team");
        }
        
        Team oldTeam = project.getTeam();
        
        // Create team change history record
        if (oldTeam != null) {
            ProjectTeamHistory teamHistory = new ProjectTeamHistory();
            teamHistory.setProject(project);
            teamHistory.setPreviousTeam(oldTeam);
            teamHistory.setNewTeam(newTeam);
            teamHistory.setChangedBy(currentUser);
            teamHistory.setChangedAt(LocalDateTime.now());
            teamHistory.setChangeReason("Team changed by " + currentUser.getFullName());
            
            // Save team change history
            projectTeamHistoryRepository.save(teamHistory);
        }
        
        // Update project team
        project.setTeam(newTeam);
        project.setUpdatedAt(LocalDateTime.now());
        
        return projectRepository.save(project);
    }
    
    /**
     * Delete a project.
     */
    @Transactional
    public void deleteProject(Long id) {
        Project project = getProjectById(id);
        projectRepository.delete(project);
    }

    /**
     * Assign a team to a project.
     */
    @Transactional
    public Project assignTeamToProject(Long projectId, Long teamId, User currentUser) {
        Project project = getProjectById(projectId);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found with ID: " + teamId));
        
        // Check if current user is the project owner or team lead
        if (!project.getOwner().equals(currentUser) && !team.getOwner().equals(currentUser)) {
            throw new RuntimeException("You don't have permission to assign this team to the project");
        }
        
        project.setTeam(team);
        return projectRepository.save(project);
    }

    /**
     * Remove team assignment from a project.
     */
    @Transactional
    public Project removeTeamFromProject(Long projectId, User currentUser) {
        Project project = getProjectById(projectId);
        
        // Check if current user is the project owner
        if (!project.getOwner().equals(currentUser)) {
            throw new RuntimeException("You don't have permission to modify this project");
        }
        
        project.setTeam(null);
        return projectRepository.save(project);
    }

    /**
     * Get projects by owner.
     */
    public List<Project> getProjectsByOwner(User owner) {
        return projectRepository.findByOwner(owner.getId());
    }

    /**
     * Get projects by team member.
     */
    public List<Project> getProjectsByTeamMember(User user) {
        List<Project> teamProjects = new ArrayList<>();
        for (Team team : user.getTeams()) {
            teamProjects.addAll(projectRepository.findByTeam(team));
        }
        return teamProjects;
    }

    /**
     * Get team member contributions for a project.
     */
    public List<TeamMemberContributionDTO> getTeamMemberContributions(Long projectId) {
        Project project = getProjectById(projectId);
        
        if (project.getTeam() == null) {
            return new ArrayList<>();
        }
        
        List<User> teamMembers = new ArrayList<>(project.getTeam().getMembers());
        List<TeamMemberContributionDTO> contributions = new ArrayList<>();
        
        // Calculate total commits for percentage calculation
        int totalProjectCommits = project.getCommits().size();
        int totalProjectTasks = project.getTasks().size();
        
        for (User member : teamMembers) {
            // Count commits by this member
            int memberCommits = 0;
            for (Commit commit : project.getCommits()) {
                // Match by GitHub username or email
                if (isCommitByMember(commit, member)) {
                    memberCommits++;
                }
            }
            
            // Count tasks assigned to this member
            int memberTasks = 0;
            int completedTasks = 0;
            for (Task task : project.getTasks()) {
                if (task.getAssignee() != null && task.getAssignee().getId().equals(member.getId())) {
                    memberTasks++;
                    if (task.getStatus() == TaskStatus.DONE) {
                        completedTasks++;
                    }
                }
            }
            
            // Calculate contribution percentage based on commits + tasks
            double contributionPercentage = 0.0;
            if (totalProjectCommits > 0 || totalProjectTasks > 0) {
                double commitWeight = totalProjectCommits > 0 ? ((double) memberCommits / totalProjectCommits) * 60.0 : 0.0;
                double taskWeight = totalProjectTasks > 0 ? ((double) memberTasks / totalProjectTasks) * 40.0 : 0.0;
                contributionPercentage = commitWeight + taskWeight;
            }
            
            TeamMemberContributionDTO contribution = new TeamMemberContributionDTO(
                member.getId(),
                member.getNickname(),
                member.getGitHubId(),
                member.getAvatarUrl(),
                memberCommits,
                memberTasks,
                completedTasks,
                contributionPercentage
            );
            
            contributions.add(contribution);
        }
        
        // Sort by contribution percentage (highest first)
        contributions.sort((a, b) -> Double.compare(b.getContributionPercentage(), a.getContributionPercentage()));
        
        return contributions;
    }
    
    /**
     * Check if a commit is made by a specific team member.
     */
    private boolean isCommitByMember(Commit commit, User member) {
        // Match by GitHub username
        if (member.getGitHubId() != null && commit.getAuthorName() != null) {
            if (commit.getAuthorName().equalsIgnoreCase(member.getGitHubId())) {
                return true;
            }
        }
        
        // Match by email
        if (member.getEmail() != null && commit.getAuthorEmail() != null) {
            if (commit.getAuthorEmail().equalsIgnoreCase(member.getEmail())) {
                return true;
            }
        }
        
        // Match by display name
        if (member.getNickname() != null && commit.getAuthorName() != null) {
            if (commit.getAuthorName().equalsIgnoreCase(member.getNickname())) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Get project team change history.
     */
    public List<ProjectTeamHistory> getProjectTeamHistory(Long projectId) {
        return projectTeamHistoryRepository.findByProjectIdOrderByChangedAtDesc(projectId);
    }
}
