package com.algo.finalproject.webcrawler.service;

import com.algo.finalproject.webcrawler.WebcrawlerApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@Service
public class CrawlerService {

    private final PriorityBlockingQueue<String> urlQueue;
    private final Set<String> visitedUrls;
    private final ExecutorService executorService;
    private static final Logger logger = LogManager.getLogger(WebcrawlerApplication.class);
    private final Semaphore semaphore;

    public CrawlerService() {
        urlQueue = new PriorityBlockingQueue<>(11,new UrlComparator());
        visitedUrls = ConcurrentHashMap.newKeySet();
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        semaphore = new Semaphore(Runtime.getRuntime().availableProcessors());
    }

    public void crawl(List<String> urls) {
        urlQueue.addAll(urls);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        do {
            while (!urlQueue.isEmpty()) {
                String currentUrl = urlQueue.poll();
                if (currentUrl == null || visitedUrls.contains(currentUrl)) {
                    continue;
                }
                visitedUrls.add(currentUrl);
                try {
                    semaphore.acquire();
                    futures.add(CompletableFuture.runAsync(() -> {
                        try {
                            processUrl(currentUrl);
                        } finally {
                            semaphore.release();
                        }
                    }, executorService).exceptionally(ex -> {
                        logger.error("Error processing URL: " + currentUrl, ex);
                        return null;
                    }));
                } catch (InterruptedException e) {
                    logger.error("Interrupted while acquiring semaphore", e);
                    Thread.currentThread().interrupt();
                }
            }
        } while (!futures.stream().allMatch(CompletableFuture::isDone) || !urlQueue.isEmpty());
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.join();
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for executor service to terminate", e);
            Thread.currentThread().interrupt();
        }
    }
    private void processUrl(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String absUrl = link.absUrl("href");
                if (isValidUrl(absUrl)) {
                    urlQueue.offer(absUrl);
                    logger.info("Added URL to queue: " + absUrl);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to fetch URL: " + url, e);
        }
    }

    private boolean isValidUrl(String url) {
        if (url.isEmpty() || visitedUrls.contains(url)) {
            return false;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return false;
        }

        return !url.contains("javascript:") && !url.contains(".onion") && !url.startsWith("tel:");
    }


    private static class UrlComparator implements Comparator<String> {

        private int calculatePriority(String url) {
            int priority = 0;

            if (url.contains("metmuseum.org") || url.contains("nps.gov") || url.contains("mfa.org")) {
                priority += 10;
            }

            // Higher priority for specific content types
            if (url.contains("/exhibitions/") || url.contains("/collections/") || url.contains("/education/")) {
                priority += 5;
            }

            // Higher priority for URLs closer to the root
            int depth = url.split("/").length;
            priority += (10 - depth);

            return priority;
        }
        @Override
        public int compare(String url1, String url2) {
            return calculatePriority(url2) - calculatePriority(url1);
        }
    }
}