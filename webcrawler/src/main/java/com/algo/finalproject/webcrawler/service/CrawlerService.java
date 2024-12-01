package com.algo.finalproject.webcrawler.service;

import com.algo.finalproject.webcrawler.WebcrawlerApplication;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.neo4j.core.Neo4jClient;
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
    private final ScheduledExecutorService scheduler;

    private static final Logger logger = LogManager.getLogger(WebcrawlerApplication.class);
    private final Semaphore semaphore;
    @Getter
    private volatile boolean crawlInProgress = false;
    private volatile boolean stopCrawl = true;
    private final Neo4jClient neo4jClient;

    private final PageRankingService pageRankingService;

    public CrawlerService(Neo4jClient neo4jClient, PageRankingService pageRankingService) {
        urlQueue = new PriorityBlockingQueue<>(11,new UrlComparator());
        visitedUrls = ConcurrentHashMap.newKeySet();
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        scheduler = Executors.newScheduledThreadPool(1);
        semaphore = new Semaphore(Runtime.getRuntime().availableProcessors());
        this.neo4jClient = neo4jClient;
        this.pageRankingService = pageRankingService;
    }
    public void startCrawling(List<String> urls) {
        if (crawlInProgress) {
            logger.info("Crawl already in progress");
            return;
        }
        crawlInProgress = true;
        stopCrawl = false;
        logger.info("Starting crawl with URLs: " + urls);
        CompletableFuture.runAsync(() -> crawl(urls));
    }

    private void crawl(List<String> urls) {
        urlQueue.addAll(urls);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Schedule a task to stop the crawling process after n minutes
        CompletableFuture<Void> timeoutFuture = new CompletableFuture<>();
        scheduler.schedule(() -> {
            logger.info("Stopping crawl after 30 minutes");
            timeoutFuture.completeExceptionally(new TimeoutException("Operation timed out"));
            crawlInProgress = false;
            stopCrawl = true;
//            executorService.shutdownNow();
        }, 1, TimeUnit.MINUTES);

        do {
            while (!urlQueue.isEmpty() && crawlInProgress) {
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

        // Combine the original future with the timeout future
        CompletableFuture<Void> combinedFuture = CompletableFuture.anyOf(allOf, timeoutFuture)
                .thenApply(result -> null);

        try {
            combinedFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error during crawling", e);
        } finally {
            scheduler.shutdown();
        }
        crawlInProgress = false;
    }
    private void processUrl(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String absUrl = link.absUrl("href");
                if (isValidUrl(absUrl)) {
                    // Check if the crawl should be stopped before adding the URL to the queue
                    if(!stopCrawl) {
                        urlQueue.offer(absUrl);
                        logger.info("Added URL to queue: " + absUrl);
                    }
                    try {
                        insertUrlToNeo4j(url, absUrl);
                        pageRankingService.updatePageRank(absUrl);
                    } catch (Exception e) {
                        logger.error("Failed to insert URL into Neo4j: " + absUrl, e);
                    }
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

        if (!url.startsWith("https://")) {
            return false;
        }

        return !url.contains("javascript:") && !url.contains(".onion") && !url.startsWith("tel:");
    }
    private void insertUrlToNeo4j(String currentUrl, String linkedUrl) {
        String cypherQuery = "MERGE (u1:Url {address: $currentUrl}) " +
                "MERGE (u2:Url {address: $linkedUrl}) " +
                "MERGE (u1)-[:LINKS_TO]->(u2)";
        neo4jClient.query(cypherQuery)
                .bind(currentUrl).to("currentUrl")
                .bind(linkedUrl).to("linkedUrl")
                .run();
        logger.info("Inserted URL into Neo4j and created relationship: " + currentUrl + " -> " + linkedUrl);
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