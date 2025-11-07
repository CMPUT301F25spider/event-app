package com.example.event_app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.event_app.TestDataLoader;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class EntrantRowTest {

    @Test
    public void fromBuildsRowWhenDocumentExists() throws IOException {
        Map<String, String> data = TestDataLoader.loadRecord("test-data/entrants.csv", "ENT5001");

        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(snapshot.exists()).thenReturn(true);
        when(snapshot.getId()).thenReturn(data.get("id"));
        when(snapshot.getString("uid")).thenReturn(data.get("uid"));
        when(snapshot.getString("name")).thenReturn(data.get("name"));
        when(snapshot.getString("email")).thenReturn(data.get("email"));
        when(snapshot.getString("status")).thenReturn(data.get("status"));
        when(snapshot.getTimestamp("selectionTimestamp")).thenReturn(timestampFrom(data.get("selectionTimestamp")));
        when(snapshot.getTimestamp("confirmationTimestamp")).thenReturn(timestampFrom(data.get("confirmationTimestamp")));
        when(snapshot.getTimestamp("invitationExpiry")).thenReturn(timestampFrom(data.get("invitationExpiry")));
        when(snapshot.getTimestamp("cancellationTimestamp")).thenReturn(timestampFrom(data.get("cancellationTimestamp")));

        EntrantRow row = EntrantRow.from(snapshot);

        assertNotNull(row);
        assertEquals(data.get("id"), row.id);
        assertEquals(data.get("uid"), row.uid);
        assertEquals(data.get("name"), row.name);
        assertEquals(data.get("email"), row.email);
        assertEquals(data.get("status"), row.status);
        assertNotNull(row.selectionTimestamp);
        assertNotNull(row.confirmationTimestamp);
    }

    @Test
    public void fromReturnsNullWhenSnapshotMissing() {
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(snapshot.exists()).thenReturn(false);

        assertNull(EntrantRow.from(snapshot));
        assertNull(EntrantRow.from(null));
    }

    private Timestamp timestampFrom(String isoString) {
        if (isoString == null || isoString.isEmpty()) {
            return null;
        }
        Instant instant = Instant.parse(isoString);
        return new Timestamp(Date.from(instant));
    }
}
