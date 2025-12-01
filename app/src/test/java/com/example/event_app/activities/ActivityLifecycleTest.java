package com.example.event_app.activities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import android.app.Activity;
import android.content.Intent;

import com.example.event_app.activities.admin.AdminBrowseEventsActivity;
import com.example.event_app.activities.admin.AdminBrowseImagesActivity;
import com.example.event_app.activities.admin.AdminBrowseUsersActivity;
import com.example.event_app.activities.admin.AdminEventDetailsActivity;
import com.example.event_app.activities.admin.AdminGeolocationAuditActivity;
import com.example.event_app.activities.admin.AdminHomeActivity;
import com.example.event_app.activities.admin.AdminNotificationLogsActivity;
import com.example.event_app.activities.admin.AdminNotificationTemplatesActivity;
import com.example.event_app.activities.entrant.BrowseEventsActivity;
import com.example.event_app.activities.entrant.EventDetailsActivity;
import com.example.event_app.activities.entrant.MainActivity;
import com.example.event_app.activities.entrant.MyEventsActivity;
import com.example.event_app.activities.entrant.NotificationsActivity;
import com.example.event_app.activities.entrant.SettingsActivity;
import com.example.event_app.activities.entrant.SplashActivity;
import com.example.event_app.activities.organizer.CreateEventActivity;
import com.example.event_app.activities.organizer.OrganizerEventDetailsActivity;
import com.example.event_app.activities.organizer.OrganizerEventsActivity;
import com.example.event_app.activities.organizer.ViewEntrantMapActivity;
import com.example.event_app.activities.organizer.ViewEntrantsActivity;
import com.example.event_app.activities.shared.ProfileSetupActivity;
import com.example.event_app.utils.Navigator;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;

@ExtendWith(RobolectricTestRunner.class)
@Config(sdk = 34)
class ActivityLifecycleTest {

    private static MockedStatic<FirebaseAuth> firebaseAuthStatic;
    private static MockedStatic<FirebaseFirestore> firebaseFirestoreStatic;
    private static FirebaseAuth mockAuth;
    private static FirebaseUser mockUser;
    private static FirebaseFirestore mockFirestore;
    private static DocumentReference mockDocument;
    private static CollectionReference mockCollection;
    private static ListenerRegistration mockRegistration;

    @BeforeAll
    static void setUpFirebase() {
        mockAuth = mock(FirebaseAuth.class, withSettings().lenient());
        mockUser = mock(FirebaseUser.class, withSettings().lenient());
        when(mockUser.getUid()).thenReturn("test-user-id");
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);

        firebaseAuthStatic = mockStatic(FirebaseAuth.class);
        firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

        mockFirestore = mock(FirebaseFirestore.class, withSettings().lenient());
        mockDocument = mock(DocumentReference.class, withSettings().lenient());
        mockCollection = mock(CollectionReference.class, withSettings().lenient());
        mockRegistration = mock(ListenerRegistration.class, withSettings().lenient());

        when(mockFirestore.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocument);
        when(mockCollection.document()).thenReturn(mockDocument);
        when(mockDocument.getId()).thenReturn("doc-id");

        Task<Object> noopTask = Tasks.forResult(new Object());
        when(mockDocument.get()).thenReturn(noopTask);
        when(mockDocument.set(any())).thenReturn(noopTask);
        when(mockDocument.addSnapshotListener(any())).thenReturn(mockRegistration);
        when(mockCollection.add(any())).thenReturn(noopTask);

