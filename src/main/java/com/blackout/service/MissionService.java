package com.blackout.service;

import com.blackout.crypto.AsymmetricEngine;
import com.blackout.crypto.DeadDropProtocol;
import com.blackout.crypto.PlayfairEngine;
import com.blackout.dto.game.MissionChallengeResponse;
import com.blackout.dto.game.SolveRequest;
import com.blackout.dto.game.SolveResponse;
import com.blackout.entity.Agent;
import com.blackout.game.MissionBank;
import com.blackout.game.MissionSessionStore;
import com.blackout.game.MissionType;
import com.blackout.game.PendingMission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * BLACKOUT // MissionService
 *
 * The game master, kept deliberately small. Generates one mission at a time from the
 * {@link MissionBank}, keeps the answers server-side in {@link PendingMission}, and
 * scores submissions with two rules only:
 *
 *   correct  -> full points (half if the AUTO button was used)
 *   wrong    -> zero points, the right answer is revealed
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MissionService {

    /** Packages shown per TAMPER_HUNT mission; exactly one is fake. */
    private static final int HUNT_PACKAGE_COUNT = 3;

    private final MissionSessionStore store;
    private final AgentService agentService;

    private final SecureRandom secureRandom = new SecureRandom();

    /* ------------------------------------------------------------------
     * Mission generation
     * ------------------------------------------------------------------ */

    public MissionChallengeResponse generate(String rawCodename, String typeRaw) {
        Agent agent = agentService.requireAgent(rawCodename);
        MissionType type = parseType(typeRaw);

        if (type == MissionType.SECRET_DROP && agent.getPublicKey() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No RSA badge found - reload the game page so your browser can mint one");
        }

        PendingMission mission = switch (type) {
            case SEAL_INTEL -> sealIntel(agent.getCodename());
            case CRACK_BROADCAST -> crackBroadcast(agent.getCodename());
            case TAMPER_HUNT -> tamperHunt(agent.getCodename());
            case SECRET_DROP -> secretDrop(agent);
        };

        store.put(mission);
        log.info("[MISSION] '{}' issued {} token={}...",
                agent.getCodename(), type, mission.getToken().substring(0, 8));
        return MissionChallengeResponse.from(mission);
    }

    /** 1) Encrypt a message under a keyword. */
    private PendingMission sealIntel(String codename) {
        String phrase = MissionBank.randomPhrase();
        String keyword = MissionBank.randomKeyword();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", phrase.toUpperCase(Locale.ROOT));
        data.put("keyword", keyword.toUpperCase(Locale.ROOT));
        data.put("bigrams", PlayfairEngine.bigramPreview(phrase, keyword));

        return pending(codename, MissionType.SEAL_INTEL, data,
                PlayfairEngine.encrypt(phrase, keyword), null, null);
    }

    /** 2) Decrypt an intercepted message. */
    private PendingMission crackBroadcast(String codename) {
        String phrase = MissionBank.randomPhrase();
        String keyword = MissionBank.randomKeyword();
        String cipher = PlayfairEngine.encrypt(phrase, keyword);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cipherText", groupPairs(cipher));
        data.put("cipherTextCompact", cipher);
        data.put("keyword", keyword.toUpperCase(Locale.ROOT));

        return pending(codename, MissionType.CRACK_BROADCAST, data,
                MissionBank.sanitize(phrase), PlayfairEngine.decrypt(cipher, keyword), null);
    }

    /** 3) Find the one package whose payload no longer matches its SHA-256 seal. */
    private PendingMission tamperHunt(String codename) {
        List<PendingMission.AuditPackage> packages = new ArrayList<>(HUNT_PACKAGE_COUNT);
        Set<Long> usedIds = new HashSet<>();
        int fakeIndex = ThreadLocalRandom.current().nextInt(HUNT_PACKAGE_COUNT);

        for (int i = 0; i < HUNT_PACKAGE_COUNT; i++) {
            String phrase = MissionBank.randomPhrase();
            String keyword = MissionBank.randomKeyword();
            String payload = PlayfairEngine.encrypt(phrase, keyword);
            String keyBlob = randomBlob();
            String seal = DeadDropProtocol.computeSeal(payload, keyBlob);

            if (i == fakeIndex) {
                payload = flipLastLetter(payload); // altered AFTER sealing - seal stays frozen
            }
            packages.add(new PendingMission.AuditPackage(
                    nextUniqueId(usedIds), payload, keyBlob, seal, i == fakeIndex));
        }

        List<Map<String, Object>> visible = packages.stream()
                .<Map<String, Object>>map(pkg -> {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("id", pkg.id());
                    view.put("payload", pkg.payload());
                    view.put("keyBlob", pkg.keyBlob());
                    view.put("seal", pkg.seal());
                    return view;
                })
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("formula", "SHA-256( payload + '|' + keyBlob ) must equal seal");
        data.put("packages", visible);

        long fakeId = packages.stream()
                .filter(PendingMission.AuditPackage::tampered)
                .findFirst().orElseThrow().id();
        return pending(codename, MissionType.TAMPER_HUNT, data,
                "package #" + fakeId, null, packages);
    }

    /** 4) The keyword is locked under the agent's own RSA badge; unwrap, then decrypt. */
    private PendingMission secretDrop(Agent agent) {
        String phrase = MissionBank.randomPhrase();
        String keyword = MissionBank.randomKeyword();
        String payload = PlayfairEngine.encrypt(phrase, keyword);
        String rsaBlob = AsymmetricEngine.encrypt(keyword,
                AsymmetricEngine.decodePublicKey(agent.getPublicKey()));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lockedKeyword", rsaBlob);
        data.put("cipherText", groupPairs(payload));
        data.put("cipherTextCompact", payload);

        return pending(agent.getCodename(), MissionType.SECRET_DROP, data,
                MissionBank.sanitize(phrase), PlayfairEngine.decrypt(payload, keyword), null);
    }

    /* ------------------------------------------------------------------
     * Scoring - two rules
     * ------------------------------------------------------------------ */

    public SolveResponse solve(String rawCodename, SolveRequest request) {
        Agent agent = agentService.requireAgent(rawCodename);
        PendingMission mission = store.get(request.token());
        if (mission == null) {
            throw new ResponseStatusException(HttpStatus.GONE,
                    "Unknown or expired mission - just start a new one");
        }
        if (!mission.getAgentCodename().equals(agent.getCodename())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "That mission belongs to another agent");
        }

        store.remove(mission.getToken()); // one attempt per mission

        boolean correct = checkAnswer(mission, request);
        int points = 0;

        if (correct) {
            points = request.assisted()
                    ? mission.getType().points() / 2   // AUTO button = half credit
                    : mission.getType().points();      // solved by hand = full credit
            agentService.recordSuccess(agent, points);
            log.info("[SOLVED] '{}' cleared {} (+{} pts{})",
                    agent.getCodename(), mission.getType(), points, request.assisted() ? ", assisted" : "");
        } else {
            agentService.recordFailure(agent);
            log.info("[MISSED] '{}' failed {}", agent.getCodename(), mission.getType());
        }

        return new SolveResponse(
                correct,
                points,
                agent.getScore(),
                agent.getMissionsSolved(),
                correct ? "Correct!" : "Not quite.",
                correct ? null : mission.getExpectedAnswer());
    }

    private boolean checkAnswer(PendingMission mission, SolveRequest request) {
        return switch (mission.getType()) {
            case SEAL_INTEL -> matches(PlayfairEngine.normalize(orEmpty(request.cipherText())),
                    mission.getExpectedAnswer(), null);
            case CRACK_BROADCAST, SECRET_DROP ->
                    matches(PlayfairEngine.normalize(orEmpty(request.plainText())),
                            mission.getExpectedAnswer(), mission.getExpectedAlternative());
            case TAMPER_HUNT -> flaggedExactlyTheFake(mission, request.flaggedTamperedIds());
        };
    }

    /** Accepts either the engine-padded form or the clean readable phrase. */
    private static boolean matches(String submitted, String primary, String alternative) {
        if (submitted.isEmpty()) {
            return false;
        }
        boolean okPrimary = submitted.equals(primary);
        boolean okAlternative = alternative != null && submitted.equals(alternative);
        return okPrimary || okAlternative;
    }

    private static boolean flaggedExactlyTheFake(PendingMission mission, List<Long> flagged) {
        if (flagged == null || flagged.size() != 1) {
            return false; // exactly ONE fake - flag exactly one package
        }
        return mission.getAuditPackages().stream()
                .filter(PendingMission.AuditPackage::tampered)
                .allMatch(pkg -> pkg.id() == flagged.get(0));
    }

    /* ------------------------------------------------------------------
     * Helpers
     * ------------------------------------------------------------------ */

    private static MissionType parseType(String raw) {
        try {
            return MissionType.valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown mission type '" + raw + "'");
        }
    }

    private PendingMission pending(String codename, MissionType type, Map<String, Object> challenge,
                                   String expectedAnswer, String expectedAlternative,
                                   List<PendingMission.AuditPackage> auditPackages) {
        Instant now = Instant.now();
        return PendingMission.builder()
                .token(UUID.randomUUID().toString())
                .type(type)
                .agentCodename(codename)
                .createdAt(now)
                .expiresAt(now.plus(MissionSessionStore.MISSION_TTL))
                .challenge(challenge)
                .expectedAnswer(expectedAnswer)
                .expectedAlternative(expectedAlternative)
                .auditPackages(auditPackages)
                .build();
    }

    /** Random stand-in for an RSA blob - TAMPER_HUNT packages are dressing, not puzzles. */
    private String randomBlob() {
        byte[] blob = new byte[256];
        secureRandom.nextBytes(blob);
        return Base64.getEncoder().encodeToString(blob);
    }

    private static long nextUniqueId(Set<Long> used) {
        long id;
        do {
            id = 100 + ThreadLocalRandom.current().nextLong(900);
        } while (!used.add(id));
        return id;
    }

    private static String flipLastLetter(String payload) {
        char last = payload.charAt(payload.length() - 1);
        char replacement = last == 'Z' ? 'A' : (char) (last + 1);
        return payload.substring(0, payload.length() - 1) + replacement;
    }

    /** "GYIZSC" -> "GY IZ SC" - readable letter pairs for intercepted traffic. */
    private static String groupPairs(String compact) {
        StringBuilder sb = new StringBuilder(compact.length() + compact.length() / 2);
        for (int i = 0; i < compact.length(); i += 2) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(compact, i, Math.min(i + 2, compact.length()));
        }
        return sb.toString();
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
