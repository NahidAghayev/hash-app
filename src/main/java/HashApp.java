import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.DirectoryStream;

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
        deleteBucketDirectory(bucketDirectory);
        Files.createDirectories(bucketDirectory);

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        while (true) {
            System.out.print("Please enter the string: ");
            System.out.flush();
            String value = input.readLine();

            if (value == null) {
                break;
            }

            long newSymbols = value.codePointCount(0, value.length());

            if (newSymbols > bucketSize) {
                System.out.println("This string is too large to fit in any bucket file.");
                continue;
            }

            int hash = Hashing.murmur3_32_fixed()
                    .hashString(value, StandardCharsets.UTF_8)
                    .asInt();

            long unsignedHash = Integer.toUnsignedLong(hash);
            int remainder = (int) (unsignedHash % bucketCount);

            int bucketNumber = remainder == 0 ? bucketCount : remainder;

            Path bucketPath = bucketDirectory.resolve(bucketNumber + ".txt");
            Path currentFile = bucketPath;

            while (true) {
                Path referenceFile = getReferenceFile(currentFile);

                if (Files.exists(referenceFile)) {
                    currentFile = readNextFile(referenceFile);
                    continue;
                }

                String existingContent = "";
                if (Files.exists(currentFile)) {
                    existingContent = new String(
                            Files.readAllBytes(currentFile),
                            StandardCharsets.UTF_8
                    );
                }

                long existingSymbols = existingContent.codePointCount(0, existingContent.length());

                if (existingSymbols + newSymbols <= bucketSize) {
                    break;
                }

                currentFile = createAndLinkOverflowFile(currentFile, bucketDirectory, bucketNumber);

            }

            Files.write(currentFile,
                    value.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );

            System.out.printf("\"%s\" was stored in %s%n", value, currentFile.getFileName());
        }
    }

    private static Path getReferenceFile(Path dataFile) {
        String referenceFile = dataFile.getFileName().toString() + ".next";
        return dataFile.resolveSibling(referenceFile);
    }

    private static Path readNextFile(Path referenceFile) throws IOException {
        String nextFile = new String (Files.readAllBytes(referenceFile), StandardCharsets.UTF_8);
        return referenceFile.resolveSibling(nextFile);
    }

    private static Path createAndLinkOverflowFile
            (Path currentFile, Path bucketDirectory, int bucketNumber) throws IOException {
        int overflowNumber = 1;
        String overflowName = bucketNumber + "_overflow_" + overflowNumber + ".txt";
        Path overflowFile = bucketDirectory.resolve(overflowName);

        while (Files.exists(overflowFile)) {
            overflowNumber++;
            overflowName = bucketNumber + "_overflow_" + overflowNumber + ".txt";
            overflowFile = bucketDirectory.resolve(overflowName);
        }

        Files.createFile(overflowFile);

        Path referenceFile = getReferenceFile(currentFile);
        String nextFileName = overflowFile.getFileName().toString();

        Files.write(
                referenceFile,
                nextFileName.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );

        return overflowFile;

    }

    private static void deleteBucketDirectory(Path bucketDirectory) throws IOException {
        if (Files.exists(bucketDirectory)) {
            try (DirectoryStream<Path> entries =
                         Files.newDirectoryStream(bucketDirectory)) {
                for (Path entry : entries) {
                    Files.delete(entry);
                }
            }

            Files.delete(bucketDirectory);
        }
    }

}
