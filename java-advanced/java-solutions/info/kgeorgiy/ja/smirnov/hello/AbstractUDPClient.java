package info.kgeorgiy.ja.smirnov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

public abstract class AbstractUDPClient implements HelloClient {
    protected static final long REQUEST_WAIT = 5L;
    protected static final int SOCKET_TIME = 200;

    protected static boolean isCorrect(String received, String pref, int thread, int request) {
        var match = Pattern.compile(pref).matcher(received);

        if (!match.find()) {
            return false;
        }

        var nums = received.substring(match.end()).split("_");
        if (nums.length != 2) {
            return false;
        }

        try {
            if (Integer.parseInt(nums[0]) != thread) {
                return false;
            }
            if (Integer.parseInt(nums[1]) != request) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    protected void parseArgsAndRun(String[] args) {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Invalid arguments for main");
            return;
        }
        try {
            run(
                    args[0],
                    Integer.parseInt(args[1]),
                    args[2],
                    Integer.parseInt(args[3]),
                    Integer.parseInt(args[4])
            );
        } catch (NumberFormatException e) {
            System.err.println("Invalid format for argument, expected int got string");
        }
    }

    protected static String getMessage(String prefix, int thread, int request) {
        return String.format("%s%d_%d", prefix, thread, request);
    }
}
