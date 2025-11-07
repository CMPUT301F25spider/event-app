package com.example.event_app.utils;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for UserRole constants
 */
public class UserRoleTest {

    @Test
    public void testEntrantConstant() {
        assertEquals("ENTRANT constant should be 'entrant'", "entrant", UserRole.ENTRANT);
    }

    @Test
    public void testOrganizerConstant() {
        assertEquals("ORGANIZER constant should be 'organizer'", "organizer", UserRole.ORGANIZER);
    }

    @Test
    public void testAdminConstant() {
        assertEquals("ADMIN constant should be 'admin'", "admin", UserRole.ADMIN);
    }

    @Test
    public void testConstantsAreNotNull() {
        assertNotNull("ENTRANT should not be null", UserRole.ENTRANT);
        assertNotNull("ORGANIZER should not be null", UserRole.ORGANIZER);
        assertNotNull("ADMIN should not be null", UserRole.ADMIN);
    }

    @Test
    public void testConstantsAreNotEmpty() {
        assertFalse("ENTRANT should not be empty", UserRole.ENTRANT.isEmpty());
        assertFalse("ORGANIZER should not be empty", UserRole.ORGANIZER.isEmpty());
        assertFalse("ADMIN should not be empty", UserRole.ADMIN.isEmpty());
    }
}
