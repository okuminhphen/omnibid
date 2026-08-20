package com.omnibid.auction.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RedissonLockIntegrationTest {

    private static final int CONTENDERS = 20;

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.4-alpine")
    ).withExposedPorts(6379);

    private static RedissonClient redissonClient;

    @BeforeAll
    static void createClient() {
        Config config = new Config();
        config.useSingleServer().setAddress(
                "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379)
        );
        redissonClient = Redisson.create(config);
    }

    @AfterAll
    static void closeClient() {
        if (redissonClient != null) {
            redissonClient.shutdown();
        }
    }

    @Test
    void onlyOneThreadEntersAuctionCriticalSectionAtATime() throws Exception {
        AtomicInteger insideCriticalSection = new AtomicInteger();
        AtomicInteger maxConcurrent = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(CONTENDERS);
        CountDownLatch start = new CountDownLatch(1);

        // Use one worker per contender so every task reaches the start barrier.
        // A smaller pool would deadlock the barrier because queued tasks cannot
        // decrement the ready latch until the first wave is released.
        try (var executor = Executors.newFixedThreadPool(CONTENDERS)) {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int index = 0; index < CONTENDERS; index++) {
                tasks.add(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    RLock lock = redissonClient.getLock("test:lock:auction:concurrency");
                    if (!lock.tryLock(5, TimeUnit.SECONDS)) {
                        return false;
                    }
                    try {
                        int concurrent = insideCriticalSection.incrementAndGet();
                        maxConcurrent.accumulateAndGet(concurrent, Math::max);
                        Thread.sleep(15);
                        return true;
                    } finally {
                        insideCriticalSection.decrementAndGet();
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                });
            }

            List<Future<Boolean>> results = tasks.stream()
                    .map(executor::submit)
                    .toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            for (Future<Boolean> result : results) {
                assertThat(result.get(10, TimeUnit.SECONDS)).isTrue();
            }
        }

        assertThat(maxConcurrent).hasValue(1);
        assertThat(insideCriticalSection).hasValue(0);
    }
}
