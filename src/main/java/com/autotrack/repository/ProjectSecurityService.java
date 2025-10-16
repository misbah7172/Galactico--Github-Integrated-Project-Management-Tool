package com.autotrack.service;

import com.autotrack.model.Project;
import com.autotrack.model.User;
import com.autotrack.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for handling project-level security checks
 */
@Service
public class ProjectSecurityService {
    
    private final ProjectRepository projectRepository;
    private final UserService userService;
    
    @Autowired
    public ProjectSecurityService(ProjectRepository projectRepository, UserService userService) {
        this.projectRepository = projectRepository;
        this.userService = userService;
    }
    
    /**
     * Check if a user can access a project
     * For now, we'll allow access if the user is authenticated and the project exists
     * In a more complex scenario, you might check team membership, ownership, etc.
     */
    public boolean canAccessProject(Long projectId, String username) {
        if (projectId == null || username == null) {
            return false;
        }
        
        try {
            // Check if project exists
            Optional<Project> projectOpt = projectRepository.findById(projectId);
            if (projectOpt.isEmpty()) {
                return false;
            }
            
            Project project = projectOpt.get();
            
            // Get current user
            User currentUser = userService.getUserByNickname(username);
            if (currentUser == null) {
                return false;
            }
            
            // Check if user is project owner
            if (project.getOwner() != null && project.getOwner().getId().equals(currentUser.getId())) {
                return true;
            }
            
            // Check if user is team member
            if (project.getTeam() != null && project.getTeam().getMembers().contains(currentUser)) {
                return true;
            }
            
            // For now, allow access for authenticated users
            // In production, you might want stricter controls
            return true;
            
        } catch (Exception e) {
            // Log error and deny access on any exception
            return false;
        }
    }
}
