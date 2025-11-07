package com.example.event_app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import org.junit.Test;

public class PosterValidatorTest {

    @Test
    public void validateAcceptsPngBelowSizeLimit() {
        ContentResolver resolver = mock(ContentResolver.class);
        Uri uri = Uri.parse("content://media/external/images/media/100");

        when(resolver.getType(uri)).thenReturn("image/png");
        Cursor cursor = mock(Cursor.class);
        when(resolver.query(eq(uri), any(String[].class), any(), any(), any())).thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getColumnIndex(OpenableColumns.SIZE)).thenReturn(0);
        when(cursor.getLong(0)).thenReturn(2L * 1024 * 1024);

        PosterValidator validator = new PosterValidator(resolver, 5L * 1024 * 1024);

        Result<Void> result = validator.validate(uri);

        assertTrue(result.isOk());
        verify(cursor).close();
    }

    @Test
    public void validateRejectsUnsupportedMime() {
        ContentResolver resolver = mock(ContentResolver.class);
        Uri uri = Uri.parse("content://downloads/documents/file123");

        when(resolver.getType(uri)).thenReturn("application/pdf");

        PosterValidator validator = new PosterValidator(resolver, 5L * 1024 * 1024);

        Result<Void> result = validator.validate(uri);

        assertFalse(result.isOk());
        assertEquals("Only JPG or PNG allowed", result.getErrorMessage());
    }

    @Test
    public void validateRejectsWhenSizeIsUnknown() {
        ContentResolver resolver = mock(ContentResolver.class);
        Uri uri = Uri.parse("content://images/42");

        when(resolver.getType(uri)).thenReturn("image/jpeg");
        when(resolver.query(eq(uri), any(String[].class), any(), any(), any())).thenReturn(null);

        PosterValidator validator = new PosterValidator(resolver, 5L * 1024 * 1024);

        Result<Void> result = validator.validate(uri);

        assertFalse(result.isOk());
        assertTrue(result.getErrorMessage().contains("Unable to read file size"));
    }

    @Test
    public void validateRejectsWhenSizeTooLarge() {
        ContentResolver resolver = mock(ContentResolver.class);
        Uri uri = Uri.parse("content://images/43");

        when(resolver.getType(uri)).thenReturn("image/jpeg");
        Cursor cursor = mock(Cursor.class);
        when(resolver.query(eq(uri), any(String[].class), any(), any(), any())).thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getColumnIndex(anyString())).thenReturn(0);
        when(cursor.getLong(0)).thenReturn(7L * 1024 * 1024);

        PosterValidator validator = new PosterValidator(resolver, 5L * 1024 * 1024);

        Result<Void> result = validator.validate(uri);

        assertFalse(result.isOk());
        assertTrue(result.getErrorMessage().startsWith("Max size is"));
        verify(cursor).close();
    }
}
