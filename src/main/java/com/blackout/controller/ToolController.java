package com.blackout.controller;

import com.blackout.crypto.AsymmetricEngine;
import com.blackout.crypto.PlayfairEngine;
import com.blackout.crypto.Sha256Engine;
import com.blackout.dto.game.ToolPlayfairSealRequest;
import com.blackout.dto.game.ToolRsaUnlockRequest;
import com.blackout.dto.game.ToolRsaWrapRequest;
import com.blackout.dto.game.ToolSha256Request;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BLACKOUT // ToolController
 *
 * The field terminal's crypto bench - the same three engines the missions are built on,
 * exposed as free-play tools:
 *
 *   GET  /api/tools/playfair/grid?keyword=   live 5x5 key matrix for any keyword
 *   POST /api/tools/playfair/seal            encrypt (assisted path in SEAL INTEL)
 *   POST /api/tools/playfair/open            decrypt
 *   POST /api/tools/sha256                   hash anything (audit workhorse)
 *   POST /api/tools/rsa/wrap                 seal a secret under any public badge
 *   POST /api/tools/rsa/unlock               unwrap with a pasted private badge
 */
@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class ToolController {

    @GetMapping("/playfair/grid")
    public Map<String, Object> grid(@RequestParam(required = false, defaultValue = "") String keyword) {
        char[][] matrix = PlayfairEngine.buildMatrix(keyword);
        String[] rows = new String[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            rows[i] = new String(matrix[i]);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("keyword", keyword == null ? "" : keyword);
        out.put("matrix", rows);
        return out;
    }

    @PostMapping("/playfair/seal")
    public Map<String, Object> playfairSeal(@Valid @RequestBody ToolPlayfairSealRequest request) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("cipherText", PlayfairEngine.encrypt(request.message(), request.keyword()));
        out.put("bigramPreview", PlayfairEngine.bigramPreview(request.message(), request.keyword()));
        return out;
    }

    @PostMapping("/playfair/open")
    public Map<String, Object> playfairOpen(@Valid @RequestBody ToolPlayfairSealRequest request) {
        // message field doubles as the ciphertext for the open direction
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("plainText", PlayfairEngine.decrypt(request.message(), request.keyword()));
        return out;
    }

    @PostMapping("/sha256")
    public Map<String, Object> sha256(@Valid @RequestBody ToolSha256Request request) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("digest", Sha256Engine.sha256Hex(request.input()));
        return out;
    }

    @PostMapping("/rsa/wrap")
    public Map<String, Object> rsaWrap(@Valid @RequestBody ToolRsaWrapRequest request) {
        String blob = AsymmetricEngine.encrypt(request.secret(),
                AsymmetricEngine.decodePublicKey(request.publicKey()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("blob", blob);
        return out;
    }

    @PostMapping("/rsa/unlock")
    public Map<String, Object> rsaUnlock(@Valid @RequestBody ToolRsaUnlockRequest request) {
        String secret = AsymmetricEngine.decrypt(request.blob(),
                AsymmetricEngine.decodePrivateKey(request.privateKey()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("secret", secret);
        return out;
    }
}
