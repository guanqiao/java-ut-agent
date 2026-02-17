package com.utagent.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Result Tests")
class ResultTest {

    @Nested
    @DisplayName("Success Result Tests")
    class SuccessResultTests {

        @Test
        @DisplayName("Should create successful result")
        void shouldCreateSuccessfulResult() {
            Result<String> result = Result.success("test value");

            assertTrue(result.isSuccess());
            assertFalse(result.isFailure());
            assertEquals("test value", result.getValue());
            assertNull(result.getError());
        }

        @Test
        @DisplayName("Should map successful result")
        void shouldMapSuccessfulResult() {
            Result<Integer> result = Result.success(5);

            Result<Integer> mapped = result.map(x -> x * 2);

            assertTrue(mapped.isSuccess());
            assertEquals(10, mapped.getValue());
        }

        @Test
        @DisplayName("Should flatMap successful result")
        void shouldFlatMapSuccessfulResult() {
            Result<Integer> result = Result.success(5);

            Result<String> mapped = result.flatMap(x -> Result.success("value: " + x));

            assertTrue(mapped.isSuccess());
            assertEquals("value: 5", mapped.getValue());
        }

        @Test
        @DisplayName("Should convert to optional")
        void shouldConvertToOptional() {
            Result<String> result = Result.success("test");

            Optional<String> optional = result.toOptional();

            assertTrue(optional.isPresent());
            assertEquals("test", optional.get());
        }

        @Test
        @DisplayName("Should get or else default value")
        void shouldGetOrElseDefaultValue() {
            Result<String> result = Result.success("test");

            String value = result.getOrElse("default");

            assertEquals("test", value);
        }
    }

    @Nested
    @DisplayName("Failure Result Tests")
    class FailureResultTests {

        @Test
        @DisplayName("Should create failure result")
        void shouldCreateFailureResult() {
            Result<String> result = Result.failure("Error occurred");

            assertFalse(result.isSuccess());
            assertTrue(result.isFailure());
            assertNull(result.getValue());
            assertEquals("Error occurred", result.getError());
        }

        @Test
        @DisplayName("Should create failure result with exception")
        void shouldCreateFailureResultWithException() {
            Exception exception = new RuntimeException("Test exception");
            Result<String> result = Result.failure(exception);

            assertFalse(result.isSuccess());
            assertEquals("Test exception", result.getError());
        }

        @Test
        @DisplayName("Should not map failure result")
        void shouldNotMapFailureResult() {
            Result<Integer> result = Result.failure("Error");

            Result<Integer> mapped = result.map(x -> x * 2);

            assertTrue(mapped.isFailure());
            assertEquals("Error", mapped.getError());
        }

        @Test
        @DisplayName("Should convert to empty optional")
        void shouldConvertToEmptyOptional() {
            Result<String> result = Result.failure("Error");

            Optional<String> optional = result.toOptional();

            assertFalse(optional.isPresent());
        }

        @Test
        @DisplayName("Should get default value for failure")
        void shouldGetDefaultValueForFailure() {
            Result<String> result = Result.failure("Error");

            String value = result.getOrElse("default");

            assertEquals("default", value);
        }
    }

    @Nested
    @DisplayName("Recovery Tests")
    class RecoveryTests {

        @Test
        @DisplayName("Should recover from failure")
        void shouldRecoverFromFailure() {
            Result<String> result = Result.failure("Error");

            Result<String> recovered = result.recover(error -> "recovered");

            assertTrue(recovered.isSuccess());
            assertEquals("recovered", recovered.getValue());
        }

        @Test
        @DisplayName("Should not recover success")
        void shouldNotRecoverSuccess() {
            Result<String> result = Result.success("original");

            Result<String> recovered = result.recover(error -> "recovered");

            assertTrue(recovered.isSuccess());
            assertEquals("original", recovered.getValue());
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create from callable success")
        void shouldCreateFromCallableSuccess() {
            Result<Integer> result = Result.of(() -> 42);

            assertTrue(result.isSuccess());
            assertEquals(42, result.getValue());
        }

        @Test
        @DisplayName("Should create from callable failure")
        void shouldCreateFromCallableFailure() {
            Result<Integer> result = Result.of(() -> {
                throw new RuntimeException("Test error");
            });

            assertTrue(result.isFailure());
            assertEquals("Test error", result.getError());
        }

        @Test
        @DisplayName("Should create from optional present")
        void shouldCreateFromOptionalPresent() {
            Optional<String> optional = Optional.of("test");

            Result<String> result = Result.fromOptional(optional, "Not found");

            assertTrue(result.isSuccess());
            assertEquals("test", result.getValue());
        }

        @Test
        @DisplayName("Should create from optional empty")
        void shouldCreateFromOptionalEmpty() {
            Optional<String> optional = Optional.empty();

            Result<String> result = Result.fromOptional(optional, "Not found");

            assertTrue(result.isFailure());
            assertEquals("Not found", result.getError());
        }
    }
}
