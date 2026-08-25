package com.blackout.controller;

import com.blackout.dto.game.AgentProfileResponse;
import com.blackout.dto.game.AgentRegistrationRequest;
import com.blackout.dto.game.BadgeRegistrationRequest;
import com.blackout.dto.game.LeaderboardEntry;
import com.blackout.entity.Agent;
import com.blackout.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * BLACKOUT // AgentController
 *
 *   POST /api/agents                      enlist (or resume) by codename
 *   GET  /api/agents/{codename}           read a dossier
 *   PUT  /api/agents/{codename}/badge     register an RSA public badge
 *   GET  /api/leaderboard                 top agents by lifetime score
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/agents")
    public AgentProfileResponse enlist(@Valid @RequestBody AgentRegistrationRequest request) {
        Agent agent = agentService.registerOrLogin(request.codename());
        return AgentProfileResponse.from(agent);
    }

    @GetMapping("/agents/{codename}")
    public AgentProfileResponse dossier(@PathVariable String codename) {
        return AgentProfileResponse.from(agentService.requireAgent(codename));
    }

    @PutMapping("/agents/{codename}/badge")
    public AgentProfileResponse badge(@PathVariable String codename,
                                      @Valid @RequestBody BadgeRegistrationRequest request) {
        return AgentProfileResponse.from(agentService.registerBadge(codename, request.publicKey()));
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntry> leaderboard() {
        return agentService.leaderboard();
    }
}
