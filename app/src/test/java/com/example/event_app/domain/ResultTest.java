package com.example.event_app.domain;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Result class
 */
public class ResultTest {

    @Test
    public void testOkResult() {
        String data = "test data";
        Result<String> result = Result.ok(data);
        
        assertTrue("Result should be ok", result.isOk());
        assertEquals("Data should match", data, result.data);
        assertNull("Error should be null", result.error);
        assertNull("Error message should be null", result.getErrorMessage());
    }

    @Test
    public void testErrResultWithThrowable() {
        Exception exception = new Exception("Test error");
        Result<String> result = Result.err(exception);
        
        assertFalse("Result should not be ok", result.isOk());
        assertNull("Data should be null", result.data);
        assertEquals("Error should match", exception, result.error);
        assertEquals("Error message should match", "Test error", result.getErrorMessage());
    }

    @Test
    public void testErrResultWithString() {
        String errorMessage = "Test error message";
        Result<String> result = Result.err(errorMessage);
        
        assertFalse("Result should not be ok", result.isOk());
        assertNull("Data should be null", result.data);
        assertNotNull("Error should not be null", result.error);
        assertEquals("Error message should match", errorMessage, result.getErrorMessage());
    }

    @Test
    public void testOkResultWithNull() {
        Result<String> result = Result.ok(null);
        
        assertTrue("Result should be ok even with null data", result.isOk());
        assertNull("Data should be null", result.data);
        assertNull("Error should be null", result.error);
    }

    @Test
    public void testResultWithInteger() {
        Integer value = 42;
        Result<Integer> result = Result.ok(value);
        
        assertTrue("Result should be ok", result.isOk());
        assertEquals("Data should match", value, result.data);
    }
}
