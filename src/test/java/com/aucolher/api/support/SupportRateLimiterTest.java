package com.aucolher.api.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

/** Limite de mensagens do assistente: 10 por minuto e 60 por dia, por quem pergunta. */
class SupportRateLimiterTest {

    /** Relógio que o teste adianta à mão. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-24T12:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final SupportRateLimiter limiter = new SupportRateLimiter(clock);

    @Test
    void tenMessagesPerMinuteThenWaits() {
        for (int i = 0; i < SupportRateLimiter.PER_MINUTE; i++) {
            assertTrue(limiter.tryAcquire("ip:1.2.3.4"), "mensagem " + (i + 1));
        }
        assertFalse(limiter.tryAcquire("ip:1.2.3.4"));

        clock.advance(Duration.ofSeconds(61));
        assertTrue(limiter.tryAcquire("ip:1.2.3.4"));
    }

    @Test
    void sixtyMessagesPerDay() {
        for (int i = 0; i < SupportRateLimiter.PER_DAY; i++) {
            assertTrue(limiter.tryAcquire("user:8"), "mensagem " + (i + 1));
            clock.advance(Duration.ofMinutes(2));
        }
        assertFalse(limiter.tryAcquire("user:8"));

        clock.advance(Duration.ofDays(1));
        assertTrue(limiter.tryAcquire("user:8"));
    }

    @Test
    void eachPersonHasItsOwnLimit() {
        for (int i = 0; i < SupportRateLimiter.PER_MINUTE; i++) {
            limiter.tryAcquire("user:8");
        }
        assertFalse(limiter.tryAcquire("user:8"));
        assertTrue(limiter.tryAcquire("user:9"));
        assertTrue(limiter.tryAcquire("ip:1.2.3.4"));
    }

    @Test
    void blockedAttemptIsNotCounted() {
        for (int i = 0; i < SupportRateLimiter.PER_MINUTE; i++) {
            limiter.tryAcquire("user:8");
        }
        for (int i = 0; i < 5; i++) {
            assertFalse(limiter.tryAcquire("user:8"));
        }
        clock.advance(Duration.ofSeconds(61));
        assertTrue(limiter.tryAcquire("user:8"));
    }

    @Test
    void forgetsWhoHasBeenIdleForADay() {
        limiter.tryAcquire("ip:1.2.3.4");
        clock.advance(Duration.ofHours(23));
        limiter.tryAcquire("user:8");

        clock.advance(Duration.ofHours(2));
        limiter.evictIdle();

        assertEquals(1, limiter.trackedKeys());
    }
}
