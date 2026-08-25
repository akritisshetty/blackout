package com.blackout.game;

import com.blackout.crypto.AsymmetricEngine;
import com.blackout.crypto.DeadDropProtocol;
import com.blackout.crypto.PlayfairEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.KeyPair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack game contract tests over the simplified loop:
 * enlist -> encrypt -> decrypt -> find the fake -> secret drop -> leaderboard.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GameFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /* ---------------- helpers ---------------- */

    private JsonNode postJson(String url, String body) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode getJson(String url) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode newMission(String codename, String type) throws Exception {
        return postJson("/api/missions/" + codename + "/new?type=" + type, "{}");
    }

    /* ---------------- tests ---------------- */

    @Test
    @DisplayName("enlist creates a dossier with zeroed stats")
    void enlistCreatesDossier() throws Exception {
        mockMvc.perform(post("/api/agents")
                        .contentType(APPLICATION_JSON)
                        .content("{\"codename\":\"rookie-tester\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codename").value("ROOKIE-TESTER"))
                .andExpect(jsonPath("$.score").value(0))
                .andExpect(jsonPath("$.hasBadge").value(false));

        // idempotent re-enlist
        mockMvc.perform(post("/api/agents")
                        .contentType(APPLICATION_JSON)
                        .content("{\"codename\":\"ROOKIE-TESTER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codename").value("ROOKIE-TESTER"));
    }

    @Test
    @DisplayName("field tools: FIPS vector for SHA-256, playfair grid shape, RSA wrap/unlock round trip")
    void fieldToolsBehave() throws Exception {
        JsonNode digest = postJson("/api/tools/sha256", "{\"input\":\"abc\"}");
        assertThat(digest.get("digest").asText())
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");

        JsonNode grid = getJson("/api/tools/playfair/grid?keyword=MONARCH");
        assertThat(grid.get("matrix")).hasSize(5);
        assertThat(grid.get("matrix").get(0).asText()).isEqualTo("MONAR");

        KeyPair pair = AsymmetricEngine.generateKeyPair();
        JsonNode wrapped = postJson("/api/tools/rsa/wrap", "{\"publicKey\":\""
                + AsymmetricEngine.encodePublicKey(pair.getPublic()) + "\",\"secret\":\"quartz\"}");
        JsonNode opened = postJson("/api/tools/rsa/unlock", "{\"blob\":\"" + wrapped.get("blob").asText()
                + "\",\"privateKey\":\"" + AsymmetricEngine.encodePrivateKey(pair.getPrivate()) + "\"}");
        assertThat(opened.get("secret").asText()).isEqualTo("quartz");
    }

    @Test
    @DisplayName("SEAL INTEL: wrong answer reveals the answer; right answer pays exactly 10")
    void sealIntelScoring() throws Exception {
        postJson("/api/agents", "{\"codename\":\"seal-agent\"}");

        // miss on purpose - token burned, answer revealed, zero points
        JsonNode first = newMission("seal-agent", "SEAL_INTEL");
        JsonNode missed = postJson("/api/missions/seal-agent/solve",
                "{\"token\":\"" + first.get("token").asText() + "\",\"cipherText\":\"WRONG\"}");
        assertThat(missed.get("correct").asBoolean()).isFalse();
        assertThat(missed.get("pointsAwarded").asInt()).isZero();
        assertThat(missed.get("expectedAnswer").asText()).isNotBlank();

        // fresh token - solve using the same engine the relay runs
        JsonNode second = newMission("seal-agent", "SEAL_INTEL");
        String message = second.get("data").get("message").asText();
        String keyword = second.get("data").get("keyword").asText();
        String cipher = PlayfairEngine.encrypt(message.toLowerCase(), keyword.toLowerCase());

        JsonNode solved = postJson("/api/missions/seal-agent/solve",
                "{\"token\":\"" + second.get("token").asText() + "\",\"cipherText\":\"" + cipher + "\"}");
        assertThat(solved.get("correct").asBoolean()).isTrue();
        assertThat(solved.get("pointsAwarded").asInt()).isEqualTo(10);
        assertThat(solved.get("totalScore").asInt()).isEqualTo(10);

        // tokens are single-use
        mockMvc.perform(post("/api/missions/seal-agent/solve")
                        .contentType(APPLICATION_JSON)
                        .content("{\"token\":\"" + second.get("token").asText() + "\",\"cipherText\":\"" + cipher + "\"}"))
                .andExpect(status().isGone());
    }

    @Test
    @DisplayName("FIND THE FAKE: exactly one tampered package; recomputing the hash finds it")
    void tamperHunt() throws Exception {
        postJson("/api/agents", "{\"codename\":\"hunt-agent\"}");
        JsonNode mission = newMission("hunt-agent", "TAMPER_HUNT");

        long fakeId = -1;
        int tamperedCount = 0;
        for (JsonNode pkg : mission.get("data").get("packages")) {
            boolean intact = DeadDropProtocol.verifySeal(
                    pkg.get("payload").asText(), pkg.get("keyBlob").asText(), pkg.get("seal").asText());
            if (!intact) {
                tamperedCount++;
                fakeId = pkg.get("id").asLong();
            }
        }
        assertThat(tamperedCount).isEqualTo(1);

        JsonNode solved = postJson("/api/missions/hunt-agent/solve",
                "{\"token\":\"" + mission.get("token").asText() + "\",\"flaggedTamperedIds\":[" + fakeId + "]}");
        assertThat(solved.get("correct").asBoolean()).isTrue();
        assertThat(solved.get("pointsAwarded").asInt()).isEqualTo(20);
    }

    @Test
    @DisplayName("SECRET DROP: RSA unlock feeds the Playfair crack; badge required up front")
    void secretDropChain() throws Exception {
        // agents without a badge are refused before the mission is drawn
        postJson("/api/agents", "{\"codename\":\"naked-agent\"}");
        mockMvc.perform(post("/api/missions/naked-agent/new?type=SECRET_DROP"))
                .andExpect(status().isConflict());

        postJson("/api/agents", "{\"codename\":\"drop-agent\"}");
        KeyPair badge = AsymmetricEngine.generateKeyPair(); // stands in for the browser-minted badge
        mockMvc.perform(put("/api/agents/drop-agent/badge")
                        .contentType(APPLICATION_JSON)
                        .content("{\"publicKey\":\"" + AsymmetricEngine.encodePublicKey(badge.getPublic()) + "\"}"))
                .andExpect(status().isOk());

        JsonNode mission = newMission("drop-agent", "SECRET_DROP");
        String keyword = AsymmetricEngine.decrypt(
                mission.get("data").get("lockedKeyword").asText(), badge.getPrivate());
        String plaintext = PlayfairEngine.decrypt(
                mission.get("data").get("cipherTextCompact").asText(), keyword);

        JsonNode solved = postJson("/api/missions/drop-agent/solve",
                "{\"token\":\"" + mission.get("token").asText() + "\",\"plainText\":\"" + plaintext + "\"}");
        assertThat(solved.get("correct").asBoolean()).isTrue();
        assertThat(solved.get("pointsAwarded").asInt()).isEqualTo(25);
    }

    @Test
    @DisplayName("leaderboard orders by score and counts solves")
    void leaderboardOrdersByScore() throws Exception {
        postJson("/api/agents", "{\"codename\":\"board-agent\"}");
        JsonNode rows = getJson("/api/leaderboard");
        assertThat(rows.isArray()).isTrue();
        for (int i = 1; i < rows.size(); i++) {
            assertThat(rows.get(i - 1).get("score").asInt())
                    .isGreaterThanOrEqualTo(rows.get(i).get("score").asInt());
        }
    }
}
