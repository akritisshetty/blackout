public class TestRSA {
    public static void main(String[] args) {
        RSA rsa = new RSA();

        String plaintext = "Hello World";

        String encrypted = rsa.encrypt(plaintext);
        String decrypted = rsa.decrypt(encrypted);

        System.out.println("Original : " + plaintext);
        System.out.println("Encrypted: " + encrypted);
        System.out.println("Decrypted: " + decrypted);
    }
}
