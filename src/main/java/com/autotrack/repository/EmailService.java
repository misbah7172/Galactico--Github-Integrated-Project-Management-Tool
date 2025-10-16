package com.autotrack.service;

import com.autotrack.model.Team;
import com.autotrack.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Service for handling email notifications.
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    public EmailService(JavaMailSender javaMailSender, TemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Send team invitation email to a new member.
     */
    @Async
    public void sendTeamInvitationEmail(String email, Team team, User invitedBy) {
        try {
            logger.info("Sending team invitation email to: {}", email);
            
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("You've been invited to join " + team.getName() + " on AutoTrack");

            // Create template context
            Context context = new Context();
            context.setVariable("teamName", team.getName());
            context.setVariable("invitedBy", invitedBy.getNickname());
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("loginUrl", baseUrl + "/login");

            // Process the template
            String htmlContent = templateEngine.process("email/team-invitation", context);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
            logger.info("Team invitation email sent successfully to: {}", email);
            
        } catch (MessagingException e) {
            logger.error("Failed to send team invitation email to: " + email, e);
        } catch (Exception e) {
            logger.error("Unexpected error sending team invitation email to: " + email, e);
        }
    }

    /**
     * Send task assignment notification email.
     */
    @Async
    public void sendTaskAssignmentEmail(User assignee, String taskTitle, String taskDescription, Team team) {
        try {
            if (assignee.getEmail() == null || assignee.getEmail().isEmpty()) {
                logger.warn("Cannot send task assignment email - user {} has no email", assignee.getNickname());
                return;
            }

            logger.info("Sending task assignment email to: {}", assignee.getEmail());
            
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(assignee.getEmail());
            helper.setSubject("New Task Assigned: " + taskTitle);

            // Create template context
            Context context = new Context();
            context.setVariable("assigneeName", assignee.getNickname());
            context.setVariable("taskTitle", taskTitle);
            context.setVariable("taskDescription", taskDescription);
            context.setVariable("teamName", team.getName());
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("tasksUrl", baseUrl + "/tasks");

            // Process the template
            String htmlContent = templateEngine.process("email/task-assignment", context);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
            logger.info("Task assignment email sent successfully to: {}", assignee.getEmail());
            
        } catch (MessagingException e) {
            logger.error("Failed to send task assignment email to: " + assignee.getEmail(), e);
        } catch (Exception e) {
            logger.error("Unexpected error sending task assignment email to: " + assignee.getEmail(), e);
        }
    }

    /**
     * Send welcome email to new team member after they join.
     */
    @Async
    public void sendWelcomeEmail(User newMember, Team team) {
        try {
            if (newMember.getEmail() == null || newMember.getEmail().isEmpty()) {
                logger.warn("Cannot send welcome email - user {} has no email", newMember.getNickname());
                return;
            }

            logger.info("Sending welcome email to: {}", newMember.getEmail());
            
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(newMember.getEmail());
            helper.setSubject("Welcome to " + team.getName() + " on AutoTrack!");

            // Create template context
            Context context = new Context();
            context.setVariable("memberName", newMember.getNickname());
            context.setVariable("teamName", team.getName());
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("dashboardUrl", baseUrl + "/dashboard");
            context.setVariable("tasksUrl", baseUrl + "/tasks");

            // Process the template
            String htmlContent = templateEngine.process("email/welcome", context);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
            logger.info("Welcome email sent successfully to: {}", newMember.getEmail());
            
        } catch (MessagingException e) {
            logger.error("Failed to send welcome email to: " + newMember.getEmail(), e);
        } catch (Exception e) {
            logger.error("Unexpected error sending welcome email to: " + newMember.getEmail(), e);
        }
    }
}
