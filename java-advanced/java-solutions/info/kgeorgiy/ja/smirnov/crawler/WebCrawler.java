package info.kgeorgiy.ja.smirnov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {
    private final ExecutorService downloaders;
    private final ExecutorService extractors;
    private final Downloader downloader;
    private final int perHost;
    private final Map<String, CrawlerHost> hosts;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
        this.hosts = new ConcurrentHashMap<>();
    }

    private static boolean checkMainArgs(String[] args) {
        if (args == null) {
            return false;
        }
        if (args.length < 5) {
            return false;
        }
        for (String arg : args) {
            if (arg == null) {
                return false;
            }
        }
        return true;
    }

    private static int parseArg(String arg) throws NumberFormatException {
        return Integer.parseInt(arg);
    }

    public static void main(String[] args) {
        if (!checkMainArgs(args)) {
            System.err.println("Invalid arguments for main");
            return;
        }
        try (var crawler = new WebCrawler(
                new CachingDownloader(1.0),
                parseArg(args[2]),
                parseArg(args[3]),
                parseArg(args[4]))) {
            crawler.download(args[0], parseArg(args[1]));
        } catch (NumberFormatException e) {
            System.err.println("Number format exception: expected numbers but got strings");
        } catch (IOException e) {
            System.err.println("Exception while creating downloader");
        }
    }

    private static class DownloadData {
        private final Set<String> downloadedUrls = ConcurrentHashMap.newKeySet();
        private final Set<String> cached = ConcurrentHashMap.newKeySet();
        private final Map<String, IOException> errors = new ConcurrentHashMap<>();
        private final Phaser phaser = new Phaser(1);
    }

    @Override
    public Result download(String url, int depth) {
        var downloadData = new DownloadData();
        downloadData.cached.add(url);
        Queue<String> before = new ConcurrentLinkedDeque<>();
        Queue<String> current = new ConcurrentLinkedDeque<>();
        before.add(url);
        for (int d = depth; d > 0; --d) {
            for (String beforeUrl : before) {
                download(beforeUrl, d, downloadData, current);
            }
            downloadData.phaser.arriveAndAwaitAdvance();
            before.clear();
            before.addAll(current);
            current.clear();
        }
        return new Result(downloadData.downloadedUrls.stream().toList(), downloadData.errors);
    }

    private void download(String url, int depth,
                          DownloadData downloadData, Queue<String> queue) {
        try {
            String hostName = URLUtils.getHost(url);
            var crawlerHost = hosts.computeIfAbsent(hostName, name -> new CrawlerHost());
            downloadData.phaser.register();
            crawlerHost.addQuery(downloadQuery(crawlerHost, url, depth, downloadData, queue));
        } catch (MalformedURLException e) {
            downloadData.errors.put(url, e);
        }
    }

    private Runnable downloadQuery(CrawlerHost crawlerHost, String url, int depth,
                                   DownloadData downloadData, Queue<String> queue) {
        return () -> {
            try {
                var document = downloader.download(url);
                downloadData.downloadedUrls.add(url);
                downloadData.cached.add(url);
                if (depth > 1) {
                    downloadData.phaser.register();
                    extractors.submit(extractQuery(document, url, downloadData, queue));
                }
            } catch (IOException e) {
                downloadData.errors.put(url, e);
            } finally {
                downloadData.phaser.arriveAndDeregister();
                crawlerHost.runQuery();
            }
        };
    }

    private Runnable extractQuery(Document document, String url,
                                  DownloadData downloadData, Queue<String> queue) {
        return () -> {
            try {
                document.extractLinks().forEach(link -> {
                    if (downloadData.cached.add(link)) {
                        queue.add(link);
                    }
                });
            } catch (IOException e) {
                System.err.println("Exception while extracting links from url: " + url);
            } finally {
                downloadData.phaser.arriveAndDeregister();
            }
        };
    }
    private class CrawlerHost {
        private int inWork = 0;
        private final Queue<Runnable> notInRun = new ConcurrentLinkedDeque<>();

        private void addQuery(Runnable query) {
            if (inWork < perHost) {
                downloaders.submit(query);
                inWork++;
            } else {
                notInRun.add(query);
            }
        }

        private void runQuery() {
            var query = notInRun.poll();
            if (query != null) {
                downloaders.submit(query);
            } else {
                inWork--;
            }
        }
    }

    @Override
    public void close() {
        downloaders.shutdown();
        extractors.shutdown();
        try {
            if (!downloaders.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                downloaders.shutdownNow();
            }
            if (!extractors.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                extractors.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.err.println("await is interrupted");
        }
    }
}
