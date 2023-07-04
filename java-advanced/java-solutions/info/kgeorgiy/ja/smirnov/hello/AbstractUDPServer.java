package info.kgeorgiy.ja.smirnov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public abstract class AbstractUDPServer implements HelloServer {
    protected static final long THREAD_TERMINATION = 1000;
    protected static byte[] getResponse(final String source) {
        // :NOTE: not parallel
        return String.format("Hello, %s", source).getBytes(StandardCharsets.UTF_8);
    }

    protected void parseArgsAndStart(final String[] args) {
        if (args == null || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Invalid arguments for main");
            return;
        }
        try {
            // :NOTE: formatting
            start(Integer.parseInt(args[0]),
                    Integer.parseInt(args[1]));
        } catch (final NumberFormatException e) {
            System.err.println("Invalid format for argument, expected int got string");
        }
    }
}
