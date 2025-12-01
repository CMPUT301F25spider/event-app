package com.example.event_app.services;

import com.example.event_app.models.User;
import com.example.event_app.repositories.UserRepository;
import com.example.event_app.validation.UserValidationException;
import com.example.event_app.validation.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    private UserValidator validator;
    private UserService service;

    @BeforeEach
    void setUp() {
        validator = new UserValidator();
        service = new UserService(repository, validator);
    }

    @Test
    @DisplayName("createUser persists validated dummy profile")
    void createUser_savesValidatedProfile() {
        when(repository.findByDeviceId("device-001")).thenReturn(Optional.empty());
        User stored = new User("user-1", "device-001", "Jane Test", "jane@test.com");
        when(repository.save(any(User.class))).thenReturn(stored);

        User result = service.createUser("user-1", "device-001", "Jane Test", "jane@test.com", 28);

        assertEquals(stored, result);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        User savedUser = captor.getValue();
        assertEquals("user-1", savedUser.getUserId());
        assertEquals("device-001", savedUser.getDeviceId());
        assertEquals("Jane Test", savedUser.getName());
        assertEquals(28, savedUser.getAge());
    }

    @Test
    @DisplayName("createUser rejects duplicate device IDs with aggregated validation errors")
    void createUser_duplicateDeviceId_rejected() {
        when(repository.findByDeviceId("device-001")).thenReturn(Optional.of(new User()));

        UserValidationException exception = assertThrows(UserValidationException.class, () ->
                service.createUser("user-1", "device-001", "", "bad-email", 5)
        );

        assertTrue(exception.getErrors().contains("deviceId must be unique"));
        assertTrue(exception.getErrors().contains("email must be valid"));
        assertTrue(exception.getErrors().contains("name must be at least 3 characters"));
    }

    @Test
    @DisplayName("toggleNotifications updates persisted flag and timestamp")
    void toggleNotifications_updatesState() {
        User existing = new User("user-1", "device-001", "Jane", "jane@test.com");
        existing.setUpdatedAt(0L);
        when(repository.findById("user-1")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        User updated = service.toggleNotifications("user-1", false);

        assertFalse(updated.isNotificationsEnabled());
        verify(repository).save(existing);
        assertNotEquals(0L, updated.getUpdatedAt());
    }

    @Test
    @DisplayName("addRole injects role into user record and persists")
    void addRole_persistsRoleChange() {
        User existing = new User("user-1", "device-001", "Jane", "jane@test.com");
        when(repository.findById("user-1")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        User updated = service.addRole("user-1", "admin");

        assertTrue(updated.getRoles().contains("admin"));
        verify(repository).save(existing);
    }

    @Test
    @DisplayName("addFavorite enforces blank checks before hitting repository")
    void addFavorite_blankEventId_rejectedEarly() {
        assertThrows(IllegalArgumentException.class, () -> service.addFavorite("user-1", " "));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("addFavorite records event ID for the user")
    void addFavorite_persistsFavoriteList() {
        User existing = new User("user-1", "device-001", "Jane", "jane@test.com");
        when(repository.findById("user-1")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        User updated = service.addFavorite("user-1", "EVT-42");

        assertTrue(updated.getFavoriteEvents().contains("EVT-42"));
        verify(repository).save(existing);
    }
}
