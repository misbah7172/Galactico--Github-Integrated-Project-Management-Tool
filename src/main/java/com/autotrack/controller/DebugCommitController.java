package com.autotrack.controller;

import com.autotrack.model.Commit;
import com.autotrack.model.Project;
import com.autotrack.repository.CommitRepository;
import com.autotrack.repository.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Debug controller for checking commit data
 */
@RestController
@RequestMapping("/api/debug-commits")
public class DebugCommitController {

    private final CommitRepository commitRepository;
    private final ProjectRepository projectRepository;

    public DebugCommitController(CommitRepository commitRepository, ProjectRepository projectRepository) {
        this.commitRepository = commitRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Debug endpoint to check commits for a project
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<Map<String, Object>> debugProjectCommits(@PathVariable Long projectId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Project> projectOpt = projectRepository.findById(projectId);
            if (projectOpt.isEmpty()) {
                result.put("error", "Project not found");
                return ResponseEntity.notFound().build();
            }
            
            Project project = projectOpt.get();
            List<Commit> commits = commitRepository.findByProject(project);
            
            result.put("projectId", projectId);
            result.put("projectName", project.getName());
            result.put("totalCommits", commits.size());
            result.put("commits", commits.stream().map(commit -> {
                Map<String, Object> commitInfo = new HashMap<>();
                commitInfo.put("id", commit.getId());
                commitInfo.put("sha", commit.getSha());
                commitInfo.put("message", commit.getMessage());
                commitInfo.put("authorName", commit.getAuthorName());
                commitInfo.put("authorEmail", commit.getAuthorEmail());
                commitInfo.put("linesAdded", commit.getLinesAdded());
                commitInfo.put("linesModified", commit.getLinesModified());
                commitInfo.put("linesDeleted", commit.getLinesDeleted());
                commitInfo.put("filesChanged", commit.getFilesChanged());
                commitInfo.put("committedAt", commit.getCommittedAt());
                return commitInfo;
            }).toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("stackTrace", e.getStackTrace());
            return ResponseEntity.status(500).body(result);
        }
    }
}
