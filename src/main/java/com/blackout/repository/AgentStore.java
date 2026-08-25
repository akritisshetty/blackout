package com.blackout.repository;

import com.blackout.entity.Agent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory agent storage replacing JPA/H2. Thread-safe via ConcurrentHashMap.
 * Data lives for the lifetime of the server process.
 */
@Component
public class AgentStore {

    private final Map<String, Agent> byCodename = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    public Optional<Agent> findByCodenameIgnoreCase(String codename) {
        return Optional.ofNullable(byCodename.get(codename.toUpperCase()));
    }

    public Agent save(Agent agent) {
        if (agent.getId() == null) {
            agent.setId(idSequence.getAndIncrement());
        }
        byCodename.put(agent.getCodename().toUpperCase(), agent);
        return agent;
    }

    /** Leaderboard order - top scorers first, ties broken by earliest enlistment. */
    public List<Agent> findAllByOrderByScoreDescCreatedAtAsc() {
        return byCodename.values().stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getScore(), a.getScore());
                    return cmp != 0 ? cmp : a.getCreatedAt().compareTo(b.getCreatedAt());
                })
                .toList();
    }
}
