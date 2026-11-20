# hash_app

`hash_app` is a small Java program that reads strings from the console, hashes each one, and stores it in a bucket file. If that file is full, the program continues in an overflow file linked to the bucket.

## Run the program

You need Java 8 or newer. From the project folder, build and run the app in PowerShell:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd exec:java '-Dexec.args=5 20'
```

The example starts the app with five buckets and a limit of twenty symbols per data file. Enter one string per line. The app keeps prompting until you press Ctrl+C or send end-of-input.

The first run may need an internet connection so the Maven Wrapper can download Maven and Guava. You do not need to install Maven separately.

## Hashing and bucket selection

The app uses Guava's fixed 32-bit Murmur3 function to hash each string as UTF-8. It converts the returned Java `int` to an unsigned value before taking the remainder modulo the number of buckets.

The remainder selects the bucket file. Since the remainder ranges from zero to one less than the bucket count, this app maps remainder zero to the last bucket. For example, with five buckets, remainder zero goes to `5.txt`, while remainder three goes to `3.txt`.

## Bucket files and overflow links

Data files are created in the `bucket` folder. Each file can hold up to the configured number of Unicode code points. Strings are appended directly, without spaces or other separators. If a single input is longer than the file limit, the app skips it because it cannot fit in any bucket file.

When a data file cannot fit the next string, the app creates an overflow file. A separate `.next` file stores the name of the next data file, keeping link information out of the bucket contents. For example:

```text
3.txt.next             contains: 3_overflow_1.txt
3_overflow_1.txt.next  contains: 3_overflow_2.txt
```

For each new string, the app follows the links to the last file in the chain. It writes there if the string fits; otherwise, it creates and links another overflow file.

The app clears and recreates the `bucket` folder each time it starts, so each run begins with empty buckets.
