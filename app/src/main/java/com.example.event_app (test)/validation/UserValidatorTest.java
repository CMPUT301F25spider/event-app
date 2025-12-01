package com.example.event_app.validation;

import com.example.event_app.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class UserValidatorTest {

    private UserValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UserValidator();
    }

    @Test
    @DisplayName("valid user passes all validation rules")
    void validUser_hasNoValidationErrors() {
        User user = new User("user-123", "device-abc", "Alice Doe", "alice@example.com");
        user.setAge(25);

        List<String> errors = validator.validateForCreate(user);

        assertTrue(errors.isEmpty(), "Expected no validation errors for complete dummy user");
    }

    @ParameterizedTest(name = "invalid user -> {1}")
    @MethodSource("invalidUserCases")
    void invalidUserDetails_collectsErrors(Consumer<User> mutation, String expectedError) {
        User user = new User("user-123", "device-abc", "Alice Doe", "alice@example.com");
        mutation.accept(user);

        List<String> errors = validator.validateForCreate(user);

        assertTrue(errors.contains(expectedError),
                () -> "Expected error `" + expectedError + "` for dummy data mutation");
    }

    static Stream<Arguments> invalidUserCases() {
        return Stream.of(
                Arguments.of((Consumer<User>) user -> user.setUserId(" "), "userId is required"),
                Arguments.of((Consumer<User>) user -> user.setDeviceId(null), "deviceId is required"),
                Arguments.of((Consumer<User>) user -> user.setName("Al"), "name must be at least 3 characters"),
                Arguments.of((Consumer<User>) user -> user.setName("A".repeat(60)), "name must be at most 50 characters"),
                Arguments.of((Consumer<User>) user -> user.setEmail("alice"), "email must be valid"),
                Arguments.of((Consumer<User>) user -> user.setAge(10), "age must be at least 13"),
                Arguments.of((Consumer<User>) user -> user.setAge(150), "age must be realistic (<= 120)"),
                Arguments.of((Consumer<User>) user -> user.setPhoneNumber("abc"),
                        "phoneNumber must be digits (10-15) and may start with +")
        );
    }

    @Test
    @DisplayName("all missing required fields are reported together")
    void missingRequiredFields_aggregatesErrors() {
        User incomplete = new User();

        List<String> errors = validator.validateForCreate(incomplete);

        assertEquals(4, errors.size(), "Expected all required field errors to be surfaced at once");
        assertTrue(errors.contains("userId is required"));
        assertTrue(errors.contains("deviceId is required"));
        assertTrue(errors.contains("name is required"));
        assertTrue(errors.contains("email is required"));
    }
}
