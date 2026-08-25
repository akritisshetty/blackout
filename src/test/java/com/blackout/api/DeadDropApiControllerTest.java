package com.blackout.api;

import com.blackout.crypto.DeadDropProtocol;
import com.blackout.dto.DropResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack contract test: bury -> wiretap -> verify seal -> tamper -> detect breach.
 * Boots the real relay (Tomcat + JPA + in-memory H2) under MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DeadDropApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("bury, fetch, verify seal server-side; then tamper and watch verification fail")
    void fullDeadDropLifecycle() throws Exception {
        String transmission = """
                {
                  "codename": "operation-nightfall",
                  "locationTag": "lisbon dock 7",
                  "encryptedPayload": "GYIZSC",
                  "encryptedKey": "QUJDRA=="
                }""";

        // 1. bury - backend must stamp a 64-hex SHA-256 seal before persisting
        MvcResult created = mockMvc.perform(post("/api/drops")
                        .contentType(APPLICATION_JSON)
                        .content(transmission))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(greaterThan(0)))
                .andExpect(jsonPath("$.codename").value("OPERATION-NIGHTFALL"))
                .andExpect(jsonPath("$.locationTag").value("LISBON DOCK 7"))
                .andExpect(jsonPath("$.sha256Seal").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andReturn();

        DropResponse drop = objectMapper.readValue(
                created.getResponse().getContentAsString(), DropResponse.class);

        assertThat(drop.sha256Seal()).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(DeadDropProtocol.verifySeal("GYIZSC", "QUJDRA==", drop.sha256Seal())).isTrue();

        // 2. wiretap feed exposes the drop, newest first
        mockMvc.perform(get("/api/drops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codename").value("OPERATION-NIGHTFALL"));

        // 3. single dossier lookup
        mockMvc.perform(get("/api/drops/" + drop.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sha256Seal").value(drop.sha256Seal()));

        // 4. adversary flips a byte at the drop point - seal stays frozen
        MvcResult sabotaged = mockMvc.perform(post("/api/drops/" + drop.id() + "/tamper"))
                .andExpect(status().isOk())
                .andReturn();

        DropResponse afterTamper = objectMapper.readValue(
                sabotaged.getResponse().getContentAsString(), DropResponse.class);

        assertThat(afterTamper.encryptedPayload()).isNotEqualTo(drop.encryptedPayload());
        assertThat(afterTamper.sha256Seal()).isEqualTo(drop.sha256Seal());
        assertThat(DeadDropProtocol.verifySeal(
                afterTamper.encryptedPayload(),
                afterTamper.encryptedKey(),
                afterTamper.sha256Seal())).isFalse();
    }

    @Test
    @DisplayName("transmissions missing mandatory fields are rejected with 400")
    void validationRejectsIncompleteTransmissions() throws Exception {
        mockMvc.perform(post("/api/drops")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"locationTag": "nowhere", "encryptedPayload": "", "encryptedKey": "QUJDRA=="}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TRANSMISSION REJECTED"))
                .andExpect(jsonPath("$.fields.codename").exists());
    }

    @Test
    @DisplayName("unknown drops answer 404")
    void unknownDropIsNotFound() throws Exception {
        mockMvc.perform(get("/api/drops/999999"))
                .andExpect(status().isNotFound());
    }
}
