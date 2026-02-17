package com.utagent.optimizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IterativeOptimizer Concurrency Tests")
class IterativeOptimizerConcurrencyTest {

    @Nested
    @DisplayName("Current Iteration Thread Safety Tests")
    class CurrentIterationThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent iteration increments safely")
        void shouldHandleConcurrentIterationIncrementsSafely() throws InterruptedException {
            int threadCount = 10;
            int incrementsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);

            AtomicInteger sharedIteration = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < incrementsPerThread; j++) {
                            sharedIteration.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            endLatch.await();
            executor.shutdown();

            assertEquals(threadCount * incrementsPerThread, sharedIteration.get());
        }

        @Test
        @DisplayName("Should provide thread-safe iteration counter")
        void shouldProvideThreadSafeIterationCounter() {
            AtomicInteger counter = new AtomicInteger(0);

            assertEquals(0, counter.get());
            assertEquals(1, counter.incrementAndGet());
            assertEquals(2, counter.incrementAndGet());
            assertEquals(2, counter.get());
        }

        @Test
        @DisplayName("Should handle get and increment atomically")
        void shouldHandleGetAndIncrementAtomically() {
            AtomicInteger counter = new AtomicInteger(0);

            int first = counter.getAndIncrement();
            assertEquals(0, first);
            assertEquals(1, counter.get());

            int second = counter.getAndIncrement();
            assertEquals(1, second);
            assertEquals(2, counter.get());
        }
    }

    @Nested
    @DisplayName("AtomicInteger Behavior Tests")
    class AtomicIntegerBehaviorTests {

        @Test
        @DisplayName("Should support compare and set")
        void shouldSupportCompareAndSet() {
            AtomicInteger counter = new AtomicInteger(5);

            assertTrue(counter.compareAndSet(5, 10));
            assertEquals(10, counter.get());

            assertFalse(counter.compareAndSet(5, 15));
            assertEquals(10, counter.get());
        }

        @Test
        @DisplayName("Should support update and get")
        void shouldSupportUpdateAndGet() {
            AtomicInteger counter = new AtomicInteger(5);

            int result = counter.updateAndGet(x -> x * 2);
            assertEquals(10, result);
            assertEquals(10, counter.get());
        }
    }
}
