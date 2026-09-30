package com.curleesoft.pickem.backend.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.curleesoft.pickem.backend.model.Pick;
import com.curleesoft.pickem.backend.security.InvalidCredentialException;
import com.curleesoft.pickem.backend.security.PickemPrincipal;
import com.curleesoft.pickem.backend.service.GameMain;
import com.curleesoft.pickem.backend.service.PickService;

import jakarta.validation.Valid;

/**
 * Player game surface for the current card. Replaces {@code MainAction}. Any
 * signed-in player or manager may call it. The Next.js BFF is the public caller.
 */
@RestController
@RequestMapping("/api/game")
@PreAuthorize("hasAnyRole('PLAYER', 'MANAGER')")
public class GameController {

    private final PickService pickService;

    public GameController(PickService pickService) {
        this.pickService = pickService;
    }

    @GetMapping("/main")
    public GameMain main(Authentication authentication) {
        return pickService.main(principal(authentication).uid());
    }

    @PostMapping("/picks")
    public List<Pick> save(@Valid @RequestBody SavePicksRequest request, Authentication authentication) {
        return pickService.saveWeek(principal(authentication).uid(), request.seasonWeekId(), request.picks());
    }

    private static PickemPrincipal principal(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof PickemPrincipal caller) {
            return caller;
        }

        throw new InvalidCredentialException("Authentication is required");
    }
}