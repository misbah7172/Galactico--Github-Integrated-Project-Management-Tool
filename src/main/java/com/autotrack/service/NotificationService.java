package com.autotrack.service;

import com.autotrack.model.Notification;
import com.autotrack.model.Task;
import com.autotrack.model.TaskStatus;
import com.autotrack.model.User;
import com.autotrack.repository.NotificationRepository;
import com.autotrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing notifications.
 * Implements observer pattern for task status changes.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Optional<EmailService> emailService;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository, 
                              UserRepository userRepository,
                              Optional<EmailService> emailService) {
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
            if (task.getProject() != null && task.getProject().getTeam() != null && emailService.isPresent()) {
                emailService.get().sendTaskAssignmentEmail(
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
}
