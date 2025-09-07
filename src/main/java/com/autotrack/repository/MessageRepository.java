package com.autotrack.repository;

import com.autotrack.model.Message;
import com.autotrack.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    List<Message> findByTeamOrderByCreatedAtDesc(Team team);
    
    @Query("SELECT m FROM Message m WHERE m.team = :team ORDER BY m.createdAt DESC")
    List<Message> findRecentMessagesByTeam(@Param("team") Team team);
    
    @Query("SELECT m FROM Message m WHERE m.team.id = :teamId ORDER BY m.createdAt DESC")
    List<Message> findByTeamIdOrderByCreatedAtDesc(@Param("teamId") Long teamId);
    
    @Query("SELECT DISTINCT m FROM Message m LEFT JOIN FETCH m.reactions WHERE m.team.id = :teamId ORDER BY m.createdAt DESC")
    List<Message> findByTeamIdWithReactionsOrderByCreatedAtDesc(@Param("teamId") Long teamId);
}
