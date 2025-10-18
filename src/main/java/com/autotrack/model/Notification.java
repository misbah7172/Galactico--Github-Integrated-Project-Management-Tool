package com.autotrack.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing a notification.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne
    @JoinColumn(name = "commit_review_id")
    private CommitReview commitReview;

    @ManyToOne
    @JoinColumn(name = "team_invitation_id")
    private TeamInvitation teamInvitation;
    
    @Column(nullable = false)
    private String message;
    
    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType = NotificationType.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private NotificationStatus status;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public Notification() {}

    public Notification(Long id, User user, Task task, String message, boolean read, 
                       NotificationType notificationType, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.task = task;
        this.message = message;
        this.read = read;
        this.notificationType = notificationType != null ? notificationType : NotificationType.GENERAL;
        this.createdAt = createdAt;
    }

    public Notification(User user, String message, NotificationType notificationType) {
        this.user = user;
        this.message = message;
        this.notificationType = notificationType != null ? notificationType : NotificationType.GENERAL;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public CommitReview getCommitReview() { return commitReview; }
    public void setCommitReview(CommitReview commitReview) { this.commitReview = commitReview; }

    public TeamInvitation getTeamInvitation() { return teamInvitation; }
    public void setTeamInvitation(TeamInvitation teamInvitation) { this.teamInvitation = teamInvitation; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public NotificationType getNotificationType() { return notificationType; }
    public void setNotificationType(NotificationType notificationType) { 
        this.notificationType = notificationType != null ? notificationType : NotificationType.GENERAL; 
    }

    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Builder pattern
    public static NotificationBuilder builder() {
        return new NotificationBuilder();
    }

    public static class NotificationBuilder {
        private Long id;
        private User user;
        private Task task;
        private CommitReview commitReview;
        private TeamInvitation teamInvitation;
        private String message;
        private boolean read;
        private NotificationType notificationType = NotificationType.GENERAL;
        private NotificationStatus status;
        private LocalDateTime createdAt;

        public NotificationBuilder id(Long id) { this.id = id; return this; }
        public NotificationBuilder user(User user) { this.user = user; return this; }
        public NotificationBuilder task(Task task) { this.task = task; return this; }
        public NotificationBuilder commitReview(CommitReview commitReview) { this.commitReview = commitReview; return this; }
        public NotificationBuilder teamInvitation(TeamInvitation teamInvitation) { this.teamInvitation = teamInvitation; return this; }
        public NotificationBuilder message(String message) { this.message = message; return this; }
        public NotificationBuilder read(boolean read) { this.read = read; return this; }
        public NotificationBuilder notificationType(NotificationType notificationType) { this.notificationType = notificationType; return this; }
        public NotificationBuilder status(NotificationStatus status) { this.status = status; return this; }
        public NotificationBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Notification build() {
            Notification notification = new Notification(id, user, task, message, read, notificationType, createdAt);
            notification.setCommitReview(commitReview);
            notification.setTeamInvitation(teamInvitation);
            notification.setStatus(status);
            return notification;
        }
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum NotificationType {
        GENERAL, TEAM_INVITATION, COMMIT_REVIEW, TASK_ASSIGNMENT, PROJECT_UPDATE
    }

    public enum NotificationStatus {
        PENDING, ACCEPTED, REJECTED, COMPLETED, CANCELLED
    }
}
