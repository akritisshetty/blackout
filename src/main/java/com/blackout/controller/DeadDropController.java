package com.blackout.controller;

import com.blackout.dto.DropResponse;
import com.blackout.dto.DropSubmissionRequest;
import com.blackout.service.DeadDropService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * BLACKOUT // DeadDropController
 *
 * The relay's public wire. Field terminals talk to nothing else.
 *
 *   POST /api/drops            bury a sealed package (backend stamps the SHA-256 seal)
 *   GET  /api/drops            wiretap feed - every drop, newest first
 *   GET  /api/drops/{id}       single package dossier
 *   POST /api/drops/{id}/tamper  DEMO-ONLY sabotage switch (drives the red status path)
 */
@RestController
@RequestMapping("/api/drops")
@RequiredArgsConstructor
public class DeadDropController {

    private final DeadDropService deadDropService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DropResponse bury(@Valid @RequestBody DropSubmissionRequest request) {
        return deadDropService.submit(request);
    }

    @GetMapping
    public List<DropResponse> wiretap() {
        return deadDropService.findAll();
    }

    @GetMapping("/{id}")
    public DropResponse byId(@PathVariable Long id) {
        return deadDropService.findById(id);
    }

    /** DEMO-ONLY: corrupts a payload post-seal so integrity verification fails. */
    @PostMapping("/{id}/tamper")
    public DropResponse tamper(@PathVariable Long id) {
        return deadDropService.simulateTamper(id);
    }
}
