package com.autotrack.controller;

import com.autotrack.dto.MessageDTO;
import com.autotrack.model.Message;
import com.autotrack.model.User;
import com.autotrack.service.MessageService;
import com.autotrack.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teams/{teamId}/messages")
public class MessageController {    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;
    
    @ResponseBody
    public ResponseEntity<?> sendMessage(@PathVariable Long teamId, 
                                       @RequestParam String content,
                                       @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Authentication required");
        }
          try {
            User currentUser = userService.getCurrentUser(principal);
            messageService.sendMessage(content, currentUser, teamId);
            // Just return success status, not the full message DTO to avoid serialization issues
            return ResponseEntity.ok().body("Message sent successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }@GetMapping("/recent")
    @ResponseBody
    public ResponseEntity<?> getRecentMessages(@PathVariable Long teamId, @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Authentication required");
        }
        
        try {
            User currentUser = userService.getCurrentUser(principal);
            List<Message> messages = messageService.getRecentTeamMessages(teamId, currentUser);
            List<MessageDTO> messageDTOs = messages.stream()
                    .map(MessageDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(messageDTOs);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @DeleteMapping("/{messageId}/delete")
    @ResponseBody
    public ResponseEntity<?> deleteMessage(@PathVariable Long teamId, 
                                         @PathVariable Long messageId,
                                         @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Authentication required");
        }
        
        try {
            User currentUser = userService.getCurrentUser(principal);
            messageService.deleteMessage(messageId, currentUser);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
