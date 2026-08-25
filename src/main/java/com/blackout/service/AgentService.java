package com.blackout.service;

import com.blackout.crypto.AsymmetricEngine;
import com.blackout.dto.game.LeaderboardEntry;
import com.blackout.entity.Agent;
import com.blackout.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * BLACKOUT // AgentService
 *
 * Career management: enlistment by codename, automatic badge registration (the
 * browser-minted RSA-2048 public half), leaderboard reads and stat updates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private static final int LEADERBOARD_SIZE = 20;

    private final AgentRepository repository;

    /** Logs an agent in, creating a fresh dossier on first sight. */
    @Transactional
    public Agent registerOrLogin(String rawCodename) {
        String codename = rawCodename.trim().toUpperCase(Locale.ROOT);
        Agent agent = repository.findByCodenameIgnoreCase(codename)
                .orElseGet(() -> {
                    log.info("[ENLIST] new agent '{}' joins the network", codename);
                    return repository.save(Agent.builder()
                            .codename(codename)
                            .score(0)
                            .missionsSolved(0)
                            .missionsFailed(0)
                            .createdAt(LocalDateTime.now())
                            .lastActiveAt(LocalDateTime.now())
                            .build());
                });
        agent.setLastActiveAt(LocalDateTime.now());
        return repository.save(agent);
    }

    @Transactional(readOnly = true)
    public Agent requireAgent(String rawCodename) {
        String codename = rawCodename.trim().toUpperCase(Locale.ROOT);
        return repository.findByCodenameIgnoreCase(codename)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No dossier for agent '" + codename + "' - enlist first"));
    }

    /** Validates and stores the agent's public badge. The private half stays in the browser. */
    @Transactional
    public Agent registerBadge(String rawCodename, String publicKeyBase64) {
        Agent agent = requireAgent(rawCodename);
        try {
            AsymmetricEngine.decodePublicKey(publicKeyBase64);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "publicKey is not a valid Base64 X.509 RSA key");
        }
        agent.setPublicKey(publicKeyBase64.trim());
        log.info("[BADGE] agent '{}' registered an RSA-2048 public badge", agent.getCodename());
        return repository.save(agent);
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> leaderboard() {
        List<Agent> top = repository.findAllByOrderByScoreDescCreatedAtAsc();
        return top.subList(0, Math.min(LEADERBOARD_SIZE, top.size())).stream()
                .map(agent -> LeaderboardEntry.from(agent, top.indexOf(agent) + 1))
                .toList();
    }

    /** Applies a solved mission. */
    @Transactional
    public void recordSuccess(Agent agent, int pointsAwarded) {
        agent.setScore(agent.getScore() + pointsAwarded);
        agent.setMissionsSolved(agent.getMissionsSolved() + 1);
        agent.setLastActiveAt(LocalDateTime.now());
        repository.save(agent);
    }

    /** Applies a wrong answer: nothing gained, nothing lost. */
    @Transactional
    public void recordFailure(Agent agent) {
        agent.setMissionsFailed(agent.getMissionsFailed() + 1);
        agent.setLastActiveAt(LocalDateTime.now());
        repository.save(agent);
    }
}
