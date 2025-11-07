package com.example.event_app.organizer;

public class Result<T> {
    public final T data;
    public final Throwable error;

    private Result(T data, Throwable error) {
        this.data = data;
        this.error = error;
    }

    // Success
    public static <T> Result<T> ok(T data) {
        return new Result<>(data, null);
    }

    // Error with Throwable
    public static <T> Result<T> err(Throwable e) {
        return new Result<>(null, e);
    }

    // Error with String message (auto-wrap in Exception)
    public static <T> Result<T> err(String message) {
        return new Result<>(null, new Exception(message));
    }

    public boolean isOk() {
        return error == null;
    }

    public String getErrorMessage() {
        return (error != null) ? error.getMessage() : null;
    }
}
