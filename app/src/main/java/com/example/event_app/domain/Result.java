package com.example.event_app.domain;

public class Result<T> {
    public final T data;
    public final Throwable error;

    private Result(T data, Throwable error) {
        this.data = data;
        this.error = error;
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(data, null);
    }

    public static <T> Result<T> err(Throwable e) {
        return new Result<>(null, e);
    }

    public boolean isOk() {
        return error == null;
    }
}