        firebaseFirestoreStatic = mockStatic(FirebaseFirestore.class);
        firebaseFirestoreStatic.when(FirebaseFirestore::getInstance).thenReturn(mockFirestore);
    }

    @AfterAll
    static void tearDownFirebase() {
        firebaseAuthStatic.close();
        firebaseFirestoreStatic.close();
    }

    private <T extends Activity> T buildWithExtras(Class<T> activityClass, Intent intent) {
        T activity = Robolectric.buildActivity(activityClass, intent).setup().get();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        return activity;
    }

    private Object readField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void splashActivityAuthenticatesAndLoadsDeviceId() {
        SplashActivity activity = buildWithExtras(SplashActivity.class, new Intent());
        assertNotNull(readField(activity, "mAuth"));
        assertNotNull(readField(activity, "deviceId"));
    }

    @Test
    void mainActivityBindsNavigationAndAuth() {
        MainActivity activity = buildWithExtras(MainActivity.class, new Intent());
        assertNotNull(readField(activity, "bottomNav"));
        assertEquals(mockAuth, readField(activity, "mAuth"));
    }

    @Test
    void myEventsActivityUsesAuthenticatedUserId() {
        MyEventsActivity activity = buildWithExtras(MyEventsActivity.class, new Intent());
        assertEquals("test-user-id", readField(activity, "userId"));
    }

    @Test
    void browseEventsActivitySetsAuthAndDb() {
        BrowseEventsActivity activity = buildWithExtras(BrowseEventsActivity.class, new Intent());
        assertNotNull(readField(activity, "mAuth"));
        assertNotNull(readField(activity, "db"));
    }

    @Test
    void eventDetailsActivityReadsIntentEventId() {
        Intent intent = new Intent().putExtra(Navigator.EXTRA_EVENT_ID, "event-123");
        EventDetailsActivity activity = buildWithExtras(EventDetailsActivity.class, intent);
        assertEquals("event-123", readField(activity, "eventId"));
    }

    @Test
    void notificationsActivityLoadsUserId() {
        NotificationsActivity activity = buildWithExtras(NotificationsActivity.class, new Intent());
        assertEquals("test-user-id", readField(activity, "userId"));
    }

    @Test
    void settingsActivityLoadsUserId() {
        SettingsActivity activity = buildWithExtras(SettingsActivity.class, new Intent());
        assertEquals("test-user-id", readField(activity, "userId"));
    }

    @Test
    void profileSetupActivityReceivesDeviceAndUserIds() {
        Intent intent = new Intent()
                .putExtra("deviceId", "device-xyz")
                .putExtra("userId", "user-xyz");
        ProfileSetupActivity activity = buildWithExtras(ProfileSetupActivity.class, intent);
        assertEquals("device-xyz", readField(activity, "deviceId"));
        assertEquals("user-xyz", readField(activity, "userId"));
    }

    @Test
    void organizerEventsActivityUsesOrganizerUid() {
        OrganizerEventsActivity activity = buildWithExtras(OrganizerEventsActivity.class, new Intent());
        assertEquals("test-user-id", readField(activity, "organizerId"));
    }

    @Test
    void organizerEventDetailsActivityReadsEventId() {
        Intent intent = new Intent().putExtra("EVENT_ID", "org-event");
        OrganizerEventDetailsActivity activity = buildWithExtras(OrganizerEventDetailsActivity.class, intent);
        assertEquals("org-event", readField(activity, "eventId"));
    }

    @Test
    void createEventActivityBindsOrganizer() {
        CreateEventActivity activity = buildWithExtras(CreateEventActivity.class, new Intent());
        assertEquals("test-user-id", readField(activity, "organizerId"));
    }

    @Test
    void viewEntrantsActivityReadsEventId() {
        Intent intent = new Intent().putExtra("EVENT_ID", "entrant-event");
        ViewEntrantsActivity activity = buildWithExtras(ViewEntrantsActivity.class, intent);
        assertEquals("entrant-event", readField(activity, "eventId"));
    }

    @Test
    void viewEntrantMapActivityReadsEventId() {
        Intent intent = new Intent().putExtra("EVENT_ID", "map-event");
        ViewEntrantMapActivity activity = buildWithExtras(ViewEntrantMapActivity.class, intent);
        assertEquals("map-event", readField(activity, "eventId"));
    }

    @Test
    void adminHomeActivityUsesAuthenticatedUser() {
        AdminHomeActivity activity = buildWithExtras(AdminHomeActivity.class, new Intent());
        assertEquals(mockAuth, readField(activity, "mAuth"));
    }

    @Test
    void adminBrowseEventsActivityInitializesFirestore() {
        AdminBrowseEventsActivity activity = buildWithExtras(AdminBrowseEventsActivity.class, new Intent());
        assertNotNull(readField(activity, "db"));
    }

    @Test
    void adminBrowseImagesActivityInitializesFirestore() {
        AdminBrowseImagesActivity activity = buildWithExtras(AdminBrowseImagesActivity.class, new Intent());
        assertNotNull(readField(activity, "db"));
    }

    @Test
    void adminBrowseUsersActivityInitializesFirestore() {
        AdminBrowseUsersActivity activity = buildWithExtras(AdminBrowseUsersActivity.class, new Intent());
        assertNotNull(readField(activity, "db"));
    }

    @Test
    void adminEventDetailsActivityReadsEventId() {
        Intent intent = new Intent().putExtra(AdminEventDetailsActivity.EXTRA_EVENT_ID, "admin-event");
        AdminEventDetailsActivity activity = buildWithExtras(AdminEventDetailsActivity.class, intent);
        assertEquals("admin-event", readField(activity, "eventId"));
    }

    @Test
    void adminGeolocationAuditActivityInitializesFirestore() {
        AdminGeolocationAuditActivity activity = buildWithExtras(AdminGeolocationAuditActivity.class, new Intent());
        assertNotNull(readField(activity, "db"));
    }

    @Test
    void adminNotificationLogsActivityInitializesFirestore() {
        AdminNotificationLogsActivity activity = buildWithExtras(AdminNotificationLogsActivity.class, new Intent());
        assertNotNull(readField(activity, "db"));
    }

    @Test
    void adminNotificationTemplatesActivityInitializesFirestore() {
        AdminNotificationTemplatesActivity activity = buildWithExtras(AdminNotificationTemplatesActivity.class, new Intent());
        assertNotNull(readField(activity, "db"));
    }
}
