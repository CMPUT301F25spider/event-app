const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Initialize Firebase Admin
admin.initializeApp();

/**
 * Send FCM Push Notification
 *
 * ✨ CRITICAL FIX: data.data contains the actual payload
 */
exports.sendFCMNotification = functions.https.onCall(async (request) => {

  // ✨ FIX: In Cloud Functions 2nd Gen, the data is in request.data
  const data = request.data || request;

  console.log('📱 Received request');
  console.log('📱 Data keys:', Object.keys(data));
  console.log('📱 Token:', data.token);
  console.log('📱 Title:', data.title);
  console.log('📱 Message:', data.message);

  // Validate input
  if (!data.token) {
    console.error('❌ No token in data');
    throw new functions.https.HttpsError('invalid-argument', 'FCM token is required');
  }

  if (!data.title || !data.message) {
    console.error('❌ No title or message');
    throw new functions.https.HttpsError('invalid-argument', 'Title and message are required');
  }

  console.log('📱 Sending FCM notification to token:', data.token.substring(0, 20) + '...');

  // Build notification message
  const message = {
    token: data.token,
    notification: {
      title: data.title,
      body: data.message,
    },
    data: {
      eventId: data.eventId ? String(data.eventId) : '',
      click_action: 'FLUTTER_NOTIFICATION_CLICK',
    },
    android: {
      priority: 'high',
      notification: {
        sound: 'default',
        channelId: 'event_notifications',
      },
    },
  };

  try {
    // Send notification via FCM
    const response = await admin.messaging().send(message);

    console.log('✅ Successfully sent notification:', response);

    return {
      success: true,
      messageId: response,
    };

  } catch (error) {
    console.error('❌ Error sending notification:', error);

    throw new functions.https.HttpsError('internal', 'Failed to send notification: ' + error.message);
  }
});

/**
 * Send FCM to Multiple Users (Bulk Send)
 */
exports.sendBulkFCMNotification = functions.https.onCall(async (request) => {

  // ✨ FIX: In Cloud Functions 2nd Gen, the data is in request.data
  const data = request.data || request;

  // Validate input
  if (!data.tokens || !Array.isArray(data.tokens) || data.tokens.length === 0) {
    throw new functions.https.HttpsError('invalid-argument', 'Tokens array is required');
  }

  if (!data.title || !data.message) {
    throw new functions.https.HttpsError('invalid-argument', 'Title and message are required');
  }

  console.log('📱 Sending bulk FCM notification to', data.tokens.length, 'users');

  // Build multicast message
  const message = {
    tokens: data.tokens,
    notification: {
      title: data.title,
      body: data.message,
    },
    data: {
      eventId: data.eventId ? String(data.eventId) : '',
    },
    android: {
      priority: 'high',
      notification: {
        sound: 'default',
        channelId: 'event_notifications',
      },
    },
  };

  try {
    // Send to multiple devices
    const response = await admin.messaging().sendEachForMulticast(message);

    console.log('✅ Successfully sent', response.successCount, 'notifications');
    console.log('❌ Failed to send', response.failureCount, 'notifications');

    if (response.failureCount > 0) {
      response.responses.forEach((res, index) => {
        if (!res.success) {
          console.error(`Failure for token ${index}: ${res.error.code} - ${res.error.message}`);
        }
      });
    }

    return {
      success: true,
      successCount: response.successCount,
      failureCount: response.failureCount,
    };

  } catch (error) {
    console.error('❌ Error sending bulk notifications:', error);

    throw new functions.https.HttpsError('internal', 'Failed to send notifications: ' + error.message);
  }
});