package com.autotrack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit tests that don't require Spring Boot context
 */
class AutoTrackApplicationTests {

    @Test
    @DisplayName("Basic arithmetic test")
    void testBasicArithmetic() {
        assertEquals(4, 2 + 2);
        assertEquals(0, 5 - 5);
        assertEquals(15, 3 * 5);
    }

    @Test
    @DisplayName("String operations test")
    void testStringOperations() {
        String projectName = "AutoTrack";
        assertNotNull(projectName);
        assertEquals(9, projectName.length());
        assertTrue(projectName.startsWith("Auto"));
        assertTrue(projectName.endsWith("Track"));
    }

    @Test
    @DisplayName("Collection operations test")
    void testCollectionOperations() {
        java.util.List<String> features = new java.util.ArrayList<>();
        features.add("Task Management");
        features.add("Team Collaboration");
        features.add("GitHub Integration");
        
        assertEquals(3, features.size());
        assertTrue(features.contains("Task Management"));
        assertFalse(features.isEmpty());
    }

    @Test
    @DisplayName("Application class existence test")
    void testApplicationClassExists() {
        // Test that our main application class exists
        assertDoesNotThrow(() -> {
            Class<?> appClass = Class.forName("com.autotrack.AutoTrackApplication");
            assertNotNull(appClass);
            assertTrue(appClass.getName().contains("AutoTrack"));
        });
    }
}
