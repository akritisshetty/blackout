import java.util.LinkedHashSet;

/*
Playfair.java

- Implements the Playfair Cipher – a digraphic substitution cipher invented by Charles Wheatstone in 1854 and popularised by Lord Playfair.

* Algorithm Overview:
1. Build a 5×5 key square using the keyword (I and J share one cell).
2. Prepare the plaintext:
- Convert to uppercase and replace J with I.
- Split into digraphs (pairs of letters).
- Insert 'X' between identical letters in a pair.
- Append 'X' if the total length is odd.
3. Encrypt each digraph using three rules:
- Same row    → shift each letter one position RIGHT (wraps).
- Same column → shift each letter one position DOWN  (wraps).
- Rectangle   → swap columns within the same rows.
4. Decryption reverses the shifts:
- Same row    → shift LEFT.
- Same column → shift UP.
- Rectangle   → same swap as encryption.
*/

public class Playfair {

    // Size of the Playfair grid
    private static final int SIZE = 5;

    // Filler character inserted between duplicate digraph letters
    private static final char FILLER = 'X';

    // Public API
    /*
     - Encrypts the given plaintext with the supplied keyword.
     
     @param plaintext  Text to encrypt (letters only; digits/punctuation ignored).
     @param key        Keyword used to build the 5×5 matrix.
     @return           Encrypted ciphertext (uppercase letters, space-separated digraphs).
     @throws IllegalArgumentException if plaintext or key is null/empty.
    */
    public String encrypt(String plaintext, String key) {
        validateInputs(plaintext, key);
        char[][] matrix = buildMatrix(key);
        String prepared = prepareText(plaintext);
        return processDigraphs(prepared, matrix, true);
    }

    /*
     - Decrypts the given ciphertext with the supplied keyword.
     
     @param ciphertext  Text to decrypt (should have been produced by encrypt()).
     @param key         Keyword used to build the 5×5 matrix.
     @return            Decrypted plaintext (filler 'X' characters may remain).
     @throws IllegalArgumentException if ciphertext or key is null/empty.
    */
    public String decrypt(String ciphertext, String key) {
        validateInputs(ciphertext, key);
        char[][] matrix = buildMatrix(key);
        // Strip spaces from ciphertext before processing
        String prepared = ciphertext.replaceAll("[^A-Za-z]", "").toUpperCase().replace('J', 'I');
        return processDigraphs(prepared, matrix, false);
    }

    /*
     - Returns a pretty-printed 5×5 representation of the Playfair matrix for the given keyword (useful for UI display).
     
     @param key  The keyword.
     @return     A formatted string showing the matrix.
    */
    public String getMatrixDisplay(String key) {
        if (key == null || key.isEmpty()) return "(enter a keyword to see the matrix)";
        char[][] matrix = buildMatrix(key);
        StringBuilder sb = new StringBuilder("  Playfair Matrix:\n");
        sb.append("  ┌───┬───┬───┬───┬───┐\n");
        for (int row = 0; row < SIZE; row++) {
            sb.append("  │");
            for (int col = 0; col < SIZE; col++) {
                sb.append(" ").append(matrix[row][col]).append(" │");
            }
            sb.append("\n");
            if (row < SIZE - 1) sb.append("  ├───┼───┼───┼───┼───┤\n");
        }
        sb.append("  └───┴───┴───┴───┴───┘");
        return sb.toString();
    }

    // Matrix construction
    
    /*
     - Builds the 5×5 Playfair key matrix from the supplied keyword.
     - Rules:
        - Use only unique letters from the keyword (first occurrence wins).
        - I and J are treated as the same letter (J → I).
        - Fill remaining cells with unused letters of the alphabet in order.
     
     @param key  The keyword string.
     @return     A 5×5 char array representing the matrix.
    */
    private char[][] buildMatrix(String key) {
        // Use a LinkedHashSet to preserve insertion order and ensure uniqueness
        LinkedHashSet<Character> seen = new LinkedHashSet<>();

        // Add keyword letters first (normalise: uppercase, J→I, letters only)
        for (char ch : key.toUpperCase().toCharArray()) {
            if (Character.isLetter(ch)) {
                seen.add(ch == 'J' ? 'I' : ch);
            }
        }

        // Add remaining alphabet letters (skipping J)
        for (char ch = 'A'; ch <= 'Z'; ch++) {
            if (ch != 'J') seen.add(ch);
        }

        // Fill the 5×5 matrix
        char[][] matrix = new char[SIZE][SIZE];
        int idx = 0;
        for (char ch : seen) {
            matrix[idx / SIZE][idx % SIZE] = ch;
            idx++;
        }
        return matrix;
    }

