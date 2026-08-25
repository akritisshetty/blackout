package com.blackout.crypto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * BLACKOUT // PlayfairEngine
 *
 * A from-scratch implementation of the classical Playfair cipher (Wheatstone, 1854).
 * No third-party crypto libraries - just letters, a 5x5 grid and arithmetic.
 *
 * Rules implemented:
 *   1. Dynamic 5x5 key matrix generated from an arbitrary keyword; 'I' and 'J' share a cell.
 *   2. Plaintext sanitisation: uppercase, strip non-letters, map J to I.
 *   3. Bigram splitting. Duplicate letters inside a pair are separated by an 'X' filler
 *      (or 'Q' when the duplicate letter is itself 'X'); a trailing lone letter is padded with 'X'.
 *   4. Same row    -> shift RIGHT to encrypt, LEFT to decrypt.
 *   5. Same column -> shift DOWN to encrypt, UP to decrypt.
 *   6. Rectangle   -> each letter is replaced by the letter in its own row and the
 *                     other letter's column (columns are mirrored).
 *
 * Known classical limitations (documented, not bugs): the cipher drops spacing,
 * punctuation and case, and cannot distinguish I from J. Decryption therefore returns
 * the padded, sanitised plaintext - fillers such as 'X' remain visible, exactly as in
 * the original field manuals.
 *
 * Thread-safety: stateless static utility, safe for concurrent use.
 */
public final class PlayfairEngine {

    private static final int GRID = 5;
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** Filler inserted between duplicate letters of a bigram. */
    private static final char PRIMARY_FILLER = 'X';
    /** Alternate filler used when the duplicate letter would itself pair with 'X'. */
    private static final char SECONDARY_FILLER = 'Q';

    private PlayfairEngine() {
        // static utility - never instantiated
    }

    /* ------------------------------------------------------------------
     * Public API
     * ------------------------------------------------------------------ */

    /**
     * Builds the 5x5 key matrix for a keyword: unique keyword letters first
     * (in order of first appearance), then the remaining alphabet with J merged into I.
     * Returned copy may be freely inspected by UI layers (grid visualizer).
     */
    public static char[][] buildMatrix(String keyword) {
        StringBuilder sequence = new StringBuilder(GRID * GRID);
        boolean[] used = new boolean[26];

        for (char c : normalize(keyword).toCharArray()) {
            markUsed(sequence, used, c);
        }
        for (char c : ALPHABET.toCharArray()) {
            markUsed(sequence, used, c);
        }

        char[][] matrix = new char[GRID][GRID];
        for (int i = 0; i < GRID * GRID; i++) {
            matrix[i / GRID][i % GRID] = sequence.charAt(i);
        }
        return matrix;
    }

    /**
     * Encrypts arbitrary text under the supplied keyword.
     * Output is uppercase letters only, always an even length.
     */
    public static String encrypt(String plaintext, String keyword) {
        char[][] matrix = buildMatrix(keyword);
        StringBuilder out = new StringBuilder();
        for (char[] bigram : toBigrams(normalize(plaintext))) {
            out.append(transform(bigram[0], bigram[1], matrix, true));
        }
        return out.toString();
    }

    /**
     * Decrypts Playfair ciphertext produced by {@link #encrypt}. The ciphertext must be
     * an even-length string; it is NOT re-padded (padding exists only in encryption).
     * The result still contains inserted fillers ('X'/'Q') - classic Playfair behaviour.
     */
    public static String decrypt(String ciphertext, String keyword) {
        String clean = normalize(ciphertext);
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Ciphertext is empty - nothing to decrypt.");
        }
        if (clean.length() % 2 != 0) {
            throw new IllegalArgumentException(
                    "Ciphertext length must be even; Playfair operates strictly on bigrams.");
        }
        char[][] matrix = buildMatrix(keyword);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < clean.length(); i += 2) {
            out.append(transform(clean.charAt(i), clean.charAt(i + 1), matrix, false));
        }
        return out.toString();
    }

    /**
     * Renders the padded bigram stream for a message/keyword pair without encrypting -
     * feeds the "OUTGOING BIGRAM STREAM" readout in the grid visualizer panel.
     */
    public static String bigramPreview(String plaintext, String keyword) {
        StringBuilder sb = new StringBuilder();
        for (char[] bigram : toBigrams(normalize(plaintext))) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(bigram[0]).append(bigram[1]);
        }
        return sb.toString();
    }

    /**
     * Sanitisation used across the whole workflow: uppercase, keep A-Z only, map J to I.
     * Exposed publicly because tests and UI previews need the exact same normalisation
     * the engines apply internally.
     */
    public static String normalize(String raw) {
        String source = raw == null ? "" : raw.toUpperCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(source.length());
        for (char c : source.toCharArray()) {
            if (c < 'A' || c > 'Z') {
                continue;
            }
            sb.append(c == 'J' ? 'I' : c);
        }
        return sb.toString();
    }

    /* ------------------------------------------------------------------
     * Internals
     * ------------------------------------------------------------------ */

    private static void markUsed(StringBuilder sequence, boolean[] used, char c) {
        if (c == 'J') {
            c = 'I'; // defensive: normalize already maps J away
        }
        int index = c - 'A';
        if (!used[index]) {
            used[index] = true;
            sequence.append(c);
        }
    }

    private static List<char[]> toBigrams(String clean) {
        List<char[]> bigrams = new ArrayList<>();
        int i = 0;
        while (i < clean.length()) {
            char a = clean.charAt(i);
            if (i == clean.length() - 1) {
                // trailing lone letter -> pad
                bigrams.add(new char[]{a, PRIMARY_FILLER});
                break;
            }
            char b = clean.charAt(i + 1);
            if (a == b) {
                // duplicate letters -> inject filler, re-process second occurrence
                char filler = (a == PRIMARY_FILLER) ? SECONDARY_FILLER : PRIMARY_FILLER;
                bigrams.add(new char[]{a, filler});
                i += 1;
            } else {
                bigrams.add(new char[]{a, b});
                i += 2;
            }
        }
        if (bigrams.isEmpty()) {
            throw new IllegalArgumentException("Message contains no encipherable letters (A-Z).");
        }
        return bigrams;
    }

    private static String transform(char a, char b, char[][] matrix, boolean forward) {
        int[] pa = locate(matrix, a);
        int[] pb = locate(matrix, b);

        // +1 down/right when encrypting; (5-1)%5 == -1 up/left when decrypting.
        int shift = forward ? 1 : GRID - 1;
        int r1 = pa[0], c1 = pa[1];
        int r2 = pb[0], c2 = pb[1];

        if (r1 == r2) {                       // rule 4: same row
            c1 = (c1 + shift) % GRID;
            c2 = (c2 + shift) % GRID;
        } else if (c1 == c2) {                // rule 5: same column
            r1 = (r1 + shift) % GRID;
            r2 = (r2 + shift) % GRID;
        } else {                              // rule 6: rectangle -> mirror columns
            int tmp = c1;
            c1 = c2;
            c2 = tmp;
        }
        return String.valueOf(new char[]{matrix[r1][c1], matrix[r2][c2]});
    }

    private static int[] locate(char[][] matrix, char c) {
        for (int row = 0; row < GRID; row++) {
            for (int col = 0; col < GRID; col++) {
                if (matrix[row][col] == c) {
                    return new int[]{row, col};
                }
            }
        }
        throw new IllegalStateException("Character '" + c + "' missing from key matrix - programming error.");
    }
}
