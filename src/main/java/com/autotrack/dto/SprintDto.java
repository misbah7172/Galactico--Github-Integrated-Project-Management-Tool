package com.autotrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Sprint
 */
public class SprintDto {
    
    @NotBlank(message = "Sprint name is required")
    @Size(min = 1, max = 100, message = "Sprint name must be between 1 and 100 characters")
    private String name;
    
    @Size(max = 500, message = "Sprint goal must not exceed 500 characters")
    private String goal;
    
    private LocalDateTime startDate;
    
    private LocalDateTime endDate;
    
    private Integer plannedVelocity;
    
    // Constructors
    public SprintDto() {}
    
    public SprintDto(String name, String goal, LocalDateTime startDate, LocalDateTime endDate) {
        this.name = name;
        this.goal = goal;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getGoal() {
        return goal;
    }
    
    public void setGoal(String goal) {
        this.goal = goal;
    }
    
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
    
    public Integer getPlannedVelocity() {
        return plannedVelocity;
    }
    
    public void setPlannedVelocity(Integer plannedVelocity) {
        this.plannedVelocity = plannedVelocity;
    }
}