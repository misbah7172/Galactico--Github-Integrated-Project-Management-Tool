package com.autotrack.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing a GitHub commit.
 */
@Entity
@Table(name = "commits")
public class Commit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String sha;
    
    @Column(nullable = false)
    private String message;
    
    @Column(name = "author_name")
    private String authorName;
    
    @Column(name = "author_email")
    private String authorEmail;
    
    @Column(name = "committed_at")
    private LocalDateTime committedAt;
    
    @Column(name = "github_url")
    private String gitHubUrl;
    
    @Column(name = "lines_added")
    private Integer linesAdded = 0;
    
    @Column(name = "lines_modified")
    private Integer linesModified = 0;
    
    @Column(name = "lines_deleted")
    private Integer linesDeleted = 0;
    
    @Column(name = "files_changed")
    private Integer filesChanged = 0;
    
    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;
    
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Constructors
    public Commit() {}

    public Commit(Long id, String sha, String message, String authorName, String authorEmail, 
                  LocalDateTime committedAt, String gitHubUrl, Integer linesAdded, Integer linesModified, 
                  Integer linesDeleted, Integer filesChanged, Task task, Project project) {
        this.id = id;
        this.sha = sha;
        this.message = message;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.committedAt = committedAt;
        this.gitHubUrl = gitHubUrl;
        this.linesAdded = linesAdded != null ? linesAdded : 0;
        this.linesModified = linesModified != null ? linesModified : 0;
        this.linesDeleted = linesDeleted != null ? linesDeleted : 0;
        this.filesChanged = filesChanged != null ? filesChanged : 0;
        this.task = task;
        this.project = project;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSha() { return sha; }
    public void setSha(String sha) { this.sha = sha; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }

    public LocalDateTime getCommittedAt() { return committedAt; }
    public void setCommittedAt(LocalDateTime committedAt) { this.committedAt = committedAt; }

    public String getGitHubUrl() { return gitHubUrl; }
    public void setGitHubUrl(String gitHubUrl) { this.gitHubUrl = gitHubUrl; }

    public Integer getLinesAdded() { return linesAdded; }
    public void setLinesAdded(Integer linesAdded) { this.linesAdded = linesAdded != null ? linesAdded : 0; }

    public Integer getLinesModified() { return linesModified; }
    public void setLinesModified(Integer linesModified) { this.linesModified = linesModified != null ? linesModified : 0; }

    public Integer getLinesDeleted() { return linesDeleted; }
    public void setLinesDeleted(Integer linesDeleted) { this.linesDeleted = linesDeleted != null ? linesDeleted : 0; }

    public Integer getFilesChanged() { return filesChanged; }
    public void setFilesChanged(Integer filesChanged) { this.filesChanged = filesChanged != null ? filesChanged : 0; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    // Builder pattern
    public static CommitBuilder builder() {
        return new CommitBuilder();
    }

    public static class CommitBuilder {
        private Long id;
        private String sha;
        private String message;
        private String authorName;
        private String authorEmail;
        private LocalDateTime committedAt;
        private String gitHubUrl;
        private Integer linesAdded = 0;
        private Integer linesModified = 0;
        private Integer linesDeleted = 0;
        private Integer filesChanged = 0;
        private Task task;
        private Project project;

        public CommitBuilder id(Long id) { this.id = id; return this; }
        public CommitBuilder sha(String sha) { this.sha = sha; return this; }
        public CommitBuilder message(String message) { this.message = message; return this; }
        public CommitBuilder authorName(String authorName) { this.authorName = authorName; return this; }
        public CommitBuilder authorEmail(String authorEmail) { this.authorEmail = authorEmail; return this; }
        public CommitBuilder committedAt(LocalDateTime committedAt) { this.committedAt = committedAt; return this; }
        public CommitBuilder gitHubUrl(String gitHubUrl) { this.gitHubUrl = gitHubUrl; return this; }
        public CommitBuilder linesAdded(Integer linesAdded) { this.linesAdded = linesAdded; return this; }
        public CommitBuilder linesModified(Integer linesModified) { this.linesModified = linesModified; return this; }
        public CommitBuilder linesDeleted(Integer linesDeleted) { this.linesDeleted = linesDeleted; return this; }
        public CommitBuilder filesChanged(Integer filesChanged) { this.filesChanged = filesChanged; return this; }
        public CommitBuilder task(Task task) { this.task = task; return this; }
        public CommitBuilder project(Project project) { this.project = project; return this; }

        public Commit build() {
            return new Commit(id, sha, message, authorName, authorEmail, committedAt, gitHubUrl, 
                            linesAdded, linesModified, linesDeleted, filesChanged, task, project);
        }
    }
}
