package com.utagent.model;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Result<T> {

    private final T value;
    private final String error;
    private final boolean success;

    private Result(T value, String error, boolean success) {
        this.value = value;
        this.error = error;
        this.success = success;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, null, true);
    }

    public static <T> Result<T> failure(String error) {
        return new Result<>(null, error, false);
    }

    public static <T> Result<T> failure(Exception exception) {
        return new Result<>(null, exception.getMessage(), false);
    }

    public static <T> Result<T> of(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(e);
        }
    }

    public static <T> Result<T> fromOptional(Optional<T> optional, String errorIfEmpty) {
        return optional.map(Result::<T>success)
            .orElseGet(() -> failure(errorIfEmpty));
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public T getValue() {
        return value;
    }

    public String getError() {
        return error;
    }

    public <U> Result<U> map(Function<T, U> mapper) {
        if (isFailure()) {
            return failure(error);
        }
        try {
            return success(mapper.apply(value));
        } catch (Exception e) {
            return failure(e);
        }
    }

    public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
        if (isFailure()) {
            return failure(error);
        }
        try {
            return mapper.apply(value);
        } catch (Exception e) {
            return failure(e);
        }
    }

    public Result<T> recover(Function<String, T> recoveryFunction) {
        if (isSuccess()) {
            return this;
        }
        try {
            return success(recoveryFunction.apply(error));
        } catch (Exception e) {
            return failure(e);
        }
    }

    public Optional<T> toOptional() {
        return isSuccess() ? Optional.ofNullable(value) : Optional.empty();
    }

    public T getOrElse(T defaultValue) {
        return isSuccess() ? value : defaultValue;
    }

    public T getOrElseGet(Supplier<T> defaultSupplier) {
        return isSuccess() ? value : defaultSupplier.get();
    }

    @Override
    public String toString() {
        if (isSuccess()) {
            return "Result.success(" + value + ")";
        }
        return "Result.failure(" + error + ")";
    }
}
