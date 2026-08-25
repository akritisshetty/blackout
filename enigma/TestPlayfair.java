public class TestPlayfair {

    public static void main(String[] args) {
        Playfair playfair = new Playfair();

        String key = "MONARCHY";
        String plaintext = "HELLO WORLD";

        String encrypted = playfair.encrypt(plaintext, key);
        String decrypted = playfair.decrypt(encrypted, key);

        System.out.println("Keyword   : " + key);
        System.out.println();
        System.out.println(playfair.getMatrixDisplay(key));
        System.out.println();
        System.out.println("Plaintext : " + plaintext);
        System.out.println("Encrypted : " + encrypted);
        System.out.println("Decrypted : " + decrypted);
    }
}
