package com.example.event_app.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.event_app.TestDataLoader;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UserModelTest {

    @Test
    public void constructorInitializesDefaultRolesAndNotifications() throws IOException {
        Map<String, String> data = TestDataLoader.loadRecord("test-data/users.csv", "USR3001");

        User user = new User(data.get("userId"), data.get("deviceId"), data.get("name"), data.get("email"));

        assertEquals(data.get("userId"), user.getUserId());
        assertTrue(user.isNotificationsEnabled());
        assertFalse(user.hasRole("entrant"));
        assertFalse(user.hasRole("organizer"));
    }

    @Test
    public void roleHelpersReflectAssignedRoles() throws IOException {
        Map<String, String> data = TestDataLoader.loadRecord("test-data/users.csv", "USR3001");
        User user = new User();
        user.setUserId(data.get("userId"));
        user.setDeviceId(data.get("deviceId"));
        user.setName(data.get("name"));
        user.setEmail(data.get("email"));

        String[] roles = data.get("roles").split("\\|");
        for (String role : roles) {
            user.addRole(role);
        }

        assertTrue(user.isOrganizer());
        assertTrue(user.isEntrant());
        assertFalse(user.isAdmin());
    }

    @Test
    public void settersOverrideListOfRoles() {
        User user = new User("USR9999", "device-xyz", "Taylor", "taylor@example.com");

        List<String> updatedRoles = Arrays.asList("admin", "organizer");
        user.setRoles(updatedRoles);

        assertTrue(user.hasRole("admin"));
        assertTrue(user.isOrganizer());
        assertFalse(user.isEntrant());
    }
}
