package com.autotrack.controller;

import com.autotrack.model.Commit;
import com.autotrack.model.Project;
import com.autotrack.repository.CommitRepository;
import com.autotrack.repository.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Test controller for manually adding test commits
 */
@RestController
@RequestMapping("/api/test-commits")
public class TestCommitController {

    private final CommitRepository commitRepository;
    private final ProjectRepository projectRepository;

    public TestCommitController(CommitRepository commitRepository, ProjectRepository projectRepository) {
        this.commitRepository = commitRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Add a test commit to see if analytics work
     */
    @PostMapping("/project/{projectId}/add-test-commit")
    public ResponseEntity<Map<String, Object>> addTestCommit(@PathVariable Long projectId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Project> projectOpt = projectRepository.findById(projectId);
            if (projectOpt.isEmpty()) {
                result.put("error", "Project not found");
                return ResponseEntity.notFound().build();
            }
            
            Project project = projectOpt.get();
            
            // Create a test commit
            Commit testCommit = new Commit();
            testCommit.setSha("test-commit-" + System.currentTimeMillis());
            testCommit.setMessage("Test commit for analytics verification");
            testCommit.setAuthorName("Test User");
            testCommit.setAuthorEmail("test@example.com");
            testCommit.setCommittedAt(LocalDateTime.now());
            testCommit.setGitHubUrl("https://github.com/test/test");
            testCommit.setLinesAdded(50);
            testCommit.setLinesModified(10);
            testCommit.setLinesDeleted(5);
            testCommit.setFilesChanged(3);
            testCommit.setProject(project);
            
            Commit savedCommit = commitRepository.save(testCommit);
            
            result.put("success", true);
            result.put("message", "Test commit added successfully");
            result.put("commitId", savedCommit.getId());
            result.put("projectId", projectId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
