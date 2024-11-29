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
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@Service
public class CrawlerService {

    private final ConcurrentLinkedQueue<String> urlQueue;
    private final Set<String> visitedUrls;
    private final ExecutorService executorService;
    private static final Logger logger = LogManager.getLogger(WebcrawlerApplication.class);
    private final Semaphore semaphore;

    public CrawlerService() {
        urlQueue = new ConcurrentLinkedQueue<>();
        visitedUrls = ConcurrentHashMap.newKeySet();
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        semaphore = new Semaphore(Runtime.getRuntime().availableProcessors());
    }

    public void crawl(List<String> urls) {
        urlQueue.addAll(urls);
        while (!urlQueue.isEmpty()) {
            String currentUrl = urlQueue.poll();
            if (currentUrl == null || visitedUrls.contains(currentUrl)) {
                continue;
            }
            visitedUrls.add(currentUrl);
            try {
                semaphore.acquire();
                CompletableFuture.runAsync(() -> {
                    try {
                        processUrl(currentUrl);
                    } finally {
                        semaphore.release();
                    }
                }, executorService).exceptionally(ex -> {
                    logger.error("Error processing URL: " + currentUrl, ex);
                    return null;
                });
            } catch (InterruptedException e) {
                logger.error("Interrupted while acquiring semaphore", e);
                Thread.currentThread().interrupt();
            }
        }
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
        return !url.isEmpty() && !visitedUrls.contains(url);
    }
}