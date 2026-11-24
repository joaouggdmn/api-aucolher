package com.aucolher.api.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Limite de mensagens do assistente, em memória, por quem pergunta: a conta logada
 * ("user:{id}") ou, para visitante, o IP ("ip:{endereço}"). Protege a cota do Gemini
 * contra abuso. Com mais de uma instância da API, iria para um cache compartilhado (Redis).
 */
@Component
public class SupportRateLimiter {

    static final int PER_MINUTE = 10;
    static final int PER_DAY = 60;
    private static final Duration MINUTE = Duration.ofMinutes(1);
    private static final Duration DAY = Duration.ofDays(1);

    private final Clock clock;
    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    public SupportRateLimiter() {
        this(Clock.systemUTC());
    }

    SupportRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Registra a mensagem se ainda couber nos limites; false = limite atingido (nada é registrado). */
    public boolean tryAcquire(String key) {
        Instant now = clock.instant();
        Deque<Instant> times = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && !times.peekFirst().isAfter(now.minus(DAY))) {
                times.pollFirst();
            }
            long lastMinute = times.stream().filter(t -> t.isAfter(now.minus(MINUTE))).count();
            if (lastMinute >= PER_MINUTE || times.size() >= PER_DAY) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }

    /** De hora em hora, esquece quem não pergunta nada há mais de um dia (a memória não cresce sem parar). */
    @Scheduled(fixedDelay = 3_600_000)
    public void evictIdle() {
        Instant limit = clock.instant().minus(DAY);
        hits.entrySet().removeIf(entry -> {
            Deque<Instant> times = entry.getValue();
            synchronized (times) {
                return times.isEmpty() || !times.peekLast().isAfter(limit);
            }
        });
    }

    int trackedKeys() {
        return hits.size();
    }
}
