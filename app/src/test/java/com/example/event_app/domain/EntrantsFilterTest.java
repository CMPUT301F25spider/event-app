package com.example.event_app.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EntrantsFilterTest {

    @Test
    public void fromLabelMatchesIgnoringCase() {
        assertEquals(EntrantsFilter.Accepted, EntrantsFilter.fromLabel("accepted"));
        assertEquals(EntrantsFilter.Declined, EntrantsFilter.fromLabel("DECLINED"));
        assertEquals(EntrantsFilter.Pending, EntrantsFilter.fromLabel(" Pending "));
    }

    @Test
    public void fromLabelDefaultsToSelected() {
        assertEquals(EntrantsFilter.Selected, EntrantsFilter.fromLabel(null));
        assertEquals(EntrantsFilter.Selected, EntrantsFilter.fromLabel("unknown"));
    }
}
