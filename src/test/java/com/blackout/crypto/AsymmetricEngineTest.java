package com.blackout.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AsymmetricEngineTest {

    @Test
    @DisplayName("RSA-2048 round trip: encrypt with public, decrypt with private")
    void roundTripRecoversPlaintext() {
        KeyPair badge = AsymmetricEngine.generateKeyPair();
        String secret = "playfair-keyword-7";

        String sealed = AsymmetricEngine.encrypt(secret, badge.getPublic());

        assertThat(sealed).isBase64();
        assertThat(AsymmetricEngine.decrypt(sealed, badge.getPrivate())).isEqualTo(secret);
    }

    @Test
    @DisplayName("blobs decrypt under strict OAEP(SHA-256, MGF1-SHA-256) - the WebCrypto parameters")
    void blobMatchesWebCryptoOaepParameters() throws Exception {
        KeyPair badge = AsymmetricEngine.generateKeyPair();
        String sealed = AsymmetricEngine.encrypt("obsidian", badge.getPublic());

        // independent cipher configured exactly like the browser's RSA-OAEP + SHA-256:
        // if the engine ever drifts back to the lazy transformation (MGF1 defaults to
        // SHA-1 on the stock provider), this decryption fails with BadPadding.
        Cipher strict = Cipher.getInstance("RSA/ECB/OAEPPadding");
        strict.init(Cipher.DECRYPT_MODE, badge.getPrivate(), new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));

        byte[] plain = strict.doFinal(Base64.getDecoder().decode(sealed));

        assertThat(new String(plain, "UTF-8")).isEqualTo("obsidian");
    }

    @Test
    @DisplayName("OAEP randomisation: identical plaintexts seal to different ciphertexts")
    void encryptionIsRandomised() {
        KeyPair badge = AsymmetricEngine.generateKeyPair();
        PublicKey pub = badge.getPublic();

        String first = AsymmetricEngine.encrypt("same-secret", pub);
        String second = AsymmetricEngine.encrypt("same-secret", pub);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("decrypting with the wrong badge fails loudly, never silently")
    void wrongKeyIsRejected() {
        KeyPair alice = AsymmetricEngine.generateKeyPair();
        KeyPair mallory = AsymmetricEngine.generateKeyPair();

        String sealed = AsymmetricEngine.encrypt("classified", alice.getPublic());

        assertThatThrownBy(() -> AsymmetricEngine.decrypt(sealed, mallory.getPrivate()))
                .isInstanceOf(CryptoOperationException.class);
    }

    @Test
    @DisplayName("encoded keys survive a Base64 dossier round trip")
    void encodedKeysParseBack() {
        KeyPair badge = AsymmetricEngine.generateKeyPair();

        String pubB64 = AsymmetricEngine.encodePublicKey(badge.getPublic());
        String privB64 = AsymmetricEngine.encodePrivateKey(badge.getPrivate());

        assertThat(Base64.getDecoder().decode(pubB64)).isNotEmpty();

        assertDoesNotThrow(() -> AsymmetricEngine.decodePublicKey(pubB64));
        assertDoesNotThrow(() -> AsymmetricEngine.decodePrivateKey(privB64));

        assertThat(AsymmetricEngine.decodePublicKey(pubB64).getEncoded())
                .isEqualTo(badge.getPublic().getEncoded());
    }

    @Test
    @DisplayName("plaintext beyond OAEP capacity is refused up front")
    void oversizePlaintextIsRejected() {
        KeyPair badge = AsymmetricEngine.generateKeyPair();
        String tooBig = "X".repeat(300);

        assertThatThrownBy(() -> AsymmetricEngine.encrypt(tooBig, badge.getPublic()))
                .isInstanceOf(CryptoOperationException.class)
                .hasMessageContaining("capacity");
    }
}
