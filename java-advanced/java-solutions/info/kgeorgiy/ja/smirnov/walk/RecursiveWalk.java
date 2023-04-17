package info.kgeorgiy.ja.smirnov.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class RecursiveWalk {
    public static void main(final String[] args) {
        if (args == null) {
            System.out.println("Arguments are null");
            return;
        }
        if (args.length != 2) {
            System.out.println("Invalid number of arguments");
            return;
        }
        if (args[0] == null || args[1] == null) {
            System.out.println("Invalid arguments");
            return;
        }

        final Path input = assignPath(args[0]);
        final Path output = assignPath(args[1]);

        // :NOTE: style
        if (input == null || output == null) {
            return;
        }

        try (final var reader = Files.newBufferedReader(input)) {
            try (final var writer = Files.newBufferedWriter(output)) {
                final var visitor = new WalkFileVisitor(writer, "SHA-256");
                try {
                    String file;
                    while ((file = reader.readLine()) != null) {
                        final Path filePath;
                        try {
                            filePath = Path.of(file);
                            try {
                                Files.walkFileTree(filePath, visitor);
                            } catch (final IOException e) {
                                System.out.println("Can not walk through start file: " + e.getMessage());
                            }
                        } catch (final InvalidPathException e) {
                            System.out.println("Invalid filename in input file: " + e.getMessage());
                            makeNote(INVALID, file, writer);
                        }
                    }
                } catch (final IOException e) {
                    System.out.println("Error occurred while reading from input file " + e.getMessage());
                }

            } catch (final IOException e) {
                System.out.println("Error occurred while writing to output file: " + e.getMessage());
            }
        } catch (final IOException e) {
            System.out.println("Error occurred while reading from input file: " + e.getMessage());
        }
    }

    private static Path assignPath(String file) {
        Path result = null;
        try {
            result = Path.of(file);
            if (result.getParent() != null) {
                Files.createDirectories(result.getParent());
            }
            return result;
        } catch (final InvalidPathException e) {
            System.out.println("Invalid input file path: " + e.getMessage());
            return null;
        } catch (final IOException e) {
            System.out.println("Can not create parent directory: " + e.getMessage());
            return result;
        }
    }

    private static final String INVALID = "0".repeat(64);

    static class WalkFileVisitor extends SimpleFileVisitor<Path> {
        private MessageDigest md;
        private final BufferedWriter writer;
        private static final int BUFFER_SIZE = 1024;
        private final byte[] buffer = new byte[BUFFER_SIZE];
        private static final HexFormat formatter = HexFormat.of();

        public WalkFileVisitor(final BufferedWriter writer, final String hashAlgorithm) {
            this.writer = writer;
            try {
                md = MessageDigest.getInstance(hashAlgorithm);
            } catch (final NoSuchAlgorithmException e) {
                // :NOTE: success
                System.out.println("Not such algorithm of hashing exception: " + e.getMessage());
            }
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
            // :NOTE: -> IOException
            return makeNote(getHash(file), file.toString(), writer) ? FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
        }

        @Override
        public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
            return makeNote(INVALID, file.toString(), writer) ? FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
        }

        private String getHash(final Path filePath) {
            try (final var stream = Files.newInputStream(filePath)) {
                md.reset(); // :NOTE: NPE
                for (int numRead; (numRead = stream.read(buffer)) > 0; ) {
                    md.update(buffer, 0, numRead);
                }
                return formatter.formatHex(md.digest());
            } catch (final IOException e) {
                System.out.println("Exception occurred in input stream while hashing file " + filePath);
                return INVALID;
            }
        }
    }

    private static boolean makeNote(final String hash, final String filename, final BufferedWriter writer) {
        try {
            writer.write(String.format("%s %s\n", hash, filename));
            return true;
        } catch (final IOException e) {
            System.out.println("Exception occurred while writing hash of file " + filename);
            return false;
        }
    }
}
