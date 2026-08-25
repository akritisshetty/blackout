public class TestSHA256 {

    public static void main(String[] args) {
        SHA256 sha = new SHA256();

        String input = "Hello World";

        System.out.println("Input : " + input);
        System.out.println("Hash  : " + sha.hash(input));

        System.out.println("\nDetailed Output:");
        System.out.println(sha.hashWithDetails(input));
    }
}
