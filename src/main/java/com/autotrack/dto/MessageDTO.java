package com.autotrack.dto;

import com.autotrack.model.Message;
import com.autotrack.model.MessageReaction;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DTO for Message entity to avoid circular references in JSON serialization.
 */
public class MessageDTO {
    private Long id;
    private String content;
    private UserDTO sender;
    private Long teamId;
    private LocalDateTime createdAt;
    private Map<String, List<UserDTO>> reactions;

    // Default constructor
    public MessageDTO() {}    // Constructor from Message entity
    public MessageDTO(Message message) {
        this.id = message.getId();
        this.content = message.getContent();
        this.sender = new UserDTO(message.getSender());
        this.teamId = message.getTeam().getId();
        this.createdAt = message.getCreatedAt();
        
        // Group reactions by emoji - handle null reactions
        if (message.getReactions() != null) {
            this.reactions = message.getReactions().stream()
                .collect(Collectors.groupingBy(
                    MessageReaction::getEmoji,
                    Collectors.mapping(
                        reaction -> new UserDTO(reaction.getUser()),
                        Collectors.toList()
                    )
                ));
        } else {
            this.reactions = new HashMap<>();
        }
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UserDTO getSender() {
        return sender;
    }

    public void setSender(UserDTO sender) {
        this.sender = sender;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, List<UserDTO>> getReactions() {
        return reactions;
    }

    public void setReactions(Map<String, List<UserDTO>> reactions) {
        this.reactions = reactions;
    }

    /**
     * Simple UserDTO for Message sender information.
     */
    public static class UserDTO {
        private Long id;
        private String nickname;
        private String avatarUrl;

        public UserDTO() {}

        public UserDTO(com.autotrack.model.User user) {
            this.id = user.getId();
            this.nickname = user.getNickname();
            this.avatarUrl = user.getAvatarUrl();
        }

        // Getters and setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }
    }
}
