package com.blackout.controller;

import com.blackout.dto.game.MissionChallengeResponse;
import com.blackout.dto.game.SolveRequest;
import com.blackout.dto.game.SolveResponse;
import com.blackout.service.MissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * BLACKOUT // MissionController
 *
 *   POST /api/missions/{codename}/new?type=   draw the next mission
 *   POST /api/missions/{codename}/solve       submit a solution
 */
@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @PostMapping("/{codename}/new")
    public MissionChallengeResponse newMission(@PathVariable String codename,
                                               @RequestParam String type) {
        return missionService.generate(codename, type);
    }

    @PostMapping("/{codename}/solve")
    public SolveResponse solve(@PathVariable String codename,
                               @Valid @RequestBody SolveRequest request) {
        return missionService.solve(codename, request);
    }
}
