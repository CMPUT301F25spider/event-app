

  > **Legend:** 🔄 = Modified from Part 3 | ✨ = New in Part 4 | 🎯 = Implemented

  ---

  ## Core Domain Classes

  ### Event 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | • Hold event metadata (id, name, description, location, eventDate, posterUrl, capacity, status)<br>• Track registration window (registrationStartDate, registrationEndDate)<br>• Report registration state:
  `isRegistrationOpen()`, `isRegistrationClosed()`<br>• Provide organizer summaries (totalSelected, totalCancelled, totalAttending counts)<br>🔄 **Calculate cancellation rate: `getCancellationRate()`, 
  `hasHighCancellationRate()`**<br>🔄 **Store organizerId and organizerName for filtering**<br>🔄 **Maintain waiting list, selected list, signed up users, declined users**<br>🔄 **Store entrant locations map 
  for geolocation**<br>🔄 **Track lottery state: `isLotteryRun()`, `hasReplacementPool()`, `isCapacityFull()`**<br>🔄 **Maintain replacement log for audit trail** | • User (Organizer)<br>• GeolocationAudit<br>•
   NotificationLog<br>• FirebaseFirestore<br>• FirebaseStorage<br>• All Event Adapters<br>• Real-time listeners |

  **User Stories:** US 02.01.04, US 01.01.03, US 02.06.01–02.06.03, US 02.03.01, US 02.05.02–02.05.03
  **Changes:** Added lottery management, geolocation, real-time sync, replacement tracking

  ---

  ### User 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | • Represent device-identified user (userId, deviceId, name, email, phoneNumber)<br>🔄 **Support multiple roles (List<String>): entrant, organizer, admin**<br>🔄 **Track notification preferences (Map<String,
   Boolean>)**<br>🔄 **Maintain favorite events list (List<String>)**<br>• Maintain timestamps (createdAt, updatedAt)<br>🔄 **Provide role checking methods: `isAdmin()`, `isOrganizer()`, `isEntrant()`, 
  `hasRole(role)`**<br>🔄 **Support dynamic role addition: `addRole(role)`**<br>• Update and delete profile (right to leave the app)<br>✨ **Store profile image URL** | • UserRole (utility)<br>•
  ProfileSetupActivity<br>• SettingsActivity<br>• ProfileFragment<br>• SplashActivity<br>• FirebaseAuth<br>• FirebaseFirestore<br>• FirebaseStorage<br>• UserAdapter<br>• FavoritesManager |

  **User Stories:** US 01.02.01–01.02.04, US 01.04.03, US 01.07.01
  **Changes:** Added favorites, notification preferences, profile images, role management

  ---

  ### Notification ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Store notification data (notificationId, userId, eventId, type, title, message)**<br>✨ **Track read status (read boolean)**<br>✨ **Maintain timestamp (createdAt)**<br>✨ **Define notification 
  types:**<br>&nbsp;&nbsp;• TYPE_WAITLIST_JOINED<br>&nbsp;&nbsp;• TYPE_SELECTED<br>&nbsp;&nbsp;• TYPE_NOT_SELECTED<br>&nbsp;&nbsp;• TYPE_INVITATION_SENT<br>&nbsp;&nbsp;• TYPE_INVITATION_DECLINED<br>✨ **Provide
   read state methods: `isRead()`, `setRead(boolean)`** | • User<br>• Event<br>• NotificationService<br>• NotificationAdapter<br>• NotificationsActivity<br>• FirebaseFirestore<br>• Real-time listeners |

  **User Stories:** US 01.04.01 (receive notification when chosen), US 01.04.02 (receive notification when not chosen), US 02.07.01–02.07.03 (send notifications)  
  **Changes:** New model for notification system with real-time updates

  ---

  ### GeolocationAudit ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Log geolocation access for privacy compliance**<br>✨ **Store audit data (auditId, userId, userName, eventId, eventName)**<br>✨ **Record location (latitude, longitude)**<br>✨ **Track timestamp and 
  action type**<br>✨ **Support admin audit queries**<br>✨ **Provide location data: `getLatitude()`, `getLongitude()`** | • Event<br>• User<br>• EventDetailsActivity<br>• AdminGeolocationAuditActivity<br>•
  GeolocationAuditAdapter<br>• FirebaseFirestore |

  **User Stories:** US 02.02.02 (capture geolocation), Admin privacy compliance  
  **Changes:** New model for geolocation audit trail

  ---

  ### NotificationLog ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Log all sent notifications for compliance**<br>✨ **Store log data (logId, userId, eventId, type, title, message)**<br>✨ **Track sent timestamp (sentAt)**<br>✨ **Record delivery status**<br>✨ 
  **Support admin filtering and export** | • NotificationService<br>• AdminNotificationLogsActivity<br>• NotificationLogAdapter<br>• FirebaseFirestore |

  **User Stories:** Admin compliance tracking  
  **Changes:** New model for notification audit trail

  ---

  ### NotificationTemplate ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Store reusable notification templates**<br>✨ **Define template data (templateId, name, type, title, message)**<br>✨ **Track usage (createdAt, lastUsed)**<br>✨ **Apply template with variables: 
  `applyTemplate(Map)`**<br>✨ **Support admin CRUD operations** | • NotificationService<br>• AdminNotificationTemplatesActivity<br>• NotificationTemplateAdapter<br>• FirebaseFirestore |

  **User Stories:** US 02.07.01 (send notifications to entrants)  
  **Changes:** New model for notification template management

  ---

  ### ImageData ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Track uploaded images (imageId, url, uploadedBy)**<br>✨ **Store metadata (uploadedAt, type, eventId)**<br>✨ **Support flagging: `isFlagged()`, `setFlagged(boolean)`**<br>✨ **Enable admin 
  moderation**<br>✨ **Link to events and users** | • Event<br>• User<br>• AdminBrowseImagesActivity<br>• ImageAdapter<br>• FirebaseStorage<br>• FirebaseFirestore |

  **User Stories:** US 03.03.01 (remove images), US 03.06.01 (browse images)  
  **Changes:** New model for image management and moderation

  ---

  ## Service Classes

  ### NotificationService 🎯

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🎯 **Send individual notifications**<br>🎯 **Send bulk notifications to entrant lists**<br>🎯 **Create notification logs for compliance**<br>🎯 **Fetch user notifications with real-time updates**<br>🎯 
  **Mark notifications as read: `markAsRead(id)`**<br>🎯 **Mark all as read: `markAllAsRead(userId)`**<br>🎯 **Delete notifications: `deleteNotification(id)`, `deleteAllNotifications(userId)`**<br>✨ **Apply 
  notification templates**<br>✨ **Integrate with FCM (future)** | • Notification<br>• NotificationLog<br>• NotificationTemplate<br>• User<br>• Event<br>• FirebaseFirestore<br>• All Activities<br>•
  FCMTokenManager |

  **User Stories:** US 01.04.01–01.04.02, US 02.07.01–02.07.03  
  **Changes:** Implemented with logging, templates, and real-time support

  ---

  ### QRService 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | • Parse scanned QR codes and validate event IDs<br>🔄 **Use Navigator to route to event details**<br>• Validate QR code format: `isValidEventId(eventId)`<br>🔄 **Process QR content: `processQrCode(context, 
  qrContent)`**<br>• Handle invalid QR codes with error messages<br>✨ **Generate QR codes: `generateQRCode(eventId, size)`**<br>✨ **Upload QR codes to Firebase Storage** | • Navigator<br>• MainActivity<br>•
  CreateEventActivity<br>• ZXing library<br>• FirebaseStorage |

  **User Stories:** US 01.06.01, US 01.06.02, US 02.01.01 (generate QR)  
  **Changes:** Added QR generation, removed EventCreatedActivity (integrated into CreateEventActivity)

  ---

  ### Navigator 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🔄 **Navigate to event details: `navigateToEventDetails(context, eventId)`**<br>✨ **Navigate to organizer event details: `navigateToOrganizerEventDetails(context, eventId)`**<br>✨ **Navigate to admin 
  event details: `navigateToAdminEventDetails(context, eventId)`**<br>🔄 **Show invalid QR error: `showInvalidQrError(context)`**<br>• Centralize navigation logic<br>• Define navigation constants
  (EXTRA_EVENT_ID, EXTRA_USER_ID) | • QRService<br>• MainActivity<br>• All Activities<br>• All Adapters |

  **User Stories:** US 01.06.01–01.06.02, Navigation  
  **Changes:** Extended for multi-role navigation

  ---

  ### ReportExporter 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🔄 **Export platform statistics to CSV**<br>🔄 **Generate reports: total users, events, organizers**<br>🔄 **Export event entrant lists to CSV**<br>✨ **Export users to CSV**<br>✨ **Export geolocation 
  audits**<br>✨ **Export notification logs**<br>• Create timestamped report files<br>• Share reports via Intent (email, drive)<br>• Use FileProvider for secure file sharing | • AdminHomeActivity<br>•
  OrganizerEventDetailsActivity<br>• ViewEntrantsActivity<br>• Event<br>• User<br>• FirebaseFirestore<br>• Android FileProvider |

  **User Stories:** US 03.13.01 (export reports), US 02.06.05 (export entrants)  
  **Changes:** Extended for multiple export types

  ---

  ### PermissionManager 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🔄 **Check camera permission: `isCameraPermissionGranted(context)`**<br>✨ **Check location permission: `isLocationPermissionGranted(context)`**<br>🔄 **Request permissions: `requestPermissions(activity, 
  permissions[])`**<br>• Handle permission denial gracefully<br>• Provide permission rationale to users | • MainActivity<br>• HomeFragment<br>• EventDetailsActivity<br>• QRService<br>• Android Permissions API |

  **User Stories:** US 01.06.01 (camera), US 02.02.02 (location)  
  **Changes:** Added location permission support

  ---

  ### UserRole 🎯

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🎯 **Define role constants: ROLE_ENTRANT, ROLE_ORGANIZER, ROLE_ADMIN**<br>🎯 **Provide role checking: `hasRole(user, role)`**<br>🎯 **Add roles: `addRole(user, role)`**<br>• Support role-based routing and 
  access control | • User<br>• SplashActivity<br>• ProfileSetupActivity<br>• SettingsActivity<br>• All Activities |

  **User Stories:** US 01.07.01, multi-role support  
  **Changes:** Fully implemented

  ---

  ### FavoritesManager ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Add events to favorites: `addToFavorites(userId, eventId)`**<br>✨ **Remove from favorites: `removeFromFavorites(userId, eventId)`**<br>✨ **Check favorite status: `isFavorite(userId, eventId, 
  callback)`**<br>✨ **Toggle favorite state**<br>✨ **Sync with Firestore user document** | • User<br>• Event<br>• HomeFragment<br>• EventDetailsActivity<br>• FullEventAdapter<br>• FirebaseFirestore |

  **User Stories:** Enhanced user experience  
  **Changes:** New utility for favorites management

  ---

  ### FCMTokenManager ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Update FCM tokens: `updateFCMToken(userId, token)`**<br>✨ **Retrieve FCM tokens: `getFCMToken(userId, callback)`**<br>✨ **Sync tokens with Firestore**<br>✨ **Support push notification delivery** | •
   User<br>• NotificationService<br>• Firebase Cloud Messaging<br>• FirebaseFirestore |

  **User Stories:** US 01.04.01–01.04.02 (notifications)  
  **Changes:** New utility for FCM integration

  ---

  ### AccessibilityHelper ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Apply accessibility settings to activities**<br>✨ **Adjust text sizes**<br>✨ **Configure screen reader support**<br>✨ **Apply high contrast themes** | • All Activities<br>• SettingsActivity<br>• 
  Android Accessibility APIs |

  **User Stories:** Accessibility compliance  
  **Changes:** New utility for accessibility support

  ---

  ## Activity Classes - Entrant

  ### MainActivity 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🔄 **Serve as entrant home screen with bottom navigation**<br>✨ **Host fragments: HomeFragment, EventsFragment, ProfileFragment**<br>✨ **Manage fragment transactions**<br>✨ **Handle bottom navigation 
  item selection**<br>🔄 **Check user role on startup**<br>• Navigate to settings<br>• Provide logout functionality | • HomeFragment<br>• EventsFragment<br>• ProfileFragment<br>• SettingsActivity<br>• 
  FirebaseAuth<br>• FirebaseFirestore<br>• BottomNavigationView |

  **User Stories:** US 01.01.03 (browse), Navigation  
  **Changes:** Converted to fragment host with bottom navigation

  ---

  ### SplashActivity 🎯

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🎯 **Handle device-based authentication flow**<br>🎯 **Get Android device ID (Settings.Secure.ANDROID_ID)**<br>🎯 **Sign in anonymously to Firebase Auth**<br>🎯 **Check if user profile exists in 
  Firestore**<br>🎯 **Route based on user role:**<br>&nbsp;&nbsp;• Admin → AdminHomeActivity<br>&nbsp;&nbsp;• Organizer → MainActivity (will see organizer features)<br>&nbsp;&nbsp;• Entrant → MainActivity<br>🎯
   **Navigate to ProfileSetupActivity for new users**<br>• Show splash screen with 2-second delay | • FirebaseAuth<br>• FirebaseFirestore<br>• User<br>• UserRole<br>• ProfileSetupActivity<br>• MainActivity<br>•
   AdminHomeActivity |

  **User Stories:** US 01.07.01 (device-based auth)  
  **Changes:** Fully implemented with role-based routing

  ---

  ### EventDetailsActivity 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Display event details with real-time updates**<br>✨ **Use real-time listener for live sync**<br>🔄 **Join/leave waiting list**<br>✨ **Capture geolocation when joining (if enabled)**<br>✨ **Log 
  geolocation audit**<br>✨ **Accept/decline invitations**<br>✨ **Validate user list integrity**<br>✨ **Auto-fix data corruption**<br>✨ **Show lottery information dialog**<br>✨ **Check if user is organizer 
  and redirect**<br>✨ **Handle location permissions**<br>• Display event poster with Glide<br>• Show waiting list count<br>• Open location in maps | • Event<br>• User<br>• GeolocationAudit<br>•
  NotificationService<br>• Navigator<br>• PermissionManager<br>• FirebaseFirestore<br>• FirebaseAuth<br>• FusedLocationProviderClient<br>• ListenerRegistration<br>• Glide |

  **User Stories:** US 01.01.01–01.01.02, US 01.05.02–01.05.03, US 01.05.05, US 02.02.02  
  **Changes:** Added real-time updates, geolocation, data integrity validation, auto-redirect for organizers

  ---

  ### NotificationsActivity 🎯

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🎯 **Display notifications with real-time updates**<br>🎯 **Use real-time listener for instant updates**<br>🎯 **Mark individual notifications as read**<br>🎯 **Mark all notifications as read**<br>🎯 
  **Delete individual notifications**<br>🎯 **Clear all notifications**<br>🎯 **Navigate to event details on click**<br>✨ **Show unread count badge**<br>✨ **Update button states based on content** | •
  Notification<br>• NotificationService<br>• NotificationAdapter<br>• EventDetailsActivity<br>• Navigator<br>• FirebaseFirestore<br>• FirebaseAuth<br>• ListenerRegistration<br>• RecyclerView |

  **User Stories:** US 01.04.01–01.04.02  
  **Changes:** Fully implemented with real-time updates

  ---

  ### MyEventsActivity 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🔄 **Display user's joined events**<br>✨ **Filter by status: waiting, selected, attending**<br>✨ **Show event status badges**<br>✨ **Navigate to EventDetailsActivity on click**<br>• Load user's events 
  from Firestore<br>• Support search functionality | • Event<br>• MyEventsAdapter<br>• EventDetailsActivity<br>• Navigator<br>• FirebaseFirestore<br>• FirebaseAuth<br>• RecyclerView |

  **User Stories:** US 01.02.03 (event history)  
  **Changes:** Enhanced with filtering and search

  ---

  ### BrowseEventsActivity 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🔄 **Display all available events**<br>✨ **Use BrowseEventsTabFragment for event list**<br>• Support search and filtering<br>• Navigate to EventDetailsActivity | • BrowseEventsTabFragment<br>• Event<br>• 
  EventAdapter<br>• EventDetailsActivity<br>• Navigator |

  **User Stories:** US 01.01.03 (browse events)  
  **Changes:** Converted to fragment container

  ---

  ### SettingsActivity 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🔄 **Display and update user profile**<br>✨ **Upload/update profile image**<br>✨ **Manage notification preferences**<br>✨ **Become organizer (add role)**<br>✨ **View accessibility settings**<br>🔄 
  **Delete profile with cleanup**<br>🔄 **Remove from all event lists**<br>• Sign out user<br>• Navigate to main screen after deletion | • User<br>• Event<br>• UserRole<br>• FirebaseAuth<br>•
  FirebaseFirestore<br>• FirebaseStorage<br>• MainActivity |

  **User Stories:** US 01.02.01–01.02.04  
  **Changes:** Added role management, profile images, notification preferences

  ---

  ## Activity Classes - Organizer

  ### OrganizerEventsActivity ✨
  *Note: Should rename to match navigation*

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Display organizer's events list**<br>✨ **Navigate to CreateEventActivity**<br>✨ **Navigate to OrganizerEventDetailsActivity**<br>✨ **Support search and filtering** | • Event<br>• 
  OrganizerEventsAdapter<br>• CreateEventActivity<br>• OrganizerEventDetailsActivity<br>• FirebaseFirestore |

  **User Stories:** US 02.01.01, Organizer dashboard  
  **Changes:** New organizer home activity

  ---

  ### OrganizerEventDetailsActivity 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🔄 **Display event details with real-time updates**<br>✨ **Use real-time listener for live counts**<br>✨ **Run lottery and select winners**<br>✨ **Draw replacement from pool**<br>✨ **Send notifications 
  to entrants**<br>✨ **Export entrant lists to CSV**<br>✨ **Update event poster**<br>✨ **Cancel event**<br>✨ **Generate and display QR code**<br>✨ **View entrant map (geolocation)**<br>✨ **Navigate to 
  ViewEntrantsActivity**<br>✨ **Send event reminders**<br>✨ **Update lottery button visibility**<br>• Display live entrant counts | • Event<br>• User<br>• NotificationService<br>• ReportExporter<br>•
  QRService<br>• ViewEntrantsActivity<br>• ViewEntrantMapActivity<br>• FirebaseFirestore<br>• FirebaseStorage<br>• ListenerRegistration |

  **User Stories:** US 02.01.01, US 02.02.01–02.02.03, US 02.04.02, US 02.05.02–02.05.03, US 02.06.01–02.06.05, US 02.07.01–02.07.03  
  **Changes:** Added real-time updates, lottery, replacement draws, comprehensive event management

  ---

  ### CreateEventActivity 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🔄 **Collect event details (name, description, location, capacity)**<br>🔄 **Upload event poster to Firebase Storage**<br>🔄 **Set event dates via DatePicker/TimePicker**<br>🔄 **Set registration 
  window**<br>🔄 **Toggle geolocation requirement**<br>🔄 **Save Event to Firestore**<br>✨ **Generate and upload QR code immediately**<br>✨ **Display generated QR code**<br>✨ **Add organizer role to user if 
  needed**<br>✨ **Navigate back to organizer events**<br>• Handle image selection<br>• Validate inputs | • Event<br>• User<br>• UserRole<br>• QRService<br>• FirebaseFirestore<br>• FirebaseStorage<br>•
  FirebaseAuth<br>• DatePickerDialog<br>• TimePickerDialog |

  **User Stories:** US 02.01.01, US 02.01.04, US 02.04.01, US 02.02.03  
  **Changes:** Integrated QR generation, removed separate EventCreatedActivity

  ---

  ### ViewEntrantsActivity 🎯

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🎯 **Display entrants filtered by type (waiting, selected, confirmed, declined)**<br>🎯 **Show entrant details (name, email, status)**<br>🎯 **Support entrant search**<br>✨ **Export entrants to CSV**<br>✨
   **Navigate to user profiles** | • User<br>• Event<br>• EntrantListAdapter<br>• ReportExporter<br>• FirebaseFirestore<br>• RecyclerView |

  **User Stories:** US 02.02.01, US 02.06.01–02.06.04, US 02.06.05  
  **Changes:** Implemented with CSV export

  ---

  ### ViewEntrantMapActivity 🎯

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🎯 **Display map of entrant locations**<br>🎯 **Load event with geolocation data**<br>🎯 **Show markers for each entrant**<br>🎯 **Cluster markers for better visualization**<br>• Initialize Google Maps<br>•
   Handle map ready callback | • Event<br>• GeolocationAudit<br>• FirebaseFirestore<br>• Google Maps API |

  **User Stories:** US 02.02.02 (view map of entrants)  
  **Changes:** Implemented with Google Maps

  ---

  ## Activity Classes - Admin

  ### AdminHomeActivity 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🔄 **Display platform statistics dashboard**<br>🔄 **Show: total events, users, organizers, active events**<br>✨ **Display flagged content count**<br>🔄 **Navigate to browse events**<br>🔄 **Navigate to 
  browse users**<br>🔄 **Navigate to browse images**<br>✨ **Navigate to geolocation audit**<br>✨ **Navigate to notification logs**<br>✨ **Navigate to notification templates**<br>🔄 **Export platform 
  report**<br>✨ **Switch to user mode**<br>• Check admin access<br>• Load statistics from Firestore | • Event<br>• User<br>• ReportExporter<br>• AdminBrowseEventsActivity<br>• AdminBrowseUsersActivity<br>•
  AdminBrowseImagesActivity<br>• AdminGeolocationAuditActivity<br>• AdminNotificationLogsActivity<br>• AdminNotificationTemplatesActivity<br>• MainActivity<br>• FirebaseFirestore |

  **User Stories:** US 03.04.01, US 03.05.01, US 03.06.01, US 03.13.01, Admin compliance  
  **Changes:** Added compliance features, flagged content, mode switching

  ---

  ### AdminBrowseEventsActivity 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🔄 **Display all events with real-time updates**<br>✨ **Use real-time listener for instant sync**<br>🔄 **Support search by name/organizer**<br>🔄 **Filter by status (All, Active, Inactive, Completed, 
  Cancelled, Flagged)**<br>🔄 **Sort by: name, date, entrant count**<br>🔄 **Navigate to AdminEventDetailsActivity**<br>• Show event metadata<br>• Display entrant counts | • Event<br>• AdminEventAdapter<br>•
  AdminEventDetailsActivity<br>• Navigator<br>• FirebaseFirestore<br>• ListenerRegistration<br>• RecyclerView |

  **User Stories:** US 03.01.01, US 03.04.01  
  **Changes:** Added real-time updates, flagged filter

  ---

  ### AdminBrowseUsersActivity 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🔄 **Display all users with real-time updates**<br>✨ **Use real-time listener for instant sync**<br>✨ **Search users by name/email**<br>🔄 **Show user info: name, email, roles**<br>🔄 **Delete user with 
  confirmation**<br>✨ **Delete user profile (privacy compliance)**<br>• Remove user from Firestore<br>• Show empty state | • User<br>• UserAdapter<br>• FirebaseFirestore<br>• ListenerRegistration<br>•
  RecyclerView |

  **User Stories:** US 03.02.01, US 03.05.01, US 03.07.01  
  **Changes:** Added real-time updates, search

  ---

  ### AdminBrowseImagesActivity 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🔄 **Display all uploaded images with real-time updates**<br>✨ **Use real-time listener for instant sync**<br>🔄 **Show event poster images in grid**<br>🔄 **Delete images from Firebase Storage**<br>✨ 
  **Flag inappropriate images**<br>✨ **Filter by flagged status**<br>• Load images from Storage<br>• Use Glide for loading | • ImageData<br>• ImageAdapter<br>• FirebaseStorage<br>• FirebaseFirestore<br>•
  ListenerRegistration<br>• Glide<br>• RecyclerView |

  **User Stories:** US 03.03.01, US 03.06.01  
  **Changes:** Added real-time updates, flagging

  ---

  ### AdminEventDetailsActivity 🎯

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🎯 **Display detailed event information**<br>🎯 **Show event poster**<br>🎯 **Display statistics and metrics**<br>🎯 **Show warning for flagged events**<br>🎯 **Delete event with confirmation**<br>✨ 
  **Unflag event**<br>• Load event by ID | • Event<br>• FirebaseFirestore<br>• Glide |

  **User Stories:** US 03.01.01, US 03.04.01  
  **Changes:** Added unflag capability

  ---

  ### AdminNotificationTemplatesActivity ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Display notification templates with real-time updates**<br>✨ **Use real-time listener**<br>✨ **Create new templates**<br>✨ **Edit existing templates**<br>✨ **Delete templates**<br>✨ **Preview 
  template messages** | • NotificationTemplate<br>• NotificationTemplateAdapter<br>• FirebaseFirestore<br>• ListenerRegistration<br>• RecyclerView |

  **User Stories:** US 02.07.01 (notification management)  
  **Changes:** New admin feature for template management

  ---

  ### AdminNotificationLogsActivity ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Display notification logs with real-time updates**<br>✨ **Use real-time listener**<br>✨ **Filter by date range**<br>✨ **Filter by event**<br>✨ **Filter by user**<br>✨ **Export logs to CSV**<br>✨ 
  **Show delivery status** | • NotificationLog<br>• NotificationLogAdapter<br>• ReportExporter<br>• FirebaseFirestore<br>• ListenerRegistration<br>• RecyclerView |

  **User Stories:** Admin compliance tracking  
  **Changes:** New admin feature for notification audit

  ---

  ### AdminGeolocationAuditActivity ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Display geolocation audit logs with real-time updates**<br>✨ **Use real-time listener**<br>✨ **Filter by event**<br>✨ **Filter by user**<br>✨ **Export audit to CSV**<br>✨ **Show location access 
  timeline** | • GeolocationAudit<br>• GeolocationAuditAdapter<br>• ReportExporter<br>• FirebaseFirestore<br>• ListenerRegistration<br>• RecyclerView |

  **User Stories:** Privacy compliance  
  **Changes:** New admin feature for geolocation audit

  ---

  ## Activity Classes - Shared

  ### ProfileSetupActivity 🎯

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🎯 **Collect user information (name, email, phone)**<br>🎯 **Validate input (email format, non-empty name)**<br>🎯 **Create User object with ENTRANT role by default**<br>🎯 **Save to Firestore**<br>🎯 
  **Route to MainActivity**<br>• Handle Firebase Auth user creation<br>• Store device ID | • User<br>• UserRole<br>• FirebaseAuth<br>• FirebaseFirestore<br>• MainActivity |

  **User Stories:** US 01.02.01 (provide information)  
  **Changes:** Fully implemented, simplified role selection

  ---

  ## Fragment Classes ✨

  ### HomeFragment ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Display entrant home feed**<br>✨ **Show "Happening Soon" events with real-time updates**<br>✨ **Show "Popular" events with real-time updates**<br>✨ **Show "Favorites" with real-time updates**<br>✨ 
  **Display notification badge with unread count**<br>✨ **Launch QR scanner**<br>✨ **Navigate to browse events**<br>✨ **Navigate to notifications**<br>✨ **Navigate to my events**<br>✨ **Navigate to create 
  event**<br>✨ **Use 4 concurrent real-time listeners**<br>✨ **Clean up listeners on destroy** | • Event<br>• HorizontalEventAdapter (x3)<br>• NotificationService<br>• QRService<br>• PermissionManager<br>•
  FavoritesManager<br>• FirebaseFirestore<br>• FirebaseAuth<br>• ListenerRegistration (x4)<br>• RecyclerView |

  **User Stories:** US 01.01.03, US 01.06.01, US 01.04.01, Enhanced UX  
  **Changes:** New main feed with real-time updates

  ---

  ### EventsFragment ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Host event browsing tabs**<br>✨ **Manage ViewPager2 with 2 tabs:**<br>&nbsp;&nbsp;• BrowseEventsTabFragment<br>&nbsp;&nbsp;• MyOrganizedEventsTabFragment<br>✨ **Link TabLayout with ViewPager** | • 
  BrowseEventsTabFragment<br>• MyOrganizedEventsTabFragment<br>• ViewPager2<br>• TabLayout |

  **User Stories:** Navigation, US 02.01.01  
  **Changes:** New tab container

  ---

  ### BrowseEventsTabFragment ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Display all active events with real-time updates**<br>✨ **Use real-time listener**<br>✨ **Support search by name/organizer/location/description**<br>✨ **Filter by category**<br>✨ **Sort by: date, 
  name, popularity**<br>✨ **Show waiting list count**<br>✨ **Navigate to EventDetailsActivity**<br>✨ **Clean up listener on destroy** | • Event<br>• FullEventAdapter<br>• EventDetailsActivity<br>•
  Navigator<br>• FirebaseFirestore<br>• ListenerRegistration<br>• RecyclerView |

  **User Stories:** US 01.01.03, US 01.01.04, US 01.05.04  
  **Changes:** New fragment with real-time updates

  ---

  ### MyOrganizedEventsTabFragment ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Display organizer's events with real-time updates**<br>✨ **Use real-time listener**<br>✨ **Show create event button**<br>✨ **Navigate to CreateEventActivity**<br>✨ **Navigate to 
  OrganizerEventDetailsActivity**<br>✨ **Clean up listener on destroy** | • Event<br>• OrganizerEventsAdapter<br>• CreateEventActivity<br>• OrganizerEventDetailsActivity<br>• Navigator<br>•
  FirebaseFirestore<br>• FirebaseAuth<br>• ListenerRegistration<br>• RecyclerView |

  **User Stories:** US 02.01.01, Organizer event management  
  **Changes:** New fragment with real-time updates

  ---

  ### ProfileFragment ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Display user profile**<br>✨ **Show profile image**<br>✨ **Show user information**<br>✨ **Show roles**<br>✨ **Navigate to settings**<br>✨ **Upload/update profile image**<br>✨ **Load profile from 
  Firestore** | • User<br>• SettingsActivity<br>• FirebaseFirestore<br>• FirebaseAuth<br>• FirebaseStorage<br>• Glide |

  **User Stories:** US 01.02.01–01.02.02  
  **Changes:** New profile management fragment

  ---

  ## Adapter Classes

  ### HorizontalEventAdapter ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Bind Event objects for horizontal RecyclerView**<br>✨ **Display event name, date, poster**<br>✨ **Handle event clicks**<br>✨ **Navigate to EventDetailsActivity**<br>✨ **Update list dynamically: 
  `setEvents(events)`**<br>• Use Glide for image loading | • Event<br>• HomeFragment<br>• EventDetailsActivity<br>• Navigator<br>• Glide<br>• RecyclerView |

  **User Stories:** Enhanced UX  
  **Changes:** New adapter for horizontal scrolling

  ---

  ### FullEventAdapter ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Bind Event objects for full-detail list**<br>✨ **Display comprehensive event info**<br>✨ **Show waiting list count**<br>✨ **Show favorite button**<br>✨ **Handle favorite toggle**<br>✨ **Navigate 
  to EventDetailsActivity**<br>• Use Glide for images | • Event<br>• BrowseEventsTabFragment<br>• EventDetailsActivity<br>• FavoritesManager<br>• Navigator<br>• Glide<br>• RecyclerView |

  **User Stories:** US 01.01.03, US 01.05.04  
  **Changes:** New adapter with favorites

  ---

  ### OrganizerEventsAdapter 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🔄 **Bind Event objects for organizer view**<br>🔄 **Display event name, details, attendee count**<br>✨ **Show live entrant counts**<br>✨ **Handle event clicks**<br>✨ **Navigate to 
  OrganizerEventDetailsActivity**<br>✨ **Update list dynamically** | • Event<br>• MyOrganizedEventsTabFragment<br>• OrganizerEventDetailsActivity<br>• Navigator<br>• RecyclerView |

  **User Stories:** Organizer dashboard  
  **Changes:** Added navigation, live counts

  ---

  ### EventAdapter ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Generic event adapter**<br>✨ **Bind Event objects**<br>✨ **Display event cards**<br>• Reusable across different screens | • Event<br>• Various Activities<br>• RecyclerView |

  **User Stories:** Generic event display  
  **Changes:** New generic adapter

  ---

  ### MyEventsAdapter ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Bind Event objects for user's events**<br>✨ **Display event with status badge**<br>✨ **Show waiting/selected/attending status**<br>✨ **Handle event clicks**<br>• Navigate to EventDetailsActivity | •
   Event<br>• MyEventsActivity<br>• EventDetailsActivity<br>• Navigator<br>• RecyclerView |

  **User Stories:** US 01.02.03  
  **Changes:** New adapter for user's event history

  ---

  ### AdminEventAdapter 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🔄 **Bind Event objects for admin view**<br>🔄 **Display comprehensive event info**<br>✨ **Show organizer name**<br>✨ **Show entrant counts**<br>✨ **Show flagged status**<br>🔄 **Handle event 
  clicks**<br>🔄 **Navigate to AdminEventDetailsActivity** | • Event<br>• AdminBrowseEventsActivity<br>• AdminEventDetailsActivity<br>• Navigator<br>• RecyclerView |

  **User Stories:** US 03.04.01  
  **Changes:** Added flagged status

  ---

  ### NotificationAdapter 🎯

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🎯 **Bind Notification objects**<br>🎯 **Display notification title, message, timestamp**<br>🎯 **Show read/unread status**<br>🎯 **Handle notification clicks**<br>🎯 **Handle delete button clicks**<br>• 
  Navigate to EventDetailsActivity | • Notification<br>• NotificationsActivity<br>• EventDetailsActivity<br>• Navigator<br>• RecyclerView |

  **User Stories:** US 01.04.01–01.04.02  
  **Changes:** Implemented with callbacks

  ---

  ### EntrantListAdapter 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🔄 **Bind User IDs to entrant list**<br>✨ **Pre-load user data to prevent async issues**<br>✨ **Cache loaded users**<br>🔄 **Display entrant name, email**<br>✨ **Show entrant status**<br>• Use Firestore 
  to load user details | • User<br>• ViewEntrantsActivity<br>• FirebaseFirestore<br>• RecyclerView |

  **User Stories:** US 02.02.01, US 02.06.01–02.06.04  
  **Changes:** Fixed async loading bug with caching

  ---

  ### UserAdapter 🔄

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🔄 **Bind User objects**<br>🔄 **Display user name, email, roles**<br>✨ **Show profile images**<br>🔄 **Handle delete button clicks**<br>• Invoke callback for deletion | • User<br>• 
  AdminBrowseUsersActivity<br>• Glide<br>• RecyclerView |

  **User Stories:** US 03.05.01, US 03.02.01  
  **Changes:** Added profile images

  ---

  ### ImageAdapter ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Bind ImageData objects**<br>✨ **Display images in grid**<br>✨ **Show flagged indicator**<br>✨ **Handle delete button clicks**<br>✨ **Handle flag button clicks**<br>• Use Glide for loading | • 
  ImageData<br>• AdminBrowseImagesActivity<br>• Glide<br>• RecyclerView |

  **User Stories:** US 03.06.01, US 03.03.01  
  **Changes:** New adapter for image management

  ---

  ### NotificationLogAdapter ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Bind NotificationLog objects**<br>✨ **Display log entries**<br>✨ **Show timestamp, type, status**<br>✨ **Format timestamps** | • NotificationLog<br>• AdminNotificationLogsActivity<br>• RecyclerView 
  |

  **User Stories:** Admin compliance  
  **Changes:** New adapter for notification logs

  ---

  ### GeolocationAuditAdapter ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Bind GeolocationAudit objects**<br>✨ **Display audit entries**<br>✨ **Show user, event, location, timestamp**<br>✨ **Format coordinates** | • GeolocationAudit<br>• AdminGeolocationAuditActivity<br>•
   RecyclerView |

  **User Stories:** Privacy compliance  
  **Changes:** New adapter for geolocation audit

  ---

  ### NotificationTemplateAdapter ✨

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | ✨ **Bind NotificationTemplate objects**<br>✨ **Display template name, type**<br>✨ **Show last used timestamp**<br>✨ **Handle edit button clicks**<br>✨ **Handle delete button clicks**<br>• Invoke 
  callbacks | • NotificationTemplate<br>• AdminNotificationTemplatesActivity<br>• RecyclerView |

  **User Stories:** US 02.07.01  
  **Changes:** New adapter for template management

  ---

  ## Firebase SDK Classes (External)

  ### FirebaseFirestore 🎯

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🎯 **Provide singleton instance**<br>🎯 **CRUD operations on collections (users, events, notifications)**<br>🎯 **Handle subcollections (waitingList, entrants)**<br>🎯 **Support real-time listeners: 
  `addSnapshotListener()`**<br>🎯 **Execute queries with filters and sorting**<br>✨ **Batch writes and transactions** | • All Activities<br>• All Fragments<br>• All Services<br>• All Adapters |

  **User Stories:** All data persistence  
  **Changes:** Extensively used with real-time listeners

  ---

  ### FirebaseAuth 🎯

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🎯 **Provide singleton instance**<br>🎯 **Handle anonymous authentication**<br>🎯 **Manage current user session**<br>🎯 **Sign out users**<br>🎯 **Get current user: `getCurrentUser()`** | • 
  SplashActivity<br>• ProfileSetupActivity<br>• SettingsActivity<br>• All Activities needing auth |

  **User Stories:** US 01.07.01  
  **Changes:** Fully implemented

  ---

  ### FirebaseStorage 🎯

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🎯 **Provide singleton instance**<br>🎯 **Upload image files to cloud storage**<br>🎯 **Generate download URLs**<br>🎯 **Delete stored files**<br>🎯 **Store event posters in /posters path**<br>✨ **Store 
  profile images in /profiles path**<br>✨ **Store QR codes in /qrcodes path** | • CreateEventActivity<br>• ProfileFragment<br>• SettingsActivity<br>• AdminBrowseImagesActivity<br>• QRService |

  **User Stories:** US 02.04.01, US 03.03.01  
  **Changes:** Extended for multiple image types

  ---

  ### FusedLocationProviderClient 🎯

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🎯 **Get current location: `getCurrentLocation()`**<br>🎯 **Get last known location: `getLastLocation()`**<br>🎯 **Handle location permissions**<br>• Return Task<Location> | • EventDetailsActivity<br>• 
  PermissionManager<br>• Google Play Services |

  **User Stories:** US 02.02.02  
  **Changes:** Implemented for geolocation

  ---

  ### ListenerRegistration 🎯

  | **Responsibilities** | **Collaborators** |
  |----------------------|-------------------|
  | 🎯 **Represent active Firestore listener**<br>🎯 **Remove listener: `remove()`**<br>🎯 **Prevent memory leaks**<br>• Clean up in onDestroy/onDestroyView | • All Activities with listeners<br>• All Fragments 
  with listeners<br>• FirebaseFirestore |

  **User Stories:** Real-time updates, Memory management  
  **Changes:** Critical for real-time feature implementation

  ---

  ## Summary of Part 4 Changes

  ### 🔄 Major Architectural Changes

  | Change | Impact |
  |--------|--------|
  | **Fragment-based navigation** | MainActivity now hosts fragments instead of direct navigation |
  | **Real-time listeners everywhere** | 15+ real-time listeners across app for instant updates |
  | **Admin compliance features** | NotificationLog, GeolocationAudit, NotificationTemplate models added |
  | **Favorites system** | FavoritesManager utility and UI integration |
  | **Data integrity validation** | Auto-fix for data corruption in EventDetailsActivity |
  | **Enhanced adapters** | 13 total adapters with proper ViewHolder patterns |

  ### ✨ New Features Implemented

  1. **Real-Time Updates** - All event lists, notifications, admin panels update instantly
  2. **Geolocation Audit** - Privacy compliance tracking for location access
  3. **Notification Logs** - Complete notification audit trail
  4. **Notification Templates** - Reusable notification templates for organizers
  5. **Image Management** - Admin can browse, flag, and delete images
  6. **Favorites** - Users can favorite events
  7. **Data Validation** - Automatic integrity checks and fixes
  8. **Profile Images** - Users can upload profile pictures
  9. **Role Management** - Users can become organizers from settings
  10. **CSV Export** - Multiple export types for reports



