package com.autotrack.dto;

import java.time.LocalDate;

/**
 * DTO representing a data point for burndown charts.
 * Contains the remaining task count for a specific date.
 */
public class BurndownDataPoint {
    
    private LocalDate date;
    private int remainingTasks;
    private int idealRemaining;
    
    // Constructors
    public BurndownDataPoint() {}
    
    public BurndownDataPoint(LocalDate date, int remainingTasks, int idealRemaining) {
        this.date = date;
        this.remainingTasks = remainingTasks;
        this.idealRemaining = idealRemaining;
    }
    
    // Getters and Setters
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public int getRemainingTasks() {
        return remainingTasks;
    }
    
    public void setRemainingTasks(int remainingTasks) {
        this.remainingTasks = remainingTasks;
    }
    
    public int getIdealRemaining() {
        return idealRemaining;
    }
    
    public void setIdealRemaining(int idealRemaining) {
        this.idealRemaining = idealRemaining;
    }
}
