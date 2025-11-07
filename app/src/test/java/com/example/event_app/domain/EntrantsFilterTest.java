package com.example.event_app.domain;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for EntrantsFilter enum
 */
public class EntrantsFilterTest {

    @Test
    public void testFromLabel_Selected() {
        EntrantsFilter filter = EntrantsFilter.fromLabel("Selected");
        assertEquals("Should return Selected", EntrantsFilter.Selected, filter);
    }

    @Test
    public void testFromLabel_Pending() {
        EntrantsFilter filter = EntrantsFilter.fromLabel("Pending");
        assertEquals("Should return Pending", EntrantsFilter.Pending, filter);
    }

    @Test
    public void testFromLabel_Accepted() {
        EntrantsFilter filter = EntrantsFilter.fromLabel("Accepted");
        assertEquals("Should return Accepted", EntrantsFilter.Accepted, filter);
    }

    @Test
    public void testFromLabel_Declined() {
        EntrantsFilter filter = EntrantsFilter.fromLabel("Declined");
        assertEquals("Should return Declined", EntrantsFilter.Declined, filter);
    }

    @Test
    public void testFromLabel_Confirmed() {
        EntrantsFilter filter = EntrantsFilter.fromLabel("Confirmed");
        assertEquals("Should return Confirmed", EntrantsFilter.Confirmed, filter);
    }

    @Test
    public void testFromLabel_Cancelled() {
        EntrantsFilter filter = EntrantsFilter.fromLabel("Cancelled");
        assertEquals("Should return Cancelled", EntrantsFilter.Cancelled, filter);
    }

    @Test
    public void testFromLabel_All() {
        EntrantsFilter filter = EntrantsFilter.fromLabel("All");
        assertEquals("Should return All", EntrantsFilter.All, filter);
    }

    @Test
    public void testFromLabel_CaseInsensitive() {
        EntrantsFilter filter1 = EntrantsFilter.fromLabel("selected");
        EntrantsFilter filter2 = EntrantsFilter.fromLabel("SELECTED");
        EntrantsFilter filter3 = EntrantsFilter.fromLabel("SeLeCtEd");
        
        assertEquals("Should handle lowercase", EntrantsFilter.Selected, filter1);
        assertEquals("Should handle uppercase", EntrantsFilter.Selected, filter2);
        assertEquals("Should handle mixed case", EntrantsFilter.Selected, filter3);
    }

    @Test
    public void testFromLabel_WithWhitespace() {
        EntrantsFilter filter = EntrantsFilter.fromLabel("  Selected  ");
        assertEquals("Should trim whitespace", EntrantsFilter.Selected, filter);
    }

    @Test
    public void testFromLabel_Null() {
        EntrantsFilter filter = EntrantsFilter.fromLabel(null);
        assertEquals("Should default to Selected for null", EntrantsFilter.Selected, filter);
    }

    @Test
    public void testFromLabel_InvalidLabel() {
        EntrantsFilter filter = EntrantsFilter.fromLabel("InvalidFilter");
        assertEquals("Should default to Selected for invalid label", EntrantsFilter.Selected, filter);
    }

    @Test
    public void testFromLabel_EmptyString() {
        EntrantsFilter filter = EntrantsFilter.fromLabel("");
        assertEquals("Should default to Selected for empty string", EntrantsFilter.Selected, filter);
    }
}
