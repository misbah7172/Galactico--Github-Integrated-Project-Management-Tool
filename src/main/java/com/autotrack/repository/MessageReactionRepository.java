package com.autotrack.repository;

import com.autotrack.model.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {
    
    List<MessageReaction> findByMessageId(Long messageId);
    
    @Query("SELECT mr FROM MessageReaction mr WHERE mr.message.id = :messageId AND mr.user.id = :userId AND mr.emoji = :emoji")
    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(@Param("messageId") Long messageId, 
                                                              @Param("userId") Long userId, 
                                                              @Param("emoji") String emoji);
    
    void deleteByMessageId(Long messageId);
}
