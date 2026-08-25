package com.blackout.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification of the hand-rolled Playfair engine against manually derived vectors
 * (keyword KEYWORD, worked by hand on paper - the old-school way).
 */
class PlayfairEngineTest {

    private static String flatten(char[][] matrix) {
        StringBuilder sb = new StringBuilder();
        for (char[] row : matrix) {
            sb.append(row);
        }
        return sb.toString();
    }

    @Test
    @DisplayName("key matrix: keyword letters first, then alphabet sans J, 25 unique cells")
    void matrixIsWellFormed() {
        String flat = flatten(PlayfairEngine.buildMatrix("KEYWORD"));

        assertThat(flat).hasSize(25);
        assertThat(flat).startsWith("KEYWORD");
        assertThat(flat).doesNotContain("J");
        Set<Character> unique = new HashSet<>();
        flat.chars().forEach(c -> unique.add((char) c));
        assertThat(unique).hasSize(25);

        // keyword KEYWORD + remaining letters in alphabetical order, I/J merged
        assertThat(flat).isEqualTo("KEYWORDABCFGHILMNPQSTUVXZ");
    }

    @Test
    @DisplayName("empty keyword falls back to plain alphabet grid")
    void emptyKeywordYieldsAlphabetGrid() {
        assertThat(flatten(PlayfairEngine.buildMatrix(""))).isEqualTo("ABCDEFGHIKLMNOPQRSTUVWXYZ");
    }

    @Test
    @DisplayName("known vector: HELLO under KEYWORD -> GYIZSC (row/rect/column rules)")
    void knownVectorEncryptDecrypt() {
        assertThat(PlayfairEngine.encrypt("HELLO", "KEYWORD")).isEqualTo("GYIZSC");
        assertThat(PlayfairEngine.decrypt("GYIZSC", "KEYWORD")).isEqualTo("HELXLO"); // filler X visible
    }

    @Test
    @DisplayName("duplicate-letter padding inserts X between repeated characters")
    void duplicateLettersAreSplitByFiller() {
        // BA LX LO ON -> CB IZ SC ES
        assertThat(PlayfairEngine.encrypt("BALLOON", "KEYWORD")).isEqualTo("CBIZSCES");
        assertThat(PlayfairEngine.decrypt("CBIZSCES", "KEYWORD")).isEqualTo("BALXLOON");
    }

    @Test
    @DisplayName("odd-length message is padded with trailing X")
    void oddLengthIsPadded() {
        assertThat(PlayfairEngine.encrypt("SPY", "KEYWORD")).isEqualTo("MQWV");
        assertThat(PlayfairEngine.decrypt("MQWV", "KEYWORD")).isEqualTo("SPYX");
    }

    @Test
    @DisplayName("J is normalised to I before ciphering")
    void jNormalisesToI() {
        assertThat(PlayfairEngine.normalize("Hello, World!")).isEqualTo("HELLOWORLD");
        // IAZZ -> bigrams IA / ZX / ZX (duplicate Z split, trailing pad on final Z)
        assertThat(PlayfairEngine.decrypt(PlayfairEngine.encrypt("JAZZ", "KEYWORD"), "KEYWORD"))
                .isEqualTo("IAZXZX");
    }

    @Test
    @DisplayName("decrypt rejects odd-length ciphertext")
    void decryptRejectsOddLength() {
        assertThatThrownBy(() -> PlayfairEngine.decrypt("ABC", "KEYWORD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("even");
    }

    @Test
    @DisplayName("encrypt rejects messages with no encipherable letters")
    void encryptRejectsEmptyPayload() {
        assertThatThrownBy(() -> PlayfairEngine.encrypt("123 !!!", "KEYWORD"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
