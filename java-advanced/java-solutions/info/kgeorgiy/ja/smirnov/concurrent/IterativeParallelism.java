package info.kgeorgiy.ja.smirnov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {
    private final ParallelMapper mapper;

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    public IterativeParallelism() {
        mapper = null;
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return query(threads, values,
                s -> s.map(Object::toString).collect(Collectors.joining()),
                s -> s.collect(Collectors.joining()));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return query(threads, values,
                s -> s.filter(predicate).collect(Collectors.toList()),
                s -> s.flatMap(List::stream).collect(Collectors.toList()));
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return query(threads, values,
                s -> s.map(f).collect(Collectors.toList()),
                s -> s.flatMap(List::stream).toList());
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        return query(threads, values,
                s -> s.max(comparator).orElse(null),
                s -> s.max(comparator).orElse(null));
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return query(threads, values,
                s -> s.allMatch(predicate),
                s -> s.allMatch(a -> a));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return query(threads, values,
                s -> s.anyMatch(predicate),
                s -> s.anyMatch(a -> a));
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return query(threads, values,
                s -> s.filter(predicate).count(),
                s -> s.reduce(0L, Long::sum)).intValue();
    }

    private <T> List<Stream<? extends T>> divideList(int threads, List<? extends T> values) {
        int n = Math.min(threads, values.size());
        // :NOTE: деление на ноль
        int blockSize = n > 0 ? values.size() / n : 0;
        int tailSize = values.size() - n * blockSize;
        var subStreams = new ArrayList<Stream<? extends T>>();

        for (int i = 0, currentIdx = 0; i < n; i++) {
            int currentSize = blockSize + (tailSize > 0 ? 1 : 0);
            --tailSize;
            int finalCurrentIdx = currentIdx;
            var subStream = values.subList(finalCurrentIdx,
                            finalCurrentIdx + currentSize)
                    .stream();
            subStreams.add(subStream);
            currentIdx += currentSize;
        }
        return subStreams;
    }

    private <T, R> R query(int threads, List<? extends T> values, final Function<Stream<? extends T>, ? extends R> function,
                           final Function<Stream<R>, ? extends R> foldRes) throws InterruptedException {
        var subStreams = divideList(threads, values);

        List<R> result;
        if (mapper != null) {
            result = mapper.map(function, subStreams);
        } else {
            // :NOTE: -> List<Thread>
            final var res = new ArrayList<R>(Collections.nCopies(subStreams.size(), null));
            var ts = new ArrayList<Thread>();
            for (int i = 0; i < subStreams.size(); i++) {
                int finalI = i;
                var thread = new Thread(() -> res.set(finalI,
                        function.apply(subStreams.get(finalI))));

                ts.add(thread);
                thread.start();
            }
            result = res;
            // :NOTE: прокинуть интеррапт в другие потоки
            for (var thread : ts) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    for (var t : ts) {
                        t.interrupt(); // :NOTE: ещё раз сделать join
                        try {
                            t.join();
                        } catch (InterruptedException ee) {
                            e.addSuppressed(ee);
                        }
                    }
                    throw e;
                }
            }
        }

        return foldRes.apply(result.stream());
    }
}
