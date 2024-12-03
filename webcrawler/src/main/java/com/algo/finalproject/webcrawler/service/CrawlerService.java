package com.algo.finalproject.webcrawler.service;

import com.algo.finalproject.webcrawler.WebcrawlerApplication;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Logger logger = LogManager.getLogger(WebcrawlerApplication.class);
    private final PriorityBlockingQueue<UrlDepthPair> urlQueue;
    private final Set<String> visitedUrls;
    private final ExecutorService executorService;
    private final Semaphore semaphore;
    private final Neo4jClient neo4jClient;
    private final PageRankingService pageRankingService;
    private int maxDepth;
    @Getter
    private volatile boolean crawlInProgress = false;
    private volatile boolean stopCrawl = true;

    @Autowired
    public CrawlerService(Neo4jClient neo4jClient, PageRankingService pageRankingService) {
        this.urlQueue = new PriorityBlockingQueue<>(11, new UrlComparator());
        this.visitedUrls = ConcurrentHashMap.newKeySet();
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.semaphore = new Semaphore(Runtime.getRuntime().availableProcessors());
        this.neo4jClient = neo4jClient;
        this.pageRankingService = pageRankingService;
        this.maxDepth = 3;

    }

    public CrawlerService(Neo4jClient neo4jClient, PageRankingService pageRankingService, int maxDepth) {
        this.urlQueue = new PriorityBlockingQueue<>(11, new UrlComparator());
        this.visitedUrls = ConcurrentHashMap.newKeySet();
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.semaphore = new Semaphore(Runtime.getRuntime().availableProcessors());
        this.neo4jClient = neo4jClient;
        this.pageRankingService = pageRankingService;
        this.maxDepth = maxDepth;
    }


    public void startCrawling(List<String> urls, int maxDepth) {
        if (crawlInProgress) {
            logger.info("Crawl already in progress");
            return;
        }
        this.maxDepth = maxDepth;
        crawlInProgress = true;
        stopCrawl = false;

        logger.info("Starting crawl with URLs: " + urls);
        clean();
        logger.info("Starting crawl at time: " + System.currentTimeMillis());
        initCrawl(urls);
        List<UrlDepthPair> initialUrls = urls.stream().map(url -> new UrlDepthPair(url, 1)).toList();
        urlQueue.addAll(initialUrls);
        CompletableFuture.runAsync(this::crawl);
    }

    private void clean() {
        String cypherQuery = """
                MATCH (n) DETACH DELETE n;
                """;
        neo4jClient.query(cypherQuery)
                .run();
        logger.info("Cleaned Neo4j database");
    }

    private void initCrawl(List<String> urls) {
        float initialPageRank = 1;
        String cypherQuery = """
                  MERGE (u1:Page {url: $currentUrl})
                  SET u1.pageRank = $initialPageRank
                """;
        for (String url : urls) {
            neo4jClient.query(cypherQuery)
                    .bind(url).to("currentUrl")
                    .bind(initialPageRank).to("initialPageRank")
                    .run();
        }
    }

    private void crawl() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        do {
            while (!urlQueue.isEmpty() && crawlInProgress) {
                UrlDepthPair current = urlQueue.poll();
                if (current == null || visitedUrls.contains(current.url)) {
                    continue;
                }

                // Skip URLs exceeding max depth
                if (current.depth >= maxDepth) {
                    continue;
                }

                visitedUrls.add(current.url);
                try {
                    semaphore.acquire();
                    futures.add(CompletableFuture.runAsync(() -> {
                        try {
                            processUrl(current.url, current.depth + 1);
                        } finally {
                            semaphore.release();
                        }
                    }, executorService).exceptionally(ex -> {
                        logger.error("Error processing URL: " + current.url, ex);
                        return null;
                    }));
                } catch (InterruptedException e) {
                    logger.error("Interrupted while acquiring semaphore", e);
                    Thread.currentThread().interrupt();
                }
            }
        } while (!futures.stream().allMatch(CompletableFuture::isDone) || !urlQueue.isEmpty());
        logger.info("Crawl completed at time: " + System.currentTimeMillis());
        crawlInProgress = false;
    }

    private void processUrl(String url, int nextDepth) {
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(10000).get();
            List<String> extractedLinks = extractLinks(doc);

            for (String absUrl : extractedLinks) {
                if (isValidUrl(absUrl) && !stopCrawl) {
                    urlQueue.offer(new UrlDepthPair(absUrl, nextDepth));
                    logger.info("Added URL to queue: " + absUrl + " with depth: " + nextDepth);

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

    private List<String> extractLinks(Document doc) {
        List<String> links = new ArrayList<>();
        Elements elements = doc.select("a[href]");
        for (Element link : elements) {
            links.add(link.absUrl("href"));
        }
        return links;
    }

    private boolean isValidUrl(String url) {
        return !url.isEmpty() && !visitedUrls.contains(url) && url.startsWith("https://")
                && !url.contains("javascript:") && !url.contains(".onion") && !url.startsWith("tel:");
    }

    private void insertUrlToNeo4j(String currentUrl, String linkedUrl) {
        String cypherQuery = """
                MERGE (u1:Page {url: $currentUrl})
                MERGE (u2:Page {url: $linkedUrl})
                MERGE (u1)-[:LINKS_TO]->(u2)
                """;
        neo4jClient.query(cypherQuery)
                .bind(currentUrl).to("currentUrl")
                .bind(linkedUrl).to("linkedUrl")
                .run();
        logger.info("Inserted URL into Neo4j and created relationship: {} -> {}", currentUrl, linkedUrl);
    }


    private static class UrlDepthPair {
        final String url;
        final int depth;

        UrlDepthPair(String url, int depth) {
            this.url = url;
            this.depth = depth;
        }
    }

    private static class UrlComparator implements Comparator<UrlDepthPair> {
        @Override
        public int compare(UrlDepthPair u1, UrlDepthPair u2) {
            return calculatePriority(u2.url) - calculatePriority(u1.url);
        }

        private int calculatePriority(String url) {
            int priority = 0;
            if (url.contains("metmuseum.org") || url.contains("nps.gov") || url.contains("mfa.org")) {
                priority += 10;
            }
            if (url.contains("/exhibitions/") || url.contains("/collections/")) {
                priority += 5;
            }
            int depth = url.split("/").length;
            priority += (10 - depth);
            return priority;
        }
    }
}
