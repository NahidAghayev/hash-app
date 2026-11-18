import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.google.common.hash.Hashing;

public final class HashApp {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Error: Please provide the number of buckets and the maximum symbols per bucket.");
            System.err.println("Example: java HashApp 5 20");
            System.exit(1);
        }

        int bucketCount;
        int bucketSize;

        try {
            bucketCount = Integer.parseInt(args[0]);
            bucketSize = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Bucket count and bucket size must be whole numbers.");
            System.exit(1);
            return;
        }

        if (bucketCount <= 0 || bucketSize <= 0) {
            System.err.println("Bucket count and bucket size must be greater than zero.");
            System.exit(1);
        }

        System.out.printf("hash_app started with %d buckets of %d symbols each.%n", bucketCount, bucketSize);

        Path bucketDirectory = Paths.get("bucket");
        Files.createDirectories(bucketDirectory);

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        while (true) {
            System.out.print("Please enter the string: ");
            System.out.flush();
            String value = input.readLine();

            if (value == null) {
                break;
            }

            int hash = Hashing.murmur3_32_fixed()
                    .hashString(value, StandardCharsets.UTF_8)
                    .asInt();

            long unsignedHash = Integer.toUnsignedLong(hash);
            int remainder = (int) (unsignedHash % bucketCount);

            int bucketNumber = remainder == 0 ? bucketCount : remainder;

            Path bucketPath = bucketDirectory.resolve(bucketNumber + ".txt");

            String existingContent = "";
            if (Files.exists(bucketPath)) {
                existingContent = new String(
                        Files.readAllBytes(bucketPath),
                        StandardCharsets.UTF_8
                );
            }

            long existingSymbols = existingContent.codePointCount(0, existingContent.length());
            long newSymbols = value.codePointCount(0, value.length());

            if (existingSymbols + newSymbols > bucketSize) {
                System.out.printf(
                        "Bucket %d.txt is full; this string was not stored yet.%n",
                        bucketNumber
                );
                continue;
            }

            Files.write(
                    bucketPath,
                    value.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );

            System.out.printf("\"%s\" was stored in %d.txt%n", value, bucketNumber);
        }
    }

}
