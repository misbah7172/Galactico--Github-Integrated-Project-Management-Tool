package com.autotrack.service;

import com.autotrack.model.Notification;
import com.autotrack.model.Task;
import com.autotrack.model.TaskStatus;
import com.autotrack.model.User;
import com.autotrack.repository.NotificationRepository;
import com.autotrack.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing notifications.
 * Implements observer pattern for task status changes.
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository, 
                              UserRepository userRepository,
                              EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Get all notifications for a user.
     */
    public List<Notification> getNotificationsByUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Get unread notifications for a user.
     */
    public List<Notification> getUnreadNotificationsByUser(User user) {
        return notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user);
    }

    /**
     * Count unread notifications for a user.
     */
    public long countUnreadNotifications(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }

    /**
     * Mark a notification as read.
     */
    @Transactional
    public void markNotificationAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    /**
     * Mark all notifications for a user as read.
     */
    @Transactional
    public void markAllNotificationsAsRead(User user) {
        List<Notification> unreadNotifications = notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user);
        unreadNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    /**
     * Create a notification for task creation.
     */
    @Async
    @Transactional
    public void notifyTaskCreated(Task task) {
        if (task.getAssignee() != null) {
            createNotification(
                task.getAssignee(),
                task,
                "You have been assigned to task: " + task.getTitle()
            );
        }
        
        // Notify team lead
        notifyTeamLead(task, "New task created: " + task.getTitle());
    }

    /**
     * Create a notification for task status change.
     */
    @Async
    @Transactional
    public void notifyTaskStatusChanged(Task task, TaskStatus oldStatus) {
        if (task.getAssignee() != null) {
            createNotification(
                task.getAssignee(),
                task,
                "Task status changed from " + oldStatus + " to " + task.getStatus() + ": " + task.getTitle()
            );
        }
        
        // Notify team lead if status changed to DONE
        if (task.getStatus() == TaskStatus.DONE) {
            notifyTeamLead(task, "Task completed: " + task.getTitle());
        }
    }

    /**
     * Create a notification for task assignment change.
     */
    @Async
    @Transactional
    public void notifyTaskAssigneeChanged(Task task, User oldAssignee) {
        if (task.getAssignee() != null) {
            createNotification(
                task.getAssignee(),
                task,
                "You have been assigned to task: " + task.getTitle()
            );
            
            // Send email notification for new task assignment
            if (task.getProject() != null && task.getProject().getTeam() != null) {
                emailService.sendTaskAssignmentEmail(
                    task.getAssignee(), 
                    task.getTitle(), 
                    task.getTitle(), // Using title as description since Task doesn't have description field
                    task.getProject().getTeam()
                );
            }
        }
        
        if (oldAssignee != null && (task.getAssignee() == null || !task.getAssignee().getId().equals(oldAssignee.getId()))) {
            createNotification(
                oldAssignee,
                task,
                "You have been unassigned from task: " + task.getTitle()
            );
        }
    }

    /**
     * Create a notification for task decline.
     */
    @Async
    @Transactional
    public void notifyTaskDeclined(Task task, User teamLeader) {
        if (task.getAssignee() != null) {
            String message = String.format(
                "Your task '%s' has been declined by %s. Reason: %s", 
                task.getTitle(), 
                teamLeader.getNickname(),
                task.getDeclineReason() != null ? task.getDeclineReason() : "Quality standards not met"
            );
            createNotification(task.getAssignee(), task, message);
        }
    }

    /**
     * Utility method to notify team lead.
     */
    private void notifyTeamLead(Task task, String message) {
        task.getProject().getTeam().getMembers().stream()
            .filter(member -> member.hasRole("TEAM_LEAD"))
            .forEach(teamLead -> createNotification(teamLead, task, message));
    }

    /**
     * Utility method to create a notification.
     */
    private void createNotification(User user, Task task, String message) {
        Notification notification = Notification.builder()
            .user(user)
            .task(task)
            .message(message)
            .read(false)
            .build();
        
        notificationRepository.save(notification);
    }
    
    /**
     * Public method to create a general notification.
     */
    public void createNotification(User user, String title, String message, String actionUrl) {
        Notification notification = Notification.builder()
            .user(user)
            .message(title + ": " + message)
            .read(false)
            .build();
        
        notificationRepository.save(notification);
    }

    /**
     * Create notification for team invitation.
     */
    public void createTeamInvitationNotification(User user, com.autotrack.model.TeamInvitation invitation) {
        String message = String.format("You have been invited to join team '%s' by %s", 
                invitation.getTeam().getName(), 
                invitation.getInviter().getNickname());
        
        Notification notification = Notification.builder()
            .user(user)
            .message(message)
            .read(false)
            .build();
        
        notificationRepository.save(notification);
    }

    /**
     * Create notification for invitation response.
     */
    public void createInvitationResponseNotification(User teamOwner, com.autotrack.model.TeamInvitation invitation, boolean accepted) {
        String status = accepted ? "accepted" : "rejected";
        String inviteeName = invitation.getInvitee() != null ? 
                invitation.getInvitee().getNickname() : 
                invitation.getInviteeGithubUrl().replace("https://github.com/", "");
        
        String message = String.format("%s has %s the invitation to join team '%s'", 
                inviteeName, status, invitation.getTeam().getName());
        
        Notification notification = Notification.builder()
            .user(teamOwner)
            .message(message)
            .read(false)
            .build();
        
        notificationRepository.save(notification);
    }

    /**
     * Send notification when a sprint starts.
     */
    @Async
    public void sendSprintStartNotification(com.autotrack.model.Sprint sprint) {
        String message = String.format("Sprint '%s' has started for project '%s'", 
                sprint.getName(), sprint.getProject().getName());
        
        // Notify all team members of the project
        List<User> projectMembers = userRepository.findByTeams_Projects_Id(sprint.getProject().getId());
        
        for (User member : projectMembers) {
            Notification notification = Notification.builder()
                .user(member)
                .message(message)
                .read(false)
                .build();
            
            notificationRepository.save(notification);
        }
    }

    /**
     * Send notification when a sprint is completed.
     */
    @Async
    public void sendSprintCompletionNotification(com.autotrack.model.Sprint sprint) {
        String message = String.format("Sprint '%s' has been completed for project '%s'", 
                sprint.getName(), sprint.getProject().getName());
        
        List<User> projectMembers = userRepository.findByTeams_Projects_Id(sprint.getProject().getId());
        
        for (User member : projectMembers) {
            Notification notification = Notification.builder()
                .user(member)
                .message(message)
                .read(false)
                .build();
            
            notificationRepository.save(notification);
        }
    }

    /**
     * Send reminder notification for sprints ending soon with incomplete tasks.
     */
    @Async
    public void sendSprintEndingReminderNotification(com.autotrack.model.Sprint sprint, 
                                                   com.autotrack.dto.SprintProgressDto progress) {
        String message = String.format("Sprint '%s' is ending soon with %.1f%% completion. " +
                "Please review remaining tasks.", 
                sprint.getName(), progress.getCompletionPercentage());
        
        List<User> projectMembers = userRepository.findByTeams_Projects_Id(sprint.getProject().getId());
        
        for (User member : projectMembers) {
            Notification notification = Notification.builder()
                .user(member)
                .message(message)
                .read(false)
                .build();
            
            notificationRepository.save(notification);
        }
    }
    
    // Backlog-related notification methods
    
    /**
     * Notify when a backlog item is assigned to a user
     */
    @Async
    @Transactional
    public void notifyBacklogItemAssigned(com.autotrack.model.BacklogItem backlogItem) {
        if (backlogItem.getAssignedTo() != null) {
            String message = String.format("You have been assigned backlog item: %s (Priority: %s)", 
                backlogItem.getTitle(), backlogItem.getPriorityLevel());
            
            Notification notification = Notification.builder()
                .user(backlogItem.getAssignedTo())
                .message(message)
                .read(false)
                .build();
            
            notificationRepository.save(notification);
            
            logger.info("Sent backlog assignment notification to user: {}", backlogItem.getAssignedTo().getNickname());
        }
    }
    
    /**
     * Notify when a backlog item's priority is changed
     */
    @Async
    @Transactional
    public void notifyBacklogItemPriorityChanged(com.autotrack.model.BacklogItem backlogItem, 
                                               com.autotrack.model.PriorityLevel oldPriority) {
        if (backlogItem.getAssignedTo() != null) {
            String message = String.format("Priority changed for backlog item '%s' from %s to %s", 
                backlogItem.getTitle(), oldPriority, backlogItem.getPriorityLevel());
            
            Notification notification = Notification.builder()
                .user(backlogItem.getAssignedTo())
                .message(message)
                .read(false)
                .build();
            
            notificationRepository.save(notification);
            
            logger.info("Sent priority change notification for backlog item: {}", backlogItem.getTitle());
        }
    }
    
    /**
     * Notify when a backlog item is moved to a sprint
     */
    @Async
    @Transactional
    public void notifyBacklogItemMovedToSprint(com.autotrack.model.BacklogItem backlogItem, 
                                             com.autotrack.model.Sprint sprint) {
        if (backlogItem.getAssignedTo() != null) {
            String message = String.format("Backlog item '%s' has been moved to sprint '%s'", 
                backlogItem.getTitle(), sprint.getName());
            
            Notification notification = Notification.builder()
                .user(backlogItem.getAssignedTo())
                .message(message)
                .read(false)
                .build();
            
            notificationRepository.save(notification);
            
            logger.info("Sent sprint assignment notification for backlog item: {}", backlogItem.getTitle());
        }
    }
    
    /**
     * Notify when a backlog item is removed from a sprint
     */
    @Async
    @Transactional
    public void notifyBacklogItemRemovedFromSprint(com.autotrack.model.BacklogItem backlogItem, 
                                                 com.autotrack.model.Sprint sprint) {
        if (backlogItem.getAssignedTo() != null) {
            String message = String.format("Backlog item '%s' has been removed from sprint '%s'", 
                backlogItem.getTitle(), sprint.getName());
            
            Notification notification = Notification.builder()
                .user(backlogItem.getAssignedTo())
                .message(message)
                .read(false)
                .build();
            
            notificationRepository.save(notification);
            
            logger.info("Sent sprint removal notification for backlog item: {}", backlogItem.getTitle());
        }
    }
    
    /**
     * Notify when a backlog item is completed
     */
    @Async
    @Transactional
    public void notifyBacklogItemCompleted(com.autotrack.model.BacklogItem backlogItem) {
        if (backlogItem.getAssignedTo() != null) {
            String message = String.format("Backlog item completed: %s (Story Points: %s)", 
                backlogItem.getTitle(), backlogItem.getStoryPoints());
            
            Notification notification = Notification.builder()
                .user(backlogItem.getAssignedTo())
                .message(message)
                .read(false)
                .build();
            
            notificationRepository.save(notification);
            
            // Also notify project manager or team lead
            if (backlogItem.getProject() != null && backlogItem.getProject().getTeam() != null) {
                notifyTeamLead(backlogItem.getProject(), 
                    String.format("Backlog item '%s' was completed by %s", 
                        backlogItem.getTitle(), 
                        backlogItem.getAssignedTo().getNickname()));
            }
            
            logger.info("Sent completion notification for backlog item: {}", backlogItem.getTitle());
        }
    }
    
    /**
     * Helper method to notify team lead about project events
     */
    private void notifyTeamLead(com.autotrack.model.Project project, String message) {
        // This is a simplified implementation - you might want to add a team lead field to Project
        // For now, we'll notify all team members with admin privileges
        List<User> teamMembers = userRepository.findByTeams_Projects_Id(project.getId());
        
        for (User member : teamMembers) {
            // You might want to add a role field to determine team leads
            Notification notification = Notification.builder()
                .user(member)
                .message("[Team Lead] " + message)
                .read(false)
                .build();
            
            notificationRepository.save(notification);
        }
    }
    
    // Commit Review notification methods
    
    /**
     * Create notification for commit review submission.
     */
    @Async
    @Transactional
    public void createCommitReviewNotification(com.autotrack.model.CommitReview commitReview, String message) {
        try {
            // Create notification for the reviewer
            Notification notification = Notification.builder()
                .user(commitReview.getReviewer())
                .message(message)
                .read(false)
                .notificationType(com.autotrack.model.Notification.NotificationType.COMMIT_REVIEW)
                .commitReview(commitReview)
                .build();
            
            notificationRepository.save(notification);
            
            logger.info("Created commit review notification for reviewer: {}", commitReview.getReviewer().getNickname());
            
        } catch (Exception e) {
            logger.error("Error creating commit review notification: {}", e.getMessage());
        }
    }
    
    /**
     * Create notification when commit is approved.
     */
    @Async
    @Transactional
    public void notifyCommitApproved(com.autotrack.model.CommitReview commitReview) {
        try {
            // Find the commit author and create notification
            String authorEmail = commitReview.getCommit().getAuthorEmail();
            
            // Try to find user by email
            userRepository.findByEmail(authorEmail).ifPresent(author -> {
                String message = String.format("Your commit '%s' has been approved by %s", 
                    truncateCommitMessage(commitReview.getCommit().getMessage()), 
                    commitReview.getReviewer().getNickname());
                
                Notification notification = Notification.builder()
                    .user(author)
                    .message(message)
                    .read(false)
                    .notificationType(com.autotrack.model.Notification.NotificationType.COMMIT_REVIEW)
                    .commitReview(commitReview)
                    .status(com.autotrack.model.Notification.NotificationStatus.ACCEPTED)
                    .build();
                
                notificationRepository.save(notification);
                
                logger.info("Created commit approval notification for author: {}", author.getNickname());
            });
            
        } catch (Exception e) {
            logger.error("Error creating commit approval notification: {}", e.getMessage());
        }
    }
    
    /**
     * Create notification when commit is rejected.
     */
    @Async
    @Transactional
    public void notifyCommitRejected(com.autotrack.model.CommitReview commitReview) {
        try {
            // Find the commit author and create notification
            String authorEmail = commitReview.getCommit().getAuthorEmail();
            
            // Try to find user by email
            userRepository.findByEmail(authorEmail).ifPresent(author -> {
                String message = String.format("Your commit '%s' has been rejected by %s", 
                    truncateCommitMessage(commitReview.getCommit().getMessage()), 
                    commitReview.getReviewer().getNickname());
                
                if (commitReview.getComments() != null && !commitReview.getComments().trim().isEmpty()) {
                    message += ". Comments: " + truncateComments(commitReview.getComments());
                }
                
                Notification notification = Notification.builder()
                    .user(author)
                    .message(message)
                    .read(false)
                    .notificationType(com.autotrack.model.Notification.NotificationType.COMMIT_REVIEW)
                    .commitReview(commitReview)
                    .status(com.autotrack.model.Notification.NotificationStatus.REJECTED)
                    .build();
                
                notificationRepository.save(notification);
                
                logger.info("Created commit rejection notification for author: {}", author.getNickname());
            });
            
        } catch (Exception e) {
            logger.error("Error creating commit rejection notification: {}", e.getMessage());
        }
    }
    
    /**
     * Update team invitation notification status.
     */
    @Transactional
    public void updateTeamInvitationNotificationStatus(com.autotrack.model.TeamInvitation invitation, 
                                                     com.autotrack.model.Notification.NotificationStatus status) {
        try {
            // Find notifications related to this invitation and update their status
            List<Notification> notifications = notificationRepository.findByTeamInvitation(invitation);
            
            for (Notification notification : notifications) {
                notification.setStatus(status);
                notificationRepository.save(notification);
            }
            
            logger.info("Updated {} team invitation notification(s) to status: {}", notifications.size(), status);
            
        } catch (Exception e) {
            logger.error("Error updating team invitation notification status: {}", e.getMessage());
        }
    }
    
    /**
     * Create notification with full status and type support.
     */
    public void createNotificationWithStatus(User user, String message, 
                                           com.autotrack.model.Notification.NotificationType type,
                                           com.autotrack.model.Notification.NotificationStatus status) {
        try {
            Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .read(false)
                .notificationType(type)
                .status(status)
                .build();
            
            notificationRepository.save(notification);
            
            logger.info("Created notification for user: {} with type: {} and status: {}", 
                       user.getNickname(), type, status);
            
        } catch (Exception e) {
            logger.error("Error creating notification with status: {}", e.getMessage());
        }
    }
    
    /**
     * Helper method to truncate commit message for notifications.
     */
    private String truncateCommitMessage(String message) {
        if (message == null) return "Unknown commit";
        return message.length() > 50 ? message.substring(0, 50) + "..." : message;
    }
    
    /**
     * Helper method to truncate comments for notifications.
     */
    private String truncateComments(String comments) {
        if (comments == null) return "";
        return comments.length() > 100 ? comments.substring(0, 100) + "..." : comments;
    }
}
