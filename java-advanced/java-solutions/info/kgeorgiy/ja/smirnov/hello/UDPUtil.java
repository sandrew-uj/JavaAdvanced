package info.kgeorgiy.ja.smirnov.hello;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class UDPUtil {
    public static void terminationAwait(ExecutorService es, long secs) {
        if (es == null) {
            return;
        }
        es.shutdown();
        try {
            if (!es.awaitTermination(secs, TimeUnit.SECONDS)) {
                es.shutdownNow();
            }
        } catch(InterruptedException e) {
            System.err.println("Termination of thread pool was interrupted");
        }
    }
}
