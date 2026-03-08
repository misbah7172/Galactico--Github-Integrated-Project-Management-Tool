package com.autotrack.service;

import com.autotrack.model.Message;
import com.autotrack.model.Team;
import com.autotrack.model.User;
import com.autotrack.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private TeamService teamService;

    public Message sendMessage(String content, User sender, Long teamId) {
        Team team = teamService.findById(teamId);
        
        // Check if user is a member of the team
        if (!teamService.isUserMemberOfTeam(sender, team)) {
            throw new RuntimeException("User is not a member of this team");
        }
        
        Message message = new Message(content, sender, team);
        return messageRepository.save(message);
    }    public List<Message> getTeamMessages(Long teamId, User user) {
        Team team = teamService.findById(teamId);
        
        // Check if user is a member of the team
        if (!teamService.isUserMemberOfTeam(user, team)) {
            throw new RuntimeException("User is not a member of this team");
        }
        
        return messageRepository.findByTeamIdWithReactionsOrderByCreatedAtDesc(teamId);
    }

    public List<Message> getRecentTeamMessages(Long teamId, User user) {
        Team team = teamService.findById(teamId);
        
        // Check if user is a member of the team
        if (!teamService.isUserMemberOfTeam(user, team)) {
            throw new RuntimeException("User is not a member of this team");
        }
        
        List<Message> messages = messageRepository.findByTeamIdWithReactionsOrderByCreatedAtDesc(teamId);
        // Return latest 50 messages
        return messages.size() > 50 ? messages.subList(0, 50) : messages;
    }
    
    public Message getMessageById(Long messageId) {
        return messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found"));
    }
    
    public void deleteMessage(Long messageId, User user) {
        Message message = getMessageById(messageId);
        
        // Check if user is the sender of the message
        if (!message.getSender().getId().equals(user.getId())) {
            throw new RuntimeException("You can only delete your own messages");
        }
        
        messageRepository.delete(message);
    }
}
