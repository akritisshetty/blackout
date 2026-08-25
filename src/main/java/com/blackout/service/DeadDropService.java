package com.blackout.service;

import com.blackout.crypto.DeadDropProtocol;
import com.blackout.dto.DropResponse;
import com.blackout.dto.DropSubmissionRequest;
import com.blackout.entity.DeadDrop;
import com.blackout.repository.DeadDropRepository;
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
 * BLACKOUT // DeadDropService
 *
 * Burial and retrieval of packages. Every submission is sealed HERE, on the backend,
 * with SHA-256 (via {@link java.security.MessageDigest}, wrapped by Sha256Engine)
 * before the row touches the database. Clients can therefore never forge a seal -
 * they can only verify one against a package that may have been altered in transit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeadDropService {

    private final DeadDropRepository repository;

    /**
     * Seals and buries a new dead drop.
     * seal = SHA-256(encryptedPayload | encryptedKey), computed server-side only.
     */
    @Transactional
    public DropResponse submit(DropSubmissionRequest request) {
        String seal = DeadDropProtocol.computeSeal(request.encryptedPayload(), request.encryptedKey());

        DeadDrop saved = repository.save(DeadDrop.builder()
                .codename(request.codename().trim().toUpperCase(Locale.ROOT))
                .locationTag(request.locationTag() == null
                        ? "UNKNOWN"
                        : request.locationTag().trim().toUpperCase(Locale.ROOT))
                .encryptedPayload(request.encryptedPayload())
                .encryptedKey(request.encryptedKey())
                .sha256Seal(seal)
                .timestamp(LocalDateTime.now())
                .build());

        log.info("[BURIAL] dead drop #{} '{}' sealed (sha256={})",
                saved.getId(), saved.getCodename(), abbreviate(seal));
        return DropResponse.from(saved);
    }

    /** Live wiretap feed, newest first. */
    @Transactional(readOnly = true)
    public List<DropResponse> findAll() {
        return repository.findAllByOrderByTimestampDesc().stream()
                .map(DropResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DropResponse findById(Long id) {
        return repository.findById(id)
                .map(DropResponse::from)
                .orElseThrow(() -> notFound(id));
    }

    /**
     * DEMO-ONLY: simulates an adversary flipping a byte inside the payload while it sits
     * at the drop point. The stored seal is deliberately left untouched, so the very next
     * integrity check on this drop MUST fail and drive the red [STATUS: COMPROMISED] path.
     */
    @Transactional
    public DropResponse simulateTamper(Long id) {
        DeadDrop drop = repository.findById(id)
                .orElseThrow(() -> notFound(id));

        String payload = drop.getEncryptedPayload();
        char sabotageChar = payload.charAt(payload.length() - 1) == '#' ? '$' : '#';
        drop.setEncryptedPayload(payload.substring(0, payload.length() - 1) + sabotageChar);
        repository.save(drop);

        log.warn("[BREACH SIM] drop #{} payload mutated after sealing - integrity check will now fail", id);
        return DropResponse.from(drop);
    }

    private static ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Dead drop #" + id + " does not exist");
    }

    private static String abbreviate(String seal) {
        return seal.substring(0, 12) + "…" + seal.substring(52);
    }
}
