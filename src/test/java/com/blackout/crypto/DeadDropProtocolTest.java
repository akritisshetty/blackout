package com.blackout.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeadDropProtocolTest {

    /** Reference digest of the empty string, straight from the FIPS test vectors. */
    private static final String SHA256_OF_EMPTY =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @Test
    @DisplayName("Sha256Engine matches the FIPS 180 empty-string vector")
    void sha256MatchesReferenceVector() {
        assertThat(Sha256Engine.sha256Hex("")).isEqualTo(SHA256_OF_EMPTY);
        assertThat(Sha256Engine.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    @DisplayName("seals are deterministic and exactly 64 lowercase hex characters")
    void sealIsStableHex() {
        String seal = DeadDropProtocol.computeSeal("GYIZSC", "QUJDRA==");

        assertThat(seal).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(DeadDropProtocol.computeSeal("GYIZSC", "QUJDRA==")).isEqualTo(seal);
        assertThat(DeadDropProtocol.computeSeal("GYIZSX", "QUJDRA==")).isNotEqualTo(seal);
    }

    @Test
    @DisplayName("verification passes for intact packages and fails after any mutation")
    void verificationDetectsTampering() {
        String payload = "GYIZSC";
        String key = "QUJDRA==";
        String seal = DeadDropProtocol.computeSeal(payload, key);

        assertThat(DeadDropProtocol.verifySeal(payload, key, seal)).isTrue();

        // one flipped character anywhere in the package must break the seal
        assertThat(DeadDropProtocol.verifySeal("GYIZS#", key, seal)).isFalse();
        assertThat(DeadDropProtocol.verifySeal(payload, "QUJERA==", seal)).isFalse();
        assertThat(DeadDropProtocol.verifySeal(payload, key, null)).isFalse();
        assertThat(DeadDropProtocol.verifySeal(payload, key, "")).isFalse();
    }

    @Test
    @DisplayName("canonical package joins fields with the documented delimiter")
    void canonicalFormIsDocumented() {
        assertThat(DeadDropProtocol.canonicalPackage("AA", "BB")).isEqualTo("AA|BB");
    }
}