    // Text preparation

    /*
     - Prepares plaintext for Playfair encryption:
        1. Converts to uppercase.
        2. Replaces J with I.
        3. Removes non-letter characters.
        4. Inserts 'X' between identical letters in a digraph.
        5. Pads with 'X' if length is odd.
     
     @param text  Raw plaintext.
     @return      Prepared uppercase string of even length, ready to be split into digraphs.
    */
    private String prepareText(String text) {
        // Step 1-3: normalise
        String clean = text.toUpperCase().replace('J', 'I').replaceAll("[^A-Z]", "");

        // Step 4: insert filler between duplicate digraph letters
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < clean.length()) {
            char first = clean.charAt(i);
            sb.append(first);

            if (i + 1 < clean.length()) {
                char second = clean.charAt(i + 1);
                if (first == second) {
                    // Same letter pair → insert filler and advance only one position
                    sb.append(FILLER);
                    i++; // Only consume 'first'; 'second' will be re-read next iteration
                } else {
                    sb.append(second);
                    i += 2;
                }
            } else {
                i++;
            }
        }

        // Step 5: pad to even length
        if (sb.length() % 2 != 0) {
            sb.append(FILLER);
        }

        return sb.toString();
    }

    // Digraph processing (shared by encrypt and decrypt)

    /*
     - Processes a prepared text string as Playfair digraphs.
     
     @param text       Normalised, even-length uppercase text.
     @param matrix     The 5×5 Playfair matrix.
     @param encrypting {@code true} to encrypt; {@code false} to decrypt.
     @return           The resulting ciphertext / plaintext (space-separated digraphs).
    */
    private String processDigraphs(String text, char[][] matrix, boolean encrypting) {
        // Direction: +1 for encrypt (right/down), -1 for decrypt (left/up)
        int shift = encrypting ? 1 : -1;

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < text.length(); i += 2) {
            char a = text.charAt(i);
            char b = text.charAt(i + 1);

            int[] posA = findPosition(matrix, a);
            int[] posB = findPosition(matrix, b);

            int rowA = posA[0], colA = posA[1];
            int rowB = posB[0], colB = posB[1];

            char encA, encB;

            if (rowA == rowB) {
                // --- Same row: shift columns ---
                encA = matrix[rowA][(colA + shift + SIZE) % SIZE];
                encB = matrix[rowB][(colB + shift + SIZE) % SIZE];

            } else if (colA == colB) {
                // --- Same column: shift rows ---
                encA = matrix[(rowA + shift + SIZE) % SIZE][colA];
                encB = matrix[(rowB + shift + SIZE) % SIZE][colB];

            } else {
                // --- Rectangle: swap columns ---
                encA = matrix[rowA][colB];
                encB = matrix[rowB][colA];
            }

            if (result.length() > 0) result.append(" ");
            result.append(encA).append(encB);
        }

        return result.toString();
    }

    // Helper: locate a character in the matrix

    /*
     - Finds the [row, column] position of a character in the 5×5 matrix.
     
     @param matrix  The Playfair matrix.
     @param ch      Character to find (must be uppercase A-Z, not J).
     @return        int[]{row, col} – zero-indexed.
    */
    private int[] findPosition(char[][] matrix, char ch) {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (matrix[row][col] == ch) {
                    return new int[]{row, col};
                }
            }
        }
        // Should never reach here if the text was properly prepared
        throw new IllegalStateException("Character '" + ch + "' not found in Playfair matrix.");
    }

    // Input validation

    /*
     - Validates that both text and key are non-null and non-empty.
     
     @param text  Input text.
     @param key   Cipher keyword.
     @throws IllegalArgumentException on invalid input.
    */
    private void validateInputs(String text, String key) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be empty.");
        }
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Playfair key cannot be empty. Please enter a keyword.");
        }
        // Ensure at least one letter exists in the text
        if (!text.matches(".*[A-Za-z].*")) {
            throw new IllegalArgumentException("Input text must contain at least one letter.");
        }
    }
}
