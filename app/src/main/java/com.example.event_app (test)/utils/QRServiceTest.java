package com.example.event_app.utils;

import android.content.Context;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class QRServiceTest {

    @Test
    @DisplayName("valid event IDs navigate to event details")
    void processQrCode_validId_routesToDetails() {
        Navigator navigator = mock(Navigator.class);
        QRService qrService = new QRService(navigator);
        Context fakeContext = mock(Context.class);

        qrService.processQrCode(fakeContext, "EVT-123456789");

        verify(navigator).navigateToEventDetails(fakeContext, "EVT-123456789");
        verify(navigator, never()).showInvalidQrError(any());
    }

    @Test
    @DisplayName("invalid codes raise invalid-QR feedback and skip navigation")
    void processQrCode_invalidId_showsError() {
        Navigator navigator = mock(Navigator.class);
        QRService qrService = new QRService(navigator);
        Context fakeContext = mock(Context.class);

        qrService.processQrCode(fakeContext, "bad");

        verify(navigator, never()).navigateToEventDetails(any(), any());
        verify(navigator).showInvalidQrError(fakeContext);
    }

    @Test
    @DisplayName("blank codes are treated as invalid")
    void processQrCode_blank_rejected() {
        Navigator navigator = mock(Navigator.class);
        QRService qrService = new QRService(navigator);
        Context fakeContext = mock(Context.class);

        qrService.processQrCode(fakeContext, "   ");

        verify(navigator, never()).navigateToEventDetails(any(), any());
        verify(navigator).showInvalidQrError(fakeContext);
    }
}
