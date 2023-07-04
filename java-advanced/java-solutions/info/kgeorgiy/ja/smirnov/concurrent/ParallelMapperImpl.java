package info.kgeorgiy.ja.smirnov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> ts;
    private final Queue<Runnable> queries;
    private static final int MAX_SIZE = 1000;

    public ParallelMapperImpl(int threads) {
        ts = new ArrayList<>();
        queries = new ArrayDeque<>();
        var threadAction = getThreadAction();
        for (int i = 0; i < threads; i++) {
            // :NOTE: сделать runnable singleton
            var thread = new Thread(threadAction);
            ts.add(thread);
            thread.start();
        }
    }

    private Runnable getThreadAction() {
        return () -> {
            try {
                while (!Thread.interrupted()) {
                    final Runnable query;
                    synchronized (queries) {
                        while (queries.isEmpty()) {
                            queries.wait();
                        }
                        query = queries.poll();
                        queries.notifyAll();
                    }
                    query.run();
                }
            } catch (InterruptedException ignored) {

            } finally {
                Thread.currentThread().interrupt();
            }
        };
    }

    static class ConcurrentList<R> {
        private final List<R> data;
        private int count;

        public ConcurrentList(int size) {
            data = new ArrayList<>(Collections.nCopies(size, null));
            count = 0;
        }

        public void set(int index, R value) {
            data.set(index, value);
            synchronized (this) {
                if (++count >= data.size()) {
                    notifyAll();
                }
            }
        }

        public synchronized List<R> getData() throws InterruptedException {
            while (count < data.size()) {
                wait();
            }

            return data;
        }
    }

    private void addQuery(Runnable query) throws InterruptedException {
        synchronized (queries) {
            while (queries.size() > MAX_SIZE) {
                queries.wait();
            }
            queries.add(query);
            queries.notifyAll();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args)
            throws InterruptedException {
        var result = new ConcurrentList<R>(args.size());
        var e = new RuntimeException();
        for (int i = 0; i < args.size(); i++) {
            int index = i;
            addQuery(() -> {
                        try {
                            // :NOTE: set е вызовется
                            result.set(index, f.apply(args.get(index)));
                        } catch (RuntimeException ee) {
                            result.set(index, null);
                            synchronized (e) {
                                e.addSuppressed(ee);
                            }
                        }
                    }
            );
        }

        // :NOTE: exception from f
        var data = result.getData();
        if (e.getSuppressed().length > 0) {
            throw e;
        }

        return data;
    }

    @Override
    public void close() {
        // :NOTE: надо дождаться остальных потоков
        var messages = new ArrayList<String>();
        ts.forEach(t -> {
            t.interrupt();
            try {
                t.join();
            } catch (InterruptedException e) {
                messages.add(e.getMessage());
            }
        });
        for (var message : messages) {
            System.out.println(message);
        }
    }
}
