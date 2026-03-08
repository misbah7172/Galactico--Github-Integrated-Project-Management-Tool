package com.autotrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_change_metrics")
public class FileChangeMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "commit_id", nullable = false)
    private Commit commit;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_extension")
    private String fileExtension;

    @Column(name = "lines_added")
    private Integer linesAdded = 0;

    @Column(name = "lines_modified")
    private Integer linesModified = 0;

    @Column(name = "lines_deleted")
    private Integer linesDeleted = 0;

    @Column(name = "change_type")
    @Enumerated(EnumType.STRING)
    private ChangeType changeType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public FileChangeMetrics() {
        this.createdAt = LocalDateTime.now();
    }

    public FileChangeMetrics(Commit commit, Project project, String filePath, String fileName) {
        this();
        this.commit = commit;
        this.project = project;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileExtension = extractFileExtension(fileName);
    }

    private String extractFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Commit getCommit() { return commit; }
    public void setCommit(Commit commit) { this.commit = commit; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { 
        this.fileName = fileName; 
        this.fileExtension = extractFileExtension(fileName);
    }

    public String getFileExtension() { return fileExtension; }
    public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }

    public Integer getLinesAdded() { return linesAdded; }
    public void setLinesAdded(Integer linesAdded) { this.linesAdded = linesAdded != null ? linesAdded : 0; }

    public Integer getLinesModified() { return linesModified; }
    public void setLinesModified(Integer linesModified) { this.linesModified = linesModified != null ? linesModified : 0; }

    public Integer getLinesDeleted() { return linesDeleted; }
    public void setLinesDeleted(Integer linesDeleted) { this.linesDeleted = linesDeleted != null ? linesDeleted : 0; }

    public ChangeType getChangeType() { return changeType; }
    public void setChangeType(ChangeType changeType) { this.changeType = changeType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getTotalChanges() {
        return (linesAdded != null ? linesAdded : 0) + 
               (linesModified != null ? linesModified : 0) + 
               (linesDeleted != null ? linesDeleted : 0);
    }

    public enum ChangeType {
        ADDED, MODIFIED, DELETED, RENAMED
    }
}